package com.cinema.ticket_booking.ui.chatbot

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.request.BookingRequest
import com.cinema.ticket_booking.data.model.response.BookingResponse
import com.cinema.ticket_booking.data.repository.BookingRepository
import com.cinema.ticket_booking.data.repository.ChatbotRepository
import com.cinema.ticket_booking.data.repository.ShowtimeRepository
import com.cinema.ticket_booking.ui.booking.SelectComboViewModel
import com.cinema.ticket_booking.ui.booking.SelectSeatViewModel
import com.cinema.ticket_booking.ui.booking.SelectShowtimeViewModel
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.ArrayList
import javax.inject.Inject

@HiltViewModel
class ChatbotViewModel @Inject constructor(
    private val chatbotRepo: ChatbotRepository,
    private val bookingRepo: BookingRepository,
    private val showtimeRepo: ShowtimeRepository
) : ViewModel() {

    companion object {
        const val WELCOME_TEXT = "Xin chào! Em là Nova, trợ lý ảo của NovaTicket. Em có thể giúp gì cho anh/chị ạ?"
    }

    private val _messages = MutableLiveData<List<ChatMessage>>(ArrayList())
    val messages: LiveData<List<ChatMessage>> = _messages

    init {
        resetToWelcomeMessage()
    }

    private fun resetToWelcomeMessage() {
        val initial = ArrayList<ChatMessage>()
        initial.add(ChatMessage(WELCOME_TEXT, false))
        _messages.value = initial
    }

    fun clearChatSession(onComplete: (() -> Unit)? = null) {
        chatbotRepo.clearSession().observeForever {
            resetToWelcomeMessage()
            onComplete?.invoke()
        }
    }

    fun sendMessage(msg: String) {
        val currentList = ArrayList<ChatMessage>(_messages.value ?: ArrayList())

        // Add User Message
        currentList.add(ChatMessage.createUserMessage(msg))

        // Add Loading Indicator
        currentList.add(ChatMessage.createLoadingMessage())
        _messages.value = currentList

        chatbotRepo.sendMessage(msg).observeForever { resource ->
            val updateList = ArrayList<ChatMessage>(_messages.value ?: ArrayList())

            // Ensure loading indicator is removed
            updateList.removeIf { it.isLoading }

            when (resource.status) {
                Resource.Status.SUCCESS -> {
                    if (!resource.data.isNullOrEmpty()) {
                        updateList.add(ChatMessage.createBotMessage(resource.data))
                    } else {
                        updateList.add(ChatMessage.createBotMessage("Em xin lỗi, em chưa hiểu ý anh/chị lắm. Anh/chị có thể hỏi lại được không ạ?"))
                    }
                }
                Resource.Status.ERROR -> {
                    val errorMsg = resource.message ?: "Không thể kết nối với Nova Assistant. Vui lòng kiểm tra mạng!"
                    updateList.add(ChatMessage.createBotMessage("⚠️ $errorMsg"))
                }
                Resource.Status.LOADING -> return@observeForever
            }
            _messages.value = updateList
        }
    }

    /**
     * Nạp dữ liệu từ vé nháp (Draft Booking), kiểm tra hợp lệ toàn diện,
     * và chỉ khi thành công 100% mới cập nhật state để điều hướng sang ConfirmBookingFragment.
     */
    fun prepareDraftBooking(
        draftId: String,
        onResult: (isSuccess: Boolean, errorMessage: String?, quote: BookingResponse?) -> Unit
    ) {
        bookingRepo.getDraftBooking(draftId).observeForever { draftRes ->
            when (draftRes.status) {
                Resource.Status.LOADING -> return@observeForever
                Resource.Status.ERROR -> {
                    val errMsg = draftRes.message ?: "Đơn vé nháp không tồn tại hoặc đã hết hạn (tối đa 10 phút). Vui lòng yêu cầu Nova tạo lại nhé!"
                    onResult(false, errMsg, null)
                }
                Resource.Status.SUCCESS -> {
                    val draft = draftRes.data
                    if (draft == null || draft.showtimeId.isNullOrEmpty() || draft.showtimeSeatIds.isNullOrEmpty()) {
                        onResult(false, "Dữ liệu vé nháp không đầy đủ. Vui lòng tạo lại yêu cầu đặt vé!", null)
                        return@observeForever
                    }

                    // Tải thông tin Suất chiếu để lấy Tên phim, Rạp, Giờ chiếu, Poster
                    showtimeRepo.getShowtimeById(draft.showtimeId).observeForever { showtimeRes ->
                        val showtime = showtimeRes.data

                        // Chuẩn bị danh sách Combo
                        val comboItems = ArrayList<BookingRequest.ComboItem>()
                        draft.combos?.forEach { c ->
                            if (!c.comboId.isNullOrEmpty() && c.quantity > 0) {
                                comboItems.add(BookingRequest.ComboItem(c.comboId, c.quantity))
                            }
                        }

                        // Lấy báo giá chính thức từ Server
                        val quoteReq = BookingRequest(
                            showtimeId = draft.showtimeId,
                            showtimeSeatIds = draft.showtimeSeatIds,
                            combos = comboItems,
                            voucherCode = null
                        )

                        bookingRepo.getBookingQuote(quoteReq).observeForever quoteObs@{ quoteRes ->
                            when (quoteRes.status) {
                                Resource.Status.LOADING -> return@quoteObs
                                Resource.Status.ERROR -> {
                                    onResult(false, quoteRes.message ?: "Không thể tính toán báo giá vé nháp", null)
                                }
                                Resource.Status.SUCCESS -> {
                                    val quote = quoteRes.data
                                    if (quote != null) {
                                        // Ghi nhận Atomic Pending State cho toàn bộ Booking Flow
                                        SelectShowtimeViewModel.pendingShowtimeId = draft.showtimeId
                                        SelectShowtimeViewModel.pendingMovieTitle = showtime?.movieTitle ?: quote.movieTitle
                                        SelectShowtimeViewModel.pendingCinemaName = showtime?.cinemaName ?: quote.cinemaName
                                        SelectShowtimeViewModel.pendingMoviePoster = showtime?.moviePosterUrl ?: quote.moviePosterUrl

                                        val rawTime = showtime?.startTime ?: quote.startTime
                                        if (rawTime != null && rawTime.contains("T")) {
                                            val parts = rawTime.split("T")
                                            SelectShowtimeViewModel.pendingShowDate = parts.getOrNull(0)
                                            SelectShowtimeViewModel.pendingShowtimeTime = parts.getOrNull(1)?.take(5)
                                        } else {
                                            SelectShowtimeViewModel.pendingShowtimeTime = rawTime
                                        }

                                        SelectSeatViewModel.pendingSeatIds = draft.showtimeSeatIds
                                        SelectSeatViewModel.pendingTotalAmount = quote.totalAmount ?: 0.0

                                        SelectComboViewModel.pendingCombos.clear()
                                        draft.combos?.forEach { c ->
                                            if (!c.comboId.isNullOrEmpty() && c.quantity > 0) {
                                                SelectComboViewModel.pendingCombos[c.comboId] = c.quantity
                                            }
                                        }

                                        onResult(true, null, quote)
                                    } else {
                                        onResult(false, "Lỗi tính toán báo giá", null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
