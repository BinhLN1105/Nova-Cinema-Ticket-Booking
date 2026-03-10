package com.cinema.ticket_booking.ui.search;

import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.*;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentSearchBinding;
import com.cinema.ticket_booking.ui.home.MovieAdapter;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentSearchBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        binding.rvResults.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                String q = s.toString().trim();
                if (q.length() >= 2) viewModel.search(q);
                else if (q.isEmpty()) viewModel.clearResults();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        viewModel.getResults().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data == null || resource.data.getContent() == null || resource.data.getContent().isEmpty()) {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                        binding.rvResults.setVisibility(View.GONE);
                    } else {
                        binding.tvEmpty.setVisibility(View.GONE);
                        binding.rvResults.setVisibility(View.VISIBLE);
                        binding.rvResults.setAdapter(new MovieAdapter(resource.data.getContent(), movieId -> {
                            Bundle args = new Bundle();
                            args.putString("movieId", movieId);
                            Navigation.findNavController(view)
                                    .navigate(R.id.action_search_to_movieDetail, args);
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

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
