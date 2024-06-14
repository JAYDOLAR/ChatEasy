package Utility;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
        return currentUser.getUid(); // No need for null check, getUid() already returns an empty string if currentUser is null
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
        AuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential, callback);
    }

    private static void signInWithPhoneAuthCredential(AuthCredential credential, VerificationCallback callback) {
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
        editor.apply(); // Use apply() instead of commit()
    }

    public static void setLoggedIn(boolean isLoggedIn, String currentUserId, String CurrentRegisteredNumber, boolean hasRegistered, Context context) {
        if (isLoggedIn) {
            // Set the user ID when logging in
            setUserId(currentUserId, CurrentRegisteredNumber, hasRegistered, context);
        } else {
            // Clear the user ID when logging out
            clearUserId(context);
            logout(); // Call logout() separately when needed
        }
    }

    private static void clearUserId(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(USER_ID_KEY);
        editor.remove(KEY_PHONE_NUMBER);
        editor.remove(HAS_REGISTERED_KEY);
        editor.apply(); // Use apply() instead of commit()
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

    // Extract common code for creating PhoneAuthOptions.Builder
    private static PhoneAuthOptions.Builder createPhoneAuthOptionsBuilder(Activity activity, String phoneNumber, PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks) {
        return PhoneAuthOptions.newBuilder(FirebaseAuthUtils.auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks);
    }
   /* public static boolean isRegistered(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return hasRegistered(context);
    }*/

    /*public static void setRegistered(boolean hasRegistered, String currentUserId, Context context) {
        if (Objects.equals(getUserId(context), currentUserId)) {
            sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(HAS_REGISTERED_KEY, hasRegistered);
            editor.apply(); // Use apply() instead of commit()
        }
    }*/

    public interface VerificationCallback {
        void onVerificationCompleted(FirebaseUser user);

        void onVerificationFailed(Exception exception);
    }
}