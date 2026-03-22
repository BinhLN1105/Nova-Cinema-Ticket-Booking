package com.cinema.ticket_booking.ui.payment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.*;
import android.webkit.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.repository.BookingRepository;
import com.cinema.ticket_booking.databinding.FragmentPaymentBinding;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PaymentFragment extends Fragment {

    private FragmentPaymentBinding binding;
    private String bookingId;

    @Inject
    BookingRepository bookingRepository;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentPaymentBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null)
            bookingId = getArguments().getString("bookingId");

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        // Tạo payment URL rồi load WebView
        bookingRepository.createPayment(bookingId).observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data != null && resource.data.getPaymentUrl() != null) {
                        setupWebView(view, resource.data.getPaymentUrl());
                    }
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(View view, String url) {
        binding.webView.setVisibility(View.VISIBLE);
        WebSettings settings = binding.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView wv, WebResourceRequest req) {
                String loadUrl = req.getUrl().toString();
                // Deep link callback từ VNPay: cinema://payment/result
                if (loadUrl.startsWith("cinema://payment")) {
                    String status = req.getUrl().getQueryParameter("vnp_ResponseCode");
                    boolean success = "00".equals(status);
                    Bundle args = new Bundle();
                    args.putString("bookingId", bookingId);
                    args.putBoolean("paymentSuccess", success);
                    Navigation.findNavController(view)
                            .navigate(R.id.action_payment_to_bookingDetail, args);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView wv, String url) {
                binding.progressBar.setVisibility(View.GONE);
            }
        });

        binding.webView.loadUrl(url);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
