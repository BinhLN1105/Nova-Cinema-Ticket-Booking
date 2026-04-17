package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.VoucherResponse;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.VoucherMapper;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.model.UserVoucher;
import com.cinema.ticket_booking.model.Voucher;
import com.cinema.ticket_booking.enums.UserVoucherStatus;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.repository.UserVoucherRepository;
import com.cinema.ticket_booking.repository.VoucherRepository;
import com.cinema.ticket_booking.service.UserVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserVoucherServiceImpl implements UserVoucherService {

    private final UserVoucherRepository userVoucherRepository;
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final VoucherMapper voucherMapper;

    @Override
    public void claimVoucher(UUID userId, String code) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại: " + code));

        if (userVoucherRepository.existsByUserIdAndVoucherId(userId, voucher.getId())) {
            throw new BadRequestException("Bạn đã sở hữu mã giảm giá này rồi");
        }

        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            throw new BadRequestException("Mã giảm giá này đã bị vô hiệu hóa");
        }

        LocalDateTime now = LocalDateTime.now();
        if (voucher.getValidFrom() != null && voucher.getValidFrom().isAfter(now)) {
            throw new BadRequestException("Mã giảm giá này chưa đến ngày hiệu lực");
        }
        if (voucher.getValidTo() != null && voucher.getValidTo().isBefore(now)) {
            throw new BadRequestException("Mã giảm giá này đã hết hạn");
        }

        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new BadRequestException("Mã giảm giá này đã hết lượt sử dụng");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        UserVoucher userVoucher = UserVoucher.builder()
                .user(user)
                .voucher(voucher)
                .status(UserVoucherStatus.AVAILABLE)
                .savedAt(now)
                .build();

        userVoucherRepository.save(userVoucher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getMyVouchers(UUID userId) {
        List<UserVoucher> uvList = userVoucherRepository.findAllByUserIdWithVoucher(userId);
        return uvList.stream()
                .map(uv -> {
                    VoucherResponse resp = voucherMapper.toResponse(uv.getVoucher());
                    resp.setStatus(uv.getStatus()); // Gắn trạng thái riêng của user
                    return resp;
                })
                .collect(Collectors.toList());
    }
}
