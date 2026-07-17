package com.cinema.ticket_booking.ui.home

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.response.ApiResponse
import com.cinema.ticket_booking.data.model.response.StaffDashboardStatsResponse
import com.cinema.ticket_booking.data.model.response.UpcomingShowtimeResponse
import com.cinema.ticket_booking.data.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class StaffHomeViewModel @Inject constructor(
    private val staffRepository: StaffRepository
) : ViewModel() {

    companion object {
        private const val POLL_INTERVAL_MS = 30_000L // 30 giây
    }

    private val pollingHandler = Handler(Looper.getMainLooper())

    private val _stats = MutableLiveData<StaffDashboardStatsResponse>()
    val stats: LiveData<StaffDashboardStatsResponse> = _stats

    private val _upcomingShowtimes = MutableLiveData<List<UpcomingShowtimeResponse>>()
    val upcomingShowtimes: LiveData<List<UpcomingShowtimeResponse>> = _upcomingShowtimes

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val pollRunnable = object : Runnable {
        override fun run() {
            silentRefreshStats() // Refresh nhẹ không show loading
            pollingHandler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    init {
        loadStats() // Load lần đầu (có loading indicator)
        loadUpcoming() // Load suất chiếu sắp tới
        startPolling() // Bắt đầu polling 30 giây
    }

    /** Load lần đầu — có hiện loading indicator */
    fun loadStats() {
        _isLoading.value = true
        staffRepository.getDashboardStats().enqueue(object : Callback<ApiResponse<StaffDashboardStatsResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<StaffDashboardStatsResponse>>,
                response: Response<ApiResponse<StaffDashboardStatsResponse>>
            ) {
                _isLoading.value = false
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    _stats.value = response.body()!!.data
                } else {
                    _error.value = "Không thể tải thống kê"
                }
            }

            override fun onFailure(call: Call<ApiResponse<StaffDashboardStatsResponse>>, t: Throwable) {
                _isLoading.value = false
                _error.value = "Lỗi kết nối: ${t.message}"
            }
        })
    }

    /** Refresh thầm lặng — không show loading, badge tự nhảy số */
    private fun silentRefreshStats() {
        staffRepository.getDashboardStats().enqueue(object : Callback<ApiResponse<StaffDashboardStatsResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<StaffDashboardStatsResponse>>,
                response: Response<ApiResponse<StaffDashboardStatsResponse>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    _stats.value = response.body()!!.data
                }
            }

            override fun onFailure(call: Call<ApiResponse<StaffDashboardStatsResponse>>, t: Throwable) {
                // Silent fail — không hiện lỗi khi polling
            }
        })
    }

    /** Load danh sách suất chiếu sắp bắt đầu */
    fun loadUpcoming() {
        staffRepository.getUpcomingShowtimes().enqueue(object : Callback<ApiResponse<List<UpcomingShowtimeResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<UpcomingShowtimeResponse>>>,
                response: Response<ApiResponse<List<UpcomingShowtimeResponse>>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    _upcomingShowtimes.value = response.body()!!.data
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<UpcomingShowtimeResponse>>>, t: Throwable) {
                // Silent fail
            }
        })
    }

    private fun startPolling() {
        pollingHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS)
    }

    /** Gọi khi user bấm nút Làm mới */
    fun refresh() {
        loadStats()
        loadUpcoming()
    }

    override fun onCleared() {
        super.onCleared()
        pollingHandler.removeCallbacks(pollRunnable) // Dừng polling khi ViewModel bị destroy
    }
}
