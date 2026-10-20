package com.yourname.safereport.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.yourname.safereport.databinding.ActivityLoginBinding;
import com.yourname.safereport.ui.MainActivity;
import com.yourname.safereport.utils.Constants;
import com.yourname.safereport.viewmodel.AuthViewModel;

/**
 * User Authentication screen with input validation for email and password.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupObservers();
        setupListeners();
        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            String prefillEmail = intent.getStringExtra("prefill_email");
            if (prefillEmail != null && !prefillEmail.isEmpty()) {
                binding.etEmail.setText(prefillEmail);
            }
            String successMsg = intent.getStringExtra("registration_success");
            if (successMsg != null && !successMsg.isEmpty()) {
                Snackbar.make(binding.getRoot(), successMsg, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void setupObservers() {
        authViewModel.getLoadingState().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnLogin.setEnabled(!isLoading);
            binding.btnAnonymous.setEnabled(!isLoading);
        });

        authViewModel.getErrorState().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Snackbar.make(binding.getRoot(), errorMsg, Snackbar.LENGTH_LONG).show();
            }
        });

        authViewModel.getUserState().observe(this, user -> {
            if (user != null) {
                String role = (user.getRole() != null && !user.getRole().isEmpty()) ? user.getRole() : Constants.ROLE_USER;
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("user_role", role);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
            String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

            if (email.isEmpty()) {
                Snackbar.make(binding.getRoot(), "Please enter your email address.", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Snackbar.make(binding.getRoot(), "Please enter a valid email address.", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (password.isEmpty()) {
                Snackbar.make(binding.getRoot(), "Please enter your password.", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Snackbar.make(binding.getRoot(), "Password must be at least 6 characters long.", Snackbar.LENGTH_SHORT).show();
                return;
            }

            authViewModel.loginUser(email, password);
        });

        binding.btnAnonymous.setOnClickListener(v -> authViewModel.signInAnonymously());

        binding.tvAdminLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, AdminLoginActivity.class);
            startActivity(intent);
        });

        binding.tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}
