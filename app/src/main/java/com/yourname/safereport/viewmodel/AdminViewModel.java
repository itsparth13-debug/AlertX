package com.yourname.safereport.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.yourname.safereport.model.Incident;
import com.yourname.safereport.repository.IncidentRepository;

import java.util.List;

/**
 * ViewModel managing Admin / Security Dashboard, status/severity filtering, comment updates, and Analytics.
 */
public class AdminViewModel extends AndroidViewModel {

    private final IncidentRepository repository;

    private final MutableLiveData<String> filterStatusState = new MutableLiveData<>("ALL");
    private final MutableLiveData<String> operationSuccessState = new MutableLiveData<>();
    private final MutableLiveData<String> errorState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingState = new MutableLiveData<>(false);

    public AdminViewModel(@NonNull Application application) {
        super(application);
        this.repository = new IncidentRepository(application);
    }

    public LiveData<List<Incident>> getAllIncidents() {
        return repository.getAllIncidents();
    }

    public LiveData<String> getOperationSuccessState() {
        return operationSuccessState;
    }

    public LiveData<String> getErrorState() {
        return errorState;
    }

    public LiveData<Boolean> getLoadingState() {
        return loadingState;
    }

    public void resetOperationSuccessState() {
        operationSuccessState.setValue(null);
    }

    public void updateIncidentStatus(String incidentId, String newStatus, String assignee, String comment, String updatedBy) {
        if (newStatus == null || newStatus.isEmpty()) {
            errorState.setValue("Please select a valid status.");
            return;
        }

        loadingState.setValue(true);
        repository.updateIncidentStatusAndComment(
                incidentId, newStatus, assignee, comment, updatedBy,
                new IncidentRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        loadingState.setValue(false);
                        operationSuccessState.setValue("Incident updated successfully.");
                    }

                    @Override
                    public void onError(String errorMessage) {
                        loadingState.setValue(false);
                        errorState.setValue(errorMessage);
                    }
                }
        );
    }
}
