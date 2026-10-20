package com.yourname.safereport.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yourname.safereport.R;
import com.yourname.safereport.databinding.FragmentAdminDashboardBinding;
import com.yourname.safereport.model.Incident;
import com.yourname.safereport.ui.adapter.IncidentAdapter;
import com.yourname.safereport.utils.Constants;
import com.yourname.safereport.viewmodel.AdminViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin / Security Dashboard providing search, status/severity filtering,
 * and access to Analytics and Map views.
 */
public class AdminDashboardFragment extends Fragment {

    private FragmentAdminDashboardBinding binding;
    private AdminViewModel viewModel;
    private IncidentAdapter adapter;

    private List<Incident> rawIncidentList = new ArrayList<>();
    private String selectedFilter = "ALL";
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

            setupRecyclerView();
            setupFilterChips();
            setupSearch();
            setupNavButtons();

            observeIncidents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupRecyclerView() {
        adapter = new IncidentAdapter(incident -> {
            try {
                Bundle bundle = new Bundle();
                bundle.putString("incidentId", incident.getId());
                Navigation.findNavController(requireView()).navigate(R.id.action_adminDashboard_to_detail, bundle);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        binding.rvAdminIncidents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAdminIncidents.setAdapter(adapter);
    }

    private void setupFilterChips() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedFilter = "ALL";
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipFilterSubmitted) selectedFilter = Constants.STATUS_SUBMITTED;
                else if (id == R.id.chipFilterReview) selectedFilter = Constants.STATUS_UNDER_REVIEW;
                else if (id == R.id.chipFilterInProgress) selectedFilter = Constants.STATUS_IN_PROGRESS;
                else if (id == R.id.chipFilterResolved) selectedFilter = Constants.STATUS_RESOLVED;
                else if (id == R.id.chipFilterCritical) selectedFilter = Constants.SEVERITY_CRITICAL;
                else selectedFilter = "ALL";
            }
            applyFilters();
        });
    }

    private void setupSearch() {
        binding.etSearchCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupNavButtons() {
        binding.btnAnalytics.setOnClickListener(v -> {
            try {
                Navigation.findNavController(requireView()).navigate(R.id.action_adminDashboard_to_analytics);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        binding.btnMap.setOnClickListener(v -> {
            try {
                Navigation.findNavController(requireView()).navigate(R.id.action_adminDashboard_to_map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void observeIncidents() {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.getAllIncidents().observe(getViewLifecycleOwner(), incidents -> {
            if (binding != null) {
                binding.progressBar.setVisibility(View.GONE);
                if (incidents != null) {
                    rawIncidentList = incidents;
                    updateStats(incidents);
                    applyFilters();
                }
            }
        });
    }

    private void updateStats(List<Incident> incidents) {
        int total = incidents.size();
        int critical = 0;
        int resolved = 0;
        for (Incident inc : incidents) {
            if (Constants.SEVERITY_CRITICAL.equalsIgnoreCase(inc.getSeverity())) {
                critical++;
            }
            if (Constants.STATUS_RESOLVED.equalsIgnoreCase(inc.getStatus())) {
                resolved++;
            }
        }
        binding.tvStatTotal.setText(String.valueOf(total));
        binding.tvStatCritical.setText(String.valueOf(critical));
        binding.tvStatResolved.setText(String.valueOf(resolved));
    }

    private void applyFilters() {
        if (binding == null) return;
        List<Incident> filtered = new ArrayList<>();
        for (Incident inc : rawIncidentList) {
            boolean matchesFilter = "ALL".equalsIgnoreCase(selectedFilter)
                    || selectedFilter.equalsIgnoreCase(inc.getStatus())
                    || selectedFilter.equalsIgnoreCase(inc.getSeverity());

            boolean matchesSearch = searchQuery.isEmpty()
                    || (inc.getTrackingCode() != null && inc.getTrackingCode().toLowerCase().contains(searchQuery))
                    || (inc.getTitle() != null && inc.getTitle().toLowerCase().contains(searchQuery));

            if (matchesFilter && matchesSearch) {
                filtered.add(inc);
            }
        }

        if (filtered.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            adapter.setIncidents(null);
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
            adapter.setIncidents(filtered);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
