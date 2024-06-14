package Models;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

public enum MessageType {
    TEXT("Text"),
    IMAGE("Image"),
    VIDEO("Video"),
    AUDIO("Audio"),
    FILE("File"),
    LOCATION("Location"),
    OTHER("Other");

    private final String displayName;

    MessageType(String displayName) {
        this.displayName = displayName;
    }

    // Get the corresponding enum constant from a string representation
    public static MessageType fromString(String text) {
        for (MessageType messageType : MessageType.values()) {
            if (messageType.displayName.equalsIgnoreCase(text)) {
                return messageType;
            }
        }
        return TEXT; // If no matching enum constant BY DEFAULT 'TEXT'
    }

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

    // Check if the message type is a media message (image, video, audio)
    @Contract(pure = true)
    public boolean isMediaMessage() {
        return this == IMAGE || this == VIDEO || this == AUDIO;
    }

    // Check if the message type is a file message (file or other attachments)
    @Contract(pure = true)
    public boolean isFileMessage() {
        return this == FILE || this == OTHER;
    }

    // Check if the message type is a location message
    @Contract(pure = true)
    public boolean isLocationMessage() {
        return this == LOCATION;
    }

    // Check if the message type is other than the provided types
    @Contract(pure = true)
    public boolean isOtherType() {
        return this == OTHER;
    }
}
