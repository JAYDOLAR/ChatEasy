package Models;

import com.google.firebase.Timestamp;

import org.jetbrains.annotations.Contract;

public class MessageModel {
    private String messageId;
    private String senderId;
    private String receiverId;
    private MessageType messageType;
    private MessageStatus messageStatus;
    private String messageContent;
    private Timestamp messageTimestamp;

    // Constructor, getters, and setters

    @Contract(pure = true)
    public MessageModel() {
    }

    @Contract(pure = true)
    public MessageModel(String messageId, String senderId, String receiverId, MessageType messageType, MessageStatus messageStatus, String messageContent, Timestamp messageTimestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageType = messageType;
        this.messageStatus = messageStatus;
        this.messageContent = messageContent;
        this.messageTimestamp = messageTimestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public MessageStatus getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public Timestamp getMessageTimestamp() {
        return messageTimestamp;
    }

    public void setMessageTimestamp(Timestamp messageTimestamp) {
        this.messageTimestamp = messageTimestamp;
    }
}
