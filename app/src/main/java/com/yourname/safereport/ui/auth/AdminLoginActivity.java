package com.yourname.safereport.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.yourname.safereport.databinding.ActivityAdminLoginBinding;
import com.yourname.safereport.ui.MainActivity;
import com.yourname.safereport.utils.Constants;
import com.yourname.safereport.viewmodel.AuthViewModel;

/**
 * Dedicated Administrator Login Portal.
 * Verifies role == "ADMIN" in Cloud Firestore after Firebase Auth sign-in.
 */
public class AdminLoginActivity extends AppCompatActivity {

    private ActivityAdminLoginBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupObservers();
        setupListeners();
    }

    private void setupObservers() {
        authViewModel.getLoadingState().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnAdminSubmit.setEnabled(!isLoading);
        });

        authViewModel.getErrorState().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Snackbar.make(binding.getRoot(), errorMsg, Snackbar.LENGTH_LONG).show();
            }
        });

        authViewModel.getUserState().observe(this, user -> {
            if (user != null && Constants.ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
                Intent intent = new Intent(AdminLoginActivity.this, MainActivity.class);
                intent.putExtra("user_role", Constants.ROLE_ADMIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setupListeners() {
        binding.btnAdminSubmit.setOnClickListener(v -> {
            hideKeyboard();
            String email = binding.etAdminEmail.getText() != null ? binding.etAdminEmail.getText().toString().trim() : "";
            String password = binding.etAdminPassword.getText() != null ? binding.etAdminPassword.getText().toString().trim() : "";

            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(binding.getRoot(), "Please enter email and password.", Snackbar.LENGTH_LONG).show();
                return;
            }

            authViewModel.loginAdmin(email, password);
        });

        binding.tvUserLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(AdminLoginActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
