package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.CinemaRequest;
import com.cinema.ticket_booking.dto.response.CinemaResponse;
import com.cinema.ticket_booking.dto.response.CinemaSyncResponse;
import com.cinema.ticket_booking.model.Cinema;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.CinemaMapper;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.CinemaRepository;
import com.cinema.ticket_booking.repository.ScreenRepository;
import com.cinema.ticket_booking.service.CinemaService;
import com.cinema.ticket_booking.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final ScreenRepository screenRepository;
    private final BookingRepository bookingRepository;
    private final CinemaMapper cinemaMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public List<CinemaSyncResponse> getAllForSync() {
        return cinemaRepository.findByIsActiveTrue().stream()
                .map(c -> CinemaSyncResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .address(c.getAddress())
                        .city(c.getCity())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CinemaResponse> getAll(String city) {
        List<Cinema> cinemas = (city != null && !city.isBlank())
                ? cinemaRepository.findByCityAndIsActiveTrue(city)
                : cinemaRepository.findByIsActiveTrue();
        return cinemas.stream().map(cinemaMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CinemaResponse> getAllForAdmin() {
        return cinemaRepository.findAll().stream()
                .map(cinemaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaResponse getById(UUID id) {
        return cinemaMapper.toResponse(findById(id));
    }

    @Override
    public CinemaResponse create(CinemaRequest request) {
        Cinema cinema = cinemaMapper.toEntity(request);
        return cinemaMapper.toResponse(cinemaRepository.save(cinema));
    }

    @Override
    public CinemaResponse update(UUID id, CinemaRequest request) {
        Cinema cinema = findById(id);
        cinemaMapper.updateEntity(request, cinema);
        return cinemaMapper.toResponse(cinemaRepository.save(cinema));
    }

    @Override
    @Transactional
    public CinemaResponse updateImage(UUID id, MultipartFile file) throws IOException {
        Cinema cinema = findById(id);

        String oldUrl = cinema.getImageUrl();
        String newUrl = null;

        try {
            newUrl = cloudinaryService.uploadImage(file, "Theater");

            cinema.setImageUrl(newUrl);
            cinemaRepository.save(cinema);

            if (oldUrl != null && !oldUrl.isEmpty()) {
                String publicId = cloudinaryService.extractPublicId(oldUrl);
                if (publicId != null) cloudinaryService.deleteImageAsync(publicId);
            }
            return cinemaMapper.toResponse(cinema);
        } catch (Exception e) {
            if (newUrl != null) {
                String newPublicId = cloudinaryService.extractPublicId(newUrl);
                if (newPublicId != null) cloudinaryService.deleteImageAsync(newPublicId);
            }
            throw new RuntimeException("Cập nhật ảnh rạp thất bại: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CinemaResponse updateImageFromUrl(UUID id, String url) throws IOException {
        Cinema cinema = findById(id);
        String oldUrl = cinema.getImageUrl();
        String newUrl = null;

        try {
            newUrl = cloudinaryService.uploadImageFromUrl(url, "Theater");
            cinema.setImageUrl(newUrl);
            cinemaRepository.save(cinema);

            if (oldUrl != null && !oldUrl.isEmpty()) {
                String publicId = cloudinaryService.extractPublicId(oldUrl);
                if (publicId != null) cloudinaryService.deleteImageAsync(publicId);
            }
            return cinemaMapper.toResponse(cinema);
        } catch (Exception e) {
            if (newUrl != null) {
                String newPublicId = cloudinaryService.extractPublicId(newUrl);
                if (newPublicId != null) cloudinaryService.deleteImageAsync(newPublicId);
            }
            throw new RuntimeException("Cập nhật ảnh rạp từ URL thất bại: " + e.getMessage());
        }
    }

    @Override
    public CinemaResponse toggleStatus(UUID id) {
        Cinema cinema = findById(id);
        cinema.setIsActive(!cinema.getIsActive());
        return cinemaMapper.toResponse(cinemaRepository.save(cinema));
    }

    @Override
    public void delete(UUID id) {
        Cinema cinema = findById(id);

        // Kiểm tra xem rạp có dữ liệu vận hành không (Phòng chiếu hoặc Booking)
        boolean hasScreens = screenRepository.countByCinemaIdAndIsDeletedFalse(id) > 0;
        boolean hasBookings = bookingRepository.existsByCinemaId(id);

        if (hasScreens || hasBookings) {
            throw new IllegalStateException("Rạp đã có dữ liệu hoạt động (Phòng chiếu/Booking). Vui lòng sử dụng tính năng 'Vô hiệu hóa' thay vì Xóa.");
        }

        cinemaRepository.delete(cinema);
    }

    @Override
    public Cinema findById(UUID id) {
        return cinemaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rạp chiếu", id));
    }
}
