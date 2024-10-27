package Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chateasy.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import Activitys.ChatRoom;
import Models.ChatRoomModel;
import Models.User;
import Utility.FireStoreDatabaseUtils;
import Utility.FirebaseAuthUtils;
import Utility.ImageUtils;
import Utility.LoggerUtil;
import Utility.ValidationUtils;

public class UsersAdapter extends FirestoreRecyclerAdapter<ChatRoomModel, UsersAdapter.UserViewHolder> {

    private final Context context;
    private final SparseBooleanArray selectedItems = new SparseBooleanArray();

    public UsersAdapter(@NonNull FirestoreRecyclerOptions<ChatRoomModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull UserViewHolder holder, int position, @NonNull ChatRoomModel model) {
        holder.bind(model);
        holder.list_view_item_container.setChecked(selectedItems.get(position, false));
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_view_item_container, parent, false);
        return new UserViewHolder(view);
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        private static final String TAG = "UsersAdapter";
        ImageView currentUserImage;
        TextView userName;
        TextView latestMassage;
        TextView latestMassageTimestamp;
        TextView badgeUnReadMsges;
        MaterialCardView list_view_item_container;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            currentUserImage = itemView.findViewById(R.id.CurrentUserImage);
            userName = itemView.findViewById(R.id.userName);
            latestMassage = itemView.findViewById(R.id.latestMassage);
            latestMassageTimestamp = itemView.findViewById(R.id.latestMassage_timestamp);
            badgeUnReadMsges = itemView.findViewById(R.id.badgeUnReadMsges);
            list_view_item_container = itemView.findViewById(R.id.list_view_item_container);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    toggleSelection(position);
                }
            });

            /*itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    toggleSelection(position);
                    return true;
                }
                return false;
            });*/
        }

        public void bind(@NonNull ChatRoomModel model) {
            FireStoreDatabaseUtils.getOtherUserFromChatroom(model.getParticipants(), context)
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            User otherUserModel = task.getResult().toObject(User.class);
                            if (otherUserModel != null) {
                                userName.setText(otherUserModel.getUserName());

                                final String formattedTime = getString(model);
                                latestMassageTimestamp.setVisibility(View.VISIBLE);
                                latestMassageTimestamp.setText(formattedTime);
                                badgeUnReadMsges.setText(null);


                                String lastMessage = (model.getLastMessage() != null) ? model.getLastMessage() : "";
                                if (model.getLastMessage() != null) {
                                    boolean lastMessageSentByMe = model.getLastMessage().equals(FirebaseAuthUtils.getUserId(context));
                                    if (lastMessageSentByMe) {
                                        lastMessage = context.getString(R.string.last_message_you, model.getLastMessage());
                                    } else {
                                        lastMessage = context.getString(R.string.last_message_default, model.getLastMessage());
                                    }
                                }
                                latestMassage.setText(lastMessage);

                                String userId = otherUserModel.getUserId();
                                if (userId != null) {
                                    list_view_item_container.setOnClickListener(v -> loadChatroomIfPresentOrCreateNew(userId));
                                    list_view_item_container.setOnLongClickListener(v -> {
                                        toggleSelection(getBindingAdapterPosition());
                                        return true;
                                    });
                                }

                                FireStoreDatabaseUtils storageHelper = new FireStoreDatabaseUtils();
                                StorageReference imageRef = storageHelper.getImageReference(otherUserModel.getUserId());
                                imageRef.getDownloadUrl().addOnSuccessListener(uri -> ImageUtils.loadImage(context, uri.toString(), new ImageUtils.ImageLoadListener() {
                                    @Override
                                    public Drawable onResourceReady(Drawable resource) {
                                        currentUserImage.setImageDrawable(resource);
                                        return resource;
                                    }

                                    @Override
                                    public void onLoadFailed() {
                                        showPlaceholderImage();
                                    }
                                })).addOnFailureListener(e -> showPlaceholderImage());
                            }
                        }
                    });
        }

        private @NonNull String getString(@NonNull ChatRoomModel model) {
            Timestamp lastMessageTimestamp = model.getLastMessageTimestamp();

            Date date = null;
            String formattedTime = "";

            if (lastMessageTimestamp != null) {
                date = lastMessageTimestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                formattedTime = sdf.format(date);
            }
            return formattedTime;
        }

        private void toggleSelection(int position) {
            if (selectedItems.get(position, false)) {
                selectedItems.delete(position);
            } else {
                selectedItems.put(position, true);
            }
            notifyItemChanged(position);
        }

        private void showPlaceholderImage() {
            int placeholderDrawableId = R.drawable.user;
            Drawable placeholderDrawable = ContextCompat.getDrawable(context, placeholderDrawableId);
            currentUserImage.setImageDrawable(placeholderDrawable);
        }

        private void loadChatroomIfPresentOrCreateNew(String userId) {
            try {
                if (context == null) {
                    LoggerUtil.logErrors(TAG, "Context is null");
                    return;
                }
                String currentUserId = FirebaseAuthUtils.getUserId(context);
                if (currentUserId == null || currentUserId.isEmpty()) {
                    LoggerUtil.logErrors(TAG, "User ID is null or empty");
                    return;
                }

                FireStoreDatabaseUtils.getChatsCollection()
                        .whereArrayContains("participants", currentUserId)
                        .get()
                        .addOnCompleteListener(task -> {
                            try {
                                if (task.isSuccessful()) {
                                    QuerySnapshot querySnapshot = task.getResult();
                                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                            ChatRoomModel chatRoomModel = document.toObject(ChatRoomModel.class);
                                            if (chatRoomModel != null && chatRoomModel.getParticipants() != null) {
                                                if (chatRoomModel.getParticipants().contains(userId)) {
                                                    String chatRoomId = document.getId();
                                                    navigateToChatRoom(chatRoomId);
                                                    return;
                                                }
                                            }
                                        }
                                    } else {
                                        ValidationUtils.showToast(context, "No chat room found for both users.");
                                        LoggerUtil.logErrors(TAG, "No chat room found for both users.");
                                    }
                                } else {
                                    LoggerUtil.logErrors(TAG, "Error loading chat room: " + Objects.requireNonNull(task.getException()).getMessage());
                                    ValidationUtils.showToast(context, "Error loading chat room");
                                }
                            } catch (Exception e) {
                                LoggerUtil.logErrors(TAG, "Error handling chat room loading: " + e.getMessage());
                            }
                        });
            } catch (Exception e) {
                LoggerUtil.logErrors(TAG, "Error loading chat room: " + e.getMessage());
            }
        }

        private void navigateToChatRoom(String chatRoomId) {
            Intent intent = new Intent(context, ChatRoom.class);
            intent.putExtra("chatRoomId", chatRoomId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
