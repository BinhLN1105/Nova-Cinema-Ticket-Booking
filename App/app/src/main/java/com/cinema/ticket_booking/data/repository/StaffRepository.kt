package com.cinema.ticket_booking.data.repository

import com.cinema.ticket_booking.data.model.response.ApiResponse
import com.cinema.ticket_booking.data.model.response.CheckInHistoryItemResponse
import com.cinema.ticket_booking.data.model.response.PageResponse
import com.cinema.ticket_booking.data.model.response.StaffDashboardStatsResponse
import com.cinema.ticket_booking.data.model.response.UpcomingShowtimeResponse
import com.cinema.ticket_booking.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Call

@Singleton
class StaffRepository @Inject constructor(
    private val apiService: ApiService
) {

    fun getDashboardStats(): Call<ApiResponse<StaffDashboardStatsResponse>> {
        return apiService.getStaffDashboardStats()
    }

    fun getUpcomingShowtimes(): Call<ApiResponse<List<UpcomingShowtimeResponse>>> {
        return apiService.getUpcomingShowtimes()
    }

    fun getCheckInHistory(filter: String, page: Int, size: Int): Call<ApiResponse<PageResponse<CheckInHistoryItemResponse>>> {
        return apiService.getCheckInHistory(filter, page, size)
    }
}
