package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.ShowtimeRequest;
import com.cinema.ticket_booking.dto.response.SeatMapResponse;
import com.cinema.ticket_booking.dto.response.ShowtimeResponse;
import com.cinema.ticket_booking.model.Movie;
import com.cinema.ticket_booking.model.Screen;
import com.cinema.ticket_booking.model.Seat;
import com.cinema.ticket_booking.model.Showtime;
import com.cinema.ticket_booking.model.ShowtimeSeat;
import com.cinema.ticket_booking.enums.SeatStatus;
import com.cinema.ticket_booking.enums.ShowtimeStatus;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.SeatMapper;
import com.cinema.ticket_booking.mapper.ShowtimeMapper;
import com.cinema.ticket_booking.repository.SeatRepository;
import com.cinema.ticket_booking.repository.ShowtimeRepository;
import com.cinema.ticket_booking.repository.ShowtimeSeatRepository;
import com.cinema.ticket_booking.repository.ScreenRepository;
import com.cinema.ticket_booking.service.CinemaService;
import com.cinema.ticket_booking.service.MovieService;
import com.cinema.ticket_booking.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;
    private final MovieService movieService;
    private final CinemaService cinemaService;
    private final ShowtimeMapper showtimeMapper;
    private final SeatMapper seatMapper;

    // ── Query ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getByMovieAndDate(UUID movieId, LocalDate date) {
        return showtimeRepository.findByMovieAndDate(movieId, date)
                .stream().map(s -> {
                    ShowtimeResponse r = showtimeMapper.toResponse(s);
                    r.setAvailableSeats(showtimeSeatRepository
                            .countByShowtimeIdAndStatus(s.getId(), SeatStatus.AVAILABLE));
                    return r;
                }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getByMovieCinemaAndDate(UUID movieId, UUID cinemaId, LocalDate date) {
        return showtimeRepository.findByMovieAndCinemaAndDate(movieId, cinemaId, date)
                .stream().map(s -> {
                    ShowtimeResponse r = showtimeMapper.toResponse(s);
                    r.setAvailableSeats(showtimeSeatRepository
                            .countByShowtimeIdAndStatus(s.getId(), SeatStatus.AVAILABLE));
                    return r;
                }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeResponse getById(UUID id) {
        Showtime s = findById(id);
        ShowtimeResponse r = showtimeMapper.toResponse(s);
        r.setAvailableSeats(showtimeSeatRepository
                .countByShowtimeIdAndStatus(id, SeatStatus.AVAILABLE));
        return r;
    }

    @Override
    @Transactional(readOnly = true)
    public SeatMapResponse getSeatMap(UUID showtimeId) {
        Showtime showtime = findById(showtimeId);
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeIdWithSeat(showtimeId);
        return SeatMapResponse.builder()
                .showtimeId(showtimeId.toString())
                .totalRows(showtime.getScreen().getTotalRows())
                .totalCols(showtime.getScreen().getTotalCols())
                .seats(seats.stream().map(seatMapper::toSeatItem).toList())
                .build();
    }

    // ── ADMIN: tạo suất chiếu ─────────────────────────────────────────────

    @Override
    public ShowtimeResponse create(ShowtimeRequest request) {
        Movie movie = movieService.findById(UUID.fromString(request.getMovieId()));
        Screen screen = screenRepository.findById(UUID.fromString(request.getScreenId()))
                .orElseThrow(() -> new ResourceNotFoundException("Phòng chiếu", request.getScreenId()));

        LocalDateTime endTime = request.getStartTime().plusMinutes(movie.getDuration() + 15); // +15 phút dọn phòng

        if (showtimeRepository.existsConflict(screen.getId(), request.getStartTime(), endTime)) {
            throw new BadRequestException("Phòng chiếu đã có suất chiếu trong khung giờ này");
        }

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .screen(screen)
                .startTime(request.getStartTime())
                .endTime(endTime)
                .basePrice(request.getBasePrice())
                .status(ShowtimeStatus.SCHEDULED)
                .build();
        showtimeRepository.save(showtime);

        // Tự động tạo ShowtimeSeat cho tất cả ghế trong phòng
        generateShowtimeSeats(showtime, screen, request.getBasePrice());

        return showtimeMapper.toResponse(showtime);
    }

    @Override
    @Transactional(readOnly = true)
    public Showtime findById(UUID id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Suất chiếu", id));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void generateShowtimeSeats(Showtime showtime, Screen screen, BigDecimal basePrice) {
        List<Seat> seats = seatRepository.findByScreenIdAndIsActiveTrueOrderByRowLabelAscColNumberAsc(screen.getId());
        List<ShowtimeSeat> showtimeSeats = seats.stream().map(seat -> {
            BigDecimal price = switch (seat.getSeatType()) {
                case VIP -> basePrice.multiply(new BigDecimal("1.5"));
                case COUPLE -> basePrice.multiply(new BigDecimal("2.0"));
                default -> basePrice;
            };
            return ShowtimeSeat.builder()
                    .showtime(showtime)
                    .seat(seat)
                    .status(SeatStatus.AVAILABLE)
                    .price(price)
                    .build();
        }).toList();
        showtimeSeatRepository.saveAll(showtimeSeats);
    }
}
