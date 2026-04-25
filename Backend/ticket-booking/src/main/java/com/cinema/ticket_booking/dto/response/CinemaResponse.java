package com.cinema.ticket_booking.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CinemaResponse {

    private String id;
    private String name;
    private String address;
    private String city;
    private String phone;
    private String imageUrl;
    private Boolean isActive;
}
