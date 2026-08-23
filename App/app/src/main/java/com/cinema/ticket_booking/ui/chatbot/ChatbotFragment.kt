package com.cinema.ticket_booking.ui.chatbot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.databinding.FragmentChatbotBinding
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ChatbotViewModel
    private lateinit var adapter: ChatAdapter
    private lateinit var suggestionAdapter: SuggestionChipAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
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
        adapter = ChatAdapter { draftId, position ->
            adapter.setDraftConfirmingState(position, true)
            viewModel.prepareDraftBooking(draftId) { isSuccess, errorMessage, quote ->
                if (_binding != null) {
                    adapter.setDraftConfirmingState(position, false)
                }
                if (isSuccess && quote != null) {
                    val bundle = Bundle().apply {
                        putParcelable("initialQuote", quote)
                        putLong("expireTime", System.currentTimeMillis() + 10 * 60 * 1000)
                    }
                    findNavController().navigate(R.id.confirmBookingFragment, bundle)
                } else {
                    val msg = errorMessage ?: "Đơn vé nháp không tồn tại hoặc đã hết hạn (tối đa 10 phút)."
                    SnackbarHelper.showError(binding.root, msg)
                }
            }
        }

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true
        binding.rvChat.layoutManager = layoutManager
        binding.rvChat.adapter = adapter
    }

    private fun setupSuggestionChips() {
        suggestionAdapter = SuggestionChipAdapter(SuggestionChipAdapter.getDefaultPrompts()) { prompt ->
            viewModel.sendMessage(prompt.queryText)
        }
        binding.rvSuggestions.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSuggestions.adapter = suggestionAdapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnClearChat.setOnClickListener {
            viewModel.clearChatSession {
                SnackbarHelper.showSuccess(binding.root, "Đã làm mới cuộc hội thoại")
            }
        }

        binding.btnSend.setOnClickListener {
            val msg = binding.etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                viewModel.sendMessage(msg)
                binding.etMessage.setText("")
            }
        }
    }

    private fun observeViewModel() {
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.rvChat.smoothScrollToPosition(messages.size - 1)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
