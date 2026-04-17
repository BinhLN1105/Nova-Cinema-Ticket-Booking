package com.cinema.ticket_booking.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.cinema.ticket_booking.data.local.entity.BookingEntity;
import java.util.List;

@Dao
public interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
    LiveData<List<BookingEntity>> getAllBookings();

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    LiveData<BookingEntity> getBookingById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BookingEntity booking);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BookingEntity> bookings);

    @Query("DELETE FROM bookings")
    void deleteAll();
    
    @Query("DELETE FROM bookings WHERE datetime(startTime) < datetime('now', '-3 days')")
    void deleteExpiredBookings();
}
