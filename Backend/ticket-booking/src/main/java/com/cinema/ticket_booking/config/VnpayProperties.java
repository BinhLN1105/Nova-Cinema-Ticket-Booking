package com.cinema.ticket_booking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Bind cấu hình VNPay từ application.yml.
 *
 * vnpay:
 * tmn-code: XXXXXXXX
 * hash-secret: xxxxxxxxxxxxxxxx
 * url: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
 * return-url: https://your-domain.com/api/v1/payments/vnpay/callback
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "vnpay")
public class VnpayProperties {

    @NotBlank
    private String tmnCode;

    @NotBlank
    private String hashSecret;

    @NotBlank
    private String url;

    @NotBlank
    private String returnUrl;
}
