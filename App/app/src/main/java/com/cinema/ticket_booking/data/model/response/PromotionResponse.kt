package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PromotionResponse(
    @SerializedName("id")
    val id: String?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("imageUrl")
    val imageUrl: String?,

    @SerializedName("targetUrl")
    val targetUrl: String?,

    @SerializedName("startDate")
    val startDate: String?,

    @SerializedName("endDate")
    val endDate: String?
) : Serializable
