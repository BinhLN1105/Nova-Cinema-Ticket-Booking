package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class PageResponse<T> @JvmOverloads constructor(
    @SerializedName("content")
    var content: List<T>? = null,

    @SerializedName("page")
    var page: Int = 0,

    @SerializedName("size")
    var size: Int = 0,

    @SerializedName("totalElements")
    var totalElements: Long = 0L,

    @SerializedName("totalPages")
    var totalPages: Int = 0,

    @SerializedName("last")
    @get:JvmName("isLast")
    var last: Boolean = false
)
