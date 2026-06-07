package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.ShowtimeRequest;
import com.cinema.ticket_booking.dto.request.OverrideSeatPriceRequest;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.SeatMapResponse;
import com.cinema.ticket_booking.dto.response.ShowtimeResponse;
import com.cinema.ticket_booking.dto.response.ShowtimeSyncResponse;
import com.cinema.ticket_booking.dto.PricingResult;
import com.cinema.ticket_booking.model.Movie;
import com.cinema.ticket_booking.model.Screen;
import com.cinema.ticket_booking.model.Seat;
import com.cinema.ticket_booking.model.Showtime;
import com.cinema.ticket_booking.model.ShowtimeSeat;
import com.cinema.ticket_booking.model.PricingRule;
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
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.PaymentRepository;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.exception.ConflictException;
import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.service.CinemaService;
import com.cinema.ticket_booking.service.MovieService;
import com.cinema.ticket_booking.service.PricingEngineService;
import com.cinema.ticket_booking.service.ShowtimeService;
import com.cinema.ticket_booking.service.SeatLockService;
import com.cinema.ticket_booking.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final SystemConfigService systemConfigService;
    private final SeatLockService seatLockService;
    private final PricingEngineService pricingEngineService;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    // ── Query ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeSyncResponse> getShowtimesForSync(String movieTitle, UUID movieId, String cinemaName,
            LocalDate date) {
        int allowance = systemConfigService.getIntConfig("LATE_BOOKING_ALLOWANCE_MINS", 10);
        return showtimeRepository.findAll().stream()
                .filter(s -> date == null || s.getStartTime().toLocalDate().equals(date))
                .filter(s -> movieId == null || s.getMovie().getId().equals(movieId))
                .filter(s -> movieTitle == null
                        || s.getMovie().getTitle().toLowerCase().contains(movieTitle.toLowerCase()))
                .filter(s -> cinemaName == null
                        || s.getScreen().getCinema().getName().toLowerCase().contains(cinemaName.toLowerCase()))
                .filter(s -> s.getStartTime().plusMinutes(allowance).isAfter(LocalDateTime.now()))
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
        int allowance = systemConfigService.getIntConfig("LATE_BOOKING_ALLOWANCE_MINS", 10);
        return showtimeRepository.findByMovieAndDate(movieId, date)
                .stream()
                .filter(s -> s.getStartTime().plusMinutes(allowance).isAfter(LocalDateTime.now()))
                .map(s -> {
                    ShowtimeResponse r = showtimeMapper.toResponse(s);
                    r.setAvailableSeats(showtimeSeatRepository
                            .countByShowtimeIdAndStatus(s.getId(), SeatStatus.AVAILABLE));
                    return r;
                }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getByMovieCinemaAndDate(UUID movieId, UUID cinemaId, LocalDate date) {
        int allowance = systemConfigService.getIntConfig("LATE_BOOKING_ALLOWANCE_MINS", 10);
        return showtimeRepository.findByMovieAndCinemaAndDate(movieId, cinemaId, date)
                .stream()
                .filter(s -> s.getStartTime().plusMinutes(allowance).isAfter(LocalDateTime.now()))
                .map(s -> {
                    ShowtimeResponse r = showtimeMapper.toResponse(s);
                    r.setAvailableSeats(showtimeSeatRepository
                            .countByShowtimeIdAndStatus(s.getId(), SeatStatus.AVAILABLE));
                    return r;
                }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getByCinemaAndDate(UUID cinemaId, LocalDate date) {
        int allowance = systemConfigService.getIntConfig("LATE_BOOKING_ALLOWANCE_MINS", 10);
        return showtimeRepository.findByCinemaAndDateScheduled(cinemaId, date)
                .stream()
                .filter(s -> s.getStartTime().plusMinutes(allowance).isAfter(LocalDateTime.now()))
                .map(s -> {
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
        List<PricingRule> activeRules = pricingRuleRepository.findByIsActiveTrueOrderByPriorityAsc();

        List<String> validSeatIds = seats.stream().map(s -> s.getId().toString()).collect(Collectors.toList());
        List<String> lockedSeatIds = seatLockService.getLockedSeats(validSeatIds);

        List<SeatMapResponse.SeatItem> mappedSeats = seats.stream().map(seat -> {
            var seatItem = seatMapper.toSeatItem(seat);
            if (seatItem.getStatus() == SeatStatus.AVAILABLE && lockedSeatIds.contains(seat.getId().toString())) {
                seatItem.setStatus(SeatStatus.LOCKED);
            }
            // Recalculate price dynamically
            PricingResult result = pricingEngineService.calculateFinalSeatPrice(showtime, seat.getSeat(),
                    showtime.getBasePrice(), activeRules, 1, 0);
            seatItem.setPrice(result.finalPrice());
            seatItem.setBasePrice(showtime.getBasePrice());
            seatItem.setDiscountAmount(result.discountAmount());
            seatItem.setAppliedPromotionName(result.appliedPromotionName());
            return seatItem;
        }).toList();

        // Tính toán thời gian giữ ghế linh hoạt
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        long minutesToStart = Duration.between(
                now.atZone(ZoneId.of("Asia/Ho_Chi_Minh")),
                showtime.getStartTime().atZone(ZoneId.of("Asia/Ho_Chi_Minh"))).toMinutes();
        int seatHoldMins = systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10);

        // Nếu phim sắp chiếu (trong 15p), rút ngắn thời gian giữ ghế xuống 3p
        if (minutesToStart <= 15 && minutesToStart >= -10) {
            seatHoldMins = systemConfigService.getIntConfig("LATE_SEAT_HOLD_TIME", 3);
        }

        // Tính toán maxGridRow và maxGridCol
        int dummyGrid = 0;
        for (SeatMapResponse.SeatItem item : mappedSeats) {
            if (item.getGridRow() == 0 && item.getGridCol() == 0) {
                if (item.getRowLabel() != null && item.getColNumber() > 0) {
                    item.setGridRow(Math.max(0, item.getRowLabel() - 'A'));
                    item.setGridCol(Math.max(0, item.getColNumber() - 1));
                } else if (item.getRowLabel() == null || item.getColNumber() <= 0) {
                    item.setGridRow(dummyGrid / 10);
                    item.setGridCol(dummyGrid % 10);
                    dummyGrid++;
                }
            }
        }

        int maxGridRow = mappedSeats.stream().mapToInt(SeatMapResponse.SeatItem::getGridRow).max().orElse(0);
        int maxGridCol = mappedSeats.stream().mapToInt(SeatMapResponse.SeatItem::getGridCol).max().orElse(0);

        return SeatMapResponse.builder()
                .showtimeId(showtimeId.toString())
                .totalRows(showtime.getScreen().getTotalRows())
                .totalCols(showtime.getScreen().getTotalCols())
                .maxGridRow(maxGridRow)
                .maxGridCol(maxGridCol)
                .seats(mappedSeats)
                .seatHoldMins(seatHoldMins)
                .build();
    }

    // ── ADMIN: tạo suất chiếu ─────────────────────────────────────────────

    @Override
    public ShowtimeResponse create(ShowtimeRequest request) {
        // Validation: Phải là tương lai (Múi giờ VN)
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        if (request.getStartTime().isBefore(now.plusMinutes(5))) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            throw new BadRequestException("Suất chiếu phải bắt đầu sau ít nhất 5 phút kể từ hiện tại (Giờ hệ thống: "
                    + now.format(formatter) + "). Vui lòng chọn thời gian muộn hơn.");
        }

        Movie movie = movieService.findById(UUID.fromString(request.getMovieId()));
        Screen screen = screenRepository.findById(UUID.fromString(request.getScreenId()))
                .orElseThrow(() -> new ResourceNotFoundException("Phòng chiếu", request.getScreenId()));

        // Lấy thời gian dọn phòng từ cấu hình (đã được Cache)
        int cleanupMins = systemConfigService.getIntConfig("CLEANUP_TIME_MINUTES", 15);
        LocalDateTime endTime = request.getStartTime().plusMinutes(movie.getDuration() + cleanupMins);

        if (showtimeRepository.existsConflict(screen.getId(), request.getStartTime(), endTime)) {
            throw new BadRequestException("Phòng chiếu đã có suất chiếu khác trong khung giờ này (bao gồm "
                    + cleanupMins + " phút dọn phòng)");
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
        List<Booking> bookings = bookingRepository.findByShowtimeId(showtime.getId());

        // Kiểm tra xem có giao dịch thành công nào không
        boolean hasActiveTransactions = bookings.stream()
                .anyMatch(b -> b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.CHECKED_IN);

        if (hasActiveTransactions) {
            throw new ConflictException(
                    "Không thể xóa suất chiếu đã phát sinh giao dịch thanh toán thành công (PAID/CHECKED_IN). Vui lòng hủy suất chiếu hoặc hoàn tiền trước.");
        }

        // Nếu chỉ có các đơn nháp hoặc đã hủy/quá hạn, tiến hành dọn dẹp sạch sẽ
        for (Booking booking : bookings) {
            paymentRepository.findByBookingId(booking.getId()).ifPresent(paymentRepository::delete);
            bookingRepository.delete(booking);
        }

        showtimeSeatRepository.deleteByShowtimeId(showtime.getId());
        showtimeRepository.delete(showtime);
    }

    @Override
    public void overrideSeatPrices(UUID showtimeId,
            OverrideSeatPriceRequest request) {
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
        List<PricingRule> activeRules = pricingRuleRepository.findByIsActiveTrueOrderByPriorityAsc();

        List<ShowtimeSeat> showtimeSeats = seats.stream().map(seat -> {
            PricingResult result = pricingEngineService.calculateFinalSeatPrice(showtime, seat, basePrice,
                    activeRules, 1, 0);
            return ShowtimeSeat.builder()
                    .showtime(showtime)
                    .seat(seat)
                    .status(SeatStatus.AVAILABLE)
                    .price(result.finalPrice()) // Snapshot price at creation
                    .build();
        }).toList();
        showtimeSeatRepository.saveAll(showtimeSeats);
    }

}
