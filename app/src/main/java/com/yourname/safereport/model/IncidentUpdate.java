package com.yourname.safereport.model;

/**
 * Model for incident update subcollection item stored in incidents/{id}/updates/{id}.
 */
public class IncidentUpdate {
    private String id;
    private String message;
    private String status;
    private String updatedBy;
    private long timestamp;

    // Required empty constructor for Firestore
    public IncidentUpdate() {
    }

    public IncidentUpdate(String id, String message, String status, String updatedBy, long timestamp) {
        this.id = id;
        this.message = message;
        this.status = status;
        this.updatedBy = updatedBy;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
