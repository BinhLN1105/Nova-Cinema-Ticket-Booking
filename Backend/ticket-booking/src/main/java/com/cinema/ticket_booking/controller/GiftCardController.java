package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.GiftCardRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.GiftCardResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.PaymentResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.GiftCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/gift-cards")
@RequiredArgsConstructor
public class GiftCardController {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final GiftCardService giftCardService;

    // POST /api/v1/gift-cards/buy
    @PostMapping("/buy")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> buyGiftCard(
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser,
            @Valid @RequestBody GiftCardRequest.Buy request) {
        return ResponseEntity.ok(ApiResponse.success(
                giftCardService.buyGiftCard(currentUser.getId(), request), "Tạo link thanh toán thành công"));
    }

    // GET /api/v1/gift-cards/vnpay-return
    @GetMapping("/vnpay-return")
    public RedirectView vnpayReturn(@RequestParam Map<String, String> params) {
        try {
            giftCardService.handleVnpayCallback(params);
        } catch (Exception e) {
            log.error("[GiftCard] Lỗi khi xử lý callback VNPay: {}", e.getMessage(), e);
        }

        String responseCode = params.get("vnp_ResponseCode");
        String returnUrl = params.get("vnp_ReturnUrl");

        // Chống Open Redirect: Chỉ cho phép redirect về frontendUrl đã cấu hình
        String baseRedirectUrl = frontendUrl;
        if (returnUrl != null && !returnUrl.isBlank()) {
            try {
                URI uri = URI.create(returnUrl.trim());
                URI baseUri = URI.create(frontendUrl.trim());
                if (uri.getHost() != null && uri.getHost().equalsIgnoreCase(baseUri.getHost())) {
                    baseRedirectUrl = uri.getScheme() + "://" + uri.getAuthority();
                }
            } catch (Exception e) {
                // Fallback về frontendUrl mặc định
            }
        }

        String safeResponseCode = (responseCode != null && responseCode.matches("^[a-zA-Z0-9]+$"))
                ? responseCode : "99";
        String redirectUrl = baseRedirectUrl + "/gift-cards/result?vnp_ResponseCode=" + safeResponseCode;
        return new RedirectView(redirectUrl);
    }

    // POST /api/v1/gift-cards/redeem
    @PostMapping("/redeem")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<GiftCardResponse>> redeemGiftCard(
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser,
            @Valid @RequestBody GiftCardRequest.Redeem request) {
        return ResponseEntity.ok(ApiResponse.success(
                giftCardService.redeemGiftCard(currentUser.getId(), request),
                "Đổi thẻ quà tặng thành điểm CinePoint thành công"));
    }

    // GET /api/v1/gift-cards/me — thẻ user đã mua
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<GiftCardResponse>>> getMyGiftCards(
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity
                .ok(ApiResponse.success(giftCardService.getMyBoughtCards(currentUser.getId(), pageable)));
    }
}
