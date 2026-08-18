package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class CinemaResponse(
    @SerializedName("id")
    val id: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("address")
    val address: String?,

    @SerializedName("city")
    val city: String?,

    @SerializedName("imageUrl")
    val imageUrl: String?
)
