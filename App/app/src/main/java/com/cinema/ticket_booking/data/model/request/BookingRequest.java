package com.cinema.ticket_booking.data.model.request;
import java.util.List;
public class BookingRequest {
    private String showtimeId;
    private List<String> showtimeSeatIds;
    private List<ComboItem> combos;
    private String voucherCode;
    public BookingRequest(String showtimeId, List<String> showtimeSeatIds,
                          List<ComboItem> combos, String voucherCode) {
        this.showtimeId = showtimeId; this.showtimeSeatIds = showtimeSeatIds;
        this.combos = combos; this.voucherCode = voucherCode;
    }
    public static class ComboItem {
        private String comboId; private int quantity;
        public ComboItem(String comboId, int quantity) {
            this.comboId = comboId; this.quantity = quantity;
        }
    }
}
