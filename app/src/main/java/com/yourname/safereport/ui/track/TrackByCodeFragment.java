package com.yourname.safereport.ui.track;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.yourname.safereport.R;
import com.yourname.safereport.databinding.FragmentTrackByCodeBinding;
import com.yourname.safereport.model.Incident;
import com.yourname.safereport.utils.Constants;
import com.yourname.safereport.utils.DateTimeUtils;
import com.yourname.safereport.viewmodel.TrackViewModel;

/**
 * Fragment allowing anonymous or logged-in users to look up incidents by tracking code.
 */
public class TrackByCodeFragment extends Fragment {

    private FragmentTrackByCodeBinding binding;
    private TrackViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTrackByCodeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TrackViewModel.class);

        binding.btnSearchCode.setOnClickListener(v -> {
            String code = binding.etTrackingCode.getText() != null ? binding.etTrackingCode.getText().toString().trim() : "";
            if (code.isEmpty()) {
                Snackbar.make(binding.getRoot(), "Please enter a tracking code.", Snackbar.LENGTH_SHORT).show();
                return;
            }
            searchTrackingCode(code);
        });
    }

    private void searchTrackingCode(String trackingCode) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.itemIncident.getRoot().setVisibility(View.GONE);
        binding.tvNotFound.setVisibility(View.GONE);

        viewModel.getTrackedIncident(trackingCode).observe(getViewLifecycleOwner(), incident -> {
            binding.progressBar.setVisibility(View.GONE);
            if (incident != null) {
                bindIncidentItem(incident);
            } else {
                binding.tvNotFound.setVisibility(View.VISIBLE);
            }
        });
    }

    private void bindIncidentItem(Incident incident) {
        binding.itemIncident.getRoot().setVisibility(View.VISIBLE);
        binding.itemIncident.tvTrackingCode.setText(incident.getTrackingCode());
        binding.itemIncident.tvTitle.setText(incident.getTitle());
        binding.itemIncident.tvCategory.setText(incident.getCategory());
        binding.itemIncident.tvStatusChip.setText(incident.getStatus());
        binding.itemIncident.tvDate.setText(DateTimeUtils.formatTimestamp(incident.getCreatedAt()));

        String severity = incident.getSeverity() != null ? incident.getSeverity() : Constants.SEVERITY_LOW;
        binding.itemIncident.tvSeverityChip.setText(severity);

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
        binding.itemIncident.tvSeverityChip.getBackground().setTint(severityBg);

        binding.itemIncident.cardIncident.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("incidentId", incident.getId());
            Navigation.findNavController(requireView()).navigate(R.id.action_trackByCode_to_detail, bundle);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
