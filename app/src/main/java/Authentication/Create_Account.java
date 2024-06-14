package Authentication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.chateasy.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import Activitys.MainActivity;
import Utility.CustomViewUtility;
import Utility.FireStoreDatabaseUtils;
import Utility.FirebaseAuthUtils;
import Utility.NotificationUtils;
import Utility.ValidationUtils;

public class Create_Account extends Fragment {
    private MaterialButton sendOtpAndGoVerify, verificationSend, resendTheOTP;
    private View view;
    private AutoCompleteTextView countryCodeAutoComplete;
    private TextInputLayout countryCodeInputLayout, userPhoneNoInputLayout, otpVerifyInputLayout;
    private TextInputEditText phoneNumberEditText;

    private AutoCompleteTextView sendOtpEditText;
    /*private TextView stopAndChangeNoView;*/

    private ScrollView scrollViewView;
    private Timer timer;
    private String verificationCode;
    private PhoneAuthProvider.ForceResendingToken resendingToken;

    private String formattedPhoneNumber;

    @NonNull
    private static Map<String, String> getStringStringMap() {
        Map<String, String> countryCodesMap = new HashMap<>();
        countryCodesMap.put("United States", "+1");
        countryCodesMap.put("United Kingdom", "+44");
        countryCodesMap.put("Canada", "+1");
        countryCodesMap.put("Australia", "+61");
        countryCodesMap.put("Germany", "+49");
        countryCodesMap.put("France", "+33");
        countryCodesMap.put("Japan", "+81");
        countryCodesMap.put("Brazil", "+55");
        countryCodesMap.put("India", "+91");
        countryCodesMap.put("China", "+86");
        countryCodesMap.put("South Africa", "+27");
        countryCodesMap.put("Russia", "+7");
        countryCodesMap.put("Mexico", "+52");
        countryCodesMap.put("Spain", "+34");
        countryCodesMap.put("Italy", "+39");
        countryCodesMap.put("Argentina", "+54");
        countryCodesMap.put("South Korea", "+82");
        countryCodesMap.put("Nigeria", "+234");
        countryCodesMap.put("Saudi Arabia", "+966");
        countryCodesMap.put("Turkey", "+90");
        return countryCodesMap;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_create__account, container, false);
        initializeViews();
        setCountryCodeAutoComplete();
        setEventListeners();

