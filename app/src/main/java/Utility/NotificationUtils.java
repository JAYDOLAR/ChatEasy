package Utility;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

public class NotificationUtils {
    public static void getUserTokenAndSaveToFirestore() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d("FCM Token", token != null ? token : "Token is null");
                        saveTokenToFirestore(token);
                    } else {
                        Log.e("FCM Token", "Failed to get token", task.getException());
                    }
                });
    }

    public static void saveTokenToFirestore(String token) {
        String userId = FirebaseAuthUtils.getCurrentUser().getUid();
        if (token != null) {
            // Save the FCM token to Firestore
            FireStoreDatabaseUtils.getUsersCollection().document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "FCM token saved successfully"))
                    .addOnFailureListener(e -> Log.e("Firestore", "Error saving FCM token", e));
        }
    }
}
