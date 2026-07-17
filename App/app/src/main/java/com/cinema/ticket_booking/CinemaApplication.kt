package com.cinema.ticket_booking

import android.app.Application
import com.cinema.ticket_booking.util.ThemeManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CinemaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply saved theme preference ngay khi app khởi động
        ThemeManager.applyTheme(this)
    }
}
