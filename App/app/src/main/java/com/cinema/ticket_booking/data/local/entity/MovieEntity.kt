package com.cinema.ticket_booking.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey
    var id: String = "",
    val title: String? = null,
    val posterUrl: String? = null,
    val status: String? = null,
    val rated: String? = null,
    val duration: Int = 0,
    val avgRating: Double = 0.0
)
