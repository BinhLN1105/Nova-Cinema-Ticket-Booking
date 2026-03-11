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
import com.cinema.ticket_booking.model.Seat;
import com.cinema.ticket_booking.enums.SeatType;
import com.cinema.ticket_booking.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ScreenServiceImpl implements ScreenService {

    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
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
        Screen savedScreen = screenRepository.save(screen);

        List<Seat> seats = new ArrayList<>();
        int totalRows = savedScreen.getTotalRows();
        int totalCols = savedScreen.getTotalCols();
        
        for (int i = 0; i < totalRows; i++) {
            char rowLabel = (char) ('A' + i);
            for (int j = 1; j <= totalCols; j++) {
                SeatType type = SeatType.STANDARD;
                // Giả lập logic chọn ghế: hàng cuối là ghế đôi (COUPLE), các hàng giữa là VIP
                if (i == totalRows - 1) {
                    type = SeatType.COUPLE;
                } else if (i >= totalRows / 2 - 2 && i <= totalRows / 2 + 1) {
                    type = SeatType.VIP;
                }
                
                Seat seat = Seat.builder()
                        .screen(savedScreen)
                        .rowLabel(rowLabel)
                        .colNumber(j)
                        .seatType(type)
                        .isActive(true)
                        .build();
                seats.add(seat);
            }
        }
        seatRepository.saveAll(seats);

        return screenMapper.toResponse(savedScreen);
    }

    @Override
    public ScreenResponse update(UUID id, ScreenRequest request) {
        Screen screen = findById(id);
        screenMapper.updateEntity(request, screen);
        return screenMapper.toResponse(screenRepository.save(screen));
    }

    @Override
    public void delete(UUID id) {
        Screen screen = findById(id);
        screen.setIsActive(false); // Soft delete
        screenRepository.save(screen);
    }

    @Override
    @Transactional(readOnly = true)
    public Screen findById(UUID id) {
        return screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng chiếu", id));
    }
}
