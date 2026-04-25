package com.cinema.ticket_booking.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * ThemeManager — quản lý Dark/Light mode
 * Lưu preference, apply khi khởi động app
 *
 * Dùng:
 * ThemeManager.applyTheme(context); // gọi trong Application.onCreate()
 * ThemeManager.toggleTheme(context); // gọi khi user bật/tắt switch
 * ThemeManager.isDarkMode(context); // kiểm tra mode hiện tại
 */
public class ThemeManager {

    private static final String PREFS_NAME = "nova_theme_prefs";
    private static final String KEY_DARK_MODE = "is_dark_mode";

    // Mặc định: theo system
    private static final int DEFAULT_MODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

    /**
     * Apply theme đã lưu — gọi trong Application.onCreate() và
     * MainActivity.onCreate()
     */
    public static void applyTheme(Context context) {
        SharedPreferences prefs = getPrefs(context);
        boolean saved = prefs.contains(KEY_DARK_MODE);
        if (saved) {
            boolean isDark = prefs.getBoolean(KEY_DARK_MODE, false);
            AppCompatDelegate.setDefaultNightMode(
                    isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            // Lần đầu: follow system
            AppCompatDelegate.setDefaultNightMode(DEFAULT_MODE);
        }
    }

    /** Toggle giữa dark và light, lưu preference */
    public static void toggleTheme(Context context) {
        boolean current = isDarkMode(context);
        setDarkMode(context, !current);
    }

    /** Set cụ thể dark hoặc light */
    public static void setDarkMode(Context context, boolean dark) {
        getPrefs(context).edit().putBoolean(KEY_DARK_MODE, dark).apply();
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /** Kiểm tra mode hiện tại */
    public static boolean isDarkMode(Context context) {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES)
            return true;
        if (currentMode == AppCompatDelegate.MODE_NIGHT_NO)
            return false;
        // Follow system — đọc từ resources
        int uiMode = context.getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
