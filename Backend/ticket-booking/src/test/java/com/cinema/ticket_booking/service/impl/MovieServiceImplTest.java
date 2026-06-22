package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.MovieRequest;
import com.cinema.ticket_booking.dto.response.MovieResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.MovieSyncResponse;
import com.cinema.ticket_booking.enums.MovieStatus;
import com.cinema.ticket_booking.enums.PlatformType;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.MovieMapper;
import com.cinema.ticket_booking.model.Genre;
import com.cinema.ticket_booking.model.Movie;
import com.cinema.ticket_booking.repository.*;
import com.cinema.ticket_booking.service.CloudinaryService;
import com.cinema.ticket_booking.service.SystemConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceImplTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private MovieMapper movieMapper;

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private MovieEmbeddingRepository movieEmbeddingRepository;

    @InjectMocks
    private MovieServiceImpl movieService;

    @Test
    void testGetNowShowingForSync() {
        Genre action = Genre.builder().id(1).name("Action").build();
        Movie movie1 = Movie.builder()
                .id(UUID.randomUUID())
                .title("Movie 1")
                .status(MovieStatus.NOW_SHOWING)
                .genres(Set.of(action))
                .build();
        Movie movie2 = Movie.builder()
                .id(UUID.randomUUID())
                .title("Movie 2")
                .status(MovieStatus.COMING_SOON)
                .build();

        when(movieRepository.findAll()).thenReturn(List.of(movie1, movie2));

        List<MovieSyncResponse> resultAll = movieService.getNowShowingForSync(null);
        assertEquals(1, resultAll.size());
        assertEquals("Movie 1", resultAll.get(0).getTitle());

        List<MovieSyncResponse> resultAction = movieService.getNowShowingForSync("Action");
        assertEquals(1, resultAction.size());

        List<MovieSyncResponse> resultComedy = movieService.getNowShowingForSync("Comedy");
        assertEquals(0, resultComedy.size());
    }

    @Test
    void testGetByStatus() {
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Movie").status(MovieStatus.NOW_SHOWING).build();
        Page<Movie> page = new PageImpl<>(List.of(movie));
        Pageable pageable = PageRequest.of(0, 10);
        MovieResponse.Summary summary = new MovieResponse.Summary();

        when(movieRepository.findByStatus(MovieStatus.NOW_SHOWING, pageable)).thenReturn(page);
        when(movieMapper.toSummary(movie)).thenReturn(summary);

        PageResponse<MovieResponse.Summary> response = movieService.getByStatus(MovieStatus.NOW_SHOWING, pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void testGetMovies() {
        Pageable pageable = PageRequest.of(0, 10);
        Movie movie = new Movie();
        Page<Movie> page = new PageImpl<>(List.of(movie));
        MovieResponse.Summary summary = new MovieResponse.Summary();

        when(movieMapper.toSummary(movie)).thenReturn(summary);

        when(movieRepository.findByStatusAndTitleContainingIgnoreCase(MovieStatus.NOW_SHOWING, "Search", pageable))
                .thenReturn(page);
        PageResponse<MovieResponse.Summary> res1 = movieService.getMovies("Search", MovieStatus.NOW_SHOWING, pageable);
        assertEquals(1, res1.getContent().size());

        when(movieRepository.findByStatus(MovieStatus.NOW_SHOWING, pageable)).thenReturn(page);
        PageResponse<MovieResponse.Summary> res2 = movieService.getMovies("", MovieStatus.NOW_SHOWING, pageable);
        assertEquals(1, res2.getContent().size());

        when(movieRepository.findByStatusNotAndTitleContainingIgnoreCase(MovieStatus.ENDED, "Search", pageable))
                .thenReturn(page);
        PageResponse<MovieResponse.Summary> res3 = movieService.getMovies("Search", null, pageable);
        assertEquals(1, res3.getContent().size());

        when(movieRepository.findByStatusNot(MovieStatus.ENDED, pageable)).thenReturn(page);
        PageResponse<MovieResponse.Summary> res4 = movieService.getMovies(null, null, pageable);
        assertEquals(1, res4.getContent().size());
    }

    @Test
    void testGetAllForAdmin() {
        Pageable pageable = PageRequest.of(0, 10);
        Movie movie = new Movie();
        Page<Movie> page = new PageImpl<>(List.of(movie));
        MovieResponse.Summary summary = new MovieResponse.Summary();

        when(movieMapper.toSummary(movie)).thenReturn(summary);

        when(movieRepository.findByStatusAndTitleContainingIgnoreCase(MovieStatus.NOW_SHOWING, "Search", pageable))
                .thenReturn(page);
        PageResponse<MovieResponse.Summary> res1 = movieService.getAllForAdmin("Search", MovieStatus.NOW_SHOWING, pageable);
        assertEquals(1, res1.getContent().size());

        when(movieRepository.findByTitleContainingIgnoreCase("Search", pageable)).thenReturn(page);
        PageResponse<MovieResponse.Summary> res2 = movieService.getAllForAdmin("Search", null, pageable);
        assertEquals(1, res2.getContent().size());

        when(movieRepository.findByStatus(MovieStatus.NOW_SHOWING, pageable)).thenReturn(page);
        PageResponse<MovieResponse.Summary> res3 = movieService.getAllForAdmin("", MovieStatus.NOW_SHOWING, pageable);
        assertEquals(1, res3.getContent().size());

        when(movieRepository.findAll(pageable)).thenReturn(page);
        PageResponse<MovieResponse.Summary> res4 = movieService.getAllForAdmin(null, null, pageable);
        assertEquals(1, res4.getContent().size());
    }

    @Test
    void testSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Movie movie = new Movie();
        Page<Movie> page = new PageImpl<>(List.of(movie));
        MovieResponse.Summary summary = new MovieResponse.Summary();

        when(movieRepository.findByTitleContainingIgnoreCase("Search", pageable)).thenReturn(page);
        when(movieMapper.toSummary(movie)).thenReturn(summary);

        PageResponse<MovieResponse.Summary> res = movieService.search("Search", pageable);
        assertEquals(1, res.getContent().size());
    }

    @Test
    void testGetById_Success() {
        UUID id = UUID.randomUUID();
        Movie movie = new Movie();
        MovieResponse response = new MovieResponse();

        when(movieRepository.findById(id)).thenReturn(Optional.of(movie));
        when(movieMapper.toResponse(movie)).thenReturn(response);

        MovieResponse result = movieService.getById(id);
        assertNotNull(result);
    }

    @Test
    void testGetById_NotFound() {
        UUID id = UUID.randomUUID();
        when(movieRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> movieService.getById(id));
    }

    @Test
    void testGetNowShowingByCinema() {
        UUID cinemaId = UUID.randomUUID();
        Movie movie = new Movie();
        MovieResponse.Summary summary = new MovieResponse.Summary();

        when(movieRepository.findNowShowingByCinema(cinemaId)).thenReturn(List.of(movie));
        when(movieMapper.toSummary(movie)).thenReturn(summary);

        List<MovieResponse.Summary> list = movieService.getNowShowingByCinema(cinemaId);
        assertEquals(1, list.size());
    }

    @Test
    void testGetFeaturedMovies() {
        Movie movie = new Movie();
        MovieResponse.Summary summary = new MovieResponse.Summary();
        when(movieMapper.toSummary(movie)).thenReturn(summary);

        when(systemConfigService.getConfig("HERO_SECTION_WEB_MODE", "TOP_SALES")).thenReturn("TOP_RATING");
        when(movieRepository.findTop10ByStatusOrderByAvgRatingDesc(MovieStatus.NOW_SHOWING)).thenReturn(List.of(movie));
        List<MovieResponse.Summary> res1 = movieService.getFeaturedMovies(PlatformType.WEB);
        assertEquals(1, res1.size());

        when(systemConfigService.getConfig("HERO_SECTION_MODE", "TOP_SALES")).thenReturn("NEW_RELEASE");
        Page<Movie> page = new PageImpl<>(List.of(movie));
        when(movieRepository.findAll(any(PageRequest.class))).thenReturn(page);
        List<MovieResponse.Summary> res2 = movieService.getFeaturedMovies(PlatformType.ANDROID);
        assertEquals(1, res2.size());

        when(systemConfigService.getConfig("HERO_SECTION_WEB_MODE", "TOP_SALES")).thenReturn("MANUAL");
        when(systemConfigService.getConfig("HERO_SECTION_WEB_IDS", "")).thenReturn("");
        when(movieRepository.findTop10ByStatusOrderByAvgRatingDesc(MovieStatus.NOW_SHOWING)).thenReturn(List.of(movie));
        List<MovieResponse.Summary> res3 = movieService.getFeaturedMovies(PlatformType.WEB);
        assertEquals(1, res3.size());

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(systemConfigService.getConfig("HERO_SECTION_WEB_MODE", "TOP_SALES")).thenReturn("MANUAL");
        when(systemConfigService.getConfig("HERO_SECTION_WEB_IDS", "")).thenReturn(id1 + "," + id2);
        when(movieRepository.findAllById(anyList())).thenReturn(List.of(movie, movie));
        List<MovieResponse.Summary> res4 = movieService.getFeaturedMovies(PlatformType.WEB);
        assertEquals(2, res4.size());

        when(systemConfigService.getConfig("HERO_SECTION_WEB_MODE", "TOP_SALES")).thenReturn("TOP_SALES");
        when(movieRepository.findByStatus(eq(MovieStatus.NOW_SHOWING), any(PageRequest.class))).thenReturn(page);
        List<MovieResponse.Summary> res5 = movieService.getFeaturedMovies(PlatformType.WEB);
        assertEquals(1, res5.size());
    }

    @Test
    void testCreate_Success() {
        MovieRequest request = new MovieRequest();
        request.setReleaseDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(10));
        request.setGenreIds(List.of(1));

        Movie movie = new Movie();
        Genre genre = new Genre();
        MovieResponse response = new MovieResponse();

        when(movieMapper.toEntity(request)).thenReturn(movie);
        when(genreRepository.findById(1)).thenReturn(Optional.of(genre));
        when(movieRepository.save(any(Movie.class))).thenReturn(movie);
        when(movieMapper.toResponse(movie)).thenReturn(response);

        MovieResponse result = movieService.create(request);
        assertNotNull(result);
    }

    @Test
    void testCreate_InvalidDate() {
        MovieRequest request = new MovieRequest();
        request.setReleaseDate(LocalDate.now());
        request.setEndDate(LocalDate.now().minusDays(1));

        assertThrows(BadRequestException.class, () -> movieService.create(request));
    }

    @Test
    void testUpdate_Success() {
        UUID id = UUID.randomUUID();
        MovieRequest request = new MovieRequest();
        request.setReleaseDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(10));
        request.setGenreIds(List.of(1));

        Movie movie = new Movie();
        Genre genre = new Genre();
        MovieResponse response = new MovieResponse();

        when(movieRepository.findById(id)).thenReturn(Optional.of(movie));
        doNothing().when(movieMapper).updateEntity(request, movie);
        when(genreRepository.findById(1)).thenReturn(Optional.of(genre));
        when(movieRepository.save(any(Movie.class))).thenReturn(movie);
        when(movieMapper.toResponse(movie)).thenReturn(response);

        MovieResponse result = movieService.update(id, request);
        assertNotNull(result);
    }

    @Test
    void testUpdate_InvalidDate() {
        UUID id = UUID.randomUUID();
        MovieRequest request = new MovieRequest();
        request.setReleaseDate(LocalDate.now());
        request.setEndDate(LocalDate.now().minusDays(1));

        assertThrows(BadRequestException.class, () -> movieService.update(id, request));
    }

    @Test
    void testUpdatePoster_Success() throws IOException {
        UUID id = UUID.randomUUID();
        Movie movie = Movie.builder().posterUrl("old-poster").build();
        MultipartFile file = mock(MultipartFile.class);

        when(movieRepository.findById(id)).thenReturn(Optional.of(movie));
        when(cloudinaryService.uploadImage(file, "Movie")).thenReturn("new-poster");
        when(cloudinaryService.extractPublicId("old-poster")).thenReturn("old-public-id");
        when(movieRepository.save(movie)).thenReturn(movie);
        when(movieMapper.toResponse(movie)).thenReturn(new MovieResponse());

        MovieResponse response = movieService.updatePoster(id, file);
        assertNotNull(response);
        verify(cloudinaryService).deleteImageAsync("old-public-id");
    }

    @Test
    void testUpdatePoster_Exception() throws IOException {
        UUID id = UUID.randomUUID();
        Movie movie = new Movie();
        MultipartFile file = mock(MultipartFile.class);

        when(movieRepository.findById(id)).thenReturn(Optional.of(movie));
        when(cloudinaryService.uploadImage(file, "Movie")).thenThrow(new RuntimeException("Cloudinary error"));

        assertThrows(RuntimeException.class, () -> movieService.updatePoster(id, file));
    }

    @Test
    void testUpdatePosterFromUrl_Success() throws IOException {
        UUID id = UUID.randomUUID();
        Movie movie = Movie.builder().posterUrl("old-poster").build();

        when(movieRepository.findById(id)).thenReturn(Optional.of(movie));
        when(cloudinaryService.uploadImageFromUrl("http://url.com", "Movie")).thenReturn("new-poster");
        when(cloudinaryService.extractPublicId("old-poster")).thenReturn("old-public-id");
        when(movieRepository.save(movie)).thenReturn(movie);
        when(movieMapper.toResponse(movie)).thenReturn(new MovieResponse());

        MovieResponse response = movieService.updatePosterFromUrl(id, "http://url.com");
        assertNotNull(response);
        verify(cloudinaryService).deleteImageAsync("old-public-id");
    }

    @Test
    void testUpdatePosterFromUrl_Exception() throws IOException {
        UUID id = UUID.randomUUID();
        Movie movie = new Movie();

        when(movieRepository.findById(id)).thenReturn(Optional.of(movie));
        when(cloudinaryService.uploadImageFromUrl(anyString(), anyString())).thenReturn("new-poster");
        when(cloudinaryService.extractPublicId("new-poster")).thenReturn("new-public-id");
        when(movieRepository.save(movie)).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> movieService.updatePosterFromUrl(id, "http://url.com"));
        verify(cloudinaryService).deleteImageAsync("new-public-id");
    }

    @Test
    void testUpdateBackdrop_Success() throws IOException {
        UUID id = UUID.randomUUID();
        Movie movie = Movie.builder().backdropUrl("old-backdrop").build();
        MultipartFile file = mock(MultipartFile.class);

        when(movieRepository.findById(id)).thenReturn(Optional.of(movie));
        when(cloudinaryService.uploadImage(file, "Movie")).thenReturn("new-backdrop");
        when(cloudinaryService.extractPublicId("old-backdrop")).thenReturn("old-public-id");
        when(movieRepository.save(movie)).thenReturn(movie);
        when(movieMapper.toResponse(movie)).thenReturn(new MovieResponse());

        MovieResponse response = movieService.updateBackdrop(id, file);
        assertNotNull(response);
        verify(cloudinaryService).deleteImageAsync("old-public-id");
    }

    @Test
    void testUpdateBackdrop_Exception() throws IOException {
        UUID id = UUID.randomUUID();
        Movie movie = new Movie();
        MultipartFile file = mock(MultipartFile.class);

        when(movieRepository.findById(id)).thenReturn(Optional.of(movie));
        when(cloudinaryService.uploadImage(file, "Movie")).thenThrow(new RuntimeException("Cloudinary error"));

        assertThrows(RuntimeException.class, () -> movieService.updateBackdrop(id, file));
    }

    @Test
    void testUpdateBackdropFromUrl_Success() throws IOException {
        UUID id = UUID.randomUUID();
        Movie movie = Movie.builder().backdropUrl("old-backdrop").build();

        when(movieRepository.findById(id)).thenReturn(Optional.of(movie));
        when(cloudinaryService.uploadImageFromUrl("http://url.com", "Movie")).thenReturn("new-backdrop");
        when(cloudinaryService.extractPublicId("old-backdrop")).thenReturn("old-public-id");
        when(movieRepository.save(movie)).thenReturn(movie);
        when(movieMapper.toResponse(movie)).thenReturn(new MovieResponse());

        MovieResponse response = movieService.updateBackdropFromUrl(id, "http://url.com");
        assertNotNull(response);
        verify(cloudinaryService).deleteImageAsync("old-public-id");
    }

    @Test
    void testUpdateBackdropFromUrl_Exception() throws IOException {
        UUID id = UUID.randomUUID();
        Movie movie = new Movie();

        when(movieRepository.findById(id)).thenReturn(Optional.of(movie));
        when(cloudinaryService.uploadImageFromUrl(anyString(), anyString())).thenReturn("new-backdrop");
        when(cloudinaryService.extractPublicId("new-backdrop")).thenReturn("new-public-id");
        when(movieRepository.save(movie)).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> movieService.updateBackdropFromUrl(id, "http://url.com"));
        verify(cloudinaryService).deleteImageAsync("new-public-id");
    }

    @Test
    void testDelete_WithShowtimes() {
        UUID id = UUID.randomUUID();
        Movie movie = new Movie();

        when(movieRepository.findById(id)).thenReturn(Optional.of(movie));
        when(showtimeRepository.existsByMovieId(id)).thenReturn(true);

        movieService.delete(id);

        assertEquals(MovieStatus.ENDED, movie.getStatus());
        verify(movieRepository).save(movie);
        verify(reviewRepository).deleteByMovieId(id);
        verify(movieEmbeddingRepository).deleteByMovieId(id);
        verify(movieRepository, never()).delete(any(Movie.class));
    }

    @Test
    void testDelete_WithoutShowtimes() {
        UUID id = UUID.randomUUID();
        Movie movie = new Movie();

        when(movieRepository.findById(id)).thenReturn(Optional.of(movie));
        when(showtimeRepository.existsByMovieId(id)).thenReturn(false);

        movieService.delete(id);

        verify(movieRepository).delete(movie);
        verify(reviewRepository).deleteByMovieId(id);
        verify(movieEmbeddingRepository).deleteByMovieId(id);
        verify(movieRepository, never()).save(any(Movie.class));
    }

    @Test
    void testUpdateAvgRating_Success() {
        UUID id = UUID.randomUUID();
        Movie movie = new Movie();

        when(movieRepository.findById(id)).thenReturn(Optional.of(movie));

        movieService.updateAvgRating(id, 4.567);
        assertEquals(BigDecimal.valueOf(4.57), movie.getAvgRating());

        movieService.updateAvgRating(id, null);
        assertEquals(BigDecimal.ZERO, movie.getAvgRating());
    }
}
