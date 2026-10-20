package com.yourname.safereport.ui.track;

import android.os.Bundle;
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
import com.yourname.safereport.databinding.FragmentMyReportsBinding;
import com.yourname.safereport.ui.adapter.IncidentAdapter;
import com.yourname.safereport.viewmodel.TrackViewModel;

/**
 * Fragment displaying list of incidents submitted by the logged-in user.
 */
public class MyReportsFragment extends Fragment {

    private FragmentMyReportsBinding binding;
    private TrackViewModel viewModel;
    private IncidentAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TrackViewModel.class);

        setupRecyclerView();
        setupSwipeRefresh();
        observeIncidents();
    }

    private void setupRecyclerView() {
        adapter = new IncidentAdapter(incident -> {
            Bundle bundle = new Bundle();
            bundle.putString("incidentId", incident.getId());
            Navigation.findNavController(requireView()).navigate(R.id.action_myReports_to_detail, bundle);
        });

        binding.rvMyIncidents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMyIncidents.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            observeIncidents();
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void observeIncidents() {
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.getUserIncidents().observe(getViewLifecycleOwner(), incidents -> {
            binding.progressBar.setVisibility(View.GONE);
            if (incidents == null || incidents.isEmpty()) {
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                adapter.setIncidents(null);
            } else {
                binding.tvEmptyState.setVisibility(View.GONE);
                adapter.setIncidents(incidents);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
