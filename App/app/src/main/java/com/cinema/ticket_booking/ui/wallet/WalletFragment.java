package com.cinema.ticket_booking.ui.wallet;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.local.TokenManager;
import com.cinema.ticket_booking.databinding.FragmentWalletBinding;
import com.cinema.ticket_booking.util.Resource.Status;
import com.cinema.ticket_booking.util.SnackbarHelper;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WalletFragment extends Fragment {

    private FragmentWalletBinding binding;
    private WalletViewModel viewModel;

    @Inject
    TokenManager tokenManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWalletBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WalletViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Show current balance from profile already in TokenManager (or load profile)
        viewModel.loadProfile();

        binding.btnRedeem.setOnClickListener(v -> {
            String code = binding.etGiftCode.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                SnackbarHelper.showError(binding.getRoot(), "Vui lòng nhập mã thẻ quà tặng");
                return;
            }
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.tvRedeemMessage.setVisibility(View.GONE);
            viewModel.redeemGiftCard(code);
        });

        viewModel.getProfile().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                binding.tvBalance.setText(String.valueOf(resource.data.getCinePoints()));
            }
        });

        viewModel.getRedeemResult().observe(getViewLifecycleOwner(), resource -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvRedeemMessage.setVisibility(View.VISIBLE);
            if (resource.status == Status.SUCCESS) {
                binding.etGiftCode.setText("");
                binding.tvRedeemMessage.setText("✅ Đổi thẻ thành công! Điểm đã được cộng vào tài khoản.");
                binding.tvRedeemMessage.setTextColor(getResources().getColor(R.color.success, null));
                viewModel.loadProfile(); // refresh balance
            } else if (resource.status == Status.ERROR) {
                binding.tvRedeemMessage.setText("❌ " + resource.message);
                binding.tvRedeemMessage.setTextColor(getResources().getColor(R.color.error, null));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
