package Utility;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.example.chateasy.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.storage.StorageReference;

import Authentication.Create_UserOrEnter;
import Models.UserStatus;
import WindowPreferences.WindowPreferencesManager;

public class CustomViewUtility {

    public static IndeterminateDrawable<CircularProgressIndicatorSpec> initializeProgressIndicatorDrawable(Context context) {
        // Initialize CircularProgressIndicatorSpec and IndeterminateDrawable
        CircularProgressIndicatorSpec spec = new CircularProgressIndicatorSpec(
                context, /*attrs=*/ null, 0,
                com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall);

        return IndeterminateDrawable.createCircularDrawable(context, spec);
    }

    public static class BottomSheet extends BottomSheetDialogFragment {

        private ImageView userProfileImg;
        private MaterialTextView currentUserNameView;
        private MaterialTextView currentUserPhoneNoView;
        private View bottomSheetInternal;

        public static void focusAndShowKeyboard(@NonNull View view) {
            view.requestFocus();
            if (view.hasWindowFocus()) {
                showTheKeyboardNow(view);
            } else {
                view.getViewTreeObserver().addOnWindowFocusChangeListener(new ViewTreeObserver.OnWindowFocusChangeListener() {
                    @Override
                    public void onWindowFocusChanged(boolean hasFocus) {
                        if (hasFocus) {
                            showTheKeyboardNow(view);
                            view.getViewTreeObserver().removeOnWindowFocusChangeListener(this);
                        }
                    }
                });
            }
        }

        private static void showTheKeyboardNow(@NonNull View view) {
            if (view.isFocused()) {
                view.post(() -> {
                    InputMethodManager inputMethodManager = getInputMethodManager(view.getContext());
                    inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                });
            }
        }

        public static void hideKeyboard(@NonNull Context context, @NonNull View view) {
            InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        public static InputMethodManager getInputMethodManager(Context context) {
            return (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
            new WindowPreferencesManager(requireContext()).applyEdgeToEdgePreference(bottomSheetDialog.getWindow());
            bottomSheetDialog.setContentView(R.layout.user_profile);
            loadImage(requireContext(), getResources());
            loadCurrentUserDetail(requireContext());
            bottomSheetInternal = bottomSheetDialog.findViewById(R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetInternal);
                behavior.setPeekHeight(500);
                behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        Log.d("BottomSheet", "onStateChanged: " + newState);
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                        Log.d("BottomSheet", "onSlide: " + slideOffset);
                    }
                });

                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }

            Button editeCurrentUserBtnView = bottomSheetDialog.findViewById(R.id.editeCurrentUserBtn);
            Button logOutCurrentUserBtnView = bottomSheetDialog.findViewById(R.id.logOutCurrentUserBtn);
            Button expandToSeeMoreDetailBTNView = bottomSheetDialog.findViewById(R.id.expandCollapseIcon);
            CardView userDetailCardView = bottomSheetDialog.findViewById(R.id.userDetailCard);
            LinearLayout expandableContentView = bottomSheetDialog.findViewById(R.id.expandableContent);

            userProfileImg = bottomSheetDialog.findViewById(R.id.CurrentUserImage);
            currentUserNameView = bottomSheetDialog.findViewById(R.id.CurrentUserName);
            currentUserPhoneNoView = bottomSheetDialog.findViewById(R.id.CurrentUserPhoneNo);

