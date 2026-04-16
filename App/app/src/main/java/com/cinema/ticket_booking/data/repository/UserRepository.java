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
public class UserRepository {
    private final ApiService api;

    @Inject
    public UserRepository(ApiService api) {
        this.api = api;
    }

    public LiveData<Resource<UserResponse>> getProfile() {
        MutableLiveData<Resource<UserResponse>> r = new MutableLiveData<>();
        r.setValue(Resource.loading());
        api.getMyProfile().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> c, Response<ApiResponse<UserResponse>> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess())
                    r.setValue(Resource.success(res.body().getData()));
                else
                    r.setValue(Resource.error("Tải profile thất bại"));
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> c, Throwable t) {
                r.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return r;
    }

    public LiveData<Resource<UserResponse>> updateNotificationSettings(Boolean marketing, Boolean transaction) {
        MutableLiveData<Resource<UserResponse>> r = new MutableLiveData<>();
        r.setValue(Resource.loading());
        com.cinema.ticket_booking.data.model.request.NotificationSettingsRequest req = 
            new com.cinema.ticket_booking.data.model.request.NotificationSettingsRequest(marketing, transaction);
            
        api.updateNotificationSettings(req).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> c, Response<ApiResponse<UserResponse>> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess())
                    r.setValue(Resource.success(res.body().getData()));
                else
                    r.setValue(Resource.error("Lỗi cập nhật cài đặt"));
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> c, Throwable t) {
                r.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return r;
    }
}

