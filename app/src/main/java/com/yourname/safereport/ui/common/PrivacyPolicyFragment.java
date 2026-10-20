package com.yourname.safereport.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.yourname.safereport.databinding.FragmentPrivacyPolicyBinding;

/**
 * Fragment displaying app Privacy Policy, Anonymity guarantee, and Data Handling disclosures.
 */
public class PrivacyPolicyFragment extends Fragment {

    private FragmentPrivacyPolicyBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPrivacyPolicyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
