package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.service.BookingService;
import com.cinema.ticket_booking.service.VoucherService;
import com.cinema.ticket_booking.service.UserService;
import com.cinema.ticket_booking.service.ShowtimeService;
import com.cinema.ticket_booking.service.QrCodeService;
import com.cinema.ticket_booking.service.EmailService;
import com.cinema.ticket_booking.service.PricingEngineService;
import com.cinema.ticket_booking.dto.request.BookingRequest;
import com.cinema.ticket_booking.dto.response.BookingResponse;
import com.cinema.ticket_booking.dto.response.CheckInResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.model.*;
import com.cinema.ticket_booking.enums.*;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.BookingMapper;
import com.cinema.ticket_booking.repository.*;
import com.cinema.ticket_booking.service.SeatLockService;
import com.cinema.ticket_booking.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final BookingComboRepository bookingComboRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final TicketRepository ticketRepository;
    private final ComboRepository comboRepository;
    private final UserService userService;
    private final ShowtimeService showtimeService;
    private final VoucherService voucherService;
    private final QrCodeService qrCodeService;
    private final EmailService emailService;
    private final TransactionRepository transactionRepository;
    private final BookingMapper bookingMapper;
    private final StaffProfileRepository staffProfileRepository;
    private final SeatLockService seatLockService;
    private final SystemConfigService systemConfigService;
    private final PricingEngineService pricingEngineService;
    private final UserVoucherRepository userVoucherRepository;
    private final PricingRuleRepository pricingRuleRepository;

    // ── Tạo booking ───────────────────────────────────────────────────────

    /**
     * Luồng đặt vé:
     * 1. Validate + lock ghế (SELECT FOR UPDATE qua @Transactional)
     * 2. Validate voucher nếu có
     * 3. Tính tổng tiền
     * 4. Tạo Booking → BookingItem → BookingCombo
     * 5. Trả về BookingResponse (chưa có QR — chờ thanh toán)
     */
    @Override
    public BookingResponse createBooking(UUID userId, BookingRequest request) {
        User user = userService.findById(userId);
        Showtime showtime = showtimeService.findById(UUID.fromString(request.getShowtimeId()));

        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new BadRequestException("Suất chiếu không còn nhận đặt vé");
        }

        // ── 1. Lock ghế ────────────────────────────────────────────────────
        List<UUID> seatIds = request.getShowtimeSeatIds()
                .stream().map(UUID::fromString).toList();

        List<ShowtimeSeat> seats = showtimeSeatRepository
                .findByShowtimeAndIds(showtime.getId(), seatIds);

        if (seats.size() != seatIds.size()) {
            throw new BadRequestException("Một số ghế không thuộc suất chiếu này");
        }

        long minutesToStart = Duration.between(LocalDateTime.now(), showtime.getStartTime()).toMinutes();
        int pendingMins = systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10);
        if (minutesToStart <= 15 && minutesToStart >= -10) {
            pendingMins = systemConfigService.getIntConfig("LATE_SEAT_HOLD_TIME", 3);
        }
        LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(pendingMins);

        String tempBookingRef = UUID.randomUUID().toString(); // Use a temp booking code to lock
        List<String> lockedSoFar = new ArrayList<>();
        try {
            for (ShowtimeSeat ss : seats) {
                if (ss.getStatus() != SeatStatus.AVAILABLE) {
                    throw new BadRequestException(
                            "Ghế " + ss.getSeat().getRowLabel() + ss.getSeat().getColNumber() + " đã được bán");
                }
                boolean locked = seatLockService.lockSeat(ss.getId().toString(), tempBookingRef,
                        Duration.ofMinutes(pendingMins));
                if (!locked) {
                    throw new BadRequestException("Ghế " + ss.getSeat().getRowLabel() + ss.getSeat().getColNumber()
                            + " đang được giữ bởi người khác");
                }
                lockedSoFar.add(ss.getId().toString());
            }
        } catch (Exception e) {
            // Release the locks we acquired
            seatLockService.releaseSeats(lockedSoFar);
            throw e;
        }

        // ── 2. Tính tiền vé bằng PricingEngine ─────────────────────────────
        List<PricingRule> activeRules = pricingRuleRepository.findByIsActiveTrueOrderByPriorityAsc();
        List<BigDecimal> finalSeatPrices = new ArrayList<>();
        BigDecimal seatTotal = BigDecimal.ZERO;

        for (ShowtimeSeat ss : seats) {
            BigDecimal dynamicPrice = pricingEngineService.calculateFinalSeatPrice(showtime, ss.getSeat(), showtime.getBasePrice(), activeRules);
            finalSeatPrices.add(dynamicPrice);
            seatTotal = seatTotal.add(dynamicPrice);
        }

        BigDecimal comboTotal = BigDecimal.ZERO;
        List<BookingComboData> comboDataList = new ArrayList<>();

        if (request.getCombos() != null) {
            for (BookingRequest.ComboItem item : request.getCombos()) {
                Combo combo = comboRepository.findById(UUID.fromString(item.getComboId()))
                        .orElseThrow(() -> new ResourceNotFoundException("Combo", item.getComboId()));
                if (!combo.getIsAvailable())
                    throw new BadRequestException("Combo '" + combo.getName() + "' hiện không có sẵn");
                BigDecimal subtotal = combo.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                comboTotal = comboTotal.add(subtotal);
                comboDataList.add(new BookingComboData(combo, item.getQuantity(), combo.getPrice()));
            }
        }

        BigDecimal subtotal = seatTotal.add(comboTotal);
        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher voucher = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            voucher = voucherService.validateForOrder(request.getVoucherCode(), subtotal);
            discountAmount = voucher.calculateDiscount(subtotal);
        }

        BigDecimal totalAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);

        long pendingExp = totalAmount.divide(BigDecimal.valueOf(1000)).longValue();

        // ── 3. Tạo Booking ────────────────────────────────────────────────
        String finalBookingCode = generateBookingCode();
        Booking booking = Booking.builder()
                .user(user)
                .showtime(showtime)
                .bookingCode(finalBookingCode)
                .voucher(voucher)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING)
                .expiresAt(lockUntil)
                .pendingExp(pendingExp)
                .expAdded(false)
                .build();
        booking = bookingRepository.save(booking);

        // Update the Redis lock with final bookingId although it uses the same TTL
        for (ShowtimeSeat ss : seats) {
            seatLockService.lockSeat(ss.getId().toString(), booking.getId().toString(),
                    Duration.ofMinutes(pendingMins));
        }

        // ── 4. Tạo BookingItem + Ticket ───────────────────────────────────
        for (int i = 0; i < seats.size(); i++) {
            ShowtimeSeat ss = seats.get(i);
            BigDecimal seatPrice = finalSeatPrices.get(i);
            BookingItem item = BookingItem.builder()
                    .booking(booking)
                    .showtimeSeat(ss)
                    .seatPrice(seatPrice)
                    .build();
            item = bookingItemRepository.save(item);

            Ticket ticket = Ticket.builder()
                    .booking(booking)
                    .bookingItem(item)
                    .isUsed(false)
                    .build();
            ticketRepository.save(ticket);
        }

        // ── 5. Tạo BookingCombo ───────────────────────────────────────────
        for (BookingComboData data : comboDataList) {
            bookingComboRepository.save(BookingCombo.builder()
                    .booking(booking)
                    .combo(data.combo)
                    .quantity(data.quantity)
                    .unitPrice(data.unitPrice)
                    .build());
        }

        return buildFullResponse(booking, seats, comboDataList, subtotal);
    }

    // ── Lịch sử đặt vé ───────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse.Summary> getMyBookings(UUID userId, Pageable pageable) {
        org.springframework.data.domain.Page<Booking> page = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId,
                pageable);
        List<BookingResponse.Summary> summaries = page.getContent().stream()
                .map(booking -> {
                    BookingResponse.Summary summary = bookingMapper.toSummary(booking);
                    List<String> seats = bookingItemRepository.findByBookingIdWithSeat(booking.getId()).stream()
                            .map(item -> item.getShowtimeSeat().getSeat().getRowLabel()
                                    + String.valueOf(item.getShowtimeSeat().getSeat().getColNumber()))
                            .toList();
                    summary.setSeats(String.join(", ", seats));
                    summary.setScreenType(getScreenTypeName(booking));
                    return summary;
                }).toList();
        return PageResponse
                .of(new org.springframework.data.domain.PageImpl<>(summaries, pageable, page.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getDetail(UUID userId, UUID bookingId) {
        Booking booking = findById(bookingId);
        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xem đơn đặt vé này");
        }

        List<ShowtimeSeat> seats = bookingItemRepository
                .findByBookingIdWithSeat(bookingId)
                .stream().map(BookingItem::getShowtimeSeat).toList();

        List<BookingComboData> combos = bookingComboRepository.findByBookingId(bookingId)
                .stream().map(bc -> new BookingComboData(bc.getCombo(), bc.getQuantity(), bc.getUnitPrice())).toList();

        BigDecimal subtotal = seats.stream().map(ShowtimeSeat::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(combos.stream().map(c -> c.unitPrice.multiply(BigDecimal.valueOf(c.quantity)))
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        return buildFullResponse(booking, seats, combos, subtotal);
    }

    // ── Review Eligibility ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UUID getEligibleBookingForReview(UUID userId, UUID movieId) {
        return bookingRepository.findEligibleBookingForReview(userId, movieId)
                .map(Booking::getId)
                .orElse(null);
    }

    // ── Check-in QR ───────────────────────────────────────────────────────

    /**
     * Nhân viên quét QR → lấy toàn bộ ticket của booking → mark is_used = true.
     */
    @Override
    public CheckInResponse checkIn(User staff, String qrCode) {
        Booking booking = bookingRepository.findByQrCode(qrCode)
                .orElseGet(() -> bookingRepository.findByBookingCode(qrCode)
                        .orElseThrow(() -> new BadRequestException("QR hoặc Mã đặt vé không tồn tại")));

        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            throw new BadRequestException("Đơn vé này đã được check-in trước đó");
        }
        if (booking.getStatus() != BookingStatus.PAID) {
            throw new BadRequestException("Đơn vé chưa thanh toán hoặc đã huỷ/hết hạn");
        }

        LocalDateTime showTimeStart = booking.getShowtime().getStartTime();
        if (!showTimeStart.toLocalDate().equals(LocalDateTime.now().toLocalDate())) {
            throw new BadRequestException("Suất chiếu không thuộc ngày hôm nay");
        }

        // ── Cinema validation ─────────────────────────────────────────────
        // ADMIN: bypass hoàn toàn (không cần gọi DB)
        // STAFF: kiểm tra xem có StaffProfile và có thuộc rạp này không
        if (staff.getRole() == UserRole.STAFF) {
            UUID bookingCinemaId = booking.getShowtime().getScreen().getCinema().getId();
            staffProfileRepository.findByUserId(staff.getId())
                    .ifPresent(profile -> {
                        if (!profile.getCinema().getId().equals(bookingCinemaId)) {
                            throw new BadRequestException(
                                    "Bạn không có quyền quét vé cho rạp '" +
                                            booking.getShowtime().getScreen().getCinema().getName() + "'");
                        }
                    });
        }

        List<Ticket> tickets = ticketRepository.findByBookingIdWithSeatDetail(booking.getId());
        LocalDateTime now = LocalDateTime.now();

        for (Ticket ticket : tickets) {
            if (!ticket.getIsUsed()) {
                ticket.setIsUsed(true);
                ticket.setUsedAt(now);
            }
        }
        ticketRepository.saveAll(tickets);

        booking.setStatus(BookingStatus.CHECKED_IN);
        if (!Boolean.TRUE.equals(booking.getExpAdded())) {
            User bookingUser = booking.getUser();
            long currentExp = bookingUser.getRewardPoints() != null ? bookingUser.getRewardPoints() : 0L;
            bookingUser.setRewardPoints(currentExp + booking.getPendingExp());
            userService.save(bookingUser);
            booking.setExpAdded(true);
        }
        bookingRepository.save(booking);

        List<CheckInResponse.SeatItem> seatItems = tickets.stream().map(t -> CheckInResponse.SeatItem.builder()
                .rowLabel(t.getBookingItem().getShowtimeSeat().getSeat().getRowLabel())
                .colNumber(t.getBookingItem().getShowtimeSeat().getSeat().getColNumber())
                .seatType(t.getBookingItem().getShowtimeSeat().getSeat().getSeatType().name())
                .isUsed(true)
                .build()).toList();

        return CheckInResponse.builder()
                .bookingCode(booking.getBookingCode())
                .movieTitle(booking.getShowtime().getMovie().getTitle())
                .startTime(booking.getShowtime().getStartTime())
                .cinemaName(booking.getShowtime().getScreen().getCinema().getName())
                .screenName(booking.getShowtime().getScreen().getName())
                .seats(seatItems)
                .allCheckedIn(true)
                .checkedInAt(now)
                .build();
    }

    // ── Gọi từ PaymentService sau khi thanh toán thành công ───────────────
    @Override
    public void confirmPaid(UUID bookingId) {
        Booking booking = findById(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Trạng thái booking không hợp lệ để xác nhận thanh toán");
        }

        // Mark ghế → BOOKED
        List<String> showtimeSeatIdsToRelease = new ArrayList<>();
        bookingItemRepository.findByBookingIdWithSeat(bookingId).forEach(item -> {
            ShowtimeSeat ss = item.getShowtimeSeat();
            ss.setStatus(SeatStatus.BOOKED);
            ss.setLockedBy(null);
            ss.setLockedUntil(null);
            showtimeSeatRepository.save(ss);
            showtimeSeatIdsToRelease.add(ss.getId().toString());
        });

        seatLockService.releaseSeats(showtimeSeatIdsToRelease);

        // Tạo QR code
        String qrContent = qrCodeService.generateQrContent(booking);
        booking.setQrCode(qrContent);
        booking.setStatus(BookingStatus.PAID);
        bookingRepository.save(booking);

        // Tăng usedCount voucher và đánh dấu used trong ví
        if (booking.getVoucher() != null) {
            voucherService.incrementUsedCount(booking.getVoucher().getId());
            userVoucherRepository.findByUserIdAndVoucherId(booking.getUser().getId(), booking.getVoucher().getId())
                .ifPresent(uv -> {
                    uv.setIsUsed(true);
                    userVoucherRepository.save(uv);
                });
        }
    }

    @Override
    public void requestCancelBooking(UUID userId, UUID bookingId) {
        Booking booking = findById(bookingId);
        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền huỷ đơn đặt vé này");
        }
        if (booking.getStatus() != BookingStatus.PAID) {
            throw new BadRequestException("Chỉ có thể huỷ đơn đặt vé đã thanh toán thành công");
        }

        // Điều kiện huỷ vé: trước giờ chiếu 1 tiếng
        LocalDateTime showtimeStartTime = booking.getShowtime().getStartTime();
        if (LocalDateTime.now().isAfter(showtimeStartTime.minusHours(1))) {
            throw new BadRequestException("Chỉ được huỷ vé trước giờ chiếu ít nhất 1 tiếng");
        }

        // Tạo cancellation token hết hạn sau 15 phút
        String token = UUID.randomUUID().toString();
        booking.setCancellationToken(token);
        booking.setCancellationTokenExpiry(LocalDateTime.now().plusMinutes(15));
        bookingRepository.save(booking);

        // Gửi email
        emailService.sendCancellationConfirmEmail(booking, token);
    }

    @Override
    public void confirmCancelBooking(String token, UUID bookingId) {
        Booking booking = findById(bookingId);

        // Verify token
        if (booking.getCancellationToken() == null || !booking.getCancellationToken().equals(token)) {
            throw new BadRequestException("Mã xác nhận huỷ vé không hợp lệ");
        }
        if (booking.getCancellationTokenExpiry() != null
                && LocalDateTime.now().isAfter(booking.getCancellationTokenExpiry())) {
            throw new BadRequestException("Mã xác nhận huỷ vé đã hết hạn");
        }
        if (booking.getStatus() != BookingStatus.PAID) {
            throw new BadRequestException("Trạng thái đơn vé không hợp lệ");
        }

        // 1. Nhả ghế
        releaseSeats(bookingId);

        // 2. Cập nhật booking status
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationToken(null);
        booking.setCancellationTokenExpiry(null);
        booking.setPendingExp(0L); // Cancel points
        bookingRepository.save(booking);

        // 3. Hoàn CinePoint cho user (tỷ lệ 1000 VNĐ = 1 CinePoint)
        User user = booking.getUser();
        long rewardPointsToAdd = booking.getTotalAmount().longValue() / 1000;
        long currentPoints = user.getRewardPoints() != null ? user.getRewardPoints() : 0L;
        user.setRewardPoints(currentPoints + rewardPointsToAdd);
        userService.save(user);

        // 4. Lưu lịch sử hoàn tiền
        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(booking.getTotalAmount())
                .type(TransactionType.REFUND)
                .status(TransactionStatus.SUCCESS)
                .referenceId(booking.getBookingCode())
                .description("Hoàn trả CinePoint sau khi hủy vé " + booking.getBookingCode())
                .build();
        transactionRepository.save(transaction);
    }

    // ── Private ───────────────────────────────────────────────────────────

    private void releaseSeats(UUID bookingId) {
        bookingItemRepository.findByBookingIdWithSeat(bookingId).forEach(item -> {
            ShowtimeSeat ss = item.getShowtimeSeat();
            ss.setStatus(SeatStatus.AVAILABLE);
            ss.setLockedBy(null);
            ss.setLockedUntil(null);
            showtimeSeatRepository.save(ss);
        });
    }

    private String generateBookingCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randPart = String.format("%04d", (int) (Math.random() * 10000));
        return "BK" + datePart + randPart;
    }

    private BookingResponse buildFullResponse(Booking booking, List<ShowtimeSeat> seats,
            List<BookingComboData> combos, BigDecimal subtotal) {
        BookingResponse response = bookingMapper.toResponse(booking);
        response.setSubtotal(subtotal);
        response.setScreenType(getScreenTypeName(booking));

        response.setSeats(seats.stream().map(ss -> BookingResponse.SeatItem.builder()
                .showtimeSeatId(ss.getId().toString())
                .rowLabel(ss.getSeat().getRowLabel())
                .colNumber(ss.getSeat().getColNumber())
                .seatType(ss.getSeat().getSeatType().name())
                .price(ss.getPrice())
                .build()).toList());

        response.setCombos(combos.stream().map(c -> BookingResponse.ComboItem.builder()
                .comboId(c.combo.getId().toString())
                .comboName(c.combo.getName())
                .quantity(c.quantity)
                .unitPrice(c.unitPrice)
                .subtotal(c.unitPrice.multiply(BigDecimal.valueOf(c.quantity)))
                .build()).toList());

        return response;
    }

    @Override
    public Booking findById(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn đặt vé", id));
    }

    private String getScreenTypeName(Booking booking) {
        if (booking.getShowtime() != null && 
            booking.getShowtime().getScreen() != null && 
            booking.getShowtime().getScreen().getScreenType() != null) {
            return booking.getShowtime().getScreen().getScreenType().name();
        }
        return "STANDARD";
    }

    private record BookingComboData(Combo combo, Integer quantity, BigDecimal unitPrice) {
    }
}
