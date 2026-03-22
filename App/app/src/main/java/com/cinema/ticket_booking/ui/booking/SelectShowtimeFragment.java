package com.cinema.ticket_booking.ui.booking;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.*;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.databinding.FragmentSelectShowtimeBinding;
import java.text.SimpleDateFormat;
import java.util.*;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SelectShowtimeFragment extends Fragment {

    private FragmentSelectShowtimeBinding binding;
    private SelectShowtimeViewModel viewModel;
    private String movieId;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentSelectShowtimeBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SelectShowtimeViewModel.class);

        if (getArguments() != null) {
            movieId = getArguments().getString("movieId");
        }

        setupDateSelector();
        setupObservers(view);
        viewModel.loadCinemas();
        viewModel.loadShowtimes(movieId);

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
    }

    private void setupDateSelector() {
        List<String> dates = viewModel.getNext7Days();
        LinearLayout container = binding.dateContainer;
        container.removeAllViews();
        SimpleDateFormat display = new SimpleDateFormat("EEE\ndd", new Locale("vi", "VN"));
        SimpleDateFormat api = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (String date : dates) {
            TextView tv = new TextView(requireContext());
            tv.setText(display.format(parseDate(date)));
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tv.setPadding(24, 16, 24, 16);
            tv.setTextColor(getResources().getColor(android.R.color.white, null));
            tv.setBackgroundResource(R.drawable.bg_date_selector);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(8, 0, 8, 0);
            tv.setLayoutParams(lp);
            tv.setOnClickListener(v -> viewModel.selectDate(date));
            container.addView(tv);
        }
    }

    private void setupObservers(View view) {
        viewModel.getCinemas().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                String[] names = resource.data.stream()
                        .map(CinemaResponse::getName).toArray(String[]::new);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, names);
                binding.spinnerCinema.setAdapter(adapter);
                binding.spinnerCinema.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> p, View v2, int pos, long id) {
                        viewModel.selectCinema(resource.data.get(pos).getId());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> p) {
                    }
                });
            }
        });

        viewModel.getShowtimes().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data == null || resource.data.isEmpty()) {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                        binding.rvShowtimes.setVisibility(View.GONE);
                    } else {
                        binding.tvEmpty.setVisibility(View.GONE);
                        binding.rvShowtimes.setVisibility(View.VISIBLE);
                        binding.rvShowtimes.setLayoutManager(new GridLayoutManager(requireContext(), 3));
                        binding.rvShowtimes.setAdapter(new ShowtimeAdapter(resource.data, showtime -> {
                            SelectShowtimeViewModel.pendingShowtimeId = showtime.getId();
                            SelectShowtimeViewModel.pendingMovieTitle = showtime.getMovieTitle();
                            SelectShowtimeViewModel.pendingShowtimeTime = showtime.getStartTime();
                            SelectShowtimeViewModel.pendingCinemaName = showtime.getCinemaName();
                            Bundle args = new Bundle();
                            args.putString("showtimeId", showtime.getId());
                            Navigation.findNavController(view)
                                    .navigate(R.id.action_selectShowtime_to_selectSeat, args);
                        }));
                    }
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private java.util.Date parseDate(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(s);
        } catch (Exception e) {
            return new java.util.Date();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
