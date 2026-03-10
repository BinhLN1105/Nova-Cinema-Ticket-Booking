package com.cinema.ticket_booking.data.repository;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.util.Resource;
import retrofit2.Call; import retrofit2.Callback; import retrofit2.Response;
import java.util.List;
import javax.inject.Inject; import javax.inject.Singleton;

@Singleton
public class ShowtimeRepository {
    private final ApiService api;
    @Inject public ShowtimeRepository(ApiService api) { this.api = api; }

    public LiveData<Resource<List<ShowtimeResponse>>> getShowtimes(String movieId, String cinemaId, String date) {
        MutableLiveData<Resource<List<ShowtimeResponse>>> r = new MutableLiveData<>();
        r.setValue(Resource.loading());
        api.getShowtimes(movieId, cinemaId, date).enqueue(new Callback<>() {
            @Override public void onResponse(Call<ApiResponse<List<ShowtimeResponse>>> c, Response<ApiResponse<List<ShowtimeResponse>>> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess())
                    r.setValue(Resource.success(res.body().getData()));
                else r.setValue(Resource.error("Tải suất chiếu thất bại"));
            }
            @Override public void onFailure(Call<ApiResponse<List<ShowtimeResponse>>> c, Throwable t) {
                r.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return r;
    }

    public LiveData<Resource<SeatMapResponse>> getSeatMap(String showtimeId) {
        MutableLiveData<Resource<SeatMapResponse>> r = new MutableLiveData<>();
        r.setValue(Resource.loading());
        api.getSeatMap(showtimeId).enqueue(new Callback<>() {
            @Override public void onResponse(Call<ApiResponse<SeatMapResponse>> c, Response<ApiResponse<SeatMapResponse>> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess())
                    r.setValue(Resource.success(res.body().getData()));
                else r.setValue(Resource.error("Tải sơ đồ ghế thất bại"));
            }
            @Override public void onFailure(Call<ApiResponse<SeatMapResponse>> c, Throwable t) {
                r.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return r;
    }
}
