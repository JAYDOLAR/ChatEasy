package Utility;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import Models.ChatRoomModel;
import Models.MessageModel;
import Models.UserStatus;

public class FireStoreDatabaseUtils {

    private static final String TAG = "FirebaseStorageHelper";
    private static final String IMAGES_FOLDER = "images/";
    private static final String SHARED_PREFS_NAME = "MyPrefs";
    private static final String USER_ID_KEY = "userId";
    private final FirebaseStorage storage;
    /*private final SharedPreferences sharedPreferences;*/

    // Corrected constructor method name

    public FireStoreDatabaseUtils() {
        this.storage = FirebaseStorage.getInstance();
    }

    public static void enableOfflinePersistence(@NonNull FirebaseFirestore db) {
        // Configure Firestore settings with offline persistence
        FirebaseFirestoreSettings settings =
                new FirebaseFirestoreSettings.Builder(FirebaseFirestore.getInstance().getFirestoreSettings())
                        // Use memory-only cache
                        .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                        // Use persistent disk cache (default)
                        .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                                .build())
                        .build();
        FirebaseFirestore.getInstance().setFirestoreSettings(settings);
    }


    /*public FireStoreDatabaseUtils(Context context) {
        storage = FirebaseStorage.getInstance();
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }*/

    @NonNull
    public static CollectionReference getChatsCollection() {
        return FirebaseFirestore.getInstance().collection("chats");
    }

    @NonNull
    public static CollectionReference getMessagesCollection(String chatId) {
        return getChatsCollection().document(chatId).collection("messages");
    }

    @NonNull
    public static DocumentReference getUserDocument(String userId) {
        return FirebaseFirestore.getInstance().collection("users").document(userId);
    }

    @NonNull
    public static CollectionReference getUsersCollection() {
        return FirebaseFirestore.getInstance().collection("users");
    }

    public static void addUserToUserDocument(Map<String, Object> userData, OnCompleteListener<DocumentReference> listener) {
        FirebaseFirestore.getInstance().collection("users")
                .add(userData)
                .addOnCompleteListener(listener);
    }

    public static void getUserData(String userId, EventListener<DocumentSnapshot> listener) {
        getUserDocument(userId).addSnapshotListener(listener);
    }

    public static void getChatParticipants(String chatRoomId, OnCompleteListener<DocumentSnapshot> listener) {
        // Retrieve the chat room document based on the chat room ID
        FirebaseFirestore.getInstance().collection("chats").document(chatRoomId)
                .get()
                .addOnCompleteListener(listener);
    }


    public static void getChatMessages(String chatId, OnCompleteListener<QuerySnapshot> listener) {
        getMessagesCollection(chatId).orderBy("messageTimestamp", Query.Direction.ASCENDING).get().addOnCompleteListener(listener);
    }

    public static void updateUserName(String userId, String newName, OnCompleteListener<Void> listener) {
        getUserDocument(userId).update("userName", newName).addOnCompleteListener(listener);
    }

    public static void createNewChat(String participant1Id, String participant2Id, OnCompleteListener<DocumentReference> listener) {
        // Logging the current user ID and the start user ID if present
        if (participant1Id != null && !participant1Id.isEmpty()) {
            LoggerUtil.logInfo("Participant 1 ID: " + participant1Id);
        } else {
            LoggerUtil.logError("Participant 1 ID is null or empty", null);
        }
        if (participant2Id != null && !participant2Id.isEmpty()) {
            LoggerUtil.logInfo("Participant 2 ID: " + participant2Id);
        } else {
            LoggerUtil.logError("Participant 2 ID is null or empty", null);
        }

        List<String> participants = new ArrayList<>();
        participants.add(participant1Id);
        participants.add(participant2Id);

        /*final List<ChatRoomModel.Participant> participants = getParticipants(participant1Id, participant2Id);*/
        // Create a new chat with two participants
        ChatRoomModel chat = new ChatRoomModel(participants, null, 0, Timestamp.now());
        getChatsCollection().add(chat).addOnCompleteListener(listener);
    }

    @NonNull
    public static DocumentReference getOtherUserFromChatroom(@NonNull List<String> userIds, Context context) {
        if (userIds.get(0).equals(FirebaseAuthUtils.getUserId(context))) {
            return getUserDocument(userIds.get(1));
        } else {
            return getUserDocument(userIds.get(0));
        }
    }

    public static void addMessageToChat(String chatId, MessageModel message, OnCompleteListener<DocumentReference> listener) {
        // Add a new message to a chat
        getMessagesCollection(chatId).add(message).addOnCompleteListener(listener);
    }

