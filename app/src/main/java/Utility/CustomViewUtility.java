package Utility;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

import Activitys.UserAppSettings;
import Activitys.UserProfile;
import Authentication.Create_UserOrEnter;
import Models.Feedback;
import WindowPreferences.WindowPreferencesManager;

public class CustomViewUtility {

    @NonNull
    public static IndeterminateDrawable<CircularProgressIndicatorSpec> initializeProgressIndicatorDrawable(Context context) {
        // Initialize CircularProgressIndicatorSpec and IndeterminateDrawable
        CircularProgressIndicatorSpec spec = new CircularProgressIndicatorSpec(context, /*attrs=*/ null, 0, com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall);

        return IndeterminateDrawable.createCircularDrawable(context, spec);
    }

    public static class BottomSheet extends BottomSheetDialogFragment {

        private ImageView userProfileImg;
        private MaterialTextView currentUserNameView;
        private MaterialTextView currentUserPhoneNoView, aboutUserContent;
        private View bottomSheetInternal;

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
            Button settingCurrentUserBtnView = bottomSheetDialog.findViewById(R.id.settingCurrentUserBtn);
            Button userFeedBackViewBtn = bottomSheetDialog.findViewById(R.id.feedbackToApp);
            CardView userDetailCardView = bottomSheetDialog.findViewById(R.id.userDetailCard);
            LinearLayout expandableContentView = bottomSheetDialog.findViewById(R.id.expandableContent);

            userProfileImg = bottomSheetDialog.findViewById(R.id.CurrentUserImage);
            currentUserNameView = bottomSheetDialog.findViewById(R.id.CurrentUserName);
            currentUserPhoneNoView = bottomSheetDialog.findViewById(R.id.CurrentUserPhoneNo);
            aboutUserContent = bottomSheetDialog.findViewById(R.id.aboutUserContent);

            assert editeCurrentUserBtnView != null;
            editeCurrentUserBtnView.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), UserProfile.class));
                requireActivity().finish();
            });

            assert settingCurrentUserBtnView != null;
            settingCurrentUserBtnView.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), UserAppSettings.class));
                requireActivity().finish();
            });
            assert logOutCurrentUserBtnView != null;
            logOutCurrentUserBtnView.setOnClickListener(v -> {
                ValidationUtils.showLogoutDialog(requireContext(), (dialogInterface, i) -> {
                    String userId = FirebaseAuthUtils.getUserId(requireContext());

                    if (userId == null) {
                        LoggerUtil.logErrors("User  is null: ", null);
                    } else {
                        UserStatusManager userStatusManager = new UserStatusManager(userId);
                        userStatusManager.setUserAway();
                    }

                    // Save empty token to Firestore
                    NotificationUtils.saveTokenToFirestore("");

                    // Log out the user
                    FirebaseAuthUtils.setLoggedIn(false, FirebaseAuthUtils.getCurrentUserId(), null, false, requireContext());

                    // Start the Create_UserOrEnter activity and finish the current activity
                    Intent intent = new Intent(requireContext(), Create_UserOrEnter.class);
                    startActivity(intent);
                    requireActivity().finish();
                });
            });

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


            assert userFeedBackViewBtn != null;
            userFeedBackViewBtn.setOnClickListener(v -> {
                showFeedbackDialog();
            });

            return bottomSheetDialog;
        }

        private void showFeedbackDialog() {
            FeedbackDialog dialog = FeedbackDialog.newInstance();
            dialog.setOnFeedbackSubmitListener((feedback, success) -> {
                if (success) {
                    Toast.makeText(getContext(), "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                    logFeedback(feedback);
                }
            });
            dialog.show(getParentFragmentManager(), "feedback_dialog");
        }

        private void logFeedback(@NonNull Feedback feedback) {
            // Log feedback details (for analytics or debugging)
            Log.d("FeedBack :: ", "Feedback received: " + "\nRating: " + feedback.getRating() + "\nText: " + feedback.getFeedbackText() + "\nEmail: " + (feedback.isAnonymous() ? "Anonymous" : feedback.getUserEmail()) + "\nTimestamp: " + feedback.getTimestamp());
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
                    public Drawable onResourceReady(Drawable resource) {
                        renderProfileImage(resource);
                        return resource;
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
                    aboutUserContent.setText(snapshot.getString("userAbout"));
                } else {
                    // User document does not exist
                    ValidationUtils.showToast(context, "User document does not exist");
                    ValidationUtils.showSnackBarWithAction(bottomSheetInternal, "User document does not exist", "LogOut", v -> {
                        ValidationUtils.showLogoutDialog(requireContext(), (dialogInterface, i) -> {
                            FirebaseAuthUtils.safeLogout(new FirebaseAuthUtils.OnLogoutListener() {
                                @Override
                                public void onLogoutComplete() {
                                    try {
                                        Intent intent = new Intent(requireContext(), Create_UserOrEnter.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    } catch (Exception e) {
                                        LoggerUtil.logErrors("Navigation Error: ", e.getMessage());
                                    }
                                }

                                @Override
                                public void onLogoutError(Exception e) {
                                    LoggerUtil.logErrors("Logout Error: ", e.getMessage());
                                    ValidationUtils.showSnackBar(requireView(),
                                            "Error logging out. Please try again.");
                                }
                            });
                        });
                    });
                }
            });
        }
    }
}