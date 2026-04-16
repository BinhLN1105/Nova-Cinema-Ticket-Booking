package com.cinema.ticket_booking.ui.wallet;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cinema.ticket_booking.data.model.response.ApiResponse;
import com.cinema.ticket_booking.data.model.response.PageResponse;
import com.cinema.ticket_booking.data.model.response.VoucherSummary;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.util.Resource;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class VoucherViewModel extends ViewModel {

    private final ApiService apiService;
    private final MutableLiveData<Resource<PageResponse<VoucherSummary>>> vouchers = new MutableLiveData<>();

    @Inject
    public VoucherViewModel(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<Resource<PageResponse<VoucherSummary>>> getVouchers() {
        return vouchers;
    }

    public void loadVouchers() {
        vouchers.setValue(Resource.loading());
        apiService.getMyVouchers(0, 50).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<PageResponse<VoucherSummary>>> call,
                    Response<ApiResponse<PageResponse<VoucherSummary>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    vouchers.setValue(Resource.success(response.body().getData()));
                else
                    vouchers.setValue(Resource.error("Không tải được danh sách voucher"));
            }

            @Override
            public void onFailure(Call<ApiResponse<PageResponse<VoucherSummary>>> call, Throwable t) {
                vouchers.setValue(Resource.error("Lỗi kết nối"));
            }
        });
    }
}

