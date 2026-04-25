package com.cinema.ticket_booking.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cinema.ticket_booking.data.model.request.ReviewRequest;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.util.Resource;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class ReviewRepository {
    private final ApiService apiService;

    @Inject
    public ReviewRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<Resource<PageResponse<ReviewResponse>>> getReviews(String movieId, int page, int size) {
        return getReviews(movieId, page, size, null);
    }

    public LiveData<Resource<PageResponse<ReviewResponse>>> getReviews(String movieId, int page, int size, Integer rating) {
        MutableLiveData<Resource<PageResponse<ReviewResponse>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        apiService.getReviews(movieId, page, size, rating).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<PageResponse<ReviewResponse>>> call,
                    Response<ApiResponse<PageResponse<ReviewResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    result.setValue(Resource.success(response.body().getData()));
                else
                    result.setValue(Resource.error("Tải đánh giá thất bại"));
            }

            @Override
            public void onFailure(Call<ApiResponse<PageResponse<ReviewResponse>>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<ReviewResponse>> createReview(ReviewRequest request) {
        MutableLiveData<Resource<ReviewResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        apiService.createReview(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<ReviewResponse>> call,
                    Response<ApiResponse<ReviewResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    result.setValue(Resource.success(response.body().getData()));
                else
                    result.setValue(Resource.error(response.body() != null
                            ? response.body().getMessage()
                            : "Gửi đánh giá thất bại"));
            }

            @Override
            public void onFailure(Call<ApiResponse<ReviewResponse>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }
}

