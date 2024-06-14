package Activitys;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.chateasy.R;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.StorageReference;

import org.jetbrains.annotations.Contract;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import Adapters.MessageAdapter;
import Models.MessageModel;
import Models.MessageStatus;
import Models.MessageType;
import Models.User;
import Models.UserStatus;
import Utility.FireStoreDatabaseUtils;
import Utility.FirebaseAuthUtils;
import Utility.ImageUtils;
import Utility.LoggerUtil;
import Utility.ValidationUtils;

public class ChatRoom extends AppCompatActivity {

    // Constants
    private static final String FCM_ENDPOINT = "https://fcm.googleapis.com/fcm/send";
    private static final String AUTHORIZATION_KEY = "AAAAHY2iXOY:APA91bFbSONb8YBu1pnSleiMFUc2X0xYb_xRfD4yhNSSLvgdD2LyMt7oPSKlwTcJl3oTVlyCvFgO8JUdaQmu1vL1FeRM9D6HIWLZYlvNgnkjjCDq2gMzuqFitXp3aebQLmj6yY72pHYG";
    private static final String TAG = "ChatRoom";
    private EditText msgeditText;
    private MaterialButton sendDocButton;
    private MaterialButton sendMsgButton;
    private androidx.appcompat.widget.Toolbar presentUserChaToolbar; // Change Toolbar import to androidx.appcompat.widget.Toolbar
    private ImageView CurrentUserImageView;
    private MaterialTextView CurrentUserNameView, CurrentUserStatusView;
    private ConstraintLayout coordinator;
    private String oppositeUserId;
    private String chatRoomId;
    private MessageAdapter adapter;
    private RecyclerView recyclerView;
    private ListenerRegistration userStatusListenerRegistration; // Declare userStatusListenerRegistration as a class-level variable

    public static void sendFCMMessage(final Context context, final String receiverId, final String message, final String chatRoomId) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
        final String[] currentUserName = {""};

        FireStoreDatabaseUtils.getUserData(FirebaseAuthUtils.getUserId(context), (snapshot, error) -> {
            if (error != null) {
                // Log error
                LoggerUtil.logError("Error getting document", error);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                User otherUserModel = snapshot.toObject(User.class);
                if (otherUserModel != null) {
                    // Update name using fetched data
                    currentUserName[0] = otherUserModel.getUserName();
                }
            } else {
                LoggerUtil.logError("No such document", new Exception("Document not found"));
            }
        });


