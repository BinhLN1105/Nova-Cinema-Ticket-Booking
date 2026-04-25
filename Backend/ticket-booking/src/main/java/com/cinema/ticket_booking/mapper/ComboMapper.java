package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.response.ComboResponse;
import com.cinema.ticket_booking.model.Combo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ComboMapper {

    @Mapping(target = "id", expression = "java(combo.getId().toString())")
    ComboResponse toResponse(Combo combo);
}
