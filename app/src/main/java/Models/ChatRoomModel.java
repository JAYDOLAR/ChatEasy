package Models;

import com.google.firebase.Timestamp;

import org.jetbrains.annotations.Contract;

import java.util.List;

public class ChatRoomModel {
    private List<String> participants;
    private String lastMessage;
    private int numberOfUnreadMessages;
    private Timestamp chatRoomCreatedTimestamp;
    // Constructor, getters, and setters

    @Contract(pure = true)
    public ChatRoomModel() {
    }

    @Contract(pure = true)
    public ChatRoomModel(List<String> participants, String lastMessage, int numberOfUnreadMessages, Timestamp chatRoomCreatedTimestamp) {
        this.participants = participants;
        this.lastMessage = lastMessage;
        this.numberOfUnreadMessages = numberOfUnreadMessages;
        this.chatRoomCreatedTimestamp = chatRoomCreatedTimestamp;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public int getNumberOfUnreadMessages() {
        return numberOfUnreadMessages;
    }

    public void setNumberOfUnreadMessages(int numberOfUnreadMessages) {
        this.numberOfUnreadMessages = numberOfUnreadMessages;
    }

    public Timestamp getChatRoomCreatedTimestamp() {
        return chatRoomCreatedTimestamp;
    }

    public void setChatRoomCreatedTimestamp(Timestamp chatRoomCreatedTimestamp) {
        this.chatRoomCreatedTimestamp = chatRoomCreatedTimestamp;
    }

    public static class Participant {
        private UserStatus userStatus;
        private int messageCount;

        // Constructors, getters, and setters for Participant class

        @Contract(pure = true)
        public Participant(String userId, UserStatus userStatus, int messageCount) {
            this.userStatus = userStatus;
            this.messageCount = messageCount;
        }

        public UserStatus getUserStatus() {
            return userStatus;
        }

        public void setUserStatus(UserStatus userStatus) {
            this.userStatus = userStatus;
        }

        public int getMessageCount() {
            return messageCount;
        }

        public void setMessageCount(int messageCount) {
            this.messageCount = messageCount;
        }
    }
}
