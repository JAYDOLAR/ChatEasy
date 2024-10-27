package Utility;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;

import Models.Feedback;

public class FeedbackEmailSender {
    private static final String TAG = "FeedbackEmailSender";
    private static final String[] SUPPORT_EMAIL = {"meg353020@gmail.com"};
    private static final String EMAIL_SUBJECT = "ChatEasy App Feedback";
    private static final String CHOOSER_TITLE = "Send feedback via:";

    private final WeakReference<Context> contextRef;
    private final Feedback feedback;


    public FeedbackEmailSender(@NonNull Context context, @NonNull Feedback feedback) {
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(feedback, "Feedback cannot be null");

        this.contextRef = new WeakReference<>(context.getApplicationContext());
        this.feedback = feedback;
    }


    public boolean sendFeedback() {
        Context context = contextRef.get();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return false;
        }

        try {
            Intent emailIntent = createEmailIntent(context);
            if (isEmailAppAvailable(context, emailIntent)) {
                launchEmailChooser(context, emailIntent);
                return true;
            } else {
                showError(context, "No email app found");
                return false;
            }
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            Log.e(TAG, "Error sending feedback: " + errorMessage, e);
            showError(context, "Error sending feedback: " +
                    (errorMessage != null ? errorMessage : "Unknown error"));
            return false;
        }
    }


    @NonNull
    private Intent createEmailIntent(@NonNull Context context) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, SUPPORT_EMAIL);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, EMAIL_SUBJECT);

        String htmlContent = FeedbackEmailTemplate.createEmailContent(feedback);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            emailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(htmlContent,
                    Html.FROM_HTML_MODE_LEGACY));
        } else {
            @SuppressWarnings("deprecation")
            CharSequence sequence = Html.fromHtml(htmlContent);
            emailIntent.putExtra(Intent.EXTRA_TEXT, sequence);
        }

        return emailIntent;
    }

    private boolean isEmailAppAvailable(@NonNull Context context, @NonNull Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        return intent.resolveActivity(packageManager) != null;
    }


    private void launchEmailChooser(@NonNull Context context, @NonNull Intent emailIntent) {
        Intent chooserIntent = Intent.createChooser(emailIntent, CHOOSER_TITLE);
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooserIntent);
    }


    private void showError(@Nullable Context context, @NonNull String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
        Log.e(TAG, message);
    }
}