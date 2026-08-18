package com.cinema.ticket_booking.data.model.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class BookingResponse(
    @SerializedName("id")
    var id: String? = null,

    @SerializedName("bookingCode")
    var bookingCode: String? = null,

    @SerializedName("status")
    var status: String? = null,

    @SerializedName("movieTitle")
    var movieTitle: String? = null,

    @SerializedName("moviePosterUrl")
    var moviePosterUrl: String? = null,

    @SerializedName("startTime")
    var startTime: String? = null,

    @SerializedName("cinemaName")
    var cinemaName: String? = null,

    @SerializedName("cinemaAddress")
    var cinemaAddress: String? = null,

    @SerializedName("screenName")
    var screenName: String? = null,

    @SerializedName("screenType")
    var screenType: String? = null,

    @SerializedName("subtotal")
    var subtotal: Double? = null,

    @SerializedName("discountAmount")
    var discountAmount: Double? = null,

    @SerializedName("totalAmount")
    var totalAmount: Double? = null,

    @SerializedName("totalOriginalAmount")
    var totalOriginalAmount: Double? = null,

    @SerializedName("promotionDiscountAmount")
    var promotionDiscountAmount: Double? = null,

    @SerializedName("appliedPromotionName")
    var appliedPromotionName: String? = null,

    @SerializedName("warningMessage")
    var warningMessage: String? = null,

    @SerializedName("qrCode")
    var qrCode: String? = null,

    @SerializedName("expiresAt")
    var expiresAt: String? = null,

    @SerializedName("seats")
    var seats: List<SeatItem>? = null,

    @SerializedName("combos")
    var combos: List<ComboItem>? = null,

    @SerializedName("pointsUsed")
    var pointsUsed: Long? = null,

    @SerializedName("pointDiscount")
    var pointDiscount: Double? = null,

    @SerializedName("remainingAmount")
    var remainingAmount: Double? = null
) : Parcelable {

    @Parcelize
    data class SeatItem(
        @SerializedName("rowLabel")
        val rowLabel: String? = null,

        @SerializedName("colNumber")
        val colNumber: Int = 0,

        @SerializedName("seatType")
        val seatType: String? = null,

        @SerializedName("price")
        val price: Double = 0.0,

        @SerializedName("showtimeSeatId")
        var showtimeSeatId: String? = null
    ) : Parcelable

    @Parcelize
    data class ComboItem(
        @SerializedName("comboName")
        val comboName: String? = null,

        @SerializedName("quantity")
        val quantity: Int = 0,

        @SerializedName("unitPrice")
        val unitPrice: Double = 0.0,

        @SerializedName("subtotal")
        val subtotal: Double = 0.0,

        @SerializedName("comboId")
        var comboId: String? = null
    ) : Parcelable
}
