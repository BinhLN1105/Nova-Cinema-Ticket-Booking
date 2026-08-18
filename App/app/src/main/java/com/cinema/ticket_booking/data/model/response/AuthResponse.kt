package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("accessToken")
    val accessToken: String?,

    @SerializedName("refreshToken")
    val refreshToken: String?,

    @SerializedName("tokenType")
    val tokenType: String?,

    @SerializedName("user")
    val user: UserInfo?
) {
    data class UserInfo(
        @SerializedName("id")
        val id: String?,

        @SerializedName("email")
        val email: String?,

        @SerializedName("fullName")
        val fullName: String?,

        @SerializedName("avatarUrl")
        val avatarUrl: String?,

        @SerializedName("role")
        val role: String?
    )
}
