package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class VoucherSyncResponse(
    @SerializedName("id")
    val id: String?,

    @SerializedName("code")
    val code: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("discountType")
    val discountType: String?,

    @SerializedName("discountValue")
    val discountValue: BigDecimal?
)
