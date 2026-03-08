package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.ComboResponse;
import com.cinema.ticket_booking.mapper.ComboMapper;
import com.cinema.ticket_booking.repository.ComboRepository;
import com.cinema.ticket_booking.service.ComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComboServiceImpl implements ComboService {

    private final ComboRepository comboRepository;
    private final ComboMapper comboMapper;

    @Override
    public List<ComboResponse> getAvailable() {
        return comboRepository.findByIsAvailableTrue()
                .stream().map(comboMapper::toResponse).toList();
    }
}
