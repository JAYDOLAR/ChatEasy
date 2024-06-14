/*
package Activitys;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chateasy.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Adapters.UserAdapter;
import Utility.CustomViewUtility;
import Utility.FireStoreDatabaseUtils;
import Utility.FirebaseAuthUtils;
import Utility.ImageUtils;
import Utility.LoggerUtil;
import Utility.ValidationUtils;

public class MainActivity extends AppCompatActivity {

    private ExtendedFloatingActionButton addNewUser;
    private Toolbar mainAppControlView;
    private CoordinatorLayout coordinator_main_userPageView;
    private final String TAG = "MainActivity";
    private UserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);

        initializeViews();
        loadImage();
        loadChatroomIfPresentOrCreateNew();

        if (addNewUser != null) {
            addNewUser.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchAndAddNewUser.class)));
        }
    }

    private void initializeViews() {
        coordinator_main_userPageView = findViewById(R.id.coordinator_main_userPage);
        mainAppControlView = findViewById(R.id.mainAppControl);
        addNewUser = findViewById(R.id.addNewUser);
        RecyclerView recyclerView = findViewById(R.id.userPresentList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        FirebaseFirestore.getInstance();
        adapter = new UserAdapter(this); // Initialize the adapter
        recyclerView.setAdapter(adapter);
    }

    private void loadImage() {
        FireStoreDatabaseUtils storageHelper = new FireStoreDatabaseUtils();
        StorageReference imageRef = storageHelper.getImageReference(FirebaseAuthUtils.getUserId(this));

        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            ImageUtils.loadImage(MainActivity.this, uri.toString(), new ImageUtils.ImageLoadListener() {
                @Override
                public void onResourceReady(Drawable resource) {
                    renderProfileImage(resource);
                }

                @Override
                public void onLoadFailed() {
                    showPlaceholderImage();
                }
            });
        }).addOnFailureListener(e -> showPlaceholderImage());
    }

    private void renderProfileImage(Drawable drawable) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mainAppControlView != null) {
                MenuItem profileMainPic = mainAppControlView.getMenu().findItem(R.id.profile);
                if (profileMainPic != null) {
                    profileMainPic.setIcon(drawable);
                    profileMainPic.setOnMenuItemClickListener(item -> {
                        new CustomViewUtility.BottomSheet().show(getSupportFragmentManager(), TAG);
                        return false;
                    });
                }
            }
        });
    }

    private void showPlaceholderImage() {
        if (mainAppControlView != null) {
            int placeholderDrawableId = R.drawable.user;
            Drawable placeholderDrawable = ResourcesCompat.getDrawable(getResources(), placeholderDrawableId, null);
            MenuItem profileMainPic = mainAppControlView.getMenu().findItem(R.id.profile);

            if (profileMainPic != null) {
                profileMainPic.setIcon(placeholderDrawable);
                profileMainPic.setOnMenuItemClickListener(item -> {
                    new CustomViewUtility.BottomSheet().show(getSupportFragmentManager(), "");
                    return false;
                });
                ValidationUtils.showSnackBar(coordinator_main_userPageView, "Failed to load image. Please try again.");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void loadChatroomIfPresentOrCreateNew() {
        String currentUserId = FirebaseAuthUtils.getUserId(getApplicationContext());
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
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                List<String> participantIDs = (List<String>) document.get("participants");
                                if (participantIDs != null) {
                                    // Iterate over participantIDs to find the other user's ID
                                    for (String participantID : participantIDs) {
                                        if (!participantID.equals(currentUserId)) {
                                            // Add the other user's ID to the adapter
                                            Map<String, Object> userData = new HashMap<>();
                                            userData.put("userId", participantID);
                                            adapter.addUser(userData);
                                            adapter.notifyDataSetChanged();
                                        }
                                    }
                                }
                            }
                        } else {
                            ValidationUtils.showToast(getApplicationContext(), "No chat rooms found for the current user.");
                        }
                    } else {
                        LoggerUtil.logError("Error loading chat rooms", task.getException());
                        ValidationUtils.showToast(getApplicationContext(), "Error loading chat rooms");
                    }
                });
    }
}
*/


package Activitys;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chateasy.R;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.StorageReference;

