package com.cinema.ticket_booking.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey
    var id: String = "",
    var bookingCode: String? = null,
    var status: String? = null,
    var movieTitle: String? = null,
    var moviePosterUrl: String? = null,
    var startTime: String? = null,
    var cinemaName: String? = null,
    var cinemaAddress: String? = null,
    var screenName: String? = null,
    var screenType: String? = null,
    var subtotal: Double = 0.0,
    var discountAmount: Double = 0.0,
    var totalAmount: Double = 0.0,
    var totalOriginalAmount: Double = 0.0,
    var promotionDiscountAmount: Double = 0.0,
    var appliedPromotionName: String? = null,
    var warningMessage: String? = null,
    var qrCode: String? = null,
    var expiresAt: String? = null,
    var seatsJson: String? = null,
    var combosJson: String? = null,
    var createdAt: String? = null
)
