package com.cinema.ticket_booking.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.CinemaResponse
import com.cinema.ticket_booking.databinding.FragmentSelectShowtimeBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class SelectShowtimeFragment : Fragment() {

    private var _binding: FragmentSelectShowtimeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SelectShowtimeViewModel
    private var movieId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectShowtimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SelectShowtimeViewModel::class.java]

        arguments?.let {
            movieId = it.getString("movieId")
        }

        setupDateSelector()
        setupObservers(view)
        viewModel.loadCinemas()
        viewModel.loadShowtimes(movieId)

        binding.btnBack.setOnClickListener {
            Navigation.findNavController(view).popBackStack()
        }
    }

    private fun setupDateSelector() {
        val dates = viewModel.next7Days
        val container = binding.dateContainer
        container.removeAllViews()
        val display = SimpleDateFormat("EEE\ndd", Locale("vi", "VN"))

        for (date in dates) {
            val tv = TextView(requireContext()).apply {
                text = display.format(parseDate(date))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(24, 16, 24, 16)
                setTextColor(resources.getColor(android.R.color.white, null))
                setBackgroundResource(R.drawable.bg_date_selector)
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 8, 0)
            }
            tv.layoutParams = lp
            tv.setOnClickListener { viewModel.selectDate(date) }
            container.addView(tv)
        }
    }

    private fun setupObservers(view: View) {
        viewModel.cinemas.observe(viewLifecycleOwner) { resource ->
            if (resource.isSuccess && resource.data != null) {
                val names = resource.data.map { it.name }.toTypedArray()
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    names
                )
                binding.spinnerCinema.adapter = adapter
                binding.spinnerCinema.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                        viewModel.selectCinema(resource.data[pos].id)
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                }
            }
        }

        viewModel.showtimes.observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.LOADING -> binding.progressBar.visibility = View.VISIBLE
                Resource.Status.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    if (resource.data.isNullOrEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvShowtimes.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvShowtimes.visibility = View.VISIBLE
                        binding.rvShowtimes.layoutManager = GridLayoutManager(requireContext(), 3)
                        binding.rvShowtimes.adapter = ShowtimeAdapter(resource.data) { showtime ->
                            SelectShowtimeViewModel.pendingShowtimeId = showtime.id
                            SelectShowtimeViewModel.pendingMovieTitle = showtime.movieTitle
                            SelectShowtimeViewModel.pendingShowtimeTime = showtime.startTime
                            SelectShowtimeViewModel.pendingCinemaName = showtime.cinemaName
                            SelectShowtimeViewModel.pendingShowDate = viewModel.selectedDate.value
                            val args = Bundle().apply {
                                putString("showtimeId", showtime.id)
                            }
                            Navigation.findNavController(view)
                                .navigate(R.id.action_selectShowtime_to_selectSeat, args)
                        }
                    }
                }
                Resource.Status.ERROR -> {
                    binding.progressBar.visibility = View.GONE
                    SnackbarHelper.showError(binding.root, resource.message ?: "Lỗi tải suất chiếu")
                }
            }
        }
    }

    private fun parseDate(s: String): Date {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(s) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
