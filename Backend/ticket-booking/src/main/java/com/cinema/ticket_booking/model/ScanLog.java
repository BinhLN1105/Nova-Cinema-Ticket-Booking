package com.cinema.ticket_booking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lưu lịch sử mỗi lần nhân viên quét QR soát vé.
 * Bao gồm cả thành công và thất bại để phục vụ màn hình "Lịch sử soát vé".
 */
@Entity
@Table(name = "scan_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanLog {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Nhân viên thực hiện quét */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    /** Rạp nơi nhân viên làm việc (denormalized để dễ filter) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    /** Booking được quét — null nếu QR hoàn toàn không hợp lệ */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    /** QR code raw (để trace nếu cần debug) */
    @Column(name = "qr_code_raw", columnDefinition = "TEXT")
    private String qrCodeRaw;

    /** true = check-in thành công, false = thất bại */
    @Column(name = "success", nullable = false)
    private boolean success;

    /** Lý do thất bại — null nếu thành công */
    @Column(name = "fail_reason", length = 200)
    private String failReason;

    /** Tên phim (denormalized để hiển thị nhanh kể cả khi booking null) */
    @Column(name = "movie_title", length = 200)
    private String movieTitle;

    /** Poster URL phim (denormalized) */
    @Column(name = "movie_poster_url", columnDefinition = "TEXT")
    private String moviePosterUrl;

    /** Thông tin khách hàng (denormalized) */
    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    /** Ghế đã check-in, dạng "A3, A4, B5" */
    @Column(name = "seats_checked", length = 100)
    private String seatsChecked;

    /** Tên phòng chiếu */
    @Column(name = "screen_name", length = 50)
    private String screenName;

    @CreationTimestamp
    @Column(name = "scanned_at", updatable = false)
    private LocalDateTime scannedAt;
}
