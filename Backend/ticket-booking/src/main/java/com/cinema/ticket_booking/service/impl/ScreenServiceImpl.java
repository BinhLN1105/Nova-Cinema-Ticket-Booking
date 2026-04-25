package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.ScreenRequest;
import com.cinema.ticket_booking.dto.request.ScreenSeatLayoutRequest;
import com.cinema.ticket_booking.dto.response.ScreenResponse;
import com.cinema.ticket_booking.model.Cinema;
import com.cinema.ticket_booking.model.Screen;
import com.cinema.ticket_booking.model.Seat;
import com.cinema.ticket_booking.enums.SeatType;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.ScreenMapper;
import com.cinema.ticket_booking.repository.ScreenRepository;
import com.cinema.ticket_booking.repository.SeatRepository;
import com.cinema.ticket_booking.repository.ShowtimeRepository;
import com.cinema.ticket_booking.service.CinemaService;
import com.cinema.ticket_booking.service.ScreenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ScreenServiceImpl implements ScreenService {

    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final CinemaService cinemaService;
    private final ScreenMapper screenMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ScreenResponse> getByCinema(UUID cinemaId) {
        cinemaService.findById(cinemaId); // validate tồn tại
        return screenRepository.findByCinemaIdAndIsActiveTrueAndIsDeletedFalse(cinemaId)
                .stream().map(screenMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScreenResponse> getByCinemaForAdmin(UUID cinemaId) {
        cinemaService.findById(cinemaId); // validate tồn tại
        return screenRepository.findByCinemaIdAndIsDeletedFalse(cinemaId)
                .stream().map(screenMapper::toResponse).toList();
    }

    @Override
    public ScreenResponse create(ScreenRequest request) {
        Cinema cinema = cinemaService.findById(UUID.fromString(request.getCinemaId()));
        Screen screen = screenMapper.toEntity(request);
        screen.setCinema(cinema);
        Screen savedScreen = screenRepository.save(screen);

        // ── Auto-generate ghế (chế độ tự động) ──
        int totalRows = savedScreen.getTotalRows();
        int totalCols = savedScreen.getTotalCols();

        for (int i = 0; i < totalRows; i++) {
            String rowLabel = generateRowLabel(i);
            for (int j = 1; j <= totalCols; j++) {
                Seat seat = Seat.builder()
                        .screen(savedScreen)
                        .rowLabel(rowLabel.charAt(0)) // Giữ tương thích cũ nếu rowLabel vẫn là char trong DB
                        .colNumber(j)
                        .gridRow(i)
                        .gridCol(j - 1)
                        .seatLabel(rowLabel + j)
                        .seatType(SeatType.STANDARD)
                        .isActive(true)
                        .build();
                savedScreen.getSeats().add(seat);
            }
        }
        screenRepository.save(savedScreen);

        return screenMapper.toResponse(savedScreen);
    }

    // ── Lưu bố trí ghế tuỳ chỉnh (Custom Layout) ──────────────────────────
    @Override
    public void saveCustomLayout(ScreenSeatLayoutRequest request) {
        Screen screen = findById(UUID.fromString(request.getScreenId()));

        // Chặn sửa layout nếu phòng đã từng được dùng cho bất kỳ suất chiếu nào (tránh FK violation)
        boolean hasHistory = showtimeRepository.existsByScreenId(screen.getId());
        if (hasHistory) {
            throw new BadRequestException(
                    "Không thể thay đổi bố trí ghế khi phòng chiếu đã có lịch sử suất chiếu (kể cả đã kết thúc). " +
                            "Để thay đổi sơ đồ, vui lòng tạo 'Phòng Mới' hoặc xóa các suất chiếu liên quan trước.");
        }

        // Xóa toàn bộ ghế cũ (Sử dụng orphanRemoval = true để thực hiện Hard Delete thật sự)
        // Việc Hard Delete là bắt buộc để tránh vi phạm Unique Constraint khi lưu Layout mới
        screen.getSeats().clear();
        screenRepository.saveAndFlush(screen); // Đảm bảo xóa ngay lập tức để tránh trùng lặp khi insert mới


        // Tạo ghế mới từ layout custom
        // Tính maxRow, maxCol để cập nhật kích thước Screen
        int maxRow = 0;
        int maxCol = 0;
        List<Seat> newSeats = new ArrayList<>();
        int seatIndex = 0;

        for (ScreenSeatLayoutRequest.SeatDefinition def : request.getSeats()) {
            maxRow = Math.max(maxRow, def.getGridRow());
            maxCol = Math.max(maxCol, def.getGridCol());

            // Tính rowLabel & colNumber legacy từ seatLabel
            char rowLabel = def.getSeatLabel().charAt(0);
            int colNumber = seatIndex + 1;
            try {
                colNumber = Integer.parseInt(def.getSeatLabel().substring(1));
            } catch (NumberFormatException e) {
                colNumber = seatIndex + 1;
            }

            Seat seat = Seat.builder()
                    .screen(screen)
                    .rowLabel(rowLabel)
                    .colNumber(colNumber)
                    .gridRow(def.getGridRow())
                    .gridCol(def.getGridCol())
                    .seatLabel(def.getSeatLabel())
                    .seatType(def.getSeatType())
                    .isActive(true)
                    .build();
            newSeats.add(seat);
            seatIndex++;
        }

        screen.getSeats().addAll(newSeats);


        // Cập nhật kích thước ma trận của Screen
        screen.setTotalRows(maxRow + 1);
        screen.setTotalCols(maxCol + 1);
        screenRepository.save(screen);
    }

    // ── Lấy danh sách ghế của phòng chiếu (cho Seat Builder) ─────────────
    @Override
    @Transactional(readOnly = true)
    public List<Seat> getSeats(UUID screenId) {
        findById(screenId); // validate tồn tại
        return seatRepository.findByScreenIdAndIsActiveTrueOrderByRowLabelAscColNumberAsc(screenId);
    }

    @Override
    public ScreenResponse update(UUID id, ScreenRequest request) {
        Screen screen = findById(id);

        // Khách hàng bấm chuyển sang trạng thái Bảo trì
        if (Boolean.TRUE.equals(screen.getIsActive()) && Boolean.FALSE.equals(request.getIsActive())) {
            boolean hasScheduled = showtimeRepository.existsConflict(
                    screen.getId(),
                    LocalDateTime.now(),
                    LocalDateTime.now().plusYears(10));
            if (hasScheduled) {
                throw new BadRequestException(
                        "Không thể bảo trì phòng này vì đang có suất chiếu chưa đá. " +
                                "Vui lòng hủy suất chiếu trước!");
            }
        }

        screenMapper.updateEntity(request, screen);
        return screenMapper.toResponse(screenRepository.save(screen));
    }

    @Override
    public void delete(UUID id, String type) {
        Screen screen = findById(id);
        
        if ("hard".equalsIgnoreCase(type)) {
            // Kiểm tra xem phòng chiếu đã từng có lịch chiếu chưa
            if (showtimeRepository.existsByScreenId(screen.getId())) {
                throw new BadRequestException("Không thể xóa cứng phòng chiếu đã có lịch sử suất chiếu. Vui lòng sử dụng phương thức Xóa mềm (Bảo trì).");
            }
            // Hibernate sẽ tự động xóa ghế do cấu hình cascade = CascadeType.ALL, orphanRemoval = true
            screenRepository.delete(screen);
        } else {
            // Soft delete
            // Kiểm tra nếu có lịch chiếu tương lai thì không cho xóa mềm
            boolean hasScheduled = showtimeRepository.existsConflict(
                    screen.getId(),
                    LocalDateTime.now(),
                    LocalDateTime.now().plusYears(10)
            );
            if (hasScheduled) {
                throw new BadRequestException("Không thể xóa mềm (đưa vào thùng rác) vì phòng đang có suất chiếu chưa diễn ra. Vui lòng hủy các suất chiếu trước.");
            }
            screen.setIsDeleted(true);
            screenRepository.save(screen);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Screen findById(UUID id) {
        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng chiếu", id));
        if (Boolean.TRUE.equals(screen.getIsDeleted())) {
            throw new ResourceNotFoundException("Phòng chiếu", id);
        }
        return screen;
    }

    private String generateRowLabel(int index) {
        StringBuilder label = new StringBuilder();
        int n = index;
        while (n >= 0) {
            label.insert(0, (char) ('A' + (n % 26)));
            n = (n / 26) - 1;
        }
        return label.toString();
    }
}
