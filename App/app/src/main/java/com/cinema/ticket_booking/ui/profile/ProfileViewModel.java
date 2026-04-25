package com.cinema.ticket_booking.ui.profile;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.UserResponse;
import com.cinema.ticket_booking.data.repository.AuthRepository;
import com.cinema.ticket_booking.data.repository.UserRepository;
import com.cinema.ticket_booking.util.Resource;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class ProfileViewModel extends ViewModel {
    private final UserRepository userRepo;
    private final AuthRepository authRepo;
    private final MutableLiveData<Resource<UserResponse>> profile = new MutableLiveData<>();

    @Inject
    public ProfileViewModel(UserRepository userRepo, AuthRepository authRepo) {
        this.userRepo = userRepo;
        this.authRepo = authRepo;
    }

    public LiveData<Resource<UserResponse>> getProfile() {
        return profile;
    }

    public void loadProfile() {
        userRepo.getProfile().observeForever(profile::setValue);
    }

    public void logout() {
        authRepo.logout();
    }

    public LiveData<Resource<UserResponse>> updateNotificationSettings(Boolean marketing, Boolean transaction) {
        return userRepo.updateNotificationSettings(marketing, transaction);
    }
}

