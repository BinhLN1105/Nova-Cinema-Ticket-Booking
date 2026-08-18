package com.cinema.ticket_booking.data.model.request

import com.google.gson.annotations.SerializedName

data class UpdateProfileRequest @JvmOverloads constructor(
    @SerializedName("fullName")
    var fullName: String? = null,
    @SerializedName("phone")
    var phone: String? = null,
    @SerializedName("avatarUrl")
    var avatarUrl: String? = null
)
