package com.cinema.ticket_booking.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffDashboardResponse {

    /** Số suất chiếu có lịch hôm nay tại rạp của nhân viên */
    private long totalShowtimesToday;

    /** Số vé đã soát (check-in) hôm nay tại rạp của nhân viên */
    private long ticketsCheckedToday;

    /** Số vé đã soát (check-in) trong tháng này tại rạp của nhân viên */
    private long ticketsCheckedThisMonth;
}
