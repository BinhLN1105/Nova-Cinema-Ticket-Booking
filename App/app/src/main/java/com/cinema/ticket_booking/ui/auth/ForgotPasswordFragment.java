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
import androidx.navigation.NavController;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentForgotPasswordBinding;
import com.cinema.ticket_booking.util.SnackbarHelper;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;
    private AuthViewModel viewModel;

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
            String email = binding.etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                SnackbarHelper.showError(binding.getRoot(), "Vui lòng nhập Email");
                return;
            }
            viewModel.forgotPassword(email);
        });

        viewModel.getPasswordResult().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    setLoadingState(true);
                    break;
                case SUCCESS:
                    setLoadingState(false);
                    // Use Safe Args for navigation
                    String email = binding.etEmail.getText().toString().trim();
                    NavController navController = Navigation.findNavController(requireView());
                    if (navController.getCurrentDestination() != null &&
                            navController.getCurrentDestination()
                                    .getId() == com.cinema.ticket_booking.R.id.forgotPasswordFragment) {
                        navController.navigate(
                                ForgotPasswordFragmentDirections.actionForgotToVerifyOtp(email));
                    }

                    SnackbarHelper.showSuccess(binding.getRoot(), "Mã OTP đã được gửi đến email của bạn");
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

