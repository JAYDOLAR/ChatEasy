package Utility;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FCMSender {
    private static final String FCM_SEND_ENDPOINT = "https://fcm.googleapis.com/fcm/send";
    private static final String SERVER_KEY = "YOUR_SERVER_KEY"; // Replace with your actual FCM server key
    private static final String TAG = "FCMSender";

    public static void sendMessage(Context context, String receiverToken, String title, String message, String chatRoomId, String senderId) {
        RequestQueue queue = Volley.newRequestQueue(context);

        try {
            JSONObject payload = new JSONObject();
            JSONObject notification = new JSONObject();
            JSONObject data = new JSONObject();

            notification.put("title", title);
            notification.put("body", message);

            data.put("chatRoomId", chatRoomId);
            data.put("senderId", senderId);

            payload.put("to", receiverToken);
            payload.put("notification", notification);
            payload.put("data", data);

            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, FCM_SEND_ENDPOINT, payload,
                    response -> Log.d(TAG, "FCM message sent successfully: " + response.toString()),
                    error -> Log.e(TAG, "FCM message sending failed: " + error.toString())) {
                @NonNull
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Authorization", "key=" + SERVER_KEY);
                    return headers;
                }
            };

            queue.add(jsonRequest);

        } catch (JSONException e) {
            Log.e(TAG, "JSONException: " + e.getMessage());
        }
    }
}