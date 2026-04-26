package com.cinema.ticket_booking.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cinema.ticket_booking.data.model.response.UserResponse;
import com.cinema.ticket_booking.data.repository.AuthRepository;
import com.cinema.ticket_booking.data.repository.UserRepository;
import com.cinema.ticket_booking.util.Resource;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MainViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final MutableLiveData<Resource<UserResponse>> userProfile = new MutableLiveData<>();
    /** Tab cần mở khi CheckInHistoryFragment được hiển thị từ Dashboard (Staff) */
    private final MutableLiveData<String> pendingHistoryFilter = new MutableLiveData<>();
    /** Tab cần mở khi BookingHistoryFragment được hiển thị từ Profile (Customer) */
    private final MutableLiveData<String> pendingBookingTab = new MutableLiveData<>();

    @Inject
    public MainViewModel(UserRepository userRepository, AuthRepository authRepository) {
        this.userRepository = userRepository;
        this.authRepository = authRepository;
    }

    public void logout() {
        authRepository.logout();
        userProfile.setValue(null);
    }

    public LiveData<Resource<UserResponse>> getUserProfile() {
        return userProfile;
    }

    public void loadUserProfile() {
        // Only load if not already loading or success to avoid redundant calls
        if (userProfile.getValue() != null && userProfile.getValue().status == Resource.Status.SUCCESS) {
            return;
        }
        
        userRepository.getProfile().observeForever(resource -> {
            userProfile.setValue(resource);
        });
    }

    public void refreshUserProfile() {
        userRepository.getProfile().observeForever(resource -> {
            userProfile.setValue(resource);
        });
    }

    public LiveData<Resource<UserResponse>> updateNotificationSettings(boolean marketing, boolean transaction) {
        return userRepository.updateNotificationSettings(marketing, transaction);
    }

    /** Gọi từ Dashboard khi muốn mở History ở tab cụ thể */
    public void requestHistoryTab(String filter) {
        pendingHistoryFilter.setValue(filter);
    }

    /** Gọi từ CheckInHistoryFragment sau khi đã đọc filter để reset */
    public void consumeHistoryFilter() {
        pendingHistoryFilter.setValue(null);
    }

    public LiveData<String> getPendingHistoryFilter() {
        return pendingHistoryFilter;
    }

    /** Điều hướng từ Profile sang tab con của BookingHistory */
    public void requestBookingTab(String tab) {
        pendingBookingTab.setValue(tab);
    }

    public void consumeBookingTab() {
        pendingBookingTab.setValue(null);
    }

    public LiveData<String> getPendingBookingTab() {
        return pendingBookingTab;
    }
}

