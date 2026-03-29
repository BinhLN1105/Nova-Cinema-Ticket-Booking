package com.cinema.ticket_booking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Cấu hình linh hoạt: Chỉ lưu type info khi thực sự cần thiết
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(), 
            ObjectMapper.DefaultTyping.EVERYTHING, 
            com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );
        
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues()
                .entryTtl(Duration.ofHours(1));

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                "system_configs", defaultConfig.entryTtl(Duration.ZERO),
                "movies_now_showing", defaultConfig.entryTtl(Duration.ofHours(1)),
                "movies_coming_soon", defaultConfig.entryTtl(Duration.ofHours(1)),
                "promotions", defaultConfig.entryTtl(Duration.ofHours(1)),
                "combos", defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }
}
