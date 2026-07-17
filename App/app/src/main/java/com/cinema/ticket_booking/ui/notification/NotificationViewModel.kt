package com.cinema.ticket_booking.ui.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.response.NotificationResponse
import com.cinema.ticket_booking.data.model.response.PageResponse
import com.cinema.ticket_booking.data.repository.NotificationRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repo: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableLiveData<Resource<PageResponse<NotificationResponse>>>()
    val notifications: LiveData<Resource<PageResponse<NotificationResponse>>> = _notifications

    init {
        load()
    }

    fun markAllAsRead() {
        repo.markAllAsRead()
    }

    fun deleteNotification(id: String) {
        repo.deleteNotification(id)
    }

    fun refresh() {
        load()
    }

    private fun load() {
        repo.getNotifications(0).observeForever { _notifications.value = it }
    }
}
