package com.cinema.ticket_booking.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.ui.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class CinemaFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "cinema_notifications";
    private static final String PREF_NAME = "fcm_prefs";
    private static final String KEY_TOKEN = "fcm_token";

    @Inject
    ApiService apiService;

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .edit().putString(KEY_TOKEN, token).apply();
        syncTokenToBackend(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        String title = "NOVA Ticket";
        String body = "Bạn có thông báo mới";
        String type = null;
        String targetId = null;

        // 1. Hứng payload Data (cho Deep Linking)
        if (message.getData().size() > 0) {
            type = message.getData().get("type");
            targetId = message.getData().get("targetId");
        }

        // 2. Hứng payload Notification (khi app ở Foreground)
        if (message.getNotification() != null) {
            title = message.getNotification().getTitle();
            body = message.getNotification().getBody();
        }

        showNotification(title, body, type, targetId);
    }

    private void showNotification(String title, String body, String type, String targetId) {
        createChannel();

        Intent intent = new Intent(this, MainActivity.class);
        // SINGLE_TOP: không tạo lại MainActivity nếu đã mở
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Nhét Data vào Intent cho Deep Linking
        if (type != null) intent.putExtra("type", type);
        if (targetId != null) intent.putExtra("targetId", targetId);

        // FLAG_UPDATE_CURRENT: đảm bảo Intent nhận được putExtra mới nhất
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Cinema Notifications", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void syncTokenToBackend(String token) {
        if (apiService == null)
            return;
        apiService.updateFcmToken(token).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call call, Response response) {
            }

            @Override
            public void onFailure(Call call, Throwable t) {
            }
        });
    }

    public static String getSavedToken(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return sp.getString(KEY_TOKEN, null);
    }
}
