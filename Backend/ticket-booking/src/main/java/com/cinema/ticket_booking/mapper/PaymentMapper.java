package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.response.PaymentResponse;
import com.cinema.ticket_booking.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "id", expression = "java(payment.getId().toString())")
    @Mapping(target = "bookingId", expression = "java(payment.getBooking().getId().toString())")
    @Mapping(target = "bookingCode", source = "booking.bookingCode")
    @Mapping(target = "paymentUrl", ignore = true) // service gắn URL VNPay
    PaymentResponse toResponse(Payment payment);
}
