package Utility;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.chateasy.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import Authentication.Create_UserOrEnter;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "chatEasy_channel";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains data payload
        remoteMessage.getData();
        if (!remoteMessage.getData().isEmpty()) {
            // Handle data payload
            String senderName = remoteMessage.getData().get("senderName");
            String message = remoteMessage.getData().get("message");
            String chatRoomId = remoteMessage.getData().get("chatRoomId");

            // Display notification
            Log.e("MyFirebaseMessagingService", senderName + "--" + message + "--" + chatRoomId);
            showNotification(senderName, message, chatRoomId);
        }
    }

    @SuppressLint("MissingPermission")
    private void showNotification(String senderName, String message, String chatRoomId) {
        // Create a notification channel (for Android Oreo and higher)
        createNotificationChannel();

        // Create intent to open Create_UserOrEnter activity with chatRoomId
        Intent intent = new Intent(this, Create_UserOrEnter.class);
        intent.putExtra("chatRoomId", chatRoomId);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

/*
        startActivity(new Intent(this, MainActivity.class));
*/

        // Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(senderName)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent) // Set the PendingIntent
                .setAutoCancel(true); // Dismiss the notification when clicked

        // Show notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(0, builder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because the NotificationChannel class is new and not in the support library
        CharSequence name = getString(R.string.channel_name);
        String description = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        // Register the channel with the system
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
}



/*
package Utility;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.chateasy.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import Authentication.Create_UserOrEnter;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "default_channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Inside onMessageReceived() method
        if (!remoteMessage.getData().isEmpty()) {
            // Handle data payload.
            Map<String, String> map = remoteMessage.getData();
            String title = map.get("senderName");
            String messageBody = map.get("message");
            String chatRoomId = map.get("chatRoomId");
            sendNotification(title, messageBody, chatRoomId);
            // You can handle the data payload as needed.
        }

    }

    @Override
    public void onNewToken(@NonNull String s) {
        saveTokenToFirestore(s);
        super.onNewToken(s);
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

    private void sendNotification(String title, String messageBody, String chatId) {
        Intent intent = new Intent(this, Create_UserOrEnter.class);
        intent.putExtra("chatRoomId", chatId); // Pass the chat ID as an extra
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel);

        notificationManager.notify(0, notificationBuilder.build());
    }
}
*/
