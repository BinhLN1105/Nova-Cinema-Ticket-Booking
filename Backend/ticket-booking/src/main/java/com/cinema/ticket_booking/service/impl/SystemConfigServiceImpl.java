package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.model.SystemConfig;
import com.cinema.ticket_booking.repository.SystemConfigRepository;
import com.cinema.ticket_booking.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository repository;

    @Transactional
    public void initDefaults() {
        // Initialize default configurations if they don't exist
        createIfMissing("DEFAULT_SEAT_HOLD_TIME", "10", "Thời gian giữ ghế mặc định (phút)");
        createIfMissing("LATE_SEAT_HOLD_TIME", "3", "Thời gian giữ ghế cực ngắn khi đặt vé sát giờ chiếu (phút)");
        createIfMissing("LATE_BOOKING_ALLOWANCE_MINS", "10", "Cho phép đặt vé sau giờ chiếu tối đa (phút)");
        createIfMissing("CLEANUP_TIME_MINUTES", "15", "Thời gian dọn phòng sau mỗi suất chiếu (phút)");
        createIfMissing("CANCEL_MIN_HOURS_BEFORE", "2", "Thời gian tối thiểu cho phép hủy vé trước giờ chiếu (giờ)");
        createIfMissing("REFUND_PERCENT_CINEPOINT", "100", "Phần trăm số tiền hoàn lại dưới dạng CinePoint (%)");
        
        // Homepage Banner Configs
        createIfMissing("HERO_SECTION_MODE", "TOP_SALES", "Chế độ hiển thị banner Android (TOP_RATING, NEW_RELEASE, MANUAL, TOP_SALES)");
        createIfMissing("HERO_SECTION_IDS", "", "Danh sách ID phim cho banner Android (Dùng cho chế độ MANUAL)");
        createIfMissing("HERO_SECTION_WEB_MODE", "TOP_RATING", "Chế độ hiển thị banner Web (TOP_RATING, NEW_RELEASE, MANUAL, TOP_SALES)");
        createIfMissing("HERO_SECTION_WEB_IDS", "", "Danh sách ID phim cho banner Web (Dùng cho chế độ MANUAL)");

        // Booking Limits Configs
        createIfMissing("BOOKING_MAX_SEATS", "6", "Số lượng ghế tối đa được đặt trong một giao dịch");
        createIfMissing("BOOKING_MAX_COMBOS", "8", "Số lượng combo bắp nước tối đa được đặt trong một giao dịch");
    }

    private void createIfMissing(String key, String defaultValue, String description) {
        if (!repository.existsById(key)) {
            repository.save(SystemConfig.builder()
                    .key(key)
                    .value(defaultValue)
                    .description(description)
                    .build());
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "system_configs", key = "#key")
    public String getConfig(String key, String defaultValue) {
        return repository.findById(key)
                .map(SystemConfig::getValue)
                .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public int getIntConfig(String key, int defaultValue) {
        try {
            return Integer.parseInt(getConfig(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "system_configs", key = "'all_configs'")
    public Map<String, String> getAllConfigs() {
        return repository.findAll().stream()
                .collect(Collectors.toMap(SystemConfig::getKey, SystemConfig::getValue));
    }

    @Override
    @Transactional
    @CacheEvict(value = "system_configs", allEntries = true)
    public void updateConfig(String key, String value, String description) {
        SystemConfig config = repository.findById(key).orElse(
                SystemConfig.builder().key(key).description(description).build());
        config.setValue(value);
        if (description != null && !description.isEmpty()) {
            config.setDescription(description);
        }
        repository.save(config);
    }
}
