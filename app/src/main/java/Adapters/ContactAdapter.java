package Adapters;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chateasy.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Map;

import Activitys.ChatRoom;
import Models.ChatRoomModel;
import Models.User;
import Utility.FireStoreDatabaseUtils;
import Utility.FirebaseAuthUtils;
import Utility.ImageUtils;
import Utility.LoggerUtil;
import Utility.ValidationUtils;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {
    private final String TAG = "ContactAdapter";
    private final Context context;
    private final ArrayList<Map<String, Object>> contacts;
    private final ImageView user_expanded_image_view;

    private Animator currentAnimator;
    private int shortAnimationDuration;

    public ContactAdapter(Context context, ImageView user_expanded_image_view) {
        this.context = context;
        this.user_expanded_image_view = user_expanded_image_view;
        contacts = new ArrayList<>();
    }

    public void addUser(Map<String, Object> user) {
        contacts.add(user);
    }

    public ArrayList<Map<String, Object>> getContacts() {
        return contacts;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_contact_item, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Map<String, Object> contact = contacts.get(position);
        holder.bind(contact);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public class ContactViewHolder extends RecyclerView.ViewHolder {

        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();
        private final ConstraintLayout userListLayout;
        private final TextView nameTextView;
        private final TextView phoneTextView;
        private final ImageView profileImageView;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.addUserName);
            phoneTextView = itemView.findViewById(R.id.addUserMobileNumber);
            profileImageView = itemView.findViewById(R.id.addCurrentUserImage);
            userListLayout = itemView.findViewById(R.id.manage_profile_name_container);
        }

        public void bind(Map<String, Object> contact) {
            String userId = (String) contact.get("userId");
            if (userId != null) {
                loadProfilePicture(userId);
            }
        }

        private void userClickOnStart(String newUserId) {
            userListLayout.setOnClickListener(v -> {
                // Open chat room activity with the selected ID (userId)
                if (newUserId != null) {
                    /*Intent intent = new Intent(context, ChatRoom.class);
                    intent.putExtra("newUserId", newUserId); // Pass the userId to the ChatRoomActivity
                    context.startActivity(intent);*/
                    loadChatroomIfPresentOrCreateNew(newUserId);
                }
            });
/*
            profileImageView.setOnClickListener(v -> zoomImageFromThumb(profileImageView, profileImageView.getDrawable()));
*/
            shortAnimationDuration = 400;
        }

        /*        private void loadChatroomIfPresentOrCreateNew(String userId) {
                    if (context == null) {
                        Log.e(TAG, "Context is null");
                        return;
                    }

                    ValidationUtils.showToast(context, "Checking for existing chat room...");

                    String currentUserId = FirebaseAuthUtils.getUserId(context);
                    if (currentUserId == null || currentUserId.isEmpty()) {
                        Log.e(TAG, "User ID is null or empty");
                        return;
                    }

                    FireStoreDatabaseUtils.getChatsCollection()
                            .whereArrayContains("participants", currentUserId)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    QuerySnapshot querySnapshot = task.getResult();
                                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                        // Filter the documents to find one where userId is also a participant
                                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                            List<String> participantIDs = (List<String>) document.get("participants");
                                            if (participantIDs != null && participantIDs.contains(userId)) {
                                                // Chat room found where both users are participants
                                                String chatRoomId = document.getId();
                                                ValidationUtils.showToast(context, "Existing chat room founded");
                                                navigateToChatRoom(chatRoomId);
                                                return;
                                            }
                                        }
                                    }

                                    // No chat room found where both users are participants, create a new one
                                    ValidationUtils.showToast(context, "No chat room found for both users, creating new one...");
                                    createNewChatRoom(userId);
                                } else {
                                    LoggerUtil.logError("Error loading chat room", task.getException());
                                    ValidationUtils.showToast(context, "Error loading chat room");
                                }
                            });
                }*/
        private void loadChatroomIfPresentOrCreateNew(String userId) {
            if (context == null) {
                Log.e(TAG, "Context is null");
                return;
            }

            ValidationUtils.showToast(context, "Checking for existing chat room...");

            String currentUserId = FirebaseAuthUtils.getUserId(context);
            if (currentUserId == null || currentUserId.isEmpty()) {
                Log.e(TAG, "User ID is null or empty");
                return;
            }

            FireStoreDatabaseUtils.getChatsCollection()
                    .whereArrayContains("participants", currentUserId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                // Filter the documents to find one where userId is also a participant
                                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                    ChatRoomModel chatRoom = document.toObject(ChatRoomModel.class);
                                    if (chatRoom != null && chatRoom.getParticipants().contains(userId)) {
                                        // Chat room found where both users are participants
                                        String chatRoomId = document.getId();
                                        ValidationUtils.showToast(context, "Existing chat room found");
                                        navigateToChatRoom(chatRoomId);
                                        return;
                                    }
                                }
                            }

                            // No chat room found where both users are participants, create a new one
                            ValidationUtils.showToast(context, "No chat room found for both users, creating new one...");
                            createNewChatRoom(userId);
                        } else {
                            LoggerUtil.logError("Error loading chat room", task.getException());
                            ValidationUtils.showToast(context, "Error loading chat room");
                        }
                    });
        }

        private void createNewChatRoom(String userId) {
            // Create a new chat room between the current user and the selected user
            FireStoreDatabaseUtils.createNewChat(FirebaseAuthUtils.getUserId(context), userId,
                    task -> {
                        if (task.isSuccessful()) {
                            // Retrieve the newly created chat room ID
                            String chatRoomId = task.getResult().getId();
                            navigateToChatRoom(chatRoomId);
                        } else {
                            LoggerUtil.logError("Error creating chat room", task.getException());
                        }
                    });
        }

        private void navigateToChatRoom(String chatRoomId) {
            Intent intent = new Intent(context, ChatRoom.class);
            intent.putExtra("chatRoomId", chatRoomId);
            context.startActivity(intent);
        }

        private void loadProfilePicture(String userId) {
            // Fetch user details from Firestore using userId
            FireStoreDatabaseUtils.getUserData(userId, (snapshot, error) -> {
                if (error != null) {
                    // Log error
                    LoggerUtil.logError("Error getting document", error);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    User otherUserModel = snapshot.toObject(User.class);
                    if (otherUserModel != null) {
                        // Update name and phoneTextView using fetched data
                        String name = otherUserModel.getUserName();
                        String phone = otherUserModel.getUserMobileNumber();
                        nameTextView.setText(name);
                        phoneTextView.setText(phone);
                        userClickOnStart(userId);
                        // Now load profile picture
                        // Assuming the profile picture URL is stored in userData with key "profilePictureUrl"
                        FireStoreDatabaseUtils storageHelper = new FireStoreDatabaseUtils();
                        StorageReference imageRef = storageHelper.getImageReference(userId); // Assuming you have a method to get StorageReference for the user's image

                        // Download the image from Firebase Storage
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Load the image using Glide or any other image loading library
                            ImageUtils.loadImage(context, uri.toString(), new ImageUtils.ImageLoadListener() {
                                @Override
                                public void onResourceReady(Drawable resource) {
                                    profileImageView.setImageDrawable(resource);
                                }

                                @Override
                                public void onLoadFailed() {
                                    // Handle load failure
                                    /*ValidationUtils.showToast(context, "Error occurred to load the Image");*/
                                    showPlaceholderImage(context.getResources());
                                }
                            });
                        }).addOnFailureListener(e -> showPlaceholderImage(context.getResources()));
                    }
                } else {
                    LoggerUtil.logError("No such document", new Exception("Document not found"));
                }
            });
        }

        private void showPlaceholderImage(Resources resources) {
            int placeholderDrawableId = R.drawable.user;
            Drawable placeholderDrawable = ResourcesCompat.getDrawable(resources, placeholderDrawableId, null);
            profileImageView.setImageDrawable(placeholderDrawable);
        }

        private void zoomImageFromThumb(final View thumbView, Drawable imageDrawable) {
            // If there's an animation in progress, cancel it immediately and
            // proceed with this one.
            if (currentAnimator != null) {
                currentAnimator.cancel();
            }

            // Load the high-resolution "zoomed-in" image.
            user_expanded_image_view.setImageDrawable(imageDrawable);


            // The start bounds are the global visible rectangle of the thumbnail,
            // and the final bounds are the global visible rectangle of the
            // container view. Set the container view's offset as the origin for the
            // bounds, since that's the origin for the positioning animation
            // properties (X, Y).
            thumbView.getGlobalVisibleRect(startBounds);
            user_expanded_image_view.getGlobalVisibleRect(finalBounds, globalOffset);
            startBounds.offset(-globalOffset.x, -globalOffset.y);
            finalBounds.offset(-globalOffset.x, -globalOffset.y);

            // Using the "center crop" technique, adjust the start bounds to be the
            // same aspect ratio as the final bounds. This prevents unwanted
            // stretching during the animation. Calculate the start scaling factor.
            // The end scaling factor is always 1.0.
            float startScale;
            if ((float) finalBounds.width() / finalBounds.height()
                    > (float) startBounds.width() / startBounds.height()) {
                // Extend start bounds horizontally.
                startScale = (float) startBounds.height() / finalBounds.height();
                float startWidth = startScale * finalBounds.width();
                float deltaWidth = (startWidth - startBounds.width()) / 2;
                startBounds.left -= (int) deltaWidth;
                startBounds.right += (int) deltaWidth;
            } else {
                // Extend start bounds vertically.
                startScale = (float) startBounds.width() / finalBounds.width();
                float startHeight = startScale * finalBounds.height();
                float deltaHeight = (startHeight - startBounds.height()) / 2;
                startBounds.top -= (int) deltaHeight;
                startBounds.bottom += (int) deltaHeight;
            }

            // Hide the thumbnail and show the zoomed-in view. When the animation
            // begins, it positions the zoomed-in view in the place of the
            // thumbnail.
            thumbView.setAlpha(0f);

            animateZoomToLargeImage(startBounds, finalBounds, startScale);
            setDismissLargeImageAnimation(thumbView, startBounds, startScale);
        }

        private void animateZoomToLargeImage(Rect startBounds, Rect finalBounds, float startScale) {
            user_expanded_image_view.setVisibility(View.VISIBLE);

            // Set the pivot point for SCALE_X and SCALE_Y transformations to the
            // top-left corner of the zoomed-in view. The default is the center of
            // the view.
            user_expanded_image_view.setPivotX(0f);
            user_expanded_image_view.setPivotY(0f);

            // Construct and run the parallel animation of the four translation and
            // scale properties: X, Y, SCALE_X, and SCALE_Y.
            AnimatorSet set = new AnimatorSet();
            set.play(ObjectAnimator.ofFloat(user_expanded_image_view, View.X,
                            startBounds.left, finalBounds.left))
                    .with(ObjectAnimator.ofFloat(user_expanded_image_view, View.Y,
                            startBounds.top, finalBounds.top))
                    .with(ObjectAnimator.ofFloat(user_expanded_image_view, View.SCALE_X,
                            startScale, 1f))
                    .with(ObjectAnimator.ofFloat(user_expanded_image_view, View.SCALE_Y,
                            startScale, 1f));
            set.setDuration(shortAnimationDuration);
            set.setInterpolator(new DecelerateInterpolator());
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    currentAnimator = null;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    currentAnimator = null;
                }
            });
            set.start();
            currentAnimator = set;
        }

        private void setDismissLargeImageAnimation(View thumbView, Rect startBounds, float startScale) {
            // When the zoomed-in image is tapped, it zooms down to the original
            // bounds and shows the thumbnail instead of the expanded image.
            user_expanded_image_view.setOnClickListener(view -> {
                if (currentAnimator != null) {
                    currentAnimator.cancel();
                }

                // Animate the four positioning and sizing properties in
                // parallel, back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator
                                .ofFloat(user_expanded_image_view, View.X, startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(user_expanded_image_view,
                                        View.Y, startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(user_expanded_image_view,
                                        View.SCALE_X, startScale))
                        .with(ObjectAnimator
                                .ofFloat(user_expanded_image_view,
                                        View.SCALE_Y, startScale));
                set.setDuration(shortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbView.setAlpha(1f);
                        user_expanded_image_view.setVisibility(View.GONE);
                        currentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbView.setAlpha(1f);
                        user_expanded_image_view.setVisibility(View.GONE);
                        currentAnimator = null;
                    }
                });
                set.start();
                currentAnimator = set;
            });
        }
    }
}
