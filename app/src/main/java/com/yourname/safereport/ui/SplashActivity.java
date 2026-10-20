package com.yourname.safereport.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.yourname.safereport.R;
import com.yourname.safereport.repository.AuthRepository;
import com.yourname.safereport.ui.auth.LoginActivity;

/**
 * Splash screen checking session status and routing user based on role instantly.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        AuthRepository authRepository = new AuthRepository();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        boolean isValidSession = currentUser != null && !currentUser.isAnonymous() && authRepository.isLoggedIn(this);

        if (isValidSession) {
            String role = authRepository.getUserRole(this);
            navigateToMain(role);
        } else {
            authRepository.signOut(this);
            navigateToLogin();
        }
    }

    private void navigateToMain(String role) {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.putExtra("user_role", role);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
