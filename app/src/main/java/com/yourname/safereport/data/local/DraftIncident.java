package com.yourname.safereport.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity representing an offline draft incident.
 */
@Entity(tableName = "draft_incidents")
public class DraftIncident {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String title;
    private String description;
    private String category;
    private String severity;
    private boolean isAnonymous;
    private String localPhotoPath;
    private double latitude;
    private double longitude;
    private long createdAt;

    public DraftIncident(String title, String description, String category, String severity,
                         boolean isAnonymous, String localPhotoPath, double latitude,
                         double longitude, long createdAt) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.severity = severity;
        this.isAnonymous = isAnonymous;
        this.localPhotoPath = localPhotoPath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }

    public String getLocalPhotoPath() {
        return localPhotoPath;
    }

    public void setLocalPhotoPath(String localPhotoPath) {
        this.localPhotoPath = localPhotoPath;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
