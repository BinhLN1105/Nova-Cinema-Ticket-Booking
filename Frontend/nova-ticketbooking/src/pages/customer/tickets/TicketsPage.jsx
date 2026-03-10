import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { useQuery } from "@tanstack/react-query";
import { Ticket, Calendar, MapPin, ChevronRight } from "lucide-react";
import { bookingApi } from "@/api/endpoints";
import { formatDateTime, formatCurrency, getStatusBadge, cn } from "@/utils";

export default function TicketsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["bookings", "my"],
    queryFn: () => bookingApi.getMyAll(),
  });

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-16">
      <div className="max-w-2xl mx-auto px-4 sm:px-6">
        <div className="mb-8">
          <h1 className="font-display text-3xl font-bold text-white mb-1">
            Vé của tôi
          </h1>
          <p className="text-cinema-300">
            Lịch sử đặt vé và vé đang có hiệu lực
          </p>
        </div>

        {isLoading ? (
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="skeleton h-28 rounded-2xl" />
            ))}
          </div>
        ) : data?.content?.length === 0 ? (
          <div className="text-center py-24">
            <Ticket className="w-16 h-16 mx-auto text-cinema-600 mb-4" />
            <p className="text-cinema-300 text-lg mb-6">Bạn chưa có vé nào</p>
            <Link to="/movies" className="btn-primary px-6 py-3">
              Đặt vé ngay
            </Link>
          </div>
        ) : (
          <div className="space-y-3">
            {data?.content?.map((booking, i) => {
              const badge = getStatusBadge(booking.status);
              return (
                <motion.div
                  key={booking.id}
                  initial={{ opacity: 0, y: 12 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.06 }}
                >
                  <Link
                    to={`/tickets/${booking.id}`}
                    className="flex items-center gap-4 card-cinema p-4 group"
                  >
                    <img
                      src={booking.moviePosterUrl}
                      alt={booking.movieTitle}
                      className="w-14 h-20 object-cover rounded-xl flex-shrink-0"
                    />
                    <div className="flex-1 min-w-0">
                      <h3 className="font-display font-bold text-white text-base line-clamp-1 mb-1">
                        {booking.movieTitle}
                      </h3>
                      <p className="text-cinema-400 text-xs flex items-center gap-1 mb-1">
                        <MapPin className="w-3 h-3" /> {booking.cinemaName}
                      </p>
                      <p className="text-cinema-400 text-xs flex items-center gap-1">
                        <Calendar className="w-3 h-3" />{" "}
                        {formatDateTime(booking.startTime)}
                      </p>
                      <div className="flex items-center gap-2 mt-2">
                        <span
                          className={cn(
                            "badge text-xs",
                            `badge-${badge.color}`,
                          )}
                        >
                          {badge.label}
                        </span>
                        <span className="text-cinema-400 text-xs">
                          {booking.seatCount} ghế
                        </span>
                      </div>
                    </div>
                    <div className="text-right flex-shrink-0">
                      <p className="text-white font-bold text-sm">
                        {formatCurrency(booking.totalAmount)}
                      </p>
                      <ChevronRight
                        className="w-4 h-4 text-cinema-500 mt-2 ml-auto
                        group-hover:text-brand-400 transition-colors"
                      />
                    </div>
                  </Link>
                </motion.div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
