package com.cinema.ticket_booking.model;

import com.cinema.ticket_booking.enums.MovieStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "original_title", length = 200)
    private String originalTitle;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Thời lượng tính bằng phút
    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "director", length = 150)
    private String director;

    @Column(name = "movie_cast", columnDefinition = "TEXT")
    private String cast;

    @Column(name = "language", length = 50)
    @Builder.Default
    private String language = "Vietnamese";

    // Nhãn phân loại: P, C13, C16, C18
    @Column(name = "rated", length = 10)
    private String rated;

    @Column(name = "poster_url", columnDefinition = "TEXT")
    private String posterUrl;

    @Column(name = "backdrop_url", columnDefinition = "TEXT")
    private String backdropUrl;

    @Column(name = "trailer_url", columnDefinition = "TEXT")
    private String trailerUrl;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MovieStatus status;

    // Quan hệ nhiều-nhiều với Genre qua bảng trung gian MovieGenre
    @ManyToMany
    @JoinTable(name = "movie_genres", joinColumns = @JoinColumn(name = "movie_id"), inverseJoinColumns = @JoinColumn(name = "genre_id"))
    @Builder.Default
    private Set<Genre> genres = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
