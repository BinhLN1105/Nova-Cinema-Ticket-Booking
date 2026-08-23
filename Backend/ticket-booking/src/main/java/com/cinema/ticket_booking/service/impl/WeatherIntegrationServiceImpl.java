package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.WeatherShowtimeResponse;
import com.cinema.ticket_booking.model.Cinema;
import com.cinema.ticket_booking.model.CinemaWeatherCache;
import com.cinema.ticket_booking.model.Showtime;
import com.cinema.ticket_booking.repository.CinemaWeatherCacheRepository;
import com.cinema.ticket_booking.repository.ShowtimeRepository;
import com.cinema.ticket_booking.service.WeatherIntegrationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.UUID;

import com.cinema.ticket_booking.repository.CinemaRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WeatherIntegrationServiceImpl implements WeatherIntegrationService {

    private final CinemaWeatherCacheRepository weatherCacheRepository;
    private final ShowtimeRepository showtimeRepository;
    private final CinemaRepository cinemaRepository;
    private final ObjectMapper objectMapper;
    private RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${app.weather.api-key:mock_key}")
    private String apiKey;

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    @Transactional
    public WeatherShowtimeResponse getWeatherForCinema(UUID cinemaId) {

        Cinema cinema = cinemaRepository.findById(cinemaId).orElse(null);
        if (cinema == null) {
            return createEmptyResponse(true, "Không tìm thấy thông tin rạp chiếu.");
        }

        return fetchAndProcessWeather(cinema, LocalDateTime.now(ZONE_VN));
    }

    @Override
    @Transactional
    public WeatherShowtimeResponse getWeatherForShowtime(UUID showtimeId) {

        Showtime showtime = showtimeRepository.findById(showtimeId).orElse(null);
        if (showtime == null) {
            return createEmptyResponse(true, "Không tìm thấy thông tin suất chiếu.");
        }

        if (showtime.getScreen() == null || showtime.getScreen().getCinema() == null) {
            return createEmptyResponse(true, "Không tìm thấy thông tin rạp chiếu.");
        }

        Cinema cinema = showtime.getScreen().getCinema();
        return fetchAndProcessWeather(cinema, showtime.getStartTime());
    }

    private WeatherShowtimeResponse fetchAndProcessWeather(Cinema cinema, LocalDateTime targetTime) {
        UUID cinemaId = cinema.getId();
        CinemaWeatherCache cache = weatherCacheRepository.findById(cinemaId).orElse(null);

        if (cache == null || cache.getLatitude() == null || cache.getLongitude() == null) {
            return createEmptyResponse(true, "Rạp hiện chưa được cấu hình tọa độ thời tiết.");
        }

        if ("mock_key".equalsIgnoreCase(apiKey) || apiKey.contains("mock")) {
            return buildMockResponse(targetTime, cinema.getName());
        }

        String jsonWeatherData = resolveWeatherData(cinema, cache);
        if (jsonWeatherData != null) {
            try {
                return parseWeatherFromJson(jsonWeatherData, targetTime, cinema.getName());
            } catch (Exception e) {
                log.error(" Lỗi parse JSON thời tiết rạp '{}': {}", cinema.getName(), e.getMessage());
            }
        }

        return createEmptyResponse(false, "Có lỗi xảy ra khi xử lý dữ liệu thời tiết.");
    }

    private String resolveWeatherData(Cinema cinema, CinemaWeatherCache cache) {
        boolean needUpdate = cache.getLastWeatherData() == null || cache.getLastFetchedAt() == null ||
                cache.getLastFetchedAt().plusHours(4).isBefore(LocalDateTime.now(ZONE_VN));

        if (!needUpdate) {
            return cache.getLastWeatherData();
        }

        try {

            String url = String.format(
                    "http://api.weatherapi.com/v1/forecast.json?key=%s&q=%f,%f&days=3&aqi=no&alerts=no",
                    apiKey, cache.getLatitude(), cache.getLongitude());

            String rawResponse = restTemplate.getForObject(url, String.class);
            if (rawResponse != null && !rawResponse.isBlank()) {
                cache.setLastWeatherData(rawResponse);
                cache.setLastFetchedAt(LocalDateTime.now(ZONE_VN));
                cache.setProvider("WeatherAPI");
                weatherCacheRepository.save(cache);
                return rawResponse;
            }
        } catch (Exception e) {
            log.error(" Lỗi kết nối Weather API rạp '{}': {}. Sử dụng stale cache...",
                    cinema.getName(), e.getMessage());
        }

        return cache.getLastWeatherData();
    }

    private WeatherShowtimeResponse parseWeatherFromJson(String json, LocalDateTime startTime, String cinemaName)
            throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode forecastNode = root.path("forecast").path("forecastday");

        if (forecastNode.isMissingNode() || !forecastNode.isArray()) {
            return createEmptyResponse(false, "Dữ liệu dự báo của API bị sai định dạng.");
        }

        String targetDateStr = startTime.toLocalDate().toString(); // YYYY-MM-DD
        int targetHour = startTime.getHour(); // 0..23

        JsonNode targetDayNode = null;
        for (JsonNode dayNode : forecastNode) {
            if (targetDateStr.equals(dayNode.path("date").asText())) {
                targetDayNode = dayNode;
                break;
            }
        }

        // Suất chiếu nằm ngoài range 3 ngày của API free-tier
        if (targetDayNode == null) {
            WeatherShowtimeResponse response = createEmptyResponse(false, "");
            response.setOutOfForecastRange(true);
            response.setWarningMessage(String.format(
                    "Dạ, suất chiếu ngày %s tại rạp %s còn khá xa nên hiện tại em chưa có dự báo thời tiết chính xác. Gần ngày chiếu anh/chị xem lại giúp em nhé! 🌦️",
                    startTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), cinemaName));
            return response;
        }

        JsonNode hourArray = targetDayNode.path("hour");
        JsonNode targetHourNode = null;

        // So khớp mốc giờ chẵn gần nhất (Ví dụ suất chiếu 20h30 -> so khớp với giờ mốc
        // 20h)
        for (JsonNode hNode : hourArray) {
            String timeStr = hNode.path("time").asText(); // YYYY-MM-DD HH:00
            String expectedTimePart = String.format(" %02d:00", targetHour);
            if (timeStr.contains(expectedTimePart)) {
                targetHourNode = hNode;
                break;
            }
        }

        // Fallback lấy giờ đầu tiên nếu không khớp chính xác
        if (targetHourNode == null && hourArray.size() > 0) {
            targetHourNode = hourArray.get(0);
        }

        if (targetHourNode != null) {
            double temp = targetHourNode.path("temp_c").asDouble();
            String conditionText = targetHourNode.path("condition").path("text").asText();

            // Tự động nhận diện thời tiết xấu ở tầng Java
            String condLower = conditionText.toLowerCase();
            boolean isBad = condLower.contains("mưa") || condLower.contains("rain") ||
                    condLower.contains("bão") || condLower.contains("storm") ||
                    condLower.contains("dông") || condLower.contains("thunder") ||
                    condLower.contains("shower") || condLower.contains("tuyết") ||
                    condLower.contains("snow") || condLower.contains("lốc");

            String timeFormat = startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            String warning;
            if (isBad) {
                warning = String.format(
                        "Dự báo thời tiết lúc %s tại rạp %s sẽ có %s (%.1f°C) 🌧️. Anh/chị nên mang theo áo mưa hoặc đi sớm chút để tránh trơn trượt/kẹt xe nhé!",
                        timeFormat, cinemaName, conditionText, temp);
            } else {
                warning = String.format(
                        "Dự báo thời tiết lúc %s tại rạp %s thuận lợi, %s (%.1f°C) ☀️. Anh/chị có thể an tâm di chuyển và tận hưởng buổi xem phim!",
                        timeFormat, cinemaName, conditionText, temp);
            }

            return WeatherShowtimeResponse.builder()
                    .condition(conditionText)
                    .temperature(temp)
                    .isBadWeather(isBad)
                    .outOfForecastRange(false)
                    .warningMessage(warning)
                    .build();
        }

        return createEmptyResponse(false, "Không tìm thấy dữ liệu mốc giờ chiếu.");
    }

    private WeatherShowtimeResponse buildMockResponse(LocalDateTime startTime, String cinemaName) {
        // Thiết lập dữ liệu mock ổn định phục vụ unit test nhanh
        String timeFormat = startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        boolean isBad = startTime.getHour() >= 19; // Giả lập tối sau 19h thời tiết xấu có mưa to để test warning

        String condition = isBad ? "Mưa giông lớn" : "Trời quang mây tạnh";
        double temp = isBad ? 25.5 : 30.0;
        String warning = isBad
                ? String.format(
                        "Dự báo thời tiết lúc %s tại rạp %s sẽ có %s (%.1f°C) 🌧️. Anh/chị nên mang theo áo mưa hoặc đi sớm chút để tránh trơn trượt/kẹt xe nhé!",
                        timeFormat, cinemaName, condition, temp)
                : String.format(
                        "Dự báo thời tiết lúc %s tại rạp %s thuận lợi, %s (%.1f°C) ☀️. Anh/chị có thể an tâm di chuyển và tận hưởng buổi xem phim!",
                        timeFormat, cinemaName, condition, temp);

        return WeatherShowtimeResponse.builder()
                .condition(condition)
                .temperature(temp)
                .isBadWeather(isBad)
                .outOfForecastRange(false)
                .warningMessage(warning)
                .build();
    }

    private WeatherShowtimeResponse createEmptyResponse(boolean outOfRange, String fallbackMsg) {
        return WeatherShowtimeResponse.builder()
                .condition(null)
                .temperature(null)
                .isBadWeather(false)
                .outOfForecastRange(outOfRange)
                .warningMessage(fallbackMsg.isEmpty()
                        ? "Hiện chưa lấy được thông tin thời tiết, anh/chị kiểm tra trước khi đi nhé."
                        : fallbackMsg)
                .build();
    }
}
