package com.cinema.ticket_booking.ui.staff

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.response.ApiResponse
import com.cinema.ticket_booking.data.model.response.CheckInHistoryItemResponse
import com.cinema.ticket_booking.data.model.response.PageResponse
import com.cinema.ticket_booking.data.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayList
import javax.inject.Inject

@HiltViewModel
class CheckInHistoryViewModel @Inject constructor(
    private val staffRepository: StaffRepository
) : ViewModel() {

    // Tab "Hôm nay"
    private val _todayItems = MutableLiveData<List<CheckInHistoryItemResponse>>(ArrayList())
    val todayItems: LiveData<List<CheckInHistoryItemResponse>> = _todayItems

    // Tab "Tháng này"
    private val _monthItems = MutableLiveData<List<CheckInHistoryItemResponse>>(ArrayList())
    val monthItems: LiveData<List<CheckInHistoryItemResponse>> = _monthItems

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        // Load cả 2 tab ngay khi khởi tạo
        loadToday()
        loadThisMonth()
    }

    fun loadToday() {
        _isLoading.value = true
        staffRepository.getCheckInHistory("TODAY", 0, 50)
            .enqueue(object : Callback<ApiResponse<PageResponse<CheckInHistoryItemResponse>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PageResponse<CheckInHistoryItemResponse>>>,
                    response: Response<ApiResponse<PageResponse<CheckInHistoryItemResponse>>>
                ) {
                    _isLoading.value = false
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.success) {
                        val page = body.data
                        if (page?.content != null) {
                            _todayItems.value = page.content
                        } else {
                            _todayItems.value = ArrayList()
                        }
                    } else {
                        _error.value = "Không thể tải lịch sử hôm nay"
                    }
                }

                override fun onFailure(
                    call: Call<ApiResponse<PageResponse<CheckInHistoryItemResponse>>>,
                    t: Throwable
                ) {
                    _isLoading.value = false
                    _error.value = "Lỗi kết nối: " + t.message
                }
            })
    }

    fun loadThisMonth() {
        staffRepository.getCheckInHistory("THIS_MONTH", 0, 100)
            .enqueue(object : Callback<ApiResponse<PageResponse<CheckInHistoryItemResponse>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PageResponse<CheckInHistoryItemResponse>>>,
                    response: Response<ApiResponse<PageResponse<CheckInHistoryItemResponse>>>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.success) {
                        val page = body.data
                        if (page?.content != null) {
                            _monthItems.value = page.content
                        } else {
                            _monthItems.value = ArrayList()
                        }
                    }
                }

                override fun onFailure(
                    call: Call<ApiResponse<PageResponse<CheckInHistoryItemResponse>>>,
                    t: Throwable
                ) {
                    // silent fail - today tab sẽ show error nếu có
                }
            })
    }

    fun refresh() {
        _error.value = null
        _todayItems.value = ArrayList()
        _monthItems.value = ArrayList()
        loadToday()
        loadThisMonth()
    }
}
