package com.cinema.ticket_booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClaimVoucherRequest {
    @NotBlank(message = "Mã voucher không được để trống")
    private String code;
}
