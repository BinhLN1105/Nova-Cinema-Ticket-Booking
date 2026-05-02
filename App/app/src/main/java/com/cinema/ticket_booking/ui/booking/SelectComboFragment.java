package com.cinema.ticket_booking.ui.booking;

import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.ComboResponse;
import com.cinema.ticket_booking.databinding.FragmentSelectComboBinding;
import com.cinema.ticket_booking.data.model.response.BookingResponse;
import com.cinema.ticket_booking.util.Resource;
import com.cinema.ticket_booking.util.SnackbarHelper;

import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;

@AndroidEntryPoint
public class SelectComboFragment extends Fragment {

    private FragmentSelectComboBinding binding;
    private SelectComboViewModel viewModel;
    private ComboAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentSelectComboBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SelectComboViewModel.class);

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        binding.tvMovieTitle.setText(SelectShowtimeViewModel.pendingMovieTitle);

        setupRecyclerView();
        observeViewModel();
        setupClickListeners();
    }

    private void setupRecyclerView() {
        binding.rvCombos.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void observeViewModel() {
        viewModel.getCombos().observe(getViewLifecycleOwner(), resource -> {
            binding.progressBar.setVisibility(resource.isLoading() ? View.VISIBLE : View.GONE);
            if (resource.isSuccess() && resource.data != null) {
                adapter = new ComboAdapter(resource.data,
                        comboId -> {
                            viewModel.addCombo(comboId);
                            updateItemSelectedCount();
                        },
                        comboId -> {
                            viewModel.removeCombo(comboId);
                            updateItemSelectedCount();
                        },
                        viewModel.getSelectedCombos());
                binding.rvCombos.setAdapter(adapter);
                updateItemSelectedCount();
            } else if (resource.isError()) {
                SnackbarHelper.showError(binding.getRoot(), resource.message);
            }
        });

        // Instant Total Calculation (Optimistic UI)
        viewModel.getLocalTotal().observe(getViewLifecycleOwner(), total -> {
            binding.tvTotal.setText(String.format(java.util.Locale.getDefault(), "%,.0fđ", total));
        });
    }

    private void setupClickListeners() {
        binding.btnSkip.setOnClickListener(v -> handleContinue());
        binding.btnContinue.setOnClickListener(v -> handleContinue());
    }

    private void handleContinue() {
        // Show loading state for the final transition quote
        showLoading(true);

        viewModel.getFinalQuote().observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                navigateToConfirm(resource.data);
            } else if (resource.status == Resource.Status.ERROR) {
                showLoading(false);
                SnackbarHelper.showError(binding.getRoot(),
                        resource.message != null ? resource.message : "Lỗi tính toán giá cuối cùng");
            }
        });
    }

    private void navigateToConfirm(BookingResponse quote) {
        Bundle bundle = new Bundle();
        if (getArguments() != null) {
            bundle.putLong("expireTime", getArguments().getLong("expireTime"));
        }

        // Pass the Parcelable quote to the next screen
        bundle.putParcelable("initialQuote", quote);

        NavHostFragment.findNavController(this)
                .navigate(R.id.action_selectCombo_to_confirmBooking, bundle);
    }

    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnContinue.setEnabled(!isLoading);
        binding.btnSkip.setEnabled(!isLoading);
        binding.btnBack.setEnabled(!isLoading);

        if (isLoading) {
            binding.btnContinue.setAlpha(0.6f);
            binding.btnContinue.setText("Đang xử lý...");
        } else {
            binding.btnContinue.setAlpha(1.0f);
            binding.btnContinue.setText("Tiếp tục");
        }
    }

    private void updateItemSelectedCount() {
        int items = 0;
        for (Integer qty : viewModel.getSelectedCombos().values()) {
            items += qty;
        }
        binding.tvItemSelected.setText("(" + items + " phần)");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
