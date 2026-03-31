package com.cinema.ticket_booking.ui.booking;

import android.os.Bundle;
import android.view.*;
import com.cinema.ticket_booking.util.SnackbarHelper;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.ComboResponse;
import com.cinema.ticket_booking.databinding.FragmentSelectComboBinding;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;

@AndroidEntryPoint
public class SelectComboFragment extends Fragment {

    private FragmentSelectComboBinding binding;
    private SelectComboViewModel viewModel;
    private ComboAdapter adapter;
    private List<ComboResponse> currentCombos;

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
        
        // Show pending movie title
        binding.tvMovieTitle.setText(SelectShowtimeViewModel.pendingMovieTitle);

        binding.btnSkip.setOnClickListener(v -> {
            SelectComboViewModel.pendingCombos.clear();
            
            Bundle bundle = new Bundle();
            if (getArguments() != null) {
                bundle.putLong("expireTime", getArguments().getLong("expireTime"));
            }
            Navigation.findNavController(view).navigate(R.id.action_selectCombo_to_confirmBooking, bundle);
        });

        binding.btnContinue.setOnClickListener(v -> {
            SelectComboViewModel.pendingCombos.clear();
            SelectComboViewModel.pendingCombos.putAll(viewModel.getSelectedCombos());
            
            Bundle bundle = new Bundle();
            if (getArguments() != null) {
                bundle.putLong("expireTime", getArguments().getLong("expireTime"));
            }
            Navigation.findNavController(view).navigate(R.id.action_selectCombo_to_confirmBooking, bundle);
        });

        binding.rvCombos.setLayoutManager(new LinearLayoutManager(requireContext()));

        viewModel.getCombos().observe(getViewLifecycleOwner(), resource -> {
            binding.progressBar.setVisibility(resource.isLoading() ? View.VISIBLE : View.GONE);
            if (resource.isSuccess() && resource.data != null) {
                currentCombos = resource.data;
                adapter = new ComboAdapter(resource.data, 
                    comboId -> {
                        viewModel.addCombo(comboId);
                        updateTotal();
                    }, 
                    comboId -> {
                        viewModel.removeCombo(comboId);
                        updateTotal();
                    }, 
                    viewModel.getSelectedCombos());
                binding.rvCombos.setAdapter(adapter);
                updateTotal();
            } else if (resource.isError()) {
                SnackbarHelper.showError(binding.getRoot(), resource.message);
            }
        });
    }

    private void updateTotal() {
        if (currentCombos == null) return;
        double total = viewModel.calculateTotalCombos(currentCombos);
        binding.tvTotal.setText(String.format("%,.0fđ", total));

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
