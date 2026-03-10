package com.cinema.ticket_booking.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Lưu trữ Access Token và Refresh Token an toàn
 * bằng EncryptedSharedPreferences (AES-256).
 */
public class TokenManager {

    private static final String PREFS_NAME     = "cinema_secure_prefs";
    private static final String KEY_ACCESS     = "access_token";
    private static final String KEY_REFRESH    = "refresh_token";
    private static final String KEY_USER_ID    = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME  = "user_name";
    private static final String KEY_USER_ROLE  = "user_role";
    private static final String KEY_AVATAR_URL = "avatar_url";

    private final SharedPreferences prefs;

    public TokenManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("Không thể khởi tạo EncryptedSharedPreferences", e);
        }
    }

    // ── Save ─────────────────────────────────────────────────────────────

    public void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString(KEY_ACCESS, accessToken)
                .putString(KEY_REFRESH, refreshToken)
                .apply();
    }

    public void saveUserInfo(String userId, String email, String fullName,
                             String role, String avatarUrl) {
        prefs.edit()
                .putString(KEY_USER_ID,    userId)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_NAME,  fullName)
                .putString(KEY_USER_ROLE,  role)
                .putString(KEY_AVATAR_URL, avatarUrl)
                .apply();
    }

    // ── Get ──────────────────────────────────────────────────────────────

    public String getAccessToken()  { return prefs.getString(KEY_ACCESS, null); }
    public String getRefreshToken() { return prefs.getString(KEY_REFRESH, null); }
    public String getUserId()       { return prefs.getString(KEY_USER_ID, null); }
    public String getUserEmail()    { return prefs.getString(KEY_USER_EMAIL, null); }
    public String getUserName()     { return prefs.getString(KEY_USER_NAME, null); }
    public String getUserRole()     { return prefs.getString(KEY_USER_ROLE, null); }
    public String getAvatarUrl()    { return prefs.getString(KEY_AVATAR_URL, null); }

    public boolean isLoggedIn()     { return getAccessToken() != null; }
    public boolean isAdmin()        { return "ADMIN".equals(getUserRole()); }

    // ── Clear ─────────────────────────────────────────────────────────────

    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
