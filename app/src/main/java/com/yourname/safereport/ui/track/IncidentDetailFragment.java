package com.yourname.safereport.ui.track;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.yourname.safereport.R;
import com.yourname.safereport.databinding.FragmentIncidentDetailBinding;
import com.yourname.safereport.model.Incident;
import com.yourname.safereport.repository.AuthRepository;
import com.yourname.safereport.ui.adapter.IncidentUpdateAdapter;
import com.yourname.safereport.utils.Constants;
import com.yourname.safereport.utils.DateTimeUtils;
import com.yourname.safereport.viewmodel.AdminViewModel;
import com.yourname.safereport.viewmodel.TrackViewModel;

/**
 * Fragment showing full details of an incident, photo evidence, map location,
 * timeline update history, and Admin/Security management actions.
 */
public class IncidentDetailFragment extends Fragment implements OnMapReadyCallback {

    private FragmentIncidentDetailBinding binding;
    private TrackViewModel trackViewModel;
    private AdminViewModel adminViewModel;
    private AuthRepository authRepository;

    private IncidentUpdateAdapter updateAdapter;
    private GoogleMap googleMap;

    private String incidentId;
    private Incident currentIncident;
    private String userRole = Constants.ROLE_USER;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentIncidentDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            incidentId = getArguments().getString("incidentId");
        }

        trackViewModel = new ViewModelProvider(this).get(TrackViewModel.class);
        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);
        authRepository = new AuthRepository();

        setupRecyclerView();
        setupAdminStatusDropdown();
        checkUserRoleAndSetupAdminCard();

        if (incidentId != null) {
            observeIncidentDetail();
            observeUpdates();
        }

        setupMap();
        setupAdminListeners();
    }

    private void setupRecyclerView() {
        updateAdapter = new IncidentUpdateAdapter();
        binding.rvUpdates.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvUpdates.setAdapter(updateAdapter);
    }

    private void setupAdminStatusDropdown() {
        String[] statuses = new String[]{
                Constants.STATUS_SUBMITTED,
                Constants.STATUS_UNDER_REVIEW,
                Constants.STATUS_IN_PROGRESS,
                Constants.STATUS_RESOLVED
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses);
        binding.actvAdminStatus.setAdapter(adapter);
    }

    private void checkUserRoleAndSetupAdminCard() {
        String cachedRole = authRepository.getUserRole(requireContext());
        if (Constants.ROLE_ADMIN.equalsIgnoreCase(cachedRole) || Constants.ROLE_SECURITY.equalsIgnoreCase(cachedRole)) {
            userRole = cachedRole;
            binding.cardAdminManagement.setVisibility(View.VISIBLE);
        }

        authRepository.getCurrentUserRole(new AuthRepository.RoleCallback() {
            @Override
            public void onRoleFetched(String role) {
                userRole = role;
                if (Constants.ROLE_ADMIN.equalsIgnoreCase(role) || Constants.ROLE_SECURITY.equalsIgnoreCase(role)) {
                    binding.cardAdminManagement.setVisibility(View.VISIBLE);
                } else if (!Constants.ROLE_ADMIN.equalsIgnoreCase(cachedRole) && !Constants.ROLE_SECURITY.equalsIgnoreCase(cachedRole)) {
                    binding.cardAdminManagement.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (!Constants.ROLE_ADMIN.equalsIgnoreCase(cachedRole) && !Constants.ROLE_SECURITY.equalsIgnoreCase(cachedRole)) {
                    binding.cardAdminManagement.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.detailMapFragment);
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
        if (currentIncident != null) {
            updateMapMarker(currentIncident);
        }
    }

    private void observeIncidentDetail() {
        binding.progressBar.setVisibility(View.VISIBLE);
        trackViewModel.getIncidentDetail(incidentId).observe(getViewLifecycleOwner(), incident -> {
            binding.progressBar.setVisibility(View.GONE);
            if (incident != null) {
                currentIncident = incident;
                bindIncidentData(incident);
                if (googleMap != null) {
                    updateMapMarker(incident);
                }
            }
        });
    }

    private void bindIncidentData(Incident incident) {
        binding.tvDetailTrackingCode.setText(incident.getTrackingCode());
        binding.tvDetailTitle.setText(incident.getTitle());
        binding.tvDetailCategory.setText(incident.getCategory());
        binding.tvDetailStatus.setText(incident.getStatus());
        binding.tvDetailDescription.setText(incident.getDescription());

        String reporterText = "Reporter: " + (incident.getReporterId() != null ? incident.getReporterId() : "Anonymous")
                + " • " + DateTimeUtils.formatTimestamp(incident.getCreatedAt());
        binding.tvDetailReporterAndDate.setText(reporterText);

        // Severity styling
        String severity = incident.getSeverity() != null ? incident.getSeverity() : Constants.SEVERITY_LOW;
        binding.tvDetailSeverityChip.setText(severity);

        int severityBg;
        switch (severity.toUpperCase()) {
            case Constants.SEVERITY_CRITICAL:
                severityBg = Color.parseColor("#D32F2F");
                break;
            case Constants.SEVERITY_HIGH:
                severityBg = Color.parseColor("#E65100");
                break;
            case Constants.SEVERITY_MEDIUM:
                severityBg = Color.parseColor("#ED6C02");
                break;
            default:
                severityBg = Color.parseColor("#2E7D32");
                break;
        }
        binding.tvDetailSeverityChip.getBackground().setTint(severityBg);

        // Photo loading
        if (incident.getMediaUrls() != null && !incident.getMediaUrls().isEmpty()) {
            binding.ivDetailPhoto.setVisibility(View.VISIBLE);
            Glide.with(this).load(incident.getMediaUrls().get(0)).into(binding.ivDetailPhoto);
        } else {
            binding.ivDetailPhoto.setVisibility(View.GONE);
        }

        // Default admin status selection
        binding.actvAdminStatus.setText(incident.getStatus(), false);
        if (incident.getAssignedTo() != null) {
            binding.etAssignee.setText(incident.getAssignedTo());
        }
    }

    private void updateMapMarker(Incident incident) {
        if (incident.getLatitude() != 0.0 || incident.getLongitude() != 0.0) {
            binding.tvLocationLabel.setVisibility(View.VISIBLE);
            binding.mapContainer.setVisibility(View.VISIBLE);

            LatLng pos = new LatLng(incident.getLatitude(), incident.getLongitude());
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(pos).title(incident.getTitle()));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
        } else {
            binding.tvLocationLabel.setVisibility(View.GONE);
            binding.mapContainer.setVisibility(View.GONE);
        }
    }

    private void observeUpdates() {
        trackViewModel.getIncidentUpdates(incidentId).observe(getViewLifecycleOwner(), updates -> {
            updateAdapter.setUpdates(updates);
        });
    }

    private void setupAdminListeners() {
        binding.btnUpdateIncident.setOnClickListener(v -> {
            String newStatus = binding.actvAdminStatus.getText() != null ? binding.actvAdminStatus.getText().toString() : Constants.STATUS_SUBMITTED;
            String assignee = binding.etAssignee.getText() != null ? binding.etAssignee.getText().toString() : "";
            String comment = binding.etComment.getText() != null ? binding.etComment.getText().toString() : "";

            binding.progressBar.setVisibility(View.VISIBLE);
            adminViewModel.updateIncidentStatus(incidentId, newStatus, assignee, comment, userRole);
        });

        adminViewModel.getOperationSuccessState().observe(getViewLifecycleOwner(), msg -> {
            binding.progressBar.setVisibility(View.GONE);
            if (msg != null) {
                String newStatus = binding.actvAdminStatus.getText() != null ? binding.actvAdminStatus.getText().toString() : "";
                if (!newStatus.isEmpty()) {
                    binding.tvDetailStatus.setText(newStatus);
                }
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                binding.etComment.setText("");
                adminViewModel.resetOperationSuccessState();
            }
        });

        adminViewModel.getErrorState().observe(getViewLifecycleOwner(), err -> {
            binding.progressBar.setVisibility(View.GONE);
            if (err != null) {
                Snackbar.make(binding.getRoot(), err, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
