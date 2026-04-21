package com.cinema.ticket_booking.data.repository;

import com.cinema.ticket_booking.data.model.response.ApiResponse;
import com.cinema.ticket_booking.data.model.response.CheckInHistoryItemResponse;
import com.cinema.ticket_booking.data.model.response.PageResponse;
import com.cinema.ticket_booking.data.model.response.StaffDashboardStatsResponse;
import com.cinema.ticket_booking.data.model.response.UpcomingShowtimeResponse;
import com.cinema.ticket_booking.network.ApiService;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;

@Singleton
public class StaffRepository {

    private final ApiService apiService;

    @Inject
    public StaffRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public Call<ApiResponse<StaffDashboardStatsResponse>> getDashboardStats() {
        return apiService.getStaffDashboardStats();
    }

    public Call<ApiResponse<List<UpcomingShowtimeResponse>>> getUpcomingShowtimes() {
        return apiService.getUpcomingShowtimes();
    }

    public Call<ApiResponse<PageResponse<CheckInHistoryItemResponse>>> getCheckInHistory(int page, int size) {
        return apiService.getCheckInHistory(page, size);
    }
}
