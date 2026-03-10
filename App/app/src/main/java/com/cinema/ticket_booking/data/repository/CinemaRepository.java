package com.cinema.ticket_booking.data.repository;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.util.Resource;
import retrofit2.Call; import retrofit2.Callback; import retrofit2.Response;
import java.util.List;
import javax.inject.Inject; import javax.inject.Singleton;

@Singleton
public class CinemaRepository {
    private final ApiService api;
    @Inject public CinemaRepository(ApiService api) { this.api = api; }

    public LiveData<Resource<List<CinemaResponse>>> getCinemas(String city) {
        MutableLiveData<Resource<List<CinemaResponse>>> r = new MutableLiveData<>();
        r.setValue(Resource.loading());
        api.getCinemas(city).enqueue(new Callback<>() {
            @Override public void onResponse(Call<ApiResponse<List<CinemaResponse>>> c, Response<ApiResponse<List<CinemaResponse>>> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess())
                    r.setValue(Resource.success(res.body().getData()));
                else r.setValue(Resource.error("Tải rạp thất bại"));
            }
            @Override public void onFailure(Call<ApiResponse<List<CinemaResponse>>> c, Throwable t) {
                r.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return r;
    }
}