        DocumentReference docRef = FireStoreDatabaseUtils.getUserDocument(receiverId);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                final String recipientToken = documentSnapshot.getString("fcmToken");
                if (recipientToken != null) {
                    // Create the message payload
                    JSONObject payload = new JSONObject();
                    JSONObject data = new JSONObject();
                    if (!currentUserName[0].isEmpty()) {

                        try {
                            Log.e(TAG, "Current User Name : " + currentUserName[0]);
                            data.put("senderName", currentUserName[0]);
                            data.put("message", message);
                            data.put("chatRoomId", chatRoomId);
                            payload.put("data", data);
                            payload.put("to", recipientToken);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "JSON exception: " + e.getMessage());
                            return;
                        }
                        // Request a string response from the provided URL.
                        StringRequest stringRequest = new StringRequest(Request.Method.POST, FCM_ENDPOINT,
                                response -> {
                                    // Handle response
                                    Log.d(TAG, "FCM message sent successfully");
                                }, error -> {
                            // Handle error
                            Log.e(TAG, "FCM message sending failed: " + error.getMessage());
                        }) {
                            @NonNull
                            @Override
                            public Map<String, String> getHeaders() {
                                Map<String, String> headers = new HashMap<>();
                                headers.put("Authorization", "key=" + AUTHORIZATION_KEY);
                                headers.put("Content-Type", "application/json");
                                return headers;
                            }

                            @Override
                            public byte[] getBody() {
                                return payload.toString().getBytes();
                            }
                        };

                        // Add the request to the RequestQueue.
                        queue.add(stringRequest);
                    } else {
                        Log.e(TAG, "Error: Current user name is empty");
                    }
                } else {
                    Log.e(TAG, "Error: Recipient token is null");
                }
            } else {
                Log.e(TAG, "Document does not exist for user: " + receiverId);
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error getting document: " + e.getMessage()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        // Initialize views
        initializeViews();
        FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(getApplicationContext()), UserStatus.ONLINE);
        EdgeToEdge.enable(this);

        // Set navigation click listener
        presentUserChaToolbar.setNavigationOnClickListener(v -> finish());

        // Load conversation user details
        conversationUser();
        // Setup input panel
        chatRoomConversationInputPanel();
    }

    private void initializeViews() {
        msgeditText = findViewById(R.id.open_message_view_edit_text);
        sendDocButton = findViewById(R.id.sendDocToSend);
        sendMsgButton = findViewById(R.id.sendToSend);
        presentUserChaToolbar = findViewById(R.id.presentUserChaToolbar);
        CurrentUserImageView = findViewById(R.id.CurrentUserImage);
        CurrentUserNameView = findViewById(R.id.CurrentUserName);
        CurrentUserStatusView = findViewById(R.id.CurrentUserStatus);
        coordinator = findViewById(R.id.coordinator);
    }

    private void chatRoomConversationInputPanel() {
        msgeditText.addTextChangedListener(new TextWatcher() {
            private final long TYPING_DELAY = 1000; // Adjust this value as needed
            private Timer typingTimer = new Timer();
            private boolean isTyping = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No implementation needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No implementation needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isTyping) {
                    // Update user status to "Typing" when user starts typing
                    FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(getApplicationContext()), UserStatus.TYPING);
                    isTyping = true;
                }

                // Cancel the previous typing timer
                typingTimer.cancel();

                // Start a new typing timer
                typingTimer = new Timer();
                typingTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // Update user status to "Online" after typing delay
                        FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(getApplicationContext()), UserStatus.ONLINE);
                        isTyping = false;
                    }
                }, TYPING_DELAY);
            }
        });
    }

    private void setupSendMessageButton() {
        sendMsgButton.setOnClickListener(v -> {
            String userMessage = msgeditText.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                sendMessage(userMessage);
            } else {
                // Handle empty message case
                ValidationUtils.showToast(ChatRoom.this, "Please enter a message");
            }
        });

    }

    private void sendMessage(@NonNull String messageText) {
        // Validate message text
        if (messageText.isEmpty()) {
            // Notify the user that the message cannot be empty
            ValidationUtils.showToast(ChatRoom.this, "Message cannot be empty");
            return;
        }

        String receiver = oppositeUserId;

        // Create a new message object
        MessageModel message = new MessageModel();
        message.setMessageTimestamp(Timestamp.now()); // Set the creation timestamp
        message.setSenderId(FirebaseAuthUtils.getUserId(getApplicationContext())); // Set sender ID
        message.setReceiverId(receiver); // Set recipient ID
        message.setMessageType(MessageType.TEXT); // Set message type
        message.setMessageStatus(MessageStatus.SENDING); // Set message status
        message.setMessageContent(messageText); // Set message text
        setItToLastMessage(chatRoomId, messageText);
        // Save the message to the database (e.g., Firebase Firestore)
        saveMessageToDatabase(message);
        sendFCMMessage(getApplicationContext(), receiver, messageText, chatRoomId);
        setItToLastMessage(chatRoomId, messageText);
        /*getToken(messageText, receiver, chatRoomId);*/
        /*setFCMToken(messageText, FirebaseAuthUtils.getUserId(getApplicationContext()), chatRoomId);*/
    }
