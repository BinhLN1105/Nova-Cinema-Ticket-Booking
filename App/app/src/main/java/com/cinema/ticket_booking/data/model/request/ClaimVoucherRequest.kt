package com.cinema.ticket_booking.data.model.request

import com.google.gson.annotations.SerializedName

data class ClaimVoucherRequest(
    @SerializedName("code")
    var code: String?
)
