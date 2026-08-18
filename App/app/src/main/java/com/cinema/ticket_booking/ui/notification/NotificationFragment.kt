package com.cinema.ticket_booking.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cinema.ticket_booking.databinding.FragmentNotificationBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NotificationViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[NotificationViewModel::class.java]

        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.btnBack.setOnClickListener { v -> Navigation.findNavController(view).popBackStack() }
        binding.btnMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead()
            SnackbarHelper.showSuccess(binding.root, "Đã đánh dấu tất cả là đã đọc")
        }

        viewModel.notifications.observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.LOADING -> binding.progressBar.visibility = View.VISIBLE
                Resource.Status.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    val content = resource.data?.content
                    if (content.isNullOrEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        val adapter = NotificationAdapter(content.toMutableList())
                        binding.rvNotifications.adapter = adapter
                        setupSwipeToDelete(adapter)
                    }
                }
                Resource.Status.ERROR -> {
                    binding.progressBar.visibility = View.GONE
                    SnackbarHelper.showError(binding.root, resource.message ?: "Có lỗi xảy ra")
                }
            }
        }
    }

    private fun setupSwipeToDelete(adapter: NotificationAdapter) {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                t: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val pos = vh.adapterPosition
                val item = adapter.getItem(pos)

                // Tạm thời xóa khỏi UI
                adapter.removeItem(pos)

                // Hiện Snackbar với nút Undo
                val snackbar = Snackbar.make(binding.root, "Đã xóa thông báo", Snackbar.LENGTH_LONG)
                val undoPressed = booleanArrayOf(false)

                snackbar.setAction("Hoàn tác") {
                    undoPressed[0] = true
                    // Tải lại dữ liệu (đơn giản nhất) hoặc insert lại adapter
                    viewModel.notifications.observe(viewLifecycleOwner) { r ->
                        if (r.status == Resource.Status.SUCCESS) {
                            val content = r.data?.content
                            val list = content?.toMutableList() ?: mutableListOf()
                            binding.rvNotifications.adapter = NotificationAdapter(list)
                        }
                    }
                }

                snackbar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                        if (!undoPressed[0]) {
                            // Nếu không Undo thì mới gọi API xóa thật
                            item.id?.toString()?.let { viewModel.deleteNotification(it) }
                        }
                    }
                })

                snackbar.show()
            }
        }).attachToRecyclerView(binding.rvNotifications)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
