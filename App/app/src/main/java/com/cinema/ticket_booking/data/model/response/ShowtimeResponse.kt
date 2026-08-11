package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class ShowtimeResponse(
    @SerializedName("id")
    val id: String?,

    @SerializedName("startTime")
    val startTime: String?,

    @SerializedName("endTime")
    val endTime: String?,

    @SerializedName("basePrice")
    val basePrice: Double,

    @SerializedName("status")
    val status: String?,

    @SerializedName("movieTitle")
    val movieTitle: String?,

    @SerializedName("screenName")
    val screenName: String?,

    @SerializedName("screenType")
    val screenType: String?,

    @SerializedName("cinemaName")
    val cinemaName: String?,

    @SerializedName("cinemaAddress")
    val cinemaAddress: String?,

    @SerializedName("availableSeats")
    val availableSeats: Long,

    @SerializedName("movieId")
    val movieId: String?,

    @SerializedName("moviePosterUrl")
    val moviePosterUrl: String?,

    @SerializedName("movieGenres")
    val movieGenres: List<String>?
)
