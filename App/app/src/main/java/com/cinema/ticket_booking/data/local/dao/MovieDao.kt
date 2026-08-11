package com.cinema.ticket_booking.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cinema.ticket_booking.data.local.entity.MovieEntity

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies ORDER BY title ASC")
    fun getAllMovies(): LiveData<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE status = :status")
    fun getMoviesByStatus(status: String): LiveData<List<MovieEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(movies: List<MovieEntity>)

    @Query("DELETE FROM movies")
    fun deleteAll()
}
