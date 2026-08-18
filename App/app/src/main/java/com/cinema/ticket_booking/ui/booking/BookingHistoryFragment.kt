package com.cinema.ticket_booking.ui.booking

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.local.TokenManager
import com.cinema.ticket_booking.data.model.response.ApiResponse
import com.cinema.ticket_booking.data.model.response.BookingSummary
import com.cinema.ticket_booking.databinding.FragmentBookingHistoryBinding
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.ui.MainViewModel
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class BookingHistoryFragment : Fragment() {

    private var _binding: FragmentBookingHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BookingHistoryViewModel
    private val allBookings = ArrayList<BookingSummary>()
    private var isUpcomingTab = true

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var apiService: ApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // If not logged in, show login prompt BEFORE creating ViewModel
        if (!tokenManager.isLoggedIn) {
            binding.rvBookings.visibility = View.GONE
            binding.tvEmpty.visibility = View.GONE
            binding.layoutLoginPrompt.visibility = View.VISIBLE
            binding.swipeRefresh.isEnabled = false
            binding.btnLoginPrompt.setOnClickListener {
                Navigation.findNavController(view).navigate(R.id.loginFragment)
            }
            return
        }

        // Only create ViewModel (which auto-loads data) AFTER login check
        viewModel = ViewModelProvider(this)[BookingHistoryViewModel::class.java]

        // Handle redirection from Profile via ViewModel
        val mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        mainViewModel.getPendingBookingTab().observe(viewLifecycleOwner) { tab ->
            if ("HISTORY" == tab) {
                isUpcomingTab = false
                viewModel.isUpcomingTab = false
                updateTabStyles()
                updateList()
                mainViewModel.consumeBookingTab() // Clear intent
            }
        }

        // Handle redirection from Profile via Arguments (legacy support)
        arguments?.let {
            if ("HISTORY" == it.getString("MapsToTab")) {
                isUpcomingTab = false
                viewModel.isUpcomingTab = false
            }
        }

        // Restore tab state from ViewModel
        this.isUpcomingTab = viewModel.isUpcomingTab
        updateTabStyles()

        binding.rvBookings.layoutManager = LinearLayoutManager(requireContext())

        binding.tabUpcoming.setOnClickListener {
            if (isUpcomingTab) return@setOnClickListener
            isUpcomingTab = true
            viewModel.isUpcomingTab = true // Persist
            updateTabStyles()
            updateList()
        }

        binding.tabHistory.setOnClickListener {
            if (!isUpcomingTab) return@setOnClickListener
            isUpcomingTab = false
            viewModel.isUpcomingTab = false // Persist
            updateTabStyles()
            updateList()
        }

        binding.nestedScrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                if (scrollY == (v.getChildAt(0).measuredHeight - v.measuredHeight)) {
                    viewModel.loadMore()
                }
            }
        )

        viewModel.bookings.observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.LOADING -> {
                    if (viewModel.isLoadingMore()) {
                        binding.progressBarLoadMore.visibility = View.VISIBLE
                    } else {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
                Resource.Status.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    binding.progressBarLoadMore.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    allBookings.clear()
                    resource.data?.content?.let {
                        allBookings.addAll(it)
                    }
                    updateList()
                }
                Resource.Status.ERROR -> {
                    binding.progressBar.visibility = View.GONE
                    binding.progressBarLoadMore.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    SnackbarHelper.showError(binding.root, resource.message ?: "Lỗi tải lịch sử đặt vé")
                }
            }
        }

        binding.btnBrowseMovies.setOnClickListener {
            Navigation.findNavController(view).popBackStack(R.id.homeFragment, false)
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
    }

    private fun updateTabStyles() {
        val colorPrimary = resources.getColor(R.color.primary, null)
        val colorOnSurfaceVariant = resources.getColor(R.color.on_surface_variant, null)
        val bgActive = ColorStateList.valueOf(resources.getColor(R.color.surface_container_highest, null))
        val bgInactive = ColorStateList.valueOf(resources.getColor(android.R.color.transparent, null))

        binding.tabUpcoming.setTextColor(if (isUpcomingTab) colorPrimary else colorOnSurfaceVariant)
        binding.tabUpcoming.backgroundTintList = if (isUpcomingTab) bgActive else bgInactive

        binding.tabHistory.setTextColor(if (!isUpcomingTab) colorPrimary else colorOnSurfaceVariant)
        binding.tabHistory.backgroundTintList = if (!isUpcomingTab) bgActive else bgInactive
    }

    private fun updateList() {
        val filtered = ArrayList<BookingSummary>()
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val sdf2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        for (b in allBookings) {
            var isFuture = true
            try {
                if (b.startTime != null) {
                    var d: Date?
                    try {
                        d = sdf.parse(b.startTime)
                    } catch (e: Exception) {
                        d = sdf2.parse(b.startTime)
                    }
                    if (d != null) {
                        isFuture = d.time > now
                    }
                }
            } catch (e: Exception) {
            }

            // Kiểm tra vé PENDING đã quá hạn thanh toán chưa
            var isPendingExpired = false
            if ("PENDING".equals(b.status, ignoreCase = true) && b.expiresAt != null) {
                try {
                    var expDate: Date?
                    try {
                        expDate = sdf.parse(b.expiresAt)
                    } catch (e2: Exception) {
                        expDate = sdf2.parse(b.expiresAt)
                    }
                    if (expDate != null && expDate.time <= now) {
                        isPendingExpired = true
                    }
                } catch (ignored: Exception) {
                }
            }

            // Vé PENDING còn hạn → tab Sắp chiếu (để user thanh toán)
            // Vé PENDING quá hạn → tab Lịch sử
            // Vé CANCELLED/PAID/CHECKED_IN với giờ chiếu tương lai → tab Sắp chiếu
            val isUpcoming = if ("PENDING".equals(b.status, ignoreCase = true)) {
                !isPendingExpired
            } else {
                ("PAID".equals(b.status, ignoreCase = true) ||
                        "CHECKED_IN".equals(b.status, ignoreCase = true) ||
                        "CANCELLED".equals(b.status, ignoreCase = true)) && isFuture
            }

            if (isUpcomingTab == isUpcoming) {
                filtered.add(b)
            }
        }

        if (filtered.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvBookings.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvBookings.visibility = View.VISIBLE
            binding.rvBookings.adapter = BookingHistoryAdapter(
                filtered,
                object : BookingHistoryAdapter.Listener {
                    override fun onClick(bookingId: String) {
                        val args = Bundle().apply {
                            putString("bookingId", bookingId)
                        }
                        if (view != null) {
                            Navigation.findNavController(requireView()).navigate(R.id.action_history_to_bookingDetail, args)
                        }
                    }

                    override fun onPayClick(bookingId: String) {
                        val args = Bundle().apply {
                            putString("bookingId", bookingId)
                        }
                        if (view != null) {
                            Navigation.findNavController(requireView()).navigate(R.id.action_history_to_payment, args)
                        }
                    }

                    override fun onReviewClick(s: BookingSummary) {
                        val args = Bundle().apply {
                            putString("movieId", s.movieId)
                            putString("movieTitle", s.movieTitle)
                            putString("bookingId", s.id)
                        }
                        if (view != null) {
                            Navigation.findNavController(requireView()).navigate(R.id.action_history_to_writeReview, args)
                        }
                    }

                    override fun onCancelClick(s: BookingSummary) {
                        showCancelConfirmDialog(s)
                    }
                }
            )
        }
    }

    private fun showCancelConfirmDialog(s: BookingSummary) {
        binding.progressBar.visibility = View.VISIBLE
        // Lấy policy huỷ từ API
        apiService.getCancelPolicy().enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(
                call: Call<ApiResponse<Map<String, Any>>>,
                response: Response<ApiResponse<Map<String, Any>>>
            ) {
                if (!isAdded) return
                binding.progressBar.visibility = View.GONE

                var refundPercent = 100 // default
                if (response.isSuccessful && response.body()?.data != null) {
                    try {
                        val percentObj = response.body()?.data?.get("refundPercent")
                        if (percentObj is Number) {
                            refundPercent = percentObj.toInt()
                        } else if (percentObj is String) {
                            refundPercent = percentObj.toInt()
                        }
                    } catch (ignored: Exception) {
                    }
                }

                // Hiển thị dialog
                val totalFormatted = String.format("%,.0f", s.totalAmount)

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Hủy vé xem phim")
                    .setMessage(
                        "Bạn chắc chắn muốn hủy vé \"${s.movieTitle}\"?\n\n" +
                                "Hệ thống sẽ hoàn lại $refundPercent% giá trị vé ($totalFormatted" + "đ) " +
                                "thành điểm CP (Cinema Points) vào tài khoản của bạn theo quy định.\n\n" +
                                "⚠️ Thao tác này không thể hoàn tác."
                    )
                    .setNegativeButton("Hủy bỏ", null)
                    .setPositiveButton("Đồng ý hủy vé") { _, _ ->
                        s.id?.let { executeCancelBooking(it) }
                    }
                    .show()
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                if (!isAdded) return
                binding.progressBar.visibility = View.GONE
                SnackbarHelper.showError(binding.root, "Không thể tải chính sách hoàn vé.")
            }
        })
    }

    private fun executeCancelBooking(bookingId: String) {
        binding.progressBar.visibility = View.VISIBLE

        apiService.cancelRequest(bookingId).enqueue(object : Callback<ApiResponse<Void>> {
            override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {
                if (!isAdded) return
                binding.progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    SnackbarHelper.showSuccess(
                        binding.root,
                        "Yêu cầu hủy đã được gửi! Vui lòng kiểm tra email để xác nhận."
                    )
                    viewModel.refresh()
                } else {
                    var msg = "Gửi yêu cầu hủy vé thất bại"
                    try {
                        val errorBody = response.errorBody()?.string()
                        if (errorBody != null && errorBody.contains("message")) {
                            var parsedMsg = errorBody.substring(errorBody.indexOf("message") + 10)
                            parsedMsg = parsedMsg.substring(0, parsedMsg.indexOf("\""))
                            msg = parsedMsg
                        }
                    } catch (ignored: Exception) {
                    }
                    SnackbarHelper.showError(binding.root, msg)
                }
            }

            override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                if (!isAdded) return
                binding.progressBar.visibility = View.GONE
                SnackbarHelper.showError(binding.root, "Lỗi kết nối: ${t.message}")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.refresh()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
