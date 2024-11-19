package Activitys;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;

import org.jetbrains.annotations.Contract;

import Utility.AppLifecycleTracker;
import Utility.AppStateManager;
import Utility.FCMV1Manager;
import Utility.FirebaseAuthUtils;
import Utility.UserStatusManager;

// Create a custom Application class
public class ChatApplication extends Application {
    private UserStatusManager userStatusManager;
    private int activeActivities = 0;
    private static FCMV1Manager fcmManager;

    @Override
    public void onCreate() {
        super.onCreate();
        AppStateManager.init(this);
        // Initialize Firebase if not already done
        FirebaseApp.initializeApp(this);

        // Initialize UserStatusManager only if user is logged in and registered
        String userId = FirebaseAuthUtils.getUserId(getApplicationContext());
        if (userId != null) {
            userStatusManager = new UserStatusManager(userId);
        } else {
            // Log or handle the case where userId is null
            // This might indicate the user is not logged in yet
            Log.e("ChatApplication", "User ID is null. Cannot initialize UserStatusManager.");
        }

        // Register activity lifecycle callbacks
        ActivityLifecycleCallbacks activityLifecycleCallbacks = new ApplicationActivityLifecycleCallbacks();
        registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
        AppLifecycleTracker.init(this);
        fcmManager = new FCMV1Manager(this);
    }

    @Override
    public void onTerminate() {
        if (fcmManager != null) {
            fcmManager.shutdown();
        }
        super.onTerminate();
    }


    private class ApplicationActivityLifecycleCallbacks implements ActivityLifecycleCallbacks {
        @Contract(pure = true)
        @Override
        public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            if (activeActivities == 0) {
                // App went to foreground
                String userId = FirebaseAuthUtils.getUserId(getApplicationContext());
                if (userId != null && userStatusManager != null) {
                    userStatusManager.setUserOnline();
                } else {
                    Log.e("ChatApplication", "Cannot set user online. User ID or UserStatusManager is null.");
                }
            }
            activeActivities++;
        }

        @Contract(pure = true)
        @Override
        public void onActivityResumed(@NonNull Activity activity) {
        }

        @Contract(pure = true)
        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            activeActivities--;
            if (activeActivities == 0) {
                // App went to background
                String userId = FirebaseAuthUtils.getUserId(getApplicationContext());
                if (userId != null && userStatusManager != null) {
                    userStatusManager.setUserOffline();
                } else {
                    Log.e("ChatApplication", "Cannot set user offline. User ID or UserStatusManager is null.");
                }
            }
        }

        @Contract(pure = true)
        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        }

        @Contract(pure = true)
        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            // App went to background
            String userId = FirebaseAuthUtils.getUserId(getApplicationContext());
            if (userId != null && userStatusManager != null) {
                userStatusManager.setUserOffline();
            } else {
                Log.e("ChatApplication", "Cannot set user offline. User ID or UserStatusManager is null.");
            }
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        // App is about to be terminated
        String userId = FirebaseAuthUtils.getUserId(getApplicationContext());
        if (userId != null && userStatusManager != null) {
            userStatusManager.setUserOffline();
        } else {
            Log.e("ChatApplication", "Cannot set user offline. User ID or UserStatusManager is null.");
        }
    }

}