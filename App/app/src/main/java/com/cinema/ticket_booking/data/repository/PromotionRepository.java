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
public class PromotionRepository {
    private final ApiService api;

    @Inject
    public PromotionRepository(ApiService api) {
        this.api = api;
    }

    public LiveData<Resource<List<PromotionResponse>>> getActivePromotions() {
        MutableLiveData<Resource<List<PromotionResponse>>> r = new MutableLiveData<>();
        r.setValue(Resource.loading());
        api.getActivePromotions().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<PromotionResponse>>> c,
                    Response<ApiResponse<List<PromotionResponse>>> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess())
                    r.setValue(Resource.success(res.body().getData()));
                else
                    r.setValue(Resource.error(
                            res.body() != null ? res.body().getMessage() : "Không lấy được danh sách banner khuyến mãi"));
            }

            @Override
            public void onFailure(Call<ApiResponse<List<PromotionResponse>>> c, Throwable t) {
                r.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return r;
    }

    public LiveData<Resource<PromotionResponse>> getPopupPromotion() {
        MutableLiveData<Resource<PromotionResponse>> r = new MutableLiveData<>();
        r.setValue(Resource.loading());
        api.getPopupPromotion().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<PromotionResponse>> c,
                    Response<ApiResponse<PromotionResponse>> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess())
                    r.setValue(Resource.success(res.body().getData()));
                else
                    r.setValue(Resource.error("Không lấy được popup khuyến mãi"));
            }

            @Override
            public void onFailure(Call<ApiResponse<PromotionResponse>> c, Throwable t) {
                r.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return r;
    }
}

