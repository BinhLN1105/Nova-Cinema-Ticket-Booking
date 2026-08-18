package com.cinema.ticket_booking.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cinema.ticket_booking.data.model.response.ApiResponse
import com.cinema.ticket_booking.data.model.response.CinemaResponse
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.util.Resource
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Singleton
class CinemaRepository @Inject constructor(
    private val api: ApiService
) {

    fun getCinemas(city: String?): LiveData<Resource<List<CinemaResponse>>> {
        val r = MutableLiveData<Resource<List<CinemaResponse>>>()
        r.value = Resource.loading()
        api.getCinemas(city).enqueue(object : Callback<ApiResponse<List<CinemaResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<CinemaResponse>>>,
                res: Response<ApiResponse<List<CinemaResponse>>>
            ) {
                if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                    r.value = Resource.success(res.body()!!.data)
                } else {
                    r.value = Resource.error("Tải rạp thất bại")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<CinemaResponse>>>, t: Throwable) {
                r.value = Resource.error("Mất kết nối mạng, vui lòng kiểm tra lại Internet.")
            }
        })
        return r
    }
}
