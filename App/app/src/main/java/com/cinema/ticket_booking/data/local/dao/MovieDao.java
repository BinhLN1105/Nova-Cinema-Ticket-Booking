package com.cinema.ticket_booking.data.local.dao;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.cinema.ticket_booking.data.local.entity.MovieEntity;
import java.util.List;

@Dao
public interface MovieDao {
    @Query("SELECT * FROM movies ORDER BY title ASC")
    LiveData<List<MovieEntity>> getAllMovies();

    @Query("SELECT * FROM movies WHERE status = :status")
    LiveData<List<MovieEntity>> getMoviesByStatus(String status);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MovieEntity> movies);

    @Query("DELETE FROM movies")
    void deleteAll();
}
