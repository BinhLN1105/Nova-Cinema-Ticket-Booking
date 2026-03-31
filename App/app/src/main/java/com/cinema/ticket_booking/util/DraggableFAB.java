package com.cinema.ticket_booking.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class DraggableFAB extends FloatingActionButton implements View.OnTouchListener {

    private final static float CLICK_DRAG_TOLERANCE = 10; // Ngưỡng phân biệt giữa Click và Kéo (Đơn vị: pixels)
    private float downRawX, downRawY;
    private float dX, dY;

    public DraggableFAB(@NonNull Context context) {
        super(context);
        init();
    }

    public DraggableFAB(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DraggableFAB(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        
        // 1. Khi người dùng chạm ngón tay xuống
        if (action == MotionEvent.ACTION_DOWN) {
            downRawX = motionEvent.getRawX();
            downRawY = motionEvent.getRawY();
            dX = view.getX() - downRawX;
            dY = view.getY() - downRawY;
            return true; // Xác nhận đã xử lý sự kiện Touch
            
        // 2. Khi người dùng di chuyển ngón tay (Kéo)
        } else if (action == MotionEvent.ACTION_MOVE) {
            int viewWidth = view.getWidth();
            int viewHeight = view.getHeight();

            View viewParent = (View) view.getParent();
            int parentWidth = viewParent.getWidth();
            int parentHeight = viewParent.getHeight();

            // Tính toán vị trí X mới và đảm bảo không vượt quá biên trái/phải của màn hình
            float newX = motionEvent.getRawX() + dX;
            newX = Math.max(0, Math.min(parentWidth - viewWidth, newX));

            // Tính toán vị trí Y mới và đảm bảo không vượt quá biên trên/dưới của màn hình
            float newY = motionEvent.getRawY() + dY;
            newY = Math.max(0, Math.min(parentHeight - viewHeight, newY));

            // Cập nhật vị trí của View ngay lập tức (không có độ trễ)
            view.animate()
                    .x(newX)
                    .y(newY)
                    .setDuration(0)
                    .start();

            return true;
            
        // 3. Khi người dùng nhấc ngón tay lên
        } else if (action == MotionEvent.ACTION_UP) {
            float upRawX = motionEvent.getRawX();
            float upRawY = motionEvent.getRawY();

            float distanceX = upRawX - downRawX;
            float distanceY = upRawY - downRawY;

            // Nếu khoảng cách di chuyển rất nhỏ -> Coi là hành động Click
            if (Math.abs(distanceX) < CLICK_DRAG_TOLERANCE && Math.abs(distanceY) < CLICK_DRAG_TOLERANCE) {
                return performClick();
            } else {
                // Nếu là hành động Kéo -> Thực hiện hiệu ứng hít vào cạnh màn hình gần nhất
                snapToEdge();
                return true;
            }
        }
        return false;
    }

    /**
     * Hiệu ứng "Hít vào cạnh": Tự động di chuyển nút về lề trái hoặc lề phải
     * dựa trên vị trí hiện tại của nút so với trục giữa màn hình.
     */
    private void snapToEdge() {
        View viewParent = (View) getParent();
        int parentWidth = viewParent.getWidth();
        float currentX = getX();
        float middle = parentWidth / 2f;

        float targetX;
        // Nếu tâm nút nằm bên trái trục giữa -> Di chuyển về lề trái
        if (currentX + getWidth() / 2f < middle) {
            targetX = 16 * getResources().getDisplayMetrics().density; // Margin lề trái: 16dp
        } else {
            // Nếu tâm nút nằm bên phải trục giữa -> Di chuyển về lề phải
            targetX = parentWidth - getWidth() - (16 * getResources().getDisplayMetrics().density); // Margin lề phải: 16dp
        }

        // Thực hiện hiệu ứng di chuyển mượt mà trong 300ms
        animate().x(targetX)
                .setDuration(300)
                .start();
    }
}
