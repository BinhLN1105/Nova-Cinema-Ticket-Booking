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

@Singleton
public class NotificationRepository {
    private final ApiService api;

    @Inject
    public NotificationRepository(ApiService api) {
        this.api = api;
    }

    public LiveData<Resource<PageResponse<NotificationResponse>>> getNotifications(int page) {
        MutableLiveData<Resource<PageResponse<NotificationResponse>>> r = new MutableLiveData<>();
        r.setValue(Resource.loading());
        api.getNotifications(page, 20).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<PageResponse<NotificationResponse>>> c,
                    Response<ApiResponse<PageResponse<NotificationResponse>>> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess())
                    r.setValue(Resource.success(res.body().getData()));
                else
                    r.setValue(Resource.error("Tải thông báo thất bại"));
            }

            @Override
            public void onFailure(Call<ApiResponse<PageResponse<NotificationResponse>>> c, Throwable t) {
                r.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return r;
    }

    public void markAllAsRead() {
        api.markAllAsRead().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) {
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {
            }
        });
    }
}

