package com.cinema.ticket_booking.data.model.request

import com.google.gson.annotations.SerializedName

data class SocialLoginRequest(
    @SerializedName("idToken")
    val idToken: String?,
    @SerializedName("provider")
    val provider: String?
)
