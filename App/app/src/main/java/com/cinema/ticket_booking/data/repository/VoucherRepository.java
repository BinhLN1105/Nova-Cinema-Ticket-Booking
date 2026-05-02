package com.cinema.ticket_booking.data.repository;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.request.ClaimVoucherRequest;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.util.Resource;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;

@Singleton
public class VoucherRepository {
    private final ApiService api;

    @Inject
    public VoucherRepository(ApiService api) {
        this.api = api;
    }

    public LiveData<Resource<VoucherSummary>> validateVoucher(String code) {
        MutableLiveData<Resource<VoucherSummary>> r = new MutableLiveData<>();
        r.setValue(Resource.loading());
        api.validateVoucher(code).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<VoucherSummary>> c, Response<ApiResponse<VoucherSummary>> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess())
                    r.setValue(Resource.success(res.body().getData()));
                else
                    r.setValue(Resource.error(res.body() != null ? res.body().getMessage() : "Voucher không hợp lệ"));
            }

            @Override
            public void onFailure(Call<ApiResponse<VoucherSummary>> c, Throwable t) {
                r.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return r;
    }

    public LiveData<Resource<List<VoucherSyncResponse>>> getActiveVouchers() {
        MutableLiveData<Resource<List<VoucherSyncResponse>>> r = new MutableLiveData<>();
        r.setValue(Resource.loading());
        api.getActiveVouchers().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<VoucherSyncResponse>>> c,
                    Response<ApiResponse<List<VoucherSyncResponse>>> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess())
                    r.setValue(Resource.success(res.body().getData()));
                else
                    r.setValue(Resource.error(
                            res.body() != null ? res.body().getMessage() : "Không lấy được danh sách khuyến mãi"));
            }

            @Override
            public void onFailure(Call<ApiResponse<List<VoucherSyncResponse>>> c, Throwable t) {
                r.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return r;
    }

    public LiveData<Resource<List<VoucherSummary>>> getMyVouchers() {
        MutableLiveData<Resource<List<VoucherSummary>>> r = new MutableLiveData<>();
        r.setValue(Resource.loading());
        api.getMyVouchers().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<VoucherSummary>>> c,
                    Response<ApiResponse<List<VoucherSummary>>> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess())
                    r.setValue(Resource.success(res.body().getData()));
                else
                    r.setValue(Resource.error(
                            res.body() != null ? res.body().getMessage() : "Không lấy được danh sách khuyến mãi"));
            }

            @Override
            public void onFailure(Call<ApiResponse<List<VoucherSummary>>> c, Throwable t) {
                r.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return r;
    }

    public LiveData<Resource<Void>> claimVoucher(String code) {
        MutableLiveData<Resource<Void>> r = new MutableLiveData<>();
        r.setValue(Resource.loading());
        api.claimVoucher(new ClaimVoucherRequest(code)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> res) {
                if (res.isSuccessful()) {
                    r.setValue(Resource.success(null));
                } else {
                    String errorMsg = "Mã không hợp lệ";
                    try {
                        if (res.errorBody() != null) {
                            String body = res.errorBody().string();
                            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                            if (obj.has("message"))
                                errorMsg = obj.get("message").getAsString();
                        }
                    } catch (Exception ignored) {
                    }
                    r.setValue(Resource.error(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {
                r.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return r;
    }
}
