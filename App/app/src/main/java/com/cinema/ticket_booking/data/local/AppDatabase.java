package com.cinema.ticket_booking.data.local;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.cinema.ticket_booking.data.local.dao.BookingDao;
import com.cinema.ticket_booking.data.local.dao.MovieDao;
import com.cinema.ticket_booking.data.local.entity.BookingEntity;
import com.cinema.ticket_booking.data.local.entity.MovieEntity;

@Database(
    entities = { MovieEntity.class, BookingEntity.class },
    version  = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MovieDao  movieDao();
    public abstract BookingDao bookingDao();
}