/*    private void retrieveMessages(String chatRoomId) {
        // Query messages from the database based on the chat room ID
        FireStoreDatabaseUtils.getChatMessages(chatRoomId, querySnapshot -> {
            if (querySnapshot.isSuccessful()) {
                List<MessageModel> messages = new ArrayList<>();
                for (DocumentSnapshot document : querySnapshot.getResult()) {
                    // Map document data to a message model object
                    MessageModel message = document.toObject(MessageModel.class);
                    if (message != null) {
                        messages.add(message);
                    }
                }
                // Pass the retrieved messages to the RecyclerView adapter
                displayMessages(messages);
            } else {
                // Handle failure to retrieve messages
                ValidationUtils.showToast(this, "Failed to retrieve messages. Please try again later.");
                LoggerUtil.logError("Error retrieving messages", querySnapshot.getException());
            }
        });
    }

    private void displayMessages(List<MessageModel> messages) {
        if (messages == null || messages.isEmpty()) {
            // Handle case where there are no messages to display
            ValidationUtils.showToast(this, "No messages to display");
            return;
        }
        String currentUserId = FirebaseAuthUtils.getUserId(getApplicationContext());
        if (currentUserId == null || currentUserId.isEmpty()) {
            // Handle case where current user ID is invalid or empty
            ValidationUtils.showToast(this, "Invalid current user ID");
            return;
        }

        // Assuming you have an instance of MessageAdapter
        MessageAdapter adapter = new MessageAdapter(messages, currentUserId); // Pass the list of messages and the current user ID

        // Assuming you have a RecyclerView with id "recyclerView" in your layout
        RecyclerView recyclerView = findViewById(R.id.messageSendReceive);
        if (recyclerView == null) {
            // Handle case where RecyclerView is not found
            ValidationUtils.showToast(this, "RecyclerView not found");
            return;
        }

        // Set the layout manager and adapter for your RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);// Start displaying items from the bottom of the list
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        setupSendMessageButton();
    }*/

    private void saveMessageToDatabase(@NonNull MessageModel message) {
        // Set the status of the message as sending (status = 0)
/*
        message.setMessageStatus(MessageStatus.SENDING); // Set message status
*/

        // Assuming you have a method to save the message to the database
        FireStoreDatabaseUtils.addMessageToChat(chatRoomId, message, task -> {
            if (task.isSuccessful()) {
                // Message sent successfully
                // Clear the message input field
                msgeditText.getText().clear();
                // Update the status of the message to sent (status = 1)
                message.setMessageStatus(MessageStatus.DELIVERED);  // 1 = sent
            } else {
                // Failed to send message
                ValidationUtils.showToast(ChatRoom.this, "Failed to send message. Please try again later.");
                LoggerUtil.logError("Error sending message", task.getException());
                // Update the status of the message to failed (status = 4)
                message.setMessageStatus(MessageStatus.ERROR); // 4 = failed
            }
        });
    }

    private void conversationUser() {
        Intent intent = getIntent();
        if (intent != null) {
            chatRoomId = intent.getStringExtra("chatRoomId");
            if (chatRoomId != null) {
                retrieveOppositeUserDetails(chatRoomId);
            } else {
                ValidationUtils.showSnackBar(coordinator, "Error occurred to load the User Detail Re start the Chat.");
            }
        }
    }

    /*private void loadProfilePicture(String userId) {
        // Fetch user details from Firestore using userId
        FireStoreDatabaseUtils.getUserData(userId, task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    User otherUserModel = document.toObject(User.class);
                    if (otherUserModel != null) {
                        // Update name and phoneTextView using fetched data
                        CurrentUserNameView.setText(otherUserModel.getUserName());

                        // Setup the user status listener
                        FireStoreDatabaseUtils.listenForUserStatus(userId, (snapshot, error) -> {
                            if (snapshot != null && snapshot.exists()) {
                                String status = snapshot.getString("status");
                                if (status != null) {
                                    CurrentUserStatusView.setVisibility(View.VISIBLE);
                                    CurrentUserStatusView.setText(status);
                                }
                            }
                        });

                        // Assuming the profile picture URL is stored in otherUserModel
                        FireStoreDatabaseUtils storageHelper = new FireStoreDatabaseUtils();
                        StorageReference imageRef = storageHelper.getImageReference(userId); // Assuming you have a method to get StorageReference for the user's image

                        // Download the image from Firebase Storage
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Load the image using Glide or any other image loading library
                            ImageUtils.loadImage(this, uri.toString(), new ImageUtils.ImageLoadListener() {
                                @Override
                                public void onResourceReady(Drawable resource) {
                                    CurrentUserImageView.setImageDrawable(resource);
                                }

                                @Override
                                public void onLoadFailed() {
                                    // Handle load failure
                                    ValidationUtils.showToast(ChatRoom.this, "Error occurred to load the Image");
                                }
                            });
                        }).addOnFailureListener(e -> showPlaceholderImage(getResources()));
                    }
                } else {
                    LoggerUtil.logError("No such document", task.getException());
                }
            } else {
                LoggerUtil.logError("Error getting document", task.getException());
            }
        });
    }*/

    private void retrieveOppositeUserDetails(String chatRoomId) {
        FireStoreDatabaseUtils.getChatRoomDocument(chatRoomId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> participantIds = (List<String>) documentSnapshot.get("participants");
                        if (participantIds != null && participantIds.size() == 2) {
                            oppositeUserId = getOppositeUserId(participantIds);
                            if (oppositeUserId != null) {
                                // Fetch the details of the opposite user
                                loadProfilePicture(oppositeUserId);
                                /*retrieveMessages(chatRoomId);*/
                                setupChatRecyclerView(chatRoomId);
                            } else {
                                Log.e(TAG, "Error retrieving opposite user details: Opposite user ID not found");
                            }
                        } else {
                            Log.e(TAG, "Error retrieving opposite user details: Invalid participant IDs");
                        }
                    } else {
                        Log.e(TAG, "Error retrieving opposite user details: Chat room document does not exist");
                    }
                })
                .addOnFailureListener(e -> LoggerUtil.logError("Error retrieving opposite user details", e));
    }

    private void setupChatRecyclerView(String chatroomId) {
        Query query = FireStoreDatabaseUtils.getMessagesCollection(chatroomId)
                .orderBy("messageTimestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<MessageModel> options = new FirestoreRecyclerOptions.Builder<MessageModel>()
                .setQuery(query, MessageModel.class).build();
        recyclerView = findViewById(R.id.messageSendReceive);
        adapter = new MessageAdapter(options, FirebaseAuthUtils.getUserId(getApplicationContext()));
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });
        setupSendMessageButton();
    }

    @Nullable
    private String getOppositeUserId(@NonNull List<String> participantIds) {
        String currentUserId = FirebaseAuthUtils.getUserId(getApplicationContext());
        for (String participantId : participantIds) {
            if (!participantId.equals(currentUserId)) {
                return participantId;
            }
        }
        return null; // Opposite user ID not found
    }

