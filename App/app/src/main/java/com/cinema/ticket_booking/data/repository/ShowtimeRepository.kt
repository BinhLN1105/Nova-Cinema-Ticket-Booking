package com.cinema.ticket_booking.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cinema.ticket_booking.data.model.response.ApiResponse
import com.cinema.ticket_booking.data.model.response.SeatMapResponse
import com.cinema.ticket_booking.data.model.response.ShowtimeResponse
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.util.Resource
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Singleton
class ShowtimeRepository @Inject constructor(
    private val api: ApiService
) {

    fun getShowtimes(movieId: String?, cinemaId: String?, date: String?): LiveData<Resource<List<ShowtimeResponse>>> {
        val r = MutableLiveData<Resource<List<ShowtimeResponse>>>()
        r.value = Resource.loading()
        api.getShowtimes(movieId, cinemaId, date).enqueue(object : Callback<ApiResponse<List<ShowtimeResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<ShowtimeResponse>>>,
                res: Response<ApiResponse<List<ShowtimeResponse>>>
            ) {
                if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                    r.value = Resource.success(res.body()!!.data)
                } else {
                    r.value = Resource.error("Tải suất chiếu thất bại")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<ShowtimeResponse>>>, t: Throwable) {
                r.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return r
    }

    fun getSeatMap(showtimeId: String): LiveData<Resource<SeatMapResponse>> {
        val r = MutableLiveData<Resource<SeatMapResponse>>()
        r.value = Resource.loading()
        api.getSeatMap(showtimeId).enqueue(object : Callback<ApiResponse<SeatMapResponse>> {
            override fun onResponse(c: Call<ApiResponse<SeatMapResponse>>, res: Response<ApiResponse<SeatMapResponse>>) {
                if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                    r.value = Resource.success(res.body()!!.data)
                } else {
                    r.value = Resource.error("Tải sơ đồ ghế thất bại")
                }
            }

            override fun onFailure(c: Call<ApiResponse<SeatMapResponse>>, t: Throwable) {
                r.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return r
    }
}
