package com.yourname.safereport.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yourname.safereport.data.local.AppDatabase;
import com.yourname.safereport.data.local.DraftIncident;
import com.yourname.safereport.model.Incident;
import com.yourname.safereport.model.IncidentUpdate;
import com.yourname.safereport.utils.Constants;
import com.yourname.safereport.utils.TrackingCodeGenerator;
import com.yourname.safereport.worker.UploadWorker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Main Incident repository handling Cloud Firestore CRUD, Firebase Storage photo uploads,
 * real-time snapshot listeners, and Room offline draft synchronization.
 */
public class IncidentRepository {

    public interface IncidentCallback {
        void onSuccess(Incident incident);
        void onError(String errorMessage);
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final FirebaseAuth auth;
    private final AppDatabase appDatabase;
    private final Context context;

    public IncidentRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.appDatabase = AppDatabase.getInstance(this.context);
    }

    public void submitIncident(
            String title,
            String description,
            String category,
            String severity,
            boolean isAnonymous,
            Uri photoUri,
            double latitude,
            double longitude,
            IncidentCallback callback) {

        FirebaseUser user = auth != null ? auth.getCurrentUser() : null;
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        String localUid = prefs.getString("user_uid", null);

        String activeUid = (user != null) ? user.getUid() : localUid;
        String reporterId = isAnonymous ? null : activeUid;

        String trackingCode = TrackingCodeGenerator.generateTrackingCode();
        long now = System.currentTimeMillis();

        // Save submitted tracking code locally
        Set<String> savedCodes = new HashSet<>(prefs.getStringSet("user_tracking_codes", new HashSet<>()));
        savedCodes.add(trackingCode);
        prefs.edit().putStringSet("user_tracking_codes", savedCodes).apply();

        // Use tracking code as document ID
        DocumentReference docRef = db.collection(Constants.COLLECTION_INCIDENTS).document(trackingCode);
        List<String> mediaUrls = new ArrayList<>();

        Incident incident = new Incident(
                trackingCode, title, description, category, severity,
                Constants.STATUS_SUBMITTED, reporterId, trackingCode,
                latitude, longitude, mediaUrls, now, "Unassigned"
        );

        if (photoUri != null) {
            uploadPhoto(photoUri, downloadUrl -> {
                if (downloadUrl != null) {
                    incident.getMediaUrls().add(downloadUrl);
                }
                saveToFirestore(docRef, incident, reporterId, callback);
            });
        } else {
            saveToFirestore(docRef, incident, reporterId, callback);
        }
    }

    private void saveToFirestore(DocumentReference docRef, Incident incident, String reporterId, IncidentCallback callback) {
        final boolean[] completed = {false};

        docRef.set(incident)
                .addOnSuccessListener(aVoid -> {
                    if (!completed[0]) {
                        completed[0] = true;
                        addIncidentUpdate(docRef.getId(), "Incident reported and submitted.",
                                Constants.STATUS_SUBMITTED, reporterId != null ? "Reporter" : "Anonymous Reporter");
                        callback.onSuccess(incident);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!completed[0]) {
                        completed[0] = true;
                        callback.onError("Failed to save report to Firebase: " + e.getLocalizedMessage());
                    }
                });

        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.postDelayed(() -> {
            if (!completed[0]) {
                completed[0] = true;
                callback.onError("Firebase save timed out. Please check your internet connection.");
            }
        }, 5000);
    }

    private interface PhotoUploadCallback {
        void onUrlReady(String downloadUrl);
    }

