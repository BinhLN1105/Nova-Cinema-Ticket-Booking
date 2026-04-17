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
}

