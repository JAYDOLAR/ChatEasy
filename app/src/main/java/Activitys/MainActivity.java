package Activitys;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chateasy.R;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.StorageReference;

import Adapters.UsersAdapter;
import Models.ChatRoomModel;
import Utility.CustomViewUtility;
import Utility.FireStoreDatabaseUtils;
import Utility.FirebaseAuthUtils;
import Utility.ImageUtils;
import Utility.ValidationUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String KEY_RECYCLER_STATE = "recycler_state";
    private static final String KEY_IMAGE_LOADED = "image_loaded";

    private ExtendedFloatingActionButton addNewUser;
    private Toolbar mainAppControlView;
    private CoordinatorLayout coordinator_main_userPageView;
    private UsersAdapter adapter;
    private RecyclerView recyclerView;
    private LinearProgressIndicator updateNewTask;
    private AppBarLayout appBarLayout;
    private boolean isImageLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);

        initializeViews();

        if (savedInstanceState != null) {
            isImageLoaded = savedInstanceState.getBoolean(KEY_IMAGE_LOADED, false);
        }


        setupClickListeners();
        setupScrollBehavior();

        if (!isImageLoaded) {
            loadImage();
        }

        setupRecyclerView();
    }

    private void initializeViews() {
        coordinator_main_userPageView = findViewById(R.id.coordinator_main_userPage);
        mainAppControlView = findViewById(R.id.mainAppControl);
        addNewUser = findViewById(R.id.addNewUser);
        recyclerView = findViewById(R.id.userPresentList);
        updateNewTask = findViewById(R.id.updateNewTask);
        appBarLayout = findViewById(R.id.appbarLayout);
    }


    private void setupScrollBehavior() {
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                // Collapsed
                addNewUser.shrink();
            } else if (verticalOffset == 0) {
                // Expanded
                addNewUser.extend();
            }
        });
    }

    private void setupClickListeners() {
        if (addNewUser != null) {
            addNewUser.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SearchAndAddNewUser.class);
                startActivity(intent);
            });
        }
    }

    private void showLoadingState() {
        if (updateNewTask != null) {
            updateNewTask.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoadingState() {
        if (updateNewTask != null) {
            updateNewTask.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        if (coordinator_main_userPageView != null) {
            Snackbar.make(coordinator_main_userPageView, message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void loadImage() {
        showLoadingState();

        String userId = FirebaseAuthUtils.getUserId(this);
        if (userId == null || userId.isEmpty()) {
            hideLoadingState();
            showError("Unable to load profile image. User ID not found.");
            showPlaceholderImage();
            return;
        }

        FireStoreDatabaseUtils storageHelper = new FireStoreDatabaseUtils();
        StorageReference imageRef = storageHelper.getImageReference(userId);

        imageRef.getDownloadUrl()
                .addOnSuccessListener(uri -> ImageUtils.loadImage(MainActivity.this, uri.toString(),
                        new ImageUtils.ImageLoadListener() {
                            @Override
                            public Drawable onResourceReady(Drawable resource) {
                                hideLoadingState();
                                isImageLoaded = true;
                                renderProfileImage(resource);
                                return resource;
                            }

                            @Override
                            public void onLoadFailed() {
                                hideLoadingState();
                                showPlaceholderImage();
                            }
                        }))
                .addOnFailureListener(e -> {
                    hideLoadingState();
                    Log.e(TAG, "Failed to get image download URL", e);
                    showPlaceholderImage();
                });
    }

    private void renderProfileImage(Drawable drawable) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mainAppControlView != null) {
                MenuItem profileMainPic = mainAppControlView.getMenu().findItem(R.id.profile);
                if (profileMainPic != null) {
                    profileMainPic.setIcon(drawable);
                    profileMainPic.setOnMenuItemClickListener(item -> {
                        CustomViewUtility.BottomSheet bottomSheet = new CustomViewUtility.BottomSheet();
                        bottomSheet.show(getSupportFragmentManager(), TAG);
                        return true;
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
                    CustomViewUtility.BottomSheet bottomSheet = new CustomViewUtility.BottomSheet();
                    bottomSheet.show(getSupportFragmentManager(), TAG);
                    return true;
                });
                if (coordinator_main_userPageView != null) {
                    ValidationUtils.showSnackBar(coordinator_main_userPageView, "Failed to load image. Please try again.");
                }
            }
        }
    }

    private void setupRecyclerView() {
        try {
            String currentUserId = FirebaseAuthUtils.getUserId(getApplicationContext());
            if (currentUserId == null || currentUserId.isEmpty()) {
                Log.e(TAG, "User ID is null or empty");
                showError("Unable to load chat list. Please try again.");
                return;
            }

            Query query = FireStoreDatabaseUtils.getChatsCollection()
                    .whereArrayContains("participants", currentUserId);

            FirestoreRecyclerOptions<ChatRoomModel> options = new FirestoreRecyclerOptions.Builder<ChatRoomModel>()
                    .setQuery(query, ChatRoomModel.class)
                    .build();

            adapter = new UsersAdapter(options, getApplicationContext());
            recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            recyclerView.setAdapter(adapter);

            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    // Hide/show FAB based on scroll direction
                    if (dy > 0 && addNewUser.isExtended()) {
                        addNewUser.shrink();
                    } else if (dy < 0 && !addNewUser.isExtended()) {
                        addNewUser.extend();
                    }
                }
            });

            adapter.startListening();

        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
            showError("Unable to load chat list. Please try again.");
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IMAGE_LOADED, isImageLoaded);
        if (recyclerView.getLayoutManager() != null) {
            outState.putParcelable(KEY_RECYCLER_STATE,
                    recyclerView.getLayoutManager().onSaveInstanceState());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (recyclerView.getLayoutManager() != null) {
            recyclerView.getLayoutManager().onRestoreInstanceState(
                    savedInstanceState.getParcelable(KEY_RECYCLER_STATE));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null && !adapter.getSnapshots().isListening()) {
            adapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null && adapter.getSnapshots().isListening()) {
            adapter.stopListening();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}