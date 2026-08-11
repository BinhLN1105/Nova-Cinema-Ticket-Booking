package com.cinema.ticket_booking.ui.chatbot

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.cinema.ticket_booking.databinding.DialogChatbotBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatbotBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogChatbotBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ChatbotViewModel
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Nova Optimization: use transparent background for modern rounded corners
        setStyle(STYLE_NORMAL, com.cinema.ticket_booking.R.style.TransparentBottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ChatbotViewModel::class.java]

        adapter = ChatAdapter()
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChat.adapter = adapter

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.rvChat.smoothScrollToPosition(messages.size - 1)
            }
        }

        binding.btnSend.setOnClickListener {
            val msg = binding.etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                viewModel.sendMessage(msg)
                binding.etMessage.setText("")
            }
        }

        binding.btnClose.setOnClickListener { dismiss() }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        // Cấu hình hành vi (Behavior) khi Dialog được hiển thị
        dialog.setOnShowListener { dialogInterface ->
            val d = dialogInterface as BottomSheetDialog
            val bottomSheet = d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior = BottomSheetBehavior.from<View>(bottomSheet)
                // Ép buộc BottomSheet luôn ở trạng thái Mở rộng toàn bộ (Expanded)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                // Bỏ qua trạng thái lấp lửng (Collapsed) để không bị kẹt khi vuốt xuống
                behavior.skipCollapsed = true

                // Thiết lập chiều cao cho khớp với màn hình (Match Parent)
                bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }

        // Cấu hình chế độ bàn phím: adjustResize giúp đẩy giao diện lên khi bàn phím hiện ra
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
