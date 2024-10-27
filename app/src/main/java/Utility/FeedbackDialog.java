package Utility;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.chateasy.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

import Models.Feedback;

public class FeedbackDialog extends DialogFragment {
    private RatingBar ratingBar;
    private TextInputEditText feedbackInput;
    private TextInputEditText emailInput;
    private MaterialCheckBox anonymousCheckBox;
    private MaterialButton submitButton;
    private MaterialButton cancelButton;
    private OnFeedbackSubmitListener listener;
    private CircularProgressIndicator progressBar;
    private View contentContainer;

    public interface OnFeedbackSubmitListener {
        void onFeedbackSubmit(Feedback feedback, boolean success);
    }

    public static FeedbackDialog newInstance() {
        return new FeedbackDialog();
    }

    public void setOnFeedbackSubmitListener(OnFeedbackSubmitListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_feedback, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupDialogProperties();
        setupListeners();
        updateSubmitButtonState();
    }

    private void initializeViews(@NonNull View view) {
        ratingBar = view.findViewById(R.id.rating_bar);
        feedbackInput = view.findViewById(R.id.feedback_input);
        emailInput = view.findViewById(R.id.email_input);
        anonymousCheckBox = view.findViewById(R.id.anonymous_checkbox);
        submitButton = view.findViewById(R.id.submit_button);
        cancelButton = view.findViewById(R.id.cancel_button);
        progressBar = view.findViewById(R.id.progress_bar);
        contentContainer = view.findViewById(R.id.content_container);
    }

    private void setupDialogProperties() {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            getDialog().setCancelable(false);
        }
    }

    private void setupListeners() {
        // Anonymous checkbox listener
        anonymousCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            emailInput.setEnabled(!isChecked);
            if (isChecked) {
                emailInput.setText("");
                emailInput.clearFocus();
            }
        });

        // Button listeners
        cancelButton.setOnClickListener(v -> dismiss());
        submitButton.setOnClickListener(v -> handleSubmission());

        // Text change listener
        feedbackInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSubmitButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void handleSubmission() {
        if (validateInput()) {
            showLoading(true);

            Feedback feedback = new Feedback(
                    ratingBar.getRating(),
                    Objects.requireNonNull(feedbackInput.getText()).toString().trim(),
                    anonymousCheckBox.isChecked() ? "" : Objects.requireNonNull(emailInput.getText()).toString().trim(),
                    anonymousCheckBox.isChecked()
            );

            FeedbackEmailSender sender = new FeedbackEmailSender(requireContext(), feedback);
            boolean success = sender.sendFeedback();

            if (listener != null) {
                listener.onFeedbackSubmit(feedback, success);
            }

            showLoading(false);
            if (success) {
                dismiss();
            }
        }
    }

    private boolean validateInput() {
        boolean isValid = true;

        // Validate rating
        if (ratingBar.getRating() == 0) {
            showError();
            isValid = false;
        }

        // Validate feedback text
        String feedback = Objects.requireNonNull(feedbackInput.getText()).toString().trim();
        if (feedback.isEmpty()) {
            feedbackInput.setError("Please provide your feedback");
            isValid = false;
        }

        // Validate email if not anonymous and provided
        if (!anonymousCheckBox.isChecked() && !Objects.requireNonNull(emailInput.getText()).toString().trim().isEmpty()) {
            String email = emailInput.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.setError("Please enter a valid email address");
                isValid = false;
            }
        }

        return isValid;
    }

    private void updateSubmitButtonState() {
        boolean hasText = !Objects.requireNonNull(feedbackInput.getText()).toString().trim().isEmpty();
        submitButton.setEnabled(hasText);
        submitButton.setAlpha(hasText ? 1.0f : 0.5f);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        contentContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        submitButton.setEnabled(!show);
        cancelButton.setEnabled(!show);
    }

    private void showError() {
        Toast.makeText(requireContext(), "Please provide a rating", Toast.LENGTH_SHORT).show();
    }
}