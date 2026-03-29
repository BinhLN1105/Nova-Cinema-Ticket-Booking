import { create } from "zustand";
import { persist } from "zustand/middleware";

const initialState = {
  selectedMovie: null,
  selectedShowtime: null,
  selectedDate: new Date().toISOString().split("T")[0],
  selectedSeats: [],
  selectedCombos: {},
  appliedVoucher: null,
  subtotal: 0,
  discount: 0,
  discount: 0,
  total: 0,
  expiryTime: null,
};

export const useBookingStore = create(
  persist(
    (set, get) => ({
      ...initialState,

      setMovie: (movie) => set({ selectedMovie: movie }),
  setShowtime: (showtime) => set({ selectedShowtime: showtime }),
  setDate: (date) => set({ selectedDate: date }),

  toggleSeat: (seat) => {
    const { selectedSeats } = get();
    const exists = selectedSeats.find(
      (s) => s.showtimeSeatId === seat.showtimeSeatId,
    );
    const updated = exists
      ? selectedSeats.filter((s) => s.showtimeSeatId !== seat.showtimeSeatId)
      : [...selectedSeats, seat];
    set({ selectedSeats: updated });
    get().calculateTotals();
  },

  clearSeats: () => {
    set({ selectedSeats: [] });
    get().calculateTotals();
  },

  setComboQty: (comboId, qty) => {
    set((state) => ({
      selectedCombos:
        qty > 0
          ? { ...state.selectedCombos, [comboId]: qty }
          : Object.fromEntries(
              Object.entries(state.selectedCombos).filter(
                ([k]) => k !== comboId,
              ),
            ),
    }));
    get().calculateTotals();
  },

  applyVoucher: (voucher) => {
    set({ appliedVoucher: voucher });
    get().calculateTotals();
  },

  clearVoucher: () => {
    set({ appliedVoucher: null });
    get().calculateTotals();
  },

  calculateTotals: (manualSubtotal = null) => {
    const { selectedSeats, selectedCombos, appliedVoucher } = get();
    
    // 1. TỔNG TIỀN VÉ (Cơ bản)
    const ticketSubtotal = selectedSeats.reduce((sum, s) => sum + s.price, 0);
    
    // Nếu có subtotal truyền từ UI (đã tính kèm Combo), ưu tiên dùng nó
    let currentSubtotal = manualSubtotal !== null ? manualSubtotal : ticketSubtotal;
    
    // 2. TÍNH KHUYẾN MÃI HỆ THỐNG (Order Level - Giảm 20k nếu > 120k)
    // Tương ứng logic PricingEngineServiceImpl.java
    let promotionDiscount = 0;
    let appliedPromotionName = null;

    if (currentSubtotal >= 120000) {
      promotionDiscount = 20000;
      appliedPromotionName = "Cuối tháng vui vẻ";
    }

    // Giá sau khi trừ KM hệ thống (Cơ sở để tính Voucher)
    const subtotalAfterPromo = Math.max(0, currentSubtotal - promotionDiscount);

    let discount = 0; // Voucher Discount
    if (appliedVoucher && subtotalAfterPromo >= (appliedVoucher.minOrder || 0)) {
      if (appliedVoucher.discountType === "PERCENTAGE") {
          const calculated = (subtotalAfterPromo * appliedVoucher.discountValue) / 100;
          discount = appliedVoucher.maxDiscount ? Math.min(calculated, appliedVoucher.maxDiscount) : calculated;
      } else {
          discount = appliedVoucher.discountValue;
      }
    }

    set({ 
      subtotal: subtotalAfterPromo, 
      promotionDiscount, 
      appliedPromotionName,
      discount, 
      total: subtotalAfterPromo - discount 
    });
  },


      setExpiryTime: (timestamp) => set({ expiryTime: timestamp }),
      clearExpiryTime: () => set({ expiryTime: null }),

      reset: () => set(initialState),
    }),
    {
      name: "booking-storage",
    }
  )
);
