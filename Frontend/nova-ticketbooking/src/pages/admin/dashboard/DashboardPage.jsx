import { motion } from "framer-motion";
import { useQuery } from "@tanstack/react-query";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  Ticket,
  Film,
  Users,
} from "lucide-react";
import { dashboardApi } from "@/api/endpoints";
import {
  formatCurrency,
  formatCompactCurrency,
  formatDate,
  getStatusBadge,
  cn,
} from "@/utils";

const StatCard = ({ title, value, change, icon: Icon, color }) => (
  <motion.div
    initial={{ opacity: 0, y: 16 }}
    animate={{ opacity: 1, y: 0 }}
    className="stat-card"
  >
    <div className="flex items-start justify-between mb-4">
      <div
        className={cn(
          "w-12 h-12 rounded-2xl flex items-center justify-center",
          color,
        )}
      >
        <Icon className="w-6 h-6" />
      </div>
      <div
        className={cn(
          "flex items-center gap-1 text-xs font-medium px-2 py-1 rounded-full",
          change >= 0 ? "bg-green-50 text-green-600" : "bg-red-50 text-red-500",
        )}
      >
        {change >= 0 ? (
          <TrendingUp className="w-3 h-3" />
        ) : (
          <TrendingDown className="w-3 h-3" />
        )}
        {Math.abs(change)}%
      </div>
    </div>
    <p className="text-3xl font-bold text-gray-900 mb-1">{value}</p>
    <p className="text-sm text-gray-500">{title}</p>
  </motion.div>
);

export default function AdminDashboard() {
  const { data: stats, isLoading } = useQuery({
    queryKey: ["admin", "dashboard"],
    queryFn: dashboardApi.getStats,
    refetchInterval: 60_000,
  });

  const STATS = stats
    ? [
        {
          title: "Doanh thu tháng",
          value: formatCompactCurrency(stats.totalRevenue),
          change: stats.revenueChange,
          icon: DollarSign,
          color: "bg-green-100 text-green-600",
        },
        {
          title: "Vé đã bán",
          value: stats.totalBookings.toLocaleString(),
          change: stats.bookingChange,
          icon: Ticket,
          color: "bg-blue-100 text-blue-600",
        },
        {
          title: "Phim đang chiếu",
          value: String(stats.totalMovies),
          change: 0,
          icon: Film,
          color: "bg-purple-100 text-purple-600",
        },
        {
          title: "Người dùng",
          value: stats.totalUsers.toLocaleString(),
          change: 0,
          icon: Users,
          color: "bg-orange-100 text-orange-600",
        },
      ]
    : [];

  return (
    <div className="space-y-6">
      {/* Title */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900 font-display">
          Dashboard
        </h1>
        <p className="text-gray-500 text-sm mt-1">
          Tổng quan hoạt động hệ thống
        </p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {isLoading
          ? Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="skeleton h-36 rounded-2xl" />
            ))
          : STATS.map((s) => <StatCard key={s.title} {...s} />)}
      </div>

      {/* Chart + Top movies */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Revenue chart */}
        <div className="lg:col-span-2 bg-white rounded-2xl p-6 border border-gray-100 shadow-sm">
          <h2 className="font-bold text-gray-900 mb-1">
            Doanh thu 7 ngày gần nhất
          </h2>
          <p className="text-sm text-gray-400 mb-6">
            Biểu đồ doanh thu theo ngày
          </p>
          {isLoading ? (
            <div className="skeleton h-52 rounded-xl" />
          ) : (
            <ResponsiveContainer width="100%" height={210}>
              <AreaChart data={stats?.revenueByDay}>
                <defs>
                  <linearGradient id="rev" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#E50914" stopOpacity={0.15} />
                    <stop offset="95%" stopColor="#E50914" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis
                  dataKey="date"
                  tickFormatter={(d) => formatDate(d, "dd/MM")}
                  tick={{ fontSize: 12, fill: "#9ca3af" }}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis
                  tickFormatter={(v) => formatCompactCurrency(v)}
                  tick={{ fontSize: 12, fill: "#9ca3af" }}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip
                  formatter={(v) => [formatCurrency(v), "Doanh thu"]}
                  labelFormatter={(l) => formatDate(l, "dd/MM/yyyy")}
                  contentStyle={{
                    borderRadius: 12,
                    border: "none",
                    boxShadow: "0 4px 24px rgba(0,0,0,0.1)",
                  }}
                />

                <Area
                  type="monotone"
                  dataKey="revenue"
                  stroke="#E50914"
                  strokeWidth={2}
                  fill="url(#rev)"
                  dot={{ r: 4, fill: "#E50914" }}
                />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Top movies */}
        <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm">
          <h2 className="font-bold text-gray-900 mb-4">Phim ăn khách nhất</h2>
          {isLoading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <div key={i} className="skeleton h-14 rounded-xl" />
              ))}
            </div>
          ) : (
            <div className="space-y-3">
              {stats?.topMovies
                .slice(0, 5)
                .map(({ movie, bookings, revenue }, i) => (
                  <div key={movie.id} className="flex items-center gap-3">
                    <span className="text-xl font-bold text-gray-200 w-6 flex-shrink-0">
                      {i + 1}
                    </span>
                    <img
                      src={movie.posterUrl}
                      alt={movie.title}
                      className="w-10 h-14 object-cover rounded-lg flex-shrink-0"
                    />
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-semibold text-gray-800 line-clamp-1">
                        {movie.title}
                      </p>
                      <p className="text-xs text-gray-400">
                        {bookings.toLocaleString()} vé
                      </p>
                      <p className="text-xs font-medium text-brand-500">
                        {formatCompactCurrency(revenue)}
                      </p>
                    </div>
                  </div>
                ))}
            </div>
          )}
        </div>
      </div>

      {/* Recent bookings */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        <div className="p-6 border-b border-gray-100">
          <h2 className="font-bold text-gray-900">Đặt vé gần đây</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Mã vé</th>
                <th>Phim</th>
                <th>Rạp</th>
                <th>Thời gian</th>
                <th>Số tiền</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {isLoading
                ? Array.from({ length: 5 }).map((_, i) => (
                    <tr key={i}>
                      {Array.from({ length: 6 }).map((_, j) => (
                        <td key={j}>
                          <div className="skeleton h-4 rounded w-20" />
                        </td>
                      ))}
                    </tr>
                  ))
                : stats?.recentBookings.map((b) => {
                    const badge = getStatusBadge(b.status);
                    return (
                      <tr key={b.id}>
                        <td className="font-mono text-xs text-gray-500">
                          {b.bookingCode}
                        </td>
                        <td className="font-medium text-gray-800 max-w-[150px] truncate">
                          {b.movieTitle}
                        </td>
                        <td className="text-gray-600">{b.cinemaName}</td>
                        <td className="text-gray-500 text-xs">
                          {formatDate(b.startTime, "dd/MM HH:mm")}
                        </td>
                        <td className="font-semibold text-gray-800">
                          {formatCurrency(b.totalAmount)}
                        </td>
                        <td>
                          <span
                            className={cn(
                              "badge text-xs",
                              `badge-${badge.color}`,
                            )}
                          >
                            {badge.label}
                          </span>
                        </td>
                      </tr>
                    );
                  })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
