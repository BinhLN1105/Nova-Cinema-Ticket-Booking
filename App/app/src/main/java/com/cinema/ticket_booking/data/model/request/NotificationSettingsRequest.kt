package com.cinema.ticket_booking.data.model.request

import com.google.gson.annotations.SerializedName

data class NotificationSettingsRequest(
    @SerializedName("allowMarketingNotification")
    val allowMarketingNotification: Boolean?,

    @SerializedName("allowTransactionNotification")
    val allowTransactionNotification: Boolean?
)