            assert editeCurrentUserBtnView != null;
            editeCurrentUserBtnView.setOnClickListener(v -> Toast.makeText(requireContext(), "Edit Button will be clicked", Toast.LENGTH_SHORT).show());
            assert logOutCurrentUserBtnView != null;
            logOutCurrentUserBtnView.setOnClickListener(v -> ValidationUtils.showLogoutDialog(requireContext(), (dialogInterface, i) -> {
                if (FirebaseAuthUtils.getUserId(requireContext()) == null) {
                    LoggerUtil.logErrors("User is null : ", FirebaseAuthUtils.getUserId(requireContext()));
                } else {
                    FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(requireContext()), UserStatus.AWAY);
                }
                NotificationUtils.saveTokenToFirestore("");
                FirebaseAuthUtils.setLoggedIn(false, FirebaseAuthUtils.getCurrentUserId(), null, false, requireContext());
                startActivity(new Intent(requireContext(), Create_UserOrEnter.class));
                requireActivity().finish();
            }));

            AnimatorSet iconRotation;
            final boolean[] isExpanded = {false};
            iconRotation = (AnimatorSet) AnimatorInflater.loadAnimator(requireContext(), R.animator.icon_transition);
            assert expandToSeeMoreDetailBTNView != null;
            expandToSeeMoreDetailBTNView.setOnClickListener(view -> {
                TransitionSet transitionSet = new TransitionSet();
                transitionSet.addTransition(new Fade().setDuration(600));
                transitionSet.addTransition(new ChangeBounds().setDuration(600));

                assert userDetailCardView != null;
                TransitionManager.beginDelayedTransition(userDetailCardView, transitionSet);
                assert expandableContentView != null;
                if (isExpanded[0]) {
                    // Collapse the card
                    expandableContentView.setVisibility(View.GONE);
                    iconRotation.setTarget(expandToSeeMoreDetailBTNView);
                    iconRotation.start();
                } else {
                    // Expand the card
                    expandableContentView.setVisibility(View.VISIBLE);
                    iconRotation.setTarget(expandToSeeMoreDetailBTNView);
                    iconRotation.reverse();
                }
                isExpanded[0] = !isExpanded[0];
            });

            return bottomSheetDialog;
        }

        private void loadImage(Context context, Resources resources) {
            // Get the StorageReference for the image
            FireStoreDatabaseUtils storageHelper = new FireStoreDatabaseUtils();
            StorageReference imageRef = storageHelper.getImageReference(FirebaseAuthUtils.getUserId(context)); // Assuming ImageUtils.getImageReference() returns the StorageReference

            // Download the image from Firebase Storage
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Load the image using Glide or any other image loading library
                ImageUtils.loadImage(context, uri.toString(), new ImageUtils.ImageLoadListener() {
                    @Override
                    public void onResourceReady(Drawable resource) {
                        renderProfileImage(resource);
                    }

                    @Override
                    public void onLoadFailed() {
                        showPlaceholderImage(resources);
                    }
                });
            }).addOnFailureListener(e -> showPlaceholderImage(resources));
        }

        private void renderProfileImage(Drawable placehUserDrawable) {
            userProfileImg.setImageDrawable(placehUserDrawable);
        }

        private void showPlaceholderImage(Resources resources) {
            assert userProfileImg != null;
            int placeholderDrawableId = R.drawable.user;
            Drawable placeholderDrawable = ResourcesCompat.getDrawable(resources, placeholderDrawableId, null);
            userProfileImg.setImageDrawable(placeholderDrawable);
        }

        /*        private void loadCurrentUserDetail(Context context) {
                    FireStoreDatabaseUtils.getUserData(FirebaseAuthUtils.getUserId(context), task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                // Handle the user data here
                                currentUserNameView.setText(document.getString("userName"));
                                currentUserPhoneNoView.setText(document.getString("userMobileNumber"));
                            } else {
                                // User document does not exist
                                ValidationUtils.showToast(context, "User document does not exist");
                                ValidationUtils.showSnackBarWithAction(bottomSheetInternal, "User document does not exist", "LogOut", v -> {

                                });
                            }
                        } else {
                            // Failed to retrieve user data
                            ValidationUtils.showToast(context, "Failed to retrieve user data");
                        }
                    });
                }
            }*/
        private void loadCurrentUserDetail(Context context) {
            FireStoreDatabaseUtils.getUserData(FirebaseAuthUtils.getUserId(context), (snapshot, error) -> {
                if (error != null) {
                    // Failed to retrieve user data
                    ValidationUtils.showToast(context, "Failed to retrieve user data");
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    // Handle the user data here
                    currentUserNameView.setText(snapshot.getString("userName"));
                    currentUserPhoneNoView.setText(snapshot.getString("userMobileNumber"));
                } else {
                    // User document does not exist
                    ValidationUtils.showToast(context, "User document does not exist");
                    ValidationUtils.showSnackBarWithAction(bottomSheetInternal, "User document does not exist", "LogOut", v -> {
                        ValidationUtils.showLogoutDialog(requireContext(), (dialogInterface, i) -> {
                                    if (FirebaseAuthUtils.getUserId(requireContext()) == null) {
                                        LoggerUtil.logErrors("User is null : ", FirebaseAuthUtils.getUserId(requireContext()));
                                    } else {
                                        FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(requireContext()), UserStatus.AWAY);
                                    }
                                    NotificationUtils.saveTokenToFirestore("");
                                    FirebaseAuthUtils.setLoggedIn(false, FirebaseAuthUtils.getCurrentUserId(), null, false, requireContext());
                                    startActivity(new Intent(requireContext(), Create_UserOrEnter.class));
                                    requireActivity().finish();
                                }
                        );
                    });
                }
            });
        }
    }
}