package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class MovieSummary(
    @SerializedName("id")
    val id: String?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("posterUrl")
    val posterUrl: String?,

    @SerializedName("duration")
    val duration: Int,

    @SerializedName("rated")
    val rated: String?,

    @SerializedName("avgRating")
    val avgRating: Double,

    @SerializedName("status")
    val status: String?,

    @SerializedName("genres")
    val genres: List<Genre>?
)
