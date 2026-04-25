package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.GiftCardRequest;
import com.cinema.ticket_booking.dto.response.GiftCardResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.PaymentResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface GiftCardService {
    
    // Khởi tạo giao dịch mua thẻ quà tặng qua VNPay
    PaymentResponse buyGiftCard(UUID userId, GiftCardRequest.Buy request);

    // Xử lý callback VNPay -> Tạo thẻ quà tặng thực tế
    void handleVnpayCallback(Map<String, String> params);

    // Người dùng dùng mã thẻ để nạp vào CinePoint
    GiftCardResponse redeemGiftCard(UUID userId, GiftCardRequest.Redeem request);

    // Lấy danh sách thẻ đã mua của user
    PageResponse<GiftCardResponse> getMyBoughtCards(UUID userId, Pageable pageable);

    // Lấy danh sách thẻ đã nạp của user
    PageResponse<GiftCardResponse> getMyRedeemedCards(UUID userId, Pageable pageable);
}
