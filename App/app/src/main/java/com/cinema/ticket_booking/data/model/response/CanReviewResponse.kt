package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class CanReviewResponse(
    @SerializedName("canReview")
    @get:JvmName("isCanReview")
    val canReview: Boolean,

    @SerializedName("alreadyReviewed")
    @get:JvmName("isAlreadyReviewed")
    val alreadyReviewed: Boolean,

    @SerializedName("bookingId")
    val bookingId: String?,

    @SerializedName("existingReview")
    val existingReview: ReviewResponse?
)
