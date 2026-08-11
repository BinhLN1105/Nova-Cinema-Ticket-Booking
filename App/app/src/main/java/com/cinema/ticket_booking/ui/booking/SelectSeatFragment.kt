package com.cinema.ticket_booking.ui.booking

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.SeatMapResponse
import com.cinema.ticket_booking.databinding.FragmentSelectSeatBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class SelectSeatFragment : Fragment() {

    private var _binding: FragmentSelectSeatBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SelectSeatViewModel
    private var showtimeId: String? = null
    private var currentSeatMap: SeatMapResponse? = null
    private var expireTime: Long = 0

    // Polling for real-time seat status updates
    private val pollHandler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null

    // Map of showtimeSeatId -> seat button view (for efficient targeted updates)
    private val seatButtonMap = HashMap<String, TextView>()

    companion object {
        private const val POLL_INTERVAL_MS = 12_000L // 12 seconds
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectSeatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SelectSeatViewModel::class.java]

        arguments?.let {
            showtimeId = it.getString("showtimeId")
            showtimeId?.let { id -> viewModel.loadSeatMap(id) }
        }

        val rawTime = SelectShowtimeViewModel.pendingShowtimeTime
        var displayTime = rawTime ?: ""
        if (rawTime != null) {
            try {
                val sdfIn = if (rawTime.contains("T")) {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                } else {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                }
                val d = sdfIn.parse(rawTime)
                if (d != null) {
                    val sdfOut = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
                    displayTime = sdfOut.format(d)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        binding.tvShowtimeInfo.text = "${SelectShowtimeViewModel.pendingMovieTitle ?: ""} • $displayTime"

        binding.btnBack.setOnClickListener {
            Navigation.findNavController(view).popBackStack()
        }

        binding.btnConfirm.setOnClickListener {
            if (viewModel.selectedSeatIds.isEmpty()) {
                SnackbarHelper.showError(binding.root, "Vui lòng chọn ít nhất 1 ghế")
                return@setOnClickListener
            }
            SelectSeatViewModel.pendingSeatIds = ArrayList(viewModel.selectedSeatIds)
            SelectSeatViewModel.pendingTotalAmount = viewModel.calculateTotal(currentSeatMap)

            val bundle = Bundle().apply {
                putLong("expireTime", expireTime)
            }
            Navigation.findNavController(view).navigate(R.id.action_selectSeat_to_selectCombo, bundle)
        }

        viewModel.seatMap.observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.LOADING -> binding.progressBar.visibility = View.VISIBLE
                Resource.Status.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    resource.data?.let { data ->
                        currentSeatMap = data
                        expireTime = System.currentTimeMillis() + data.seatHoldMins.toLong() * 60 * 1000
                        seatButtonMap.clear()
                        renderSeatMap(data)
                        startPolling()
                    }
                }
                Resource.Status.ERROR -> {
                    binding.progressBar.visibility = View.GONE
                    SnackbarHelper.showError(binding.root, resource.message ?: "Lỗi tải bản đồ ghế")
                }
            }
        }

        // Silent refresh observer — only updates changed seat colors
        viewModel.seatRefresh.observe(viewLifecycleOwner) { freshMap ->
            if (freshMap?.seats == null) return@observe
            currentSeatMap = freshMap
            var anyDeselected = false
            for (seat in freshMap.seats) {
                val seatId = seat.showtimeSeatId ?: continue
                val btn = seatButtonMap[seatId] ?: continue
                var isSelectedByMe = viewModel.selectedSeatIds.contains(seatId)
                // If seat became taken but user had selected it, deselect
                if (isSelectedByMe && ("BOOKED" == seat.status || "LOCKED" == seat.status)) {
                    viewModel.selectedSeatIds.remove(seatId)
                    isSelectedByMe = false
                    anyDeselected = true
                }
                updateSeatButtonStyle(btn, seat, isSelectedByMe)
            }
            if (anyDeselected) {
                SnackbarHelper.showError(binding.root, "Một số ghế bạn chọn vừa bị người khác đặt mất!")
                updateSummary()
            }
        }
    }

    private fun startPolling() {
        pollRunnable?.let { pollHandler.removeCallbacks(it) }
        pollRunnable = object : Runnable {
            override fun run() {
                if (_binding != null) {
                    viewModel.refreshSeatStatuses()
                    pollHandler.postDelayed(this, POLL_INTERVAL_MS)
                }
            }
        }
        pollRunnable?.let { pollHandler.postDelayed(it, POLL_INTERVAL_MS) }
    }

    private fun renderSeatMap(seatMap: SeatMapResponse) {
        binding.seatContainer.removeAllViews()
        if (seatMap.seats == null) return

        val totalRows = seatMap.maxGridRow + 1
        val totalCols = seatMap.maxGridCol + 1

        // Build lookup map: "gridRow_gridCol" -> SeatItem
        val seatGrid = HashMap<String, SeatMapResponse.SeatItem>()
        for (seat in seatMap.seats) {
            seatGrid["${seat.gridRow}_${seat.gridCol}"] = seat
        }

        val seatSize = (resources.displayMetrics.density * 34).toInt()
        val margin = (resources.displayMetrics.density * 3).toInt()

        for (r in 0 until totalRows) {
            // Determine row label: scan seats in this row to find the label char
            var label = ('A' + r).toString()
            for (c in 0 until totalCols) {
                val s = seatGrid["${r}_${c}"]
                if (s != null && !s.seatLabel.isNullOrEmpty()) {
                    label = s.seatLabel.substring(0, 1)
                    break
                }
            }
            // Always render the row (including empty rows) so the seat map is consistent

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, margin, 0, margin)
            }

            // Left label
            val rowLabelLeft = TextView(requireContext()).apply {
                text = label
                setTextColor(resources.getColor(R.color.on_surface_variant, null))
                setTypeface(null, android.graphics.Typeface.BOLD)
                minWidth = (resources.displayMetrics.density * 40).toInt()
                gravity = Gravity.CENTER
            }
            row.addView(rowLabelLeft)

            for (c in 0 until totalCols) {
                val seat = seatGrid["${r}_${c}"]

                if (seat != null) {
                    val seatBtn = TextView(requireContext())
                    val seatText = if (seat.seatLabel != null && seat.seatLabel.length > 1) {
                        seat.seatLabel.substring(1)
                    } else {
                        seat.colNumber.toString()
                    }

                    seatBtn.text = seatText
                    seatBtn.gravity = Gravity.CENTER
                    seatBtn.textSize = 10f

                    val lp = LinearLayout.LayoutParams(seatSize, seatSize).apply {
                        setMargins(margin, 0, margin, 0)
                    }
                    seatBtn.layoutParams = lp

                    val seatId = seat.showtimeSeatId ?: ""
                    // Track this button for real-time updates
                    seatButtonMap[seatId] = seatBtn

                    val isInitiallySelected = viewModel.selectedSeatIds.contains(seatId)
                    updateSeatButtonStyle(seatBtn, seat, isInitiallySelected)

                    seatBtn.setOnClickListener {
                        val toggled = viewModel.toggleSeat(seat)
                        if (toggled) {
                            val isSelected = viewModel.selectedSeatIds.contains(seatId)
                            updateSeatButtonStyle(seatBtn, seat, isSelected)
                            updateSummary()
                        }
                    }
                    row.addView(seatBtn)
                } else {
                    val spacer = View(requireContext())
                    val lp = LinearLayout.LayoutParams(seatSize, seatSize).apply {
                        setMargins(margin, 0, margin, 0)
                    }
                    spacer.setLayoutParams(lp)
                    row.addView(spacer)
                }
            }
            // Right label
            val rowLabelRight = TextView(requireContext()).apply {
                text = label
                setTextColor(resources.getColor(R.color.on_surface_variant, null))
                minWidth = (resources.displayMetrics.density * 40).toInt()
                gravity = Gravity.CENTER
            }
            row.addView(rowLabelRight)

            binding.seatContainer.addView(row)
        }
        updateSummary()

        binding.zoomLayout.post {
            binding.zoomLayout.zoomTo(1.0f, false)
        }
    }

    private fun updateSeatButtonStyle(tv: TextView, seat: SeatMapResponse.SeatItem, selected: Boolean) {
        val colorRes = if (selected) {
            R.color.seat_selected
        } else if ("BOOKED" == seat.status || "LOCKED" == seat.status) {
            R.color.seat_booked
        } else if ("VIP" == seat.seatType) {
            R.color.seat_vip
        } else if ("COUPLE" == seat.seatType) {
            R.color.seat_couple
        } else {
            R.color.seat_available
        }

        val color = resources.getColor(colorRes, null)

        val gd = GradientDrawable().apply {
            setColor(color)
            val radius = resources.displayMetrics.density * 6
            cornerRadius = radius
        }

        if (!selected && "BOOKED" != seat.status) {
            gd.setStroke(2, resources.getColor(R.color.outline_variant, null))
        }

        tv.background = gd
        tv.setTextColor(resources.getColor(android.R.color.white, null))
    }

    private fun updateSummary() {
        val count = viewModel.selectedSeatIds.size
        val total = viewModel.calculateTotal(currentSeatMap)
        binding.tvSelectedCount.text = "$count ghế đã chọn"
        binding.tvTotalPrice.text = String.format("%,.0fđ", total)
        binding.btnConfirm.isEnabled = count > 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pollRunnable?.let { pollHandler.removeCallbacks(it) }
        _binding = null
    }
}
