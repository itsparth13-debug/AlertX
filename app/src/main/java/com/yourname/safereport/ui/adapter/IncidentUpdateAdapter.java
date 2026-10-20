package com.yourname.safereport.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yourname.safereport.databinding.ItemIncidentUpdateBinding;
import com.yourname.safereport.model.IncidentUpdate;
import com.yourname.safereport.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying incident status update timeline items.
 */
public class IncidentUpdateAdapter extends RecyclerView.Adapter<IncidentUpdateAdapter.UpdateViewHolder> {

    private final List<IncidentUpdate> updateList = new ArrayList<>();

    public void setUpdates(List<IncidentUpdate> updates) {
        this.updateList.clear();
        if (updates != null) {
            this.updateList.addAll(updates);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UpdateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemIncidentUpdateBinding binding = ItemIncidentUpdateBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new UpdateViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UpdateViewHolder holder, int position) {
        holder.bind(updateList.get(position));
    }

    @Override
    public int getItemCount() {
        return updateList.size();
    }

    static class UpdateViewHolder extends RecyclerView.ViewHolder {
        private final ItemIncidentUpdateBinding binding;

        public UpdateViewHolder(@NonNull ItemIncidentUpdateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(IncidentUpdate update) {
            binding.tvUpdateStatus.setText("Status: " + update.getStatus());
            binding.tvUpdateMessage.setText(update.getMessage());
            binding.tvUpdateTimestamp.setText(DateTimeUtils.formatTimestamp(update.getTimestamp()));
            binding.tvUpdatedBy.setText("Updated by: " + (update.getUpdatedBy() != null ? update.getUpdatedBy() : "System"));
        }
    }
}
