package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class ReviewResponse(
    @SerializedName("id")
    val id: String?,

    @SerializedName("userFullName")
    val userFullName: String?,

    @SerializedName("userAvatarUrl")
    val userAvatarUrl: String?,

    @SerializedName("rating")
    val rating: Int,

    @SerializedName("comment")
    val comment: String?,

    @SerializedName("createdAt")
    val createdAt: String?
)
