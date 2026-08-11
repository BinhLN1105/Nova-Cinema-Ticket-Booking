package com.cinema.ticket_booking.data.model.request

import com.google.gson.annotations.SerializedName

data class RegisterRequest @JvmOverloads constructor(
    @SerializedName("email")
    val email: String?,
    @SerializedName("password")
    val password: String?,
    @SerializedName("fullName")
    val fullName: String?,
    @SerializedName("phone")
    val phone: String? = null
)
