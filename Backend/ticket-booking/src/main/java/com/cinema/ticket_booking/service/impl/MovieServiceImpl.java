package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.MovieRequest;
import com.cinema.ticket_booking.dto.response.MovieResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.MovieSyncResponse;
import com.cinema.ticket_booking.model.Genre;
import com.cinema.ticket_booking.model.Movie;
import com.cinema.ticket_booking.enums.MovieStatus;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.MovieMapper;
import com.cinema.ticket_booking.repository.GenreRepository;
import com.cinema.ticket_booking.repository.MovieRepository;
import com.cinema.ticket_booking.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service // Bắt buộc phải có ở file Impl
@RequiredArgsConstructor
@Transactional
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieMapper movieMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MovieSyncResponse> getNowShowingForSync(String genre) {
        return movieRepository.findAll().stream()
                .filter(m -> m.getStatus() == MovieStatus.NOW_SHOWING)
                .filter(m -> genre == null || m.getGenres().stream().anyMatch(g -> g.getName().equalsIgnoreCase(genre)))
                .map(m -> MovieSyncResponse.builder()
                        .id(m.getId())
                        .title(m.getTitle())
                        .description(m.getDescription())
                        .duration(m.getDuration())
                        .rated(m.getRated())
                        .language(m.getLanguage())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MovieResponse.Summary> getByStatus(MovieStatus status, Pageable pageable) {
        return PageResponse.of(
                movieRepository.findByStatus(status, pageable).map(movieMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MovieResponse.Summary> search(String title, Pageable pageable) {
        return PageResponse.of(
                movieRepository.findByTitleContainingIgnoreCase(title, pageable).map(movieMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponse getById(UUID id) {
        return movieMapper.toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse.Summary> getNowShowingByCinema(UUID cinemaId) {
        return movieRepository.findNowShowingByCinema(cinemaId)
                .stream().map(movieMapper::toSummary).toList();
    }

    @Override
    public MovieResponse create(MovieRequest request) {
        Movie movie = movieMapper.toEntity(request);
        movie.setGenres(resolveGenres(request.getGenreIds()));
        return movieMapper.toResponse(movieRepository.save(movie));
    }

    @Override
    public MovieResponse update(UUID id, MovieRequest request) {
        Movie movie = findById(id);
        movieMapper.updateEntity(request, movie);
        if (request.getGenreIds() != null) {
            movie.setGenres(resolveGenres(request.getGenreIds()));
        }
        return movieMapper.toResponse(movieRepository.save(movie));
    }

    @Override
    public void delete(UUID id) {
        Movie movie = findById(id);
        movie.setStatus(MovieStatus.ENDED);
        movieRepository.save(movie);
    }

    @Override
    // Cập nhật avgRating sau khi có review mới (gọi từ ReviewService)
    public void updateAvgRating(UUID movieId, Double newAvg) {
        Movie movie = findById(movieId);
        movie.setAvgRating(newAvg != null
                ? new java.math.BigDecimal(newAvg).setScale(2, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.ZERO);
        movieRepository.save(movie);
    }

    @Override
    public Movie findById(UUID id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phim", id));
    }

    private Set<Genre> resolveGenres(List<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty())
            return new HashSet<>();
        Set<Genre> genres = new HashSet<>();
        for (Integer gid : genreIds) {
            genres.add(genreRepository.findById(gid)
                    .orElseThrow(() -> new ResourceNotFoundException("Thể loại", gid)));
        }
        return genres;
    }
}
