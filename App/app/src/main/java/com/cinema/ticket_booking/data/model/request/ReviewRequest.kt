package com.cinema.ticket_booking.data.model.request

import com.google.gson.annotations.SerializedName

data class ReviewRequest(
    @SerializedName("movieId")
    val movieId: String?,
    @SerializedName("bookingId")
    val bookingId: String?,
    @SerializedName("rating")
    val rating: Int,
    @SerializedName("comment")
    val comment: String?
)
