package Utility;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

import Models.ChatRoomModel;
import Models.MessageStatus;

public class FireStoreDatabaseUtils {

    private static final String IMAGES_FOLDER = "images/";
    private final FirebaseStorage storage;

    public FireStoreDatabaseUtils() {
        this.storage = FirebaseStorage.getInstance();
    }

    public static void enableOfflinePersistence() {
        FirebaseFirestoreSettings settings =
                new FirebaseFirestoreSettings.Builder(FirebaseFirestore.getInstance().getFirestoreSettings())
                        .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                        .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                        .build();
        FirebaseFirestore.getInstance().setFirestoreSettings(settings);
    }

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

    public static void getUserData(String userId, EventListener<DocumentSnapshot> listener) {
        getUserDocument(userId).addSnapshotListener(listener);
    }

    public static void createNewChat(String participant1Id, String participant2Id, OnCompleteListener<DocumentReference> listener) {
        List<String> participants = new ArrayList<>();
        participants.add(participant1Id);
        participants.add(participant2Id);
        ChatRoomModel chat = new ChatRoomModel(participants, null, 0, Timestamp.now(), null, null);
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

    @NonNull
    public static Task<Void> updateMessageStatus(String chatRoomId, String messageId, MessageStatus status) {
        if (chatRoomId == null || chatRoomId.isEmpty() || messageId == null || messageId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ChatRoom ID or Message ID is null or empty"));
        }

        return getMessagesCollection(chatRoomId)
                .document(messageId)
                .update("messageStatus", status);
    }

    public static void deleteChat(String chatId, OnCompleteListener<Void> listener) {
        getChatsCollection().document(chatId).delete().addOnCompleteListener(listener);
        getMessagesCollection(chatId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DocumentSnapshot document : task.getResult()) {
                    document.getReference().delete();
                }
            }
        });
    }

    public static void fetchUserData(String userId, OnFetchCompleteListener onFetchComplete) {
        FireStoreDatabaseUtils.getUserData(userId, (snapshot, error) -> {
            if (error != null) {
                onFetchComplete.onError("Failed to retrieve user data");
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                onFetchComplete.onSuccess(snapshot);
            } else {
                onFetchComplete.onError("User document does not exist");
            }
        });
    }

    // Callback interface for data fetching
    public interface OnFetchCompleteListener {
        void onSuccess(DocumentSnapshot snapshot);

        void onError(String errorMessage);
    }

    @NonNull
    public static Task<DocumentSnapshot> getChatRoomDocument(String chatRoomId) {
        return FirebaseFirestore.getInstance().collection("chats").document(chatRoomId).get();
    }

    @NonNull
    public static DocumentReference updateLastMessage(String chatRoomId) {
        return FirebaseFirestore.getInstance().collection("chats").document(chatRoomId);
    }

    @NonNull
    public static DocumentReference updateLastMessageTimestamp(String chatRoomId) {
        return FirebaseFirestore.getInstance().collection("chats").document(chatRoomId);
    }

    // Method to upload user profile image to Firebase Storage
    public void uploadUserProfileImage(Uri imageUri, UploadListener listener, String imageName) {
        if (imageUri == null) {
            listener.onFailure(new Exception("Image URI is null"));
            return;
        }

        // Create a unique filename for the image
        StorageReference imagesRef = storage.getReference().child(IMAGES_FOLDER + imageName);

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
        return storage.getReference().child(IMAGES_FOLDER + imageName);
    }

    public interface UploadListener {
        void onSuccess(Uri downloadUri);

        void onFailure(Exception e);
    }
}
