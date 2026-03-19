package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.CinemaRequest;
import com.cinema.ticket_booking.dto.response.CinemaResponse;
import com.cinema.ticket_booking.dto.response.CinemaSyncResponse;
import com.cinema.ticket_booking.model.Cinema;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.CinemaMapper;
import com.cinema.ticket_booking.repository.CinemaRepository;
import com.cinema.ticket_booking.service.CinemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final CinemaMapper cinemaMapper;

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
    public void deactivate(UUID id) {
        Cinema cinema = findById(id);
        cinema.setIsActive(false);
        cinemaRepository.save(cinema);
    }

    @Override
    public Cinema findById(UUID id) {
        return cinemaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rạp chiếu", id));
    }
}
