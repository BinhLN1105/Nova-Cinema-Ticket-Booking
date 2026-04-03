package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.CinemaRequest;
import com.cinema.ticket_booking.dto.response.CinemaResponse;
import com.cinema.ticket_booking.dto.response.CinemaSyncResponse;
import com.cinema.ticket_booking.model.Cinema;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface CinemaService {

    List<CinemaResponse> getAll(String city);

    // Lấy tất cả rạp (Dành cho Admin)
    List<CinemaResponse> getAllForAdmin();


    CinemaResponse getById(UUID id);

    CinemaResponse create(CinemaRequest request);

    CinemaResponse update(UUID id, CinemaRequest request);

     CinemaResponse updateImage(UUID id, MultipartFile file) throws IOException;

    CinemaResponse updateImageFromUrl(UUID id, String url) throws IOException;

    CinemaResponse toggleStatus(UUID id);

    void delete(UUID id);


    Cinema findById(UUID id); // Dùng cho nội bộ hoặc các service khác gọi sang

    List<CinemaSyncResponse> getAllForSync();
}
