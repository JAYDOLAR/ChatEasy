package Utility;

import android.util.Log;

import com.google.firebase.firestore.FieldValue;

import org.jetbrains.annotations.Contract;

import java.util.HashMap;
import java.util.Map;

import Models.UserStatus;

// UserStatusManager class (updated)
public class UserStatusManager {
    private static final String TAG = "UserStatusManager";
    private final String userId;
    private UserStatus currentStatus = UserStatus.OFFLINE;

    @Contract(pure = true)
    public UserStatusManager(String userId) {
        this.userId = userId;
    }

    public void updateUserStatus(UserStatus status) {
        if (status == currentStatus) {
            return; // Avoid unnecessary updates
        }

        currentStatus = status;
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("lastActiveTime", FieldValue.serverTimestamp());
        FireStoreDatabaseUtils.getUserDocument(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User status updated successfully to " + status))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating user status", e));
    }

    public void setUserOnline() {
        updateUserStatus(UserStatus.ONLINE);
    }

    public void setUserOffline() {
        updateUserStatus(UserStatus.OFFLINE);
    }

    public void setUserAway() {
        updateUserStatus(UserStatus.AWAY);
    }

    public UserStatus getCurrentStatus() {
        return currentStatus;
    }
}
