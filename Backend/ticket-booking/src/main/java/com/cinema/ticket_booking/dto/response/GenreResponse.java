package com.cinema.ticket_booking.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenreResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String name;
    private String slug;
}
