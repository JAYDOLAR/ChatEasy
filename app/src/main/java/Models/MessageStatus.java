package Models;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

public enum MessageStatus {
    SENDING("Sending"),
    DELIVERED("Delivered"),
    SEEN("Seen"),
    RECEIVED("Received"),
    ERROR("Error");

    private final String displayName;

    @Contract(pure = true)
    MessageStatus(String displayName) {
        this.displayName = displayName;
    }

    // Get the corresponding enum constant from a string representation
    public static MessageStatus fromString(String text) {
        for (MessageStatus status : MessageStatus.values()) {
            if (status.displayName.equalsIgnoreCase(text)) {
                return status;
            }
        }
        return SENDING; // Default to SENDING if no match found
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
