package com.cinema.ticket_booking.ui.wallet;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cinema.ticket_booking.data.model.response.GiftCardResponse;
import com.cinema.ticket_booking.data.model.response.UserResponse;
import com.cinema.ticket_booking.data.repository.AuthRepository;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.data.model.response.ApiResponse;
import com.cinema.ticket_booking.util.Resource;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class WalletViewModel extends ViewModel {

    private final ApiService apiService;
    private final MutableLiveData<Resource<UserResponse>> profile = new MutableLiveData<>();
    private final MutableLiveData<Resource<GiftCardResponse>> redeemResult = new MutableLiveData<>();

    @Inject
    public WalletViewModel(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<Resource<UserResponse>> getProfile() {
        return profile;
    }

    public LiveData<Resource<GiftCardResponse>> getRedeemResult() {
        return redeemResult;
    }

    public void loadProfile() {
        apiService.getMyProfile().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    profile.setValue(Resource.success(response.body().getData()));
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
            }
        });
    }

    public void redeemGiftCard(String code) {
        redeemResult.setValue(Resource.loading());
        Map<String, String> body = new HashMap<>();
        body.put("code", code);
        apiService.redeemGiftCard(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<GiftCardResponse>> call,
                    Response<ApiResponse<GiftCardResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    redeemResult.setValue(Resource.success(response.body().getData()));
                else
                    redeemResult.setValue(Resource.error(response.body() != null
                            ? response.body().getMessage()
                            : "Mã thẻ không hợp lệ"));
            }

            @Override
            public void onFailure(Call<ApiResponse<GiftCardResponse>> call, Throwable t) {
                redeemResult.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
    }
}
