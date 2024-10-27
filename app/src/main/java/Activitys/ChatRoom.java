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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.StorageReference;

import org.jetbrains.annotations.Contract;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import Adapters.MessageAdapter;
import Models.ChatRoomModel;
import Models.MessageModel;
import Models.MessageStatus;
import Models.MessageType;
import Models.User;
import Utility.FireStoreDatabaseUtils;
import Utility.FirebaseAuthUtils;
import Utility.ImageUtils;
import Utility.LoggerUtil;
import Utility.ValidationUtils;

public class ChatRoom extends AppCompatActivity {

    // Constants
    /*private static final String FCM_ENDPOINT = "https://fcm.googleapis.com/fcm/send";*/
    private static final String FCM_API_V1_ENDPOINT = "https://fcm.googleapis.com/v1/projects/chateasy-209e2/messages:send";

    private static final String AUTHORIZATION_KEY = "AAAAHY2iXOY:APA91bFbSONb8YBu1pnSleiMFUc2X0xYb_xRfD4yhNSSLvgdD2LyMt7oPSKlwTcJl3oTVlyCvFgO8JUdaQmu1vL1FeRM9D6HIWLZYlvNgnkjjCDq2gMzuqFitXp3aebQLmj6yY72pHYG";
    private static final String TAG = "ChatRoom";
    private EditText msgeditText;
    private MaterialButton sendMsgButton;
    private androidx.appcompat.widget.Toolbar presentUserChaToolbar; // Change Toolbar import to androidx.appcompat.widget.Toolbar
    private ImageView CurrentUserImageView;
    private MaterialTextView CurrentUserNameView, CurrentUserStatusView;
    private ConstraintLayout coordinator;
    private String oppositeUserId;
    private String chatRoomId;
    private RecyclerView recyclerView;
    private ListenerRegistration userStatusListenerRegistration; // Declare userStatusListenerRegistration as a class-level variable

    public static void sendFCMMessage(final Context context, final String receiverId, final String message, final String chatRoomId, String senderId) {
        RequestQueue queue = Volley.newRequestQueue(context);
        final String[] currentUserName = {""};

        // Fetch current user details (example from Firestore)
        FireStoreDatabaseUtils.getUserData(FirebaseAuthUtils.getUserId(context), (snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error getting document", error);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                User currentUser = snapshot.toObject(User.class);
                if (currentUser != null) {
                    currentUserName[0] = currentUser.getUserName();
                }
            } else {
                Log.e(TAG, "No such document");
            }
        });

        // Fetch recipient's FCM token from Firestore
        DocumentReference docRef = FireStoreDatabaseUtils.getUserDocument(receiverId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                final String recipientToken = documentSnapshot.getString("fcmToken");
                if (recipientToken != null) {
                    // Build the message payload for FCM v1
                    JSONObject payload = new JSONObject();
                    JSONObject messageObject = new JSONObject();
                    JSONObject notificationObject = new JSONObject();
                    JSONObject dataObject = new JSONObject();

                    try {
                        // Notification part (optional)
                        notificationObject.put("title", "New Message from " + currentUserName[0]);
                        notificationObject.put("body", message);

                        // Data part (background message handling)
                        dataObject.put("senderName", currentUserName[0]);
                        dataObject.put("message", message);
                        dataObject.put("chatRoomId", chatRoomId);
                        dataObject.put("senderId", senderId);

                        // Add recipient token and message parts
                        messageObject.put("token", recipientToken);
                        messageObject.put("notification", notificationObject);  // For foreground notifications
                        messageObject.put("data", dataObject);  // For background data processing

                        payload.put("message", messageObject);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON exception: " + e.getMessage());
                        return;
                    }

                    // Send the request
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, FCM_API_V1_ENDPOINT,
                            response -> Log.d(TAG, "FCM message sent successfully: " + response),
                            error -> Log.e(TAG, "FCM message sending failed: " + Arrays.toString(error.getStackTrace()))) {
                        @NonNull
                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Authorization", "Bearer " + AUTHORIZATION_KEY); // Or "key=" for legacy keys
                            headers.put("Content-Type", "application/json");
                            return headers;
                        }

                        @Override
                        public byte[] getBody() {
                            return payload.toString().getBytes();
                        }
                    };

