package com.cinema.ticket_booking.ui.notification;

import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cinema.ticket_booking.data.model.response.NotificationResponse;
import com.cinema.ticket_booking.databinding.ItemNotificationBinding;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {
    private final List<NotificationResponse> items;

    public NotificationAdapter(List<NotificationResponse> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(ItemNotificationBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        h.bind(items.get(pos));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void removeItem(int pos) {
        items.remove(pos);
        notifyItemRemoved(pos);
    }

    public NotificationResponse getItem(int pos) {
        return items.get(pos);
    }

    class VH extends RecyclerView.ViewHolder {
        final ItemNotificationBinding b;

        VH(ItemNotificationBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(NotificationResponse n) {
            b.tvTitle.setText(n.getTitle());
            b.tvBody.setText(n.getBody());
            b.tvTime.setText(formatDate(n.getSentAt()));
            b.getRoot().setAlpha(n.isRead() ? 0.6f : 1f);
        }

        private String formatDate(String rawDate) {
            if (rawDate == null)
                return "";
            try {
                String cleanDate = rawDate;
                if (rawDate.contains(".")) {
                    cleanDate = rawDate.substring(0, rawDate.indexOf("."));
                }

                SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = sdfIn.parse(cleanDate);

                SimpleDateFormat sdfOut = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return sdfOut.format(date);
            } catch (Exception e) {
                return rawDate;
            }
        }
    }
}