        return view;
    }

    private void initializeViews() {
        scrollViewView = view.findViewById(R.id.scroll_view);
        verificationSend = view.findViewById(R.id.verifyAndGoNext);
        sendOtpAndGoVerify = view.findViewById(R.id.sendOtpAndGoVerify);
        resendTheOTP = view.findViewById(R.id.resendTheOTP);

        countryCodeAutoComplete = view.findViewById(R.id.countryCode);
        phoneNumberEditText = view.findViewById(R.id.mobileNo);
        sendOtpEditText = view.findViewById(R.id.sendOtp);
        countryCodeInputLayout = view.findViewById(R.id.countryCodeOut);
        userPhoneNoInputLayout = view.findViewById(R.id.userPhoneNo);
        otpVerifyInputLayout = view.findViewById(R.id.otpVerify);
        /*stopAndChangeNoView = view.findViewById(R.id.stopAndChangeNo);*/
    }

    private void setCountryCodeAutoComplete() {
        Map<String, String> countryCodesMap = getStringStringMap();

        List<String> countryNamesList = new ArrayList<>(countryCodesMap.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                countryNamesList
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countryCodeAutoComplete.setAdapter(adapter);

        countryCodeAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCountryName = (String) parent.getItemAtPosition(position);
            String selectedCountryCode = countryCodesMap.get(selectedCountryName);

            countryCodeAutoComplete.setText(selectedCountryCode);

            Toast.makeText(requireContext(), "Selected Country: " + selectedCountryName, Toast.LENGTH_SHORT).show();
        });
    }

    private void setEventListeners() {
        verificationSend.setOnClickListener(v -> onVerificationSendClicked());
        sendOtpAndGoVerify.setOnClickListener(v -> {
            String selectedCountryString = countryCodeAutoComplete.getText().toString();
            String selectedCountryCode = ValidationUtils.extractCountryCode(selectedCountryString);
            String enteredPhoneNumber = phoneNumberEditText.getText() != null ? phoneNumberEditText.getText().toString() : "";

            Runnable onConfirmed = () -> onSendOtpAndGoVerifyClicked(enteredPhoneNumber, selectedCountryCode, false);
            Runnable onEditNumber = () -> {
            };

            formattedPhoneNumber = ValidationUtils.formattedPhoneNumber(enteredPhoneNumber, selectedCountryCode);
            ValidationUtils.showConfirmNumberDialogIfTranslated(
                    requireContext(),
                    R.string.RegistrationActivity_phone_number_verification_dialog_title, // Replace with your title resource ID
                    R.string.RegistrationActivity_a_verification_code_will_be_sent_to_this_number, // Replace with your message resource ID
                    formattedPhoneNumber,
                    onConfirmed,
                    onEditNumber
            );
        });
        resendTheOTP.setOnClickListener(v -> {
            String selectedCountryString = countryCodeAutoComplete.getText().toString();
            String enteredPhoneNumber = phoneNumberEditText.getText() != null ? phoneNumberEditText.getText().toString() : "";
            onSendOtpAndGoVerifyClicked(enteredPhoneNumber, selectedCountryString, true);
            resendTheOTP.setEnabled(false);
            resendTheOTP.setIcon(CustomViewUtility.initializeProgressIndicatorDrawable(requireContext()));
            verificationSend.setEnabled(true);
        });
    }

    private void onVerificationSendClicked() {
        String OTPSendOtp = sendOtpEditText.getText() != null ? sendOtpEditText.getText().toString() : "";

        if (!TextUtils.isEmpty(OTPSendOtp)) {
            otpVerifyInputLayout.setError(null);
            verificationSend.setIcon(CustomViewUtility.initializeProgressIndicatorDrawable(requireContext()));
            FirebaseAuthUtils.verifyPhoneNumberWithCode(OTPSendOtp, verificationCode, new FirebaseAuthUtils.VerificationCallback() {
                @Override
                public void onVerificationCompleted(FirebaseUser user) {
                    timer.cancel();
                    // Check if the user is already present in FireStore
                    if (user != null) {
                        checkUserInFireStore(FirebaseAuthUtils.getCurrentUserId());
                        NotificationUtils.getUserTokenAndSaveToFirestore();
                    } else {
                        // Handle the case where user is not authenticated (should not happen in this flow)
                        ValidationUtils.showToast(requireContext(), "Authentication failed");
                    }
                }

                @Override
                public void onVerificationFailed(Exception exception) {
                    verificationSend.setIcon(null);
                    ValidationUtils.showToast(requireContext(), "Verification failed: " + exception.getMessage());
                }
            });
        } else {
            verificationSend.setIcon(null);
            otpVerifyInputLayout.setError("Enter the Valid OTP");
            Toast.makeText(requireContext(), "Invalid OTP Entered", Toast.LENGTH_SHORT).show();
        }
    }

    private void onSendOtpAndGoVerifyClicked(String phoneNumber, String countryCode, boolean isResend) {
        if (ValidationUtils.isValidPhoneNumber(phoneNumber)) {
            formattedPhoneNumber = ValidationUtils.formattedPhoneNumber(phoneNumber, countryCode);
            if (isResend) {
                resendTheOTP.setIcon(CustomViewUtility.initializeProgressIndicatorDrawable(requireContext()));
                sendOtpAndGoVerify.setEnabled(false);
                userPhoneNoInputLayout.setEnabled(false);
                countryCodeInputLayout.setEnabled(false);
                FirebaseAuthUtils.resendVerificationCode(requireActivity(), formattedPhoneNumber, resendingToken, new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Handle verification completion if needed
                        ValidationUtils.showSnackBar(scrollViewView, "Verification Completed Successfully");
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException exception) {
                        ValidationUtils.showToast(requireContext(), /*"Resend OTP Failed" + */exception.getMessage());
                        Log.e("Resend OTP Failed: ", Objects.requireNonNull(exception.getMessage()));
                        userPhoneNoInputLayout.setEnabled(true);
                        countryCodeInputLayout.setEnabled(true);
                        sendOtpAndGoVerify.setEnabled(true);
                        sendOtpAndGoVerify.setIcon(null);
                        verificationSend.setIcon(null);
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        verificationCode = s;
                        resendingToken = forceResendingToken;
                        resendTheOTP.setIcon(null);
                        long[] timeoutSeconds = {60L};
                        startOtpTimer(timeoutSeconds);
                        ValidationUtils.showToast(requireContext(), "OTP Resent Successfully");
                    }
                });
            } else {
                sendOtpAndGoVerify.setIcon(CustomViewUtility.initializeProgressIndicatorDrawable(requireContext()));
                sendOtpAndGoVerify.setEnabled(false);
                userPhoneNoInputLayout.setEnabled(false);
                countryCodeInputLayout.setEnabled(false);
                FirebaseAuthUtils.sendVerificationCode(requireActivity(), formattedPhoneNumber, new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Handle verification completion if needed
                        ValidationUtils.showSnackBar(scrollViewView, "Verification Completed Successfully");
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException exception) {
                        ValidationUtils.showToast(requireContext(), /*"OTP Sent Failed Please try again :" + */exception.getMessage());
                        sendOtpAndGoVerify.setIcon(null);
                        userPhoneNoInputLayout.setEnabled(true);
                        countryCodeInputLayout.setEnabled(true);
                        sendOtpAndGoVerify.setEnabled(true);
                        Log.e("OTP Sent Failed Please try again :", Objects.requireNonNull(exception.getMessage()));
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        verificationCode = s;
                        resendingToken = forceResendingToken;

                        sendOtpAndGoVerify.setIcon(null);
                        long[] timeoutSeconds = {60L};
                        startOtpTimer(timeoutSeconds);
                        ValidationUtils.showToast(requireContext(), "OTP Sent Successfully");
                        handleValidPhoneNumber();
                    }
                });
            }
        } else {
            handlePhoneNumberError(countryCode, phoneNumber);
        }
    }

    private void handleValidPhoneNumber() {
        countryCodeInputLayout.setError(null);
        userPhoneNoInputLayout.setError(null);

        otpVerifyInputLayout.setEnabled(true);
        verificationSend.setEnabled(true);

       /* stopAndChangeNoView.setOnClickListener(v -> {
            userPhoneNoInputLayout.setEnabled(true);
            countryCodeInputLayout.setEnabled(true);
            sendOtpAndGoVerify.setEnabled(true);
            sendOtpAndGoVerify.setIcon(null);
            verificationSend.setIcon(null);

            stopAndChangeNoView.setEnabled(false);
            timer.cancel();
            otpVerifyInputLayout.setHelperText(null);
            resendTheOTP.setEnabled(false);
            resendTheOTP.setIcon(null);
            otpVerifyInputLayout.setEnabled(false);
            sendOtpEditText.setText(null);
            verificationSend.setEnabled(false);
        });*/
    }

    private void startOtpTimer(long[] timeoutSeconds) {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            otpVerifyInputLayout.setHelperText(
                                    String.format(Locale.US, "Resend OTP ( 00:%02d )", timeoutSeconds[0]));
                        }
                    });

                    timeoutSeconds[0]--;

                    if (timeoutSeconds[0] <= -1) {
                        timer.cancel();
                        handleTimerCompletion();
                    }
                } else {
                    timer.cancel();
                }
            }
        }, 0, 1000);
    }


    private void handleTimerCompletion() {
        requireActivity().runOnUiThread(() -> {
            otpVerifyInputLayout.setHelperText("OTP Will Expire, Resend It");
            resendTheOTP.setEnabled(true);
            verificationSend.setEnabled(false);
        });
    }

    private void replaceFragment(Fragment fragment, String phoneNumber) {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        Bundle bundle = new Bundle();
        bundle.putString("phoneNumber", phoneNumber);
        fragment.setArguments(bundle);

        fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        fragmentTransaction.replace(R.id.allForUserCreation, fragment);
        fragmentTransaction.commit();
    }

    private void handlePhoneNumberError(String selectedCountryCode, String enteredPhoneNumber) {
        if (TextUtils.isEmpty(selectedCountryCode)) {
            countryCodeInputLayout.setError("Enter The Country Code");
        } else {
            countryCodeInputLayout.setError(null);
        }
        if (TextUtils.isEmpty(enteredPhoneNumber)) {
            userPhoneNoInputLayout.setError("Enter The Phone number");
        } else {
            userPhoneNoInputLayout.setError(null);
        }
    }

    private void checkUserInFireStore(String uid) {
        FireStoreDatabaseUtils.getUserDocument(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // User is already registered, proceed to MainActivity
                    verificationSend.setIcon(null);
                    Log.e("number form ::: ", formattedPhoneNumber + "  from document...");
                    FirebaseAuthUtils.setLoggedIn(true, FirebaseAuthUtils.getCurrentUserId(), formattedPhoneNumber, true, requireContext());
                    /*FirebaseAuthUtils.setRegistered(true, FirebaseAuthUtils.getCurrentUserId(), requireContext());*/
                    startActivity(new Intent(requireActivity(), MainActivity.class));
                    requireActivity().finish();
                } else {
                    verificationSend.setIcon(null);
                    Log.e("number form ::: ", formattedPhoneNumber + "  from FillDetail...");
                    // User is not registered, move to Fill_User_Details fragment
                    FirebaseAuthUtils.setLoggedIn(true, FirebaseAuthUtils.getCurrentUserId(), formattedPhoneNumber, false, requireContext());
                    String enteredPhoneNumber = phoneNumberEditText.getText() != null ? phoneNumberEditText.getText().toString() : "";
                    replaceFragment(new Fill_User_Details(), FirebaseAuthUtils.getUserIdPhoneNumber(requireContext()));
                }
            } else {
                verificationSend.setIcon(null);
                // Handle errors while checking FireStore
                ValidationUtils.showToast(requireContext(), /*"FireStore check failed: " + */Objects.requireNonNull(task.getException()).getMessage());
            }
        });
    }
}
