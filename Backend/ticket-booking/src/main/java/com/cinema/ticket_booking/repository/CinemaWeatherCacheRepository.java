package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.CinemaWeatherCache;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CinemaWeatherCacheRepository extends JpaRepository<CinemaWeatherCache, UUID> {
}
