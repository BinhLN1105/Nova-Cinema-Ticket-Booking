package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.response.UserResponse;
import com.cinema.ticket_booking.model.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", expression = "java(user.getId().toString())")
    @Mapping(target = "cinemaId", ignore = true)
    @Mapping(target = "cinemaName", ignore = true)
    @Mapping(target = "currentTierMinPoints", ignore = true)
    @Mapping(target = "nextTierMinPoints", ignore = true)
    UserResponse toResponse(User user);

    @AfterMapping
    default void mapTierPoints(User user, @MappingTarget UserResponse.UserResponseBuilder responseBuilder) {
        long currentExp = user.getAvailableExp() != null ? user.getAvailableExp() : 0;
        long currentMin = 0;
        long nextMin = 500;
        if (currentExp >= 10000) {
            currentMin = 10000;
            nextMin = 10000; // max
        } else if (currentExp >= 3000) {
            currentMin = 3000;
            nextMin = 10000;
        } else if (currentExp >= 500) {
            currentMin = 500;
            nextMin = 3000;
        }
        responseBuilder.currentTierMinPoints(currentMin);
        responseBuilder.nextTierMinPoints(nextMin);
    }
}
