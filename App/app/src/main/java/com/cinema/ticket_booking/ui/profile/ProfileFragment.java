package com.cinema.ticket_booking.ui.profile;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentProfileBinding;
import com.cinema.ticket_booking.util.ThemeManager;
import com.cinema.ticket_booking.data.local.TokenManager;
import com.cinema.ticket_booking.ui.MainViewModel;
import com.cinema.ticket_booking.util.SnackbarHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.messaging.FirebaseMessaging;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private MainViewModel viewModel; // Nova: Use activity-shared ViewModel

    @Inject
    TokenManager tokenManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentProfileBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Nova: Connect to the Activity-level ViewModel to share pre-fetched user data
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        updateUI(tokenManager.isLoggedIn());

        // Nova Optimization: Instant UI update if data is already in RAM
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null)
                return;

            boolean isLoggedIn = tokenManager.isLoggedIn();
            if (resource.isSuccess() && resource.data != null && isLoggedIn) {
                var user = resource.data;
                binding.tvName.setText(user.getFullName());
                binding.tvEmail.setText(user.getEmail());
                // Professional Membership Logic (Nova Algorithm - Refined for EXP/CP)
                long exp = user.getAvailableExp();
                long cp = user.getCinePoints();

                long currentMin = user.getCurrentTierMinPoints() != null ? user.getCurrentTierMinPoints() : 0;
                long nextMin = user.getNextTierMinPoints() != null ? user.getNextTierMinPoints() : 500;

                String tier = user.getRank() != null ? user.getRank() : "BRONZE";
                String nextTier = "SILVER";
                if (tier.equalsIgnoreCase("SILVER"))
                    nextTier = "GOLD";
                else if (tier.equalsIgnoreCase("GOLD"))
                    nextTier = "DIAMOND";
                else if (tier.equalsIgnoreCase("DIAMOND"))
                    nextTier = "MAX";

                long nextSub = nextMin - exp;
                if (nextSub < 0 || tier.equalsIgnoreCase("DIAMOND"))
                    nextSub = 0;

                int progress = 100;
                long totalRange = nextMin - currentMin;
                if (totalRange > 0 && !tier.equalsIgnoreCase("DIAMOND")) {
                    progress = (int) (((exp - currentMin) * 100) / totalRange);
                    if (progress < 0)
                        progress = 0;
                    if (progress > 100)
                        progress = 100;
                }

                binding.tvTierBadge.setText("⭐ " + tier.toUpperCase());
                binding.tvCurrentTier.setText("CẤP ĐỘ HIỆN TẠI: " + tier.toUpperCase());
                binding.tvCinePoints.setText(String.valueOf(cp));
                binding.tvPointsToNext
                        .setText((nextSub > 0) ? (nextSub + " EXP ĐỂ LÊN " + nextTier) : "BẠN ĐÃ ĐẠT CẤP TỐI ĐA");
                binding.progressBarRank.setProgress(progress);

                // Hiển thị ảnh đại diện (avatar)
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    binding.ivAvatar.clearColorFilter(); // Xóa tint để hiện đúng màu ảnh thật
                    Glide.with(this)
                            .load(user.getAvatarUrl())
                            .transform(new com.bumptech.glide.load.resource.bitmap.CircleCrop())
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(binding.ivAvatar);
                } else {
                    Glide.with(this).clear(binding.ivAvatar);
                    binding.ivAvatar.setImageResource(R.drawable.ic_profile);
                }

                // ── Granular Notification Settings (Nova Enterprise Logic) ──
                setupNotificationSwitches(user);

            } else if (resource.isError() && isLoggedIn) {
                if (resource.message != null && resource.message.contains("401")) {
                    tokenManager.clearAll();
                    updateUI(false);
                } else {
                    // Mạng lỗi (Offline) -> Lấy dữ liệu đệm từ TokenManager
                    binding.tvName.setText(tokenManager.getUserName() != null ? tokenManager.getUserName() : "Không rõ");
                    binding.tvEmail.setText(tokenManager.getUserEmail() != null ? tokenManager.getUserEmail() : "Đang offline");
                    
                    binding.tvTierBadge.setText("⭐ TIER");
                    binding.tvCurrentTier.setText("CẤP ĐỘ: OFFLINE");
                    binding.tvCinePoints.setText("-");
                    binding.tvPointsToNext.setText("KHÔNG THỂ TẢI EXP");
                    binding.progressBarRank.setProgress(0);

                    if (tokenManager.getAvatarUrl() != null && !tokenManager.getAvatarUrl().isEmpty()) {
                        binding.ivAvatar.clearColorFilter();
                        Glide.with(this)
                                .load(tokenManager.getAvatarUrl())
                                .transform(new com.bumptech.glide.load.resource.bitmap.CircleCrop())
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .into(binding.ivAvatar);
                    } else {
                        Glide.with(this).clear(binding.ivAvatar);
                        binding.ivAvatar.setImageResource(R.drawable.ic_profile);
                    }

                    if (resource.message != null && !resource.message.isEmpty()) {
                        SnackbarHelper.showError(binding.getRoot(), "Đang xem ở chế độ Offline");
                    }
                }
            }
        });

        // ── Dark/light mode toggle ──
        boolean isDark = ThemeManager.isDarkMode(requireContext());
        binding.switchDarkMode.setOnCheckedChangeListener(null);
        binding.switchDarkMode.setChecked(isDark);
        binding.switchDarkMode
                .setOnCheckedChangeListener((btn, isChecked) -> ThemeManager.setDarkMode(requireContext(), isChecked));
    }

    private void switchToTab(int menuItemId) {
        try {
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = requireActivity()
                    .findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(menuItemId);
            } else {
                // Fallback to direct navigation if bottomNav is not accessible
                if (menuItemId == R.id.bookingHistoryFragment) {
                    Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_history);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUI(boolean loggedIn) {
        if (loggedIn) {
            viewModel.loadUserProfile();
            binding.btnLogout.setText("ĐĂNG XUẤT");
            binding.btnLogout.setTextColor(getResources().getColor(R.color.error, null));
            binding.btnLogout.setStrokeColorResource(R.color.error);
            binding.btnLogout.setOnClickListener(v -> {
                viewModel.logout();
                // Khởi động lại ứng dụng để đưa người dùng về biểu đồ điều hướng (NavGraph) mặc
                // định (Dành cho khách)
                requireActivity().finish();
                startActivity(
                        new android.content.Intent(requireActivity(), com.cinema.ticket_booking.ui.MainActivity.class));
            });

            binding.btnNavReviews.setOnClickListener(v -> switchToTab(R.id.bookingHistoryFragment));
            binding.btnNavHistory.setOnClickListener(v -> switchToTab(R.id.bookingHistoryFragment));
            binding.btnNavWatchlist.setOnClickListener(v -> switchToTab(R.id.searchFragment)); // Watchlist could map to
                                                                                               // Search/Discover or
                                                                                               // specific Screen
            binding.btnNavGiftCards.setOnClickListener(
                    v -> Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_voucher));

            binding.btnRedeem.setOnClickListener(
                    v -> Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_wallet));
            binding.btnEditProfile.setOnClickListener(
                    v -> Navigation.findNavController(requireView()).navigate(R.id.editProfileFragment));

        } else {
            binding.tvName.setText("Chưa đăng nhập");
            binding.tvEmail.setText("Vui lòng đăng nhập để sử dụng tính năng");
            binding.tvCinePoints.setText("-");
            binding.tvPointsToNext.setText("0 PTS TO NEXT TIER");
            binding.progressBarRank.setProgress(0);

            binding.btnLogout.setText("ĐĂNG NHẬP");
            binding.btnLogout.setTextColor(getResources().getColor(R.color.primary, null));
            binding.btnLogout.setStrokeColorResource(R.color.primary);
            binding.btnLogout.setOnClickListener(v -> {
                Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_login);
            });

            View.OnClickListener loginListener = v -> Navigation.findNavController(requireView())
                    .navigate(R.id.action_profile_to_login);
            binding.btnNavHistory.setOnClickListener(loginListener);
            binding.btnNavGiftCards.setOnClickListener(loginListener);
            binding.btnRedeem.setOnClickListener(loginListener);
            binding.btnNavWatchlist.setOnClickListener(loginListener);
            binding.btnNavReviews.setOnClickListener(loginListener);
            binding.rowChangePassword.setOnClickListener(loginListener);
            binding.btnEditProfile.setOnClickListener(loginListener);

            // Disable switches if not logged in
            binding.switchTransactionNotify.setEnabled(false);
            binding.switchMarketingNotify.setEnabled(false);
        }
    }

    private void setupNotificationSwitches(com.cinema.ticket_booking.data.model.response.UserResponse user) {
        // Initial State
        binding.switchTransactionNotify.setOnCheckedChangeListener(null);
        binding.switchMarketingNotify.setOnCheckedChangeListener(null);

        binding.switchTransactionNotify.setChecked(user.getAllowTransactionNotification());
        binding.switchMarketingNotify.setChecked(user.getAllowMarketingNotification());

        // Marketing Logic (Simple Topic Sync)
        binding.switchMarketingNotify.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic("nova_all_users");
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("nova_all_users");
            }
            // Sync to Backend
            viewModel.updateNotificationSettings(isChecked, binding.switchTransactionNotify.isChecked())
                    .observe(getViewLifecycleOwner(), res -> {
                        if (res.isSuccess()) SnackbarHelper.showRaw(binding.getRoot(), "Đã cập nhật tùy chọn khuyến mãi");
                    });
        });

        // Transactional Logic (With Warning Dialog)
        binding.switchTransactionNotify.setOnCheckedChangeListener((btn, isChecked) -> {
            if (!isChecked) {
                // Show Warning Dialog before allowing to turn OFF
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("⚠️ Tắt thông báo vé?")
                        .setMessage("Nếu tắt tính năng này, bạn sẽ không nhận được mã QR soát vé tự động và các thông báo thay đổi lịch chiếu khẩn cấp. Bạn có chắc chắn muốn tắt không?")
                        .setPositiveButton("VẪN TẮT", (dialog, which) -> {
                            syncTransactionSetting(false);
                        })
                        .setNegativeButton("GIỮ BẬT", (dialog, which) -> {
                            binding.switchTransactionNotify.setOnCheckedChangeListener(null);
                            binding.switchTransactionNotify.setChecked(true);
                            setupNotificationSwitches(user); // Re-attach listener
                        })
                        .setCancelable(false)
                        .show();
            } else {
                syncTransactionSetting(true);
            }
        });
    }

    private void syncTransactionSetting(boolean isEnabled) {
        viewModel.updateNotificationSettings(binding.switchMarketingNotify.isChecked(), isEnabled)
                .observe(getViewLifecycleOwner(), res -> {
                    if (res.isSuccess()) SnackbarHelper.showRaw(binding.getRoot(), "Đã cập nhật tùy chọn giao dịch");
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Mỗi lần quay lại Profile, reload dữ liệu mới nhất (CP, EXP, tier...)
        // Dùng refreshUserProfile() thay vì loadUserProfile() vì load sẽ skip nếu đã có data
        if (tokenManager.isLoggedIn() && viewModel != null) {
            viewModel.refreshUserProfile();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

