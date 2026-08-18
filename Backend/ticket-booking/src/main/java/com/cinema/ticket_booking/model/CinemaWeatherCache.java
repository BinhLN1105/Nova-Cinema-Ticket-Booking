package com.cinema.ticket_booking.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cinema_weather_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CinemaWeatherCache {

    @Id
    @Column(name = "cinema_id")
    private UUID cinemaId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "last_weather_data", columnDefinition = "TEXT")
    private String lastWeatherData;

    @Column(name = "last_fetched_at")
    private LocalDateTime lastFetchedAt;

    @Column(name = "provider", length = 50)
    private String provider;
}
