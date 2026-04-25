package com.cinema.ticket_booking.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieSyncResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID id;
    private String title;
    private String description;
    private Integer duration;
    private String rated;
    private String language;
}
