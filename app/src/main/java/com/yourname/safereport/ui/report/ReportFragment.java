package com.yourname.safereport.ui.report;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.yourname.safereport.R;
import com.yourname.safereport.databinding.DialogTrackingCodeBinding;
import com.yourname.safereport.databinding.FragmentReportBinding;
import com.yourname.safereport.model.Incident;
import com.yourname.safereport.utils.Constants;
import com.yourname.safereport.viewmodel.ReportViewModel;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment allowing users to report incidents with photo, interactive Google Map location picker/preview,
 * severity, anonymous switch, and SOS FAB.
 */
public class ReportFragment extends Fragment implements OnMapReadyCallback {

    private FragmentReportBinding binding;
    private ReportViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;

    private GoogleMap googleMap;
    private double[] currentLocation;
    private LatLng pendingLocation = null;

    private Uri photoUri;
    private String currentPhotoPath;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && photoUri != null) {
                    viewModel.setPhotoUri(photoUri, currentPhotoPath);
                }
            }
    );

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    dispatchTakePictureIntent();
                } else {
                    Snackbar.make(binding.getRoot(), "Camera permission is required to capture evidence.", Snackbar.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fineGranted || coarseGranted) {
                    fetchCurrentLocation();
                } else {
                    Snackbar.make(binding.getRoot(), "Location permission denied. Location unattached.", Snackbar.LENGTH_SHORT).show();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupCategoryDropdown();
        setupObservers();
        setupListeners();
        setupMap();


    }

    private void setupCategoryDropdown() {
        String[] categories = new String[]{
                Constants.CAT_HARASSMENT,
                Constants.CAT_THEFT,
                Constants.CAT_FIRE,
                Constants.CAT_MEDICAL,
                Constants.CAT_INFRASTRUCTURE,
                Constants.CAT_OTHER
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        binding.actvCategory.setAdapter(adapter);
        binding.actvCategory.setText(Constants.CAT_OTHER, false);
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.reportMapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        this.googleMap.setBuildingsEnabled(true);
        this.googleMap.setIndoorEnabled(true);
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);
        this.googleMap.getUiSettings().setCompassEnabled(true);

        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                this.googleMap.setMyLocationEnabled(true);
            }
        } catch (SecurityException e) {
            // Ignore
        }

        this.googleMap.setOnMapClickListener(latLng -> {
            viewModel.setLocation(latLng.latitude, latLng.longitude);
            updateMapLocation(latLng.latitude, latLng.longitude);
            Snackbar.make(binding.getRoot(), "Location set on map!", Snackbar.LENGTH_SHORT).show();
        });

        if (pendingLocation != null) {
            updateMapLocation(pendingLocation.latitude, pendingLocation.longitude);
            pendingLocation = null;
        } else if (currentLocation != null && (currentLocation[0] != 0.0 || currentLocation[1] != 0.0)) {
            updateMapLocation(currentLocation[0], currentLocation[1]);
        }
    }

    private void updateMapLocation(double lat, double lng) {
        binding.reportMapContainer.setVisibility(View.VISIBLE);
        if (lat != 0.0 || lng != 0.0) {
            LatLng pos = new LatLng(lat, lng);
            if (googleMap != null) {
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions().position(pos).title("Incident Location"));
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
            } else {
                pendingLocation = pos;
            }
        }
    }

    private void setupObservers() {
        viewModel.getLoadingState().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSubmit.setEnabled(!isLoading);
            binding.btnSaveDraft.setEnabled(!isLoading);
        });

        viewModel.getErrorState().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Snackbar.make(binding.getRoot(), errorMsg, Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getPhotoUriState().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null) {
                binding.ivPhotoPreview.setVisibility(View.VISIBLE);
                binding.btnRemovePhoto.setVisibility(View.VISIBLE);
                Glide.with(this).load(uri).into(binding.ivPhotoPreview);
            } else {
                binding.ivPhotoPreview.setVisibility(View.GONE);
                binding.btnRemovePhoto.setVisibility(View.GONE);
            }
        });

        viewModel.getLocationState().observe(getViewLifecycleOwner(), loc -> {
            binding.reportMapContainer.setVisibility(View.VISIBLE);
            if (loc != null && (loc[0] != 0.0 || loc[1] != 0.0)) {
                currentLocation = loc;
                binding.tvLocationStatus.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f", loc[0], loc[1]));
                updateMapLocation(loc[0], loc[1]);
            } else {
                currentLocation = null;
                binding.tvLocationStatus.setText(R.string.location_not_attached);
                if (googleMap != null) {
                    googleMap.clear();
                }
            }
        });

        viewModel.getSubmitSuccessState().observe(getViewLifecycleOwner(), incident -> {
            if (incident != null) {
                showTrackingCodeDialog(incident);
                clearForm();
                viewModel.resetSubmitSuccessState();
            }
        });

        viewModel.getDraftSavedState().observe(getViewLifecycleOwner(), saved -> {
            if (Boolean.TRUE.equals(saved)) {
                Snackbar.make(binding.getRoot(), "Draft saved offline! Will sync automatically when online.", Snackbar.LENGTH_LONG).show();
                clearForm();
            }
        });
    }

    private void setupListeners() {
        binding.btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndCap());
        binding.btnRemovePhoto.setOnClickListener(v -> viewModel.clearPhoto());
        binding.btnAttachLocation.setOnClickListener(v -> checkLocationPermissionAndFetch());

        binding.btnSubmit.setOnClickListener(v -> {
            String title = binding.etTitle.getText() != null ? binding.etTitle.getText().toString() : "";
            String description = binding.etDescription.getText() != null ? binding.etDescription.getText().toString() : "";
            String category = binding.actvCategory.getText() != null ? binding.actvCategory.getText().toString() : Constants.CAT_OTHER;
            String severity = getSelectedSeverity();
            boolean isAnonymous = binding.switchAnonymous.isChecked();

            viewModel.submitIncident(title, description, category, severity, isAnonymous);
        });

        binding.btnSaveDraft.setOnClickListener(v -> {
            String title = binding.etTitle.getText() != null ? binding.etTitle.getText().toString() : "";
            String description = binding.etDescription.getText() != null ? binding.etDescription.getText().toString() : "";
            String category = binding.actvCategory.getText() != null ? binding.actvCategory.getText().toString() : Constants.CAT_OTHER;
            String severity = getSelectedSeverity();
            boolean isAnonymous = binding.switchAnonymous.isChecked();

            viewModel.saveDraftOffline(title, description, category, severity, isAnonymous);
        });

        binding.fabSos.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Trigger Emergency SOS?")
                .setMessage("This will instantly broadcast a CRITICAL emergency incident report to Security Staff.")
                .setPositiveButton("SEND SOS", (dialog, which) -> {
                    triggerSosWithCurrentLocation();
                })
                .setNegativeButton("Cancel", null)
                .show());
    }

    private void triggerSosWithCurrentLocation() {
        double[] loc = viewModel.getLocationState().getValue();
        if (loc != null && (loc[0] != 0.0 || loc[1] != 0.0)) {
            viewModel.triggerSosEmergency(loc[0], loc[1], true);
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                try {
                    CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                            .addOnSuccessListener(location -> {
                                if (location != null) {
                                    viewModel.triggerSosEmergency(location.getLatitude(), location.getLongitude(), true);
                                } else {
                                    fusedLocationClient.getLastLocation()
                                            .addOnSuccessListener(lastLocation -> {
                                                if (lastLocation != null) {
                                                    viewModel.triggerSosEmergency(lastLocation.getLatitude(), lastLocation.getLongitude(), true);
                                                } else {
                                                    viewModel.triggerSosEmergency(0.0, 0.0, false);
                                                }
                                            })
                                            .addOnFailureListener(e -> viewModel.triggerSosEmergency(0.0, 0.0, false));
                                }
                            })
                            .addOnFailureListener(e -> {
                                fusedLocationClient.getLastLocation()
                                        .addOnSuccessListener(lastLocation -> {
                                            if (lastLocation != null) {
                                                viewModel.triggerSosEmergency(lastLocation.getLatitude(), lastLocation.getLongitude(), true);
                                            } else {
                                                viewModel.triggerSosEmergency(0.0, 0.0, false);
                                            }
                                        })
                                        .addOnFailureListener(ex -> viewModel.triggerSosEmergency(0.0, 0.0, false));
                            });
                } catch (SecurityException e) {
                    viewModel.triggerSosEmergency(0.0, 0.0, false);
                }
            } else {
                viewModel.triggerSosEmergency(0.0, 0.0, false);
            }
        }
    }

    private String getSelectedSeverity() {
        int checkedChipId = binding.chipGroupSeverity.getCheckedChipId();
        if (checkedChipId == R.id.chipCritical) return Constants.SEVERITY_CRITICAL;
        if (checkedChipId == R.id.chipHigh) return Constants.SEVERITY_HIGH;
        if (checkedChipId == R.id.chipMedium) return Constants.SEVERITY_MEDIUM;
        return Constants.SEVERITY_LOW;
    }

    private void checkCameraPermissionAndCap() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Snackbar.make(binding.getRoot(), "Error creating image file.", Snackbar.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.yourname.safereport.fileprovider",
                        photoFile
                );
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                cameraLauncher.launch(takePictureIntent);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void fetchCurrentLocation() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = locationManager != null && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        if (!gpsEnabled) {
            Snackbar.make(binding.getRoot(), "GPS is turned off. Please enable location services.", Snackbar.LENGTH_LONG)
                    .setAction("Settings", v -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnAttachLocation.setEnabled(false);

        try {
            CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.btnAttachLocation.setEnabled(true);
                            handleObtainedLocation(location.getLatitude(), location.getLongitude());
                        } else {
                            fusedLocationClient.getLastLocation()
                                    .addOnSuccessListener(lastLocation -> {
                                        binding.progressBar.setVisibility(View.GONE);
                                        binding.btnAttachLocation.setEnabled(true);
                                        if (lastLocation != null) {
                                            handleObtainedLocation(lastLocation.getLatitude(), lastLocation.getLongitude());
                                        } else {
                                            handleFallbackLocation();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        binding.progressBar.setVisibility(View.GONE);
                                        binding.btnAttachLocation.setEnabled(true);
                                        handleFallbackLocation();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        fusedLocationClient.getLastLocation()
                                .addOnSuccessListener(lastLocation -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    binding.btnAttachLocation.setEnabled(true);
                                    if (lastLocation != null) {
                                        handleObtainedLocation(lastLocation.getLatitude(), lastLocation.getLongitude());
                                    } else {
                                        handleFallbackLocation();
                                    }
                                })
                                .addOnFailureListener(ex -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    binding.btnAttachLocation.setEnabled(true);
                                    handleFallbackLocation();
                                });
                    });
        } catch (SecurityException e) {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnAttachLocation.setEnabled(true);
            Snackbar.make(binding.getRoot(), "Location permission required.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void handleObtainedLocation(double lat, double lng) {
        viewModel.setLocation(lat, lng);
        if (googleMap != null) {
            updateMapLocation(lat, lng);
        } else {
            pendingLocation = new LatLng(lat, lng);
        }
        Snackbar.make(binding.getRoot(), "Location attached", Snackbar.LENGTH_SHORT).show();
    }

    private void handleFallbackLocation() {
        binding.progressBar.setVisibility(View.GONE);
        binding.btnAttachLocation.setEnabled(true);
        if (googleMap != null) {
            LatLng center = googleMap.getCameraPosition().target;
            handleObtainedLocation(center.latitude, center.longitude);
            Snackbar.make(binding.getRoot(), "Using map center. Tap map to adjust position.", Snackbar.LENGTH_LONG).show();
        } else {
            handleLocationFailure();
        }
    }

    private void handleLocationFailure() {
        viewModel.clearLocation();
        binding.reportMapContainer.setVisibility(View.VISIBLE);
        Snackbar.make(binding.getRoot(), "Could not get your location. Move to an open area or tap the map to choose a spot.", Snackbar.LENGTH_LONG).show();
    }

    private void showTrackingCodeDialog(Incident incident) {
        DialogTrackingCodeBinding dialogBinding = DialogTrackingCodeBinding.inflate(getLayoutInflater());
        dialogBinding.tvTrackingCodeCode.setText(incident.getTrackingCode());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogBinding.getRoot())
                .setCancelable(false)
                .create();

        dialogBinding.btnCopyCode.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Tracking Code", incident.getTrackingCode());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Snackbar.make(binding.getRoot(), R.string.code_copied, Snackbar.LENGTH_SHORT).show();
            }
        });

        dialogBinding.btnDone.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void clearForm() {
        binding.etTitle.setText("");
        binding.etDescription.setText("");
        binding.switchAnonymous.setChecked(false);
        viewModel.clearPhoto();
        viewModel.clearLocation();
        if (googleMap != null) {
            googleMap.clear();
        }
        binding.reportMapContainer.setVisibility(View.VISIBLE);
        binding.tvLocationStatus.setText(R.string.location_not_attached);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
