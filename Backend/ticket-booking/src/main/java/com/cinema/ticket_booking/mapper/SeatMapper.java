package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.response.SeatMapResponse;
import com.cinema.ticket_booking.model.ShowtimeSeat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SeatMapper {

    @Mapping(target = "showtimeSeatId", expression = "java(ss.getId().toString())")
    @Mapping(target = "seatId", expression = "java(ss.getSeat().getId().toString())")
    @Mapping(target = "rowLabel", source = "seat.rowLabel")
    @Mapping(target = "colNumber", source = "seat.colNumber")
    @Mapping(target = "gridRow", source = "seat.gridRow")
    @Mapping(target = "gridCol", source = "seat.gridCol")
    @Mapping(target = "seatLabel", source = "seat.seatLabel")
    @Mapping(target = "seatType", source = "seat.seatType")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "price", source = "price")
    SeatMapResponse.SeatItem toSeatItem(ShowtimeSeat ss);
}
