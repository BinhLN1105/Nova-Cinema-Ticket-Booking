package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.request.CinemaRequest;
import com.cinema.ticket_booking.dto.response.CinemaResponse;
import com.cinema.ticket_booking.model.Cinema;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CinemaMapper {

    @Mapping(target = "id", expression = "java(cinema.getId().toString())")
    CinemaResponse toResponse(Cinema cinema);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Cinema toEntity(CinemaRequest request);

    // Cập nhật các field không null từ request vào entity đã có
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(CinemaRequest request, @MappingTarget Cinema cinema);
}
