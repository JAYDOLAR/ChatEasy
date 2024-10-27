package Models;

import android.os.Build;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Feedback {
    private float rating;
    private String feedbackText;
    private String userEmail;
    private boolean isAnonymous;
    private String deviceInfo;
    private String timestamp;

    public Feedback(float rating, String feedbackText, String userEmail, boolean isAnonymous) {
        this.rating = rating;
        this.feedbackText = feedbackText;
        this.userEmail = userEmail;
        this.isAnonymous = isAnonymous;
        this.deviceInfo = generateDeviceInfo();
        this.timestamp = generateTimestamp();
    }

    @NonNull
    private String generateDeviceInfo() {
        return String.format(Locale.getDefault(),
                "Device: %s\nModel: %s\nAndroid Version: %s\nAPI Level: %d",
                Build.DEVICE,
                Build.MODEL,
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT);
    }

    @NonNull
    private String generateTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Getters
    public float getRating() { return rating; }
    public String getFeedbackText() { return feedbackText; }
    public String getUserEmail() { return userEmail; }
    public boolean isAnonymous() { return isAnonymous; }
    public String getDeviceInfo() { return deviceInfo; }
    public String getTimestamp() { return timestamp; }
}