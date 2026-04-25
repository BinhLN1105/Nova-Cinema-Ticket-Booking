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
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentResetPasswordBinding;
import com.cinema.ticket_booking.util.SnackbarHelper;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ResetPasswordFragment extends Fragment {

    private FragmentResetPasswordBinding binding;
    private AuthViewModel viewModel;
    private String resetToken;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            resetToken = ResetPasswordFragmentArgs.fromBundle(getArguments()).getResetToken();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentResetPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        binding.btnReset.setOnClickListener(v -> {
            String newPass = binding.etNewPassword.getText().toString();
            String confirmPass = binding.etConfirmPassword.getText().toString();

            if (newPass.length() < 6) {
                SnackbarHelper.showError(binding.getRoot(), "Mật khẩu phải có ít nhất 6 ký tự");
                return;
            }

            if (!newPass.equals(confirmPass)) {
                SnackbarHelper.showError(binding.getRoot(), "Mật khẩu xác nhận không khớp");
                return;
            }

            viewModel.resetPassword(resetToken, newPass);
        });

        viewModel.getPasswordResult().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    setLoadingState(true);
                    break;
                case SUCCESS:
                    setLoadingState(false);
                    SnackbarHelper.showSuccess(binding.getRoot(), "Đổi mật khẩu thành công! Vui lòng đăng nhập.");

                    // Use SafeArgs and clear backstack
                    Navigation.findNavController(requireView()).navigate(
                            ResetPasswordFragmentDirections.actionResetPasswordToLogin());
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
        binding.btnReset.setEnabled(!isLoading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

