package com.yourname.safereport.model;

/**
 * User profile model stored in Firestore under users/{uid}.
 */
public class User {
    private String uid;
    private String name;
    private String email;
    private String role; // USER, SECURITY, ADMIN
    private String fcmToken;

    // Required empty constructor for Firestore deserialization
    public User() {
    }

    public User(String uid, String name, String email, String role, String fcmToken) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.fcmToken = fcmToken;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
