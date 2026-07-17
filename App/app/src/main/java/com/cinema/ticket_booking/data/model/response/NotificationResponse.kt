package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    @SerializedName("id")
    val id: String?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("body")
    val body: String?,

    @SerializedName("type")
    val type: String?,

    @SerializedName("isRead")
    val isRead: Boolean,

    @SerializedName("sentAt")
    val sentAt: String?
)
