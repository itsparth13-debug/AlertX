package com.yourname.safereport.service;

import android.app.PendingIntent;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.yourname.safereport.R;
import com.yourname.safereport.repository.AuthRepository;
import com.yourname.safereport.ui.MainActivity;
import com.yourname.safereport.utils.Constants;

/**
 * Firebase Cloud Messaging Service handling FCM tokens and incoming push notifications.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        AuthRepository authRepository = new AuthRepository();
        authRepository.updateFcmToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "AlertX Notification";
        String body = "An update occurred for your reported incident.";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        } else if (remoteMessage.getData().containsKey("title")) {
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
        }

        String incidentId = remoteMessage.getData().get("incidentId");

        showNotification(title, body, incidentId);
    }

    private void showNotification(String title, String body, String incidentId) {
        Intent intent = new Intent(this, MainActivity.class);
        if (incidentId != null) {
            intent.putExtra("incidentId", incidentId);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException ignored) {
            // Permission handling checked in UI
        }
    }
}
