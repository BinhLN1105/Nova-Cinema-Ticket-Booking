package com.cinema.ticket_booking.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.viewpager2.widget.ViewPager2
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.CinemaResponse
import com.cinema.ticket_booking.data.model.response.MovieSummary
import com.cinema.ticket_booking.databinding.FragmentHomeBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val bannerHandler = Handler(Looper.getMainLooper())
    private var bannerRunnable: Runnable? = null
    private val heroHandler = Handler(Looper.getMainLooper())
    private var heroRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        binding.rvMovies.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvMovies)

        binding.rvSearchResults.layoutManager = GridLayoutManager(requireContext(), 2)

        // Mặc định khi vào app sẽ hiển thị danh sách phim Đang chiếu
        observeMovies(true)

        // Lắng nghe sự kiện chuyển Tab giữa "ĐANG CHIẾU" và "SẮP CHIẾU"
        binding.tabLayoutMovies.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                observeMovies(tab.position == 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        viewModel.getFeaturedMovies().observe(viewLifecycleOwner) { resource ->
            if (resource.isSuccess && !resource.data.isNullOrEmpty()) {
                val movies = resource.data
                val heroAdapter = HeroAdapter(movies)
                binding.vpHero.adapter = heroAdapter

                // Initial setup for the first movie
                updateHeroOverlay(movies[0])

                binding.vpHero.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        if (movies.isNotEmpty()) {
                            updateHeroOverlay(movies[position % movies.size])
                        }
                        // Restart auto-scroll timer
                        heroRunnable?.let { heroHandler.removeCallbacks(it) }
                        setupAutoScrollForHero(movies.size)
                    }
                })

                setupAutoScrollForHero(movies.size)
            } else if (resource.isError) {
                binding.tvHeroTitle.text = "Unable to load featured movies"
                binding.btnBookHero.visibility = View.GONE
            }
        }

        viewModel.getPopupPromotion().observe(viewLifecycleOwner) { resource ->
            if (resource.isSuccess && resource.data != null) {
                // Show Promotion Popup if not dismissed today (Galaxy Cinema Style)
                if (PromoDialogFragment.shouldShow(requireContext())) {
                    PromoDialogFragment.newInstance(resource.data)
                        .show(parentFragmentManager, "PromoDialog")
                }
            }
        }

        viewModel.getActivePromotions().observe(viewLifecycleOwner) { resource ->
            if (resource.isSuccess && !resource.data.isNullOrEmpty()) {
                binding.cvPromoContainer.visibility = View.VISIBLE
                binding.tabLayoutPromo.visibility = View.VISIBLE

                val promotionAdapter = PromotionAdapter(resource.data) { promotion ->
                    val url = promotion.targetUrl
                    if (!url.isNullOrBlank()) {
                        try {
                            if (url.startsWith("/")) {
                                if (url.startsWith("/movie") || url.startsWith("/movies") || url.startsWith("/promotion") || url.startsWith("/promotions")) {
                                    Navigation.findNavController(binding.root).navigate(R.id.action_global_promotionList)
                                } else if (url.startsWith("/cinema")) {
                                    val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)
                                    bottomNav?.selectedItemId = R.id.searchFragment
                                } else {
                                    SnackbarHelper.showInfo(binding.root, "Khuyến mãi được tự động áp dụng khi thanh toán!")
                                }
                            } else {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                startActivity(intent)
                            }
                        } catch (e: Exception) {
                            SnackbarHelper.showError(binding.root, "Không thể mở liên kết!")
                        }
                    }
                }
                binding.vpPromotions.adapter = promotionAdapter

                // Kết nối TabLayout với ViewPager2 để hiển thị các dấu chấm (Dot Indicator)
                TabLayoutMediator(binding.tabLayoutPromo, binding.vpPromotions) { _, _ -> }.attach()

                binding.tvSeeAllPromotions.visibility = View.VISIBLE
                binding.tvSeeAllPromotions.setOnClickListener {
                    Navigation.findNavController(binding.root).navigate(R.id.action_global_promotionList)
                }

                // Thiết lập tự động chuyển banner sau mỗi 4 giây
                setupAutoScrollForBanners(resource.data.size)
            } else {
                binding.cvPromoContainer.visibility = View.GONE
                binding.tabLayoutPromo.visibility = View.GONE
                binding.tvSeeAllPromotions.visibility = View.GONE
                bannerRunnable?.let { bannerHandler.removeCallbacks(it) }
            }
        }

        // Xử lý làm mới dữ liệu khi kéo từ trên xuống
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadHomeData()
            binding.swipeRefresh.isRefreshing = false
        }

        // Điều hướng từ các nút trên Header (Thanh tiêu đề)
        binding.btnNotification.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.notificationFragment)
        }

        binding.ivUserAvatar.setOnClickListener {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)
            if (bottomNav != null && bottomNav.visibility == View.VISIBLE) {
                bottomNav.selectedItemId = R.id.profileFragment
            } else {
                Navigation.findNavController(it).navigate(R.id.profileFragment)
            }
        }

        binding.btnLocation.setOnClickListener {
            SnackbarHelper.showSuccess(binding.root, "Bộ lọc địa điểm sẽ sớm ra mắt!")
        }

        // Xử lý các sự kiện bấm nhanh (Quick Booking Flow)
        binding.btnQuickMovie.setOnClickListener { showMovieBottomSheet() }
        binding.btnQuickCinema.setOnClickListener { showCinemaBottomSheet() }
        binding.btnQuickDate.setOnClickListener { showDateBottomSheet() }

        viewModel.getQuickSelectedMovie().observe(viewLifecycleOwner) { movie ->
            binding.tvQuickMovie.text = movie?.title ?: "Phim"
        }

        viewModel.getQuickSelectedCinema().observe(viewLifecycleOwner) { cinema ->
            binding.tvQuickCinema.text = cinema?.name ?: "Rạp"
        }

        viewModel.getQuickSelectedDate().observe(viewLifecycleOwner) { date ->
            binding.tvQuickDate.text = date ?: "Ngày"
        }

        binding.btnQuickSubmit.setOnClickListener {
            val movie = viewModel.getQuickSelectedMovie().value
            val cinema = viewModel.getQuickSelectedCinema().value
            val date = viewModel.getQuickSelectedDate().value

            if (movie == null || cinema == null || date == null) {
                SnackbarHelper.showError(binding.root, "Vui lòng chọn đủ Phim, Rạp và Ngày!")
                return@setOnClickListener
            }

            val args = Bundle().apply {
                putString("movieId", movie.id)
            }
            Navigation.findNavController(it).navigate(R.id.selectShowtimeFragment, args)
        }

        // Xử lý logic tìm kiếm (Search) với Debounce để giảm tải cho Server
        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.viewSearchDim.visibility = View.VISIBLE
            } else if (binding.etSearch.text.toString().isEmpty()) {
                binding.viewSearchDim.visibility = View.GONE
                binding.rvSearchResults.visibility = View.GONE
            }
        }

        // Đóng vùng tìm kiếm khi bấm vào vùng mờ (Dim Background)
        binding.viewSearchDim.setOnClickListener {
            binding.etSearch.clearFocus()
            binding.viewSearchDim.visibility = View.GONE
            binding.rvSearchResults.visibility = View.GONE
            // Ẩn bàn phím ảo
            val view1 = requireActivity().currentFocus
            if (view1 != null) {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(view1.windowToken, 0)
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                val query = s.toString().trim()
                if (query.isEmpty()) {
                    binding.rvSearchResults.visibility = View.GONE
                    if (!binding.etSearch.hasFocus()) {
                        binding.viewSearchDim.visibility = View.GONE
                    }
                    viewModel.clearSearch()
                } else if (query.length >= 2) {
                    binding.viewSearchDim.visibility = View.VISIBLE
                    searchRunnable = Runnable { viewModel.searchMovies(query) }
                    searchHandler.postDelayed(searchRunnable!!, 500) // 500ms Debounce
                }
            }
            override fun afterTextChanged(s: Editable) {}
        })

        viewModel.getSearchResults().observe(viewLifecycleOwner) { resource ->
            if (resource == null || resource.status == Resource.Status.LOADING) {
                return@observe
            }
            if (resource.isSuccess && resource.data != null && !resource.data.content.isNullOrEmpty()) {
                binding.rvSearchResults.visibility = View.VISIBLE
                binding.rvSearchResults.adapter = MovieAdapter(resource.data.content!!) { movieId ->
                    navigateToDetail(view, movieId)
                }
            } else {
                binding.rvSearchResults.visibility = View.GONE
            }
        }
    }

    private fun setupAutoScrollForHero(size: Int) {
        if (size <= 1) return
        heroRunnable?.let { heroHandler.removeCallbacks(it) }
        heroRunnable = Runnable {
            val current = binding.vpHero.currentItem
            val next = (current + 1) % size
            binding.vpHero.setCurrentItem(next, true)
        }
        heroHandler.postDelayed(heroRunnable!!, 5000) // 5s interval for Hero
    }

    private fun updateHeroOverlay(movie: MovieSummary) {
        binding.tvHeroTitle.text = movie.title?.uppercase(Locale.getDefault()) ?: ""
        binding.btnBookHero.visibility = View.VISIBLE
        binding.btnBookHero.setOnClickListener { movie.id?.let { id -> navigateToDetail(requireView(), id) } }
    }

    private fun setupAutoScrollForBanners(size: Int) {
        if (size <= 1) return // No need to scroll if only 1 banner

        bannerRunnable?.let { bannerHandler.removeCallbacks(it) }

        bannerRunnable = Runnable {
            val currentItem = binding.vpPromotions.currentItem
            val nextItem = (currentItem + 1) % size
            binding.vpPromotions.setCurrentItem(nextItem, true)
        }

        bannerHandler.postDelayed(bannerRunnable!!, 4000)

        binding.vpPromotions.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bannerRunnable?.let {
                    bannerHandler.removeCallbacks(it)
                    bannerHandler.postDelayed(it, 4000)
                }
            }
        })
    }

    private fun <T> chunkList(list: List<T>, chunkSize: Int): List<List<T>> {
        val chunks = mutableListOf<List<T>>()
        var i = 0
        while (i < list.size) {
            chunks.add(list.subList(i, Math.min(list.size, i + chunkSize)))
            i += chunkSize
        }
        return chunks
    }

    private fun observeMovies(nowShowing: Boolean) {
        if (nowShowing) {
            viewModel.getNowShowing().observe(viewLifecycleOwner) { resource ->
                if (resource.isSuccess && resource.data != null && resource.data.content != null) {
                    val pages = chunkList(resource.data.content!!, 6)
                    binding.rvMovies.adapter = MoviePageAdapter(pages) { movieId ->
                        navigateToDetail(requireView(), movieId)
                    }
                }
            }
        } else {
            viewModel.getComingSoon().observe(viewLifecycleOwner) { resource ->
                if (resource.isSuccess && resource.data != null && resource.data.content != null) {
                    val pages = chunkList(resource.data.content!!, 6)
                    binding.rvMovies.adapter = MoviePageAdapter(pages) { movieId ->
                        navigateToDetail(requireView(), movieId)
                    }
                }
            }
        }
    }

    private fun navigateToDetail(view: View, movieId: String) {
        val args = Bundle().apply {
            putString("movieId", movieId)
        }
        Navigation.findNavController(view).navigate(R.id.movieDetailFragment, args)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bannerRunnable?.let { bannerHandler.removeCallbacks(it) }
        heroRunnable?.let { heroHandler.removeCallbacks(it) }
        _binding = null
    }

    private fun showMovieBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val listView = ListView(requireContext())

        val resource = viewModel.getNowShowing().value
        if (resource == null || !resource.isSuccess || resource.data == null || resource.data.content == null) {
            SnackbarHelper.showError(binding.root, "Không thể tải danh sách phim")
            return
        }

        val movies = resource.data.content!!
        val movieTitles = movies.map { it.title }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, movieTitles)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            viewModel.getQuickSelectedMovie().value = movies[position]
            // Reset cascading selections
            viewModel.getQuickSelectedCinema().value = null
            viewModel.getQuickSelectedDate().value = null
            dialog.dismiss()
            showCinemaBottomSheet() // Auto cascade
        }

        dialog.setContentView(listView)
        dialog.show()
    }

    private fun showCinemaBottomSheet() {
        if (viewModel.getQuickSelectedMovie().value == null) {
            SnackbarHelper.showError(binding.root, "Vui lòng chọn Phim trước!")
            return
        }

        val dialog = BottomSheetDialog(requireContext())
        val listView = ListView(requireContext())

        viewModel.getCinemas("").observe(viewLifecycleOwner) { resource ->
            if (resource.isSuccess && resource.data != null) {
                val cinemas = resource.data
                val cinemaNames = cinemas.map { it.name }

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, cinemaNames)
                listView.adapter = adapter
                listView.setOnItemClickListener { _, _, position, _ ->
                    viewModel.getQuickSelectedCinema().value = cinemas[position]
                    viewModel.getQuickSelectedDate().value = null
                    dialog.dismiss()
                    showDateBottomSheet() // Auto cascade
                }

                dialog.setContentView(listView)
                if (!dialog.isShowing) dialog.show()
            }
        }
    }

    private fun showDateBottomSheet() {
        if (viewModel.getQuickSelectedCinema().value == null) {
            SnackbarHelper.showError(binding.root, "Vui lòng chọn Rạp trước!")
            return
        }

        val dialog = BottomSheetDialog(requireContext())
        val listView = ListView(requireContext())

        val dates = mutableListOf<String>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        for (i in 0 until 7) {
            dates.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, dates)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            viewModel.getQuickSelectedDate().value = dates[position]
            dialog.dismiss()
        }

        dialog.setContentView(listView)
        dialog.show()
    }
}
