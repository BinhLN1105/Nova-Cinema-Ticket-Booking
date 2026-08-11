package com.cinema.ticket_booking.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cinema.ticket_booking.data.local.dao.BookingDao
import com.cinema.ticket_booking.data.local.dao.MovieDao
import com.cinema.ticket_booking.data.local.entity.BookingEntity
import com.cinema.ticket_booking.data.local.entity.MovieEntity

@Database(entities = [MovieEntity::class, BookingEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun bookingDao(): BookingDao
}
