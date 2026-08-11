package com.cinema.ticket_booking.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cinema.ticket_booking.data.model.response.ApiResponse
import com.cinema.ticket_booking.data.model.response.NotificationResponse
import com.cinema.ticket_booking.data.model.response.PageResponse
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.util.Resource
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Singleton
class NotificationRepository @Inject constructor(
    private val api: ApiService
) {

    fun getNotifications(page: Int): LiveData<Resource<PageResponse<NotificationResponse>>> {
        val r = MutableLiveData<Resource<PageResponse<NotificationResponse>>>()
        r.value = Resource.loading()
        api.getNotifications(page, 20).enqueue(object : Callback<ApiResponse<PageResponse<NotificationResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<NotificationResponse>>>,
                res: Response<ApiResponse<PageResponse<NotificationResponse>>>
            ) {
                if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                    r.value = Resource.success(res.body()!!.data)
                } else {
                    r.value = Resource.error("Tải thông báo thất bại")
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<NotificationResponse>>>, t: Throwable) {
                r.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return r
    }

    fun markAllAsRead() {
        api.markAllAsRead().enqueue(object : Callback<ApiResponse<Void>> {
            override fun onResponse(c: Call<ApiResponse<Void>>, r: Response<ApiResponse<Void>>) {}
            override fun onFailure(c: Call<ApiResponse<Void>>, t: Throwable) {}
        })
    }

    fun deleteNotification(id: String) {
        api.deleteNotification(id).enqueue(object : Callback<ApiResponse<Void>> {
            override fun onResponse(c: Call<ApiResponse<Void>>, r: Response<ApiResponse<Void>>) {}
            override fun onFailure(c: Call<ApiResponse<Void>>, t: Throwable) {}
        })
    }
}
