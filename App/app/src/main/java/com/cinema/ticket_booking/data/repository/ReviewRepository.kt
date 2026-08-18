package com.cinema.ticket_booking.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cinema.ticket_booking.data.model.request.ReviewRequest
import com.cinema.ticket_booking.data.model.response.*
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.util.Resource
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Singleton
class ReviewRepository @Inject constructor(
    private val apiService: ApiService
) {

    fun getReviews(movieId: String, page: Int, size: Int): LiveData<Resource<PageResponse<ReviewResponse>>> {
        return getReviews(movieId, page, size, null)
    }

    fun getReviews(movieId: String, page: Int, size: Int, rating: Int?): LiveData<Resource<PageResponse<ReviewResponse>>> {
        val result = MutableLiveData<Resource<PageResponse<ReviewResponse>>>()
        result.value = Resource.loading()
        apiService.getReviews(movieId, page, size, rating).enqueue(object : Callback<ApiResponse<PageResponse<ReviewResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<ReviewResponse>>>,
                response: Response<ApiResponse<PageResponse<ReviewResponse>>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(response.body()!!.data)
                } else {
                    result.value = Resource.error("Tải đánh giá thất bại")
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<ReviewResponse>>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    fun createReview(request: ReviewRequest): LiveData<Resource<ReviewResponse>> {
        val result = MutableLiveData<Resource<ReviewResponse>>()
        result.value = Resource.loading()
        apiService.createReview(request).enqueue(object : Callback<ApiResponse<ReviewResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<ReviewResponse>>,
                response: Response<ApiResponse<ReviewResponse>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(response.body()!!.data)
                } else {
                    result.value = Resource.error(
                        if (response.body() != null) response.body()!!.message else "Gửi đánh giá thất bại"
                    )
                }
            }

            override fun onFailure(call: Call<ApiResponse<ReviewResponse>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    fun updateReview(id: String, request: ReviewRequest): LiveData<Resource<ReviewResponse>> {
        val result = MutableLiveData<Resource<ReviewResponse>>()
        result.value = Resource.loading()
        apiService.updateReview(id, request).enqueue(object : Callback<ApiResponse<ReviewResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<ReviewResponse>>,
                response: Response<ApiResponse<ReviewResponse>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(response.body()!!.data)
                } else {
                    result.value = Resource.error(
                        if (response.body() != null) response.body()!!.message else "Cập nhật đánh giá thất bại"
                    )
                }
            }

            override fun onFailure(call: Call<ApiResponse<ReviewResponse>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }
}
