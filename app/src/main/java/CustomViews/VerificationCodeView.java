package CustomViews;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.example.chateasy.R;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VerificationCodeView extends FrameLayout {
    private final List<TextInputLayout> containers = new ArrayList<>(6);
    private final PasteTextWatcher textWatcher = new PasteTextWatcher();
    private OnCodeEnteredListener listener;
    private int index = 0;

    public VerificationCodeView(Context context) {
        this(context, null);
    }

    public VerificationCodeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerificationCodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public VerificationCodeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.verification_code_view, this);

        containers.add(findViewById(R.id.container_zero));
        containers.add(findViewById(R.id.container_one));
        containers.add(findViewById(R.id.container_two));
        containers.add(findViewById(R.id.container_three));
        containers.add(findViewById(R.id.container_four));
        containers.add(findViewById(R.id.container_five));

        for (TextInputLayout container : containers) {
            if (container.getEditText() != null) {
                container.getEditText().setShowSoftInputOnFocus(false);
                container.getEditText().addTextChangedListener(textWatcher);
            }
        }
    }

    public void setOnCompleteListener(OnCodeEnteredListener listener) {
        this.listener = listener;
    }

    public void append(int digit) {
        if (index >= containers.size()) return;
        Objects.requireNonNull(containers.get(index++).getEditText()).setText(String.valueOf(digit));

        if (index == containers.size()) {
            if (listener != null) {
                StringBuilder codeBuilder = new StringBuilder();
                for (TextInputLayout container : containers) {
                    if (container.getEditText() != null) {
                        codeBuilder.append(container.getEditText().getText());
                    }
                }
                listener.onCodeComplete(codeBuilder.toString());
            }
            return;
        }

        if (containers.get(index).getEditText() != null) {
            Objects.requireNonNull(containers.get(index).getEditText()).requestFocus();
        }
    }

    public void delete() {
        if (index < 0) return;
        String text = "";
        if (index == 0) {
            text = Objects.requireNonNull(containers.get(index).getEditText()).getText().toString();
        } else {
            index--;
            text = Objects.requireNonNull(containers.get(index).getEditText()).getText().toString();
        }
        Objects.requireNonNull(containers.get(index).getEditText()).setText("");
        if (containers.get(index).getEditText() != null) {
            Objects.requireNonNull(containers.get(index).getEditText()).requestFocus();
        }
    }

    public void clear() {
        if (index != 0) {
            for (TextInputLayout container : containers) {
                if (container.getEditText() != null) {
                    container.getEditText().setText("");
                }
            }
            index = 0;
            if (containers.get(index).getEditText() != null) {
                Objects.requireNonNull(containers.get(index).getEditText()).requestFocus();
            }
        }
    }

    public interface OnCodeEnteredListener {
        void onCodeComplete(String code);
    }

    private class PasteTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s == null) {
                return;
            }

            if (s.length() > 1) {
                char[] enteredText = s.toString().toCharArray();
                for (char c : enteredText) {
                    int castInt = Character.getNumericValue(c);
                    if (castInt == -1) {
                        s.clear();
                        return;
                    } else {
                        append(castInt);
                    }
                }
            }
        }
    }
}
