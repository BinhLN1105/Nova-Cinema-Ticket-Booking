import { create } from "zustand";
import { persist } from "zustand/middleware";
import { bookingApi } from "@/api/endpoints";

const initialState = {
  selectedMovie: null,
  selectedShowtime: null,
  selectedDate: new Date().toISOString().split("T")[0],
  selectedSeats: [],
  selectedCombos: {},
  appliedVoucher: null,
  subtotal: 0,
  promotionDiscount: 0,
  appliedPromotionName: null,
  appliedPromotionName: null,
  discount: 0,
  total: 0,
  originalTotal: 0,
  warningMessage: null,
  expiryTime: null,
};

let debounceTimeout = null;

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
        set({ appliedVoucher: null, warningMessage: null });
        get().calculateTotals();
      },

      calculateTotals: () => {
        const { selectedShowtime, selectedSeats, selectedCombos, appliedVoucher } = get();
        
        if (!selectedShowtime || selectedSeats.length === 0) {
            set({ subtotal: 0, promotionDiscount: 0, discount: 0, total: 0 });
            return;
        }

        // DEBOUNCE: Clear previous timer
        if (debounceTimeout) clearTimeout(debounceTimeout);

        debounceTimeout = setTimeout(async () => {
          try {
            const request = {
              showtimeId: selectedShowtime.id,
              showtimeSeatIds: selectedSeats.map(s => s.showtimeSeatId),
              combos: Object.entries(selectedCombos).map(([id, qty]) => ({
                comboId: id,
                quantity: qty
              })),
              voucherCode: appliedVoucher?.code || null
            };

            const response = await bookingApi.getQuote(request);
            
            set({
              subtotal: response.subtotal,
              promotionDiscount: response.promotionDiscountAmount,
              appliedPromotionName: response.appliedPromotionName,
              discount: response.discountAmount,
              total: response.totalAmount,
              originalTotal: response.totalOriginalAmount,
              warningMessage: response.warningMessage
            });
          } catch (error) {
            console.error("Lỗi khi tính toán báo giá:", error);
          }
        }, 500);
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
