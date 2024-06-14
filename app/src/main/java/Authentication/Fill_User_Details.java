package Authentication;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.chateasy.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;

import org.jetbrains.annotations.Contract;

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
import Utility.ValidationUtils;

public class Fill_User_Details extends Fragment {
    private View view;
    private MaterialButton photoSelectView, finishFillFormView;
    private TextView namePreviewView;
    private TextInputEditText userFirstNameView, userLastNameView, userEmailIdView, userBirthDateView, userAboutView;
    private TextInputLayout userFirstNameLayoutView;
    private TextInputLayout userBirthDateLayoutView;
    private ImageView userImgView;
    private ScrollView fillUserDetailFrameView;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private Uri selectedImageUri;
    private Uri croppedImageUri;
    private final ActivityResultLauncher<Intent> cropActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            assert result.getData() != null;
                            String croppedImageUriString = result.getData().getStringExtra("croppedImageUri");
                            croppedImageUri = Uri.parse(croppedImageUriString);
                            userImgView.setImageURI(croppedImageUri);
                        }
                    });
    private String userPhoneNumber;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_fill__user__details, container, false);
        initializeViews();
        setEndIconOnClickListener();
        getRequestPermissionLauncher();
        setOnFinishFillForm();
        setNamePreviewViewFrom();
        return view;
    }

    private void initializeViews() {
        fillUserDetailFrameView = view.findViewById(R.id.fillUserDetailFrame);
        photoSelectView = view.findViewById(R.id.photoSelect);
        finishFillFormView = view.findViewById(R.id.finish_fillForm);
        namePreviewView = view.findViewById(R.id.name_preview);
        userFirstNameView = view.findViewById(R.id.user_FirstName);
        userLastNameView = view.findViewById(R.id.user_LastName);
        userEmailIdView = view.findViewById(R.id.user_EmailId);
        userBirthDateView = view.findViewById(R.id.user_birthDate);
        userAboutView = view.findViewById(R.id.user_About);
        userFirstNameLayoutView = view.findViewById(R.id.user_firstNameLayout);
        /*TextInputLayout userLastNameLayoutView = view.findViewById(R.id.user_LastNameLayout);
        TextInputLayout userEmailIdLayoutView = view.findViewById(R.id.user_EmailIdLayout);*/
        userBirthDateLayoutView = view.findViewById(R.id.user_birthDateLayout);
        userImgView = view.findViewById(R.id.userImg);
    }

    private void setEndIconOnClickListener() {
        userBirthDateLayoutView.setEndIconOnClickListener(v -> showDatePicker());
    }

    private void setNamePreviewViewFrom() {
        userFirstNameView.addTextChangedListener(new NameTextWatcher());
        userLastNameView.addTextChangedListener(new NameTextWatcher());
    }

    private void showDatePicker() {
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
    }

    private void setOnFinishFillForm() {
        finishFillFormView.setOnClickListener(v -> {
            finishFillFormView.setIcon(CustomViewUtility.initializeProgressIndicatorDrawable(requireContext()));
            setSelectedUserValues();
        });
    }

    private void getRequestPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchPickImageIntent();
                    } else {
                        showPermissionDeniedDialog();
                    }
                });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        handleImagePickResult(result.getData());
                    }
                });

        photoSelectView.setOnClickListener(v -> checkPermissionAndPickImage());
    }

    private void checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            launchPickImageIntent();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void launchPickImageIntent() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(pickImageIntent);
    }

    private void handleImagePickResult(Intent data) {
        if (data == null || data.getData() == null) {
            return;
        }

        selectedImageUri = data.getData();
        try {
            startCropViewActivity();
        } catch (Exception e) {
            LoggerUtil.logError("An error occurred", e);
        }
    }

    private void showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Permission Denied");
        builder.setMessage("To pick an image, the app needs permission to access your storage. Please grant the permission in the app settings.");
        builder.setPositiveButton("OK", (dialog, which) -> builder.setCancelable(true));
        builder.setNegativeButton("Grant", (dialog, which) -> redirectToAppSettings());
        builder.show();
    }

    private void redirectToAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void startCropViewActivity() {
        Intent intent = new Intent(requireContext(), CustomCropViewHandler.class);
        intent.putExtra("imageUri", selectedImageUri.toString());
        cropActivityResultLauncher.launch(intent);
    }

    private void setSelectedUserValues() {
        String firstName = ValidationUtils.getStringFromEditText(userFirstNameView);
        if (firstName.isEmpty()) {
            userFirstNameLayoutView.setError("required to fill this field");
            return;
        }
        userFirstNameLayoutView.setError(null);
        String lastName = ValidationUtils.getStringFromEditText(userLastNameView);
        String fullName = firstName + " " + lastName;
        if (ValidationUtils.isValidUsername(fullName)) {
            ValidationUtils.showSnackBar(fillUserDetailFrameView, "Please input valid user Name");
        }
        String userEmail = ValidationUtils.getStringFromEditText(userEmailIdView);
        String userAbout = ValidationUtils.getStringFromEditText(userAboutView);

        Bundle args = getArguments();
        userPhoneNumber = "";
        if (args != null) {
            userPhoneNumber = args.getString("phoneNumber", "");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        Date userDob;
        try {
            userDob = sdf.parse(ValidationUtils.getStringFromEditText(userBirthDateView));
        } catch (Exception e) {
            LoggerUtil.logError("Error parsing date string", e);
            ValidationUtils.showToast(requireContext(), "Error parsing date");
            return;
        }
        final String[] FCMToken = {""};
        FirebaseMessagingUtils.getUserTokenAndSaveToFirestore(new FirebaseMessagingUtils.TokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                // Handle token
                Log.d("FCM Token Received", token);
                // Save token to Firestore or perform other operations
                FCMToken[0] = token;
            }

            @Override
            public void onTokenError(Exception exception) {
                // Handle error
                Log.e("FCM Token Error", Objects.requireNonNull(exception.getMessage()));
                FCMToken[0] = "";
            }
        });


        User user = new User(FirebaseAuthUtils.getCurrentUserId(), fullName, userPhoneNumber, userEmail, userAbout, userDob, Timestamp.now(), Timestamp.now(), Timestamp.now(), UserStatus.ONLINE, FCMToken[0]);

        boolean[] successFlags = {false, false};

        FireStoreDatabaseUtils.getUserDocument(FirebaseAuthUtils.getCurrentUserId()).set(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        successFlags[0] = true;
                        checkAndStartMainActivity(successFlags);
                        NotificationUtils.getUserTokenAndSaveToFirestore();
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

            storageHelper.uploadImage(croppedImageUri, FirebaseAuthUtils.getCurrentUserId(), uploadListener);
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
