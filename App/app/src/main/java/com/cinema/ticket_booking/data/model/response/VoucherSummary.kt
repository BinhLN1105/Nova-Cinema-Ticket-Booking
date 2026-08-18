package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class VoucherSummary(
    @SerializedName("code")
    val code: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("discountType")
    val discountType: String?,

    @SerializedName("discountValue")
    val discountValue: Double,

    @SerializedName("maxDiscount")
    val maxDiscount: Double,

    @SerializedName("minOrder")
    val minOrder: Double,

    @SerializedName("endDate")
    val endDate: String?,

    @SerializedName("status")
    val status: String?
)
