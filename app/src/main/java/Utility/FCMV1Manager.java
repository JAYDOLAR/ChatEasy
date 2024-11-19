package Utility;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.auth.oauth2.GoogleCredentials;

import org.jetbrains.annotations.Contract;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FCMV1Manager {
    private static final String TAG = "FCMManager";
    private static final String FCM_V1_ENDPOINT = "https://fcm.googleapis.com/v1/projects/%s/messages:send";
    private static final String PROJECT_ID = "convoflow-2aedc";
    private static final String SERVICE_ACCOUNT_PATH = "service-account.json";
    private static final int TOKEN_REFRESH_TIMEOUT = 30;
    private static final int TOKEN_WAIT_TIMEOUT = 5;

    private final Context context;
    private volatile String accessToken;
    private final ExecutorService executorService;
    private volatile boolean isRefreshing;
    private final Object tokenLock;

    public FCMV1Manager(@NonNull Context context) {
        this.context = context.getApplicationContext(); // Use application context to prevent leaks
        this.executorService = Executors.newSingleThreadExecutor();
        this.isRefreshing = false;
        this.tokenLock = new Object();
        refreshAccessTokenSync(); // Initial token refresh
    }

    public interface FCMCallback {
        void onSuccess();

        void onFailure(@NonNull String error);
    }

    private boolean refreshAccessTokenSync() {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};

        synchronized (tokenLock) {
            if (isRefreshing) {
                try {
                    tokenLock.wait(TimeUnit.SECONDS.toMillis(TOKEN_WAIT_TIMEOUT));
                    return accessToken != null;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    Log.e(TAG, "Token refresh wait interrupted", e);
                    return false;
                }
            }
            isRefreshing = true;
        }

        executorService.execute(() -> {
            InputStream serviceAccount = null;
            try {
                serviceAccount = context.getAssets().open(SERVICE_ACCOUNT_PATH);
                GoogleCredentials googleCredentials = GoogleCredentials
                        .fromStream(serviceAccount)
                        .createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"));

                googleCredentials.refresh();
                accessToken = googleCredentials.getAccessToken().getTokenValue();
                success[0] = true;
                Log.d(TAG, "Access token refreshed successfully");
            } catch (IOException e) {
                Log.e(TAG, "Error refreshing access token", e);
                success[0] = false;
            } finally {
                if (serviceAccount != null) {
                    try {
                        serviceAccount.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Error closing service account stream", e);
                    }
                }
                synchronized (tokenLock) {
                    isRefreshing = false;
                    tokenLock.notifyAll();
                }
                latch.countDown();
            }
        });

        try {
            if (!latch.await(TOKEN_REFRESH_TIMEOUT, TimeUnit.SECONDS)) {
                Log.e(TAG, "Token refresh timeout");
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Token refresh wait interrupted", e);
            return false;
        }

        return success[0];
    }

    public void sendMessageNotification(
            @NonNull String recipientToken,
            @NonNull String message,
            @NonNull String senderId,
            @NonNull String senderName,
            @NonNull String chatId,
            @NonNull FCMCallback callback
    ) {
        // Input validation
        if (recipientToken.isEmpty() || message.isEmpty() || senderId.isEmpty() ||
                senderName.isEmpty() || chatId.isEmpty()) {
            callback.onFailure("Invalid input parameters");
            return;
        }

        // Ensure we have a valid token before sending
        if (accessToken == null && !refreshAccessTokenSync()) {
            callback.onFailure("Failed to obtain access token");
            return;
        }

        try {
            String endpoint = String.format(FCM_V1_ENDPOINT, PROJECT_ID);
            JSONObject fcmMessage = createMessagePayload(recipientToken, message, senderId, senderName, chatId);
            JsonObjectRequest request = createJsonRequest(endpoint, fcmMessage, callback);

            VolleySingleton.getInstance(context).getRequestQueue().add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating message payload", e);
            callback.onFailure("Error creating message payload: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error sending message", e);
            callback.onFailure("Unexpected error: " + e.getMessage());
        }
    }

    @NonNull
    private JSONObject createMessagePayload(
            @NonNull String recipientToken,
            @NonNull String message,
            @NonNull String senderId,
            @NonNull String senderName,
            @NonNull String chatId
    ) throws JSONException {
        JSONObject messageData = new JSONObject();
        messageData.put("senderName", senderName);
        messageData.put("message", message);
        messageData.put("senderId", senderId);
        messageData.put("chatRoomId", chatId);
        messageData.put("timestamp", String.valueOf(System.currentTimeMillis()));

        JSONObject notification = new JSONObject();
        notification.put("title", senderName);
        notification.put("body", message);

        JSONObject android = new JSONObject();
        android.put("priority", "high");
        JSONObject androidNotification = new JSONObject()
                .put("channel_id", "chat_messages")
                .put("click_action", "OPEN_CHAT_ACTIVITY")
                .put("sound", "default");
        android.put("notification", androidNotification);

        JSONObject payload = new JSONObject();
        payload.put("token", recipientToken);
        payload.put("data", messageData);
        payload.put("notification", notification);
        payload.put("android", android);

        return new JSONObject().put("message", payload);
    }

    @NonNull
    @Contract("_, _, _ -> new")
    private JsonObjectRequest createJsonRequest(
            @NonNull String endpoint,
            @NonNull JSONObject fcmMessage,
            @NonNull FCMCallback callback
    ) {
        return new JsonObjectRequest(
                Request.Method.POST,
                endpoint,
                fcmMessage,
                response -> {
                    Log.d(TAG, "Message sent successfully");
                    callback.onSuccess();
                },
                error -> handleRequestError(error, endpoint, fcmMessage, callback)
        ) {
            @NonNull
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
    }

    private void handleRequestError(
            @NonNull com.android.volley.VolleyError error,
            @NonNull String endpoint,
            @NonNull JSONObject fcmMessage,
            @NonNull FCMCallback callback
    ) {
        if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
            // Token expired, refresh and retry
            if (refreshAccessTokenSync()) {
                // Retry the request with new token
                JsonObjectRequest retryRequest = createJsonRequest(endpoint, fcmMessage, callback);
                VolleySingleton.getInstance(context).getRequestQueue().add(retryRequest);
                return;
            }
        }

        String errorMsg = buildErrorMessage(error);
        Log.e(TAG, errorMsg, error);
        callback.onFailure(errorMsg);
    }

    @NonNull
    private String buildErrorMessage(@NonNull com.android.volley.VolleyError error) {
        StringBuilder errorMsg = new StringBuilder("Error sending message: ");
        if (error.networkResponse != null) {
            errorMsg.append("Status Code: ").append(error.networkResponse.statusCode);
            if (error.networkResponse.data != null) {
                errorMsg.append(" Response: ").append(new String(error.networkResponse.data));
            }
        } else {
            errorMsg.append(error.getMessage() != null ? error.getMessage() : "Unknown error");
        }
        return errorMsg.toString();
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }
}