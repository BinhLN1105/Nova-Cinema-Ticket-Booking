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
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentProfileBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.loadProfile();

        // Hiển thị trạng thái theme switch
        binding.switchDarkMode.setChecked(ThemeManager.isDarkMode(requireContext()));
        binding.switchDarkMode
                .setOnCheckedChangeListener((btn, isChecked) -> ThemeManager.setDarkMode(requireContext(), isChecked));

        viewModel.getProfile().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                var u = resource.data;
                binding.tvName.setText(u.getFullName() != null ? u.getFullName() : "Người dùng");
                binding.tvEmail.setText(u.getEmail());
                if (u.getAvatarUrl() != null && !u.getAvatarUrl().isEmpty()) {
                    Glide.with(this).load(u.getAvatarUrl())
                            .circleCrop()
                            .placeholder(R.drawable.ic_profile)
                            .into(binding.ivAvatar);
                }
            }
        });

        binding.btnLogout.setOnClickListener(v -> {
            viewModel.logout();
            Navigation.findNavController(view).navigate(R.id.action_profile_to_login);
        });

        binding.rowMyTickets
                .setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.bookingHistoryFragment));

        binding.rowNotifications
                .setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.notificationFragment));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
