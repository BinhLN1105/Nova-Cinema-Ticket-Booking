package com.cinema.ticket_booking.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.dao.DataIntegrityViolationException;

import com.cinema.ticket_booking.dto.request.LoginRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.hibernate.StaleObjectStateException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 400 Bad Request ────────────────────────────────────────────────────

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // @Valid / @Validated validation lỗi — trả về từng field
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        if (ex.getBindingResult().getTarget() instanceof LoginRequest) {
            LoginRequest req = (LoginRequest) ex.getBindingResult().getTarget();
            boolean isMissingFields = req.getEmail() == null || req.getEmail().trim().isEmpty()
                    || req.getPassword() == null || req.getPassword().trim().isEmpty();
            if (!isMissingFields) {
                ErrorResponse body = ErrorResponse.builder()
                        .success(false)
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .message("Tài khoản hoặc mật khẩu không chính xác")
                        .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }
        }

        Map<String, String> errors = new HashMap<>();
        StringBuilder detailMessage = new StringBuilder("Dữ liệu không hợp lệ");
        boolean first = true;
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
            if (first) {
                detailMessage.append(": ");
                first = false;
            } else {
                detailMessage.append(", ");
            }
            detailMessage.append(fe.getDefaultMessage());
        }
        ErrorResponse body = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.BAD_REQUEST.value())
                .message(detailMessage.toString())
                .errors(errors)
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    // ── 401 Unauthorized ───────────────────────────────────────────────────

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // ── 403 Forbidden ──────────────────────────────────────────────────────

    @ExceptionHandler({
            ForbiddenException.class,
            AuthorizationDeniedException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<ErrorResponse> handleForbidden(Exception ex) {
        log.warn("Access Denied: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện hành động này");
    }

    // ── 404 Not Found ──────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ── 409 Conflict ───────────────────────────────────────────────────────

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Xử lý xung đột dữ liệu (optimistic locking)
    @ExceptionHandler({ ObjectOptimisticLockingFailureException.class, StaleObjectStateException.class })
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(Exception ex) {
        log.warn("Data conflict detected: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT,
                "Yêu cầu đang được xử lý, vui lòng không thao tác liên tiếp");
    }

    // Xử lý lỗi trùng lặp dữ liệu / Vi phạm ràng buộc database (Unique Constraint)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        String message = "Vi phạm ràng buộc dữ liệu hoặc dữ liệu đã tồn tại trong hệ thống";

        // Phát hiện cụ thể unique constraint trùng tọa độ ghế trên screen
        if (ex.getMessage() != null && ex.getMessage().contains("uk_seat_screen_grid")) {
            message = "Tọa độ ghế (hàng và cột) trên phòng chiếu này đã tồn tại";
        }

        return build(HttpStatus.CONFLICT, message);
    }

    // Xử lý trạng thái nghiệp vụ không hợp lệ (ví dụ: Xóa rạp khi đang có phòng
    // chiếu/booking)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Xử lý tải tệp tin quá giới hạn cho phép
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("Max upload size exceeded: {}", ex.getMessage());
        return build(HttpStatus.CONTENT_TOO_LARGE, "Kích thước tệp tải lên vượt quá giới hạn cho phép");
    }

    // ── 402 Payment Required ───────────────────────────────────────────────

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePayment(PaymentException ex) {
        return build(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    // ── Custom Application Exceptions (e.g., Rate Limiting) ────────────────
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        return build(ex.getStatus(), ex.getMessage());
    }

    // ── 500 Internal Server Error (fallback) ──────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau");
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .success(false)
                        .status(status.value())
                        .message(message)
                        .build());
    }
}
