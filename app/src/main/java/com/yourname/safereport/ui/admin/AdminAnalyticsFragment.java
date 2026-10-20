package com.yourname.safereport.ui.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.yourname.safereport.databinding.FragmentAdminAnalyticsBinding;
import com.yourname.safereport.model.Incident;
import com.yourname.safereport.utils.Constants;
import com.yourname.safereport.viewmodel.AdminViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analytics fragment rendering Pie Chart and Bar Chart using MPAndroidChart library.
 */
public class AdminAnalyticsFragment extends Fragment {

    private FragmentAdminAnalyticsBinding binding;
    private AdminViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminAnalyticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        setupSwipeRefresh();
        observeData();
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            observeData();
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void observeData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.getAllIncidents().observe(getViewLifecycleOwner(), incidents -> {
            binding.progressBar.setVisibility(View.GONE);
            if (incidents == null || incidents.isEmpty()) {
                binding.emptyStateLayout.setVisibility(View.VISIBLE);
                binding.chartsContainer.setVisibility(View.GONE);
                binding.tvStatTotal.setText("0");
                binding.tvStatPending.setText("0");
                binding.tvStatResolved.setText("0");
            } else {
                binding.emptyStateLayout.setVisibility(View.GONE);
                binding.chartsContainer.setVisibility(View.VISIBLE);
                updateStats(incidents);
                setupPieChart(incidents);
                setupBarChart(incidents);
            }
        });
    }

    private void updateStats(List<Incident> incidents) {
        int total = incidents.size();
        int pending = 0;
        int resolved = 0;

        for (Incident inc : incidents) {
            String status = inc.getStatus() != null ? inc.getStatus() : Constants.STATUS_SUBMITTED;
            if (Constants.STATUS_RESOLVED.equalsIgnoreCase(status)) {
                resolved++;
            } else {
                pending++;
            }
        }

        binding.tvStatTotal.setText(String.valueOf(total));
        binding.tvStatPending.setText(String.valueOf(pending));
        binding.tvStatResolved.setText(String.valueOf(resolved));
    }

    private void setupPieChart(List<Incident> incidents) {
        PieChart pieChart = binding.pieChartCategory;
        Map<String, Integer> categoryCounts = new HashMap<>();

        for (Incident incident : incidents) {
            String cat = incident.getCategory() != null ? incident.getCategory() : Constants.CAT_OTHER;
            categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0) + 1);
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Incident Categories");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.getLegend().setTextColor(Color.WHITE);
        pieChart.getLegend().setTextSize(12f);
        pieChart.getLegend().setWordWrapEnabled(true);
        pieChart.animateY(800);
        pieChart.invalidate();
    }

    private void setupBarChart(List<Incident> incidents) {
        BarChart barChart = binding.barChartStatus;

        int submitted = 0, review = 0, resolved = 0;

        for (Incident incident : incidents) {
            String status = incident.getStatus() != null ? incident.getStatus() : Constants.STATUS_SUBMITTED;
            if (Constants.STATUS_RESOLVED.equalsIgnoreCase(status)) {
                resolved++;
            } else if (Constants.STATUS_UNDER_REVIEW.equalsIgnoreCase(status) || Constants.STATUS_IN_PROGRESS.equalsIgnoreCase(status)) {
                review++;
            } else {
                submitted++;
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, submitted));
        entries.add(new BarEntry(1f, review));
        entries.add(new BarEntry(2f, resolved));

        BarDataSet dataSet = new BarDataSet(entries, "Status Counts");
        dataSet.setColors(new int[]{Color.parseColor("#3B82F6"), Color.parseColor("#FBBF24"), Color.parseColor("#22C55E")});
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);

        String[] labels = new String[]{"Submitted", "Under Review", "Resolved"};
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);

        barChart.getAxisLeft().setTextColor(Color.WHITE);
        barChart.getAxisLeft().setGranularity(1f);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setGridColor(Color.parseColor("#334155"));

        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setTextColor(Color.WHITE);
        barChart.getLegend().setEnabled(false);

        barChart.animateY(800);
        barChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
