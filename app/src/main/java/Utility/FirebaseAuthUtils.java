package Utility;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class FirebaseAuthUtils {

    private static final String SHARED_PREFS_NAME = "MyPrefs";
    private static final String USER_ID_KEY = "userId";
    private static final String HAS_REGISTERED_KEY = "hasRegistered";
    private static final String KEY_PHONE_NUMBER = "phone_number";
    // Create a local variable for FirebaseAuth instance
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static SharedPreferences sharedPreferences;

    public static FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public static String getCurrentUserId() {
        FirebaseUser currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    public static void sendVerificationCode(Activity activity, String phoneNumber, PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks) {
        // Reuse PhoneAuthOptions.Builder
        PhoneAuthOptions options = createPhoneAuthOptionsBuilder(activity, phoneNumber, callbacks).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public static void resendVerificationCode(Activity activity, String phoneNumber, PhoneAuthProvider.ForceResendingToken token, PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks) {
        // Reuse PhoneAuthOptions.Builder
        PhoneAuthOptions options = createPhoneAuthOptionsBuilder(activity, phoneNumber, callbacks)
                .setForceResendingToken(token)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public static void verifyPhoneNumberWithCode(String code, String verificationId, VerificationCallback callback) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential, callback);
    }

    private static void signInWithPhoneAuthCredential(PhoneAuthCredential credential, VerificationCallback callback) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        if (callback != null) {
                            callback.onVerificationCompleted(user);
                        }
                    } else {
                        if (callback != null) {
                            callback.onVerificationFailed(task.getException());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseAuthUtils", "Authentication failed", e);
                });
    }

    public static String getUserId(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(USER_ID_KEY, null);
    }

    public static String getUserIdPhoneNumber(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_PHONE_NUMBER, null);
    }

    public static void setUserId(String userId, String phoneNumber, boolean hasRegistered, Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(USER_ID_KEY, userId);
        editor.putString(KEY_PHONE_NUMBER, phoneNumber);
        editor.putBoolean(HAS_REGISTERED_KEY, hasRegistered);
        editor.apply();
    }

    public static void setLoggedIn(boolean isLoggedIn, String currentUserId, String CurrentRegisteredNumber, boolean hasRegistered, Context context) {
        if (isLoggedIn) {

            setUserId(currentUserId, CurrentRegisteredNumber, hasRegistered, context);
        } else {
            clearUserId(context);
            logout();
        }
    }

    private static void clearUserId(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(USER_ID_KEY);
        editor.remove(KEY_PHONE_NUMBER);
        editor.remove(HAS_REGISTERED_KEY);
        editor.clear().apply();
    }

    public static boolean hasRegistered(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(HAS_REGISTERED_KEY, false);
    }

    public static boolean isLoggedIn(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return getUserId(context) != null;
    }

    public static void logout() {
        auth.signOut();
    }

    // Add this utility method
    public static void safeLogout(OnLogoutListener listener) {
        try {
            String currentUserId = getCurrentUserId();
            if (currentUserId != null) {
                UserStatusManager userStatusManager = new UserStatusManager(currentUserId);
                userStatusManager.setUserAway();
            }

            NotificationUtils.saveTokenToFirestore("")
                    .addOnCompleteListener(task -> {

                        if (listener != null) {
                            listener.onLogoutComplete();
                        }
                    });
        } catch (Exception e) {
            LoggerUtil.logErrors("Logout Error: ", e.getMessage());
            if (listener != null) {
                listener.onLogoutError(e);
            }
        }
    }

    // Listener interface for logout events
    public interface OnLogoutListener {
        void onLogoutComplete();

        void onLogoutError(Exception e);
    }

    // Extract common code for creating PhoneAuthOptions.Builder
    private static PhoneAuthOptions.Builder createPhoneAuthOptionsBuilder(Activity activity, String phoneNumber, PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks) {
        return PhoneAuthOptions.newBuilder(FirebaseAuthUtils.auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks);
    }

    public interface VerificationCallback {
        void onVerificationCompleted(FirebaseUser user);

        void onVerificationFailed(Exception exception);
    }
}