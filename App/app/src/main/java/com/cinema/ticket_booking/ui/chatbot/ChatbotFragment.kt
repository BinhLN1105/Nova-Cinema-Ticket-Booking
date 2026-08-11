package com.cinema.ticket_booking.ui.chatbot

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.cinema.ticket_booking.databinding.FragmentChatbotBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ChatbotViewModel
    private lateinit var adapter: ChatAdapter

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
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true
        binding.rvChat.layoutManager = layoutManager
        binding.rvChat.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { v -> Navigation.findNavController(v).navigateUp() }

        binding.btnSend.setOnClickListener {
            val msg = binding.etMessage.text.toString().trim()
            if (!TextUtils.isEmpty(msg)) {
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
