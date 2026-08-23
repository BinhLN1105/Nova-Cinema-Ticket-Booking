package com.cinema.ticket_booking.ui.chatbot

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.databinding.DialogChatbotBinding
import com.cinema.ticket_booking.ui.MainActivity
import com.cinema.ticket_booking.util.SnackbarHelper
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
    private lateinit var suggestionAdapter: SuggestionChipAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialog)
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

        setupRecyclerView()
        setupSuggestionChips()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ChatbotUiHelper.createChatAdapter(
            viewModel = viewModel,
            isViewActive = { _binding != null },
            rootView = { binding.root },
            onNavigateToConfirm = { bundle ->
                dismiss()
                try {
                    findNavController().navigate(R.id.confirmBookingFragment, bundle)
                } catch (e: Exception) {
                    (activity as? MainActivity)?.let { act ->
                        val navHost = act.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                        navHost?.navController?.navigate(R.id.confirmBookingFragment, bundle)
                    }
                }
            }
        )
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChat.adapter = adapter
    }

    private fun setupSuggestionChips() {
        suggestionAdapter = ChatbotUiHelper.createSuggestionAdapter(viewModel)
        binding.rvSuggestions.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSuggestions.adapter = suggestionAdapter
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val msg = binding.etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                viewModel.sendMessage(msg)
                binding.etMessage.setText("")
            }
        }

        binding.btnClearChat.setOnClickListener {
            viewModel.clearChatSession {
                SnackbarHelper.showSuccess(binding.root, "Đã làm mới cuộc hội thoại")
            }
        }

        binding.btnClose.setOnClickListener { dismiss() }
    }

    private fun observeViewModel() {
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.rvChat.smoothScrollToPosition(messages.size - 1)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val d = dialogInterface as BottomSheetDialog
            val bottomSheet = d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior = BottomSheetBehavior.from<View>(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
