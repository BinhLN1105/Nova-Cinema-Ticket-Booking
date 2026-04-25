package com.cinema.ticket_booking.dto.request;

import com.cinema.ticket_booking.enums.MovieStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class MovieRequest {

    @NotBlank(message = "Tên phim không được để trống")
    @Size(max = 200)
    private String title;

    @Size(max = 200)
    private String originalTitle;

    private String description;

    @Min(value = 1, message = "Thời lượng phải lớn hơn 0 phút")
    @NotNull(message = "Thời lượng không được để trống")
    private Integer duration;

    @NotNull(message = "Ngày khởi chiếu không được để trống")
    private LocalDate releaseDate;

    private LocalDate endDate;

    @Size(max = 150)
    private String director;

    private String cast;

    private String language;

    // P | C13 | C16 | C18
    @Size(max = 10)
    private String rated;

    private String posterUrl;
    private String backdropUrl;
    private String trailerUrl;

    @NotNull(message = "Trạng thái không được để trống")
    private MovieStatus status;

    // Danh sách genre id
    private List<Integer> genreIds;
}
