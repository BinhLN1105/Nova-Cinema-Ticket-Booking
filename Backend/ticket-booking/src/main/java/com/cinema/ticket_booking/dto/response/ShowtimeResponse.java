package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.ShowtimeStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ShowtimeResponse {

    private String id;
    private ShowtimeStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;

    // Thông tin phim rút gọn
    private String movieId;
    private String movieTitle;
    private String moviePosterUrl;
    private Integer movieDuration;
    private String movieRated;
    private List<String> movieGenres;

    // Thông tin phòng + rạp
    private String screenId;
    private String screenName;
    private String screenType;
    private String cinemaId;
    private String cinemaName;
    private String cinemaCity;

    // Số ghế còn trống (tính thêm khi cần)
    private Long availableSeats;
}
