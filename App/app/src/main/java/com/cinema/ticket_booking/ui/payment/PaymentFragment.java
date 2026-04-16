package com.cinema.ticket_booking.ui.payment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.*;
import android.webkit.*;
import com.cinema.ticket_booking.util.SnackbarHelper;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.repository.BookingRepository;
import com.cinema.ticket_booking.databinding.FragmentPaymentBinding;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import androidx.activity.OnBackPressedCallback;

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

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding != null && binding.webView.canGoBack()) {
                    binding.webView.goBack();
                } else {
                    Navigation.findNavController(view).popBackStack();
                }
            }
        });

        binding.btnBack.setOnClickListener(v -> {
            boolean handled = false;
            if (binding.webView.canGoBack()) {
                binding.webView.goBack();
                handled = true;
            }
            if (!handled) {
                Navigation.findNavController(view).popBackStack();
            }
        });

        // Tạo payment URL rồi load WebView
        bookingRepository.createPayment(bookingId).observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data != null && resource.data.getPaymentUrl() != null) {
                        setupWebView(view, resource.data.getPaymentUrl());
                    } else {
                        SnackbarHelper.showError(binding.getRoot(), "Lỗi: Không nhận được link thanh toán");
                    }
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    SnackbarHelper.showError(binding.getRoot(), "Lỗi kết nối: " + resource.message);
                }
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(View view, String url) {
        binding.webView.setVisibility(View.VISIBLE);
        binding.webView.setBackgroundColor(android.graphics.Color.WHITE);

        WebSettings settings = binding.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setUserAgentString(settings.getUserAgentString()
                + " CineNoirApp/1.0");

        // Log URL for debugging
        android.util.Log.d("PaymentFragment", "Loading VNPay URL: " + url);

        binding.webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView wv, int progress) {
                if (progress < 100) {
                    binding.progressBar.setVisibility(View.VISIBLE);
                } else {
                    binding.progressBar.setVisibility(View.GONE);
                }
            }
        });

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView wv, WebResourceRequest req) {
                String loadUrl = req.getUrl().toString();
                android.util.Log.d("PaymentFragment", "Navigating: " + loadUrl);
                
                // Catch deep link scheme to finalize payment flow ONLY AFTER backend redirects
                if (loadUrl.startsWith("cinema://payment")) {
                    String status = req.getUrl().getQueryParameter("vnp_ResponseCode");
                    view.postDelayed(() -> {
                        if ("00".equals(status)) {
                            // Success
                            Bundle args = new Bundle();
                            args.putString("bookingId", bookingId);
                            args.putBoolean("paymentSuccess", true);
                            try {
                                Navigation.findNavController(view)
                                        .navigate(R.id.action_payment_to_bookingDetail, args);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            // Cancelled or Failed
                            SnackbarHelper.showError(binding.getRoot(), "Giao dịch đã bị hủy hoặc thất bại");
                            try {
                                Navigation.findNavController(view).popBackStack();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, 300); // Đợi 300ms để WebView hoàn tất xử lý trước khi đóng Fragment
                    return true;
                }

                if (loadUrl.startsWith("http://") || loadUrl.startsWith("https://")) {
                    return false;
                }

                try {
                    android.content.Intent intent = android.content.Intent.parseUri(loadUrl, android.content.Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        view.getContext().startActivity(intent);
                    }
                } catch (Exception e) {
                    android.util.Log.e("PaymentFragment", "Cannot handle intent or app not installed: " + loadUrl);
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView wv, String url) {
                android.util.Log.d("PaymentFragment", "Page finished: " + url);
                binding.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView wv, WebResourceRequest req, WebResourceError error) {
                if (req.isForMainFrame()) {
                    android.util.Log.e("PaymentFragment", "WebView error: " + error.getDescription());
                    binding.progressBar.setVisibility(View.GONE);
                    SnackbarHelper.showError(binding.getRoot(),
                            "Không thể tải trang thanh toán. Vui lòng kiểm tra kết nối mạng.");
                }
            }

            @Override
            public void onReceivedSslError(WebView wv, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                android.util.Log.w("PaymentFragment", "SSL error: " + error.toString());
                handler.proceed(); // VNPay sandbox may have SSL issues
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

