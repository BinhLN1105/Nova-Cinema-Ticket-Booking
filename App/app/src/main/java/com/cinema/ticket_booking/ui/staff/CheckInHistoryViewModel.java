package com.cinema.ticket_booking.ui.staff;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cinema.ticket_booking.data.model.response.ApiResponse;
import com.cinema.ticket_booking.data.model.response.CheckInHistoryItemResponse;
import com.cinema.ticket_booking.data.model.response.PageResponse;
import com.cinema.ticket_booking.data.repository.StaffRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class CheckInHistoryViewModel extends ViewModel {

    private final StaffRepository staffRepository;

    // Tab "Hôm nay"
    private final MutableLiveData<List<CheckInHistoryItemResponse>> _todayItems = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<CheckInHistoryItemResponse>> todayItems = _todayItems;

    // Tab "Tháng này"
    private final MutableLiveData<List<CheckInHistoryItemResponse>> _monthItems = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<CheckInHistoryItemResponse>> monthItems = _monthItems;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    @Inject
    public CheckInHistoryViewModel(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
        // Load cả 2 tab ngay khi khởi tạo
        loadToday();
        loadThisMonth();
    }

    public void loadToday() {
        _isLoading.setValue(true);
        staffRepository.getCheckInHistory("TODAY", 0, 50)
                .enqueue(new Callback<ApiResponse<PageResponse<CheckInHistoryItemResponse>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<PageResponse<CheckInHistoryItemResponse>>> call,
                                           Response<ApiResponse<PageResponse<CheckInHistoryItemResponse>>> response) {
                        _isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            PageResponse<CheckInHistoryItemResponse> page = response.body().getData();
                            if (page != null && page.getContent() != null) {
                                _todayItems.setValue(page.getContent());
                            } else {
                                _todayItems.setValue(new ArrayList<>());
                            }
                        } else {
                            _error.setValue("Không thể tải lịch sử hôm nay");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<PageResponse<CheckInHistoryItemResponse>>> call, Throwable t) {
                        _isLoading.setValue(false);
                        _error.setValue("Lỗi kết nối: " + t.getMessage());
                    }
                });
    }

    public void loadThisMonth() {
        staffRepository.getCheckInHistory("THIS_MONTH", 0, 100)
                .enqueue(new Callback<ApiResponse<PageResponse<CheckInHistoryItemResponse>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<PageResponse<CheckInHistoryItemResponse>>> call,
                                           Response<ApiResponse<PageResponse<CheckInHistoryItemResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            PageResponse<CheckInHistoryItemResponse> page = response.body().getData();
                            if (page != null && page.getContent() != null) {
                                _monthItems.setValue(page.getContent());
                            } else {
                                _monthItems.setValue(new ArrayList<>());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<PageResponse<CheckInHistoryItemResponse>>> call, Throwable t) {
                        // silent fail - today tab sẽ show error nếu có
                    }
                });
    }

    public void refresh() {
        _error.setValue(null);
        _todayItems.setValue(new ArrayList<>());
        _monthItems.setValue(new ArrayList<>());
        loadToday();
        loadThisMonth();
    }
}
