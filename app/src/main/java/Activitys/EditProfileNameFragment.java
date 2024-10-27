package Activitys;

import static Utility.ValidationUtils.replaceFragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.chateasy.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Authentication.Create_UserOrEnter;
import Utility.CustomViewUtility;
import Utility.FireStoreDatabaseUtils;
import Utility.FirebaseAuthUtils;
import Utility.LoggerUtil;
import Utility.ValidationUtils;

public class EditProfileNameFragment extends Fragment {
    private View view;
    private TextInputEditText edit_profile_name_given_nameView, edit_profile_name_family_nameView;
    private MaterialButton edit_profile_name_saveView;
    private TextInputLayout edit_profile_given_name_wrapperView;
    private Toolbar toolbar;
    private String firstName = "", lastName = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.edit_profile_name_fragment, container, false);
        initializeViews();
        fetchUserData();  // Fetch user data initially from storage
        setUpEventListeners(); // Set up action events
        return view;
    }

    private void initializeViews() {
        toolbar = view.findViewById(R.id.toolbar);
        edit_profile_name_given_nameView = view.findViewById(R.id.edit_profile_name_given_name);
        edit_profile_name_family_nameView = view.findViewById(R.id.edit_profile_name_family_name);
        edit_profile_name_saveView = view.findViewById(R.id.edit_profile_name_save);
        edit_profile_given_name_wrapperView = view.findViewById(R.id.edit_profile_given_name_wrapper);
    }

    private void setUpEventListeners() {
        toolbar.setNavigationOnClickListener(v -> replaceFragment(new Fragment_User_Profile(), requireActivity()));

        edit_profile_name_saveView.setOnClickListener(v -> {
            fetchUserInput(); // Fetch latest values from view

            // Validate first name
            if (firstName.isEmpty()) {
                edit_profile_given_name_wrapperView.setError("This field is required");
                return;
            }
            edit_profile_given_name_wrapperView.setError(null);
            edit_profile_name_saveView.setIcon(CustomViewUtility.initializeProgressIndicatorDrawable(requireContext()));

            // Construct full name and save
            String fullName = firstName + (lastName.isEmpty() ? "" : " " + lastName);
            updateUserName(fullName);
        });
    }

    private void updateUserName(String newUsername) {
        String currentUserId = FirebaseAuthUtils.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        FireStoreDatabaseUtils.getUserDocument(currentUserId)
                .update("userName", newUsername)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Username updated successfully", Toast.LENGTH_SHORT).show();
                    edit_profile_name_saveView.setIcon(null);
                    replaceFragment(new Fragment_User_Profile(), requireActivity());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error updating username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    edit_profile_name_saveView.setIcon(null);
                });
    }

    private void fetchUserInput() {
        firstName = ValidationUtils.getStringFromEditText(edit_profile_name_given_nameView);
        lastName = ValidationUtils.getStringFromEditText(edit_profile_name_family_nameView);
    }

    private void fetchUserData() {
        FireStoreDatabaseUtils.fetchUserData(FirebaseAuthUtils.getUserId(requireContext()), new FireStoreDatabaseUtils.OnFetchCompleteListener() {
            @Override
            public void onSuccess(DocumentSnapshot snapshot) {
                if (snapshot != null) {
                    String fullName = snapshot.getString("userName");
                    if (fullName != null) {
                        Pattern pattern = Pattern.compile("(\\S+)\\s*(\\S*)");
                        Matcher matcher = pattern.matcher(fullName);

                        if (matcher.matches()) {
                            String firstNameFromStorage = matcher.group(1);
                            String lastNameFromStorage = matcher.group(2);

                            edit_profile_name_given_nameView.setText(firstNameFromStorage);
                            edit_profile_name_family_nameView.setText(lastNameFromStorage);
                        }
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                ValidationUtils.showToast(requireContext(), errorMessage);
                ValidationUtils.showSnackBarWithAction(requireView(), errorMessage, "LogOut", v -> {
                    ValidationUtils.showLogoutDialog(requireContext(), (dialogInterface, i) -> {
                        FirebaseAuthUtils.safeLogout(new FirebaseAuthUtils.OnLogoutListener() {
                            @Override
                            public void onLogoutComplete() {
                                Intent intent = new Intent(requireContext(), Create_UserOrEnter.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }

                            @Override
                            public void onLogoutError(Exception e) {
                                LoggerUtil.logErrors("Logout Error: ", e.getMessage());
                                ValidationUtils.showSnackBar(requireView(), "Error logging out. Please try again.");
                            }
                        });
                    });
                });
            }
        });
    }
}
