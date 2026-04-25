package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {
    // Tìm thể loại phim
    Optional<Genre> findBySlug(String slug);

    // Kiểm tra thể loại có hay chưa
    boolean existsByName(String name);
}
