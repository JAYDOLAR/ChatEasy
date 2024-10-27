package Utility;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.chateasy.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import Authentication.Create_UserOrEnter;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "chat_channel";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            // Display notification
            sendNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody(), null, null);
        }

        // Check if message contains data payload
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            // Process message data
            handleDataMessage(remoteMessage.getData());
        }
    }

    private void handleDataMessage(@NonNull Map<String, String> data) {
        String senderName = data.get("senderName");
        String message = data.get("message");
        String chatRoomId = data.get("chatRoomId");
        String senderId = data.get("senderId");

        Log.d(TAG, "Message from: " + senderName + ", ChatRoomId: " + chatRoomId);
        sendNotification(senderName, message, senderId, chatRoomId);
    }

    private void sendNotification(String title, String messageBody, String senderId, String chatRoomId) {
        Intent intent = new Intent(this, Create_UserOrEnter.class);
        intent.putExtra("chatRoomId", chatRoomId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)  // Default icon in case image loading fails
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Chat Notifications", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        if (senderId != null) {
            loadProfilePictureAndShowNotification(senderId, notificationBuilder, notificationManager);
        } else {
            notificationManager.notify(0, notificationBuilder.build());
        }
    }

    private void loadProfilePictureAndShowNotification(String userId, NotificationCompat.Builder notificationBuilder, NotificationManager notificationManager) {
        // Fetch the user's profile picture URL from Firebase
        FireStoreDatabaseUtils.getUserData(userId, (snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error fetching user data", error);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                // Load the image asynchronously using Glide
                Glide.with(this)
                        .asBitmap()
                        .load(userId)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                                // Set the large icon for the notification
                                notificationBuilder.setLargeIcon(resource);
                                notificationManager.notify(0, notificationBuilder.build());
                            }

                            @Override
                            public void onLoadCleared(Drawable placeholder) {
                                // Handle the case where the image is being cleared
                            }

                            @Override
                            public void onLoadFailed(Drawable errorDrawable) {
                                // Use the default icon if loading fails
                                Log.e(TAG, "Error loading profile image");
                                notificationManager.notify(0, notificationBuilder.build());
                            }
                        });
            }
        });
    }
}
