package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.MovieStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MovieResponse {

    private String id;
    private String title;
    private String originalTitle;
    private String description;
    private Integer duration; // phút
    private LocalDate releaseDate;
    private LocalDate endDate;
    private String director;
    private String cast;
    private String language;
    private String rated;
    private String posterUrl;
    private String trailerUrl;
    private BigDecimal avgRating;
    private MovieStatus status;
    private LocalDateTime createdAt;

    private List<GenreResponse> genres;

    // Tóm tắt ngắn cho list (không cần description, cast)
    @Data
    @Builder
    public static class Summary {
        private String id;
        private String title;
        private String posterUrl;
        private Integer duration;
        private String rated;
        private LocalDate releaseDate;
        private BigDecimal avgRating;
        private MovieStatus status;
        private List<GenreResponse> genres;
    }
}
