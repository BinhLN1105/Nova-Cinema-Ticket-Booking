package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.response.GenreResponse;
import com.cinema.ticket_booking.model.Genre;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GenreMapper {

    GenreResponse toResponse(Genre genre);
}
