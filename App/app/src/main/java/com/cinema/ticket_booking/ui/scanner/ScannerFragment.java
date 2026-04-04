package com.cinema.ticket_booking.ui.scanner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.cinema.ticket_booking.util.SnackbarHelper;
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

import javax.inject.Inject;
import com.cinema.ticket_booking.data.local.TokenManager;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ScannerFragment extends Fragment {

    private FragmentScannerBinding binding;
    private ScannerViewModel viewModel;

    @Inject
    TokenManager tokenManager;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    SnackbarHelper.showSuccess(binding.getRoot(), "Đã huỷ quét");
                } else {
                    String qrCode = result.getContents();

                    // NOVA Dual-Flow Logic: STAFF vs CUSTOMER
                    if (qrCode.startsWith("http") || qrCode.startsWith("novaticket://")) {
                        // CUSTOMER FLOW: Deep Link Processing
                        try {
                            android.content.Intent intent = new android.content.Intent(
                                    android.content.Intent.ACTION_VIEW, android.net.Uri.parse(qrCode));
                            startActivity(intent);
                        } catch (Exception e) {
                            SnackbarHelper.showError(binding.getRoot(), "Mã QR không hợp lệ hoặc không được hỗ trợ");
                        }
                    } else {
                        // STAFF FLOW: Booking Check-In
                        binding.progressBar.setVisibility(View.VISIBLE);
                        binding.cardResult.setVisibility(View.GONE);
                        viewModel.checkInTicket(qrCode);
                    }
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

        boolean isStaff = "STAFF".equals(tokenManager.getUserRole()) || "ADMIN".equals(tokenManager.getUserRole());
        if (!isStaff) {
            binding.tvScannerTitle.setText("Quét QR Tiện Ích");
            binding.tvScannerSubtitle.setText("Quét mã để xem thông tin phim, ưu đãi hoặc tham gia sự kiện");

            // Nova Optimization: Change Logout to Back for normal users
            binding.btnLogout.setText("Quay lại");
            binding.btnLogout.setTextColor(getResources().getColor(R.color.primary, null));
            binding.btnLogout.setOnClickListener(v -> {
                if (!Navigation.findNavController(view).popBackStack()) {
                    Navigation.findNavController(view).navigate(R.id.homeFragment);
                }
            });
        } else {
            binding.tvScannerTitle.setText("Soát Vé Phim");
            binding.tvScannerSubtitle.setText("Dành cho nhân viên kiểm soát vé tại rạp");
            binding.btnLogout.setText("Quay lại");
            binding.btnLogout.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_scanner_to_login);
            });
        }

        binding.btnScan.setOnClickListener(v -> startScan());

        viewModel.getCheckInResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == Resource.Status.LOADING) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.cardResult.setVisibility(View.GONE);
            } else if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                binding.progressBar.setVisibility(View.GONE);
                showResult(resource.data);
            } else if (resource.status == Resource.Status.ERROR) {
                binding.progressBar.setVisibility(View.GONE);
                SnackbarHelper.showError(binding.getRoot(), resource.message);
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
        options.setPrompt("Hướng camera vào mã QR vé.\nBấm Tăng Âm Lượng để bật Flash.");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);

        // Nova Enterprise: Force Portrait Mode (Like Momo/Zalo)
        // We set Locked to false so it respects the Manifest setting of
        // PortraitCaptureActivity
        options.setOrientationLocked(false);
        options.setCaptureActivity(PortraitCaptureActivity.class);

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
