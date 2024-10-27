package Authentication;

import static Utility.ValidationUtils.showDatePicker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.chateasy.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;

import org.jetbrains.annotations.Contract;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import Activitys.MainActivity;
import CustomViews.CustomCropViewHandler;
import Models.User;
import Models.UserStatus;
import Utility.CustomViewUtility;
import Utility.FireStoreDatabaseUtils;
import Utility.FirebaseAuthUtils;
import Utility.FirebaseMessagingUtils;
import Utility.LoggerUtil;
import Utility.NotificationUtils;
import Utility.UserStatusManager;
import Utility.ValidationUtils;

public class Fill_User_Details extends Fragment {
    private View view;
    private MaterialButton finishFillFormView;
    private TextView namePreviewView;
    private TextInputEditText userFirstNameView, userLastNameView, userEmailIdView, userBirthDateView, userAboutView;
    private TextInputLayout userFirstNameLayoutView;
    private TextInputLayout userBirthDateLayoutView;
    private ImageView userImgView;
    private Uri selectedImageUri;
    private Uri croppedImageUri;
    private String userPhoneNumber;

    private UserStatusManager userStatusManager;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> cropActivityResultLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_fill__user__details, container, false);
        initializeViews();
        setEndIconOnClickListener();
        initializePhotoPickerLauncher();
        initializeTraditionalImagePickerLauncher();
        initializePermissionLauncher();
        initializeCropActivityLauncher();
        setOnFinishFillForm();
        setNamePreviewViewFrom();
        return view;
    }

    private void initializeViews() {
        MaterialButton photoSelectView = view.findViewById(R.id.photoSelect);
        finishFillFormView = view.findViewById(R.id.finish_fillForm);
        namePreviewView = view.findViewById(R.id.name_preview);
        userFirstNameView = view.findViewById(R.id.user_FirstName);
        userLastNameView = view.findViewById(R.id.user_LastName);
        userEmailIdView = view.findViewById(R.id.user_EmailId);
        userBirthDateView = view.findViewById(R.id.user_birthDate);
        userAboutView = view.findViewById(R.id.user_About);
        userFirstNameLayoutView = view.findViewById(R.id.user_firstNameLayout);
        userBirthDateLayoutView = view.findViewById(R.id.user_birthDateLayout);
        userImgView = view.findViewById(R.id.userImg);

        photoSelectView.setOnClickListener(v -> checkAndPickImage());
    }

    private void initializePhotoPickerLauncher() {
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                startCropViewActivity();
            } else {
                Log.d("PhotoPicker", "No media selected");
            }
        });
    }

    private void initializeTraditionalImagePickerLauncher() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            selectedImageUri = data.getData();
                            startCropViewActivity();
                        }
                    }
                });
    }

    private void initializePermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchTraditionalImagePicker();
                    } else {
                        showPermissionDeniedDialog();
                    }
                });
    }

    private void initializeCropActivityLauncher() {
        cropActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String croppedImageUriString = data.getStringExtra("croppedImageUri");
                            croppedImageUri = Uri.parse(croppedImageUriString);
                            userImgView.setImageURI(croppedImageUri);
                        }
                    }
                });
    }

    private void checkAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launchPhotoPicker();
        } else {
            checkPermissionAndLaunchTraditionalPicker();
        }
    }

    private void launchPhotoPicker() {
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void checkPermissionAndLaunchTraditionalPicker() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            launchTraditionalImagePicker();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void launchTraditionalImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void showPermissionDeniedDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Permission Denied")
                .setMessage("To pick an image, the app needs permission to access your storage. Please grant the permission in the app settings.")
                .setPositiveButton("OK", (dialog, which) -> {
                })
                .setNegativeButton("Settings", (dialog, which) -> openAppSettings())
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void setEndIconOnClickListener() {
        userBirthDateLayoutView.setEndIconOnClickListener(v -> showDatePicker(getParentFragmentManager(), "Select a BirthDate", userBirthDateView, "dd/MM/yyyy"));
    }

    private void setNamePreviewViewFrom() {
        userFirstNameView.addTextChangedListener(new NameTextWatcher());
        userLastNameView.addTextChangedListener(new NameTextWatcher());
    }

/*    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select a BirthDate")
                .build();

        datePicker.show(getParentFragmentManager(), "DatePicker");

        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                long selectedDate = selection;
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                    String formattedDate = sdf.format(new Date(selectedDate));
                    userBirthDateView.setText(formattedDate);
                } catch (Exception e) {
                    LoggerUtil.logError("An error occurred", e);
                }
            }
        });
    }*/

    private void setOnFinishFillForm() {
        finishFillFormView.setOnClickListener(v -> {
            finishFillFormView.setIcon(CustomViewUtility.initializeProgressIndicatorDrawable(requireContext()));
            setSelectedUserValues();
        });
    }

    private void startCropViewActivity() {
        Intent intent = new Intent(requireContext(), CustomCropViewHandler.class);
        intent.putExtra("imageUri", selectedImageUri.toString());
        cropActivityResultLauncher.launch(intent);
    }

    private void setSelectedUserValues() {
        // Validate only the first name
        String firstName = ValidationUtils.getStringFromEditText(userFirstNameView);
        if (firstName.isEmpty()) {
            userFirstNameLayoutView.setError("This field is required");
            return;
        }
        userFirstNameLayoutView.setError(null);

        // Retrieve other user input fields without enforcing required validation
        String lastName = ValidationUtils.getStringFromEditText(userLastNameView);
        String userEmail = ValidationUtils.getStringFromEditText(userEmailIdView);
        String userAbout = ValidationUtils.getStringFromEditText(userAboutView);

        // Concatenate first and last name (allowing last name to be empty)
        String fullName = firstName + (lastName.isEmpty() ? "" : " " + lastName);

        // Retrieve user phone number from arguments if available
        Bundle args = getArguments();
        userPhoneNumber = args != null ? args.getString("phoneNumber", "") : "";

        // Parse date of birth if provided
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        Date userDob = null;
        String dobString = ValidationUtils.getStringFromEditText(userBirthDateView);
        if (!dobString.isEmpty()) {
            try {
                userDob = sdf.parse(dobString);
            } catch (ParseException e) {
                LoggerUtil.logError("Error parsing date string", e);
                ValidationUtils.showToast(requireContext(), "Error parsing date");
                return;
            }
        }

        // Retrieve FCM token asynchronously and proceed with user creation
        final String[] FCMToken = {""};
        Date finalUserDob = userDob;
        FirebaseMessagingUtils.getUserTokenAndSaveToFirestore(new FirebaseMessagingUtils.TokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                Log.d("FCM Token Received", token);
                FCMToken[0] = token;
                saveUserDetails(fullName, userPhoneNumber, userEmail, userAbout, finalUserDob, FCMToken[0]);
            }

            @Override
            public void onTokenError(Exception exception) {
                Log.e("FCM Token Error", Objects.requireNonNull(exception.getMessage()));
                FCMToken[0] = ""; // Set empty token if an error occurs
                saveUserDetails(fullName, userPhoneNumber, userEmail, userAbout, finalUserDob, FCMToken[0]);
            }
        });
    }

    private void saveUserDetails(String fullName, String phoneNumber, String email, String about, Date dob, String token) {
        User user = new User(FirebaseAuthUtils.getCurrentUserId(), fullName, phoneNumber, email, about, dob,
                Timestamp.now(), Timestamp.now(), Timestamp.now(), UserStatus.ONLINE, token);

        boolean[] successFlags = {false, false};

        FireStoreDatabaseUtils.getUserDocument(FirebaseAuthUtils.getCurrentUserId()).set(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        successFlags[0] = true;
                        checkAndStartMainActivity(successFlags);
                        NotificationUtils.getUserTokenAndSaveToFirestore();

                        String userId = FirebaseAuthUtils.getUserId(requireContext());
                        if (userId != null) {
                            userStatusManager = new UserStatusManager(userId);
                        } else {
                            Log.e("ChatApplication", "User ID is null. Cannot initialize UserStatusManager.");
                        }
                    } else {
                        Log.e("ChatApplication", "Failed to save user details to Firestore.");
                    }
                });

        if (croppedImageUri != null) {
            FireStoreDatabaseUtils storageHelper = new FireStoreDatabaseUtils();
            FireStoreDatabaseUtils.UploadListener uploadListener = new FireStoreDatabaseUtils.UploadListener() {
                @Override
                public void onSuccess(Uri downloadUri) {
                    successFlags[1] = true;
                    checkAndStartMainActivity(successFlags);
                }

                @Override
                public void onFailure(Exception e) {
                    LoggerUtil.logError("Upload failed: ", e);
                    ValidationUtils.showToast(requireContext(), "Upload failed");
                    checkAndStartMainActivity(successFlags);
                }
            };

            storageHelper.uploadUserProfileImage(croppedImageUri, uploadListener, FirebaseAuthUtils.getCurrentUserId());
        } else {
            ValidationUtils.showToast(requireContext(), "Cropped image URI is null");
            checkAndStartMainActivity(successFlags);
        }
    }


    private void checkAndStartMainActivity(@NonNull boolean[] successFlags) {
        if (successFlags[0] && successFlags[1]) {
            FirebaseAuthUtils.setLoggedIn(true, FirebaseAuthUtils.getCurrentUserId(), userPhoneNumber, true, requireContext());
            finishFillFormView.setIcon(null);
            Intent intent = new Intent(requireActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    private void setOnUserNamePreview() {
        String firstName = userFirstNameView.getText() != null ? userFirstNameView.getText().toString() : "";
        String lastName = userLastNameView.getText() != null ? userLastNameView.getText().toString() : "";
        String fullName = firstName + " " + lastName;
        namePreviewView.setText(fullName);
    }

    private class NameTextWatcher implements TextWatcher {
        @Contract(pure = true)
        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            setOnUserNamePreview();
        }

        @Contract(pure = true)
        @Override
        public void afterTextChanged(Editable editable) {
        }
    }
}