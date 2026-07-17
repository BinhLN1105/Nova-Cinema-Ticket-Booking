package com.cinema.ticket_booking.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cinema.ticket_booking.data.model.response.*
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.util.Resource
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Singleton
class MovieRepository @Inject constructor(
    private val apiService: ApiService
) {

    fun getMovies(status: String, page: Int, size: Int): LiveData<Resource<PageResponse<MovieSummary>>> {
        val result = MutableLiveData<Resource<PageResponse<MovieSummary>>>()
        result.value = Resource.loading()
        apiService.getMovies(status, page, size).enqueue(object : Callback<ApiResponse<PageResponse<MovieSummary>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<MovieSummary>>>,
                response: Response<ApiResponse<PageResponse<MovieSummary>>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(response.body()!!.data)
                } else {
                    result.value = Resource.error("Tải phim thất bại")
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<MovieSummary>>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    fun getMovieDetail(id: String): LiveData<Resource<MovieDetail>> {
        val result = MutableLiveData<Resource<MovieDetail>>()
        result.value = Resource.loading()
        apiService.getMovieDetail(id).enqueue(object : Callback<ApiResponse<MovieDetail>> {
            override fun onResponse(call: Call<ApiResponse<MovieDetail>>, response: Response<ApiResponse<MovieDetail>>) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(response.body()!!.data)
                } else {
                    result.value = Resource.error("Không tìm thấy phim")
                }
            }

            override fun onFailure(call: Call<ApiResponse<MovieDetail>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    fun canReview(id: String): LiveData<Resource<CanReviewResponse>> {
        val result = MutableLiveData<Resource<CanReviewResponse>>()
        result.value = Resource.loading()
        apiService.canReview(id).enqueue(object : Callback<ApiResponse<CanReviewResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<CanReviewResponse>>,
                response: Response<ApiResponse<CanReviewResponse>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(response.body()!!.data)
                } else {
                    result.value = Resource.error(
                        if (response.body() != null) response.body()!!.message else "Lỗi kiểm tra quyền đánh giá"
                    )
                }
            }

            override fun onFailure(call: Call<ApiResponse<CanReviewResponse>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    fun searchMovies(query: String, page: Int, size: Int): LiveData<Resource<PageResponse<MovieSummary>>> {
        val result = MutableLiveData<Resource<PageResponse<MovieSummary>>>()
        result.value = Resource.loading()
        apiService.searchMovies(query, page, size).enqueue(object : Callback<ApiResponse<PageResponse<MovieSummary>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<MovieSummary>>>,
                response: Response<ApiResponse<PageResponse<MovieSummary>>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(response.body()!!.data)
                } else {
                    result.value = Resource.error("Tìm kiếm thất bại")
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<MovieSummary>>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    fun getGenres(): LiveData<Resource<List<Genre>>> {
        val result = MutableLiveData<Resource<List<Genre>>>()
        result.value = Resource.loading()
        apiService.getAllGenres().enqueue(object : Callback<ApiResponse<List<Genre>>> {
            override fun onResponse(call: Call<ApiResponse<List<Genre>>>, response: Response<ApiResponse<List<Genre>>>) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(response.body()!!.data)
                } else {
                    result.value = Resource.error("Tải thể loại thất bại")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Genre>>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    fun getFeaturedMovies(): LiveData<Resource<List<MovieSummary>>> {
        val result = MutableLiveData<Resource<List<MovieSummary>>>()
        result.value = Resource.loading()
        apiService.getFeaturedMovies().enqueue(object : Callback<ApiResponse<List<MovieSummary>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<MovieSummary>>>,
                response: Response<ApiResponse<List<MovieSummary>>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(response.body()!!.data)
                } else {
                    result.value = Resource.error("Tải phim nổi bật thất bại")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<MovieSummary>>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }
}
