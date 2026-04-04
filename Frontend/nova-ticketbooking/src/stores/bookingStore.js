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
        get().updateLocalSubtotal();
      },

      clearSeats: () => {
        set({ selectedSeats: [] });
        get().updateLocalSubtotal();
      },

      setComboQty: (comboId, qty, comboPrice = 0) => {
        set((state) => ({
          selectedCombos:
            qty > 0
              ? { ...state.selectedCombos, [comboId]: { quantity: qty, price: comboPrice } }
              : Object.fromEntries(
                  Object.entries(state.selectedCombos).filter(
                    ([k]) => k !== comboId,
                  ),
                ),
        }));
        get().updateLocalSubtotal();
      },

      updateLocalSubtotal: () => {
        const { selectedSeats, selectedCombos } = get();
        
        const seatTotal = selectedSeats.reduce((acc, s) => acc + (s.price || 0), 0);
        const comboTotal = Object.values(selectedCombos).reduce((acc, c) => acc + (c.price * c.quantity), 0);
        
        const newSubtotal = seatTotal + comboTotal;
        set({ 
            subtotal: newSubtotal,
            total: newSubtotal // Temporary total for selection pages
        });
      },

      applyVoucher: (voucher) => {
        set({ appliedVoucher: voucher });
        get().fetchServerQuote();
      },

      clearVoucher: () => {
        set({ appliedVoucher: null, warningMessage: null });
        get().updateLocalSubtotal(); // Return to local total
      },

      fetchServerQuote: async () => {
        const { selectedShowtime, selectedSeats, selectedCombos, appliedVoucher } = get();
        
        if (!selectedShowtime || selectedSeats.length === 0) {
            set({ subtotal: 0, promotionDiscount: 0, discount: 0, total: 0 });
            return;
        }

        try {
            const request = {
              showtimeId: selectedShowtime.id,
              showtimeSeatIds: selectedSeats.map(s => s.showtimeSeatId),
              combos: Object.entries(selectedCombos).map(([id, data]) => ({
                comboId: id,
                quantity: data.quantity
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
            return response;
        } catch (error) {
            console.error("Lỗi khi tính toán báo giá:", error);
            throw error;
        }
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
