package com.cinema.ticket_booking.util

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.abs

class DraggableFAB @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr), View.OnTouchListener {

    companion object {
        private const val CLICK_DRAG_TOLERANCE = 10f // Ngưỡng phân biệt giữa Click và Kéo (Đơn vị: pixels)
    }

    private var downRawX = 0f
    private var downRawY = 0f
    private var dX = 0f
    private var dY = 0f

    init {
        setOnTouchListener(this)
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        val action = motionEvent.action

        // 1. Khi người dùng chạm ngón tay xuống
        if (action == MotionEvent.ACTION_DOWN) {
            downRawX = motionEvent.rawX
            downRawY = motionEvent.rawY
            dX = view.x - downRawX
            dY = view.y - downRawY
            return true // Xác nhận đã xử lý sự kiện Touch

        // 2. Khi người dùng di chuyển ngón tay (Kéo)
        } else if (action == MotionEvent.ACTION_MOVE) {
            val viewWidth = view.width
            val viewHeight = view.height

            val viewParent = view.parent as View
            val parentWidth = viewParent.width
            val parentHeight = viewParent.height

            // Tính toán vị trí X mới và đảm bảo không vượt quá biên trái/phải của màn hình
            var newX = motionEvent.rawX + dX
            newX = Math.max(0f, Math.min((parentWidth - viewWidth).toFloat(), newX))

            // Tính toán vị trí Y mới và đảm bảo không vượt quá biên trên/dưới của màn hình
            var newY = motionEvent.rawY + dY
            newY = Math.max(0f, Math.min((parentHeight - viewHeight).toFloat(), newY))

            // Cập nhật vị trí của View ngay lập tức (không có độ trễ)
            view.animate()
                .x(newX)
                .y(newY)
                .setDuration(0)
                .start()

            return true

        // 3. Khi người dùng nhấc ngón tay lên
        } else if (action == MotionEvent.ACTION_UP) {
            val upRawX = motionEvent.rawX
            val upRawY = motionEvent.rawY

            val distanceX = upRawX - downRawX
            val distanceY = upRawY - downRawY

            // Nếu khoảng cách di chuyển rất nhỏ -> Coi là hành động Click
            if (abs(distanceX) < CLICK_DRAG_TOLERANCE && abs(distanceY) < CLICK_DRAG_TOLERANCE) {
                return performClick()
            } else {
                // Nếu là hành động Kéo -> Thực hiện hiệu ứng hít vào cạnh màn hình gần nhất
                snapToEdge()
                return true
            }
        }
        return false
    }

    /**
     * Hiệu ứng "Hít vào cạnh": Tự động di chuyển nút về lề trái hoặc lề phải
     * dựa trên vị trí hiện tại của nút so với trục giữa màn hình.
     */
    private fun snapToEdge() {
        val viewParent = parent as View
        val parentWidth = viewParent.width
        val currentX = x
        val middle = parentWidth / 2f

        val targetX: Float
        // Nếu tâm nút nằm bên trái trục giữa -> Di chuyển về lề trái
        if (currentX + width / 2f < middle) {
            targetX = 16 * resources.displayMetrics.density // Margin lề trái: 16dp
        } else {
            // Nếu tâm nút nằm bên phải trục giữa -> Di chuyển về lề phải
            targetX = parentWidth - width - (16 * resources.displayMetrics.density) // Margin lề phải: 16dp
        }

        // Thực hiện hiệu ứng di chuyển mượt mà trong 300ms
        animate().x(targetX)
            .setDuration(300)
            .start()
    }
}
