package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.VoucherRequest;
import com.cinema.ticket_booking.dto.response.VoucherResponse;
import com.cinema.ticket_booking.dto.response.VoucherSyncResponse;
import com.cinema.ticket_booking.model.Voucher;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

public interface VoucherService {

    VoucherResponse create(VoucherRequest request);

    VoucherResponse update(UUID id, VoucherRequest request);

    void toggleActive(UUID id);

    Voucher validateForOrder(String code, BigDecimal orderAmount);

    void incrementUsedCount(UUID voucherId);

    VoucherResponse.Summary getSummaryByCode(String code);

    Voucher findById(UUID id);

    List<VoucherSyncResponse> getActiveVouchersForSync();
}
