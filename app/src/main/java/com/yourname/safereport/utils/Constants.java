package com.yourname.safereport.utils;

/**
 * Global application constants.
 */
public class Constants {
    // Firestore Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_INCIDENTS = "incidents";
    public static final String SUBCOLLECTION_UPDATES = "updates";

    // User Roles
    public static final String ROLE_USER = "USER";
    public static final String ROLE_SECURITY = "SECURITY";
    public static final String ROLE_ADMIN = "ADMIN";

    // Incident Severities
    public static final String SEVERITY_LOW = "LOW";
    public static final String SEVERITY_MEDIUM = "MEDIUM";
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_CRITICAL = "CRITICAL";

    // Incident Statuses
    public static final String STATUS_SUBMITTED = "SUBMITTED";
    public static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_RESOLVED = "RESOLVED";

    // Incident Categories
    public static final String CAT_HARASSMENT = "Harassment";
    public static final String CAT_THEFT = "Theft";
    public static final String CAT_FIRE = "Fire";
    public static final String CAT_MEDICAL = "Medical Emergency";
    public static final String CAT_INFRASTRUCTURE = "Infrastructure Hazard";
    public static final String CAT_OTHER = "Other";

    // Notifications
    public static final String NOTIFICATION_CHANNEL_ID = "safereport_updates_channel";
    public static final String NOTIFICATION_CHANNEL_NAME = "Incident Status Updates";
    public static final int NOTIFICATION_ID = 1001;

    // Preferences
    public static final String PREF_NAME = "SafeReportPrefs";
    public static final String PREF_USER_ROLE = "user_role";
}
