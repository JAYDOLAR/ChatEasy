package Utility;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import Activitys.ChatRoom;

public class NotificationUtils {
    public static void getUserTokenAndSaveToFirestore() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d("FCM Token", token != null ? token : "Token is null");
                        saveTokenToFirestore(token).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                            }
                        });
                    } else {
                        Log.e("FCM Token", "Failed to get token", task.getException());
                    }
                });
    }

    public static Task<Void> saveTokenToFirestore(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();

            if (token != null) {
                // Save the FCM token to Firestore
                return FireStoreDatabaseUtils.getUsersCollection().document(userId)
                        .update("fcmToken", token);
            } else {
                Log.e("NotificationUtils", "Token is null, cannot save to Firestore");
            }
        } else {
            Log.e("NotificationUtils", "User not signed in, cannot save FCM token");
        }

        return Tasks.forException(new Exception("Failed to save token to Firestore"));
    }


    public static void handleNotificationClick(Activity activity, @NonNull Bundle extras) {
        String chatId = extras.getString("chatRoomId");
        if (chatId != null) {
            Intent intent = new Intent(activity, ChatRoom.class);
            intent.putExtra("chatRoomId", chatId);
            activity.startActivity(intent);
            activity.finish();
        }
    }
}
