package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("id")
    val id: String?,

    @SerializedName("email")
    val email: String?,

    @SerializedName("fullName")
    val fullName: String?,

    @SerializedName("phone")
    val phone: String?,

    @SerializedName("avatarUrl")
    val avatarUrl: String?,

    @SerializedName("role")
    val role: String?,

    @SerializedName("rewardPoints")
    val cinePoints: Int,

    @SerializedName("cinemaId")
    val cinemaId: String?,

    @SerializedName("cinemaName")
    val cinemaName: String?,

    @SerializedName("currentTierMinPoints")
    val currentTierMinPoints: Long?,

    @SerializedName("nextTierMinPoints")
    val nextTierMinPoints: Long?,

    @SerializedName("membershipTier")
    val rank: String?,

    @SerializedName("availableExp")
    val availableExp: Long,

    @SerializedName("allowMarketingNotification")
    private val allowMarketingNotificationField: Boolean?,

    @SerializedName("allowTransactionNotification")
    private val allowTransactionNotificationField: Boolean?
) {
    val allowMarketingNotification: Boolean
        @JvmName("getAllowMarketingNotification") get() = allowMarketingNotificationField ?: true

    val allowTransactionNotification: Boolean
        @JvmName("getAllowTransactionNotification") get() = allowTransactionNotificationField ?: true
}
