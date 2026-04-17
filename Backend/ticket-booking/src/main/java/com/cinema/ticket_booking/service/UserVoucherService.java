package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.response.VoucherResponse;

import java.util.List;
import java.util.UUID;

public interface UserVoucherService {
    void claimVoucher(UUID userId, String code);
    List<VoucherResponse> getMyVouchers(UUID userId);
}
