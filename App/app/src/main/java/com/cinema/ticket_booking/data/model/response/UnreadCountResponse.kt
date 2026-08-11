package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class UnreadCountResponse(
    @SerializedName("unreadCount")
    val unreadCount: Long
)
