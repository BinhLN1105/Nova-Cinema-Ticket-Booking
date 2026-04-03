package com.cinema.ticket_booking.ui.movie;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.*;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentReviewListBinding;
import com.cinema.ticket_booking.util.SnackbarHelper;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;

@AndroidEntryPoint
public class ReviewListFragment extends Fragment {

    private FragmentReviewListBinding binding;
    private MovieDetailViewModel viewModel;
    private ReviewAdapter adapter;
    private String movieId;
    private String movieTitle;
    private int currentPage = 0;
    private boolean isLastPage = false;
    private boolean isLoading = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            movieId = getArguments().getString("movieId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        binding = FragmentReviewListBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MovieDetailViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupObservers();
        
        binding.swipeRefresh.setOnRefreshListener(this::refreshData);
        
        // Initial load
        refreshData();

        checkEligibility();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(requireView()).popBackStack());
    }

    private void setupRecyclerView() {
        adapter = new ReviewAdapter(new ArrayList<>());
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.rvReviews.setLayoutManager(layoutManager);
        binding.rvReviews.setAdapter(adapter);

        binding.rvReviews.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) { // Scrolling down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                            loadMoreData();
                        }
                    }
                }
            }
        });
    }

    private void setupObservers() {
        viewModel.getReviews().observe(getViewLifecycleOwner(), resource -> {
            isLoading = false;
            binding.swipeRefresh.setRefreshing(false);
            binding.progressBar.setVisibility(View.GONE);

            switch (resource.status) {
                case SUCCESS -> {
                    if (resource.data != null) {
                        if (currentPage == 0) adapter.clear();
                        adapter.addAll(resource.data.getContent());
                        isLastPage = resource.data.isLast();
                        
                        binding.tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                    }
                }
                case ERROR -> SnackbarHelper.showError(binding.getRoot(), resource.message);
            }
        });
    }

    private void refreshData() {
        currentPage = 0;
        isLastPage = false;
        loadData();
    }

    private void loadMoreData() {
        currentPage++;
        loadData();
    }

    private void loadData() {
        isLoading = true;
        if (currentPage == 0) binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.loadReviewsPaged(movieId, currentPage, 10);
    }

    private void checkEligibility() {
        viewModel.checkReviewEligibility(movieId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == com.cinema.ticket_booking.util.Resource.Status.SUCCESS && resource.data != null) {
                binding.fabWriteReview.setVisibility(View.VISIBLE);
                binding.fabWriteReview.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putString("movieId", movieId);
                    args.putString("bookingId", resource.data.getBookingId());
                    Navigation.findNavController(requireView()).navigate(R.id.action_reviewList_to_writeReview, args);
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
