package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.ScreenRequest;
import com.cinema.ticket_booking.dto.response.ScreenResponse;
import com.cinema.ticket_booking.model.Cinema;
import com.cinema.ticket_booking.model.Screen;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.ScreenMapper;
import com.cinema.ticket_booking.repository.ScreenRepository;
import com.cinema.ticket_booking.service.CinemaService;
import com.cinema.ticket_booking.service.ScreenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ScreenServiceImpl implements ScreenService {

    private final ScreenRepository screenRepository;
    private final CinemaService cinemaService;
    private final ScreenMapper screenMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ScreenResponse> getByCinema(UUID cinemaId) {
        cinemaService.findById(cinemaId); // validate tồn tại
        return screenRepository.findByCinemaIdAndIsActiveTrue(cinemaId)
                .stream().map(screenMapper::toResponse).toList();
    }

    @Override
    public ScreenResponse create(ScreenRequest request) {
        Cinema cinema = cinemaService.findById(UUID.fromString(request.getCinemaId()));
        Screen screen = screenMapper.toEntity(request);
        screen.setCinema(cinema);
        return screenMapper.toResponse(screenRepository.save(screen));
    }

    @Override
    public ScreenResponse update(UUID id, ScreenRequest request) {
        Screen screen = findById(id);
        screenMapper.updateEntity(request, screen);
        return screenMapper.toResponse(screenRepository.save(screen));
    }

    @Override
    @Transactional(readOnly = true)
    public Screen findById(UUID id) {
        return screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng chiếu", id));
    }
}
