package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.GenreResponse;
import com.cinema.ticket_booking.exception.ConflictException;
import com.cinema.ticket_booking.mapper.GenreMapper;
import com.cinema.ticket_booking.model.Genre;
import com.cinema.ticket_booking.repository.GenreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreServiceImplTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private GenreMapper genreMapper;

    @InjectMocks
    private GenreServiceImpl genreService;

    @Test
    void testGetAll() {
        Genre genre = Genre.builder().name("Hành Động").build();
        when(genreRepository.findAll()).thenReturn(List.of(genre));

        GenreResponse response = new GenreResponse();
        response.setName("Hành Động");
        when(genreMapper.toResponse(genre)).thenReturn(response);

        List<GenreResponse> results = genreService.getAll();
        assertEquals(1, results.size());
        assertEquals("Hành Động", results.get(0).getName());
    }

    @Test
    void testCreate_AlreadyExists() {
        when(genreRepository.existsByName("Hành Động")).thenReturn(true);

        assertThrows(ConflictException.class, () -> genreService.create("Hành Động"));
        verify(genreRepository, never()).save(any());
    }

    @Test
    void testCreate_Success() {
        when(genreRepository.existsByName("Hành Động")).thenReturn(false);

        Genre savedGenre = Genre.builder().name("Hành Động").slug("hanh-dong").build();
        when(genreRepository.save(any(Genre.class))).thenReturn(savedGenre);

        GenreResponse response = new GenreResponse();
        response.setName("Hành Động");
        response.setSlug("hanh-dong");
        when(genreMapper.toResponse(savedGenre)).thenReturn(response);

        GenreResponse result = genreService.create("Hành Động");
        assertNotNull(result);
        assertEquals("Hành Động", result.getName());
        assertEquals("hanh-dong", result.getSlug());
    }
}
