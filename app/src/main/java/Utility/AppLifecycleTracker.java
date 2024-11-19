package Utility;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

public class AppLifecycleTracker {
    private static int activeActivities = 0;

    public static void init(@NonNull Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                activeActivities++;
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                activeActivities--;
            }

            // Required but unused callbacks
            @Override public void onActivityCreated(@NonNull Activity activity, Bundle bundle) {}
            @Override public void onActivityResumed(@NonNull Activity activity) {}
            @Override public void onActivityPaused(@NonNull Activity activity) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {}
            @Override public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }

    @Contract(pure = true)
    public static boolean isAppInForeground() {
        return activeActivities > 0;
    }
}