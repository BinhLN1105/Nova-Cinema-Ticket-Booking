package com.cinema.ticket_booking.ui.auth;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.NavController;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentVerifyOtpBinding;
import com.cinema.ticket_booking.util.SnackbarHelper;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VerifyOtpFragment extends Fragment {

    private FragmentVerifyOtpBinding binding;
    private AuthViewModel viewModel;
    private String email;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            email = VerifyOtpFragmentArgs.fromBundle(getArguments()).getEmail();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVerifyOtpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        binding.etOtp.requestFocus();
        binding.etOtp.postDelayed(() -> {
            if (isAdded()) {
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(binding.etOtp, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 200);

        binding.btnVerify.setOnClickListener(v -> {
            String otp = binding.etOtp.getText().toString().trim();
            if (otp.length() != 6) {
                SnackbarHelper.showError(binding.getRoot(), "Vui lòng nhập mã OTP 6 số");
                return;
            }
            viewModel.verifyOtp(email, otp);
        });

        binding.tvResend.setOnClickListener(v -> {
            viewModel.forgotPassword(email);
            SnackbarHelper.showSuccess(binding.getRoot(), "Đang gửi lại mã OTP...");
        });

        viewModel.getVerifyOtpResult().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    setLoadingState(true);
                    break;
                case SUCCESS:
                    setLoadingState(false);
                    if (resource.data != null) {
                        NavController navController = Navigation.findNavController(requireView());
                        if (navController.getCurrentDestination() != null &&
                                navController.getCurrentDestination().getId() == R.id.verifyOtpFragment) {
                            navController.navigate(
                                    VerifyOtpFragmentDirections.actionVerifyOtpToResetPassword(resource.data));
                        }
                    }
                    break;
                case ERROR:
                    setLoadingState(false);
                    SnackbarHelper.showError(binding.getRoot(), resource.message);
                    break;
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnVerify.setEnabled(!isLoading);
        binding.tvResend.setEnabled(!isLoading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
