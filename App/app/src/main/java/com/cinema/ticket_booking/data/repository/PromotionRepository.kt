package com.cinema.ticket_booking.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cinema.ticket_booking.data.model.response.ApiResponse
import com.cinema.ticket_booking.data.model.response.PromotionResponse
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.util.Resource
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Singleton
class PromotionRepository @Inject constructor(
    private val api: ApiService
) {

    fun getActivePromotions(): LiveData<Resource<List<PromotionResponse>>> {
        val r = MutableLiveData<Resource<List<PromotionResponse>>>()
        r.value = Resource.loading()
        api.getActivePromotions().enqueue(object : Callback<ApiResponse<List<PromotionResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<PromotionResponse>>>,
                res: Response<ApiResponse<List<PromotionResponse>>>
            ) {
                if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                    r.value = Resource.success(res.body()!!.data)
                } else {
                    r.value = Resource.error(
                        if (res.body() != null) res.body()!!.message else "Không lấy được danh sách banner khuyến mãi"
                    )
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<PromotionResponse>>>, t: Throwable) {
                r.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return r
    }

    fun getPopupPromotion(): LiveData<Resource<PromotionResponse>> {
        val r = MutableLiveData<Resource<PromotionResponse>>()
        r.value = Resource.loading()
        api.getPopupPromotion().enqueue(object : Callback<ApiResponse<PromotionResponse>> {
            override fun onResponse(c: Call<ApiResponse<PromotionResponse>>, res: Response<ApiResponse<PromotionResponse>>) {
                if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                    r.value = Resource.success(res.body()!!.data)
                } else {
                    r.value = Resource.error("Không lấy được popup khuyến mãi")
                }
            }

            override fun onFailure(c: Call<ApiResponse<PromotionResponse>>, t: Throwable) {
                r.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return r
    }
}
