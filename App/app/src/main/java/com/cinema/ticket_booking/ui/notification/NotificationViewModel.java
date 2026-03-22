package com.cinema.ticket_booking.ui.notification;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.data.repository.NotificationRepository;
import com.cinema.ticket_booking.util.Resource;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class NotificationViewModel extends ViewModel {
    private final NotificationRepository repo;
    private final MutableLiveData<Resource<PageResponse<NotificationResponse>>> notifications = new MutableLiveData<>();

    @Inject
    public NotificationViewModel(NotificationRepository repo) {
        this.repo = repo;
        load();
    }

    public LiveData<Resource<PageResponse<NotificationResponse>>> getNotifications() {
        return notifications;
    }

    public void markAllAsRead() {
        repo.markAllAsRead();
    }

    private void load() {
        repo.getNotifications(0).observeForever(notifications::setValue);
    }
}
