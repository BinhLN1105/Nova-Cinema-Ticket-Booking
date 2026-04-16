package com.cinema.ticket_booking.data.repository;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.util.Resource;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ComboRepository {
    private final ApiService api;

    @Inject
    public ComboRepository(ApiService api) {
        this.api = api;
    }

    public LiveData<Resource<List<ComboResponse>>> getCombos() {
        MutableLiveData<Resource<List<ComboResponse>>> r = new MutableLiveData<>();
        r.setValue(Resource.loading());
        api.getCombos().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ComboResponse>>> c,
                    Response<ApiResponse<List<ComboResponse>>> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess())
                    r.setValue(Resource.success(res.body().getData()));
                else
                    r.setValue(Resource.error("Tải combo thất bại"));
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ComboResponse>>> c, Throwable t) {
                r.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return r;
    }
}

