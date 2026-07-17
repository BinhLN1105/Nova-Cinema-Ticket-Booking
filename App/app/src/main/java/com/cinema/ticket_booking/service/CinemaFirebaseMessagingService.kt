package com.cinema.ticket_booking.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.cinema.ticket_booking.data.model.response.ApiResponse
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@AndroidEntryPoint
class CinemaFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "cinema_notifications"
        private const val PREF_NAME = "fcm_prefs"
        private const val KEY_TOKEN = "fcm_token"

        @JvmStatic
        fun getSavedToken(context: Context): String? {
            val sp = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            return sp.getString(KEY_TOKEN, null)
        }
    }

    @Inject
    lateinit var apiService: ApiService

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            .edit().putString(KEY_TOKEN, token).apply()
        syncTokenToBackend(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        var title: String? = null
        var body: String? = null
        var type: String? = null
        var targetId: String? = null

        // 1. Extract from Data payload (High priority for data-only messages)
        if (message.data.isNotEmpty()) {
            type = message.data["type"]
            targetId = message.data["targetId"]

            // Check for title in common keys
            title = message.data["title"] ?: message.data["Title"]

            // Check for body in common keys
            body = message.data["body"] ?: message.data["Body"] ?: message.data["message"]
        }

        // 2. Extract from Notification payload (Fallback)
        message.notification?.let {
            if (title == null) title = it.title
            if (body == null) body = it.body
        }

        // 3. Final Fallbacks
        if (title.isNullOrEmpty()) title = "NOVA Ticket"
        if (body.isNullOrEmpty()) body = "Bạn có thông báo mới"

        showNotification(title!!, body!!, type, targetId)
    }

    private fun showNotification(title: String, body: String, type: String?, targetId: String?) {
        createChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            // SINGLE_TOP: không tạo lại MainActivity nếu đã mở
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            // Nhét Data vào Intent cho Deep Linking
            type?.let { putExtra("type", it) }
            targetId?.let { putExtra("targetId", it) }
        }

        // FLAG_UPDATE_CURRENT: đảm bảo Intent nhận được putExtra mới nhất
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setLargeIcon(getBitmapFromVectorDrawable(R.drawable.ic_splash_logo))
            .setColor(ContextCompat.getColor(this, R.color.primary))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Cinema Notifications", NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun syncTokenToBackend(token: String) {
        if (!::apiService.isInitialized) return
        apiService.updateFcmToken(token).enqueue(object : Callback<ApiResponse<Void>> {
            override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {}
            override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {}
        })
    }

    private fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(this, drawableId) ?: return null

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
