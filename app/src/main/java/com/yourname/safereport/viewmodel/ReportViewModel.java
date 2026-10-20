package com.yourname.safereport.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.yourname.safereport.model.Incident;
import com.yourname.safereport.repository.IncidentRepository;
import com.yourname.safereport.utils.Constants;

/**
 * ViewModel managing Incident Creation, Photo attachment, Location attachment, SOS submission, and offline Drafts.
 * Includes complete input validation for title, description, category, and severity.
 */
public class ReportViewModel extends AndroidViewModel {

    private final IncidentRepository repository;

    private final MutableLiveData<Uri> photoUriState = new MutableLiveData<>(null);
    private final MutableLiveData<String> localPhotoPathState = new MutableLiveData<>(null);
    private final MutableLiveData<double[]> locationState = new MutableLiveData<>(new double[]{0.0, 0.0});
    private final MutableLiveData<Incident> submitSuccessState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> draftSavedState = new MutableLiveData<>();
    private final MutableLiveData<String> errorState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingState = new MutableLiveData<>(false);

    public ReportViewModel(@NonNull Application application) {
        super(application);
        this.repository = new IncidentRepository(application);
    }

    public LiveData<Uri> getPhotoUriState() {
        return photoUriState;
    }

    public LiveData<double[]> getLocationState() {
        return locationState;
    }

    public LiveData<Incident> getSubmitSuccessState() {
        return submitSuccessState;
    }

    public LiveData<Boolean> getDraftSavedState() {
        return draftSavedState;
    }

    public LiveData<String> getErrorState() {
        return errorState;
    }

    public LiveData<Boolean> getLoadingState() {
        return loadingState;
    }

    public void setPhotoUri(Uri uri, String localPath) {
        photoUriState.setValue(uri);
        localPhotoPathState.setValue(localPath);
    }

    public void clearPhoto() {
        photoUriState.setValue(null);
        localPhotoPathState.setValue(null);
    }

    public void setLocation(double latitude, double longitude) {
        locationState.setValue(new double[]{latitude, longitude});
    }

    public void clearLocation() {
        locationState.setValue(new double[]{0.0, 0.0});
    }

    public void resetSubmitSuccessState() {
        submitSuccessState.setValue(null);
    }

    private void executeSubmission(String title, String description, String category, String severity, boolean isAnonymous, Uri photoUri, double lat, double lng) {
        loadingState.setValue(true);
        repository.submitIncident(
                title,
                description,
                category,
                severity,
                isAnonymous,
                photoUri,
                lat,
                lng,
                new IncidentRepository.IncidentCallback() {
                    @Override
                    public void onSuccess(Incident incident) {
                        loadingState.setValue(false);
                        submitSuccessState.setValue(incident);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        loadingState.setValue(false);
                        errorState.setValue(errorMessage);
                    }
                }
        );
    }

    public void submitIncident(String title, String description, String category, String severity, boolean isAnonymous) {
        if (title == null || title.trim().isEmpty()) {
            errorState.setValue("Please enter an incident title.");
            return;
        }
        if (description == null || description.trim().isEmpty()) {
            errorState.setValue("Please enter a detailed description.");
            return;
        }
        if (category == null || category.trim().isEmpty()) {
            errorState.setValue("Please select an incident category.");
            return;
        }
        if (severity == null || severity.trim().isEmpty()) {
            errorState.setValue("Please select a severity level.");
            return;
        }

        double[] loc = locationState.getValue();
        double lat = loc != null ? loc[0] : 0.0;
        double lng = loc != null ? loc[1] : 0.0;

        executeSubmission(
                title.trim(),
                description.trim(),
                category.trim(),
                severity.trim(),
                isAnonymous,
                photoUriState.getValue(),
                lat,
                lng
        );
    }

    public void triggerSosEmergency(double latitude, double longitude, boolean isLocationAvailable) {
        String desc = isLocationAvailable
                ? "Immediate emergency triggered via one-tap SOS button. Reporter requires assistance."
                : "Immediate emergency triggered via one-tap SOS button. Location unavailable.";

        executeSubmission(
                "SOS EMERGENCY REPORT",
                desc,
                Constants.CAT_OTHER,
                Constants.SEVERITY_CRITICAL,
                false,
                photoUriState.getValue(),
                latitude,
                longitude
        );
    }

    public void saveDraftOffline(String title, String description, String category, String severity, boolean isAnonymous) {
        if (title == null || title.trim().isEmpty()) {
            errorState.setValue("Please enter a title for your draft.");
            return;
        }

        double[] loc = locationState.getValue();
        double lat = loc != null ? loc[0] : 0.0;
        double lng = loc != null ? loc[1] : 0.0;

        repository.saveDraftOffline(
                title.trim(),
                description != null ? description.trim() : "",
                category != null ? category.trim() : Constants.CAT_OTHER,
                severity != null ? severity.trim() : Constants.SEVERITY_LOW,
                isAnonymous,
                localPhotoPathState.getValue(),
                lat,
                lng,
                new IncidentRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        draftSavedState.postValue(true);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        errorState.postValue(errorMessage);
                    }
                }
        );
    }
}
