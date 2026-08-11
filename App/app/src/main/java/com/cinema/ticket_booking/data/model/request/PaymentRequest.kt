package com.cinema.ticket_booking.data.model.request

import com.google.gson.annotations.SerializedName

data class PaymentRequest(
    @SerializedName("bookingId")
    val bookingId: String?,
    @SerializedName("returnUrl")
    val returnUrl: String?
)
