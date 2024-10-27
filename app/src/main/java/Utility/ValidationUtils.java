package Utility;

import android.content.Context;
import android.content.DialogInterface;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.chateasy.R;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.jetbrains.annotations.Contract;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class ValidationUtils {
    @Contract("null -> false")
    public static boolean isValidPhoneNumber(String phoneNumber) {
        // Validate that the phone number contains only digits and has a reasonable length
        return phoneNumber != null && phoneNumber.matches("\\d{10,14}");
    }

    @NonNull
    public static String extractCountryCode(@NonNull String countryString) {
        return countryString.replaceAll("[^+0-9]", "");
    }

    @NonNull
    public static String getStringFromEditText(TextInputEditText editText) {
        return editText != null ? Objects.requireNonNull(editText.getText()).toString().trim() : "";
    }


    @NonNull
    public static String formattedPhoneNumber(@NonNull String phoneNumber, String countryCode) {
        StringBuilder sb = new StringBuilder();
        if (!phoneNumber.isEmpty() && !countryCode.isEmpty() && isValidPhoneNumber(phoneNumber)) {
            sb.append(countryCode).append(phoneNumber);
        }
        return sb.toString();
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showSnackBar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    public static void showSnackBarWithAction(View view, String message, String actionText, View.OnClickListener actionListener) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).setAction(actionText, actionListener).show();
    }


    public static void replaceFragment(Fragment fragment, @NonNull FragmentActivity requireActivity) {
        FragmentManager fragmentManager = requireActivity.getSupportFragmentManager();
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        fragmentTransaction.replace(R.id.fragment_section_setting_edit, fragment);
        fragmentTransaction.commit();
    }

    public static void showLogoutDialog(Context context, DialogInterface.OnClickListener positiveClickListener) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Logout", positiveClickListener);
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> {
            // Do nothing, dismiss dialog
            dialogInterface.dismiss();
        });
        builder.show();
    }

    public static void showConfirmNumberDialogIfTranslated(
            Context context,
            @StringRes Integer title,
            @StringRes Integer firstMessageLine,
            String e164number,
            Runnable onConfirmed,
            Runnable onEditNumber
    ) {
        SpannableStringBuilder message = new SpannableStringBuilder();
        message.append(e164number);
        if (firstMessageLine != null) {
            message.append("\n\n");
            message.append(context.getString(firstMessageLine));
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle(title != null ? title : 0)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> onConfirmed.run())
                .setNegativeButton(R.string.RegistrationActivity_edit_number, (dialog, which) -> onEditNumber.run())
                .setOnCancelListener(dialog -> onEditNumber.run())
                .show();
    }

    public static void showDatePicker(FragmentManager fragmentManager, String title, TextInputEditText textView, String dateFormat) {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(title)
                .build();

        datePicker.show(fragmentManager, "DatePicker");

        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                long selectedDate = selection;
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.US);
                    String formattedDate = sdf.format(new Date(selectedDate));
                    textView.setText(formattedDate);
                } catch (Exception e) {
                    LoggerUtil.logError("An error occurred", e);
                }
            }
        });
    }
}
