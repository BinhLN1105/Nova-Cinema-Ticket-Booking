package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.VoucherRequest;
import com.cinema.ticket_booking.dto.response.VoucherResponse;
import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.enums.UserVoucherStatus;
import com.cinema.ticket_booking.model.Voucher;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ConflictException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.VoucherMapper;
import com.cinema.ticket_booking.repository.VoucherRepository;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.UserVoucherRepository;
import com.cinema.ticket_booking.dto.response.VoucherSyncResponse;
import com.cinema.ticket_booking.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cinema.ticket_booking.dto.response.PageResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final BookingRepository bookingRepository;
    private final VoucherMapper voucherMapper;
    private final UserVoucherRepository userVoucherRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VoucherResponse> getAll(Pageable pageable) {
        Page<Voucher> page = voucherRepository.findAll(pageable);
        return PageResponse.of(page.map(voucherMapper::toResponse));
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "promotions", key = "'active_vouchers'")
    public List<VoucherSyncResponse> getActiveVouchersForSync() {
        return voucherRepository.findAll().stream()
                .filter(v -> v.getIsActive() && java.time.LocalDateTime.now().isBefore(v.getValidTo()))
                .map(v -> VoucherSyncResponse.builder()
                        .id(v.getId())
                        .code(v.getCode())
                        .description(v.getDescription())
                        .discountType(v.getDiscountType())
                        .discountValue(v.getDiscountValue())
                        .minOrder(v.getMinOrder())
                        .validTo(v.getValidTo())
                        .build())
                .toList();
    }

    @Override
    public VoucherResponse create(VoucherRequest request) {
        if (voucherRepository.findByCodeIgnoreCase(request.getCode()).isPresent()) {
            throw new ConflictException("Mã voucher '" + request.getCode() + "' đã tồn tại");
        }
        return voucherMapper.toResponse(voucherRepository.save(voucherMapper.toEntity(request)));
    }

    @Override
    public VoucherResponse update(UUID id, VoucherRequest request) {
        Voucher voucher = findById(id);
        voucherMapper.updateEntity(request, voucher);
        return voucherMapper.toResponse(voucherRepository.save(voucher));
    }

    @Override
    public void toggleActive(UUID id) {
        Voucher voucher = findById(id);
        voucher.setIsActive(!voucher.getIsActive());
        voucherRepository.save(voucher);
    }

    // ── User: validate và tính giảm giá ──────────────────────────────────

    /**
     * Validate voucher và trả về entity nếu hợp lệ.
     * Gọi từ BookingService trước khi tạo booking.
     */
    @Override
    @Transactional(readOnly = true)
    public Voucher validateForOrder(UUID userId, String code, BigDecimal orderAmount) {
        Voucher voucher = voucherRepository.findValidVoucher(code, LocalDateTime.now(), orderAmount)
                .orElseThrow(() -> new BadRequestException(
                        "Mã giảm giá không hợp lệ, đã hết hạn, hết lượt dùng hoặc đơn hàng chưa đủ điều kiện"));

        // Kiểm tra xem user đã dùng hoặc đang dùng mã này ở đơn hàng khác chưa
        userVoucherRepository.findByUserIdAndVoucherId(userId, voucher.getId())
                .ifPresent(uv -> {
                    if (uv.getStatus() == UserVoucherStatus.USED) {
                        throw new BadRequestException("Bạn đã sử dụng mã giảm giá này rồi");
                    }
                    if (uv.getStatus() == UserVoucherStatus.PENDING) {
                        // ── Senior Logic: Lazy Release ──────────────────────
                        // Kiểm tra xem có Booking PENDING nào THỰC SỰ còn hạn (expiresAt > now) đang
                        // giữ mã này không
                        boolean hasActiveBooking = bookingRepository
                                .existsByUserIdAndVoucherIdAndStatusAndExpiresAtAfter(
                                        userId, voucher.getId(), BookingStatus.PENDING, LocalDateTime.now());

                        if (hasActiveBooking) {
                            throw new BadRequestException(
                                    "Mã giảm giá này đang được áp dụng trong một đơn hàng đang chờ thanh toán");
                        } else {
                            log.info(
                                    "[LazyRelease] Voucher '{}' của user '{}' đã hết hạn giữ chỗ, cho phép đặt lại đơn mới.",
                                    code, userId);
                        }
                    }
                });

        return voucher;
    }

    /**
     * Tăng usedCount sau khi booking PAID thành công.
     * Dùng @Transactional riêng để đảm bảo atomic khi nhiều user dùng cùng mã.
     */
    @Override
    public void incrementUsedCount(UUID voucherId) {
        Voucher voucher = findById(voucherId);
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new BadRequestException("Mã giảm giá đã hết lượt sử dụng");
        }
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepository.save(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse.Summary getSummaryByCode(String code) {
        Voucher v = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher không tồn tại"));

        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(v.getIsActive())) {
            throw new BadRequestException("Mã giảm giá này đã bị vô hiệu hóa");
        }
        if (v.getValidFrom() != null && v.getValidFrom().isAfter(now)) {
            throw new BadRequestException("Mã giảm giá này chưa đến ngày hiệu lực");
        }
        if (v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit()) {
            throw new BadRequestException("Mã giảm giá này đã hết lượt sử dụng");
        }

        return voucherMapper.toSummary(v);
    }

    @Override
    public void delete(UUID id) {
        if (!voucherRepository.existsById(id)) {
            throw new ResourceNotFoundException("Voucher", id);
        }
        voucherRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Voucher findById(UUID id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", id));
    }
}
