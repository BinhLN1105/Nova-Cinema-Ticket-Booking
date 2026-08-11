package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class ComboResponse(
    @SerializedName("id")
    val id: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("price")
    val price: Double,

    @SerializedName("imageUrl")
    val imageUrl: String?
)
