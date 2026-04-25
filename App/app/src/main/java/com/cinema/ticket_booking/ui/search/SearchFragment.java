package com.cinema.ticket_booking.ui.search;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import com.cinema.ticket_booking.util.SnackbarHelper;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.*;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentSearchBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import dagger.hilt.android.AndroidEntryPoint;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@AndroidEntryPoint
public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    getLastLocation();
                } else {
                    SnackbarHelper.showSuccess(binding.getRoot(), "Quyền vị trí bị từ chối. Mặc định là Hà Nội.");
                    viewModel.loadCinemas("Hà Nội");
                    binding.tvLocation.setText("Hà Nội ▼");
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentSearchBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        binding.rvCinemas.setLayoutManager(new LinearLayoutManager(requireContext()));

        viewModel.getCinemas().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data == null || resource.data.isEmpty()) {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                        binding.rvCinemas.setVisibility(View.GONE);
                    } else {
                        binding.tvEmpty.setVisibility(View.GONE);
                        binding.rvCinemas.setVisibility(View.VISIBLE);
                        binding.rvCinemas.setAdapter(new CinemaAdapter(resource.data, cinema -> {
                            // Nova: Navigate to professional Cinema Detail page
                            Bundle args = new Bundle();
                            args.putString("cinemaId", cinema.getId());
                            args.putString("cinemaName", cinema.getName());
                            Navigation.findNavController(view).navigate(R.id.cinemaDetailFragment, args);
                        }));
                    }
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    SnackbarHelper.showError(binding.getRoot(), resource.message);
                }
            }
        });

        binding.btnGps.setOnClickListener(v -> checkLocationPermission());

        binding.locationLayout.setOnClickListener(v -> {
            // Manual selection fallback
            String[] displayCities = { "Toàn quốc", "Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ" };
            String[] queryCities = { "", "Hà Nội", "Ho Chi Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ" };
            new AlertDialog.Builder(requireContext())
                    .setTitle("Chọn khu vực")
                    .setItems(displayCities, (dialog, which) -> {
                        String selectedDisplay = displayCities[which];
                        String selectedQuery = queryCities[which];
                        binding.tvLocation.setText(selectedDisplay + " ▼");
                        viewModel.loadCinemas(selectedQuery);
                    })
                    .show();
        });

        // Tải mặc định tất cả rạp
        viewModel.loadCinemas("");
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                getCityFromLocation(location);
            } else {
                SnackbarHelper.showError(binding.getRoot(), "Không thể lấy vị trí hiện tại.");
            }
        });
    }

    private void getCityFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(requireContext(), new Locale("vi", "VN"));
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                String city = addresses.get(0).getAdminArea();
                String district = addresses.get(0).getSubAdminArea();
                
                if (district != null) {
                    binding.tvLocation.setText(district + " ▼");
                    // Biến đổi "Quận 12" thành "Q12" để match với dữ liệu test
                    String query = district.replace("Quận ", "Q").replace("quận ", "Q");
                    viewModel.loadCinemas(query);
                    return;
                }

                if (city == null)
                    city = addresses.get(0).getLocality();

                if (city != null) {
                    city = city.replace("Thành phố ", "").replace("Tỉnh ", "").trim();
                    binding.tvLocation.setText(city + " ▼");
                    
                    if (city.equals("Hồ Chí Minh")) {
                        city = "Ho Chi Minh";
                    }
                    
                    viewModel.loadCinemas(city);
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        SnackbarHelper.showError(binding.getRoot(), "Không thể xác định tên tỉnh thành.");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

