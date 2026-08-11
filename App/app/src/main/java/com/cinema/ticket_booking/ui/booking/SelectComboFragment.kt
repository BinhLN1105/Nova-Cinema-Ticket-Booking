package com.cinema.ticket_booking.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.BookingResponse
import com.cinema.ticket_booking.databinding.FragmentSelectComboBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class SelectComboFragment : Fragment() {

    private var _binding: FragmentSelectComboBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SelectComboViewModel
    private var adapter: ComboAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectComboBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SelectComboViewModel::class.java]

        binding.btnBack.setOnClickListener {
            Navigation.findNavController(view).popBackStack()
        }
        binding.tvMovieTitle.text = SelectShowtimeViewModel.pendingMovieTitle

        setupRecyclerView()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        binding.rvCombos.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeViewModel() {
        viewModel.combos.observe(viewLifecycleOwner) { resource ->
            binding.progressBar.visibility = if (resource.isLoading) View.VISIBLE else View.GONE
            if (resource.isSuccess && resource.data != null) {
                adapter = ComboAdapter(
                    resource.data,
                    { comboId ->
                        viewModel.addCombo(comboId)
                        updateItemSelectedCount()
                    },
                    { comboId ->
                        viewModel.removeCombo(comboId)
                        updateItemSelectedCount()
                    },
                    viewModel.selectedCombos
                )
                binding.rvCombos.adapter = adapter
                updateItemSelectedCount()
            } else if (resource.isError) {
                SnackbarHelper.showError(binding.root, resource.message ?: "Lỗi tải combo")
            }
        }

        // Instant Total Calculation (Optimistic UI)
        viewModel.localTotal.observe(viewLifecycleOwner) { total ->
            binding.tvTotal.text = String.format(Locale.getDefault(), "%,.0fđ", total)
        }
    }

    private fun setupClickListeners() {
        binding.btnSkip.setOnClickListener { handleContinue() }
        binding.btnContinue.setOnClickListener { handleContinue() }
    }

    private fun handleContinue() {
        // Show loading state for the final transition quote
        showLoading(true)

        viewModel.getFinalQuote().observe(viewLifecycleOwner) { resource ->
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                navigateToConfirm(resource.data)
            } else if (resource.status == Resource.Status.ERROR) {
                showLoading(false)
                SnackbarHelper.showError(
                    binding.root,
                    resource.message ?: "Lỗi tính toán giá cuối cùng"
                )
            }
        }
    }

    private fun navigateToConfirm(quote: BookingResponse) {
        val bundle = Bundle().apply {
            arguments?.let {
                putLong("expireTime", it.getLong("expireTime"))
            }
            putParcelable("initialQuote", quote)
        }

        NavHostFragment.findNavController(this)
            .navigate(R.id.action_selectCombo_to_confirmBooking, bundle)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnContinue.isEnabled = !isLoading
        binding.btnSkip.isEnabled = !isLoading
        binding.btnBack.setEnabled(!isLoading)

        if (isLoading) {
            binding.btnContinue.alpha = 0.6f
            binding.btnContinue.text = "Đang xử lý..."
        } else {
            binding.btnContinue.alpha = 1.0f
            binding.btnContinue.text = "Tiếp tục"
        }
    }

    private fun updateItemSelectedCount() {
        var items = 0
        for (qty in viewModel.selectedCombos.values) {
            items += qty
        }
        binding.tvItemSelected.text = "($items phần)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
