package com.yourname.safereport.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.yourname.safereport.R;
import com.yourname.safereport.databinding.FragmentAdminMapBinding;
import com.yourname.safereport.model.Incident;
import com.yourname.safereport.utils.Constants;
import com.yourname.safereport.viewmodel.AdminViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Map View displaying all incidents on Google Maps with color-coded severity pins.
 */
public class AdminMapFragment extends Fragment implements OnMapReadyCallback {

    private FragmentAdminMapBinding binding;
    private AdminViewModel viewModel;
    private GoogleMap googleMap;

    private final Map<Marker, String> markerIncidentMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.setBuildingsEnabled(true);
        map.setIndoorEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);

        googleMap.setOnInfoWindowClickListener(marker -> {
            String incidentId = markerIncidentMap.get(marker);
            if (incidentId != null) {
                Bundle bundle = new Bundle();
                bundle.putString("incidentId", incidentId);
                Navigation.findNavController(requireView()).navigate(R.id.action_adminMap_to_detail, bundle);
            }
        });

        observeIncidents();
    }

    private void observeIncidents() {
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.getAllIncidents().observe(getViewLifecycleOwner(), incidents -> {
            binding.progressBar.setVisibility(View.GONE);
            if (incidents != null && googleMap != null) {
                plotIncidentsOnMap(incidents);
            }
        });
    }

    private void plotIncidentsOnMap(List<Incident> incidents) {
        googleMap.clear();
        markerIncidentMap.clear();

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasPoints = false;

        for (Incident incident : incidents) {
            if (incident.getLatitude() != 0.0 || incident.getLongitude() != 0.0) {
                LatLng position = new LatLng(incident.getLatitude(), incident.getLongitude());

                float hue;
                String severity = incident.getSeverity() != null ? incident.getSeverity().toUpperCase() : Constants.SEVERITY_LOW;

                switch (severity) {
                    case Constants.SEVERITY_CRITICAL:
                        hue = BitmapDescriptorFactory.HUE_RED;
                        break;
                    case Constants.SEVERITY_HIGH:
                        hue = BitmapDescriptorFactory.HUE_ORANGE;
                        break;
                    case Constants.SEVERITY_MEDIUM:
                        hue = BitmapDescriptorFactory.HUE_YELLOW;
                        break;
                    default:
                        hue = BitmapDescriptorFactory.HUE_GREEN;
                        break;
                }

                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(incident.getTitle())
                        .snippet("Code: " + incident.getTrackingCode() + " | Status: " + incident.getStatus())
                        .icon(BitmapDescriptorFactory.defaultMarker(hue)));

                if (marker != null) {
                    markerIncidentMap.put(marker, incident.getId());
                }

                builder.include(position);
                hasPoints = true;
            }
        }

        if (hasPoints) {
            try {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
            } catch (Exception e) {
                // Fallback camera
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
