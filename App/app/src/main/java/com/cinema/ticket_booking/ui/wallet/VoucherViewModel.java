package com.cinema.ticket_booking.ui.wallet;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cinema.ticket_booking.data.model.request.ClaimVoucherRequest;
import com.cinema.ticket_booking.data.model.response.ApiResponse;
import com.cinema.ticket_booking.data.model.response.VoucherSummary;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.util.Resource;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class VoucherViewModel extends ViewModel {

    private final ApiService apiService;
    private final MutableLiveData<Resource<List<VoucherSummary>>> vouchers = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> claimResult = new MutableLiveData<>();

    @Inject
    public VoucherViewModel(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<Resource<List<VoucherSummary>>> getVouchers() {
        return vouchers;
    }

    public LiveData<Resource<Void>> getClaimResult() {
        return claimResult;
    }

    public void loadVouchers() {
        vouchers.setValue(Resource.loading());
        apiService.getMyVouchers().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<VoucherSummary>>> call,
                    Response<ApiResponse<List<VoucherSummary>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    vouchers.setValue(Resource.success(response.body().getData()));
                else
                    vouchers.setValue(Resource.error("Không tải được danh sách voucher"));
            }

            @Override
            public void onFailure(Call<ApiResponse<List<VoucherSummary>>> call, Throwable t) {
                vouchers.setValue(Resource.error("Lỗi kết nối"));
            }
        });
    }

    public void claimVoucher(String code) {
        claimResult.setValue(Resource.loading());
        apiService.claimVoucher(new ClaimVoucherRequest(code)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    claimResult.setValue(Resource.success(null));
                    loadVouchers(); // Reload list
                } else {
                    String errorMsg = "Mã không hợp lệ";
                    try {
                        // Attempt to parse specific error from API
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(errorBody).getAsJsonObject();
                            if (obj.has("message")) errorMsg = obj.get("message").getAsString();
                        }
                    } catch (Exception ignored) {}
                    claimResult.setValue(Resource.error(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                claimResult.setValue(Resource.error("Lỗi kết nối mạng"));
            }
        });
    }

    public void clearClaimResult() {
        claimResult.setValue(null);
    }
}

