import { create } from "zustand";

const initialState = {
  selectedMovie: null,
  selectedShowtime: null,
  selectedDate: new Date().toISOString().split("T")[0],
  selectedSeats: [],
  selectedCombos: {},
  appliedVoucher: null,
  subtotal: 0,
  discount: 0,
  total: 0,
};

export const useBookingStore = create((set, get) => ({
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

  calculateTotals: () => {
    const { selectedSeats, appliedVoucher } = get();
    const subtotal = selectedSeats.reduce((sum, s) => sum + s.price, 0);

    let discount = 0;
    if (appliedVoucher && subtotal >= appliedVoucher.minOrder) {
      discount =
        appliedVoucher.discountType === "PERCENTAGE"
          ? Math.min(
              (subtotal * appliedVoucher.discountValue) / 100,
              appliedVoucher.minOrder,
            )
          : appliedVoucher.discountValue;
    }

    set({ subtotal, discount, total: subtotal - discount });
  },

  reset: () => set(initialState),
}));
