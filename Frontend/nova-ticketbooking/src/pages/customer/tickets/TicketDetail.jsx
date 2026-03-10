import { useParams, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useQuery, useMutation } from "@tanstack/react-query";
import { QRCodeSVG } from "qrcode.react";
import { ArrowLeft, MapPin, Clock, Monitor, Ticket, X } from "lucide-react";
import { bookingApi } from "@/api/endpoints";
import { formatDateTime, formatCurrency, getStatusBadge, cn } from "@/utils";
import toast from "react-hot-toast";

export default function TicketDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  const {
    data: booking,
    refetch,
    isLoading,
  } = useQuery({
    queryKey: ["booking", id],
    queryFn: () => bookingApi.getById(id),
    enabled: !!id,
  });

  const cancelMutation = useMutation({
    mutationFn: () => bookingApi.cancel(id),
    onSuccess: () => {
      toast.success("Đã hủy vé");
      refetch();
    },
  });

  if (isLoading)
    return (
      <div className="min-h-screen bg-cinema-900 pt-24 flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-brand-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );

  if (!booking) return null;
  const badge = getStatusBadge(booking.status);

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-16">
      <div className="max-w-md mx-auto px-4 sm:px-6">
        <div className="flex items-center gap-4 mb-8">
          <button
            onClick={() => navigate(-1)}
            className="p-2.5 rounded-xl glass border border-white/8 text-cinema-200 hover:text-white transition-all"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <h1 className="font-display text-2xl font-bold text-white">
            Chi tiết vé
          </h1>
        </div>

        {/* Ticket card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="relative"
        >
          {/* Top part */}
          <div className="card-cinema p-5 rounded-b-none border-b-0">
            <div className="flex gap-4 mb-5">
              <img
                src={booking.moviePosterUrl}
                alt={booking.movieTitle}
                className="w-20 h-28 object-cover rounded-xl flex-shrink-0"
              />
              <div className="flex-1">
                <div className="flex items-start justify-between gap-2">
                  <h2 className="font-display font-bold text-white text-lg leading-tight">
                    {booking.movieTitle}
                  </h2>
                  <span
                    className={cn(
                      "badge text-xs flex-shrink-0",
                      `badge-${badge.color}`,
                    )}
                  >
                    {badge.label}
                  </span>
                </div>
                <div className="mt-3 space-y-1.5 text-sm text-cinema-300">
                  <p className="flex items-center gap-1.5">
                    <MapPin className="w-3.5 h-3.5" /> {booking.cinemaName}
                  </p>
                  <p className="flex items-center gap-1.5">
                    <Monitor className="w-3.5 h-3.5" /> {booking.screenName}
                  </p>
                  <p className="flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5" />{" "}
                    {formatDateTime(booking.startTime)}
                  </p>
                </div>
              </div>
            </div>

            {/* Seats */}
            <div className="p-3 rounded-xl bg-cinema-800/60">
              <p className="text-cinema-400 text-xs mb-2 uppercase tracking-wider">
                Ghế
              </p>
              <div className="flex flex-wrap gap-2">
                {booking.seats.map((s, i) => (
                  <span
                    key={i}
                    className="px-3 py-1.5 rounded-lg bg-brand-500/15
                    border border-brand-500/30 text-brand-400 text-sm font-mono font-bold"
                  >
                    {s.rowLabel}
                    {s.colNumber}
                  </span>
                ))}
              </div>
            </div>
          </div>

          {/* Ticket tear line */}
          <div className="relative h-5 overflow-hidden">
            <div className="absolute inset-0 bg-cinema-800 border-x border-white/6" />
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t-2 border-dashed border-cinema-600" />
            </div>
            <div className="absolute -left-3 top-1/2 -translate-y-1/2 w-5 h-5 rounded-full bg-cinema-900 border border-white/6" />
            <div className="absolute -right-3 top-1/2 -translate-y-1/2 w-5 h-5 rounded-full bg-cinema-900 border border-white/6" />
          </div>

          {/* Bottom part — QR */}
          <div className="card-cinema p-5 rounded-t-none border-t-0 flex flex-col items-center">
            {booking.status === "PAID" && booking.qrCode ? (
              <>
                <div className="p-4 rounded-2xl bg-white mb-3">
                  <QRCodeSVG value={booking.qrCode} size={160} />
                </div>
                <p className="text-cinema-400 text-xs text-center">
                  Xuất trình mã QR này tại quầy để check-in
                </p>
                <p className="text-cinema-600 font-mono text-xs mt-1">
                  {booking.bookingCode}
                </p>
              </>
            ) : (
              <div className="text-center py-4">
                <Ticket className="w-12 h-12 text-cinema-600 mx-auto mb-2" />
                <p className="text-cinema-400 text-sm">QR code chưa sẵn sàng</p>
              </div>
            )}
          </div>
        </motion.div>

        {/* Price */}
        <div className="card-cinema p-4 mt-4 space-y-2">
          {booking.discount > 0 && (
            <div className="flex justify-between text-sm">
              <span className="text-cinema-400">Giảm giá</span>
              <span className="text-green-400">
                - {formatCurrency(booking.discount)}
              </span>
            </div>
          )}
          <div className="flex justify-between font-bold">
            <span className="text-white">Tổng cộng</span>
            <span className="text-brand-400 text-lg">
              {formatCurrency(booking.totalAmount)}
            </span>
          </div>
        </div>

        {/* Cancel */}
        {booking.status === "PENDING" && (
          <button
            onClick={() => cancelMutation.mutate()}
            disabled={cancelMutation.isPending}
            className="w-full mt-4 flex items-center justify-center gap-2 py-3
              rounded-xl border border-brand-500/30 text-brand-400
              hover:bg-brand-500/10 transition-all text-sm disabled:opacity-50"
          >
            <X className="w-4 h-4" /> Hủy vé
          </button>
        )}
      </div>
    </div>
  );
}
