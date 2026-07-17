package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success")
    @get:JvmName("isSuccess")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: T?
)
