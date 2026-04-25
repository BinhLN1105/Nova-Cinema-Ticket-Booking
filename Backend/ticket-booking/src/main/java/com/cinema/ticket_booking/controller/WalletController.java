package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.TopUpRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.PaymentResponse;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @PostMapping("/topup")
    public ResponseEntity<ApiResponse<PaymentResponse>> createTopUp(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody TopUpRequest request,
            HttpServletRequest httpRequest) {

        // baseUrl for return url
        String scheme = httpRequest.getScheme();
        String serverName = httpRequest.getServerName();
        int serverPort = httpRequest.getServerPort();
        String returnUrlBase = scheme + "://" + serverName + ":" + serverPort;

        PaymentResponse data = walletService.createTopUpUrl(currentUser.getId(), request.getAmount(), returnUrlBase);
        return ResponseEntity.ok(ApiResponse.success(data, "Vui lòng hoàn tất thanh toán nạp tiền"));
    }

    @GetMapping("/vnpay-return")
    public void vnpayCallback(@RequestParam Map<String, String> params, HttpServletResponse response)
            throws IOException {
        String responseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef");
        String status = "00".equals(responseCode) ? "success" : "failed";

        try {
            walletService.handleVnpayCallback(params);
        } catch (Exception e) {
        }

        String redirectUrl = frontendUrl + "/profile?topup=" + status + "&vnp_ResponseCode=" + responseCode;

        response.sendRedirect(redirectUrl);
    }
}
