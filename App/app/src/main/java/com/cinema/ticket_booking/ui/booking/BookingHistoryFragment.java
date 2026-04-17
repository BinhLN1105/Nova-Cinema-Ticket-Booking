package com.cinema.ticket_booking.ui.booking;

import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import com.cinema.ticket_booking.util.SnackbarHelper;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.*;
import android.content.res.ColorStateList;
import androidx.annotation.NonNull;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentBookingHistoryBinding;
import com.cinema.ticket_booking.data.local.TokenManager;
import com.cinema.ticket_booking.data.model.response.BookingSummary;
import com.cinema.ticket_booking.data.model.response.ApiResponse;
import com.cinema.ticket_booking.network.ApiService;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.Map;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BookingHistoryFragment extends Fragment {

    private FragmentBookingHistoryBinding binding;
    private BookingHistoryViewModel viewModel;
    private java.util.List<BookingSummary> allBookings = new java.util.ArrayList<>();
    private boolean isUpcomingTab = true;

    @Inject
    TokenManager tokenManager;
    
    @Inject
    ApiService apiService;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentBookingHistoryBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // If not logged in, show login prompt BEFORE creating ViewModel
        if (!tokenManager.isLoggedIn()) {
            binding.rvBookings.setVisibility(View.GONE);
            binding.tvEmpty.setVisibility(View.GONE);
            binding.layoutLoginPrompt.setVisibility(View.VISIBLE);
            binding.swipeRefresh.setEnabled(false);
            binding.btnLoginPrompt
                    .setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.loginFragment));
            return;
        }

        // Only create ViewModel (which auto-loads data) AFTER login check
        viewModel = new ViewModelProvider(this).get(BookingHistoryViewModel.class);

        // Handle redirection from Profile
        if (getArguments() != null && "HISTORY".equals(getArguments().getString("MapsToTab"))) {
            viewModel.setUpcomingTab(false);
        }

        // Nova: Restore tab state from ViewModel
        this.isUpcomingTab = viewModel.isUpcomingTab();
        updateTabStyles();

        binding.rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));

        binding.tabUpcoming.setOnClickListener(v -> {
            if (isUpcomingTab)
                return;
            isUpcomingTab = true;
            viewModel.setUpcomingTab(true); // Persist
            updateTabStyles();
            updateList();
        });

        binding.tabHistory.setOnClickListener(v -> {
            if (!isUpcomingTab)
                return;
            isUpcomingTab = false;
            viewModel.setUpcomingTab(false); // Persist
            updateTabStyles();
            updateList();
        });

        binding.nestedScrollView.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY == (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight())) {
                viewModel.loadMore();
            }
        });

        viewModel.getBookings().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> {
                    if (viewModel.isLoadingMore()) {
                        binding.progressBarLoadMore.setVisibility(View.VISIBLE);
                    } else {
                        binding.progressBar.setVisibility(View.VISIBLE);
                    }
                }
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.progressBarLoadMore.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    allBookings.clear();
                    if (resource.data != null && resource.data.getContent() != null) {
                        allBookings.addAll(resource.data.getContent());
                    }
                    updateList();
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.progressBarLoadMore.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    SnackbarHelper.showError(binding.getRoot(), resource.message);
                }
            }
        });

        binding.btnBrowseMovies.setOnClickListener(v -> {
            // Navigate back to Home fragment which is the start destination
            Navigation.findNavController(view).popBackStack(R.id.homeFragment, false);
        });

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
    }

    private void updateTabStyles() {
        int colorPrimary = getResources().getColor(R.color.primary, null);
        int colorOnSurfaceVariant = getResources().getColor(R.color.on_surface_variant, null);
        ColorStateList bgActive = ColorStateList
                .valueOf(getResources().getColor(R.color.surface_container_highest, null));
        ColorStateList bgInactive = ColorStateList.valueOf(getResources().getColor(android.R.color.transparent, null));

        binding.tabUpcoming.setTextColor(isUpcomingTab ? colorPrimary : colorOnSurfaceVariant);
        binding.tabUpcoming.setBackgroundTintList(isUpcomingTab ? bgActive : bgInactive);

        binding.tabHistory.setTextColor(!isUpcomingTab ? colorPrimary : colorOnSurfaceVariant);
        binding.tabHistory.setBackgroundTintList(!isUpcomingTab ? bgActive : bgInactive);
    }

    private void updateList() {
        List<BookingSummary> filtered = new ArrayList<>();
        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        for (BookingSummary b : allBookings) {
            boolean isFuture = true;
            try {
                if (b.getStartTime() != null) {
                    Date d;
                    try {
                        d = sdf.parse(b.getStartTime());
                    } catch (Exception e) {
                        d = sdf2.parse(b.getStartTime());
                    }
                    if (d != null)
                        isFuture = d.getTime() > now;
                }
            } catch (Exception e) {
            }

            // Kiểm tra vé PENDING đã quá hạn thanh toán chưa
            boolean isPendingExpired = false;
            if ("PENDING".equalsIgnoreCase(b.getStatus()) && b.getExpiresAt() != null) {
                try {
                    Date expDate;
                    try {
                        expDate = sdf.parse(b.getExpiresAt());
                    } catch (Exception e2) {
                        expDate = sdf2.parse(b.getExpiresAt());
                    }
                    if (expDate != null && expDate.getTime() <= now) {
                        isPendingExpired = true;
                    }
                } catch (Exception ignored) {}
            }

            // Vé PENDING còn hạn → tab Sắp chiếu (để user thanh toán)
            // Vé PENDING quá hạn → tab Lịch sử
            // Vé CANCELLED/PAID/CHECKED_IN với giờ chiếu tương lai → tab Sắp chiếu
            boolean isUpcoming;
            if ("PENDING".equalsIgnoreCase(b.getStatus())) {
                isUpcoming = !isPendingExpired;
            } else {
                isUpcoming = ("PAID".equalsIgnoreCase(b.getStatus()) || 
                              "CHECKED_IN".equalsIgnoreCase(b.getStatus()) ||
                              "CANCELLED".equalsIgnoreCase(b.getStatus())) && isFuture;
            }

            if (isUpcomingTab == isUpcoming) {
                filtered.add(b);
            }
        }

        if (filtered.isEmpty()) {
            binding.tvEmpty.setVisibility(View.VISIBLE);
            binding.rvBookings.setVisibility(View.GONE);
        } else {
            binding.tvEmpty.setVisibility(View.GONE);
            binding.rvBookings.setVisibility(View.VISIBLE);
            binding.rvBookings.setAdapter(new BookingHistoryAdapter(
                    filtered, new BookingHistoryAdapter.Listener() {
                        @Override
                        public void onClick(String bookingId) {
                            Bundle args = new Bundle();
                            args.putString("bookingId", bookingId);
                            if (getView() != null) {
                                Navigation.findNavController(getView())
                                        .navigate(R.id.action_history_to_bookingDetail, args);
                            }
                        }

                        @Override
                        public void onPayClick(String bookingId) {
                            Bundle args = new Bundle();
                            args.putString("bookingId", bookingId);
                            if (getView() != null) {
                                Navigation.findNavController(getView())
                                        .navigate(R.id.action_history_to_payment, args);
                            }
                        }

                        @Override
                        public void onReviewClick(BookingSummary s) {
                            Bundle args = new Bundle();
                            args.putString("movieId", s.getMovieId());
                            args.putString("movieTitle", s.getMovieTitle());
                            args.putString("bookingId", s.getId());
                            if (getView() != null) {
                                Navigation.findNavController(getView())
                                        .navigate(R.id.action_history_to_writeReview, args);
                            }
                        }

                        @Override
                        public void onCancelClick(BookingSummary s) {
                            showCancelConfirmDialog(s);
                        }
                    }));
        }
    }

    private void showCancelConfirmDialog(BookingSummary s) {
        binding.progressBar.setVisibility(View.VISIBLE);
        // Lấy policy huỷ (phần trăm hoàn) từ API
        apiService.getCancelPolicy()
                .enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> call,
                            @NonNull Response<ApiResponse<Map<String, Object>>> response) {
                        if (!isAdded())
                            return;
                        binding.progressBar.setVisibility(View.GONE);

                        int refundPercent = 100; // default
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            try {
                                Object percentObj = response.body().getData().get("refundPercent");
                                if (percentObj instanceof Number) {
                                    refundPercent = ((Number) percentObj).intValue();
                                } else if (percentObj instanceof String) {
                                    refundPercent = Integer.parseInt((String) percentObj);
                                }
                            } catch (Exception ignored) {
                            }
                        }

                        // Hiển thị dialog
                        String totalFormatted = String.format("%,.0f", s.getTotalAmount());

                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Hủy vé xem phim")
                                .setMessage("Bạn chắc chắn muốn hủy vé \"" + s.getMovieTitle() + "\"?\n\n" +
                                        "Hệ thống sẽ hoàn lại " + refundPercent + "% giá trị vé (" + totalFormatted
                                        + "đ) " +
                                        "thành điểm CP (Cinema Points) vào tài khoản của bạn theo quy định.\n\n" +
                                        "⚠️ Thao tác này không thể hoàn tác.")
                                .setNegativeButton("Hủy bỏ", null)
                                .setPositiveButton("Đồng ý hủy vé", (dialog, which) -> {
                                    executeCancelBooking(s.getId());
                                })
                                .show();
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call,
                            @NonNull Throwable t) {
                        if (!isAdded())
                            return;
                        binding.progressBar.setVisibility(View.GONE);
                        SnackbarHelper.showError(binding.getRoot(), "Không thể tải chính sách hoàn vé.");
                    }
                });
    }

    private void executeCancelBooking(String bookingId) {
        binding.progressBar.setVisibility(View.VISIBLE);

        apiService.cancelBooking(bookingId)
                .enqueue(new retrofit2.Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                            @NonNull Response<ApiResponse<Void>> response) {
                        if (!isAdded())
                            return;
                        binding.progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful()) {
                            SnackbarHelper.showSuccess(binding.getRoot(),
                                    "Hủy vé thành công! Điểm CP đã được cộng vào tài khoản.");
                            // Cập nhật trạng thái ngay tại chỗ để user thấy "ĐÃ HỦY" liền
                            for (BookingSummary b : allBookings) {
                                if (bookingId.equals(b.getId())) {
                                    b.setStatus("CANCELLED");
                                    break;
                                }
                            }
                            updateList();
                            // Sau đó refresh ngầm từ server để đồng bộ
                            viewModel.refresh();
                        } else {
                            String msg = "Hủy vé thất bại";
                            try {
                                if (response.errorBody() != null) {
                                    String body = response.errorBody().string();
                                    if (body.contains("message")) {
                                        msg = body.substring(body.indexOf("message") + 10);
                                        msg = msg.substring(0, msg.indexOf("\""));
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                            SnackbarHelper.showError(binding.getRoot(), msg);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call,
                            @NonNull Throwable t) {
                        if (!isAdded())
                            return;
                        binding.progressBar.setVisibility(View.GONE);
                        SnackbarHelper.showError(binding.getRoot(), "Lỗi kết nối: " + t.getMessage());
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Mỗi khi quay lại tab Tickets, tự động refresh danh sách vé mới nhất
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

