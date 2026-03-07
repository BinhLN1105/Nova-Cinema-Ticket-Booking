package com.cinema.ticket_booking.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenreResponse {

    private Integer id;
    private String name;
    private String slug;
}
