package com.cinema.ticket_booking;

import com.cinema.ticket_booking.util.ThemeManager;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class CinemaApplication extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Apply saved theme preference ngay khi app khởi động
        ThemeManager.applyTheme(this);
    }
}