/*    private static String[] loadDetail(String userId) {
        final String[] userData = new String[2];
        // Fetch user details from Firestore using userId
        FireStoreDatabaseUtils.getUserData(userId, task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    Map<String, Object> userDataMap = document.getData();
                    if (userDataMap != null) {
                        // Update name and phoneTextView using fetched data
                        userData[0] = (String) userDataMap.get("userName");
                        userData[1] = (String) userDataMap.get("userId");
                    }
                } else {
                    Log.e("ChatRoom", "No such document for user: " + userId);
                }
            } else {
                Log.e("ChatRoom", "Error getting document for user: " + userId, task.getException());
            }
        });
        return userData;
    }*/

    private void loadProfilePicture(String userId) {
        // Fetch user details from Firestore using userId
        FireStoreDatabaseUtils.getUserData(userId, (snapshot, error) -> {
            if (error != null) {
                LoggerUtil.logError("Error getting document", error);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                User otherUserModel = snapshot.toObject(User.class);
                if (otherUserModel != null) {
                    // Update name and phoneTextView using fetched data
                    CurrentUserNameView.setText(otherUserModel.getUserName());
                    if (otherUserModel.getStatus() != null) {
                        CurrentUserStatusView.setVisibility(View.VISIBLE);
                        CurrentUserStatusView.setText(otherUserModel.getStatus().getDisplayName());
                    }
                    // Assuming the profile picture URL is stored in otherUserModel
                    FireStoreDatabaseUtils storageHelper = new FireStoreDatabaseUtils();
                    StorageReference imageRef = storageHelper.getImageReference(userId); // Assuming you have a method to get StorageReference for the user's image

                    // Download the image from Firebase Storage
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Load the image using Glide or any other image loading library
                        ImageUtils.loadImage(this, uri.toString(), new ImageUtils.ImageLoadListener() {
                            @Override
                            public void onResourceReady(Drawable resource) {
                                CurrentUserImageView.setImageDrawable(resource);
                            }

                            @Override
                            public void onLoadFailed() {
                                // Handle load failure
                                /*ValidationUtils.showToast(ChatRoom.this, "Error occurred to load the Image");*/
                            }
                        });
                    }).addOnFailureListener(e -> showPlaceholderImage(getResources()));
                }
            } else {
                LoggerUtil.logError("No such document", new Exception("Document not found"));
            }
        });
    }

    // Call this method in your activity's onDestroy() or fragment's onDestroyView()
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the listener when the activity or fragment is destroyed to avoid memory leaks
        if (userStatusListenerRegistration != null) {
            userStatusListenerRegistration.remove();
        }
    }

