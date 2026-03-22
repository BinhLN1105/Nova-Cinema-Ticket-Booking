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
        if (tokenManager.isLoggedIn()) {
            viewModel.loadProfile();

            binding.btnLogout.setText("Đăng xuất");
            binding.btnLogout.setTextColor(getResources().getColor(R.color.error, null));
            binding.btnLogout.setStrokeColorResource(R.color.error);
            binding.btnLogout.setOnClickListener(v -> {
                viewModel.logout();
                Navigation.findNavController(view).navigate(R.id.action_profile_to_login);
            });

            binding.rowMyTickets
                    .setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.bookingHistoryFragment));
            binding.rowNotifications
                    .setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.notificationFragment));
            binding.rowWallet.setOnClickListener(
                    v -> Navigation.findNavController(view).navigate(R.id.action_profile_to_wallet));
            binding.rowVoucher.setOnClickListener(
                    v -> Navigation.findNavController(view).navigate(R.id.action_profile_to_voucher));

            // Observe profile to show CinePoints
            viewModel.getProfile().observe(getViewLifecycleOwner(), resource -> {
                if (resource.isSuccess() && resource.data != null) {
                    var user = resource.data;
                    binding.tvName.setText(user.getFullName());
                    binding.tvEmail.setText(user.getEmail());
                    binding.tvCinePoints.setText(user.getCinePoints() + " pts");
                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                        Glide.with(this).load(user.getAvatarUrl()).circleCrop().into(binding.ivAvatar);
                    }
                }
            });
        } else {
            binding.tvName.setText("Chưa đăng nhập");
            binding.tvEmail.setText("Vui lòng đăng nhập để sử dụng tính năng");
            binding.tvCinePoints.setText("-");

            binding.btnLogout.setText("Đăng nhập");
            binding.btnLogout.setTextColor(getResources().getColor(R.color.primary, null));
            binding.btnLogout.setStrokeColorResource(R.color.primary);
            binding.btnLogout.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_profile_to_login);
            });

            binding.rowMyTickets
                    .setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_profile_to_login));
            binding.rowNotifications
                    .setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_profile_to_login));
            binding.rowWallet
                    .setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_profile_to_login));
            binding.rowVoucher
                    .setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_profile_to_login));
        }

        // ── Dark/light mode toggle (works for both logged in and guest) ──
        boolean isDark = ThemeManager.isDarkMode(requireContext());
        // setChecked before listener to avoid triggering onChange
        binding.switchDarkMode.setOnCheckedChangeListener(null);
        binding.switchDarkMode.setChecked(isDark);
        binding.switchDarkMode.setOnCheckedChangeListener((btn, isChecked) ->
                ThemeManager.setDarkMode(requireContext(), isChecked));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
