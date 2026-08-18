package com.cinema.ticket_booking.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    private const val PREFS_NAME = "nova_theme_prefs"
    private const val KEY_DARK_MODE = "is_dark_mode"

    private const val DEFAULT_MODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

    @JvmStatic
    fun applyTheme(context: Context) {
        val prefs = getPrefs(context)
        val saved = prefs.contains(KEY_DARK_MODE)
        if (saved) {
            val isDark = prefs.getBoolean(KEY_DARK_MODE, false)
            AppCompatDelegate.setDefaultNightMode(
                if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        } else {
            AppCompatDelegate.setDefaultNightMode(DEFAULT_MODE)
        }
    }

    @JvmStatic
    fun toggleTheme(context: Context) {
        val current = isDarkMode(context)
        setDarkMode(context, !current)
    }

    @JvmStatic
    fun setDarkMode(context: Context, dark: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DARK_MODE, dark).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    @JvmStatic
    fun isDarkMode(context: Context): Boolean {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) return true
        if (currentMode == AppCompatDelegate.MODE_NIGHT_NO) return false
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
