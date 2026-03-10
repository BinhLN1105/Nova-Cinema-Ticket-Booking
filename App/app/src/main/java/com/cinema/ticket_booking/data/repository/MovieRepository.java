package com.cinema.ticket_booking.data.repository;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.util.Resource;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class MovieRepository {

    private final ApiService apiService;

    @Inject
    public MovieRepository(ApiService apiService) { this.apiService = apiService; }

    public LiveData<Resource<PageResponse<MovieSummary>>> getMovies(String status, int page, int size) {
        MutableLiveData<Resource<PageResponse<MovieSummary>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        apiService.getMovies(status, page, size).enqueue(new Callback<>() {
            @Override public void onResponse(Call<ApiResponse<PageResponse<MovieSummary>>> call,
                                             Response<ApiResponse<PageResponse<MovieSummary>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    result.setValue(Resource.success(response.body().getData()));
                else result.setValue(Resource.error("Tải phim thất bại"));
            }
            @Override public void onFailure(Call<ApiResponse<PageResponse<MovieSummary>>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<MovieDetail>> getMovieDetail(String id) {
        MutableLiveData<Resource<MovieDetail>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        apiService.getMovieDetail(id).enqueue(new Callback<>() {
            @Override public void onResponse(Call<ApiResponse<MovieDetail>> call,
                                             Response<ApiResponse<MovieDetail>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    result.setValue(Resource.success(response.body().getData()));
                else result.setValue(Resource.error("Không tìm thấy phim"));
            }
            @Override public void onFailure(Call<ApiResponse<MovieDetail>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<PageResponse<MovieSummary>>> searchMovies(String query, int page, int size) {
        MutableLiveData<Resource<PageResponse<MovieSummary>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        apiService.searchMovies(query, page, size).enqueue(new Callback<>() {
            @Override public void onResponse(Call<ApiResponse<PageResponse<MovieSummary>>> call,
                                             Response<ApiResponse<PageResponse<MovieSummary>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    result.setValue(Resource.success(response.body().getData()));
                else result.setValue(Resource.error("Tìm kiếm thất bại"));
            }
            @Override public void onFailure(Call<ApiResponse<PageResponse<MovieSummary>>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<List<Genre>>> getGenres() {
        MutableLiveData<Resource<List<Genre>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        apiService.getAllGenres().enqueue(new Callback<>() {
            @Override public void onResponse(Call<ApiResponse<List<Genre>>> call,
                                             Response<ApiResponse<List<Genre>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    result.setValue(Resource.success(response.body().getData()));
                else result.setValue(Resource.error("Tải thể loại thất bại"));
            }
            @Override public void onFailure(Call<ApiResponse<List<Genre>>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }
}
