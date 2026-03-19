package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.VoucherRequest;
import com.cinema.ticket_booking.dto.response.VoucherResponse;
import com.cinema.ticket_booking.dto.response.VoucherSyncResponse;
import com.cinema.ticket_booking.model.Voucher;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ConflictException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.VoucherMapper;
import com.cinema.ticket_booking.repository.VoucherRepository;
import com.cinema.ticket_booking.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherMapper voucherMapper;

    // ── ADMIN ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
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
    public Voucher validateForOrder(String code, BigDecimal orderAmount) {
        return voucherRepository.findValidVoucher(code, LocalDateTime.now(), orderAmount)
                .orElseThrow(() -> new BadRequestException(
                        "Mã giảm giá không hợp lệ, đã hết hạn, hết lượt dùng hoặc đơn hàng chưa đủ điều kiện"));
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
        return voucherMapper.toSummary(v);
    }

    @Override
    @Transactional(readOnly = true)
    public Voucher findById(UUID id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", id));
    }
}
