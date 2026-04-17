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
import com.cinema.ticket_booking.exception.ForbiddenException;
import com.cinema.ticket_booking.mapper.BookingMapper;
import com.cinema.ticket_booking.repository.*;
import com.cinema.ticket_booking.service.SeatLockService;
import com.cinema.ticket_booking.service.SystemConfigService;
import com.cinema.ticket_booking.enums.UserVoucherStatus;
import com.cinema.ticket_booking.repository.UserVoucherRepository;
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
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

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
    public BookingResponse calculateQuote(UUID userId, BookingRequest request) {
        User user = userService.findById(userId);

        // ── 0. Phân quyền & Ràng buộc nghiệp vụ ──────────────────────────
        // Trong luồng hiện tại, userId chính là người thực hiện request (Staff hoặc
        // Customer)
        boolean isStaffOrAdmin = user.getRole() == UserRole.STAFF || user.getRole() == UserRole.ADMIN;

        if (!isStaffOrAdmin) {
            // KHÁCH HÀNG: Bắt buộc chọn Phim & Ghế
            if (request.getShowtimeId() == null || request.getShowtimeId().isBlank() ||
                    request.getShowtimeSeatIds() == null || request.getShowtimeSeatIds().isEmpty()) {
                throw new BadRequestException("Khách hàng đặt vé bắt buộc phải chọn Suất chiếu và Ghế");
            }
        } else {
            // POS (STAFF): Nếu không chọn Phim thì phải có Combo
            if ((request.getShowtimeId() == null || request.getShowtimeId().isBlank()) &&
                    (request.getCombos() == null || request.getCombos().isEmpty())) {
                throw new BadRequestException("Phải chọn ít nhất 1 Suất chiếu hoặc 1 Combo bắp nước");
            }
        }

        Showtime showtime = null;
        if (request.getShowtimeId() != null && !request.getShowtimeId().isBlank()) {
            showtime = showtimeService.findById(UUID.fromString(request.getShowtimeId()));
            if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
                throw new BadRequestException("Suất chiếu không còn nhận đặt vé");
            }
        }

        // ── 1. Chuẩn bị ghế ────────────────────────────────────────────────
        List<ShowtimeSeat> seats = new ArrayList<>();
        BigDecimal totalTicketBasePrice = BigDecimal.ZERO;
        List<PricingRule> activeRules = pricingRuleRepository.findByIsActiveTrueOrderByPriorityAsc();

        if (showtime != null && request.getShowtimeSeatIds() != null && !request.getShowtimeSeatIds().isEmpty()) {
            List<UUID> seatIds = request.getShowtimeSeatIds()
                    .stream().map(UUID::fromString).toList();

            seats = showtimeSeatRepository.findByShowtimeAndIds(showtime.getId(), seatIds);

            if (seats.size() != seatIds.size()) {
                throw new BadRequestException("Một số ghế không thuộc suất chiếu này");
            }

            // ── 2. Tính toán Giá nền ─────────
            for (ShowtimeSeat ss : seats) {
                PricingResult baseResult = pricingEngineService.calculateFinalSeatPrice(
                        showtime, ss.getSeat(), showtime.getBasePrice(), activeRules, seats.size(), 0);

                BigDecimal adjustedBase = baseResult.finalPrice().add(baseResult.discountAmount());
                totalTicketBasePrice = totalTicketBasePrice.add(adjustedBase);
            }
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

                BigDecimal totalComboPrice = combo.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                totalComboBasePrice = totalComboBasePrice.add(totalComboPrice);
                totalComboQty += item.getQuantity();
                comboDataList.add(new BookingComboData(combo, item.getQuantity(), combo.getPrice()));
            }
        }

        // Check if nothing is selected
        if (showtime == null && comboDataList.isEmpty()) {
            throw new BadRequestException("Phải chọn ít nhất 1 ghế hoặc 1 combo");
        }

        // ── 3. Chạy thuật toán 'Nhà Vô Địch' cho Khuyến mãi ────────────────
        PricingResult bestPromo = new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, null);

        if (showtime != null) {
            bestPromo = pricingEngineService.calculateBestOrderPromotion(
                    showtime, seats.size(), totalComboQty, totalTicketBasePrice, totalComboBasePrice, activeRules);
        }

        BigDecimal promotionDiscount = bestPromo.discountAmount();
        String appliedPromoName = bestPromo.appliedPromotionName();
        BigDecimal originalTotal = totalTicketBasePrice.add(totalComboBasePrice);

        BigDecimal amountAfterPromo = originalTotal.subtract(promotionDiscount);
        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher voucher = null;
        String warningMessage = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            try {
                voucher = voucherService.validateForOrder(userId, request.getVoucherCode(), amountAfterPromo);
                discountAmount = voucher.calculateDiscount(amountAfterPromo);
            } catch (Exception e) {
                warningMessage = "Mã giảm giá không hợp lệ hoặc đã hết hạn: " + e.getMessage();
            }
        }

        BigDecimal totalAmount = amountAfterPromo.subtract(discountAmount).max(BigDecimal.ZERO);

        // Tạo transient booking object cho mapper
        Booking transientBooking = Booking.builder()
                .user(user)
                .showtime(showtime)
                .voucher(voucher)
                .discountAmount(discountAmount)
                .promotionDiscountAmount(promotionDiscount)
                .appliedPromotionName(appliedPromoName)
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING)
                .build();

        // Gán cinema tạm thời cho mục đích hiển thị
        if (showtime != null) {
            transientBooking.setCinema(showtime.getScreen().getCinema());
        }

        BookingResponse response = buildFullResponse(transientBooking, seats, comboDataList, amountAfterPromo,
                originalTotal,
                promotionDiscount, appliedPromoName);
        response.setWarningMessage(warningMessage);
        return response;
    }

    @Override
    public BookingResponse createBooking(UUID userId, BookingRequest request) {
        // ── 1. Tính toán quote để lấy con số chính xác ──────────────────────
        BookingResponse quote = calculateQuote(userId, request);

        // ── 2. Chốt chặn bảo mật: Nếu quote có warning (Voucher xịt) -> Chặn luôn giao
        // dịch thật
        if (quote.getWarningMessage() != null) {
            throw new BadRequestException(quote.getWarningMessage());
        }

        User user = userService.findById(userId);

        // ── 0. Phân quyền & Ràng buộc nghiệp vụ (Audit cho POS) ────────────
        User processedBy = null;
        Cinema cinema = null;

        boolean isStaffOrAdmin = user.getRole() == UserRole.STAFF || user.getRole() == UserRole.ADMIN;

        if (isStaffOrAdmin) {
            processedBy = user;
            if (user.getRole() == UserRole.STAFF) {
                cinema = staffProfileRepository.findByUserId(user.getId())
                        .map(StaffProfile::getCinema)
                        .orElse(null);
            }
        }

        if (!isStaffOrAdmin) {
            // ONLINE CUSTOMER Flow: Validate Showtime & Seats
            if (request.getShowtimeId() == null || request.getShowtimeId().isBlank() ||
                    request.getShowtimeSeatIds() == null || request.getShowtimeSeatIds().isEmpty()) {
                throw new BadRequestException("Khách hàng đặt vé Online bắt buộc phải chọn Suất chiếu và Ghế");
            }
        } else {
            // POS (STAFF) Flow: Validate at least one item
            if ((request.getShowtimeId() == null || request.getShowtimeId().isBlank()) &&
                    (request.getCombos() == null || request.getCombos().isEmpty())) {
                throw new BadRequestException("Vui lòng chọn ít nhất 1 Suất chiếu hoặc 1 Combo bắp nước để thanh toán");
            }
        }

        Showtime showtime = null;
        if (request.getShowtimeId() != null && !request.getShowtimeId().isBlank()) {
            showtime = showtimeService.findById(UUID.fromString(request.getShowtimeId()));
        }

        List<ShowtimeSeat> seats = new ArrayList<>();
        if (showtime != null && request.getShowtimeSeatIds() != null && !request.getShowtimeSeatIds().isEmpty()) {
            List<UUID> seatIds = request.getShowtimeSeatIds().stream().map(UUID::fromString).toList();
            seats = showtimeSeatRepository.findByShowtimeAndIds(showtime.getId(), seatIds);
        }

        // Nếu là Online hoặc chưa lấy được cinema từ Staff, lấy từ Showtime
        if (cinema == null && showtime != null) {
            cinema = showtime.getScreen().getCinema();
        }

        // ── 4. Kiểm tra phương thức thanh toán ─────────────────────────────
        PaymentMethod method = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.VNPAY;
        if (method == PaymentMethod.CASH && processedBy == null) {
            throw new ForbiddenException("Chỉ nhân viên mới có quyền nhận tiền mặt");
        }

        // ── 5. Lock ghế (Nếu có đặt ghế) ────────────────────────────────────
        int pendingMins = systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10);
        LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(pendingMins);

        if (!seats.isEmpty()) {
            long minutesToStart = Duration.between(LocalDateTime.now(), showtime.getStartTime()).toMinutes();
            if (minutesToStart <= 15 && minutesToStart >= -10) {
                pendingMins = systemConfigService.getIntConfig("LATE_SEAT_HOLD_TIME", 3);
            }
            lockUntil = LocalDateTime.now().plusMinutes(pendingMins);

            String tempBookingRef = UUID.randomUUID().toString();
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
                seatLockService.releaseSeats(lockedSoFar);
                throw e;
            }
        }

        // ── 6. Tạo Booking ───────────────────────────────────────────────
        Voucher voucher = quote.getVoucherCode() != null
                ? voucherService.validateForOrder(userId, quote.getVoucherCode(), quote.getSubtotal())
                : null;

        boolean isZeroAmount = quote.getTotalAmount().compareTo(BigDecimal.ZERO) == 0;
        BookingStatus initialStatus = (method == PaymentMethod.CASH || isZeroAmount) ? BookingStatus.PAID : BookingStatus.PENDING;

        Booking booking = Booking.builder()
                .user(user)
                .showtime(showtime)
                .cinema(cinema)
                .bookingCode(generateBookingCode())
                .voucher(voucher)
                .discountAmount(quote.getDiscountAmount())
                .promotionDiscountAmount(quote.getPromotionDiscountAmount())
                .appliedPromotionName(quote.getAppliedPromotionName())
                .totalAmount(quote.getTotalAmount())
                .status(initialStatus)
                .paymentMethod(method)
                .processedBy(processedBy)
                .expiresAt(lockUntil)
                .earnedExp(quote.getTotalAmount().divide(new BigDecimal(1000)).longValue())
                .expAdded(false)
                .build();

        booking = bookingRepository.save(booking);

        // ── Auto-claim và chuyển UserVoucher sang PENDING ─────────────────
        if (voucher != null) {
            UserVoucher userVoucher = userVoucherRepository
                    .findByUserIdAndVoucherId(user.getId(), voucher.getId())
                    .orElseGet(() -> UserVoucher.builder()
                            .user(user)
                            .voucher(voucher)
                            .status(UserVoucherStatus.AVAILABLE)
                            .build());

            if (userVoucher.getStatus() == UserVoucherStatus.USED) {
                throw new BadRequestException("Bạn đã sử dụng mã giảm giá này trước đó");
            }
            userVoucher.setStatus(UserVoucherStatus.PENDING);
            userVoucherRepository.save(userVoucher);
        }

        // Nếu là CASH hoặc giá = 0, tạo QR sau khi đã có ID và cập nhật lại
        if (initialStatus == BookingStatus.PAID) {
            booking.setQrCode(qrCodeService.generateQrContent(booking));
            booking.setExpAdded(true); // Đồng bộ điểm luôn
            booking = bookingRepository.save(booking);
        }

        // Update Redis lock với bookingId thật
        if (!seats.isEmpty()) {
            for (ShowtimeSeat ss : seats) {
                seatLockService.lockSeat(ss.getId().toString(), booking.getId().toString(),
                        Duration.ofMinutes(pendingMins));
            }
        }

        // ── 5. Tạo BookingItem + Ticket ───────────────────────────────────
        for (BookingResponse.SeatItem sItem : quote.getSeats()) {
            ShowtimeSeat ss = showtimeSeatRepository.findById(UUID.fromString(sItem.getShowtimeSeatId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Ghế suất chiếu", sItem.getShowtimeSeatId()));

            BookingItem item = bookingItemRepository.save(BookingItem.builder()
                    .booking(booking)
                    .showtimeSeat(ss)
                    .seatPrice(sItem.getPrice())
                    .build());

            ticketRepository.save(Ticket.builder()
                    .booking(booking)
                    .bookingItem(item)
                    .isUsed(false)
                    .build());
        }

        // ── 6. Tạo BookingCombo ───────────────────────────────────────────
        if (quote.getCombos() != null) {
            for (BookingResponse.ComboItem cItem : quote.getCombos()) {
                Combo combo = comboRepository.findById(UUID.fromString(cItem.getComboId()))
                        .orElseThrow(() -> new ResourceNotFoundException("Combo", cItem.getComboId()));
                bookingComboRepository.save(BookingCombo.builder()
                        .booking(booking)
                        .combo(combo)
                        .quantity(cItem.getQuantity())
                        .unitPrice(cItem.getUnitPrice())
                        .build());
            }
        }

        return getDetail(userId, booking.getId());
    }

    // ── Lịch sử đặt vé ───────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse.Summary> getMyBookings(UUID userId, Pageable pageable) {
        Page<Booking> page = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<UUID> bookingIds = page.getContent().stream().map(Booking::getId).toList();

        if (bookingIds.isEmpty()) {
            return PageResponse.of(new PageImpl<>(Collections.emptyList(), pageable, 0));
        }

        List<BookingItem> allItems = bookingItemRepository.findByBookingIdInWithSeat(bookingIds);
        Map<UUID, List<BookingItem>> itemsMap = allItems.stream()
                .collect(Collectors.groupingBy(item -> item.getBooking().getId()));

        List<BookingResponse.Summary> summaries = page.getContent().stream()
                .map(booking -> {
                    BookingResponse.Summary summary = bookingMapper.toSummary(booking);
                    List<BookingItem> items = itemsMap.getOrDefault(booking.getId(), java.util.Collections.emptyList());
                    List<String> seats = items.stream()
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
        User currentUser = userService.findById(userId);
        Booking booking = findById(bookingId);

        boolean isStaffOrAdmin = currentUser.getRole() == UserRole.STAFF || currentUser.getRole() == UserRole.ADMIN;

        if (!isStaffOrAdmin && !booking.getUser().getId().equals(userId)) {
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

        // Tăng usedCount voucher và chuyển trạng thái UserVoucher: PENDING → USED
        if (booking.getVoucher() != null) {
            voucherService.incrementUsedCount(booking.getVoucher().getId());
            userVoucherRepository.findByUserIdAndVoucherId(booking.getUser().getId(), booking.getVoucher().getId())
                    .ifPresent(uv -> {
                        uv.setStatus(UserVoucherStatus.USED);
                        userVoucherRepository.save(uv);
                    });
        }

        // Send confirmation email ONLY if it's an online order or a specific customer
        // was selected
        if (shouldSendEmail(booking)) {
            emailService.sendBookingConfirmationEmail(booking);
        }
    }

    private boolean shouldSendEmail(Booking booking) {
        if (booking.getUser() == null)
            return false;

        // Nếu không có người xử lý (processedBy == null) -> Đơn hàng Online -> Gửi mail
        if (booking.getProcessedBy() == null)
            return true;

        // Nếu có người xử lý (Staff/Admin):
        // Chỉ gửi mail nếu Người nhận vé KHÁC với Người xử lý (tức là Staff chọn khách
        // hàng thành viên)
        return !booking.getUser().getId().equals(booking.getProcessedBy().getId());
    }

    @Override
    public void cancelBooking(User actionUser, UUID bookingId) {
        Booking booking = findById(bookingId);
        boolean isStaffOrAdmin = actionUser.getRole() == UserRole.STAFF || actionUser.getRole() == UserRole.ADMIN;

        // 1. Quyền hạn: Customer chỉ hủy vé của mình
        if (!isStaffOrAdmin && !booking.getUser().getId().equals(actionUser.getId())) {
            throw new BadRequestException("Bạn không có quyền huỷ đơn đặt vé này");
        }
        if (booking.getStatus() != BookingStatus.PAID) {
            throw new BadRequestException("Chỉ có thể huỷ đơn đặt vé đã thanh toán thành công");
        }

        // 2. Kiểm tra thời gian: Customer phải hủy trước X giờ, Staff/Admin bypass
        LocalDateTime showtimeStartTime = booking.getShowtime().getStartTime();
        int minHoursBefore = systemConfigService.getIntConfig("CANCEL_MIN_HOURS_BEFORE", 2);
        if (!isStaffOrAdmin && LocalDateTime.now().isAfter(showtimeStartTime.minusHours(minHoursBefore))) {
            throw new BadRequestException("Chỉ được huỷ vé trước giờ chiếu ít nhất " + minHoursBefore + " tiếng");
        }

        // 3. Atomic Update: Chống double-click / race condition
        int updatedRows = bookingRepository.cancelPaidBooking(bookingId);
        if (updatedRows == 0) {
            throw new BadRequestException("Đơn vé đã được hủy hoặc trạng thái không hợp lệ");
        }

        // Refresh entity sau atomic update
        booking = findById(bookingId);

        // 4. Nhả ghế
        releaseSeats(bookingId);

        // 4b. Rollback UserVoucher: USED → AVAILABLE (hoàn lại quyền dùng mã khi hủy vé)
        if (booking.getVoucher() != null) {
            userVoucherRepository.findByUserIdAndVoucherId(booking.getUser().getId(), booking.getVoucher().getId())
                    .ifPresent(uv -> {
                        uv.setStatus(UserVoucherStatus.AVAILABLE);
                        userVoucherRepository.save(uv);
                    });
        }

        // 5. Thu hồi EXP
        User bookingOwner = booking.getUser();
        if (Boolean.TRUE.equals(booking.getExpAdded())) {
            long revokeExp = booking.getEarnedExp() != null ? booking.getEarnedExp() : 0L;
            if (revokeExp > 0) {
                long currentExp = bookingOwner.getAvailableExp() != null ? bookingOwner.getAvailableExp() : 0L;
                bookingOwner.setAvailableExp(Math.max(0, currentExp - revokeExp));
                updateMembershipTier(bookingOwner);
                userExpHistoryRepository.save(UserExpHistory.builder()
                        .user(bookingOwner).amount(-revokeExp)
                        .reason("HUY_VE").referenceId(booking.getBookingCode()).build());
            }
            booking.setExpAdded(false);
            bookingRepository.save(booking);
        }

        // 6. Hoàn CP: Staff/Admin = 100%, Customer = SystemConfig %
        int refundPercent = isStaffOrAdmin ? 100
                : systemConfigService.getIntConfig("REFUND_PERCENT_CINEPOINT", 100);
        long rewardPointsToAdd = (booking.getTotalAmount().longValue() * refundPercent / 100) / 1000;
        long currentPoints = bookingOwner.getRewardPoints() != null ? bookingOwner.getRewardPoints() : 0L;
        bookingOwner.setRewardPoints(currentPoints + rewardPointsToAdd);
        userService.save(bookingOwner);

        // 7. Audit Log (Transaction)
        String description = isStaffOrAdmin
                ? "Nhân viên " + actionUser.getFullName() + " (ID: " + actionUser.getId() + ") hủy vé "
                        + booking.getBookingCode() + ". Hoàn " + rewardPointsToAdd + " CP (100%)"
                : "Khách hàng tự hủy vé " + booking.getBookingCode() + ". Hoàn " + rewardPointsToAdd + " CP ("
                        + refundPercent + "%)";

        transactionRepository.save(Transaction.builder()
                .user(bookingOwner)
                .amount(booking.getTotalAmount())
                .type(TransactionType.REFUND)
                .status(TransactionStatus.SUCCESS)
                .referenceId(booking.getBookingCode())
                .description(description)
                .build());

        // 8. Gửi email thông báo
        if (shouldSendEmail(booking)) {
            emailService.sendCancellationEmail(booking, description);
        }
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
                .showtimeSeatId(ss.getId() != null ? ss.getId().toString() : null)
                .rowLabel(ss.getSeat().getRowLabel())
                .colNumber(ss.getSeat().getColNumber())
                .seatType(ss.getSeat().getSeatType().name())
                .price(ss.getPrice())
                .build()).toList());

        response.setCombos(combos.stream().map(c -> BookingResponse.ComboItem.builder()
                .comboId(c.combo.getId() != null ? c.combo.getId().toString() : null)
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
