package CustomViews;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.example.chateasy.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class OTPInputView extends LinearLayout {

    private final int otpLength = 4; // You can change this based on your OTP length
    private final List<EditText> editTextList = new ArrayList<>();

    public OTPInputView(Context context) {
        super(context);
        init();
    }

    public OTPInputView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OTPInputView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);

        for (int i = 0; i < otpLength; i++) {
            TextInputLayout textInputLayout = new TextInputLayout(getContext());
            textInputLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            textInputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);

            TextInputEditText editText = new TextInputEditText(getContext());

            editText.setLayoutParams(new TextInputLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            editText.setWidth(getResources().getDimensionPixelSize(R.dimen.edit_text_width)); // Use dimension resource
            editText.setGravity(Gravity.CENTER);
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setMaxLines(1);
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});

            textInputLayout.addView(editText);
            addView(textInputLayout);

            int finalI = i;
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                    handleTextChange(finalI, count);
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            editTextList.add(editText);
        }
    }

    private void handleTextChange(int position, int count) {
        if (count == 1 && position < otpLength - 1) {
            editTextList.get(position + 1).requestFocus();
        } else if (count == 0 && position > 0) {
            editTextList.get(position - 1).requestFocus();
        }
    }

    // Method to get the full OTP from user input
    public String getOTP() {
        StringBuilder otpBuilder = new StringBuilder(otpLength);
        for (EditText editText : editTextList) {
            otpBuilder.append(editText.getText().toString());
        }
        return otpBuilder.toString();
    }
}
