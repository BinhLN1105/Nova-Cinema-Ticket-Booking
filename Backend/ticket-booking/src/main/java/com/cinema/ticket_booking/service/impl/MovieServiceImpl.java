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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Arrays;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.cinema.ticket_booking.service.SystemConfigService;

@Service // Bắt buộc phải có ở file Impl
@RequiredArgsConstructor
@Transactional
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieMapper movieMapper;
    private final SystemConfigService systemConfigService;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "movies_now_showing", key = "#genre == null ? 'all' : #genre")
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
    @Cacheable(value = "movies_now_showing", key = "'page_' + #status.name() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
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
    @Transactional(readOnly = true)
    public List<MovieResponse.Summary> getFeaturedMovies() {
        String mode = systemConfigService.getConfig("HERO_SECTION_MODE", "TOP_SALES");
        List<Movie> movies;

        switch (mode.toUpperCase()) {
            case "TOP_RATING":
                movies = movieRepository.findTop10ByStatusOrderByAvgRatingDesc(MovieStatus.NOW_SHOWING);
                break;
            case "NEW_RELEASE":
                movies = movieRepository.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "releaseDate")))
                        .getContent();
                break;
            case "MANUAL":
                String ids = systemConfigService.getConfig("HERO_SECTION_IDS", "");
                if (ids.isEmpty()) {
                    movies = movieRepository.findTop10ByStatusOrderByAvgRatingDesc(MovieStatus.NOW_SHOWING);
                } else {
                    List<UUID> uuidList = Arrays.stream(ids.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(UUID::fromString)
                            .toList();
                    movies = movieRepository.findAllById(uuidList);
                }
                break;
            case "TOP_SALES":
            default:
                // Fallback to Now Showing if sales metric is not yet implemented
                movies = movieRepository.findByStatus(MovieStatus.NOW_SHOWING, PageRequest.of(0, 5)).getContent();
                break;
        }

        return movies.stream().limit(5).map(movieMapper::toSummary).toList();
    }

    @Override
    @CacheEvict(value = {"movies_now_showing", "movies_coming_soon"}, allEntries = true)
    public MovieResponse create(MovieRequest request) {
        Movie movie = movieMapper.toEntity(request);
        movie.setGenres(resolveGenres(request.getGenreIds()));
        return movieMapper.toResponse(movieRepository.save(movie));
    }

    @Override
    @CacheEvict(value = {"movies_now_showing", "movies_coming_soon"}, allEntries = true)
    public MovieResponse update(UUID id, MovieRequest request) {
        Movie movie = findById(id);
        movieMapper.updateEntity(request, movie);
        if (request.getGenreIds() != null) {
            movie.setGenres(resolveGenres(request.getGenreIds()));
        }
        return movieMapper.toResponse(movieRepository.save(movie));
    }

    @Override
    @CacheEvict(value = {"movies_now_showing", "movies_coming_soon"}, allEntries = true)
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