    private void uploadPhoto(Uri photoUri, PhotoUploadCallback callback) {
        String filename = "incidents/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference().child(filename);

        final boolean[] completed = {false};

        ref.putFile(photoUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            if (!completed[0]) {
                                completed[0] = true;
                                callback.onUrlReady(uri.toString());
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (!completed[0]) {
                                completed[0] = true;
                                callback.onUrlReady(null);
                            }
                        }))
                .addOnFailureListener(e -> {
                    if (!completed[0]) {
                        completed[0] = true;
                        callback.onUrlReady(null);
                    }
                });

        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.postDelayed(() -> {
            if (!completed[0]) {
                completed[0] = true;
                callback.onUrlReady(null);
            }
        }, 4000);
    }

    public LiveData<List<Incident>> getUserIncidents() {
        MutableLiveData<List<Incident>> liveData = new MutableLiveData<>();
        FirebaseUser currentUser = auth != null ? auth.getCurrentUser() : null;

        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        String uid = (currentUser != null) ? currentUser.getUid() : prefs.getString("user_uid", null);
        Set<String> trackingCodes = prefs.getStringSet("user_tracking_codes", new HashSet<>());

        Map<String, Incident> userIncidentMap = new HashMap<>();

        // Query by reporterId without orderBy (sorted in Java)
        if (uid != null) {
            db.collection(Constants.COLLECTION_INCIDENTS)
                    .whereEqualTo("reporterId", uid)
                    .addSnapshotListener((querySnapshots, error) -> {
                        if (querySnapshots != null) {
                            for (Incident incident : querySnapshots.toObjects(Incident.class)) {
                                if (incident.getId() != null) {
                                    userIncidentMap.put(incident.getId(), incident);
                                }
                            }
                            updateAndSortList(liveData, userIncidentMap);
                        }
                    });
        }

        // Direct document reads for locally saved tracking codes
        for (String code : trackingCodes) {
            if (code != null && !code.trim().isEmpty()) {
                db.collection(Constants.COLLECTION_INCIDENTS)
                        .document(code.trim())
                        .addSnapshotListener((docSnapshot, error) -> {
                            if (docSnapshot != null && docSnapshot.exists()) {
                                Incident incident = docSnapshot.toObject(Incident.class);
                                if (incident != null && incident.getId() != null) {
                                    userIncidentMap.put(incident.getId(), incident);
                                    updateAndSortList(liveData, userIncidentMap);
                                }
                            }
                        });
            }
        }

        return liveData;
    }

    private void updateAndSortList(MutableLiveData<List<Incident>> liveData, Map<String, Incident> map) {
        List<Incident> sortedList = new ArrayList<>(map.values());
        sortedList.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        liveData.setValue(sortedList);
    }

    public LiveData<Incident> getIncidentByTrackingCode(String trackingCode) {
        MutableLiveData<Incident> liveData = new MutableLiveData<>();

        if (trackingCode == null || trackingCode.trim().isEmpty()) {
            liveData.setValue(null);
            return liveData;
        }

        // Direct document lookup using tracking code as document ID
        db.collection(Constants.COLLECTION_INCIDENTS)
                .document(trackingCode.trim())
                .addSnapshotListener((docSnapshot, error) -> {
                    if (error != null || docSnapshot == null || !docSnapshot.exists()) {
                        liveData.setValue(null);
                        return;
                    }
                    Incident incident = docSnapshot.toObject(Incident.class);
                    liveData.setValue(incident);
                });

        return liveData;
    }

    public LiveData<Incident> getIncidentById(String incidentId) {
        return getIncidentByTrackingCode(incidentId);
    }

    public LiveData<List<Incident>> getAllIncidents() {
        MutableLiveData<List<Incident>> liveData = new MutableLiveData<>();

        db.collection(Constants.COLLECTION_INCIDENTS)
                .addSnapshotListener((querySnapshots, error) -> {
                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        List<Incident> list = querySnapshots.toObjects(Incident.class);
                        list.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                        liveData.setValue(list);
                    } else {
                        liveData.setValue(new ArrayList<>());
                    }
                });

        return liveData;
    }

    public LiveData<List<IncidentUpdate>> getIncidentUpdates(String incidentId) {
        MutableLiveData<List<IncidentUpdate>> liveData = new MutableLiveData<>();

        db.collection(Constants.COLLECTION_INCIDENTS)
                .document(incidentId)
                .collection(Constants.SUBCOLLECTION_UPDATES)
                .addSnapshotListener((querySnapshots, error) -> {
                    if (error != null || querySnapshots == null) {
                        liveData.setValue(new ArrayList<>());
                        return;
                    }
                    List<IncidentUpdate> updates = querySnapshots.toObjects(IncidentUpdate.class);
                    updates.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                    liveData.setValue(updates);
                });

        return liveData;
    }

    public void updateIncidentStatusAndComment(
            String incidentId, String newStatus, String assignee, String comment,
            String updatedBy, OperationCallback callback) {

        DocumentReference docRef = db.collection(Constants.COLLECTION_INCIDENTS).document(incidentId);

        docRef.update("status", newStatus, "assignedTo", assignee)
                .addOnSuccessListener(aVoid -> {
                    String message = (comment != null && !comment.trim().isEmpty())
                            ? comment.trim() : "Status updated to " + newStatus;
                    addIncidentUpdate(incidentId, message, newStatus, updatedBy);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(e.getLocalizedMessage()));
    }

    private void addIncidentUpdate(String incidentId, String message, String status, String updatedBy) {
        DocumentReference updateRef = db.collection(Constants.COLLECTION_INCIDENTS)
                .document(incidentId)
                .collection(Constants.SUBCOLLECTION_UPDATES)
                .document();

        IncidentUpdate update = new IncidentUpdate(
                updateRef.getId(), message, status, updatedBy, System.currentTimeMillis()
        );
        updateRef.set(update);
    }

    // Save draft offline in Room and enqueue WorkManager upload
    public void saveDraftOffline(
            String title, String description, String category, String severity,
            boolean isAnonymous, String photoPath, double latitude, double longitude,
            OperationCallback callback) {

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                DraftIncident draft = new DraftIncident(
                        title, description, category, severity, isAnonymous,
                        photoPath, latitude, longitude, System.currentTimeMillis()
                );
                appDatabase.draftIncidentDao().insertDraft(draft);

                Constraints constraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

                OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(UploadWorker.class)
                        .setConstraints(constraints)
                        .build();

                WorkManager.getInstance(context).enqueue(workRequest);

                callback.onSuccess();
            } catch (Exception e) {
                callback.onError(e.getLocalizedMessage());
            }
        });
    }
}
