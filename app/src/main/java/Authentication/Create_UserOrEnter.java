package Authentication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.chateasy.R;
import com.google.firebase.FirebaseApp;

import java.util.Objects;

import Activitys.ChatRoom;
import Activitys.MainActivity;
import Utility.FirebaseAuthUtils;
import Utility.LoggerUtil;
import Utility.NotificationUtils;
import Utility.UserStatusManager;

public class Create_UserOrEnter extends AppCompatActivity {

    private boolean isLoading = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_user_or_enter);

        splashScreen.setKeepOnScreenCondition(() -> isLoading);

        // Check if the activity was started by clicking on a notification
        if (getIntent().hasExtra("chatRoomId")) {
            // Handle notification click
            handleNotificationClick(Objects.requireNonNull(getIntent().getExtras()));
        } else {
            // Otherwise, navigate to the correct screen
            navigateToCorrectScreen(savedInstanceState);
        }
    }

    private void handleNotificationClick(@NonNull Bundle extras) {
        if (getIntent().hasExtra("chatRoomId")) {
            NotificationUtils.handleNotificationClick(this, extras);
        } else {
            // Navigate to the correct screen if chat ID is null
            navigateToCorrectScreen(null);
        }
    }

    private void navigateToCorrectScreen(Bundle savedInstanceState) {
        if (FirebaseAuthUtils.isLoggedIn(this) && !FirebaseAuthUtils.hasRegistered(this)) {
            // User is logged in but user details are not filled
            loadFillUserDetailsFragment(savedInstanceState);
            if (FirebaseAuthUtils.getUserId(getApplicationContext()) == null) {
                LoggerUtil.logErrors("User is null : ", FirebaseAuthUtils.getUserId(getApplicationContext()));
            } else {
                /*FireStoreDatabaseUtils.updateUserLastActivity(FirebaseAuthUtils.getUserId(getApplicationContext()));*/
                UserStatusManager userStatusManager = new UserStatusManager(FirebaseAuthUtils.getUserId(getApplicationContext()));
                userStatusManager.setUserOnline();
            }
            isLoading = false;
            Log.e("aaaaaa", "FillUser");

        } else if (FirebaseAuthUtils.isLoggedIn(this) && FirebaseAuthUtils.hasRegistered(this)) {
            // Both login and user details are complete, move to the main activity
            /*FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(getApplicationContext()), UserStatus.ONLINE);*/
            NotificationUtils.getUserTokenAndSaveToFirestore();
            startActivity(new Intent(this, MainActivity.class));
            isLoading = false;
            finish();
            if (FirebaseAuthUtils.getUserId(getApplicationContext()) == null) {
                LoggerUtil.logErrors("User is null : ", FirebaseAuthUtils.getUserId(getApplicationContext()));
            } else {
/*
                FireStoreDatabaseUtils.updateUserLastActivity(FirebaseAuthUtils.getUserId(getApplicationContext()));
*/
            }
            Log.e("aaaaaa", "MainPage");

        } else {
            // User is not logged in, load Welcome_Users fragment
            loadWelcomeFragment(savedInstanceState);
            isLoading = false;
            Log.e("aaaaaa", "Welcome");

        }
    }

    private void loadWelcomeFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.allForUserCreation, new Welcome_Users())
                    .commit();
        }
    }

    private void loadFillUserDetailsFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            // Create a new instance of Fill_User_Details fragment
            Fill_User_Details fragment = new Fill_User_Details();

            // Pass data to the fragment using a Bundle
            Bundle bundle = new Bundle();
            String phoneNumber = FirebaseAuthUtils.getUserIdPhoneNumber(this);
            bundle.putString("phoneNumber", phoneNumber);
            Log.e("number form ::: ", phoneNumber + "  from Create_UserOrEnter...");
            fragment.setArguments(bundle);

            // Replace the fragment transaction
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.allForUserCreation, fragment)
                    .commit();
        }
    }

    /*private void openChatRoom(String chatId) {
        if (FirebaseAuthUtils.getUserId(getApplicationContext()) == null) {
            LoggerUtil.logErrors("User is null : ", FirebaseAuthUtils.getUserId(getApplicationContext()));
        } else {
            *//*FireStoreDatabaseUtils.updateUserStatus(FirebaseAuthUtils.getUserId(getApplicationContext()), UserStatus.ONLINE);*//*
        }
        Intent intent = new Intent(this, ChatRoom.class);
        intent.putExtra("chatRoomId", chatId);
        startActivity(intent);
        finish(); // Optionally, finish this activity after opening the chat room
    }*/
}
