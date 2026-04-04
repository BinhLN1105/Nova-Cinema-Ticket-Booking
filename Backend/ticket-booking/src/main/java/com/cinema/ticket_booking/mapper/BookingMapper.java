package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.response.BookingResponse;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.BookingCombo;
import com.cinema.ticket_booking.model.BookingItem;

import java.math.BigDecimal;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = { BigDecimal.class })
public interface BookingMapper {

    @Mapping(target = "id", expression = "java(booking.getId() != null ? booking.getId().toString() : null)")
    @Mapping(target = "showtimeId", expression = "java(booking.getShowtime() != null && booking.getShowtime().getId() != null ? booking.getShowtime().getId().toString() : null)")
    @Mapping(target = "movieId", expression = "java(getMovieIdFromBooking(booking))")
    @Mapping(target = "movieTitle", source = "showtime.movie.title")
    @Mapping(target = "moviePosterUrl", source = "showtime.movie.posterUrl")
    @Mapping(target = "startTime", source = "showtime.startTime")
    @Mapping(target = "cinemaName", source = "cinema.name")
    @Mapping(target = "cinemaAddress", source = "cinema.address")
    @Mapping(target = "screenName", source = "showtime.screen.name")
    @Mapping(target = "voucherCode", source = "voucher.code")
    @Mapping(target = "seats", ignore = true) // service tự map
    @Mapping(target = "combos", ignore = true) // service tự map
    @Mapping(target = "subtotal", ignore = true) // service tính
    @Mapping(target = "screenType", ignore = true) // service tính từ getScreenTypeName()
    @Mapping(target = "totalOriginalAmount", ignore = true)
    @Mapping(target = "promotionDiscountAmount", ignore = true)
    @Mapping(target = "appliedPromotionName", ignore = true)
    @Mapping(target = "warningMessage", ignore = true)
    BookingResponse toResponse(Booking booking);

    @Mapping(target = "id", expression = "java(booking.getId() != null ? booking.getId().toString() : null)")
    @Mapping(target = "movieId", expression = "java(getMovieIdFromBooking(booking))")
    @Mapping(target = "movieTitle", source = "showtime.movie.title")
    @Mapping(target = "moviePosterUrl", source = "showtime.movie.posterUrl")
    @Mapping(target = "startTime", source = "showtime.startTime")
    @Mapping(target = "cinemaName", source = "cinema.name")
    @Mapping(target = "screenName", source = "showtime.screen.name")
    @Mapping(target = "screenType", ignore = true) // service tính từ getScreenTypeName()
    @Mapping(target = "seats", ignore = true) // service tự map chuỗi "A1, A2..."
    BookingResponse.Summary toSummary(Booking booking);

    @Mapping(target = "showtimeSeatId", expression = "java(item.getShowtimeSeat() != null && item.getShowtimeSeat().getId() != null ? item.getShowtimeSeat().getId().toString() : null)")
    @Mapping(target = "rowLabel", source = "showtimeSeat.seat.rowLabel")
    @Mapping(target = "colNumber", source = "showtimeSeat.seat.colNumber")
    @Mapping(target = "seatType", expression = "java(item.getShowtimeSeat().getSeat().getSeatType().name())")
    @Mapping(target = "price", source = "seatPrice")
    BookingResponse.SeatItem toSeatItem(BookingItem item);

    @Mapping(target = "comboId", expression = "java(bc.getCombo() != null && bc.getCombo().getId() != null ? bc.getCombo().getId().toString() : null)")
    @Mapping(target = "comboName", source = "combo.name")
    @Mapping(target = "subtotal", expression = "java(bc.getUnitPrice().multiply(BigDecimal.valueOf(bc.getQuantity())))")
    BookingResponse.ComboItem toComboItem(BookingCombo bc);

    default String getMovieIdFromBooking(Booking booking) {
        if (booking != null && booking.getShowtime() != null && booking.getShowtime().getMovie() != null) {
            return booking.getShowtime().getMovie().getId().toString();
        }
        return null;
    }
}
