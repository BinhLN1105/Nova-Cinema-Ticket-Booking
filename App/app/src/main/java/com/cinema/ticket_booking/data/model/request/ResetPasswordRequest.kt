package com.cinema.ticket_booking.data.model.request

import com.google.gson.annotations.SerializedName

data class ResetPasswordRequest(
    @SerializedName("token")
    var token: String?,
    @SerializedName("newPassword")
    var newPassword: String?
)
