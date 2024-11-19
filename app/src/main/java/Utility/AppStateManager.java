package Utility;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

public class AppStateManager {
    private static int numStarted = 0;

    public static void init(@NonNull Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                numStarted++;
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                numStarted--;
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }

    @Contract(pure = true)
    public static boolean isAppInForeground() {
        return numStarted > 0;
    }
}