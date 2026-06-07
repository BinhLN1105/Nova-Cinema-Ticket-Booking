package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.MovieRequest;
import com.cinema.ticket_booking.dto.response.MovieResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.MovieSyncResponse;
import com.cinema.ticket_booking.model.Genre;
import com.cinema.ticket_booking.model.Movie;
import com.cinema.ticket_booking.enums.MovieStatus;
import com.cinema.ticket_booking.enums.PlatformType;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.MovieMapper;
import com.cinema.ticket_booking.repository.GenreRepository;
import com.cinema.ticket_booking.repository.MovieEmbeddingRepository;
import com.cinema.ticket_booking.repository.MovieRepository;
import com.cinema.ticket_booking.repository.ReviewRepository;
import com.cinema.ticket_booking.repository.ShowtimeRepository;
import com.cinema.ticket_booking.service.MovieService;
import com.cinema.ticket_booking.service.CloudinaryService;
import com.cinema.ticket_booking.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.RoundingMode;
import java.math.BigDecimal;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Arrays;

@Service // Bắt buộc phải có ở file Impl
@RequiredArgsConstructor
@Transactional
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieMapper movieMapper;
    private final SystemConfigService systemConfigService;
    private final CloudinaryService cloudinaryService;
    private final ShowtimeRepository showtimeRepository;
    private final ReviewRepository reviewRepository;
    private final MovieEmbeddingRepository movieEmbeddingRepository;

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
    public PageResponse<MovieResponse.Summary> getAllForAdmin(String search, MovieStatus status, Pageable pageable) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasStatus = status != null;

        org.springframework.data.domain.Page<Movie> page;
        if (hasSearch && hasStatus) {
            page = movieRepository.findByStatusAndTitleContainingIgnoreCase(status, search, pageable);
        } else if (hasSearch) {
            page = movieRepository.findByTitleContainingIgnoreCase(search, pageable);
        } else if (hasStatus) {
            page = movieRepository.findByStatus(status, pageable);
        } else {
            page = movieRepository.findAll(pageable);
        }

        return PageResponse.of(page.map(movieMapper::toSummary));
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
    public List<MovieResponse.Summary> getFeaturedMovies(PlatformType platform) {
        String modeKey = (platform == PlatformType.WEB) ? "HERO_SECTION_WEB_MODE" : "HERO_SECTION_MODE";
        String idsKey = (platform == PlatformType.WEB) ? "HERO_SECTION_WEB_IDS" : "HERO_SECTION_IDS";

        String mode = systemConfigService.getConfig(modeKey, "TOP_SALES");
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
                String ids = systemConfigService.getConfig(idsKey, "");
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
                movies = movieRepository.findByStatus(MovieStatus.NOW_SHOWING, PageRequest.of(0, 6)).getContent();
                break;
        }

        return movies.stream().limit(6).map(movieMapper::toSummary).toList();
    }

    @Override
    @CacheEvict(value = { "movies_now_showing", "movies_coming_soon" }, allEntries = true)
    public MovieResponse create(MovieRequest request) {
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getReleaseDate())) {
            throw new BadRequestException("Ngày kết thúc không được trước ngày phát hành");
        }
        Movie movie = movieMapper.toEntity(request);
        movie.setGenres(resolveGenres(request.getGenreIds()));
        return movieMapper.toResponse(movieRepository.save(movie));
    }

    @Override
    @CacheEvict(value = { "movies_now_showing", "movies_coming_soon" }, allEntries = true)
    public MovieResponse update(UUID id, MovieRequest request) {
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getReleaseDate())) {
            throw new BadRequestException("Ngày kết thúc không được trước ngày phát hành");
        }
        Movie movie = findById(id);
        movieMapper.updateEntity(request, movie);
        if (request.getGenreIds() != null) {
            movie.setGenres(resolveGenres(request.getGenreIds()));
        }
        return movieMapper.toResponse(movieRepository.save(movie));
    }

    @Override
    @Transactional
    @CacheEvict(value = { "movies_now_showing", "movies_coming_soon" }, allEntries = true)
    public MovieResponse updatePoster(UUID id, MultipartFile file) throws IOException {
        Movie movie = findById(id);
        String oldUrl = movie.getPosterUrl();
        String newUrl = null;

        try {
            newUrl = cloudinaryService.uploadImage(file, "Movie");
            movie.setPosterUrl(newUrl);
            movieRepository.save(movie);

            if (oldUrl != null && !oldUrl.isEmpty()) {
                String publicId = cloudinaryService.extractPublicId(oldUrl);
                if (publicId != null)
                    cloudinaryService.deleteImageAsync(publicId);
            }
            return movieMapper.toResponse(movie);
        } catch (Exception e) {
            if (newUrl != null) {
                String newPublicId = cloudinaryService.extractPublicId(newUrl);
                if (newPublicId != null)
                    cloudinaryService.deleteImageAsync(newPublicId);
            }
            throw new RuntimeException("Cập nhật Poster thất bại: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = { "movies_now_showing", "movies_coming_soon" }, allEntries = true)
    public MovieResponse updatePosterFromUrl(UUID id, String url) throws IOException {
        Movie movie = findById(id);
        String oldUrl = movie.getPosterUrl();
        String newUrl = null;

        try {
            newUrl = cloudinaryService.uploadImageFromUrl(url, "Movie");
            movie.setPosterUrl(newUrl);
            movieRepository.save(movie);

            if (oldUrl != null && !oldUrl.isEmpty()) {
                String publicId = cloudinaryService.extractPublicId(oldUrl);
                if (publicId != null)
                    cloudinaryService.deleteImageAsync(publicId);
            }
            return movieMapper.toResponse(movie);
        } catch (Exception e) {
            if (newUrl != null) {
                String newPublicId = cloudinaryService.extractPublicId(newUrl);
                if (newPublicId != null)
                    cloudinaryService.deleteImageAsync(newPublicId);
            }
            throw new RuntimeException("Cập nhật Poster từ URL thất bại: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = { "movies_now_showing", "movies_coming_soon" }, allEntries = true)
    public MovieResponse updateBackdrop(UUID id, MultipartFile file) throws IOException {
        Movie movie = findById(id);
        String oldUrl = movie.getBackdropUrl();
        String newUrl = null;

        try {
            newUrl = cloudinaryService.uploadImage(file, "Movie");
            movie.setBackdropUrl(newUrl);
            movieRepository.save(movie);

            if (oldUrl != null && !oldUrl.isEmpty()) {
                String publicId = cloudinaryService.extractPublicId(oldUrl);
                if (publicId != null)
                    cloudinaryService.deleteImageAsync(publicId);
            }
            return movieMapper.toResponse(movie);
        } catch (Exception e) {
            if (newUrl != null) {
                String newPublicId = cloudinaryService.extractPublicId(newUrl);
                if (newPublicId != null)
                    cloudinaryService.deleteImageAsync(newPublicId);
            }
            throw new RuntimeException("Cập nhật Backdrop thất bại: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = { "movies_now_showing", "movies_coming_soon" }, allEntries = true)
    public MovieResponse updateBackdropFromUrl(UUID id, String url) throws IOException {
        Movie movie = findById(id);
        String oldUrl = movie.getBackdropUrl();
        String newUrl = null;

        try {
            newUrl = cloudinaryService.uploadImageFromUrl(url, "Movie");
            movie.setBackdropUrl(newUrl);
            movieRepository.save(movie);

            if (oldUrl != null && !oldUrl.isEmpty()) {
                String publicId = cloudinaryService.extractPublicId(oldUrl);
                if (publicId != null)
                    cloudinaryService.deleteImageAsync(publicId);
            }
            return movieMapper.toResponse(movie);
        } catch (Exception e) {
            if (newUrl != null) {
                String newPublicId = cloudinaryService.extractPublicId(newUrl);
                if (newPublicId != null)
                    cloudinaryService.deleteImageAsync(newPublicId);
            }
            throw new RuntimeException("Cập nhật Backdrop từ URL thất bại: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = { "movies_now_showing", "movies_coming_soon" }, allEntries = true)
    public void delete(UUID id) {
        Movie movie = findById(id);

        // 1. Kiểm tra xem phim đã từng có suất chiếu nào chưa
        boolean hasShowtimes = showtimeRepository.existsByMovieId(id);

        // 2. Dọn dẹp dữ liệu phụ (Review, Embedding)
        // Những dữ liệu này không ảnh hưởng đến doanh thu/vé nên có thể xóa sạch
        reviewRepository.deleteByMovieId(id);
        movieEmbeddingRepository.deleteByMovieId(id);

        if (hasShowtimes) {
            // Nếu đã có suất chiếu -> Không được xóa cứng (để giữ lịch sử vé/booking)
            // Chuyển trạng thái sang ENDED
            movie.setStatus(MovieStatus.ENDED);
            movieRepository.save(movie);
        } else {
            // Nếu chưa có suất chiếu nào -> Xóa vĩnh viễn
            movieRepository.delete(movie);
        }
    }

    @Override
    // Cập nhật avgRating sau khi có review mới (gọi từ ReviewService)
    public void updateAvgRating(UUID movieId, Double newAvg) {
        Movie movie = findById(movieId);
        movie.setAvgRating(newAvg != null
                ? BigDecimal.valueOf(newAvg).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
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
