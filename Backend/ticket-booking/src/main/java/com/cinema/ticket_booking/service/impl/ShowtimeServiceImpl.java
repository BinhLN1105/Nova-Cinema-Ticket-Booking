package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.ShowtimeRequest;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.SeatMapResponse;
import com.cinema.ticket_booking.dto.response.ShowtimeResponse;
import com.cinema.ticket_booking.dto.response.ShowtimeSyncResponse;
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
import com.cinema.ticket_booking.repository.PricingRuleRepository;
import com.cinema.ticket_booking.repository.ScreenRepository;
import com.cinema.ticket_booking.service.CinemaService;
import com.cinema.ticket_booking.service.MovieService;
import com.cinema.ticket_booking.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final PricingRuleRepository pricingRuleRepository;
    private final MovieService movieService;
    private final CinemaService cinemaService;
    private final ShowtimeMapper showtimeMapper;
    private final SeatMapper seatMapper;

    // ── Query ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeSyncResponse> getShowtimesForSync(String movieTitle, UUID movieId, String cinemaName,
            LocalDate date) {
        return showtimeRepository.findAll().stream()
                .filter(s -> date == null || s.getStartTime().toLocalDate().equals(date))
                .filter(s -> movieId == null || s.getMovie().getId().equals(movieId))
                .filter(s -> movieTitle == null
                        || s.getMovie().getTitle().toLowerCase().contains(movieTitle.toLowerCase()))
                .filter(s -> cinemaName == null
                        || s.getScreen().getCinema().getName().toLowerCase().contains(cinemaName.toLowerCase()))
                .filter(s -> s.getStartTime().isAfter(java.time.LocalDateTime.now()))
                .map(s -> ShowtimeSyncResponse.builder()
                        .id(s.getId())
                        .movieId(s.getMovie().getId())
                        .movieTitle(s.getMovie().getTitle())
                        .cinemaName(s.getScreen().getCinema().getName())
                        .screenName(s.getScreen().getName())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .build())
                .toList();
    }

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

    // ── ADMIN: danh sách suất chiếu (phân trang) ──────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShowtimeResponse> adminList(Pageable pageable, String cinemaId, LocalDate date) {
        Page<Showtime> page;
        if (cinemaId != null && !cinemaId.isEmpty() && date != null) {
            page = showtimeRepository.findByCinemaIdAndDate(UUID.fromString(cinemaId), date, pageable);
        } else if (cinemaId != null && !cinemaId.isEmpty()) {
            page = showtimeRepository.findByCinemaId(UUID.fromString(cinemaId), pageable);
        } else if (date != null) {
            page = showtimeRepository.findByDate(date, pageable);
        } else {
            page = showtimeRepository.findAll(pageable);
        }

        Page<ShowtimeResponse> mapped = page.map(s -> {
            ShowtimeResponse r = showtimeMapper.toResponse(s);
            r.setAvailableSeats(showtimeSeatRepository
                    .countByShowtimeIdAndStatus(s.getId(), SeatStatus.AVAILABLE));
            return r;
        });
        return PageResponse.of(mapped);
    }

    // ── ADMIN: xoá suất chiếu ──────────────────────────────────────────────

    @Override
    public void delete(UUID id) {
        Showtime showtime = findById(id);
        showtimeSeatRepository.deleteByShowtimeId(showtime.getId());
        showtimeRepository.delete(showtime);
    }

    @Override
    public void overrideSeatPrices(UUID showtimeId,
            com.cinema.ticket_booking.dto.request.OverrideSeatPriceRequest request) {
        findById(showtimeId); // Ensure showtime exists
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeAndIds(showtimeId,
                request.getShowtimeSeatIds());
        seats.forEach(seat -> seat.setPrice(request.getNewPrice()));
        showtimeSeatRepository.saveAll(seats);
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
        List<com.cinema.ticket_booking.model.PricingRule> activeRules = pricingRuleRepository
                .findByIsActiveTrueOrderByPriorityAsc();

        // 1. Áp dụng Rule theo Ngày, Giờ, hoặc Sự kiện (Áp dụng chung cho cả phòng)
        BigDecimal timeAdjustedPrice = applyTimeRules(basePrice, showtime, activeRules);

        // 2. Từng ghế áp dụng Rule theo Loại Ghế
        List<ShowtimeSeat> showtimeSeats = seats.stream().map(seat -> {
            BigDecimal finalPrice = applySeatRules(timeAdjustedPrice, seat, activeRules);
            return ShowtimeSeat.builder()
                    .showtime(showtime)
                    .seat(seat)
                    .status(SeatStatus.AVAILABLE)
                    .price(finalPrice)
                    .build();
        }).toList();
        showtimeSeatRepository.saveAll(showtimeSeats);
    }

    private BigDecimal applyTimeRules(BigDecimal basePrice, Showtime showtime,
            List<com.cinema.ticket_booking.model.PricingRule> rules) {
        BigDecimal price = basePrice;
        for (com.cinema.ticket_booking.model.PricingRule rule : rules) {
            boolean apply = false;
            switch (rule.getRuleType()) {
                case DAY_OF_WEEK:
                    if (showtime.getStartTime().getDayOfWeek().name().equalsIgnoreCase(rule.getConditionValue())) {
                        apply = true;
                    }
                    break;
                case TIME_FRAME:
                    // Format ví dụ: "22:00-23:59"
                    String[] times = rule.getConditionValue().split("-");
                    if (times.length == 2) {
                        try {
                            java.time.LocalTime start = java.time.LocalTime.parse(times[0]);
                            java.time.LocalTime end = java.time.LocalTime.parse(times[1]);
                            java.time.LocalTime showTimeTime = showtime.getStartTime().toLocalTime();
                            if (!showTimeTime.isBefore(start) && !showTimeTime.isAfter(end)) {
                                apply = true;
                            }
                        } catch (Exception e) {
                            // Bỏ qua lỗi parse rule
                        }
                    }
                    break;
                case DATE_RANGE:
                    // Format: "2024-04-30,2024-05-01"
                    String[] dates = rule.getConditionValue().split(",");
                    if (dates.length == 2) {
                        try {
                            LocalDate startDate = LocalDate.parse(dates[0]);
                            LocalDate endDate = LocalDate.parse(dates[1]);
                            LocalDate showDate = showtime.getStartTime().toLocalDate();
                            if (!showDate.isBefore(startDate) && !showDate.isAfter(endDate)) {
                                apply = true;
                            }
                        } catch (Exception e) {
                        }
                    }
                    break;
                default:
                    break;
            }

            if (apply) {
                price = calculateAdjustment(price, rule);
            }
        }
        return price;
    }

    private BigDecimal applySeatRules(BigDecimal timeAdjustedPrice, Seat seat,
            List<com.cinema.ticket_booking.model.PricingRule> rules) {
        BigDecimal price = timeAdjustedPrice;
        for (com.cinema.ticket_booking.model.PricingRule rule : rules) {
            if (rule.getRuleType() == com.cinema.ticket_booking.enums.PricingRuleType.SEAT_TYPE) {
                if (seat.getSeatType().name().equalsIgnoreCase(rule.getConditionValue())) {
                    price = calculateAdjustment(price, rule);
                }
            }
        }
        return price;
    }

    private BigDecimal calculateAdjustment(BigDecimal currentPrice, com.cinema.ticket_booking.model.PricingRule rule) {
        switch (rule.getAdjustmentType()) {
            case PERCENTAGE:
                // Giảm/tăng %
                BigDecimal multiplier = BigDecimal.ONE.add(rule.getAdjustmentValue().divide(BigDecimal.valueOf(100)));
                return currentPrice.multiply(multiplier);
            case FIXED_AMOUNT:
                // Cộng/trừ tiền trực tiếp
                return currentPrice.add(rule.getAdjustmentValue());
            case MULTIPLIER:
                // Nhân hệ số
                return currentPrice.multiply(rule.getAdjustmentValue());
            default:
                return currentPrice;
        }
    }
}
