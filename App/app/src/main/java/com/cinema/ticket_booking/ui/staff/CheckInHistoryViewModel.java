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

    private final MutableLiveData<List<CheckInHistoryItemResponse>> _items = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<CheckInHistoryItemResponse>> items = _items;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private int currentPage = 0;
    private boolean isLastPage = false;
    private boolean isFetching = false;

    @Inject
    public CheckInHistoryViewModel(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
        loadNextPage();
    }

    public void loadNextPage() {
        if (isFetching || isLastPage) return;
        isFetching = true;
        _isLoading.setValue(true);

        staffRepository.getCheckInHistory(currentPage, 20)
                .enqueue(new Callback<ApiResponse<PageResponse<CheckInHistoryItemResponse>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<PageResponse<CheckInHistoryItemResponse>>> call,
                                           Response<ApiResponse<PageResponse<CheckInHistoryItemResponse>>> response) {
                        _isLoading.setValue(false);
                        isFetching = false;
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            PageResponse<CheckInHistoryItemResponse> page = response.body().getData();
                            if (page != null) {
                                List<CheckInHistoryItemResponse> current = new ArrayList<>(
                                        _items.getValue() != null ? _items.getValue() : new ArrayList<>());
                                if (page.getContent() != null) {
                                    current.addAll(page.getContent());
                                }
                                _items.setValue(current);
                                isLastPage = page.isLast();
                                currentPage++;
                            }
                        } else {
                            _error.setValue("Không thể tải lịch sử soát vé");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<PageResponse<CheckInHistoryItemResponse>>> call, Throwable t) {
                        _isLoading.setValue(false);
                        isFetching = false;
                        _error.setValue("Lỗi kết nối: " + t.getMessage());
                    }
                });
    }

    public void refresh() {
        currentPage = 0;
        isLastPage = false;
        isFetching = false;
        _items.setValue(new ArrayList<>());
        loadNextPage();
    }
}
