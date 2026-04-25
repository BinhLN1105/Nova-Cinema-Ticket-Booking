package com.cinema.ticket_booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CinemaRequest {

    @NotBlank(message = "Tên rạp không được để trống")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @NotBlank(message = "Thành phố không được để trống")
    @Size(max = 100)
    private String city;

    @Size(max = 15)
    private String phone;

    private String imageUrl;
}
