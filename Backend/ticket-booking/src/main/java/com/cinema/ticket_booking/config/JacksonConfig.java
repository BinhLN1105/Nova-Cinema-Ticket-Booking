package com.cinema.ticket_booking.config;

import com.cinema.ticket_booking.security.XssStringDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson config:
 * - LocalDateTime → ISO-8601 string (không phải array)
 * - camelCase field names
 * - Không in timestamp dạng số
 * - Xử lý chống XSS trên tất cả trường chuỗi JSON request body
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        SimpleModule xssModule = new SimpleModule();
        xssModule.addDeserializer(String.class, new XssStringDeserializer());

        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .addModule(xssModule)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
                .build();
    }
}
