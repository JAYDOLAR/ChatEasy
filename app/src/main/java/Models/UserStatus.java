package Models;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

public enum UserStatus {
    ONLINE("Online"),
    OFFLINE("Offline"),
    TYPING("typing..."),
    AWAY("Away");

    private final String displayName;

    @Contract(pure = true)
    UserStatus(String displayName) {
        this.displayName = displayName;
    }

    // Get the corresponding enum constant from a string representation
    public static UserStatus fromString(String text) {
        for (UserStatus status : UserStatus.values()) {
            if (status.displayName.equalsIgnoreCase(text)) {
                return status;
            }
        }
        return OFFLINE; // Default to OFFLINE if no match found
    }

    @Contract(pure = true)
    public String getDisplayName() {
        return displayName;
    }

    // Convert the enum constant to a string representation
    @Contract(pure = true)
    @NonNull
    @Override
    public String toString() {
        return displayName;
    }
}
