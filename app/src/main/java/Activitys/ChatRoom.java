package Activitys;

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

import com.example.chateasy.R;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.StorageReference;

import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import Adapters.MessageAdapter;
import Models.ChatRoomModel;
import Models.MessageModel;
import Models.MessageStatus;
import Models.MessageType;
import Models.User;
import Utility.FCMV1Manager;
import Utility.FireStoreDatabaseUtils;
import Utility.FirebaseAuthUtils;
import Utility.ImageUtils;
import Utility.LoggerUtil;
import Utility.ValidationUtils;

public class ChatRoom extends AppCompatActivity {

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
    private FCMV1Manager fcmManager;


    public ChatRoom() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        // Initialize views
        initializeViews();
        EdgeToEdge.enable(this);

        // Set navigation click listener
        presentUserChaToolbar.setNavigationOnClickListener(v -> finish());

        // Load conversation user details
        conversationUser();
        // Setup input panel
        chatRoomConversationInputPanel();
        fcmManager = new FCMV1Manager(this);

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

                final String[] currentUserName = {""};

                // Fetch current user details (example from Firestore)
                FireStoreDatabaseUtils.getUserData(FirebaseAuthUtils.getUserId(this), (snapshot, error) -> {
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

                // Send FCM message to the receiver
                DocumentReference docRef = FireStoreDatabaseUtils.getUserDocument(message.getReceiverId());
                docRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        final String receiverToken = documentSnapshot.getString("fcmToken");
                        assert receiverToken != null;
                        Log.e("FCMToken :::", receiverToken);
                        fcmManager.sendMessageNotification(
                                receiverToken,
                                message.getMessageContent(),
                                message.getSenderId(),
                                currentUserName[0],
                                chatRoomId,
                                new FCMV1Manager.FCMCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d("FCM", "Message sent via FCM V1");
                                    }

                                    @Override
                                    public void onFailure(@NonNull String error) {
                                        Log.e("FCM", "FCM V1 send failed: " + error);
                                    }
                                }
                        );
                    }
                });

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
                                    ValidationUtils.showToast(ChatRoom.this, "Error occurred to load the Image");
                                }
                            })).addOnFailureListener(e -> showPlaceholderImage(getResources()));
                }
            } else {
                LoggerUtil.logError("No such document", new Exception("Document not found"));
            }
        });
    }

    @Override
    protected void onDestroy() {
        // Shutdown FCM Manager when activity is destroyed
        if (fcmManager != null) {
            fcmManager.shutdown();
        }
        super.onDestroy();
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
