package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.response.NotificationResponse;
import com.cinema.ticket_booking.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "id", expression = "java(n.getId().toString())")
    @Mapping(target = "refId", expression = "java(n.getRefId() != null ? n.getRefId().toString() : null)")
    NotificationResponse toResponse(Notification n);
}
