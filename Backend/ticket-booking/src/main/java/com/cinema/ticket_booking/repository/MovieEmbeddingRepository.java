package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.MovieEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieEmbeddingRepository extends JpaRepository<MovieEmbedding, UUID> {
    // Tìm id của phim
    Optional<MovieEmbedding> findByMovieId(UUID movieId);

    // Kiểm tra xem một bộ phim đã có dữ liệu AI (vector) trong database hay chưa
    boolean existsByMovieId(UUID movieId);
}
