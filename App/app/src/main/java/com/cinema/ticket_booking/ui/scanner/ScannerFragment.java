package com.cinema.ticket_booking.ui.scanner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.CheckInResponse;
import com.cinema.ticket_booking.databinding.FragmentScannerBinding;
import com.cinema.ticket_booking.util.Resource;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ScannerFragment extends Fragment {

    private FragmentScannerBinding binding;
    private ScannerViewModel viewModel;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(requireContext(), "Đã huỷ quét", Toast.LENGTH_SHORT).show();
                } else {
                    String qrCode = result.getContents();
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.cardResult.setVisibility(View.GONE);
                    viewModel.checkInTicket(qrCode);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentScannerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ScannerViewModel.class);

        binding.btnScan.setOnClickListener(v -> startScan());

        binding.btnLogout.setOnClickListener(v -> {
            viewModel.logout();
            Navigation.findNavController(view).navigate(R.id.action_scanner_to_login);
        });

        viewModel.getCheckInResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == Resource.Status.LOADING) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.cardResult.setVisibility(View.GONE);
            } else if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                binding.progressBar.setVisibility(View.GONE);
                showResult(resource.data);
            } else if (resource.status == Resource.Status.ERROR) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                binding.tvCheckInStatus.setText("LỖI: \n" + resource.message);
                binding.tvCheckInStatus.setTextColor(getResources().getColor(R.color.error, null));
                binding.tvMovieTitle.setText("");
                binding.tvCinemaInfo.setText("");
                binding.tvStartTime.setText("");
                binding.tvSeats.setText("");
                binding.cardResult.setVisibility(View.VISIBLE);
            }
        });
    }

    private void startScan() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Hướng camera vào mã QR vé.\\nBấm Tăng Âm Lượng để bật Flash.");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(false);
        barcodeLauncher.launch(options);
    }

    private void showResult(CheckInResponse data) {
        binding.cardResult.setVisibility(View.VISIBLE);
        binding.tvCheckInStatus.setText("Thành công! Mã: " + data.getBookingCode());
        binding.tvCheckInStatus.setTextColor(getResources().getColor(R.color.success, null));

        binding.tvMovieTitle.setText(data.getMovieTitle());
        binding.tvCinemaInfo.setText(data.getCinemaName() + " - " + data.getScreenName());
        binding.tvStartTime.setText("Thời gian: " + data.getStartTime());

        if (data.getSeats() != null && !data.getSeats().isEmpty()) {
            String seatStr = data.getSeats().stream()
                    .map(s -> s.getRowLabel() + s.getColNumber())
                    .collect(Collectors.joining(", "));
            binding.tvSeats.setText("Ghế: " + seatStr);
        } else {
            binding.tvSeats.setText("Ghế: N/A");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
