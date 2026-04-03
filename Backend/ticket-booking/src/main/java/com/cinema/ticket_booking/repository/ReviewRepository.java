package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

  // Lấy review của 1 phim (chỉ hiển thị review công khai, mới nhất trước)
  Page<Review> findByMovieIdAndIsVisibleTrueOrderByCreatedAtDesc(UUID movieId, Pageable pageable);

  // Kiểm tra user đã review phim này chưa (1 Review / 1 Phim / 1 Account)
  boolean existsByUserIdAndMovieId(UUID userId, UUID movieId);

  // Lấy review hiện tại của user cho phim
  Optional<Review> findByUserIdAndMovieId(UUID userId, UUID movieId);

  // Tính lại điểm trung bình sau khi thêm review mới
  @Query("""
          SELECT AVG(r.rating) FROM Review r
          WHERE r.movie.id = :movieId
            AND r.isVisible = true
      """)
  Double calculateAvgRating(@Param("movieId") UUID movieId);

  // Lấy review của user
  Page<Review> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

  // Xóa tất cả các review của 1 phim (dùng khi xóa phim)
  @Transactional
  @Modifying
  @Query("DELETE FROM Review r WHERE r.movie.id = :movieId")
  void deleteByMovieId(@Param("movieId") UUID movieId);
}
