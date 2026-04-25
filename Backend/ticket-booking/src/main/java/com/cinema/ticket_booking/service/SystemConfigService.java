package com.cinema.ticket_booking.service;

import java.util.Map;

public interface SystemConfigService {
    String getConfig(String key, String defaultValue);

    int getIntConfig(String key, int defaultValue);

    Map<String, String> getAllConfigs();

    void updateConfig(String key, String value, String description);
    
    void initDefaults();
}
