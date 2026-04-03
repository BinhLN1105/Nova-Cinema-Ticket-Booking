package com.cinema.ticket_booking.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.cinema.ticket_booking.data.model.response.ApiResponse;
import com.cinema.ticket_booking.databinding.FragmentChangePasswordBinding;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.util.SnackbarHelper;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class ChangePasswordFragment extends Fragment {
    private FragmentChangePasswordBinding binding;

    @Inject
    ApiService apiService;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentChangePasswordBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle s) {
        super.onViewCreated(view, s);

        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        binding.btnChangePassword.setOnClickListener(v -> attemptChangePassword());
    }

    private void attemptChangePassword() {
        String current = binding.edtCurrentPassword.getText().toString().trim();
        String newPass = binding.edtNewPassword.getText().toString().trim();
        String confirm = binding.edtConfirmPassword.getText().toString().trim();

        // Validation
        binding.tilCurrentPassword.setError(null);
        binding.tilNewPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        if (current.isEmpty()) {
            binding.tilCurrentPassword.setError("Vui lòng nhập mật khẩu hiện tại");
            return;
        }
        if (newPass.isEmpty()) {
            binding.tilNewPassword.setError("Vui lòng nhập mật khẩu mới");
            return;
        }
        if (newPass.length() < 6) {
            binding.tilNewPassword.setError("Mật khẩu mới phải có ít nhất 6 ký tự");
            return;
        }
        if (!newPass.equals(confirm)) {
            binding.tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }
        if (current.equals(newPass)) {
            binding.tilNewPassword.setError("Mật khẩu mới phải khác mật khẩu hiện tại");
            return;
        }

        setLoading(true);

        Map<String, String> body = new HashMap<>();
        body.put("currentPassword", current);
        body.put("newPassword", newPass);

        apiService.changePassword(body)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        if (!isAdded()) return;
                        setLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            SnackbarHelper.showSuccess(binding.getRoot(),
                                    "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
                            // Delay pop to let user see the success message
                            binding.getRoot().postDelayed(() -> {
                                if (isAdded()) {
                                    Navigation.findNavController(requireView()).popBackStack();
                                }
                            }, 1500);
                        } else {
                            String msg = "Đổi mật khẩu thất bại";
                            try {
                                if (response.errorBody() != null) {
                                    msg = response.errorBody().string();
                                    if (msg.contains("message")) {
                                        msg = msg.substring(msg.indexOf("message") + 10);
                                        msg = msg.substring(0, msg.indexOf("\""));
                                    }
                                }
                            } catch (Exception ignored) {}
                            SnackbarHelper.showError(binding.getRoot(), msg);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call,
                                          @NonNull Throwable t) {
                        if (!isAdded()) return;
                        setLoading(false);
                        SnackbarHelper.showError(binding.getRoot(),
                                "Lỗi kết nối: " + t.getMessage());
                    }
                });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnChangePassword.setEnabled(!loading);
        binding.edtCurrentPassword.setEnabled(!loading);
        binding.edtNewPassword.setEnabled(!loading);
        binding.edtConfirmPassword.setEnabled(!loading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
