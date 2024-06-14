package Utility;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.example.chateasy.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.jetbrains.annotations.Contract;

import java.util.Date;
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

    @Contract("null -> false")
    public static boolean isValidEmail(String email) {
        // Validate using the built-in Android Patterns class
        return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @Contract(value = "null -> false", pure = true)
    public static boolean isValidPassword(String password) {
        // Validate that the password has a minimum length of 6 characters
        return password != null && password.length() >= 6;
    }

    @Contract("null -> false")
    public static boolean isValidUsername(String username) {
        // Validate that the username is not empty
        return username != null && !username.trim().isEmpty();
    }

    @NonNull
    public static String getStringFromEditText(TextInputEditText editText) {
        return editText != null ? Objects.requireNonNull(editText.getText()).toString().trim() : "";
    }

    @Contract("null -> false")
    public static boolean isValidMessageContent(String messageContent) {
        // Validate that the message content is not empty after trimming whitespace
        return messageContent != null && !messageContent.trim().isEmpty();
    }

    @Contract("null -> false")
    public static boolean isValidImageUrl(String imageUrl) {
        // Validate that the image URL starts with "http" or "https"
        return imageUrl != null && (imageUrl.startsWith("http") || imageUrl.startsWith("https"));
    }

    @Contract("null -> false")
    public static boolean isValidDateOfBirth(Date dob) {
        // Validate that the date of birth is not in the future
        return dob != null && dob.before(new Date());
    }

    @Contract(value = "null -> false; !null -> true", pure = true)
    public static boolean isValidProfileImage(Uri profileImage) {
        // Validate that the profile image URI is not null
        return profileImage != null;
    }

    @Contract("null -> false")
    public static boolean isValidChatId(String chatId) {
        // Validate that the chat ID is not null or empty
        return chatId != null && !chatId.trim().isEmpty();
    }

/*    public static boolean isValidChatMessage(MessageModel message) {
        // Validate that the message is not null and has valid content
        return message != null && isValidMessageContent(message.getContent());
    }*/

    @Contract("null -> false")
    public static boolean isValidOtp(String otp) {
        // Validate that the OTP is a 6-digit numeric code
        return otp != null && otp.matches("\\d{6}");
    }

    @NonNull
    public static String formattedPhoneNumber(@NonNull String phoneNumber, String countryCode) {
        StringBuilder sb = new StringBuilder();
        if (!phoneNumber.isEmpty() && !countryCode.isEmpty() && isValidPhoneNumber(phoneNumber)) {
            sb.append(countryCode).append(phoneNumber);
        }
        return sb.toString();
    }


/*    public static boolean isValidUser(User user, UserProfilePicModel userPic) {
        // Validate a User object, ensuring that essential fields are not null or empty
        return user != null &&
                isValidEmail(user.getEmail()) &&
                isValidDateOfBirth(user.getDob()) &&
                isValidMessageContent(user.getAbout()) &&
                isValidProfileImage(userPic.getProfileImage());
    }

    public static boolean isValidChatParticipants(String participant1Id, String participant2Id) {
        // Validate that the participant IDs are not null or empty and are different
        return isValidUserId(participant1Id) &&
                isValidUserId(participant2Id) &&
                !participant1Id.equals(participant2Id);
    }*/

    @Contract("null -> false")
    public static boolean isValidUserId(String userId) {
        // Validate that the user ID is not null or empty
        return userId != null && !userId.trim().isEmpty();
    }

/*    public static boolean isValidChatModel(ChatModel chatModel) {
        // Validate a ChatModel, ensuring that essential fields are not null or empty
        return chatModel != null &&
                isValidChatId(chatModel.getChatId()) &&
                isValidUserId(chatModel.getParticipant1Id()) &&
                isValidUserId(chatModel.getParticipant2Id()) &&
                isValidChatMessage(chatModel.getLastMessage());
    }*/

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showSnackBar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    public static void showSnackBarWithAction(View view, String message, String actionText, View.OnClickListener actionListener) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).setAction(actionText, actionListener).show();
    }

    /*@Nullable
    public static Activity getActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }*/

    public static void showAlertDialog(Context context, String title, String message) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public static void showInfoDialog(Context context, String title, String message) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.symbol_info_24)
                .setPositiveButton(android.R.string.ok, null)
                .show();
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
}
