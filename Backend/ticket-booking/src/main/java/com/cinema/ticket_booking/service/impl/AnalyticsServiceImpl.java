package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.AnalyticsResponse;
import com.cinema.ticket_booking.dto.response.AnalyticsResponse.*;
import com.cinema.ticket_booking.enums.BookingStatus;
import com.cinema.ticket_booking.enums.SeatStatus;
import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.model.Cinema;
import com.cinema.ticket_booking.model.ShowtimeSeat;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.CinemaRepository;
import com.cinema.ticket_booking.repository.ShowtimeSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements com.cinema.ticket_booking.service.AnalyticsService {

    private final BookingRepository bookingRepository;
    private final CinemaRepository cinemaRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;

    @Override
    public AnalyticsResponse getAnalytics() {
        return AnalyticsResponse.builder()
                .occupancyByCinema(calculateOccupancy())
                .peakHours(calculatePeakHours())
                .revenueByCinema(calculateRevenueByCinema())
                .build();
    }

    private List<OccupancyData> calculateOccupancy() {
        List<Cinema> cinemas = cinemaRepository.findAll();
        List<OccupancyData> result = new ArrayList<>();

        for (Cinema cinema : cinemas) {
            // Count all showtime seats for this cinema's screens
            List<ShowtimeSeat> allSeats = showtimeSeatRepository.findAll().stream()
                    .filter(ss -> ss.getShowtime().getScreen().getCinema().getId().equals(cinema.getId()))
                    .toList();

            long totalSeats = allSeats.size();
            long bookedSeats = allSeats.stream()
                    .filter(ss -> ss.getStatus() == SeatStatus.BOOKED)
                    .count();

            double rate = totalSeats > 0
                    ? BigDecimal.valueOf(bookedSeats * 100.0 / totalSeats).setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            result.add(new OccupancyData(cinema.getName(), totalSeats, bookedSeats, rate));
        }
        return result;
    }

    private List<PeakHourData> calculatePeakHours() {
        List<Booking> paidBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID)
                .toList();

        Map<Integer, Long> hourCounts = paidBookings.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getShowtime().getStartTime().getHour(),
                        Collectors.counting()
                ));

        List<PeakHourData> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            result.add(new PeakHourData(h, hourCounts.getOrDefault(h, 0L)));
        }
        return result;
    }

    private List<RevenueByCinema> calculateRevenueByCinema() {
        List<Booking> paidBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID)
                .toList();

        Map<String, List<Booking>> grouped = paidBookings.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getShowtime().getScreen().getCinema().getName()
                ));

        return grouped.entrySet().stream()
                .map(e -> {
                    BigDecimal revenue = e.getValue().stream()
                            .map(Booking::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new RevenueByCinema(e.getKey(), revenue, e.getValue().size());
                })
                .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
                .toList();
    }
}
