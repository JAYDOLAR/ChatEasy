package Utility;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.chateasy.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.jetbrains.annotations.Contract;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import Activitys.ChatRoom;

public class FCMMessageReceiver extends FirebaseMessagingService {
    private static final String TAG = "FCMMessageReceiver";

    // Constants for notification channel
    public static final String CHANNEL_ID = "chat_messages";
    public static final String CHANNEL_NAME = "Chat Messages";
    public static final String CHANNEL_DESCRIPTION = "Notifications for new chat messages";

    // Constants for intent actions and extras
    /*public static final String ACTION_MESSAGE_RECEIVED = "com.your.package.MESSAGE_RECEIVED";
    public static final String ACTION_TOKEN_REFRESH = "com.your.package.TOKEN_REFRESH";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_SENDER_ID = "sender_id";
    public static final String EXTRA_SENDER_NAME = "sender_name";
    public static final String EXTRA_TIMESTAMP = "timestamp";*/
    public static final String EXTRA_CHAT_ROOM_ID = "chatRoomId";
    // For generating unique notification IDs
    private final AtomicInteger notificationId = new AtomicInteger(0);

    // Token refresh callback interface
    public interface OnTokenRefreshListener {
        void onTokenRefreshed(@NonNull String newToken);
    }

    private static OnTokenRefreshListener tokenRefreshListener;

    public static void setTokenRefreshListener(OnTokenRefreshListener listener) {
        tokenRefreshListener = listener;
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM token: " + token);

        // Notify any registered listeners
        if (tokenRefreshListener != null) {
            tokenRefreshListener.onTokenRefreshed(token);
        }

        // Broadcast token refresh event
        /*Intent intent = new Intent(ACTION_TOKEN_REFRESH);
        intent.putExtra("token", token);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);*/

    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        try {
            // Process the message data
            MessageData messageData = processMessageData(remoteMessage);

            // Show notification if app is in background
            if (!isAppInForeground()) {
                showNotification(messageData);
            }

            // Broadcast message to app
//            broadcastMessage(messageData);


        } catch (Exception e) {
            Log.e(TAG, "Error processing message", e);
        }
    }

    @NonNull
    private MessageData processMessageData(@NonNull RemoteMessage remoteMessage) {
        MessageData messageData = new MessageData();
        Map<String, String> data = remoteMessage.getData();

        messageData.message = data.get("message");
        messageData.senderId = data.get("senderId");
        messageData.senderName = data.get("senderName");
        messageData.chatRoomId = data.get("chatRoomId");
        messageData.timestamp = data.get("timestamp");

        if (remoteMessage.getNotification() != null) {
            messageData.title = remoteMessage.getNotification().getTitle();
            messageData.body = remoteMessage.getNotification().getBody();
        }

        return messageData;
    }

    private void showNotification(@NonNull MessageData messageData) {
        // Ensure notification channel exists for Android O and above
        createNotificationChannel();

        // Create intent for notification click
        Intent intent = createNotificationIntent(messageData);
        PendingIntent pendingIntent = createPendingIntent(intent);

        // Build the notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round) // Replace with your icon
                .setContentTitle(messageData.senderName)
                .setContentText(messageData.message)
                .setAutoCancel(true)
                .setSound(getDefaultSoundUri())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // Show the notification
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(notificationId.getAndIncrement(), notificationBuilder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @NonNull
    private Intent createNotificationIntent(@NonNull MessageData messageData) {
        // Replace "ChatActivity" with your actual activity
        Intent intent = new Intent(this, ChatRoom.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_CHAT_ROOM_ID, messageData.chatRoomId);
        return intent;
    }

    private PendingIntent createPendingIntent(@NonNull Intent intent) {
        int flags = PendingIntent.FLAG_ONE_SHOT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(this, 0, intent, flags);
    }

    private Uri getDefaultSoundUri() {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

/*    private void broadcastMessage(@NonNull MessageData messageData) {
        Intent intent = new Intent(ACTION_MESSAGE_RECEIVED);
        intent.putExtra(EXTRA_MESSAGE, messageData.message);
        intent.putExtra(EXTRA_SENDER_ID, messageData.senderId);
        intent.putExtra(EXTRA_SENDER_NAME, messageData.senderName);
        intent.putExtra(EXTRA_CHAT_ROOM_ID, messageData.chatRoomId);
        intent.putExtra(EXTRA_TIMESTAMP, messageData.timestamp);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }*/


    @Contract(pure = true)
    private boolean isAppInForeground() {
        return AppLifecycleTracker.isAppInForeground();
    }

    // Helper class to hold message data
    private static class MessageData {
        String message;
        String senderId;
        String senderName;
        String chatRoomId;
        String timestamp;
        String title;
        String body;
    }
}