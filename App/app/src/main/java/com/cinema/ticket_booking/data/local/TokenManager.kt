package com.cinema.ticket_booking.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * Lưu trữ Access Token và Refresh Token an toàn
 * bằng EncryptedSharedPreferences (AES-256).
 */
class TokenManager(context: Context) {

    private val prefs: SharedPreferences

    init {
        var sp: SharedPreferences
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            sp = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Handle Auto-Backup bug (keys lost but file restored)
            try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
                val dir = File(context.applicationInfo.dataDir, "shared_prefs")
                File(dir, "$PREFS_NAME.xml").delete()

                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                sp = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (ex: Exception) {
                sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
        this.prefs = sp
    }

    // ── Save ─────────────────────────────────────────────────────────────

    fun saveTokens(accessToken: String?, refreshToken: String?) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .apply()
    }

    fun saveUserInfo(
        userId: String?,
        email: String?,
        fullName: String?,
        role: String?,
        avatarUrl: String?
    ) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, fullName)
            .putString(KEY_USER_ROLE, role)
            .putString(KEY_AVATAR_URL, avatarUrl)
            .apply()
    }

    // ── Get ──────────────────────────────────────────────────────────────

    @get:JvmName("getAccessToken")
    val accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)

    @get:JvmName("getRefreshToken")
    val refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)

    @get:JvmName("getUserId")
    val userId: String?
        get() = prefs.getString(KEY_USER_ID, null)

    @get:JvmName("getUserEmail")
    val userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)

    @get:JvmName("getUserName")
    val userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)

    @get:JvmName("getUserRole")
    val userRole: String?
        get() = prefs.getString(KEY_USER_ROLE, null)

    @get:JvmName("getAvatarUrl")
    val avatarUrl: String?
        get() = prefs.getString(KEY_AVATAR_URL, null)

    @get:JvmName("isLoggedIn")
    val isLoggedIn: Boolean
        get() {
            val token = accessToken
            return !token.isNullOrBlank()
        }

    @get:JvmName("isStaffOrAdmin")
    val isStaffOrAdmin: Boolean
        get() = "ADMIN" == userRole || "STAFF" == userRole

    // ── Clear ─────────────────────────────────────────────────────────────

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "cinema_secure_prefs"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_AVATAR_URL = "avatar_url"
    }
}
