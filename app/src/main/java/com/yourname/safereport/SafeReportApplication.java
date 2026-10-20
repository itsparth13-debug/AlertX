package com.yourname.safereport;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Process;

import com.google.firebase.FirebaseApp;
import com.yourname.safereport.ui.auth.LoginActivity;
import com.yourname.safereport.utils.Constants;

/**
 * Global Application class for SafeReport app initialization.
 * Configured with global uncaught exception handling to prevent system crash dialogs.
 */
public class SafeReportApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Global Uncaught Exception Handler to prevent system crash dialogs
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
            try {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } catch (Exception ignored) {}
            Process.killProcess(Process.myPid());
            System.exit(1);
        });

        // Initialize Firebase safely if not already initialized
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create Notification Channel for Android O+
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        Constants.NOTIFICATION_CHANNEL_ID,
                        Constants.NOTIFICATION_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Channel for real-time safety incident status update notifications.");

                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
