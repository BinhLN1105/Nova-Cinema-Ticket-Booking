package com.cinema.ticket_booking.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;



    /**
     * Upload ảnh lên Cloudinary vào thư mục cụ thể
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "Nova Ticket Booking/" + folder,
                        "resource_type", "auto"
                ));
        return uploadResult.get("secure_url").toString();
    }

    /**
     * Upload ảnh từ URL (Cloudinary tự fetch)
     */
    public String uploadImageFromUrl(String url, String folder) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(url,
                ObjectUtils.asMap(
                        "folder", "Nova Ticket Booking/" + folder,
                        "resource_type", "auto"
                ));
        return uploadResult.get("secure_url").toString();
    }

    /**
     * Xóa ảnh bất đồng bộ để không chặn luồng xử lý chính
     */
    @Async("asyncExecutor")
    public void deleteImageAsync(String publicId) {
        if (publicId == null || publicId.isEmpty()) return;

        try {
            log.info("[Cloudinary] Đang xóa ảnh cũ chạy ngầm: {}", publicId);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.error("[Cloudinary] Lỗi khi xóa ảnh trên Cloud: {}", e.getMessage());
        }
    }

    /**
     * Trích xuất Public ID từ URL để thực hiện lệnh xóa
     */
    public String extractPublicId(String url) {
        if (url == null || url.isEmpty()) return null;

        int uploadIndex = url.indexOf("/upload/");
        if (uploadIndex == -1) return null;

        // Cắt chuỗi từ sau "/upload/"
        String afterUpload = url.substring(uploadIndex + 8);

        // Bỏ version nếu có (ví dụ "v12345678/")
        if (afterUpload.startsWith("v") && afterUpload.indexOf('/') > 0) {
            String temp = afterUpload.substring(0, afterUpload.indexOf('/'));
            if (temp.substring(1).matches("\\d+")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf('/') + 1);
            }
        }

        // Loại bỏ phần đuôi mở rộng (extension) như ".jpg", ".png"
        int dotIndex = afterUpload.lastIndexOf('.');
        if (dotIndex != -1) {
            afterUpload = afterUpload.substring(0, dotIndex);
        }

        return afterUpload;
    }
}
