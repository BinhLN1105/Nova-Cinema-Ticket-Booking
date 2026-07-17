package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class MovieDetail(
    @SerializedName("id")
    val id: String?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("duration")
    val duration: Int,

    @SerializedName("releaseDate")
    val releaseDate: String?,

    @SerializedName("director")
    val director: String?,

    @SerializedName("cast")
    val cast: String?,

    @SerializedName("rated")
    val rated: String?,

    @SerializedName("posterUrl")
    val posterUrl: String?,

    @SerializedName("trailerUrl")
    val trailerUrl: String?,

    @SerializedName("avgRating")
    val avgRating: Double,

    @SerializedName("status")
    val status: String?,

    @SerializedName("genres")
    val genres: List<Genre>?
)
