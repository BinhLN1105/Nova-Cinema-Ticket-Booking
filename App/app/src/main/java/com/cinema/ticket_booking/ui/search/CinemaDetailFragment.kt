package com.cinema.ticket_booking.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.ShowtimeResponse
import com.cinema.ticket_booking.databinding.FragmentCinemaDetailBinding
import com.cinema.ticket_booking.ui.booking.SelectShowtimeViewModel
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CinemaDetailFragment : Fragment() {

    private var _binding: FragmentCinemaDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SelectShowtimeViewModel
    private var cinemaId: String? = null
    private var cinemaName: String? = null
    private val sharedPool = RecyclerView.RecycledViewPool()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCinemaDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SelectShowtimeViewModel::class.java]

        arguments?.let {
            cinemaId = it.getString("cinemaId")
            cinemaName = it.getString("cinemaName")
            binding.tvCinemaName.text = cinemaName
        }

        setupDateSelector()
        setupObservers(view)

        // Load showtimes for this cinema specifically
        viewModel.selectCinema(cinemaId)
        viewModel.loadShowtimes(null) // Load all movies for this cinema

        binding.btnBack.setOnClickListener { Navigation.findNavController(view).popBackStack() }
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
                setPadding(32, 20, 32, 20)
                setTextColor(resources.getColor(android.R.color.white, null))
                setBackgroundResource(R.drawable.bg_date_selector)
            }

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(12, 0, 12, 0)
            }
            tv.layoutParams = lp

            // Observe selected date to update UI
            viewModel.selectedDate.observe(viewLifecycleOwner) { selected ->
                val isSelected = date == selected
                tv.alpha = if (isSelected) 1.0f else 0.5f
                tv.scaleX = if (isSelected) 1.1f else 1.0f
                tv.scaleY = if (isSelected) 1.1f else 1.0f
            }

            tv.setOnClickListener { viewModel.selectDate(date) }
            container.addView(tv)
        }
    }

    private fun setupObservers(view: View) {
        viewModel.showtimes.observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.LOADING -> binding.progressBar.visibility = View.VISIBLE
                Resource.Status.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    val data = resource.data
                    if (data.isNullOrEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvCinemaMovies.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvCinemaMovies.visibility = View.VISIBLE

                        // Group showtimes by Movie
                        val grouped = data.groupBy { it.movieId }
                        val moviesList = ArrayList(grouped.values)

                        binding.rvCinemaMovies.layoutManager = LinearLayoutManager(requireContext())
                        binding.rvCinemaMovies.adapter = CinemaMovieAdapter(moviesList, sharedPool) { showtime ->
                            // Navigation logic
                            SelectShowtimeViewModel.pendingShowtimeId = showtime.id
                            SelectShowtimeViewModel.pendingMovieTitle = showtime.movieTitle
                            SelectShowtimeViewModel.pendingShowtimeTime = showtime.startTime
                            SelectShowtimeViewModel.pendingCinemaName = cinemaName
                            SelectShowtimeViewModel.pendingShowDate = viewModel.selectedDate.value

                            val args = Bundle().apply {
                                putString("showtimeId", showtime.id)
                            }
                            Navigation.findNavController(view).navigate(R.id.action_selectShowtime_to_selectSeat, args)
                        }
                    }
                }
                Resource.Status.ERROR -> {
                    binding.progressBar.visibility = View.GONE
                    SnackbarHelper.showError(binding.root, resource.message ?: "Lỗi tải lịch chiếu")
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
