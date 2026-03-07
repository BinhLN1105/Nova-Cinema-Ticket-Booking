package com.cinema.ticket_booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotBlank(message = "Mã booking không được để trống")
    private String bookingId;

    // URL redirect sau khi thanh toán (Android deep link)
    // VD: "cinema://payment/result"
    private String returnUrl;
}
