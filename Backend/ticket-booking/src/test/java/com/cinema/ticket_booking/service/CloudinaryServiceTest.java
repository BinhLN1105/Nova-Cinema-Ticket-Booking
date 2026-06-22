package com.cinema.ticket_booking.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
    }

    @Test
    void testUploadImage_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        byte[] bytes = "dummy image content".getBytes();
        when(file.getBytes()).thenReturn(bytes);

        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://cloudinary.com/result.jpg");
        when(uploader.upload(eq(bytes), anyMap())).thenReturn(uploadResult);

        String url = cloudinaryService.uploadImage(file, "movies");
        assertEquals("https://cloudinary.com/result.jpg", url);
    }

    @Test
    void testUploadImageFromUrl_Success() throws IOException {
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://cloudinary.com/result.jpg");
        when(uploader.upload(eq("https://someurl.com/image.jpg"), anyMap())).thenReturn(uploadResult);

        String url = cloudinaryService.uploadImageFromUrl("https://someurl.com/image.jpg", "movies");
        assertEquals("https://cloudinary.com/result.jpg", url);
    }

    @Test
    void testDeleteImageAsync_NullOrEmpty() throws IOException {
        cloudinaryService.deleteImageAsync(null);
        cloudinaryService.deleteImageAsync("");
        verify(uploader, never()).destroy(anyString(), anyMap());
    }

    @Test
    void testDeleteImageAsync_Success() throws IOException {
        cloudinaryService.deleteImageAsync("movies/sample_id");
        verify(uploader, times(1)).destroy(eq("movies/sample_id"), anyMap());
    }

    @Test
    void testDeleteImageAsync_IOException() throws IOException {
        when(uploader.destroy(eq("movies/sample_id"), anyMap())).thenThrow(new IOException("Network error"));
        // Should catch the exception without propagating it
        assertDoesNotThrow(() -> cloudinaryService.deleteImageAsync("movies/sample_id"));
    }

    @Test
    void testExtractPublicId() {
        assertNull(cloudinaryService.extractPublicId(null));
        assertNull(cloudinaryService.extractPublicId(""));
        assertNull(cloudinaryService.extractPublicId("https://example.com/image.jpg"));

        // Normal URL with version
        String url1 = "https://res.cloudinary.com/demo/image/upload/v1570975200/sample.jpg";
        assertEquals("sample", cloudinaryService.extractPublicId(url1));

        // URL without version
        String url2 = "https://res.cloudinary.com/demo/image/upload/sample.jpg";
        assertEquals("sample", cloudinaryService.extractPublicId(url2));

        // URL with subfolder and version
        String url3 = "https://res.cloudinary.com/demo/image/upload/v1570975200/movies/sample.png";
        assertEquals("movies/sample", cloudinaryService.extractPublicId(url3));

        // URL where 'v' string is not a version but part of path
        String url4 = "https://res.cloudinary.com/demo/image/upload/veryimportantpath/sample.jpg";
        assertEquals("veryimportantpath/sample", cloudinaryService.extractPublicId(url4));
    }
}
