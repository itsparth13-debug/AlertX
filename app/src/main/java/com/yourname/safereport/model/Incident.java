package com.yourname.safereport.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Safety Incident model stored in Firestore under incidents/{id}.
 */
public class Incident {
    private String id;
    private String title;
    private String description;
    private String category;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String status;   // SUBMITTED, UNDER_REVIEW, IN_PROGRESS, RESOLVED
    private String reporterId; // null if anonymous
    private String trackingCode;
    private double latitude;
    private double longitude;
    private List<String> mediaUrls;
    private long createdAt;
    private String assignedTo;

    // Required empty constructor for Firestore
    public Incident() {
        this.mediaUrls = new ArrayList<>();
    }

    public Incident(String id, String title, String description, String category, String severity,
                    String status, String reporterId, String trackingCode, double latitude,
                    double longitude, List<String> mediaUrls, long createdAt, String assignedTo) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.severity = severity;
        this.status = status;
        this.reporterId = reporterId;
        this.trackingCode = trackingCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mediaUrls = (mediaUrls != null) ? mediaUrls : new ArrayList<>();
        this.createdAt = createdAt;
        this.assignedTo = assignedTo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReporterId() {
        return reporterId;
    }

    public void setReporterId(String reporterId) {
        this.reporterId = reporterId;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }
}