import Adapters.UsersAdapter;
import Models.ChatRoomModel;
import Models.UserStatus;
import Utility.CustomViewUtility;
import Utility.FireStoreDatabaseUtils;
import Utility.FirebaseAuthUtils;
import Utility.ImageUtils;
import Utility.LoggerUtil;
import Utility.ValidationUtils;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private ExtendedFloatingActionButton addNewUser;
    private Toolbar mainAppControlView;
    private CoordinatorLayout coordinator_main_userPageView;
    private UsersAdapter adapter;
    private RecyclerView recyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);

        initializeViews();
        loadImage();
        setupRecyclerView();

        if (addNewUser != null) {
            addNewUser.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchAndAddNewUser.class)));
        }
    }

    private void initializeViews() {
        coordinator_main_userPageView = findViewById(R.id.coordinator_main_userPage);
        mainAppControlView = findViewById(R.id.mainAppControl);
        addNewUser = findViewById(R.id.addNewUser);
        recyclerView = findViewById(R.id.userPresentList);
    }

    private void loadImage() {
        FireStoreDatabaseUtils storageHelper = new FireStoreDatabaseUtils();
        StorageReference imageRef = storageHelper.getImageReference(FirebaseAuthUtils.getUserId(this));

        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            ImageUtils.loadImage(MainActivity.this, uri.toString(), new ImageUtils.ImageLoadListener() {
                @Override
                public void onResourceReady(Drawable resource) {
                    renderProfileImage(resource);
                }

                @Override
                public void onLoadFailed() {
                    showPlaceholderImage();
                }
            });
        }).addOnFailureListener(e -> showPlaceholderImage());
    }

    private void renderProfileImage(Drawable drawable) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mainAppControlView != null) {
                MenuItem profileMainPic = mainAppControlView.getMenu().findItem(R.id.profile);
                if (profileMainPic != null) {
                    profileMainPic.setIcon(drawable);
                    profileMainPic.setOnMenuItemClickListener(item -> {
                        new CustomViewUtility.BottomSheet().show(getSupportFragmentManager(), TAG);
                        return false;
                    });
                }
            }
        });
    }

    private void showPlaceholderImage() {
        if (mainAppControlView != null) {
            int placeholderDrawableId = R.drawable.user;
            Drawable placeholderDrawable = ResourcesCompat.getDrawable(getResources(), placeholderDrawableId, null);
            MenuItem profileMainPic = mainAppControlView.getMenu().findItem(R.id.profile);

            if (profileMainPic != null) {
                profileMainPic.setIcon(placeholderDrawable);
                profileMainPic.setOnMenuItemClickListener(item -> {
                    new CustomViewUtility.BottomSheet().show(getSupportFragmentManager(), "");
                    return false;
                });
                ValidationUtils.showSnackBar(coordinator_main_userPageView, "Failed to load image. Please try again.");
            }
        }
    }

    private void setupRecyclerView() {

        String currentUserId = FirebaseAuthUtils.getUserId(getApplicationContext());
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "User ID is null or empty");
            return;
        }

        Query query = FireStoreDatabaseUtils.getChatsCollection()
                .whereArrayContains("participants", currentUserId);

        FirestoreRecyclerOptions<ChatRoomModel> options = new FirestoreRecyclerOptions.Builder<ChatRoomModel>()
                .setQuery(query, ChatRoomModel.class).build();

        adapter = new UsersAdapter(options, getApplicationContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(adapter);
        adapter.startListening();

        /*final ItemTouchHelper itemTouchHelper = getItemTouchHelper();
        itemTouchHelper.attachToRecyclerView(recyclerView);*/
    }

/*    @NonNull
    private ItemTouchHelper getItemTouchHelper() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Perform action when swiped (e.g., mark as read)
                int position = viewHolder.getBindingAdapterPosition();
                // Update your dataset to mark the item as read

                // Optionally, remove the swiped item from the adapter
                //adapter.removeItem(position);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    // Get the original background color of the item
                    int originalBackgroundColor = com.google.android.material.R.attr.colorSurfaceVariant;
                    // Set the default radius for the item corners
                    float cornerRadius = 25; // Modify this value as needed

                    // Calculate the bounds for the item background
                    RectF backgroundBounds = new RectF(viewHolder.itemView.getLeft(), viewHolder.itemView.getTop(), viewHolder.itemView.getRight(), viewHolder.itemView.getBottom());

                    // Create a Paint object for the item background
                    Paint backgroundPaint = new Paint();
                    backgroundPaint.setColor(originalBackgroundColor);
                    backgroundPaint.setAntiAlias(true);

                    // Draw the background color for the item with rounded corners
                    c.drawRoundRect(backgroundBounds, cornerRadius, cornerRadius, backgroundPaint);

                    // Define the icon to be drawn on the canvas based on swipe direction
                    Drawable icon;
                    int iconMargin = 0;
                    int iconTop = 0;
                    int iconBottom = 0;
                    int iconLeft = 0;
                    int iconRight = 0;

                    if (dX > 0) { // Swiping to the right
                        icon = ContextCompat.getDrawable(MainActivity.this, R.drawable.archive_2);
                        assert icon != null;
                        iconMargin = (viewHolder.itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                        iconTop = viewHolder.itemView.getTop() + (viewHolder.itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                        iconBottom = iconTop + icon.getIntrinsicHeight();
                        iconLeft = viewHolder.itemView.getLeft() + iconMargin;
                        iconRight = viewHolder.itemView.getLeft() + iconMargin + icon.getIntrinsicWidth();
                    } else { // Swiping to the left
                        icon = ContextCompat.getDrawable(MainActivity.this, R.drawable.symbol_info_24);
                        assert icon != null;
                        iconMargin = (viewHolder.itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                        iconTop = viewHolder.itemView.getTop() + (viewHolder.itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                        iconBottom = iconTop + icon.getIntrinsicHeight();
                        iconRight = viewHolder.itemView.getRight() - iconMargin;
                        iconLeft = viewHolder.itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                    }

                    // Set the bounds for the icon
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                    // Draw the icon on the canvas
                    icon.draw(c);
                }
            }


            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                // Restore the original state of the view when swipe is cleared
                viewHolder.itemView.setAlpha(1.0f);
                viewHolder.itemView.setTranslationX(0);
            }
        };


        return new ItemTouchHelper(simpleCallback);
    }*/

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null)
            adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null)
            adapter.stopListening();
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
    public void onResume() {
        super.onResume();
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (FirebaseAuthUtils.getUserId(getApplicationContext()) == null) {
            LoggerUtil.logErrors("User is null : ", FirebaseAuthUtils.getUserId(getApplicationContext()));
        } else {
            FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(getApplicationContext()), UserStatus.ONLINE);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