/*
    private void setFCMToken(String message, String receiverId, String chatRoomId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Get the device token from the task result
                        String token = task.getResult();
                        // Save the device token to the Firestore for the current user
                        FireStoreDatabaseUtils.getUsersCollection().document(receiverId)
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("FCM", "Device token saved to Firestore successfully");
                                    // Send notification to the token
                                    sendToToken(token, message, chatRoomId);
                                })
                                .addOnFailureListener(e -> Log.e("FCM", "Error saving device token to Firestore", e));
                    } else {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                    }
                });
    }

    public void sendToToken(String token, String message, String chatRoomId) {
        // Construct the notification payload
        String[] userData = loadDetail(FirebaseAuthUtils.getUserId(getApplicationContext()));
        Map<String, String> data = new HashMap<>();
        data.put("message", message);
        data.put("senderName", userData[0]);
        data.put("senderId", userData[0]);
        data.put("chatRoomId", chatRoomId);

        // Send the notification to the token
        new RemoteMessage.Builder(token + "@gcm.googleapis.com")
                .setMessageId(String.valueOf(System.currentTimeMillis()))
                .setData(data)
                .build();

    }*/


/*    private void getToken(String message, String receiverId, String chatRoomId) {
        ValidationUtils.showToast(getApplicationContext(), "outer :"+message + receiverId + chatRoomId);
        DocumentReference docRef = FireStoreDatabaseUtils.getUsersCollection().document(receiverId);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String token = documentSnapshot.getString("fcmToken");
                if (token != null) {
                    JSONObject to = new JSONObject();
                    JSONObject data = new JSONObject();
                    try {
                        String[] userData = loadDetail(FirebaseAuthUtils.getUserId(getApplicationContext()));
                        if (userData != null && userData.length == 2) {
                            data.put("senderName", userData[0]);
                            data.put("message", message);
                            data.put("senderId", userData[1]);
                            data.put("chatRoomId", chatRoomId);

                            to.put("receiver", token);
                            to.put("senderData", data);
                            ValidationUtils.showToast(getApplicationContext(), "inner :"+message + receiverId + chatRoomId);
                            sendNotification(to);
                        } else {
                            Log.e("ChatRoom", "Error loading user details for sending notification");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Log.e("ChatRoom", "Document does not exist for user: " + receiverId);
            }
        }).addOnFailureListener(e -> Log.e("ChatRoom", "Error getting document: " + e.getMessage()));
    }


    private void sendNotification(JSONObject to) {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                FCM_ENDPOINT,
                to,
                response -> Log.d("notification", "sendNotification: " + response),
                error -> Log.e("notification", "sendNotification: " + error)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> map = new HashMap<>();
                map.put("Authorization", "key=" + AUTHORIZATION_KEY);
                map.put("Content-Type", "application/json");
                return map;
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        requestQueue.add(request);
    }*/

    private void showPlaceholderImage(Resources resources) {
        int placeholderDrawableId = R.drawable.user;
        Drawable placeholderDrawable = ResourcesCompat.getDrawable(resources, placeholderDrawableId, null);
        CurrentUserImageView.setImageDrawable(placeholderDrawable);
    }

    @Contract(pure = true)
    private void setItToLastMessage(String chatRoom, String lastMessage) {
        FireStoreDatabaseUtils.updateLastMessage(chatRoom).update("lastMessage", lastMessage)
                .addOnSuccessListener(aVoid -> {
                    // Last message updated successfully
                    // You can add further logic here if needed
                    Log.e(TAG, "Successfully set the lastMessage");
                })
                .addOnFailureListener(e -> {
                    // Handle errors
                    Log.e(TAG, "Error set the lastMessage: " + e.getMessage());
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(getApplicationContext()), UserStatus.ONLINE);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(getApplicationContext()), UserStatus.ONLINE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (FirebaseAuthUtils.getUserId(getApplicationContext()) == null) {
            LoggerUtil.logErrors("User is null : ", FirebaseAuthUtils.getUserId(getApplicationContext()));
        } else {
            FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(getApplicationContext()), UserStatus.OFFLINE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(getApplicationContext()), UserStatus.ONLINE);
    }
}
