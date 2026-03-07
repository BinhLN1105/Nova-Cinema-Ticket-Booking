package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.response.ReviewResponse;
import com.cinema.ticket_booking.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "id", expression = "java(review.getId().toString())")
    @Mapping(target = "movieId", expression = "java(review.getMovie().getId().toString())")
    @Mapping(target = "userId", expression = "java(review.getUser().getId().toString())")
    @Mapping(target = "userFullName", source = "user.fullName")
    @Mapping(target = "userAvatarUrl", source = "user.avatarUrl")
    ReviewResponse toResponse(Review review);
}
