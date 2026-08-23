package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.CinemaRequest;
import com.cinema.ticket_booking.dto.response.CinemaResponse;
import com.cinema.ticket_booking.dto.response.CinemaSyncResponse;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.CinemaMapper;
import com.cinema.ticket_booking.model.Cinema;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.CinemaRepository;
import com.cinema.ticket_booking.repository.CinemaWeatherCacheRepository;
import com.cinema.ticket_booking.repository.ScreenRepository;
import com.cinema.ticket_booking.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CinemaServiceImplTest {

    @Mock
    private CinemaRepository cinemaRepository;
    @Mock
    private ScreenRepository screenRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CinemaMapper cinemaMapper;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private CinemaWeatherCacheRepository weatherCacheRepository;

    @InjectMocks
    private CinemaServiceImpl cinemaService;

    private Cinema cinema;
    private CinemaResponse cinemaResponse;
    private UUID cinemaId;

    @BeforeEach
    void setUp() {
        cinemaId = UUID.randomUUID();
        cinema = Cinema.builder()
                .id(cinemaId)
                .name("Nova Cinema Nguyễn Trãi")
                .address("123 Nguyễn Trãi")
                .city("Hà Nội")
                .isActive(true)
                .build();

        cinemaResponse = CinemaResponse.builder()
                .id(cinemaId.toString())
                .name("Nova Cinema Nguyễn Trãi")
                .address("123 Nguyễn Trãi")
                .city("Hà Nội")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("1. getAllForSync trả về danh sách rạp active")
    void testGetAllForSync() {
        when(cinemaRepository.findByIsActiveTrue()).thenReturn(List.of(cinema));
        List<CinemaSyncResponse> result = cinemaService.getAllForSync();
        assertEquals(1, result.size());
        assertEquals("Nova Cinema Nguyễn Trãi", result.get(0).getName());
    }

    @Test
    @DisplayName("2. getAll theo city thành công")
    void testGetAllWithCity() {
        when(cinemaRepository.searchCinemas("Hà Nội")).thenReturn(List.of(cinema));
        when(cinemaMapper.toResponse(cinema)).thenReturn(cinemaResponse);

        List<CinemaResponse> result = cinemaService.getAll("Hà Nội");
        assertEquals(1, result.size());
        assertEquals("Hà Nội", result.get(0).getCity());
    }

    @Test
    @DisplayName("3. getById thành công khi tìm thấy rạp")
    void testGetByIdSuccess() {
        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(cinemaMapper.toResponse(cinema)).thenReturn(cinemaResponse);

        CinemaResponse result = cinemaService.getById(cinemaId);
        assertNotNull(result);
        assertEquals(cinemaId.toString(), result.getId());
    }

    @Test
    @DisplayName("4. getById ném ResourceNotFoundException khi không tìm thấy")
    void testGetByIdNotFound() {
        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cinemaService.getById(cinemaId));
    }

    @Test
    @DisplayName("5. create cinema thành công")
    void testCreateCinema() {
        CinemaRequest request = new CinemaRequest();
        request.setName("Nova Cinema Cầu Giấy");
        request.setAddress("456 Cầu Giấy");
        request.setCity("Hà Nội");

        when(cinemaMapper.toEntity(request)).thenReturn(cinema);
        when(cinemaRepository.save(cinema)).thenReturn(cinema);
        when(cinemaMapper.toResponse(cinema)).thenReturn(cinemaResponse);

        CinemaResponse result = cinemaService.create(request);
        assertNotNull(result);
        verify(cinemaRepository, times(1)).save(cinema);
    }
}
