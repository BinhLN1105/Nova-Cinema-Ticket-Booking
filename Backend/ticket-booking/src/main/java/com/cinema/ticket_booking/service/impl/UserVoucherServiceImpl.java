package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.VoucherResponse;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.VoucherMapper;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.model.UserVoucher;
import com.cinema.ticket_booking.model.Voucher;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.repository.UserVoucherRepository;
import com.cinema.ticket_booking.repository.VoucherRepository;
import com.cinema.ticket_booking.service.UserVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public void saveVoucher(UUID userId, UUID voucherId) {
        if (userVoucherRepository.existsByUserIdAndVoucherId(userId, voucherId)) {
            throw new BadRequestException("Bạn đã lưu mã ưu đãi này rồi");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Mã ưu đãi", voucherId));

        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            throw new BadRequestException("Mã ưu đãi này không còn hoạt động");
        }

        UserVoucher userVoucher = UserVoucher.builder()
                .user(user)
                .voucher(voucher)
                .isUsed(false)
                .build();
        
        userVoucherRepository.save(userVoucher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getMyVouchers(UUID userId) {
        List<UserVoucher> uvList = userVoucherRepository.findUnusedByUserIdWithVoucher(userId);
        return uvList.stream()
                .map(uv -> voucherMapper.toResponse(uv.getVoucher()))
                .collect(Collectors.toList());
    }
}
