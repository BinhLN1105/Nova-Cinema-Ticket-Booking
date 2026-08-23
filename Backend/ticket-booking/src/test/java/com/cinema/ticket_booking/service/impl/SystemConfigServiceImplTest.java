package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.model.SystemConfig;
import com.cinema.ticket_booking.repository.SystemConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceImplTest {

    @Mock
    private SystemConfigRepository repository;

    @InjectMocks
    private SystemConfigServiceImpl systemConfigService;

    @Test
    @DisplayName("1. initDefaults tạo các key còn thiếu và bỏ qua key đã có")
    void testInitDefaults() {
        // Giả lập DEFAULT_SEAT_HOLD_TIME đã tồn tại, các key khác chưa
        when(repository.existsById("DEFAULT_SEAT_HOLD_TIME")).thenReturn(true);
        when(repository.existsById(argThat(key -> !key.equals("DEFAULT_SEAT_HOLD_TIME")))).thenReturn(false);

        systemConfigService.initDefaults();

        // Kiểm tra save được gọi cho các key còn thiếu
        verify(repository, atLeast(5)).save(any(SystemConfig.class));
    }

    @Test
    @DisplayName("2. getConfig trả về giá trị trong DB nếu tồn tại")
    void testGetConfig_Exists() {
        SystemConfig config = SystemConfig.builder()
                .key("BOOKING_MAX_SEATS")
                .value("8")
                .description("Max seats")
                .build();
        when(repository.findById("BOOKING_MAX_SEATS")).thenReturn(Optional.of(config));

        String value = systemConfigService.getConfig("BOOKING_MAX_SEATS", "6");
        assertEquals("8", value);
    }

    @Test
    @DisplayName("3. getConfig trả về defaultValue nếu không tìm thấy key")
    void testGetConfig_NotFound() {
        when(repository.findById("NON_EXISTENT")).thenReturn(Optional.empty());

        String value = systemConfigService.getConfig("NON_EXISTENT", "default_val");
        assertEquals("default_val", value);
    }

    @Test
    @DisplayName("4. getIntConfig parse số nguyên thành công")
    void testGetIntConfig_ValidInteger() {
        SystemConfig config = SystemConfig.builder()
                .key("DEFAULT_SEAT_HOLD_TIME")
                .value("15")
                .build();
        when(repository.findById("DEFAULT_SEAT_HOLD_TIME")).thenReturn(Optional.of(config));

        int result = systemConfigService.getIntConfig("DEFAULT_SEAT_HOLD_TIME", 10);
        assertEquals(15, result);
    }

    @Test
    @DisplayName("5. getIntConfig trả về defaultValue khi giá trị không phải số hợp lệ")
    void testGetIntConfig_InvalidInteger() {
        SystemConfig config = SystemConfig.builder()
                .key("INVALID_NUM")
                .value("not_a_number")
                .build();
        when(repository.findById("INVALID_NUM")).thenReturn(Optional.of(config));

        int result = systemConfigService.getIntConfig("INVALID_NUM", 10);
        assertEquals(10, result);
    }

    @Test
    @DisplayName("6. getAllConfigs trả về Map key-value đầy đủ")
    void testGetAllConfigs() {
        SystemConfig c1 = SystemConfig.builder().key("K1").value("V1").build();
        SystemConfig c2 = SystemConfig.builder().key("K2").value("V2").build();
        when(repository.findAll()).thenReturn(List.of(c1, c2));

        Map<String, String> configs = systemConfigService.getAllConfigs();
        assertEquals(2, configs.size());
        assertEquals("V1", configs.get("K1"));
        assertEquals("V2", configs.get("K2"));
    }

    @Test
    @DisplayName("7. updateConfig cập nhật bản ghi đã tồn tại")
    void testUpdateConfig_Existing() {
        SystemConfig existing = SystemConfig.builder()
                .key("CANCEL_MIN_HOURS_BEFORE")
                .value("2")
                .description("Old desc")
                .build();
        when(repository.findById("CANCEL_MIN_HOURS_BEFORE")).thenReturn(Optional.of(existing));

        systemConfigService.updateConfig("CANCEL_MIN_HOURS_BEFORE", "4", "New desc");

        assertEquals("4", existing.getValue());
        assertEquals("New desc", existing.getDescription());
        verify(repository, times(1)).save(existing);
    }

    @Test
    @DisplayName("8. updateConfig tạo bản ghi mới nếu key chưa tồn tại")
    void testUpdateConfig_NewKey() {
        when(repository.findById("NEW_KEY")).thenReturn(Optional.empty());

        systemConfigService.updateConfig("NEW_KEY", "VAL", "Description");

        verify(repository, times(1)).save(argThat(config ->
                config.getKey().equals("NEW_KEY") &&
                config.getValue().equals("VAL") &&
                config.getDescription().equals("Description")
        ));
    }

    @Test
    @DisplayName("9. updateConfig không ghi đè description nếu truyền description rỗng")
    void testUpdateConfig_EmptyDescription() {
        SystemConfig existing = SystemConfig.builder()
                .key("KEY_1")
                .value("OLD_VAL")
                .description("Original desc")
                .build();
        when(repository.findById("KEY_1")).thenReturn(Optional.of(existing));

        systemConfigService.updateConfig("KEY_1", "NEW_VAL", "");

        assertEquals("NEW_VAL", existing.getValue());
        assertEquals("Original desc", existing.getDescription());
        verify(repository, times(1)).save(existing);
    }
}
