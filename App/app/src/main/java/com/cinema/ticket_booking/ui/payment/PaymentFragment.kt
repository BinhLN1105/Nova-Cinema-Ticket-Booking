package com.cinema.ticket_booking.ui.payment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.repository.BookingRepository
import com.cinema.ticket_booking.databinding.FragmentPaymentBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!
    private var bookingId: String? = null

    @Inject
    lateinit var bookingRepository: BookingRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            bookingId = it.getString("bookingId")
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Thoát cực gắt: Xóa toàn bộ stack và nhảy thẳng về Home
                    Navigation.findNavController(view).popBackStack(R.id.homeFragment, false)
                }
            }
        )

        binding.btnBack.setOnClickListener {
            Navigation.findNavController(view).popBackStack(R.id.homeFragment, false)
        }

        // Tạo payment URL rồi load WebView
        val id = bookingId ?: return
        bookingRepository.createPayment(id).observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.LOADING -> binding.progressBar.visibility = View.VISIBLE
                Resource.Status.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    if (resource.data != null && resource.data.paymentUrl != null) {
                        setupWebView(view, resource.data.paymentUrl)
                    } else {
                        SnackbarHelper.showError(binding.root, "Lỗi: Không nhận được link thanh toán")
                    }
                }
                Resource.Status.ERROR -> {
                    binding.progressBar.visibility = View.GONE
                    SnackbarHelper.showError(binding.root, "Lỗi kết nối: ${resource.message}")
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(view: View, url: String) {
        binding.webView.visibility = View.VISIBLE
        binding.webView.setBackgroundColor(android.graphics.Color.WHITE)

        val settings = binding.webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportMultipleWindows(false)
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.userAgentString = "${settings.userAgentString} CineNoirApp/1.0"

        // Log URL for debugging
        android.util.Log.d("PaymentFragment", "Loading VNPay URL: $url")

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(wv: WebView, progress: Int) {
                binding.progressBar.visibility = if (progress < 100) View.VISIBLE else View.GONE
            }
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(wv: WebView, req: WebResourceRequest): Boolean {
                val loadUrl = req.url.toString()
                android.util.Log.d("PaymentFragment", "Navigating: $loadUrl")

                // Catch deep link scheme to finalize payment flow ONLY AFTER backend redirects
                if (loadUrl.startsWith("cinema://payment")) {
                    val status = req.url.getQueryParameter("vnp_ResponseCode")
                    view.postDelayed({
                        if ("00" == status) {
                            // Success
                            val args = Bundle().apply {
                                putString("bookingId", bookingId)
                                putBoolean("paymentSuccess", true)
                            }
                            try {
                                Navigation.findNavController(view)
                                    .navigate(R.id.action_payment_to_bookingDetail, args)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            // Cancelled or Failed
                            SnackbarHelper.showError(binding.root, "Giao dịch đã bị hủy hoặc thất bại")
                            try {
                                Navigation.findNavController(view).popBackStack()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }, 300) // Đợi 300ms để WebView hoàn tất xử lý trước khi đóng Fragment
                    return true
                }

                if (loadUrl.startsWith("http://") || loadUrl.startsWith("https://")) {
                    return false
                }

                try {
                    val intent = Intent.parseUri(loadUrl, Intent.URI_INTENT_SCHEME)
                    if (intent != null) {
                        view.context.startActivity(intent)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PaymentFragment", "Cannot handle intent or app not installed: $loadUrl")
                }
                return true
            }

            override fun onPageFinished(wv: WebView, url: String) {
                android.util.Log.d("PaymentFragment", "Page finished: $url")
                binding.progressBar.visibility = View.GONE

                // Tiêm JS để cố gắng ẩn thanh Header/Nút back của VNPay Web (nếu có)
                wv.evaluateJavascript(
                    "try {" +
                            "   var headers = document.querySelectorAll('header, .header, .nav, .navbar');" +
                            "   for (var i = 0; i < headers.length; i++) {" +
                            "       headers[i].style.display = 'none';" +
                            "   }" +
                            "   var backs = document.querySelectorAll('[class*=\"back\" i], [id*=\"back\" i]');" +
                            "   for (var i = 0; i < backs.length; i++) {" +
                            "       backs[i].style.display = 'none';" +
                            "   }" +
                            "} catch(e) {}",
                    null
                )
            }

            override fun onReceivedError(wv: WebView, req: WebResourceRequest, error: WebResourceError) {
                if (req.isForMainFrame) {
                    android.util.Log.e("PaymentFragment", "WebView error: ${error.description}")
                    binding.progressBar.visibility = View.GONE
                    SnackbarHelper.showError(
                        binding.root,
                        "Không thể tải trang thanh toán. Vui lòng kiểm tra kết nối mạng."
                    )
                }
            }

            override fun onReceivedSslError(wv: WebView, handler: SslErrorHandler, error: android.net.http.SslError) {
                android.util.Log.w("PaymentFragment", "SSL error: $error")
                handler.proceed() // VNPay sandbox may have SSL issues
            }
        }

        binding.webView.loadUrl(url)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
