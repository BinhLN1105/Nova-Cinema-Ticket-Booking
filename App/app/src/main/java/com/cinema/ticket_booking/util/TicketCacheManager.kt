package com.cinema.ticket_booking.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TicketCacheManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val PREF_NAME = "offline_tickets"
        private const val KEY_TICKETS = "cached_tickets"
        private const val MAX_CACHED_TICKETS = 20
    }

    private val prefs: SharedPreferences
    private val gson = Gson()

    init {
        var sp: SharedPreferences
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            sp = EncryptedSharedPreferences.create(
                context, PREF_NAME, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption fails
            sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
        this.prefs = sp
    }

    /**
     * Save a ticket to the local cache.
     * Only stores the essential strings; QR bitmap is generated on demand.
     */
    fun saveTicket(ticket: CachedTicket) {
        var tickets = getAll()
        // Replace if exists
        tickets.removeIf { t -> t.bookingId == ticket.bookingId }
        tickets.add(0, ticket) // Most recent first
        // Keep only recent tickets
        if (tickets.size > MAX_CACHED_TICKETS) {
            tickets = ArrayList(tickets.subList(0, MAX_CACHED_TICKETS))
        }
        prefs.edit().putString(KEY_TICKETS, gson.toJson(tickets)).apply()
    }

    /**
     * Get a specific cached ticket by booking ID.
     */
    fun getTicket(bookingId: String): CachedTicket? {
        return getAll().firstOrNull { t -> t.bookingId == bookingId }
    }

    /**
     * Get all cached tickets.
     */
    fun getAll(): MutableList<CachedTicket> {
        val json = prefs.getString(KEY_TICKETS, null) ?: return ArrayList()
        val type = object : TypeToken<List<CachedTicket>>() {}.type
        return try {
            val list: List<CachedTicket>? = gson.fromJson(json, type)
            if (list != null) ArrayList(list) else ArrayList()
        } catch (e: Exception) {
            ArrayList()
        }
    }

    /**
     * Minimal ticket data for offline display.
     * QR code is regenerated from qrCodeString using ZXing.
     */
    class CachedTicket {
        var bookingId: String? = null
        var bookingCode: String? = null
        var qrCodeString: String? = null
        var movieTitle: String? = null
        var cinemaName: String? = null
        var screenName: String? = null
        var startTime: String? = null
        var status: String? = null
        var seats: String? = null
    }
}
