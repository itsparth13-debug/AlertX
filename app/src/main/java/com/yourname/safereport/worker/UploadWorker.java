package com.yourname.safereport.worker;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.yourname.safereport.data.local.AppDatabase;
import com.yourname.safereport.data.local.DraftIncident;
import com.yourname.safereport.model.Incident;
import com.yourname.safereport.utils.Constants;
import com.yourname.safereport.utils.TrackingCodeGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Background WorkManager Worker (Java Worker) that syncs offline draft incidents
 * from Room database to Firebase Storage and Cloud Firestore when internet is available.
 */
public class UploadWorker extends Worker {

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(context);
        List<DraftIncident> drafts = db.draftIncidentDao().getAllDrafts();

        if (drafts == null || drafts.isEmpty()) {
            return Result.success();
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        for (DraftIncident draft : drafts) {
            try {
                String photoUrl = null;
                if (draft.getLocalPhotoPath() != null && !draft.getLocalPhotoPath().isEmpty()) {
                    File file = new File(draft.getLocalPhotoPath());
                    if (file.exists()) {
                        String filename = "incidents/" + UUID.randomUUID().toString() + ".jpg";
                        StorageReference ref = storage.getReference().child(filename);
                        Tasks.await(ref.putFile(Uri.fromFile(file)));
                        Uri downloadUri = Tasks.await(ref.getDownloadUrl());
                        photoUrl = downloadUri.toString();
                    }
                }

                List<String> mediaUrls = new ArrayList<>();
                if (photoUrl != null) {
                    mediaUrls.add(photoUrl);
                }

                String reporterId = (draft.isAnonymous() || currentUser == null) ? null : currentUser.getUid();
                String trackingCode = TrackingCodeGenerator.generateTrackingCode();

                DocumentReference docRef = firestore.collection(Constants.COLLECTION_INCIDENTS).document();
                Incident incident = new Incident(
                        docRef.getId(),
                        draft.getTitle(),
                        draft.getDescription(),
                        draft.getCategory(),
                        draft.getSeverity(),
                        Constants.STATUS_SUBMITTED,
                        reporterId,
                        trackingCode,
                        draft.getLatitude(),
                        draft.getLongitude(),
                        mediaUrls,
                        draft.getCreatedAt(),
                        "Unassigned"
                );

                Tasks.await(docRef.set(incident));

                // Delete synced draft from Room
                db.draftIncidentDao().deleteDraft(draft);

            } catch (ExecutionException | InterruptedException e) {
                return Result.retry();
            } catch (Exception e) {
                // Skip invalid draft or fail
            }
        }

        return Result.success();
    }
}
