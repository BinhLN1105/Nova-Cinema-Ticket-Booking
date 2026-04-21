package com.cinema.ticket_booking.ui.home;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cinema.ticket_booking.data.model.response.ApiResponse;
import com.cinema.ticket_booking.data.model.response.StaffDashboardStatsResponse;
import com.cinema.ticket_booking.data.model.response.UpcomingShowtimeResponse;
import com.cinema.ticket_booking.data.repository.StaffRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class StaffHomeViewModel extends ViewModel {

    private static final long POLL_INTERVAL_MS = 30_000L; // 30 giây

    private final StaffRepository staffRepository;
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<StaffDashboardStatsResponse> _stats = new MutableLiveData<>();
    public final LiveData<StaffDashboardStatsResponse> stats = _stats;

    private final MutableLiveData<List<UpcomingShowtimeResponse>> _upcomingShowtimes = new MutableLiveData<>();
    public final LiveData<List<UpcomingShowtimeResponse>> upcomingShowtimes = _upcomingShowtimes;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(true);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            silentRefreshStats(); // Refresh nhẹ không show loading
            pollingHandler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Inject
    public StaffHomeViewModel(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
        loadStats();       // Load lần đầu (có loading indicator)
        loadUpcoming();    // Load suất chiếu sắp tới
        startPolling();    // Bắt đầu polling 30 giây
    }

    /** Load lần đầu — có hiện loading indicator */
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

    /** Refresh thầm lặng — không show loading, badge tự nhảy số */
    private void silentRefreshStats() {
        staffRepository.getDashboardStats().enqueue(new Callback<ApiResponse<StaffDashboardStatsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StaffDashboardStatsResponse>> call,
                                   Response<ApiResponse<StaffDashboardStatsResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    _stats.setValue(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StaffDashboardStatsResponse>> call, Throwable t) {
                // Silent fail — không hiện lỗi khi polling
            }
        });
    }

    /** Load danh sách suất chiếu sắp bắt đầu */
    public void loadUpcoming() {
        staffRepository.getUpcomingShowtimes().enqueue(new Callback<ApiResponse<List<UpcomingShowtimeResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<UpcomingShowtimeResponse>>> call,
                                   Response<ApiResponse<List<UpcomingShowtimeResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    _upcomingShowtimes.setValue(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<UpcomingShowtimeResponse>>> call, Throwable t) {
                // Silent fail
            }
        });
    }

    private void startPolling() {
        pollingHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    /** Gọi khi user bấm nút Làm mới */
    public void refresh() {
        loadStats();
        loadUpcoming();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        pollingHandler.removeCallbacks(pollRunnable); // Dừng polling khi ViewModel bị destroy
    }
}
