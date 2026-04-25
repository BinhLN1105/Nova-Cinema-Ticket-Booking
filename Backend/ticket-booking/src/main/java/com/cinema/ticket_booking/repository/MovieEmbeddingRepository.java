package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.MovieEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieEmbeddingRepository extends JpaRepository<MovieEmbedding, UUID> {
    // Tìm id của phim
    Optional<MovieEmbedding> findByMovieId(UUID movieId);

    // Kiểm tra xem một bộ phim đã có dữ liệu AI (vector) trong database hay chưa
    boolean existsByMovieId(UUID movieId);

    // Xóa dữ liệu AI của 1 phim
    @Transactional
    @Modifying
    @Query("DELETE FROM MovieEmbedding me WHERE me.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") UUID movieId);
}
