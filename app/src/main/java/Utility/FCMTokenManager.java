package Utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class FCMTokenManager {
    private static final String TAG = "FCMTokenManager";
    private static final String PREF_NAME = "FCMPrefs";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String USERS_COLLECTION = "users"; // Firestore collection for users

    private static FCMTokenManager instance;
    private final SharedPreferences prefs;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FCMTokenManager(@NonNull Context context) {
        Context context1 = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public static synchronized FCMTokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new FCMTokenManager(context);
        }
        return instance;
    }

    /**
     * Get the current FCM token
     */
    public void getToken(TokenCallback callback) {
        // First check cache
        String cachedToken = prefs.getString(KEY_FCM_TOKEN, null);

        // Get fresh token from Firebase
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();

                        // If token has changed, update it
                        if (cachedToken == null || !cachedToken.equals(token)) {
                            saveToken(token);
                            updateTokenInFirestore(token);
                        }

                        callback.onTokenReceived(token);
                    } else {
                        Log.e(TAG, "Failed to get FCM token", task.getException());
                        callback.onTokenError("Failed to get token: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    /**
     * Update token when a new one is received
     */
    public void updateToken(String token) {
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Attempted to update with null or empty token");
            return;
        }

        saveToken(token);
        updateTokenInFirestore(token);
    }

    /**
     * Delete token when user logs out
     */
    public void deleteToken() {
        FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Clear from SharedPreferences
                        prefs.edit().remove(KEY_FCM_TOKEN).apply();

                        // Remove from Firestore if user is authenticated
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            removeTokenFromFirestore(user.getUid());
                        }

                        Log.d(TAG, "FCM token deleted successfully");
                    } else {
                        Log.e(TAG, "Failed to delete FCM token", task.getException());
                    }
                });
    }

    private void saveToken(String token) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
    }

    /**
     * Update token in Firestore user document
     */
    private void updateTokenInFirestore(String token) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.d(TAG, "No user logged in, skipping token update in Firestore");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", token);
        updates.put("lastTokenUpdate", System.currentTimeMillis());
        updates.put("deviceInfo", getDeviceInfo());

        db.collection(USERS_COLLECTION)
                .document(user.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Token successfully updated in Firestore"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error updating token in Firestore", e));
    }

    /**
     * Remove token from Firestore user document
     */
    private void removeTokenFromFirestore(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", null);
        updates.put("lastTokenUpdate", null);
        updates.put("deviceInfo", null);

        db.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Token successfully removed from Firestore"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error removing token from Firestore", e));
    }

    /**
     * Get basic device information
     */
    @NonNull
    private Map<String, String> getDeviceInfo() {
        Map<String, String> deviceInfo = new HashMap<>();
        deviceInfo.put("manufacturer", android.os.Build.MANUFACTURER);
        deviceInfo.put("model", android.os.Build.MODEL);
        deviceInfo.put("osVersion", String.valueOf(android.os.Build.VERSION.SDK_INT));
        return deviceInfo;
    }

    public interface TokenCallback {
        void onTokenReceived(String token);

        void onTokenError(String error);
    }
}