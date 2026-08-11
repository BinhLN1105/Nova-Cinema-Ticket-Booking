package com.cinema.ticket_booking.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cinema.ticket_booking.data.model.request.NotificationSettingsRequest
import com.cinema.ticket_booking.data.model.response.*
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.util.Resource
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Singleton
class UserRepository @Inject constructor(
    private val api: ApiService
) {

    fun getMyProfile(): LiveData<Resource<UserResponse>> {
        val r = MutableLiveData<Resource<UserResponse>>()
        r.value = Resource.loading()
        api.getMyProfile().enqueue(object : Callback<ApiResponse<UserResponse>> {
            override fun onResponse(c: Call<ApiResponse<UserResponse>>, res: Response<ApiResponse<UserResponse>>) {
                if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                    r.value = Resource.success(res.body()!!.data)
                } else {
                    r.value = Resource.error("Tải profile thất bại")
                }
            }

            override fun onFailure(c: Call<ApiResponse<UserResponse>>, t: Throwable) {
                r.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return r
    }

    // Alias for backward compatibility
    fun getProfile(): LiveData<Resource<UserResponse>> {
        return getMyProfile()
    }

    fun updateNotificationSettings(marketing: Boolean?, transaction: Boolean?): LiveData<Resource<UserResponse>> {
        val r = MutableLiveData<Resource<UserResponse>>()
        r.value = Resource.loading()
        val req = NotificationSettingsRequest(marketing, transaction)

        api.updateNotificationSettings(req).enqueue(object : Callback<ApiResponse<UserResponse>> {
            override fun onResponse(c: Call<ApiResponse<UserResponse>>, res: Response<ApiResponse<UserResponse>>) {
                if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                    r.value = Resource.success(res.body()!!.data)
                } else {
                    r.value = Resource.error("Lỗi cập nhật cài đặt")
                }
            }

            override fun onFailure(c: Call<ApiResponse<UserResponse>>, t: Throwable) {
                r.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return r
    }

    fun redeemGiftCard(code: String): LiveData<Resource<GiftCardResponse>> {
        val r = MutableLiveData<Resource<GiftCardResponse>>()
        r.value = Resource.loading()
        val body = HashMap<String, String>().apply {
            put("code", code)
        }
        api.redeemGiftCard(body).enqueue(object : Callback<ApiResponse<GiftCardResponse>> {
            override fun onResponse(c: Call<ApiResponse<GiftCardResponse>>, res: Response<ApiResponse<GiftCardResponse>>) {
                if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                    r.value = Resource.success(res.body()!!.data)
                } else {
                    r.value = Resource.error(
                        if (res.body() != null) res.body()!!.message else "Mã thẻ không hợp lệ"
                    )
                }
            }

            override fun onFailure(c: Call<ApiResponse<GiftCardResponse>>, t: Throwable) {
                r.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return r
    }
}
