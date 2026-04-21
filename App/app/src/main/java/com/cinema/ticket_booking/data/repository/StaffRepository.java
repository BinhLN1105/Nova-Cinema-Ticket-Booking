package com.cinema.ticket_booking.data.repository;

import com.cinema.ticket_booking.data.model.response.ApiResponse;
import com.cinema.ticket_booking.data.model.response.StaffDashboardStatsResponse;
import com.cinema.ticket_booking.network.ApiService;

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
}
