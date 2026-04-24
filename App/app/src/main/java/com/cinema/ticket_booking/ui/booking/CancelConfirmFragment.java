package com.cinema.ticket_booking.ui.booking;

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
import com.cinema.ticket_booking.databinding.FragmentCancelConfirmBinding;
import com.cinema.ticket_booking.util.SnackbarHelper;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CancelConfirmFragment extends Fragment {

    private FragmentCancelConfirmBinding binding;
    private BookingDetailViewModel viewModel;
    private String token;
    private String bookingId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            token = getArguments().getString("token");
            bookingId = getArguments().getString("bookingId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCancelConfirmBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BookingDetailViewModel.class);

        if (token == null || bookingId == null) {
            showError("Thông tin xác nhận không hợp lệ");
            return;
        }

        confirmCancellation();

        binding.btnBackToDetail.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("bookingId", bookingId);
            Navigation.findNavController(v).navigate(R.id.bookingDetailFragment, args);
        });
    }

    private void confirmCancellation() {
        viewModel.cancelConfirm(token, bookingId).observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    showSuccess();
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    showError(resource.message);
                    break;
            }
        });
    }

    private void showSuccess() {
        binding.ivStatus.setImageResource(R.drawable.ic_check);
        binding.tvTitle.setText("Hủy vé thành công!");
        binding.tvMessage.setText("Yêu cầu hủy vé của bạn đã được thực hiện. Điểm CinePoint sẽ được hoàn trả vào ví của bạn.");
        binding.btnBackToDetail.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        binding.ivStatus.setImageResource(R.drawable.ic_close);
        binding.tvTitle.setText("Hủy vé thất bại");
        binding.tvMessage.setText(message);
        binding.btnBackToDetail.setVisibility(View.VISIBLE);
        binding.btnBackToDetail.setText("Quay lại");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
