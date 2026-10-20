package com.yourname.safereport.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yourname.safereport.R;
import com.yourname.safereport.databinding.ItemIncidentBinding;
import com.yourname.safereport.model.Incident;
import com.yourname.safereport.utils.Constants;
import com.yourname.safereport.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying incident cards with colored severity & status indicators.
 */
public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder> {

    public interface OnIncidentClickListener {
        void onIncidentClick(Incident incident);
    }

    private final List<Incident> incidentList = new ArrayList<>();
    private final OnIncidentClickListener listener;

    public IncidentAdapter(OnIncidentClickListener listener) {
        this.listener = listener;
    }

    public void setIncidents(List<Incident> incidents) {
        this.incidentList.clear();
        if (incidents != null) {
            this.incidentList.addAll(incidents);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public IncidentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemIncidentBinding binding = ItemIncidentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new IncidentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull IncidentViewHolder holder, int position) {
        holder.bind(incidentList.get(position));
    }

    @Override
    public int getItemCount() {
        return incidentList.size();
    }

    class IncidentViewHolder extends RecyclerView.ViewHolder {
        private final ItemIncidentBinding binding;

        public IncidentViewHolder(@NonNull ItemIncidentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Incident incident) {
            binding.tvTrackingCode.setText(incident.getTrackingCode());
            binding.tvTitle.setText(incident.getTitle());
            binding.tvCategory.setText(incident.getCategory());
            binding.tvDate.setText(DateTimeUtils.formatTimestamp(incident.getCreatedAt()));

            // Severity color formatting
            String severity = incident.getSeverity() != null ? incident.getSeverity() : Constants.SEVERITY_LOW;
            binding.tvSeverityChip.setText(severity);

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
            binding.tvSeverityChip.getBackground().setTint(severityBg);

            // Status chip formatting
            String status = incident.getStatus() != null ? incident.getStatus() : Constants.STATUS_SUBMITTED;
            binding.tvStatusChip.setText(status);

            binding.cardIncident.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onIncidentClick(incident);
                }
            });
        }
    }
}
