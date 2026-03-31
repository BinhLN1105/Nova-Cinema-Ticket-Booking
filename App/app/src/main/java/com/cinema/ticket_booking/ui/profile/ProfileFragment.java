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
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

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
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        updateUI(tokenManager.isLoggedIn());

        // Observe profile to show data or handle errors
        viewModel.getProfile().observe(getViewLifecycleOwner(), resource -> {
            boolean isLoggedIn = tokenManager.isLoggedIn();
            if (resource.isSuccess() && resource.data != null && isLoggedIn) {
                var user = resource.data;
                binding.tvName.setText(user.getFullName());
                binding.tvEmail.setText(user.getEmail());
                binding.tvCinePoints.setText(String.valueOf(user.getCinePoints()));
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    Glide.with(this).load(user.getAvatarUrl()).circleCrop().into(binding.ivAvatar);
                }

                int pts = user.getCinePoints();
                long currentMin = user.getCurrentTierMinPoints() != null ? user.getCurrentTierMinPoints() : 0;
                long nextMin = user.getNextTierMinPoints() != null ? user.getNextTierMinPoints() : 500;

                String tier;
                String nextTier;
                if (currentMin >= 10000) {
                    tier = "DIAMOND";
                    nextTier = "MAX";
                } else if (currentMin >= 3000) {
                    tier = "GOLD";
                    nextTier = "DIAMOND";
                } else if (currentMin >= 500) {
                    tier = "SILVER";
                    nextTier = "GOLD";
                } else {
                    tier = "MEMBER";
                    nextTier = "SILVER";
                }

                long nextSub = nextMin - pts;
                if (nextSub < 0 || currentMin >= 10000)
                    nextSub = 0;

                int progress = 100;
                long diff = nextMin - currentMin;
                if (diff > 0 && currentMin < 10000) {
                    progress = (int) (((pts - currentMin) * 100) / diff);
                }

                binding.tvTierBadge.setText("⭐ " + tier);
                binding.tvCurrentTier.setText("CURRENT TIER: " + tier);
                binding.tvPointsToNext.setText((nextSub > 0) ? (nextSub + " PTS TO " + nextTier) : "MAX TIER");
                binding.progressBarRank.setProgress(progress);

            } else if (resource.isError()) {
                // Nếu lỗi 401 hoặc lỗi xác thực -> có thể token đã hết hạn hoặc stale
                if (resource.message != null && resource.message.contains("401")) {
                    tokenManager.clearAll();
                    updateUI(false);
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

    private void updateUI(boolean loggedIn) {
        if (loggedIn) {
            viewModel.loadProfile();
            binding.btnLogout.setText("ĐĂNG XUẤT");
            binding.btnLogout.setTextColor(getResources().getColor(R.color.error, null));
            binding.btnLogout.setStrokeColorResource(R.color.error);
            binding.btnLogout.setOnClickListener(v -> {
                viewModel.logout();
                // Khởi động lại ứng dụng để đưa người dùng về biểu đồ điều hướng (NavGraph) mặc định (Dành cho khách)
                requireActivity().finish();
                startActivity(new android.content.Intent(requireActivity(), com.cinema.ticket_booking.ui.MainActivity.class));
            });

            binding.btnNavHistory.setOnClickListener(
                    v -> Navigation.findNavController(requireView()).navigate(R.id.bookingHistoryFragment));
            binding.btnNavGiftCards.setOnClickListener(
                    v -> Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_voucher));
            binding.btnRedeem.setOnClickListener(
                    v -> Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_wallet));
            binding.rowNotifications.setOnClickListener(
                    v -> Navigation.findNavController(requireView()).navigate(R.id.notificationFragment));

            // Coming soon features
            android.view.View.OnClickListener comingSoon = v -> android.widget.Toast
                    .makeText(requireContext(), "Tính năng đang phát triển", android.widget.Toast.LENGTH_SHORT).show();
            binding.btnNavWatchlist.setOnClickListener(comingSoon);
            binding.btnNavReviews.setOnClickListener(comingSoon);
            binding.rowChangePassword.setOnClickListener(comingSoon);
            binding.btnEditProfile.setOnClickListener(comingSoon);

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
            binding.rowNotifications.setOnClickListener(loginListener);
            binding.btnNavWatchlist.setOnClickListener(loginListener);
            binding.btnNavReviews.setOnClickListener(loginListener);
            binding.rowChangePassword.setOnClickListener(loginListener);
            binding.btnEditProfile.setOnClickListener(loginListener);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
