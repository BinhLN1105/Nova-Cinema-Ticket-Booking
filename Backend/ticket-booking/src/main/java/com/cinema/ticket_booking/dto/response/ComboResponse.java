package com.cinema.ticket_booking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ComboResponse {

    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Boolean isAvailable;
}
