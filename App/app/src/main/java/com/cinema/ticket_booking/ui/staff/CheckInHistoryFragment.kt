package com.cinema.ticket_booking.ui.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.CheckInHistoryItemResponse
import com.cinema.ticket_booking.databinding.FragmentCheckInHistoryBinding
import com.cinema.ticket_booking.ui.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CheckInHistoryFragment : Fragment() {

    private var _binding: FragmentCheckInHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CheckInHistoryViewModel
    private lateinit var adapter: CheckInHistoryAdapter
    private lateinit var mainViewModel: MainViewModel

    private var isShowingToday = true // Mặc định tab "Hôm nay"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckInHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[CheckInHistoryViewModel::class.java]
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        setupRecyclerView() // thiết lập RecyclerView và Adapter
        setupTabs() // xử lý tab "Hôm nay" và "Tháng này"
        setupToolbar() // xử lý nút quay lại
        observeViewModel() // quan sát dữ liệu được tải về

        // Đọc filter từ: 1. Arguments (điều hướng cũ), 2. SharedViewModel (từ Dashboard card)
        handleFilterRequest()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationIcon(R.drawable.ic_back)
        binding.toolbar.setNavigationOnClickListener {
            // Pop backstack để quay lại màn hình trước
            if (!Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).popBackStack()) {
                // Không có gì để pop → quay về Dashboard qua BottomNav
                val bnvView = requireActivity().findViewById<View>(R.id.bottomNav)
                if (bnvView is BottomNavigationView) {
                    bnvView.selectedItemId = R.id.staffHomeFragment
                }
            }
        }
    }

    /**
     * Xử lý filter từ 2 nguồn:
     * 1. Arguments được truyền qua navigate() (flow cũ - dự phòng)
     * 2. MainViewModel.pendingHistoryFilter (flow mới từ Dashboard cards)
     */
    private fun handleFilterRequest() {
        // Đọc từ SharedViewModel TRƯỚC (ưu tiên cao hơn)
        val sharedFilter = mainViewModel.getPendingHistoryFilter().value
        if (sharedFilter != null) {
            applyFilter(sharedFilter)
            mainViewModel.consumeHistoryFilter() // Reset để không apply lại lần sau
            return
        }

        // Dự phòng: đọc từ Arguments
        arguments?.getString("filter")?.let {
            applyFilter(it)
        }

        // Observe nếu filter đến sau (trường hợp fragment đã được tạo trước)
        mainViewModel.getPendingHistoryFilter().observe(viewLifecycleOwner) { filter ->
            if (filter != null) {
                applyFilter(filter)
                mainViewModel.consumeHistoryFilter()
            }
        }
    }

    private fun applyFilter(filter: String) {
        if ("TODAY" == filter) {
            isShowingToday = true
            setTabSelected(true)
            showCurrentTab()
        } else if ("THIS_MONTH" == filter) {
            isShowingToday = false
            setTabSelected(false)
            showCurrentTab()
        }
    }

    private fun setupRecyclerView() {
        adapter = CheckInHistoryAdapter()
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter
    }

    private fun setupTabs() {
        setTabSelected(true) // Mặc định: tab Hôm nay active

        binding.tabToday.setOnClickListener {
            if (!isShowingToday) {
                isShowingToday = true
                setTabSelected(true)
                showCurrentTab()
            }
        }

        binding.tabThisMonth.setOnClickListener {
            if (isShowingToday) {
                isShowingToday = false
                setTabSelected(false)
                showCurrentTab()
            }
        }
    }

    private fun setTabSelected(todaySelected: Boolean) {
        if (todaySelected) {
            binding.tabToday.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_gold))
            binding.tabToday.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.tabThisMonth.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
            binding.tabThisMonth.setBackgroundResource(R.drawable.bg_tab_unselected)
        } else {
            binding.tabThisMonth.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_gold))
            binding.tabThisMonth.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.tabToday.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
            binding.tabToday.setBackgroundResource(R.drawable.bg_tab_unselected)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (java.lang.Boolean.TRUE == isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = error
                binding.tvError.postDelayed({
                    _binding?.tvError?.visibility = View.GONE
                }, 3000)
            }
        }

        viewModel.todayItems.observe(viewLifecycleOwner) { items ->
            if (isShowingToday) showList(items)
        }

        viewModel.monthItems.observe(viewLifecycleOwner) { items ->
            if (!isShowingToday) showList(items)
        }
    }

    private fun showCurrentTab() {
        if (isShowingToday) {
            showList(viewModel.todayItems.value)
        } else {
            showList(viewModel.monthItems.value)
        }
    }

    private fun showList(items: List<CheckInHistoryItemResponse>?) {
        if (items.isNullOrEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
            binding.tvEmpty.text = if (isShowingToday) {
                "Chưa có vé nào được soát hôm nay"
            } else {
                "Chưa có vé nào được soát trong tháng này"
            }
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvHistory.visibility = View.VISIBLE
            adapter.submitList(items)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
