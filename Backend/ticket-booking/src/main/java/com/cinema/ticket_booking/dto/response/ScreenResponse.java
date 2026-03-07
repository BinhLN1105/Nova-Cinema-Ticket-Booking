package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.ScreenType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScreenResponse {

    private String id;
    private String cinemaId;
    private String cinemaName;
    private String name;
    private ScreenType screenType;
    private Integer totalRows;
    private Integer totalCols;
    private Boolean isActive;
}
