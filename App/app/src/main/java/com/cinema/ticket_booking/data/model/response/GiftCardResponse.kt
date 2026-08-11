package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class GiftCardResponse(
    @SerializedName("id")
    val id: String?,

    @SerializedName("code")
    val code: String?,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("status")
    val status: String?,

    @SerializedName("createdAt")
    val createdAt: String?,

    @SerializedName("redeemedAt")
    val redeemedAt: String?
)
