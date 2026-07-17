package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class PaymentResponse(
    @SerializedName("id")
    val id: String?,

    @SerializedName("bookingId")
    val bookingId: String?,

    @SerializedName("bookingCode")
    val bookingCode: String?,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("status")
    val status: String?,

    @SerializedName("paymentUrl")
    val paymentUrl: String?,

    @SerializedName("paidAt")
    val paidAt: String?,

    @SerializedName("pointsUsed")
    val pointsUsed: Long?,

    @SerializedName("pointDiscount")
    val pointDiscount: Double?,

    @SerializedName("remainingAmount")
    val remainingAmount: Double?
)
