package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.GenreResponse;
import com.cinema.ticket_booking.model.Genre;
import com.cinema.ticket_booking.exception.ConflictException;
import com.cinema.ticket_booking.mapper.GenreMapper;
import com.cinema.ticket_booking.repository.GenreRepository;
import com.cinema.ticket_booking.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    @Override
    @Transactional(readOnly = true)
    public List<GenreResponse> getAll() {
        return genreRepository.findAll().stream().map(genreMapper::toResponse).toList();
    }

    @Override
    public GenreResponse create(String name) {
        if (genreRepository.existsByName(name)) {
            throw new ConflictException("Thể loại '" + name + "' đã tồn tại");
        }
        Genre genre = Genre.builder()
                .name(name)
                .slug(toSlug(name))
                .build();
        return genreMapper.toResponse(genreRepository.save(genre));
    }

    private String toSlug(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-", "")
                .replaceAll("-$", "");
    }
}
