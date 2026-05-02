package com.cinema.ticket_booking.ui.search;

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
import com.cinema.ticket_booking.databinding.FragmentCinemaDetailBinding;
import com.cinema.ticket_booking.ui.booking.SelectShowtimeViewModel;
import com.cinema.ticket_booking.util.SnackbarHelper;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Date;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CinemaDetailFragment extends Fragment {

    private FragmentCinemaDetailBinding binding;
    private SelectShowtimeViewModel viewModel;
    private String cinemaId;
    private String cinemaName;
    private final RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentCinemaDetailBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SelectShowtimeViewModel.class);

        if (getArguments() != null) {
            cinemaId = getArguments().getString("cinemaId");
            cinemaName = getArguments().getString("cinemaName");
            binding.tvCinemaName.setText(cinemaName);
        }

        setupDateSelector();
        setupObservers(view);

        // Load showtimes for this cinema specifically
        viewModel.selectCinema(cinemaId);
        viewModel.loadShowtimes(null); // Load all movies for this cinema

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
    }

    private void setupDateSelector() {
        List<String> dates = viewModel.getNext7Days();
        LinearLayout container = binding.dateContainer;
        container.removeAllViews();
        SimpleDateFormat display = new SimpleDateFormat("EEE\ndd", new Locale("vi", "VN"));

        for (String date : dates) {
            TextView tv = new TextView(requireContext());
            tv.setText(display.format(parseDate(date)));
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tv.setPadding(32, 20, 32, 20);
            tv.setTextColor(getResources().getColor(android.R.color.white, null));
            tv.setBackgroundResource(R.drawable.bg_date_selector);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(12, 0, 12, 0);
            tv.setLayoutParams(lp);

            // Observe selected date to update UI
            viewModel.getSelectedDate().observe(getViewLifecycleOwner(), selected -> {
                boolean isSelected = date.equals(selected);
                tv.setAlpha(isSelected ? 1.0f : 0.5f);
                tv.setScaleX(isSelected ? 1.1f : 1.0f);
                tv.setScaleY(isSelected ? 1.1f : 1.0f);
            });

            tv.setOnClickListener(v -> viewModel.selectDate(date));
            container.addView(tv);
        }
    }

    private void setupObservers(View view) {
        viewModel.getShowtimes().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data == null || resource.data.isEmpty()) {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                        binding.rvCinemaMovies.setVisibility(View.GONE);
                    } else {
                        binding.tvEmpty.setVisibility(View.GONE);
                        binding.rvCinemaMovies.setVisibility(View.VISIBLE);

                        // Group showtimes by Movie
                        Map<String, List<ShowtimeResponse>> grouped = resource.data.stream()
                                .collect(Collectors.groupingBy(ShowtimeResponse::getMovieId));

                        List<List<ShowtimeResponse>> moviesList = new ArrayList<>(grouped.values());

                        binding.rvCinemaMovies.setLayoutManager(new LinearLayoutManager(requireContext()));
                        binding.rvCinemaMovies.setAdapter(new CinemaMovieAdapter(moviesList, sharedPool, showtime -> {
                            // Navigation logic
                            SelectShowtimeViewModel.pendingShowtimeId = showtime.getId();
                            SelectShowtimeViewModel.pendingMovieTitle = showtime.getMovieTitle();
                            SelectShowtimeViewModel.pendingShowtimeTime = showtime.getStartTime();
                            SelectShowtimeViewModel.pendingCinemaName = cinemaName;
                            SelectShowtimeViewModel.pendingShowDate = viewModel.getSelectedDate().getValue();

                            Bundle args = new Bundle();
                            args.putString("showtimeId", showtime.getId());
                            Navigation.findNavController(view).navigate(R.id.action_selectShowtime_to_selectSeat, args);
                        }));
                    }
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    SnackbarHelper.showError(binding.getRoot(), resource.message);
                }
            }
        });
    }

    private Date parseDate(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(s);
        } catch (Exception e) {
            return new Date();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
