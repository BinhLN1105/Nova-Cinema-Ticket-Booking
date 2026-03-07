package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.request.ScreenRequest;
import com.cinema.ticket_booking.dto.response.ScreenResponse;
import com.cinema.ticket_booking.model.Screen;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ScreenMapper {

    @Mapping(target = "id", expression = "java(screen.getId().toString())")
    @Mapping(target = "cinemaId", expression = "java(screen.getCinema().getId().toString())")
    @Mapping(target = "cinemaName", source = "cinema.name")
    ScreenResponse toResponse(Screen screen);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cinema", ignore = true) // service inject Cinema entity
    @Mapping(target = "isActive", ignore = true)
    Screen toEntity(ScreenRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cinema", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updateEntity(ScreenRequest request, @MappingTarget Screen screen);
}
