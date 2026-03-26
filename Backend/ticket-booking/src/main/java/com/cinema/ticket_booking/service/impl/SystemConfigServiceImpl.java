package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.model.SystemConfig;
import com.cinema.ticket_booking.repository.SystemConfigRepository;
import com.cinema.ticket_booking.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository repository;

    @PostConstruct
    @Transactional
    public void initDefaults() {
        // Initialize default configurations if they don't exist
        createIfMissing("DEFAULT_SEAT_HOLD_TIME", "10", "Thời gian giữ ghế mặc định (phút)");
        createIfMissing("LATE_SEAT_HOLD_TIME", "3", "Thời gian giữ ghế cực ngắn khi đặt vé sát giờ chiếu (phút)");
        createIfMissing("LATE_BOOKING_ALLOWANCE_MINS", "10", "Cho phép đặt vé sau giờ chiếu tối đa (phút)");
        createIfMissing("NO_SHOW_EXP_PENALTY_PERCENT", "0",
                "Phần trăm trừ EXP nếu người dùng mua vé nhưng không check-in (0-100)");
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
