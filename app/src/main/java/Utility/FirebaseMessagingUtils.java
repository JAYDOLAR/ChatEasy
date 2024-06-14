package Utility;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

public class FirebaseMessagingUtils {

    public static void getUserTokenAndSaveToFirestore(TokenCallback callback) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d("FCM Token", token != null ? token : "Token is null");
                        if (callback != null) {
                            callback.onTokenReceived(token);
                        }
                    } else {
                        Log.e("FCM Token", "Failed to get token", task.getException());
                        if (callback != null) {
                            callback.onTokenError(task.getException());
                        }
                    }
                });
    }

    public interface TokenCallback {
        void onTokenReceived(String token);

        void onTokenError(Exception exception);
    }
}
