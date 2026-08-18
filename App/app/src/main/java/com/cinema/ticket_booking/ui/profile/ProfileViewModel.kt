package com.cinema.ticket_booking.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.response.UserResponse
import com.cinema.ticket_booking.data.repository.AuthRepository
import com.cinema.ticket_booking.data.repository.UserRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _profile = MutableLiveData<Resource<UserResponse>>()
    val profile: LiveData<Resource<UserResponse>> = _profile

    fun loadProfile() {
        userRepo.getProfile().observeForever { _profile.value = it }
    }

    fun logout() {
        authRepo.logout()
    }

    fun updateNotificationSettings(marketing: Boolean, transaction: Boolean): LiveData<Resource<UserResponse>> {
        return userRepo.updateNotificationSettings(marketing, transaction)
    }
}
