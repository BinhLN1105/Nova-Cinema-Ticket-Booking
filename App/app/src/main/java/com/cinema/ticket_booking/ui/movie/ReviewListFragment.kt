package com.cinema.ticket_booking.ui.movie

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.databinding.FragmentReviewListBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper

class ReviewListFragment : Fragment() {

    private var _binding: FragmentReviewListBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MovieDetailViewModel
    private lateinit var adapter: ReviewAdapter
    private var movieId: String? = null
    private var currentPage = 0
    private var isLastPage = false
    private var isLoading = false
    private var currentRatingFilter: Int? = null // null = tất cả

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            movieId = it.getString("movieId")
            if (it.containsKey("rating")) {
                currentRatingFilter = it.getInt("rating")
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReviewListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[MovieDetailViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupStarFilter()
        setupObservers()

        binding.swipeRefresh.setOnRefreshListener { refreshData() }
        refreshData()
        checkEligibility()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { Navigation.findNavController(requireView()).popBackStack() }
    }

    private fun setupRecyclerView() {
        adapter = ReviewAdapter(mutableListOf())
        val layoutManager = LinearLayoutManager(requireContext())
        binding.rvReviews.layoutManager = layoutManager
        binding.rvReviews.adapter = adapter

        binding.rvReviews.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()
                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                            loadMoreData()
                        }
                    }
                }
            }
        })
    }

    private fun setupStarFilter() {
        // Thiết lập trạng thái chip ban đầu dựa trên filter được truyền vào
        currentRatingFilter?.let { rating ->
            val chipId = when (rating) {
                5 -> R.id.chip5Star
                4 -> R.id.chip4Star
                3 -> R.id.chip3Star
                2 -> R.id.chip2Star
                1 -> R.id.chip1Star
                else -> R.id.chipAll
            }
            binding.chipGroupFilter.check(chipId)
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val checkedId = checkedIds[0]
            currentRatingFilter = when (checkedId) {
                R.id.chip5Star -> 5
                R.id.chip4Star -> 4
                R.id.chip3Star -> 3
                R.id.chip2Star -> 2
                R.id.chip1Star -> 1
                else -> null
            }
            refreshData()
        }
    }

    private fun setupObservers() {
        viewModel.getReviews().observe(viewLifecycleOwner) { resource ->
            if (resource == null) return@observe
            isLoading = false
            binding.swipeRefresh.isRefreshing = false
            binding.progressBar.visibility = View.GONE

            if (resource.isSuccess) {
                val data = resource.data
                if (data != null) {
                    if (currentPage == 0) adapter.clear()
                    data.content?.let { content ->
                        adapter.addAll(content)
                    }
                    isLastPage = data.last
                    binding.tvEmpty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
                }
            } else if (resource.isError) {
                SnackbarHelper.showError(binding.root, resource.message ?: "Lỗi tải đánh giá")
            }
        }
    }

    private fun refreshData() {
        currentPage = 0
        isLastPage = false
        loadData()
    }

    private fun loadMoreData() {
        currentPage++
        loadData()
    }

    private fun loadData() {
        isLoading = true
        if (currentPage == 0) binding.progressBar.visibility = View.VISIBLE
        movieId?.let { id ->
            viewModel.loadReviewsFiltered(id, currentPage, 10, currentRatingFilter)
        }
    }

    private fun checkEligibility() {
        movieId?.let { id ->
            viewModel.checkReviewEligibility(id).observe(viewLifecycleOwner) { resource ->
                if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                    binding.fabWriteReview.visibility = View.VISIBLE
                    binding.fabWriteReview.setOnClickListener {
                        val args = Bundle().apply {
                            putString("movieId", movieId)
                            putString("bookingId", resource.data.bookingId)
                        }
                        Navigation.findNavController(requireView()).navigate(R.id.action_reviewList_to_writeReview, args)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
