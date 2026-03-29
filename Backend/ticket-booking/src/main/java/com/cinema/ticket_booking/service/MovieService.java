package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.MovieRequest;
import com.cinema.ticket_booking.dto.response.MovieResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.MovieSyncResponse;
import com.cinema.ticket_booking.model.Movie;
import com.cinema.ticket_booking.enums.MovieStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface MovieService {
    PageResponse<MovieResponse.Summary> getByStatus(MovieStatus status, Pageable pageable);

    PageResponse<MovieResponse.Summary> search(String title, Pageable pageable);

    MovieResponse getById(UUID id);
    
    List<MovieResponse.Summary> getFeaturedMovies();

    List<MovieResponse.Summary> getNowShowingByCinema(UUID cinemaId);

    MovieResponse create(MovieRequest request);

    MovieResponse update(UUID id, MovieRequest request);

    void delete(UUID id);

    void updateAvgRating(UUID movieId, Double newAvg);

    Movie findById(UUID id); // Dùng cho nội bộ các service khác gọi sang

    List<MovieSyncResponse> getNowShowingForSync(String genre);
}
