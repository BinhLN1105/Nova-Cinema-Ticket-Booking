package com.cinema.ticket_booking.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cinema.ticket_booking.data.model.response.ApiResponse;
import com.cinema.ticket_booking.data.model.response.StaffDashboardStatsResponse;
import com.cinema.ticket_booking.data.repository.StaffRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class StaffHomeViewModel extends ViewModel {

    private final StaffRepository staffRepository;

    private final MutableLiveData<StaffDashboardStatsResponse> _stats = new MutableLiveData<>();
    public final LiveData<StaffDashboardStatsResponse> stats = _stats;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(true);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    @Inject
    public StaffHomeViewModel(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
        loadStats();
    }

    public void loadStats() {
        _isLoading.setValue(true);
        staffRepository.getDashboardStats().enqueue(new Callback<ApiResponse<StaffDashboardStatsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StaffDashboardStatsResponse>> call,
                                   Response<ApiResponse<StaffDashboardStatsResponse>> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    _stats.setValue(response.body().getData());
                } else {
                    _error.setValue("Không thể tải thống kê");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StaffDashboardStatsResponse>> call, Throwable t) {
                _isLoading.setValue(false);
                _error.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }
}
