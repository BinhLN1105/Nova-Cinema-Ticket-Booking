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
      console.log("Booking created successfully:", booking);
      window.scrollTo(0, 0);
      if (booking?.id) {
        navigate(`/booking/payment/${booking.id}`);
        // Delay reset để đảm bảo không mất state khi redirect
        setTimeout(() => store.reset(), 500);
      } else {
        toast.error("Không tìm thấy mã đơn hàng trả về từ Backend");
        console.error("Missing booking ID in response:", booking);
      }
    },
    onError: (error) => {
      const msg = error.response?.data?.message || "Đặt vé thất bại, vui lòng thử lại";
      toast.error(msg);
    },
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
      showtimeSeatIds: store.selectedSeats.map((s) => s.showtimeSeatId),
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
