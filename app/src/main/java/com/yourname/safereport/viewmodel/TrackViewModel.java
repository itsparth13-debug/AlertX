package com.yourname.safereport.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.yourname.safereport.model.Incident;
import com.yourname.safereport.model.IncidentUpdate;
import com.yourname.safereport.repository.IncidentRepository;

import java.util.List;

/**
 * ViewModel managing user's incident list, anonymous tracking by code, and incident detail view.
 */
public class TrackViewModel extends AndroidViewModel {

    private final IncidentRepository repository;

    private final MutableLiveData<String> searchCodeQuery = new MutableLiveData<>();

    public TrackViewModel(@NonNull Application application) {
        super(application);
        this.repository = new IncidentRepository(application);
    }

    public LiveData<List<Incident>> getUserIncidents() {
        return repository.getUserIncidents();
    }

    public void setSearchCode(String trackingCode) {
        searchCodeQuery.setValue(trackingCode.trim().toUpperCase());
    }

    public LiveData<Incident> getTrackedIncident(String trackingCode) {
        return repository.getIncidentByTrackingCode(trackingCode);
    }

    public LiveData<Incident> getIncidentDetail(String incidentId) {
        return repository.getIncidentById(incidentId);
    }

    public LiveData<List<IncidentUpdate>> getIncidentUpdates(String incidentId) {
        return repository.getIncidentUpdates(incidentId);
    }
}
