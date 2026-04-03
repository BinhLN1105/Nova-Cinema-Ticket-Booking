package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.response.ComboResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface ComboService {

    List<ComboResponse> getAvailable();

    ComboResponse updateImage(UUID id, MultipartFile file) throws IOException;

    ComboResponse updateImageFromUrl(UUID id, String url) throws IOException;
}
