package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.response.NotificationResponse;
import com.cinema.ticket_booking.model.Notification;
import com.cinema.ticket_booking.model.GlobalNotification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "id", expression = "java(n.getId().toString())")
    @Mapping(target = "refId", expression = "java(n.getRefId() != null ? n.getRefId().toString() : null)")
    NotificationResponse toResponse(Notification n);

    @Mapping(target = "id", expression = "java(gn.getId().toString())")
    @Mapping(target = "refId", expression = "java(gn.getTargetId() != null ? gn.getTargetId().toString() : null)")
    @Mapping(target = "isRead", constant = "false")
    NotificationResponse toResponse(GlobalNotification gn);
}
