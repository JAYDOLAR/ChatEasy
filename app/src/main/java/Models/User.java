package Models;

import com.google.firebase.Timestamp;

import org.jetbrains.annotations.Contract;

import java.util.Date;

public class User {
    private String userId;
    private String userName;
    private String userMobileNumber;
    private String userEmail;
    private String userAbout;
    private Date userDOB;
    private Timestamp registeredTime;
    private Timestamp lastLoginTime;
    private Timestamp lastActiveTime;

    private UserStatus status;

    private String fcmToken;

    // Constructor, getters, and setters

    @Contract(pure = true)
    public User() {
    }

    @Contract(pure = true)
    public User(String userId, String userName, String userMobileNumber, String userEmail, String userAbout, Date userDOB, Timestamp registeredTime, Timestamp lastLoginTime, Timestamp lastActiveTime, UserStatus status, String fcmToken) {
        this.userId = userId;
        this.userName = userName;
        this.userMobileNumber = userMobileNumber;
        this.userEmail = userEmail;
        this.userAbout = userAbout;
        this.userDOB = userDOB;
        this.registeredTime = registeredTime;
        this.lastLoginTime = lastLoginTime;
        this.lastActiveTime = lastActiveTime;
        this.status = status;
        this.fcmToken = fcmToken;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserMobileNumber() {
        return userMobileNumber;
    }

    public void setUserMobileNumber(String userMobileNumber) {
        this.userMobileNumber = userMobileNumber;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserAbout() {
        return userAbout;
    }

    public void setUserAbout(String userAbout) {
        this.userAbout = userAbout;
    }

    public Date getUserDOB() {
        return userDOB;
    }

    public void setUserDOB(Date userDOB) {
        this.userDOB = userDOB;
    }

    public Timestamp getRegisteredTime() {
        return registeredTime;
    }

    public void setRegisteredTime(Timestamp registeredTime) {
        this.registeredTime = registeredTime;
    }

    public Timestamp getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(Timestamp lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public Timestamp getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(Timestamp lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