/*
    @NonNull
    private static List<ChatRoomModel.Participant> getParticipants(String participant1Id, String participant2Id) {
        ChatRoomModel.Participant participant1 = new ChatRoomModel.Participant(participant1Id, UserStatus.OFFLINE, 0);
        ChatRoomModel.Participant participant2 = new ChatRoomModel.Participant(participant2Id, UserStatus.OFFLINE, 0);
        List<ChatRoomModel.Participant> participants = new ArrayList<>();
        participants.add(participant1);
        participants.add(participant2);
        return participants;
    }
*/

    public static void markMessageAsRead(String chatId, String messageId) {
        // Mark a message as read in a chat
        getMessagesCollection(chatId).document(messageId).update("read", true);
    }

    public static void deleteChat(String chatId, OnCompleteListener<Void> listener) {
        // Delete a chat and its messages
        getChatsCollection().document(chatId).delete().addOnCompleteListener(listener);
        // You might also want to delete associated messages
        getMessagesCollection(chatId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DocumentSnapshot document : task.getResult()) {
                    document.getReference().delete();
                }
            }
        });
    }

    @NonNull
    public static Task<DocumentSnapshot> getChatRoomDocument(String chatRoomId) {
        return FirebaseFirestore.getInstance().collection("chats").document(chatRoomId).get();
    }

    public static void updateUserLastActivity(String userId) {
        // Update user's last activity timestamp
        getUserDocument(userId).update("lastActiveTime", Timestamp.now());
    }

    public static void updateUserStatus(String userId, UserStatus userStatus) {
        // Update user's last activity timestamp
        getUserDocument(userId).update("status", userStatus);
    }

    public static DocumentReference updateLastMessage(String chatRoomId) {
        return FirebaseFirestore.getInstance().collection("chats").document(chatRoomId);
    }

    public static void getLastMessage(String chatRoom, OnLastMessageListener listener) {
        FirebaseFirestore
                .getInstance()
                .collection("chats")
                .document(chatRoom)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String lastMessage = documentSnapshot.getString("lastMessage");
                        // Last message retrieved successfully
                        // No last message found
                        listener.onLastMessageReceived(lastMessage);
                    } else {
                        // Chat room document doesn't exist
                        listener.onLastMessageReceived(null);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle errors
                    Log.e(TAG, "Error getting last message: " + e.getMessage());
                    listener.onLastMessageReceived(null);
                });
    }

    public FirestoreRecyclerOptions<MessageModel> getMessagesOptions(String chatId, OnCompleteListener<QuerySnapshot> listener) {
        Query query = getMessagesCollection(chatId).orderBy("messageTimestamp", Query.Direction.ASCENDING);
        return new FirestoreRecyclerOptions.Builder<MessageModel>()
                .setQuery(query, MessageModel.class)
                .build();
    }

    // Method to upload user image to Firebase Storage
    public void uploadImage(Uri imageUri, String imageName, UploadListener listener) {
        StorageReference storageRef = storage.getReference();
        StorageReference imagesRef = storageRef.child(IMAGES_FOLDER + imageName);

        UploadTask uploadTask = imagesRef.putFile(imageUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imagesRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                if (listener != null) {
                    listener.onSuccess(downloadUri);
                }
            }).addOnFailureListener(e -> {
                if (listener != null) {
                    listener.onFailure(e);
                }
            });
        }).addOnFailureListener(e -> {
            if (listener != null) {
                listener.onFailure(e);
            }
        });
    }

    public StorageReference getImageReference(String imageName) {
        StorageReference storageRef = storage.getReference();
        return storageRef.child(IMAGES_FOLDER + imageName);
    }


    public interface UploadListener {
        void onSuccess(Uri downloadUri);

        void onFailure(Exception e);
    }

    // Define an interface for callback
    public interface OnLastMessageListener {
        void onLastMessageReceived(String lastMessage);
    }
 /*   public String getUserId() {
        return sharedPreferences.getString(USER_ID_KEY, null);
    }

    public void setUserId(String userId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(USER_ID_KEY, userId);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return getUserId() != null;
    }

    public void setLoggedIn(boolean isLoggedIn) {
        if (isLoggedIn) {
            // Set the user ID when logging in
            String userId = FirebaseAuthUtils.getCurrentUserId();
            setUserId(userId);
        } else {
            // Clear the user ID when logging out
            setUserId(null);
        }
    }*/
}
