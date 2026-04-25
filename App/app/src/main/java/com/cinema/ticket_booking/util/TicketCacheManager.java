package com.cinema.ticket_booking.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Manages offline ticket caching using EncryptedSharedPreferences.
 * Stores only ticket strings (booking code + QR code string) so QR
 * can be regenerated locally via ZXing when offline.
 */
@Singleton
public class TicketCacheManager {

    private static final String PREF_NAME = "offline_tickets";
    private static final String KEY_TICKETS = "cached_tickets";
    private static final int MAX_CACHED_TICKETS = 20;

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    @Inject
    public TicketCacheManager(@ApplicationContext Context context) {
        SharedPreferences sp;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            sp = EncryptedSharedPreferences.create(
                    context, PREF_NAME, masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            // Fallback to regular SharedPreferences if encryption fails
            sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        this.prefs = sp;
    }

    /**
     * Save a ticket to the local cache.
     * Only stores the essential strings; QR bitmap is generated on demand.
     */
    public void saveTicket(CachedTicket ticket) {
        List<CachedTicket> tickets = getAll();
        // Replace if exists
        tickets.removeIf(t -> t.bookingId.equals(ticket.bookingId));
        tickets.add(0, ticket); // Most recent first
        // Keep only recent tickets
        if (tickets.size() > MAX_CACHED_TICKETS) {
            tickets = new ArrayList<>(tickets.subList(0, MAX_CACHED_TICKETS));
        }
        prefs.edit().putString(KEY_TICKETS, gson.toJson(tickets)).apply();
    }

    /**
     * Get a specific cached ticket by booking ID.
     */
    public CachedTicket getTicket(String bookingId) {
        return getAll().stream()
                .filter(t -> t.bookingId.equals(bookingId))
                .findFirst().orElse(null);
    }

    /**
     * Get all cached tickets.
     */
    public List<CachedTicket> getAll() {
        String json = prefs.getString(KEY_TICKETS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<CachedTicket>>() {}.getType();
        try {
            List<CachedTicket> list = gson.fromJson(json, type);
            return list != null ? new ArrayList<>(list) : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Minimal ticket data for offline display.
     * QR code is regenerated from qrCodeString using ZXing.
     */
    public static class CachedTicket {
        public String bookingId;
        public String bookingCode;
        public String qrCodeString;
        public String movieTitle;
        public String cinemaName;
        public String screenName;
        public String startTime;
        public String status;
        public String seats;
    }
}
