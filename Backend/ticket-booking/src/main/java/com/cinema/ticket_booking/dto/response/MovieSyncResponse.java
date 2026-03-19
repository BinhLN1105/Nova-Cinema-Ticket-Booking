package com.cinema.ticket_booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieSyncResponse {
    private UUID id;
    private String title;
    private String description;
    private Integer duration;
    private String rated;
    private String language;
}
