package com.cinema.ticket_booking.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cinema.ticket_booking.data.local.entity.BookingEntity

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
    fun getAllBookings(): LiveData<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    fun getBookingById(id: String): LiveData<BookingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(booking: BookingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(bookings: List<BookingEntity>)

    @Query("DELETE FROM bookings")
    fun deleteAll()

    @Query("DELETE FROM bookings WHERE datetime(startTime) < datetime('now', '-3 days')")
    fun deleteExpiredBookings()
}
