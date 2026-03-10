package com.cinema.ticket_booking.ui.home;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentHomeBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding.rvNowShowing.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvComingSoon.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        viewModel.getNowShowing().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data != null) {
                        MovieAdapter adapter = new MovieAdapter(resource.data.getContent(), movieId ->
                                navigateToDetail(view, movieId));
                        binding.rvNowShowing.setAdapter(adapter);
                    }
                }
                case ERROR -> binding.progressBar.setVisibility(View.GONE);
            }
        });

        viewModel.getComingSoon().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                MovieAdapter adapter = new MovieAdapter(resource.data.getContent(), movieId ->
                        navigateToDetail(view, movieId));
                binding.rvComingSoon.setAdapter(adapter);
            }
        });

        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.refresh();
            binding.swipeRefresh.setRefreshing(false);
        });

        binding.etSearch.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.searchFragment));
    }

    private void navigateToDetail(View view, String movieId) {
        Bundle args = new Bundle();
        args.putString("movieId", movieId);
        Navigation.findNavController(view).navigate(R.id.action_home_to_movieDetail, args);
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
