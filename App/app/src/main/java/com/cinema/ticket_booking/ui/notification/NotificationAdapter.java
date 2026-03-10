package com.cinema.ticket_booking.ui.notification;

import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cinema.ticket_booking.data.model.response.NotificationResponse;
import com.cinema.ticket_booking.databinding.ItemNotificationBinding;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {
    private final List<NotificationResponse> items;
    public NotificationAdapter(List<NotificationResponse> items) { this.items = items; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(ItemNotificationBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) { h.bind(items.get(pos)); }
    @Override public int getItemCount() { return items.size(); }

    class VH extends RecyclerView.ViewHolder {
        final ItemNotificationBinding b;
        VH(ItemNotificationBinding b) { super(b.getRoot()); this.b = b; }
        void bind(NotificationResponse n) {
            b.tvTitle.setText(n.getTitle());
            b.tvBody.setText(n.getBody());
            b.tvTime.setText(n.getSentAt());
            b.getRoot().setAlpha(n.isRead() ? 0.6f : 1f);
        }
    }
}
