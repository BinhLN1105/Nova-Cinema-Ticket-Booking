package com.cinema.ticket_booking.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

        @Bean
        @SuppressWarnings("removal")
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                // Cấu hình ObjectMapper để hỗ trợ Java 8 Time và lưu thông tin type
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                // DÒNG NÀY ĐỂ LƯU THỜI GIAN DẠNG CHUỖI STRING
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                objectMapper.activateDefaultTyping(
                                objectMapper.getPolymorphicTypeValidator(),
                                ObjectMapper.DefaultTyping.NON_FINAL,
                                JsonTypeInfo.As.PROPERTY);

                // Serializer JSON cho tất cả các giá trị cache
                var jsonSerializer = RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .serializeValuesWith(jsonSerializer)
                                .disableCachingNullValues()
                                .entryTtl(Duration.ofHours(1)); // TTL mặc định 1 giờ

                // Cấu hình TTL riêng cho từng cache
                Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                                // system_configs: Không có TTL (chỉ evict khi Admin update)
                                "system_configs", defaultConfig.entryTtl(Duration.ZERO),
                                // movies: 1 giờ
                                "movies_now_showing", defaultConfig.entryTtl(Duration.ofHours(1)),
                                "movies_coming_soon", defaultConfig.entryTtl(Duration.ofHours(1)),
                                // promotions: 1 giờ
                                "promotions", defaultConfig.entryTtl(Duration.ofHours(1)),
                                // combos: 24 giờ (rất ít thay đổi)
                                "combos", defaultConfig.entryTtl(Duration.ofHours(24)));

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withInitialCacheConfigurations(cacheConfigs)
                                .transactionAware()
                                .build();
        }
}
