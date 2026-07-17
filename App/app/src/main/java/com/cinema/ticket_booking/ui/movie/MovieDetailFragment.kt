package com.cinema.ticket_booking.ui.movie

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.local.TokenManager
import com.cinema.ticket_booking.databinding.FragmentMovieDetailBinding
import com.cinema.ticket_booking.ui.booking.SelectShowtimeViewModel
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MovieDetailFragment : Fragment() {

    private var _binding: FragmentMovieDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MovieDetailViewModel
    private var movieId: String? = null

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMovieDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[MovieDetailViewModel::class.java]

        arguments?.let {
            movieId = it.getString("movieId")
            movieId?.let { id ->
                viewModel.loadMovie(id)
                viewModel.loadReviews(id)
            }
        }

        setupObservers(view)
        binding.btnBack.setOnClickListener { Navigation.findNavController(view).popBackStack() }
        binding.btnBookTicket.setOnClickListener {
            val args = Bundle().apply {
                putString("movieId", movieId)
            }
            Navigation.findNavController(view).navigate(R.id.action_movieDetail_to_selectShowtime, args)
        }
        binding.btnWriteReview.setOnClickListener {
            if (!tokenManager.isLoggedIn) {
                SnackbarHelper.showError(binding.root, "Vui lòng đăng nhập để viết đánh giá")
                return@setOnClickListener
            }
            movieId?.let { id ->
                viewModel.checkReviewEligibility(id).observe(viewLifecycleOwner) { resource ->
                    if (resource.status == Resource.Status.LOADING) return@observe
                    binding.progressBar.setVisibility(View.GONE)
                    if (resource.isSuccess && resource.data != null) {
                        val data = resource.data
                        val args = Bundle().apply {
                            putString("movieId", movieId)
                            putString("movieTitle", binding.tvTitle.text.toString())
                            putString("bookingId", data.bookingId)
                        }
                        Navigation.findNavController(requireView()).navigate(R.id.action_movieDetail_to_writeReview, args)
                    } else if (resource.isError) {
                        SnackbarHelper.showError(
                            binding.root,
                            resource.message ?: "Bạn cần mua vé và xem phim trước khi đánh giá!"
                        )
                    }
                }
            }
        }

        binding.btnViewAllReviews.setOnClickListener {
            val args = Bundle().apply {
                putString("movieId", movieId)
            }
            Navigation.findNavController(it).navigate(R.id.action_movieDetail_to_reviewList, args)
        }

        binding.btnFilterReview.setOnClickListener {
            val args = Bundle().apply {
                putString("movieId", movieId)
            }
            try {
                Navigation.findNavController(requireView()).navigate(R.id.action_movieDetail_to_reviewList, args)
            } catch (e: Exception) {
                android.widget.Toast.makeText(requireContext(), "Không thể mở đánh giá", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers(view: View) {
        viewModel.getMovie().observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.LOADING -> binding.progressBar.visibility = View.VISIBLE
                Resource.Status.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    val m = resource.data ?: return@observe
                    binding.tvTitle.text = m.title
                    binding.tvDescription.text = m.description
                    binding.tvDirector.text = "Đạo diễn: ${m.director}"
                    binding.tvCast.text = "Diễn viên: ${m.cast}"
                    binding.tvDuration.text = "${m.duration} phút"
                    binding.tvRated.text = m.rated
                    binding.tvRating.text = String.format("%.1f", m.avgRating)
                    binding.tvReleaseDate.text = "Khởi chiếu: ${m.releaseDate}"
                    if (!m.genres.isNullOrEmpty()) {
                        val sb = StringBuilder()
                        for (g in m.genres) {
                            sb.append(g.name).append("  ")
                        }
                        binding.tvGenres.text = sb.toString().trim()
                    }
                    SelectShowtimeViewModel.pendingMoviePoster = m.posterUrl
                    Glide.with(this).load(m.posterUrl)
                        .placeholder(R.drawable.ic_movie_placeholder)
                        .into(binding.ivPoster)
                    binding.btnTrailer.visibility =
                        if (!m.trailerUrl.isNullOrEmpty()) View.VISIBLE else View.GONE
                    binding.btnTrailer.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(m.trailerUrl)))
                    }
                }
                Resource.Status.ERROR -> {
                    binding.progressBar.visibility = View.GONE
                    SnackbarHelper.showError(binding.root, resource.message ?: "Lỗi tải thông tin phim")
                }
            }
        }

        // Reviews
        binding.rvReviews.layoutManager = LinearLayoutManager(requireContext())
        viewModel.getReviews().observe(viewLifecycleOwner) { resource ->
            if (resource.isSuccess && resource.data != null) {
                val list = resource.data.content
                if (list.isNullOrEmpty()) {
                    binding.tvNoReviews.visibility = View.VISIBLE
                    binding.rvReviews.visibility = View.GONE
                    binding.btnViewAllReviews.visibility = View.GONE
                } else {
                    binding.tvNoReviews.visibility = View.GONE
                    binding.rvReviews.visibility = View.VISIBLE
                    binding.rvReviews.adapter = ReviewAdapter(list.toMutableList())
                    binding.btnViewAllReviews.visibility =
                        if (resource.data.totalElements > 3) View.VISIBLE else View.GONE
                }
            }
        }

        viewModel.getCreateReviewResult().observe(viewLifecycleOwner) { resource ->
            if (resource == null) return@observe
            if (resource.isSuccess) {
                SnackbarHelper.showSuccess(binding.root, "Đã gửi đánh giá!")
                movieId?.let { viewModel.loadReviews(it) }
            } else if (resource.status == Resource.Status.ERROR) {
                SnackbarHelper.showError(binding.root, resource.message ?: "Gửi đánh giá thất bại")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
