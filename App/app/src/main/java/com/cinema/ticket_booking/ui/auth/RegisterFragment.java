package com.cinema.ticket_booking.ui.auth;

import com.cinema.ticket_booking.util.SnackbarHelper;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentRegisterBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnRegister.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString();
            String confirmPassword = binding.etConfirmPassword.getText().toString();
            String name = binding.etFullName.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                SnackbarHelper.showError(binding.getRoot(), "Vui lòng nhập đầy đủ thông tin");
                return;
            }
            if (!password.equals(confirmPassword)) {
                SnackbarHelper.showError(binding.getRoot(), "Mật khẩu không khớp");
                return;
            }
            authViewModel.register(email, password, name);
        });

        binding.tvLogin.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        authViewModel.getAuthResult().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    androidx.navigation.NavController navController = Navigation.findNavController(requireView());
                    if (navController.getCurrentDestination() != null &&
                            navController.getCurrentDestination().getId() == R.id.registerFragment) {
                        navController.navigate(R.id.action_register_to_home);
                    }
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    SnackbarHelper.showError(binding.getRoot(), resource.message);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
