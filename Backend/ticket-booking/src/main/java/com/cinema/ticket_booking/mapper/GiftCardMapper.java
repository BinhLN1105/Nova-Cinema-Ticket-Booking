package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.response.GiftCardResponse;
import com.cinema.ticket_booking.model.GiftCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GiftCardMapper {

    @Mapping(target = "id", expression = "java(giftCard.getId().toString())")
    GiftCardResponse toResponse(GiftCard giftCard);
}
