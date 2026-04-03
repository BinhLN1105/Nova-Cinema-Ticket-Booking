package com.cinema.ticket_booking.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.cinema.ticket_booking.databinding.FragmentForgotPasswordBinding;
import com.cinema.ticket_booking.util.SnackbarHelper;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;
    private AuthViewModel viewModel;
    
    private enum Step {
        REQUEST_OTP,
        VERIFY_OTP,
        RESET_PASSWORD
    }
    
    private Step currentStep = Step.REQUEST_OTP;
    private String savedEmail = "";
    private String savedResetToken = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        binding.btnSubmit.setOnClickListener(v -> {
            switch (currentStep) {
                case REQUEST_OTP:
                    String email = binding.etEmail.getText().toString().trim();
                    if (email.isEmpty()) {
                        SnackbarHelper.showError(binding.getRoot(), "Vui lòng nhập Email");
                        return;
                    }
                    savedEmail = email;
                    viewModel.forgotPassword(email);
                    break;
                    
                case VERIFY_OTP:
                    String otp = binding.etToken.getText().toString().trim();
                    if (otp.length() != 6) {
                        SnackbarHelper.showError(binding.getRoot(), "Vui lòng nhập mã OTP 6 số");
                        return;
                    }
                    viewModel.verifyOtp(savedEmail, otp);
                    break;
                    
                case RESET_PASSWORD:
                    String newPass = binding.etNewPassword.getText().toString();
                    if (newPass.length() < 6) {
                        SnackbarHelper.showError(binding.getRoot(), "Mật khẩu mới phải có ít nhất 6 ký tự");
                        return;
                    }
                    viewModel.resetPassword(savedResetToken, newPass);
                    break;
            }
        });

        viewModel.getPasswordResult().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    setLoadingState(true);
                    break;
                case SUCCESS:
                    setLoadingState(false);
                    if (currentStep == Step.REQUEST_OTP) {
                        currentStep = Step.VERIFY_OTP;
                        binding.tvSubtitle.setText("Vui lòng nhập mã OTP 6 số đã được gửi đến Email của bạn.");
                        binding.tilEmail.setEnabled(false);
                        binding.layoutResetStep.setVisibility(View.VISIBLE);
                        // Hide new password input for now
                        binding.etNewPassword.setVisibility(View.GONE);
                        ((View) binding.etNewPassword.getParent().getParent()).setVisibility(View.GONE);
                        
                        binding.btnSubmit.setText("Xác minh OTP");
                        SnackbarHelper.showSuccess(binding.getRoot(), "Đã gửi mã OTP đến Email của bạn.");
                    } else if (currentStep == Step.RESET_PASSWORD) {
                        SnackbarHelper.showSuccess(binding.getRoot(), "Đổi mật khẩu thành công! Vui lòng đăng nhập.");
                        Navigation.findNavController(view).popBackStack();
                    }
                    break;
                case ERROR:
                    setLoadingState(false);
                    SnackbarHelper.showError(binding.getRoot(), resource.message);
                    break;
            }
        });

        viewModel.getVerifyOtpResult().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    setLoadingState(true);
                    break;
                case SUCCESS:
                    setLoadingState(false);
                    if (currentStep == Step.VERIFY_OTP && resource.data != null) {
                        currentStep = Step.RESET_PASSWORD;
                        savedResetToken = resource.data;
                        
                        binding.tvSubtitle.setText("Tuyệt vời! Bây giờ hãy tạo mật khẩu mới.");
                        // Hide OTP input
                        binding.etToken.setVisibility(View.GONE);
                        ((View) binding.etToken.getParent().getParent()).setVisibility(View.GONE);
                        // Show New Password input
                        binding.etNewPassword.setVisibility(View.VISIBLE);
                        ((View) binding.etNewPassword.getParent().getParent()).setVisibility(View.VISIBLE);
                        
                        binding.btnSubmit.setText("Đặt lại mật khẩu");
                        SnackbarHelper.showSuccess(binding.getRoot(), "Xác thực OTP thành công.");
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
        binding.btnSubmit.setEnabled(!isLoading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
