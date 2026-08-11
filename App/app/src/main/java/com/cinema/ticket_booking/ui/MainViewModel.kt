package com.cinema.ticket_booking.ui

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
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val userProfile = MutableLiveData<Resource<UserResponse>?>()
    /** Tab cần mở khi CheckInHistoryFragment được hiển thị từ Dashboard (Staff) */
    private val pendingHistoryFilter = MutableLiveData<String?>()
    /** Tab cần mở khi BookingHistoryFragment được hiển thị từ Profile (Customer) */
    private val pendingBookingTab = MutableLiveData<String?>()

    fun logout() {
        authRepository.logout()
        userProfile.value = null
    }

    fun getUserProfile(): LiveData<Resource<UserResponse>?> = userProfile

    fun loadUserProfile() {
        // Only load if not already loading or success to avoid redundant calls
        if (userProfile.value != null && userProfile.value?.isSuccess == true) {
            return
        }

        userRepository.getProfile().observeForever { resource ->
            userProfile.value = resource
        }
    }

    fun refreshUserProfile() {
        userRepository.getProfile().observeForever { resource ->
            userProfile.value = resource
        }
    }

    fun updateNotificationSettings(marketing: Boolean, transaction: Boolean): LiveData<Resource<UserResponse>> {
        return userRepository.updateNotificationSettings(marketing, transaction)
    }

    /** Gọi từ Dashboard khi muốn mở History ở tab cụ thể */
    fun requestHistoryTab(filter: String?) {
        pendingHistoryFilter.value = filter
    }

    /** Gọi từ CheckInHistoryFragment sau khi đã đọc filter để reset */
    fun consumeHistoryFilter() {
        pendingHistoryFilter.value = null
    }

    fun getPendingHistoryFilter(): LiveData<String?> = pendingHistoryFilter

    /** Điều hướng từ Profile sang tab con của BookingHistory */
    fun requestBookingTab(tab: String?) {
        pendingBookingTab.value = tab
    }

    fun consumeBookingTab() {
        pendingBookingTab.value = null
    }

    fun getPendingBookingTab(): LiveData<String?> = pendingBookingTab
}
