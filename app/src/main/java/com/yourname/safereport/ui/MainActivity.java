package com.yourname.safereport.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.yourname.safereport.R;
import com.yourname.safereport.databinding.ActivityMainBinding;
import com.yourname.safereport.repository.AuthRepository;
import com.yourname.safereport.ui.auth.LoginActivity;
import com.yourname.safereport.utils.Constants;

/**
 * Main Activity hosting Navigation Component graph and role-specific BottomNavigationView
 * (USER: Report & My Reports; ADMIN: Reports Dashboard & Analytics).
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            authRepository = new AuthRepository();

            setSupportActionBar(binding.toolbar);

            String userRole = getIntent() != null && getIntent().hasExtra("user_role")
                    ? getIntent().getStringExtra("user_role")
                    : authRepository.getUserRole(this);

            if (userRole == null) {
                userRole = Constants.ROLE_USER;
            }

            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.navHostFragment);

            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();

                navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                    String screenName = "";
                    int id = destination.getId();
                    if (id == R.id.reportFragment) screenName = "Report Incident";
                    else if (id == R.id.myReportsFragment) screenName = "My Reports";
                    else if (id == R.id.adminDashboardFragment) screenName = "Security Dashboard";
                    else if (id == R.id.adminAnalyticsFragment) screenName = "Analytics";
                    else if (id == R.id.incidentDetailFragment) screenName = "Incident Details";
                    else if (id == R.id.adminMapFragment) screenName = "Map View";
                    else if (id == R.id.trackByCodeFragment) screenName = "Track by Code";
                    else screenName = destination.getLabel() != null ? destination.getLabel().toString() : "";

                    binding.toolbar.setTitle("AlertX");
                    binding.toolbar.setSubtitle(screenName);
                });

                if (Constants.ROLE_ADMIN.equalsIgnoreCase(userRole)) {
                    binding.bottomNav.getMenu().clear();
                    binding.bottomNav.inflateMenu(R.menu.bottom_nav_admin);

                    binding.bottomNav.setOnItemSelectedListener(item -> {
                        int id = item.getItemId();
                        if (id == R.id.adminDashboardFragment) {
                            try { navController.navigate(R.id.adminDashboardFragment); } catch (Exception ignored) {}
                            return true;
                        } else if (id == R.id.adminAnalyticsFragment) {
                            try { navController.navigate(R.id.adminAnalyticsFragment); } catch (Exception ignored) {}
                            return true;
                        }
                        return false;
                    });

                    if (navController.getCurrentDestination() == null || navController.getCurrentDestination().getId() != R.id.adminDashboardFragment) {
                        try {
                            navController.navigate(R.id.adminDashboardFragment);
                        } catch (Exception ignored) {}
                    }
                } else {
                    binding.bottomNav.getMenu().clear();
                    binding.bottomNav.inflateMenu(R.menu.bottom_nav_user);

                    binding.bottomNav.setOnItemSelectedListener(item -> {
                        int id = item.getItemId();
                        if (id == R.id.reportFragment) {
                            try { navController.navigate(R.id.reportFragment); } catch (Exception ignored) {}
                            return true;
                        } else if (id == R.id.myReportsFragment) {
                            try { navController.navigate(R.id.myReportsFragment); } catch (Exception ignored) {}
                            return true;
                        }
                        return false;
                    });
                }
            }

            checkNotificationPermission();

            handleIntentExtras(getIntent());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentExtras(intent);
    }

    private void handleIntentExtras(Intent intent) {
        if (intent != null && intent.hasExtra("incidentId") && navController != null) {
            String incidentId = intent.getStringExtra("incidentId");
            if (incidentId != null && !incidentId.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putString("incidentId", incidentId);
                try {
                    navController.navigate(R.id.incidentDetailFragment, bundle);
                } catch (Exception ignored) {}
            }
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 999, Menu.NONE, "Sign Out")
                .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 999) {
            try {
                authRepository.signOut(this);
            } catch (Exception ignored) {}
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
