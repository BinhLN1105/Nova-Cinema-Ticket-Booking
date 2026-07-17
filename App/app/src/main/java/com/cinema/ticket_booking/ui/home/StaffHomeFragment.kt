package com.cinema.ticket_booking.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.UserResponse
import com.cinema.ticket_booking.databinding.FragmentStaffHomeBinding
import com.cinema.ticket_booking.ui.MainViewModel
import com.cinema.ticket_booking.ui.staff.UpcomingShowtimeAdapter
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class StaffHomeFragment : Fragment() {

    private var _binding: FragmentStaffHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: StaffHomeViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var upcomingAdapter: UpcomingShowtimeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[StaffHomeViewModel::class.java]
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        setupUpcomingRecyclerView()
        setupDate()
        setupNavigation()
        observeViewModel()

        binding.btnRefresh.setOnClickListener { viewModel.refresh() }
    }

    private fun setupNavigation() {
        // ── 1. Suất chiếu hôm nay → CinemaDetailFragment ──────────────────────────
        // Dùng flag để tránh navigate nhiều lần nếu user click liên tục
        binding.cardShowtimes.setOnClickListener {
            // Disable ngay để chặn double-click
            binding.cardShowtimes.isEnabled = false

            val cached = mainViewModel.getUserProfile().value
            if (cached != null && cached.isSuccess && cached.data != null) {
                navigateToCinemaDetail(cached.data!!.cinemaId, cached.data!!.cinemaName)
                binding.cardShowtimes.isEnabled = true
            } else {
                // Chưa có data → load rồi chờ 1 lần
                mainViewModel.loadUserProfile()
                mainViewModel.getUserProfile().observe(viewLifecycleOwner, object : androidx.lifecycle.Observer<Resource<UserResponse>?> {
                    override fun onChanged(value: Resource<UserResponse>?) {
                        val resource = value ?: return
                        if (resource.isSuccess && resource.data != null) {
                            mainViewModel.getUserProfile().removeObserver(this)
                            navigateToCinemaDetail(resource.data!!.cinemaId, resource.data!!.cinemaName)
                            if (_binding != null) {
                                binding.cardShowtimes.isEnabled = true
                            }
                        } else if (resource.isError) {
                            mainViewModel.getUserProfile().removeObserver(this)
                            Toast.makeText(requireContext(), "Không thể tải thông tin rạp", Toast.LENGTH_SHORT).show()
                            if (_binding != null) {
                                binding.cardShowtimes.isEnabled = true
                            }
                        }
                    }
                })
            }
        }

        // ── 2. Vé hôm nay → History tab TODAY ────────────────────────────────────
        // Dùng BottomNav.setSelectedItemId để NavigationUI tự xử lý backstack,
        // tránh xung đột khi user muốn quay lại Dashboard từ menu.
        binding.cardCheckedToday.setOnClickListener {
            mainViewModel.requestHistoryTab("TODAY")
            val bnvToday = requireActivity().findViewById<View>(R.id.bottomNav)
            if (bnvToday is com.google.android.material.bottomnavigation.BottomNavigationView) {
                bnvToday.selectedItemId = R.id.checkInHistoryFragment
            }
        }

        // ── 3. Vé tháng này → History tab THIS_MONTH ─────────────────────────────
        binding.cardCheckedMonth.setOnClickListener {
            mainViewModel.requestHistoryTab("THIS_MONTH")
            val bnvView = requireActivity().findViewById<View>(R.id.bottomNav)
            if (bnvView is com.google.android.material.bottomnavigation.BottomNavigationView) {
                bnvView.selectedItemId = R.id.checkInHistoryFragment
            }
        }
    }

    private fun navigateToCinemaDetail(cinemaId: String?, cinemaName: String?) {
        if (!isAdded || _binding == null) return
        if (cinemaId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Nhân viên chưa được phân công rạp!", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val args = Bundle().apply {
                putString("cinemaId", cinemaId)
                putString("cinemaName", cinemaName)
            }
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(R.id.action_staffHome_to_cinemaDetail, args)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi điều hướng: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUpcomingRecyclerView() {
        upcomingAdapter = UpcomingShowtimeAdapter()
        binding.rvUpcomingShowtimes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUpcomingShowtimes.adapter = upcomingAdapter
        binding.rvUpcomingShowtimes.isNestedScrollingEnabled = false
    }

    private fun setupDate() {
        val sdf = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi", "VN"))
        binding.tvDate.text = sdf.format(Date())
    }

    private fun observeViewModel() {
        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            if (stats != null) {
                animateNumber(binding.tvShowtimesValue, stats.totalShowtimesToday)
                animateNumber(binding.tvCheckedTodayValue, stats.ticketsCheckedToday)
                animateNumber(binding.tvCheckedMonthValue, stats.ticketsCheckedThisMonth)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (java.lang.Boolean.TRUE == isLoading) {
                binding.tvShowtimesValue.text = "--"
                binding.tvCheckedTodayValue.text = "--"
                binding.tvCheckedMonthValue.text = "--"
            }
        }

        viewModel.upcomingShowtimes.observe(viewLifecycleOwner) { list ->
            val hasItems = !list.isNullOrEmpty()
            binding.tvUpcomingSubtitle.visibility = if (hasItems) View.VISIBLE else View.GONE
            if (!hasItems) {
                binding.tvUpcomingEmpty.visibility = View.VISIBLE
                binding.rvUpcomingShowtimes.visibility = View.GONE
            } else {
                binding.tvUpcomingEmpty.visibility = View.GONE
                binding.rvUpcomingShowtimes.visibility = View.VISIBLE
                upcomingAdapter.submitList(list)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        mainViewModel.getUserProfile().observe(viewLifecycleOwner) { resource ->
            if (resource != null && resource.isSuccess && resource.data != null) {
                binding.tvWelcome.text = "Xin chào, ${resource.data!!.fullName}! 👋"
                if (!resource.data!!.cinemaName.isNullOrEmpty()) {
                    binding.tvCinemaName.text = "Rạp: ${resource.data!!.cinemaName}"
                } else {
                    binding.tvCinemaName.text = "Rạp: Chưa phân công"
                }
            }
        }
    }

    private fun animateNumber(tv: TextView, newValue: Long) {
        val currentValue: Long = try {
            val current = tv.text.toString().trim()
            if ("--" == current) -1 else current.toLong()
        } catch (e: NumberFormatException) {
            -1
        }

        if (currentValue != newValue) {
            tv.animate().alpha(0.3f).setDuration(150).withEndAction {
                tv.text = newValue.toString()
                tv.animate().alpha(1f).setDuration(200).start()
            }.start()
        } else {
            tv.text = newValue.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
