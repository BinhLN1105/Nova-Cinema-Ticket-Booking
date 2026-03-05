package com.cinema.ticket_booking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lưu vector embedding của phim để thực hiện AI semantic search.
 * Yêu cầu Supabase extension: CREATE EXTENSION IF NOT EXISTS vector;
 *
 * Bảng này CHỈ được đọc/ghi bởi Python AI Service — Spring Boot không thao tác trực tiếp.
 * Spring Boot chỉ đọc để check embedding có tồn tại hay chưa.
 *
 * Dependency cần thêm vào pom.xml:
 * <dependency>
 *     <groupId>com.pgvector</groupId>
 *     <artifactId>pgvector</artifactId>
 *     <version>0.1.6</version>
 * </dependency>
 */
@Entity
@Table(name = "movie_embeddings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieEmbedding {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Mỗi phim có đúng 1 embedding (1-1)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false, unique = true)
    private Movie movie;

    /**
     * Vector embedding 768 chiều từ Gemini API.
     * Lưu dưới dạng float[] — pgvector tự xử lý kiểu VECTOR(768).
     * Dùng @Column(columnDefinition) để Hibernate không cố tạo kiểu mặc định.
     */
    @Column(name = "embedding", columnDefinition = "vector(768)", nullable = false)
    private float[] embedding;

    // Tên model đã tạo embedding: "gemini-embedding-001", ...
    @Column(name = "model_name", length = 50, nullable = false)
    private String modelName;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
