package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.response.UserResponse;
import com.cinema.ticket_booking.model.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", expression = "java(user.getId().toString())")
    UserResponse toResponse(User user);
}
