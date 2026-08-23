package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.CinemaRequest;
import com.cinema.ticket_booking.dto.response.CinemaResponse;
import com.cinema.ticket_booking.dto.response.CinemaSyncResponse;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.CinemaMapper;
import com.cinema.ticket_booking.model.Cinema;
import com.cinema.ticket_booking.model.CinemaWeatherCache;
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
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
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
                .imageUrl("https://res.cloudinary.com/demo/image/upload/old_theater.jpg")
                .isActive(true)
                .build();

        cinemaResponse = CinemaResponse.builder()
                .id(cinemaId.toString())
                .name("Nova Cinema Nguyễn Trãi")
                .address("123 Nguyễn Trãi")
                .city("Hà Nội")
                .imageUrl("https://res.cloudinary.com/demo/image/upload/old_theater.jpg")
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
    @DisplayName("3. getAll không truyền city trả về tất cả rạp active")
    void testGetAllWithoutCity() {
        when(cinemaRepository.findByIsActiveTrue()).thenReturn(List.of(cinema));
        when(cinemaMapper.toResponse(cinema)).thenReturn(cinemaResponse);

        List<CinemaResponse> result = cinemaService.getAll(null);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("4. getAllForAdmin trả về tất cả rạp")
    void testGetAllForAdmin() {
        when(cinemaRepository.findAll()).thenReturn(List.of(cinema));
        when(cinemaMapper.toResponse(cinema)).thenReturn(cinemaResponse);

        List<CinemaResponse> result = cinemaService.getAllForAdmin();
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("5. getById thành công khi tìm thấy rạp và enrich tọa độ")
    void testGetByIdSuccess() {
        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(cinemaMapper.toResponse(cinema)).thenReturn(cinemaResponse);

        CinemaWeatherCache cache = CinemaWeatherCache.builder()
                .cinemaId(cinemaId)
                .latitude(21.0285)
                .longitude(105.8542)
                .build();
        when(weatherCacheRepository.findById(cinemaId)).thenReturn(Optional.of(cache));

        CinemaResponse result = cinemaService.getById(cinemaId);
        assertNotNull(result);
        assertEquals(cinemaId.toString(), result.getId());
        assertEquals(21.0285, result.getLatitude());
        assertEquals(105.8542, result.getLongitude());
    }

    @Test
    @DisplayName("6. getById ném ResourceNotFoundException khi không tìm thấy")
    void testGetByIdNotFound() {
        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cinemaService.getById(cinemaId));
    }

    @Test
    @DisplayName("7. create cinema thành công")
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

    @Test
    @DisplayName("8. update cinema thành công")
    void testUpdateCinema() {
        CinemaRequest request = new CinemaRequest();
        request.setName("Nova Cinema Update");

        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.of(cinema));
        doNothing().when(cinemaMapper).updateEntity(request, cinema);
        when(cinemaRepository.save(cinema)).thenReturn(cinema);
        when(cinemaMapper.toResponse(cinema)).thenReturn(cinemaResponse);

        CinemaResponse result = cinemaService.update(cinemaId, request);
        assertNotNull(result);
        verify(cinemaRepository, times(1)).save(cinema);
    }

    @Test
    @DisplayName("9. updateImage thành công và xóa ảnh cũ")
    void testUpdateImage_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "image_content".getBytes());

        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(cloudinaryService.uploadImage(file, "Theater")).thenReturn("https://res.cloudinary.com/demo/image/upload/new_theater.jpg");
        when(cloudinaryService.extractPublicId("https://res.cloudinary.com/demo/image/upload/old_theater.jpg")).thenReturn("old_public_id");
        when(cinemaRepository.save(cinema)).thenReturn(cinema);
        when(cinemaMapper.toResponse(cinema)).thenReturn(cinemaResponse);

        CinemaResponse result = cinemaService.updateImage(cinemaId, file);
        assertNotNull(result);
        verify(cloudinaryService, times(1)).deleteImageAsync("old_public_id");
    }

    @Test
    @DisplayName("10. updateImage thất bại và dọn dẹp ảnh mới tải lên")
    void testUpdateImage_FailureCleansUp() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "image_content".getBytes());

        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(cloudinaryService.uploadImage(file, "Theater")).thenReturn("https://res.cloudinary.com/demo/image/upload/new_theater.jpg");
        when(cinemaRepository.save(cinema)).thenThrow(new RuntimeException("DB Error"));
        when(cloudinaryService.extractPublicId("https://res.cloudinary.com/demo/image/upload/new_theater.jpg")).thenReturn("new_public_id");

        assertThrows(RuntimeException.class, () -> cinemaService.updateImage(cinemaId, file));
        verify(cloudinaryService, times(1)).deleteImageAsync("new_public_id");
    }

    @Test
    @DisplayName("11. updateImageFromUrl thành công")
    void testUpdateImageFromUrl_Success() throws IOException {
        String newUrl = "https://images.unsplash.com/photo-12345";
        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(cloudinaryService.uploadImageFromUrl(newUrl, "Theater")).thenReturn("https://res.cloudinary.com/demo/image/upload/new_url_img.jpg");
        when(cloudinaryService.extractPublicId(cinema.getImageUrl())).thenReturn("old_pub_id");
        when(cinemaRepository.save(cinema)).thenReturn(cinema);
        when(cinemaMapper.toResponse(cinema)).thenReturn(cinemaResponse);

        CinemaResponse result = cinemaService.updateImageFromUrl(cinemaId, newUrl);
        assertNotNull(result);
        verify(cloudinaryService, times(1)).deleteImageAsync("old_pub_id");
    }

    @Test
    @DisplayName("12. updateImageFromUrl thất bại và dọn dẹp ảnh mới")
    void testUpdateImageFromUrl_Failure() throws IOException {
        String newUrl = "https://images.unsplash.com/photo-12345";
        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(cloudinaryService.uploadImageFromUrl(newUrl, "Theater")).thenReturn("https://res.cloudinary.com/demo/image/upload/new_url_img.jpg");
        when(cinemaRepository.save(cinema)).thenThrow(new RuntimeException("DB Error"));
        when(cloudinaryService.extractPublicId("https://res.cloudinary.com/demo/image/upload/new_url_img.jpg")).thenReturn("new_pub_id");

        assertThrows(RuntimeException.class, () -> cinemaService.updateImageFromUrl(cinemaId, newUrl));
        verify(cloudinaryService, times(1)).deleteImageAsync("new_pub_id");
    }

    @Test
    @DisplayName("13. toggleStatus chuyển đổi trạng thái active")
    void testToggleStatus() {
        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(cinemaRepository.save(cinema)).thenReturn(cinema);
        when(cinemaMapper.toResponse(cinema)).thenReturn(cinemaResponse);

        CinemaResponse result = cinemaService.toggleStatus(cinemaId);
        assertNotNull(result);
        assertFalse(cinema.getIsActive());
    }

    @Test
    @DisplayName("14. delete cinema thành công khi không có phòng chiếu và booking")
    void testDelete_Success() {
        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(screenRepository.countByCinemaIdAndIsDeletedFalse(cinemaId)).thenReturn(0L);
        when(bookingRepository.existsByCinemaId(cinemaId)).thenReturn(false);

        cinemaService.delete(cinemaId);
        verify(cinemaRepository, times(1)).delete(cinema);
    }

    @Test
    @DisplayName("15. delete ném lỗi khi rạp có phòng chiếu đang hoạt động")
    void testDelete_HasScreens() {
        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(screenRepository.countByCinemaIdAndIsDeletedFalse(cinemaId)).thenReturn(2L);

        assertThrows(IllegalStateException.class, () -> cinemaService.delete(cinemaId));
        verify(cinemaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("16. delete ném lỗi khi rạp đã có booking")
    void testDelete_HasBookings() {
        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(screenRepository.countByCinemaIdAndIsDeletedFalse(cinemaId)).thenReturn(0L);
        when(bookingRepository.existsByCinemaId(cinemaId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> cinemaService.delete(cinemaId));
        verify(cinemaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("17. updateCoordinates tạo cache mới khi chưa có")
    void testUpdateCoordinates_NewCache() {
        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(weatherCacheRepository.findById(cinemaId)).thenReturn(Optional.empty());

        cinemaService.updateCoordinates(cinemaId, 21.0, 105.8);

        verify(weatherCacheRepository, times(1)).save(argThat(c ->
                c.getCinemaId().equals(cinemaId) &&
                c.getLatitude().equals(21.0) &&
                c.getLongitude().equals(105.8)
        ));
    }

    @Test
    @DisplayName("18. updateCoordinates cập nhật cache đã tồn tại")
    void testUpdateCoordinates_ExistingCache() {
        CinemaWeatherCache existing = CinemaWeatherCache.builder()
                .cinemaId(cinemaId)
                .latitude(10.0)
                .longitude(106.0)
                .build();
        when(cinemaRepository.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(weatherCacheRepository.findById(cinemaId)).thenReturn(Optional.of(existing));

        cinemaService.updateCoordinates(cinemaId, 21.0, 105.8);

        assertEquals(21.0, existing.getLatitude());
        assertEquals(105.8, existing.getLongitude());
        verify(weatherCacheRepository, times(1)).save(existing);
    }
}
