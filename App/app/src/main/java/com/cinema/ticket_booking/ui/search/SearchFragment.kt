package com.cinema.ticket_booking.ui.search

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.databinding.FragmentSearchBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.util.Locale

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SearchViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getLastLocation()
        } else {
            SnackbarHelper.showSuccess(binding.root, "Quyền vị trí bị từ chối. Mặc định là Hà Nội.")
            viewModel.loadCinemas("Hà Nội")
            binding.tvLocation.text = "Hà Nội ▼"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.rvCinemas.layoutManager = LinearLayoutManager(requireContext())

        viewModel.cinemas.observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.LOADING -> binding.progressBar.visibility = View.VISIBLE
                Resource.Status.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    val data = resource.data
                    if (data.isNullOrEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvCinemas.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvCinemas.visibility = View.VISIBLE
                        binding.rvCinemas.adapter = CinemaAdapter(data, object : CinemaAdapter.OnCinemaClickListener {
                            override fun onCinemaClick(cinema: com.cinema.ticket_booking.data.model.response.CinemaResponse) {
                                // Nova: Navigate to professional Cinema Detail page
                                val args = Bundle().apply {
                                    putString("cinemaId", cinema.id)
                                    putString("cinemaName", cinema.name)
                                }
                                Navigation.findNavController(view).navigate(R.id.cinemaDetailFragment, args)
                            }
                        })
                    }
                }
                Resource.Status.ERROR -> {
                    binding.progressBar.visibility = View.GONE
                    SnackbarHelper.showError(binding.root, resource.message ?: "Lỗi tải dữ liệu")
                }
            }
        }

        binding.btnGps.setOnClickListener { checkLocationPermission() }

        binding.locationLayout.setOnClickListener {
            // Manual selection fallback
            val displayCities = arrayOf("Toàn quốc", "Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ")
            val queryCities = arrayOf("", "Hà Nội", "Ho Chi Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ")
            AlertDialog.Builder(requireContext())
                .setTitle("Chọn khu vực")
                .setItems(displayCities) { _, which ->
                    val selectedDisplay = displayCities[which]
                    val selectedQuery = queryCities[which]
                    binding.tvLocation.text = "$selectedDisplay ▼"
                    viewModel.loadCinemas(selectedQuery)
                }
                .show()
        }

        // Tải mặc định tất cả rạp
        viewModel.loadCinemas("")
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getLastLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener(requireActivity()) { location ->
            if (location != null) {
                getCityFromLocation(location)
            } else {
                SnackbarHelper.showError(binding.root, "Không thể lấy vị trí hiện tại.")
            }
        }
    }

    private fun getCityFromLocation(location: Location) {
        val geocoder = Geocoder(requireContext(), Locale("vi", "VN"))
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val city = addresses[0].adminArea
                val district = addresses[0].subAdminArea

                if (district != null) {
                    binding.tvLocation.text = "$district ▼"
                    // Biến đổi "Quận 12" thành "Q12" để match với dữ liệu test
                    val query = district.replace("Quận ", "Q").replace("quận ", "Q")
                    viewModel.loadCinemas(query)
                    return
                }

                var finalCity = city ?: addresses[0].locality
                if (finalCity != null) {
                    finalCity = finalCity.replace("Thành phố ", "").replace("Tỉnh ", "").trim()
                    binding.tvLocation.text = "$finalCity ▼"

                    if (finalCity == "Hồ Chí Minh") {
                        finalCity = "Ho Chi Minh"
                    }

                    viewModel.loadCinemas(finalCity)
                    return
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        SnackbarHelper.showError(binding.root, "Không thể xác định tên tỉnh thành.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
