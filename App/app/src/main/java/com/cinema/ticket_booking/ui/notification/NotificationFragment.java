package com.cinema.ticket_booking.ui.notification;

import android.os.Bundle;
import com.cinema.ticket_booking.util.SnackbarHelper;
import com.cinema.ticket_booking.databinding.FragmentNotificationBinding;
import com.cinema.ticket_booking.ui.notification.NotificationAdapter;
import com.google.android.material.snackbar.Snackbar;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.*;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NotificationFragment extends Fragment {

    private FragmentNotificationBinding binding;
    private NotificationViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentNotificationBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        binding.btnMarkAllRead.setOnClickListener(v -> {
            viewModel.markAllAsRead();
            SnackbarHelper.showSuccess(binding.getRoot(), "Đã đánh dấu tất cả là đã đọc");
        });

        viewModel.getNotifications().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data == null || resource.data.getContent() == null
                            || resource.data.getContent().isEmpty()) {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvEmpty.setVisibility(View.GONE);
                        NotificationAdapter adapter = new NotificationAdapter(resource.data.getContent());
                        binding.rvNotifications.setAdapter(adapter);
                        setupSwipeToDelete(adapter);
                    }
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    SnackbarHelper.showError(binding.getRoot(), resource.message);
                }
            }
        });
    }

    private void setupSwipeToDelete(NotificationAdapter adapter) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                    @NonNull RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                var item = adapter.getItem(pos);

                // Tạm thời xóa khỏi UI
                adapter.removeItem(pos);

                // Hiện Snackbar với nút Undo
                Snackbar snackbar = Snackbar.make(
                        binding.getRoot(), "Đã xóa thông báo", Snackbar.LENGTH_LONG);

                boolean[] undoPressed = { false };
                snackbar.setAction("Hoàn tác", v -> {
                    undoPressed[0] = true;
                    // Tải lại dữ liệu (đơn giản nhất) hoặc insert lại adapter
                    viewModel.getNotifications().observe(getViewLifecycleOwner(), r -> {
                        if (r.status == com.cinema.ticket_booking.util.Resource.Status.SUCCESS) {
                            binding.rvNotifications.setAdapter(new NotificationAdapter(r.data.getContent()));
                        }
                    });
                });

                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        if (!undoPressed[0]) {
                            // Nếu không Undo thì mới gọi API xóa thật
                            viewModel.deleteNotification(item.getId().toString());
                        }
                    }
                });

                snackbar.show();
            }
        }).attachToRecyclerView(binding.rvNotifications);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
