package com.yourname.safereport.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.yourname.safereport.databinding.ActivityRegisterBinding;
import com.yourname.safereport.viewmodel.AuthViewModel;

/**
 * Registration screen with input validation. All users register with the USER role.
 */
public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupObservers();
        setupListeners();
    }

    private void setupObservers() {
        authViewModel.getLoadingState().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnRegister.setEnabled(!isLoading);
        });

        authViewModel.getErrorState().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Snackbar.make(binding.getRoot(), errorMsg, Snackbar.LENGTH_LONG).show();
            }
        });

        authViewModel.getUserState().observe(this, user -> {
            if (user != null) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                intent.putExtra("prefill_email", user.getEmail());
                intent.putExtra("registration_success", "Account created. Please log in.");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setupListeners() {
        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.etName.getText() != null ? binding.etName.getText().toString().trim() : "";
            String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
            String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

            if (name.isEmpty()) {
                Snackbar.make(binding.getRoot(), "Please enter your full name.", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Snackbar.make(binding.getRoot(), "Please enter a valid email address.", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Snackbar.make(binding.getRoot(), "Password must be at least 6 characters long.", Snackbar.LENGTH_SHORT).show();
                return;
            }

            authViewModel.register(name, email, password);
        });

        binding.tvLoginLink.setOnClickListener(v -> finish());
    }
}
