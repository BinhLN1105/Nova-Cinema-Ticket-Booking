package com.cinema.ticket_booking.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "Họ tên không quá 100 ký tự")
    private String fullName;

    @Size(max = 15, message = "Số điện thoại không quá 15 ký tự")
    private String phone;

    private String avatarUrl;
}
