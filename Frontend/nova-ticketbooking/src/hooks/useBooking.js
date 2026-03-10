import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { useBookingStore } from "@/stores/bookingStore";
import { bookingApi } from "@/api/endpoints";

export function useBooking() {
  const store = useBookingStore();
  const navigate = useNavigate();

  const createBookingMutation = useMutation({
    mutationFn: bookingApi.create,
    onSuccess: (booking) => {
      navigate(`/booking/payment/${booking.id}`);
    },
    onError: () => toast.error("Đặt vé thất bại, vui lòng thử lại"),
  });

  const createPaymentMutation = useMutation({
    mutationFn: (bookingId) => bookingApi.createPayment(bookingId),
    onSuccess: (data) => {
      window.location.href = data.paymentUrl;
    },
  });

  const confirmBooking = () => {
    if (!store.selectedShowtime || store.selectedSeats.length === 0) return;
    createBookingMutation.mutate({
      showtimeId: store.selectedShowtime.id,
      seatIds: store.selectedSeats.map((s) => s.showtimeSeatId),
      combos: Object.entries(store.selectedCombos).map(
        ([comboId, quantity]) => ({
          comboId,
          quantity,
        }),
      ),
      voucherCode: store.appliedVoucher?.code,
    });
  };

  return {
    ...store,
    confirmBooking,
    createPaymentMutation,
    isConfirming: createBookingMutation.isPending,
  };
}
