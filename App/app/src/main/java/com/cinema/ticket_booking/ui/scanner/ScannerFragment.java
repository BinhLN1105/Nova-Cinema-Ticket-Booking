package com.cinema.ticket_booking.ui.scanner;

import android.content.DialogInterface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.CheckInResponse;
import com.cinema.ticket_booking.databinding.FragmentScannerBinding;
import com.cinema.ticket_booking.util.Resource;
import com.cinema.ticket_booking.util.SnackbarHelper;
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

                    if (qrCode.startsWith("http") || qrCode.startsWith("novaticket://")) {
                        try {
                            android.content.Intent intent = new android.content.Intent(
                                    android.content.Intent.ACTION_VIEW, android.net.Uri.parse(qrCode));
                            startActivity(intent);
                        } catch (Exception e) {
                            SnackbarHelper.showError(binding.getRoot(), "Mã QR không hợp lệ hoặc không được hỗ trợ");
                        }
                    } else {
                        binding.progressBar.setVisibility(View.VISIBLE);
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
            binding.tvScannerSubtitle.setText("Quét mã để xem thông tin phim, ưu đãi");
            binding.btnLogout.setText("Quay lại");
            binding.btnLogout.setTextColor(getResources().getColor(R.color.primary, null));
            binding.btnManualInput.setVisibility(View.GONE);
            binding.btnLogout.setOnClickListener(v -> {
                if (!Navigation.findNavController(view).popBackStack()) {
                    Navigation.findNavController(view).navigate(R.id.homeFragment);
                }
            });
        } else {
            binding.tvScannerTitle.setText("Soát Vé Phim");
            binding.tvScannerSubtitle.setText("Dành cho nhân viên kiểm soát vé tại rạp");
            binding.btnLogout.setText("Quay lại");
            binding.btnManualInput.setVisibility(View.VISIBLE);
            binding.btnLogout.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_scanner_to_login);
            });
        }

        binding.btnScan.setOnClickListener(v -> startScan());
        
        binding.btnManualInput.setOnClickListener(v -> showManualInputDialog());

        binding.btnCloseSuccess.setOnClickListener(v -> resetUiToMain());
        binding.btnCloseError.setOnClickListener(v -> resetUiToMain());

        viewModel.getCheckInResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == Resource.Status.LOADING) {
                binding.progressBar.setVisibility(View.VISIBLE);
            } else if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                binding.progressBar.setVisibility(View.GONE);
                showSuccessOverlay(resource.data);
            } else if (resource.status == Resource.Status.ERROR) {
                binding.progressBar.setVisibility(View.GONE);
                showErrorOverlay(resource.message);
            }
        });
    }

    private void showManualInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Nhập mã vé thủ công");
        builder.setMessage("Nhập mã đặt vé (ví dụ: BK2026...) nạp vào hệ thống để kiểm tra.");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Kiểm tra", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if (!code.isEmpty()) {
                binding.progressBar.setVisibility(View.VISIBLE);
                viewModel.checkInTicket(code);
            } else {
                SnackbarHelper.showError(binding.getRoot(), "Vui lòng nhập mã vé");
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void startScan() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Hướng camera vào mã QR vé.\\nBấm Tăng Âm Lượng để bật Flash.");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(false);
        options.setCaptureActivity(PortraitCaptureActivity.class);
        barcodeLauncher.launch(options);
    }

    private void showSuccessOverlay(CheckInResponse data) {
        binding.layoutMain.setVisibility(View.GONE);
        binding.overlayError.setVisibility(View.GONE);
        binding.overlaySuccess.setVisibility(View.VISIBLE);

        binding.tvSuccessCode.setText("Mã: " + data.getBookingCode());
        binding.tvSuccessMovie.setText(data.getMovieTitle());
        binding.tvSuccessCinema.setText(data.getCinemaName() + " - " + data.getScreenName());
        binding.tvSuccessTime.setText("Thời gian: " + data.getStartTime().replace("T", " "));

        if (data.getSeats() != null && !data.getSeats().isEmpty()) {
            String seatStr = data.getSeats().stream()
                    .map(s -> s.getRowLabel() + s.getColNumber())
                    .collect(Collectors.joining(", "));
            binding.tvSuccessSeats.setText("Ghế: " + seatStr);
        } else {
            binding.tvSuccessSeats.setText("Ghế: Theo vé combo");
        }
        
        binding.tvSuccessCustomer.setText("Khách: " + (data.getCustomerName() != null ? data.getCustomerName() : "Khách vãng lai"));
        binding.tvSuccessEmail.setText(data.getCustomerEmail() != null ? data.getCustomerEmail() : "");
        if (data.getCustomerPhone() != null && !data.getCustomerPhone().isEmpty()) {
            binding.tvSuccessPhone.setVisibility(View.VISIBLE);
            binding.tvSuccessPhone.setText("SĐT: " + data.getCustomerPhone());
        } else {
            binding.tvSuccessPhone.setVisibility(View.GONE);
        }
    }

    private void showErrorOverlay(String message) {
        binding.layoutMain.setVisibility(View.GONE);
        binding.overlaySuccess.setVisibility(View.GONE);
        binding.overlayError.setVisibility(View.VISIBLE);

        binding.tvErrorMessage.setText(message);

        // Phát âm thanh báo động khẩn cấp
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (notification == null) {
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            Ringtone r = RingtoneManager.getRingtone(requireContext(), notification);
            r.play();
            
            // Xóa tiếng sau 2 giây để tránh ồn ảo
            binding.overlayError.postDelayed(() -> {
                if (r != null && r.isPlaying()) r.stop();
            }, 2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetUiToMain() {
        binding.overlaySuccess.setVisibility(View.GONE);
        binding.overlayError.setVisibility(View.GONE);
        binding.layoutMain.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