                    queue.add(stringRequest);
                } else {
                    Log.e(TAG, "Recipient FCM token is null");
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
//        FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(getApplicationContext()), UserStatus.ONLINE);
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
        MaterialButton sendDocButton = findViewById(R.id.sendDocToSend);
        sendMsgButton = findViewById(R.id.sendToSend);
        presentUserChaToolbar = findViewById(R.id.presentUserChaToolbar);
        CurrentUserImageView = findViewById(R.id.CurrentUserImage);
        CurrentUserNameView = findViewById(R.id.CurrentUserName);
        CurrentUserStatusView = findViewById(R.id.CurrentUserStatus);
        coordinator = findViewById(R.id.coordinator);
    }

    private void chatRoomConversationInputPanel() {
        msgeditText.addTextChangedListener(new TextWatcher() {
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
//                    FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(getApplicationContext()), UserStatus.TYPING);
                    isTyping = true;
                }

                // Cancel the previous typing timer
                typingTimer.cancel();

                // Start a new typing timer
                typingTimer = new Timer();
                // Adjust this value as needed
                long TYPING_DELAY = 1000;
                typingTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // Update user status to "Online" after typing delay
//                        FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(getApplicationContext()), UserStatus.ONLINE);
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

        // Generate a new document reference for the message
        DocumentReference messageRef = FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatRoomId)
                .collection("messages")
                .document();

        // Create a new message object
        MessageModel message = new MessageModel();
        message.setMessageId(messageRef.getId());  // Set the message ID to the Firestore-generated ID
        message.setMessageTimestamp(Timestamp.now());
        message.setSenderId(FirebaseAuthUtils.getUserId(getApplicationContext()));
        message.setReceiverId(receiver);
        message.setMessageType(MessageType.TEXT);
        message.setMessageStatus(MessageStatus.SENDING); // Set initial status to SENDING
        message.setMessageContent(messageText);

        // Save the message to Firestore using the generated document reference
        saveMessageToDatabase(message, messageRef);
    }

    // Method to save the message to the database using the provided DocumentReference
    private void saveMessageToDatabase(@NonNull MessageModel message, @NonNull DocumentReference messageRef) {
        messageRef.set(message).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Clear the message input field after successfully sending the message
                msgeditText.getText().clear();

                // Update the status of the message to DELIVERED
                updateMessageStatus(message.getMessageId(), MessageStatus.DELIVERED);

                // Send FCM message to the receiver
                sendFCMMessage(getApplicationContext(), message.getReceiverId(), message.getMessageContent(), chatRoomId, message.getSenderId());

                // Update the last message in the chat room
                setItToLastMessage(chatRoomId, message.getMessageContent());
            } else {
                // Failed to send the message
                ValidationUtils.showToast(ChatRoom.this, "Failed to send message. Please try again later.");
                LoggerUtil.logError("Error sending message", task.getException());

                // Update the status of the message to ERROR
                updateMessageStatus(message.getMessageId(), MessageStatus.ERROR);
            }
        });
    }


    // Method to update the message status in Firestore
    private void updateMessageStatus(String messageId, MessageStatus status) {
        if (messageId == null || messageId.isEmpty()) {
            LoggerUtil.logError("Invalid message ID", new IllegalArgumentException("Message ID is null or empty"));
            return;
        }

        // Update the status of the message in Firestore
        FireStoreDatabaseUtils.updateMessageStatus(chatRoomId, messageId, status)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message status updated to: " + status);
                })
                .addOnFailureListener(e -> {
                    LoggerUtil.logError("Error updating message status", e);
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

    private void retrieveOppositeUserDetails(String chatRoomId) {
        FireStoreDatabaseUtils.getChatRoomDocument(chatRoomId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ChatRoomModel chatRoom = documentSnapshot.toObject(ChatRoomModel.class);
                        if (chatRoom != null && chatRoom.getParticipants() != null
                                && chatRoom.getParticipants().size() == 2) {
                            oppositeUserId = getOppositeUserId(chatRoom.getParticipants());
                            if (oppositeUserId != null) {
                                loadProfilePicture(oppositeUserId);
                                setupChatRecyclerView(chatRoomId);
                            } else {
                                Log.e(TAG, "Error: Opposite user ID not found");
                            }
                        } else {
                            Log.e(TAG, "Error: Invalid participant IDs");
                        }
                    } else {
                        Log.e(TAG, "Error: Chat room document does not exist");
                    }
                })
                .addOnFailureListener(e -> LoggerUtil.logError("Error retrieving chat room", e));
    }

    private void setupChatRecyclerView(String chatroomId) {
        Query query = FireStoreDatabaseUtils.getMessagesCollection(chatroomId)
                .orderBy("messageTimestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<MessageModel> options = new FirestoreRecyclerOptions.Builder<MessageModel>()
                .setQuery(query, MessageModel.class).build();
        recyclerView = findViewById(R.id.messageSendReceive);
        MessageAdapter adapter = new MessageAdapter(options, FirebaseAuthUtils.getUserId(getApplicationContext()));
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
                    imageRef.getDownloadUrl().addOnSuccessListener(uri ->
                            ImageUtils.loadImage(this, uri.toString(), new ImageUtils.ImageLoadListener() {
                                @Override
                                public Drawable onResourceReady(Drawable resource) {
                                    CurrentUserImageView.setImageDrawable(resource);
                                    return resource;
                                }

                                @Override
                                public void onLoadFailed() {
                                    // Handle load failure
                                    /*ValidationUtils.showToast(ChatRoom.this, "Error occurred to load the Image");*/
                                }
                            })).addOnFailureListener(e -> showPlaceholderImage(getResources()));
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

    private void showPlaceholderImage(Resources resources) {
        int placeholderDrawableId = R.drawable.user;
        Drawable placeholderDrawable = ResourcesCompat.getDrawable(resources, placeholderDrawableId, null);
        CurrentUserImageView.setImageDrawable(placeholderDrawable);
    }

    @Contract(pure = true)
    private void setItToLastMessage(String chatRoom, String lastMessage) {
        FireStoreDatabaseUtils.updateLastMessage(chatRoom).update("lastMessage", lastMessage)
                .addOnSuccessListener(aVoid -> Log.e(TAG, "Successfully set the lastMessage"))
                .addOnFailureListener(e -> Log.e(TAG, "Error set the lastMessage: " + e.getMessage()));

        FireStoreDatabaseUtils.updateLastMessageTimestamp(chatRoom).update("lastMessageTimestamp", Timestamp.now())
                .addOnCompleteListener(task -> Log.e(TAG, "Successfully set the lastMessage  Timestamp"))
                .addOnFailureListener(e -> Log.e(TAG, "Error set the lastMessage Timestamp: " + e.getMessage()));
    }
}
