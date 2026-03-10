import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Shield, CreditCard } from "lucide-react";
import { bookingApi } from "@/api/endpoints";

export default function PaymentPage() {
  const { id } = useParams();

  const { data: booking } = useQuery({
    queryKey: ["booking", id],
    queryFn: () => bookingApi.getById(id),
    enabled: !!id,
  });

  const handleVNPay = async () => {
    if (!id) return;
    const payment = await bookingApi.createPayment(id);
    window.location.href = payment.paymentUrl;
  };

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-16 flex items-start justify-center">
      <div className="max-w-md w-full px-4 sm:px-6">
        <h1 className="font-display text-3xl font-bold text-white mb-2 text-center">
          Thanh toán
        </h1>
        <p className="text-cinema-300 text-center mb-8">
          Chọn phương thức thanh toán
        </p>

        <div className="card-cinema p-5 mb-4 space-y-3">
          {booking && (
            <>
              <div className="flex justify-between text-sm">
                <span className="text-cinema-400">Mã đặt vé</span>
                <span className="text-white font-mono font-bold">
                  {booking.bookingCode}
                </span>
              </div>
              <div className="flex justify-between font-bold text-lg">
                <span className="text-white">Tổng tiền</span>
                <span className="text-brand-400">
                  {new Intl.NumberFormat("vi-VN", {
                    style: "currency",
                    currency: "VND",
                  }).format(booking.totalAmount)}
                </span>
              </div>
            </>
          )}
        </div>

        <button
          onClick={handleVNPay}
          className="w-full flex items-center justify-center gap-3 p-5 rounded-2xl
          bg-blue-600 hover:bg-blue-500 border border-blue-500 transition-all
          text-white font-semibold text-base shadow-glow-blue"
        >
          <CreditCard className="w-5 h-5" />
          Thanh toán qua VNPay
        </button>

        <div className="flex items-center justify-center gap-2 mt-6 text-cinema-400 text-xs">
          <Shield className="w-4 h-4" />
          Thanh toán được mã hóa và bảo mật
        </div>
      </div>
    </div>
  );
}
