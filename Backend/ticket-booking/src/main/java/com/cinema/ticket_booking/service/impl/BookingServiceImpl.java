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
import com.cinema.ticket_booking.dto.PricingResult;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Objects;

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
    private final UserExpHistoryRepository userExpHistoryRepository;

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

        // ── 2. Tính toán Giá nền (sau khi áp dụng Thứ/Giờ/Loại ghế) ─────────
        List<PricingRule> activeRules = pricingRuleRepository.findByIsActiveTrueOrderByPriorityAsc();
        List<BigDecimal> baseSeatPrices = new ArrayList<>();
        BigDecimal totalTicketBasePrice = BigDecimal.ZERO;

        for (ShowtimeSeat ss : seats) {
            // Chỉ lấy giá sau khi đã tính các quy tắc cơ bản (không tính Khuyến mãi ở bước
            // này)
            PricingResult baseResult = pricingEngineService.calculateFinalSeatPrice(
                    showtime, ss.getSeat(), showtime.getBasePrice(), activeRules, seats.size(), 0);

            // Note: Chúng ta cần giá 'đã qua điều chỉnh khung giờ/loại ghế' nhưng CHƯA trừ
            // khuyến mãi.
            // Thuật toán Vô địch sẽ tính mức giảm dựa trên giá đã điều chỉnh này.
            BigDecimal adjustedBase = baseResult.finalPrice().add(baseResult.discountAmount());
            baseSeatPrices.add(adjustedBase);
            totalTicketBasePrice = totalTicketBasePrice.add(adjustedBase);
        }

        BigDecimal totalComboBasePrice = BigDecimal.ZERO;
        List<BookingComboData> comboDataList = new ArrayList<>();
        int totalComboQty = 0;

        if (request.getCombos() != null) {
            for (BookingRequest.ComboItem item : request.getCombos()) {
                Combo combo = comboRepository.findById(UUID.fromString(item.getComboId()))
                        .orElseThrow(() -> new ResourceNotFoundException("Combo", item.getComboId()));
                if (!combo.getIsAvailable())
                    throw new BadRequestException("Combo '" + combo.getName() + "' hiện không có sẵn");

                BigDecimal subtotal = combo.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                totalComboBasePrice = totalComboBasePrice.add(subtotal);
                totalComboQty += item.getQuantity();
                comboDataList.add(new BookingComboData(combo, item.getQuantity(), combo.getPrice()));
            }
        }

        // ── 3. Chạy thuật toán 'Nhà Vô Địch' cho Khuyến mãi ────────────────
        PricingResult bestPromo = pricingEngineService.calculateBestOrderPromotion(
                showtime, seats.size(), totalComboQty, totalTicketBasePrice, totalComboBasePrice, activeRules);

        BigDecimal promotionDiscount = bestPromo.discountAmount();
        String appliedPromoName = bestPromo.appliedPromotionName();
        BigDecimal seatOriginalTotal = totalTicketBasePrice; // Tổng giá vé sau khi áp dụng rules cơ bản (Thứ/Giờ)

        BigDecimal subtotal = totalTicketBasePrice.add(totalComboBasePrice).subtract(promotionDiscount);
        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher voucher = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            voucher = voucherService.validateForOrder(request.getVoucherCode(), subtotal);
            discountAmount = voucher.calculateDiscount(subtotal);
        }

        BigDecimal totalAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);

        long earnedExp = totalAmount.divide(BigDecimal.valueOf(1000)).longValue();

        // ── 3. Tạo Booking ────────────────────────────────────────────────
        String finalBookingCode = generateBookingCode();
        Booking booking = Booking.builder()
                .user(user)
                .showtime(showtime)
                .bookingCode(finalBookingCode)
                .voucher(voucher)
                .discountAmount(discountAmount)
                .promotionDiscountAmount(promotionDiscount)
                .appliedPromotionName(appliedPromoName)
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING)
                .expiresAt(lockUntil)
                .earnedExp(earnedExp)
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
            // Quan trọng: Lưu giá vé đã tính các quy tắc cơ bản (Thành tiền tạm tính của
            // vé)
            // Khuyến mãi hệ thống sẽ được trừ ở cấp độ tổng đơn hàng (promotionDiscount)
            BigDecimal seatPrice = baseSeatPrices.get(i);
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

        return buildFullResponse(booking, seats, comboDataList, subtotal, seatOriginalTotal.add(totalComboBasePrice),
                promotionDiscount, appliedPromoName);
    }

    // ── Lịch sử đặt vé ───────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse.Summary> getMyBookings(UUID userId, Pageable pageable) {
        Page<Booking> page = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId,
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
                .of(new PageImpl<>(summaries, pageable, page.getTotalElements()));
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

        BigDecimal seatOriginalTotal = seats.stream().map(s -> booking.getShowtime().getBasePrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal comboTotal = combos.stream().map(c -> c.unitPrice.multiply(BigDecimal.valueOf(c.quantity)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Lấy thông tin khuyến mãi hệ thống an toàn (Null-safe cho đơn hàng cũ)
        BigDecimal promotionDiscount = Objects.requireNonNullElse(booking.getPromotionDiscountAmount(),
                BigDecimal.ZERO);
        String appliedPromoName = booking.getAppliedPromotionName();

        // subtotal là tổng tiền sau khi đã trừ khuyến mãi hệ thống (để voucher tính
        // toán dựa trên mức này)
        BigDecimal subtotal = seats.stream().map(ShowtimeSeat::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(comboTotal).subtract(promotionDiscount);

        return buildFullResponse(booking, seats, combos, subtotal, seatOriginalTotal.add(comboTotal),
                promotionDiscount, appliedPromoName);
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
        
        // Cập nhật EXP và Hạng thành viên ngay khi thanh toán thành công
        if (!Boolean.TRUE.equals(booking.getExpAdded())) {
            User bookingUser = booking.getUser();
            long earnedExp = booking.getEarnedExp() != null ? booking.getEarnedExp() : 0L;
            long currentExp = bookingUser.getAvailableExp() != null ? bookingUser.getAvailableExp() : 0L;
            
            bookingUser.setAvailableExp(currentExp + earnedExp);
            updateMembershipTier(bookingUser);
            
            userService.save(bookingUser);
            booking.setExpAdded(true);

            // Ghi log lịch sử EXP
            if (earnedExp > 0) {
                userExpHistoryRepository.save(UserExpHistory.builder()
                        .user(bookingUser)
                        .amount(earnedExp)
                        .reason("MUA_VE")
                        .referenceId(booking.getBookingCode())
                        .build());
            }
        }

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

        // Điều kiện huỷ vé: trước giờ chiếu X tiếng (từ SystemConfig)
        LocalDateTime showtimeStartTime = booking.getShowtime().getStartTime();
        int minHoursBefore = systemConfigService.getIntConfig("CANCEL_MIN_HOURS_BEFORE", 2);
        if (LocalDateTime.now().isAfter(showtimeStartTime.minusHours(minHoursBefore))) {
            throw new BadRequestException("Chỉ được huỷ vé trước giờ chiếu ít nhất " + minHoursBefore + " tiếng");
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

        // Thu hồi EXP (Rollback logic)
        User actionUser = booking.getUser();
        if (Boolean.TRUE.equals(booking.getExpAdded())) {
            long revokeExp = booking.getEarnedExp() != null ? booking.getEarnedExp() : 0L;
            if (revokeExp > 0) {
                long currentExp = actionUser.getAvailableExp() != null ? actionUser.getAvailableExp() : 0L;
                actionUser.setAvailableExp(Math.max(0, currentExp - revokeExp));
                updateMembershipTier(actionUser);
                userService.save(actionUser);

                // Ghi log thu hồi EXP
                userExpHistoryRepository.save(UserExpHistory.builder()
                        .user(actionUser)
                        .amount(-revokeExp)
                        .reason("HUY_VE")
                        .referenceId(booking.getBookingCode())
                        .build());
            }
            booking.setExpAdded(false);
        }

        // 2. Cập nhật booking status
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationToken(null);
        booking.setCancellationTokenExpiry(null);
        booking.setEarnedExp(0L); // Cancel points
        bookingRepository.save(booking);

        // 3. Hoàn CinePoint cho user (tỷ lệ X% số tiền, mặc định 1000 VNĐ = 1 CinePoint)
        User user = booking.getUser();
        int refundPercent = systemConfigService.getIntConfig("REFUND_PERCENT_CINEPOINT", 100);
        long rewardPointsToAdd = (booking.getTotalAmount().longValue() * refundPercent / 100) / 1000;
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

    private void updateMembershipTier(User user) {
        long exp = user.getAvailableExp();
        MembershipTier currentTier = user.getMembershipTier();
        MembershipTier newTier = currentTier;

        if (exp >= 10000) {
            newTier = MembershipTier.DIAMOND;
        } else if (exp >= 3000) {
            newTier = MembershipTier.GOLD;
        } else if (exp >= 500) {
            newTier = MembershipTier.SILVER;
        } else {
            newTier = MembershipTier.BRONZE;
        }

        if (newTier != currentTier) {
            user.setMembershipTier(newTier);
        }
    }

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
            List<BookingComboData> combos, BigDecimal subtotal, BigDecimal originalTotal,
            BigDecimal promotionDiscount, String appliedPromoName) {
        BookingResponse response = bookingMapper.toResponse(booking);
        response.setSubtotal(subtotal);
        response.setTotalOriginalAmount(originalTotal);
        response.setPromotionDiscountAmount(promotionDiscount);
        response.setAppliedPromotionName(appliedPromoName);
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
