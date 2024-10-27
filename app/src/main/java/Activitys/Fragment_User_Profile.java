package Activitys;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.chateasy.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;

import Authentication.Create_UserOrEnter;
import Utility.FireStoreDatabaseUtils;
import Utility.FirebaseAuthUtils;
import Utility.ImageUtils;
import Utility.LoggerUtil;
import Utility.ValidationUtils;

public class Fragment_User_Profile extends Fragment {

    private ImageView manageProfileAvatar;
    private TextView manage_profile_name, manage_profile_about;
    private ConstraintLayout manage_profile_name_container, manage_profile_about_container, manage_profile_detail_container;
    private View view;
    private MaterialToolbar materialToolbar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_user_profile, container, false);
        initializeViews();
        // Only proceed if the fragment is added
        if (isAdded()) {
            Context context = requireContext();
            Resources resources = getResources();
            loadImage(context, resources);
            loadCurrentUserDetail(context);
            actionEventOnProfile();
        }
        return view;
    }

    private void initializeViews() {
        materialToolbar = view.findViewById(R.id.toolbar);
        manageProfileAvatar = view.findViewById(R.id.manage_profile_avatar);
        manage_profile_name = view.findViewById(R.id.manage_profile_name);
        manage_profile_about = view.findViewById(R.id.manage_profile_about);
        manage_profile_name_container = view.findViewById(R.id.manage_profile_name_container);
        manage_profile_about_container = view.findViewById(R.id.manage_profile_about_container);
        manage_profile_detail_container = view.findViewById(R.id.manage_profile_detail_container);
    }

    public void actionEventOnProfile() {
        materialToolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
        manage_profile_name_container.setOnClickListener(v -> replaceFragment(new EditProfileNameFragment()));

        manage_profile_about_container.setOnClickListener(v -> {
            // Handle click events for "about" container if needed
        });
        manage_profile_detail_container.setOnClickListener(v -> replaceFragment(new EditProfileDetailFragment()));
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE); // Clear the back stack
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        fragmentTransaction.replace(R.id.fragment_section_setting_edit, fragment);
        fragmentTransaction.commit();
    }

    private void loadImage(Context context, Resources resources) {
        FireStoreDatabaseUtils storageHelper = new FireStoreDatabaseUtils();
        StorageReference imageRef = storageHelper.getImageReference(FirebaseAuthUtils.getUserId(context));

        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            if (isAdded()) {
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
            }
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                showPlaceholderImage(resources);
            }
        });
    }

    private void renderProfileImage(Drawable placehUserDrawable) {
        manageProfileAvatar.setImageDrawable(placehUserDrawable);
    }

    private void showPlaceholderImage(Resources resources) {
        int placeholderDrawableId = R.drawable.user;
        Drawable placeholderDrawable = ResourcesCompat.getDrawable(resources, placeholderDrawableId, null);
        manageProfileAvatar.setImageDrawable(placeholderDrawable);
    }

    private void loadCurrentUserDetail(Context context) {
        FireStoreDatabaseUtils.getUserData(FirebaseAuthUtils.getUserId(context), (snapshot, error) -> {
            if (!isAdded()) return;  // Ensure fragment is attached

            if (error != null) {
                ValidationUtils.showToast(context, "Failed to retrieve user data");
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                manage_profile_name.setText(snapshot.getString("userName"));
                String about = !Objects.equals(snapshot.getString("userAbout"), "")
                        ? snapshot.getString("userAbout")
                        : requireContext().getResources().getString(R.string.about_user);
                manage_profile_about.setText(about);
            } else {
                ValidationUtils.showToast(context, "User document does not exist");
                ValidationUtils.showSnackBarWithAction(requireView(), "User document does not exist", "LogOut", v -> {
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
