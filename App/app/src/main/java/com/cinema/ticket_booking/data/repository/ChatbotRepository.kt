package com.cinema.ticket_booking.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cinema.ticket_booking.data.model.request.ChatRequest
import com.cinema.ticket_booking.data.model.response.ApiResponse
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.util.Resource
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Singleton
class ChatbotRepository @Inject constructor(
    private val apiService: ApiService
) {

    fun sendMessage(msg: String): LiveData<Resource<String>> {
        val result = MutableLiveData<Resource<String>>()
        result.value = Resource.loading()

        apiService.chatWithAi(ChatRequest(msg)).enqueue(object : Callback<ApiResponse<Map<String, String>>> {
            override fun onResponse(
                call: Call<ApiResponse<Map<String, String>>>,
                response: Response<ApiResponse<Map<String, String>>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    val botReply = response.body()!!.data?.get("reply")
                    result.value = Resource.success(botReply)
                } else {
                    if (response.code() == 401) {
                        result.value = Resource.error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để sử dụng Chatbot!")
                    } else {
                        result.value = Resource.error(
                            if (response.body() != null) response.body()!!.message else "AI Chatbot không phản hồi"
                        )
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, String>>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })

        return result
    }
}
