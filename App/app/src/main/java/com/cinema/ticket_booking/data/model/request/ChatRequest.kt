package com.cinema.ticket_booking.data.model.request

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("userMessage")
    var userMessage: String?
)
