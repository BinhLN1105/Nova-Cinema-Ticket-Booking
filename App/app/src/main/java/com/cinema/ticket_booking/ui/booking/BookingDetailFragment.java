package com.cinema.ticket_booking.ui.booking;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentBookingDetailBinding;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BookingDetailFragment extends Fragment {

    private FragmentBookingDetailBinding binding;
    private BookingDetailViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentBookingDetailBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BookingDetailViewModel.class);

        String bookingId = getArguments() != null ? getArguments().getString("bookingId") : null;
        if (bookingId != null)
            viewModel.loadBooking(bookingId);

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        viewModel.getBooking().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data == null)
                        return;
                    var b = resource.data;
                    binding.tvBookingCode.setText("Mã vé: " + b.getBookingCode());
                    binding.tvMovieTitle.setText(b.getMovieTitle());
                    binding.tvCinema.setText(b.getCinemaName());
                    binding.tvScreen.setText("Phòng: " + b.getScreenName());
                    binding.tvShowtime.setText(b.getStartTime());
                    binding.tvStatus.setText(b.getStatus());
                    binding.tvTotal.setText(String.format("Tổng: %,.0fđ", b.getTotalAmount()));

                    // Danh sách ghế
                    if (b.getSeats() != null) {
                        StringBuilder seats = new StringBuilder("Ghế: ");
                        for (var s : b.getSeats())
                            seats.append(s.getRowLabel()).append(s.getColNumber()).append("  ");
                        binding.tvSeats.setText(seats.toString().trim());
                    }

                    // Poster
                    Glide.with(this).load(b.getMoviePosterUrl())
                            .placeholder(R.drawable.ic_movie_placeholder).into(binding.ivPoster);

                    // QR Code
                    if (b.getQrCode() != null && !b.getQrCode().isEmpty()) {
                        generateQrCode(b.getQrCode());
                    }

                    // Trạng thái màu
                    int statusColor = switch (b.getStatus()) {
                        case "PAID" -> R.color.seat_available;
                        case "PENDING" -> R.color.tertiary;
                        case "CANCELLED", "EXPIRED" -> R.color.error;
                        default -> R.color.on_surface_variant;
                    };
                    binding.tvStatus.setTextColor(getResources().getColor(statusColor, null));
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void generateQrCode(String content) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(content, BarcodeFormat.QR_CODE, 400, 400);
            binding.ivQrCode.setImageBitmap(bitmap);
            binding.ivQrCode.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            binding.ivQrCode.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
