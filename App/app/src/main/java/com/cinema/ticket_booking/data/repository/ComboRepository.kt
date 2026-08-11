package com.cinema.ticket_booking.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cinema.ticket_booking.data.model.response.ApiResponse
import com.cinema.ticket_booking.data.model.response.ComboResponse
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.util.Resource
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Singleton
class ComboRepository @Inject constructor(
    private val api: ApiService
) {

    fun getCombos(): LiveData<Resource<List<ComboResponse>>> {
        val r = MutableLiveData<Resource<List<ComboResponse>>>()
        r.value = Resource.loading()
        api.getCombos().enqueue(object : Callback<ApiResponse<List<ComboResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<ComboResponse>>>,
                res: Response<ApiResponse<List<ComboResponse>>>
            ) {
                if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                    r.value = Resource.success(res.body()!!.data)
                } else {
                    r.value = Resource.error("Tải combo thất bại")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<ComboResponse>>>, t: Throwable) {
                r.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return r
    }
}
