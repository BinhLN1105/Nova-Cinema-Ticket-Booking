package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class Genre(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String?,

    @SerializedName("slug")
    val slug: String?
)
