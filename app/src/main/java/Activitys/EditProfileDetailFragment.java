package Activitys;

import static Utility.ValidationUtils.replaceFragment;
import static Utility.ValidationUtils.showDatePicker;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.chateasy.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import Authentication.Create_UserOrEnter;
import Utility.CustomViewUtility;
import Utility.FireStoreDatabaseUtils;
import Utility.FirebaseAuthUtils;
import Utility.LoggerUtil;
import Utility.ValidationUtils;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EditProfileDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EditProfileDetailFragment extends Fragment {

    private View view;
    private TextInputEditText edit_profile_emailView, edit_profile_birthDateView;
    private MaterialButton edit_profile_details_saveView;
    private TextInputLayout edit_profile_family_birthDate_wrapperView;
    private Toolbar toolbar;
    private String userEmail;
    private Date userDob;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public EditProfileDetailFragment() {
        // Required empty public constructor
    }

    public static EditProfileDetailFragment newInstance(String param1, String param2) {
        EditProfileDetailFragment fragment = new EditProfileDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
            newInstance(mParam1, mParam2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_edit_profile_detail, container, false);
        initializeViews();
        fetchUserData();
        setUpEventListeners();
        return view;
    }

    private void initializeViews() {
        toolbar = view.findViewById(R.id.toolbar);
        edit_profile_emailView = view.findViewById(R.id.edit_profile_email);
        edit_profile_birthDateView = view.findViewById(R.id.edit_profile_birthDate);
        edit_profile_details_saveView = view.findViewById(R.id.edit_profile_details_save);
        edit_profile_family_birthDate_wrapperView = view.findViewById(R.id.edit_profile_family_birthDate_wrapper);
    }

    private void setUpEventListeners() {
        toolbar.setNavigationOnClickListener(v -> replaceFragment(new Fragment_User_Profile(), requireActivity()));
        edit_profile_family_birthDate_wrapperView.setEndIconOnClickListener(v ->
                showDatePicker(getParentFragmentManager(), "Select a BirthDate", edit_profile_birthDateView, "dd/MM/yyyy")
        );
        edit_profile_details_saveView.setOnClickListener(v -> {
            fetchUserInput();
            edit_profile_details_saveView.setIcon(CustomViewUtility.initializeProgressIndicatorDrawable(requireContext()));
            updateUserDetails(userEmail, userDob);
        });
    }

    private void fetchUserData() {
        FireStoreDatabaseUtils.fetchUserData(FirebaseAuthUtils.getUserId(requireContext()), new FireStoreDatabaseUtils.OnFetchCompleteListener() {
            @Override
            public void onSuccess(DocumentSnapshot snapshot) {
                if (snapshot != null && snapshot.exists()) {
                    userEmail = snapshot.getString("userEmail");
                    Date dobDate = snapshot.getDate("userDOB");

                    if (userEmail != null) {
                        edit_profile_emailView.setText(userEmail);
                    }

                    if (dobDate != null) {
                        // Format the Date object to "dd/MM/yyyy" format
                        String dobString = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(dobDate);
                        edit_profile_birthDateView.setText(dobString);
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

    private void updateUserDetails(String email, Date dob) {
        if (email == null && dob == null) {
            Toast.makeText(requireContext(), "No changes to update", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = FirebaseAuthUtils.getUserId(requireContext());
        Map<String, Object> updates = new HashMap<>();
        if (email != null && !email.isEmpty()) updates.put("userEmail", email);

        if (dob != null)
            updates.put("userDOB", dob);

        FireStoreDatabaseUtils.getUserDocument(currentUserId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    edit_profile_details_saveView.setIcon(null);
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    replaceFragment(new Fragment_User_Profile(), requireActivity());
                })
                .addOnFailureListener(e -> {
                    edit_profile_details_saveView.setIcon(null);
                    Toast.makeText(requireContext(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchUserInput() {
        userEmail = ValidationUtils.getStringFromEditText(edit_profile_emailView);
        String dobString = ValidationUtils.getStringFromEditText(edit_profile_birthDateView);

        if (!dobString.isEmpty()) {
            try {
                userDob = new SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(dobString);
            } catch (ParseException e) {
                LoggerUtil.logError("Error parsing date string", e);
                ValidationUtils.showToast(requireContext(), "Error parsing date");
            }
        }
    }
}
