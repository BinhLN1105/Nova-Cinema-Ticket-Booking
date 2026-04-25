import { motion } from "framer-motion";
import { useQuery } from "@tanstack/react-query";
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
  PieChart,
  Pie,
} from "recharts";
import { useState } from "react";
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  Ticket,
  Film,
  Users,
  Clock,
  Building2,
  BarChart3,
} from "lucide-react";
import { dashboardApi, cinemaApi } from "@/api/endpoints";
import { useNavigate } from "react-router-dom";
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
        {Math.round(Math.abs(change || 0))}%
      </div>
    </div>
    <p className="text-3xl font-bold text-gray-900 mb-1">{value}</p>
    <p className="text-sm text-gray-500">{title}</p>
  </motion.div>
);

export default function AdminDashboard() {
  const getInitialDates = (days) => {
    const vnOffset = 7 * 60 * 60 * 1000; // GMT+7
    const end = new Date(new Date().getTime() + vnOffset);
    end.setUTCHours(23, 59, 59, 999);
    
    const start = new Date(new Date().getTime() + vnOffset);
    start.setUTCDate(start.getUTCDate() - (days - 1));
    start.setUTCHours(0, 0, 0, 0);

    return {
      start: start.toISOString().slice(0, 16),
      end: end.toISOString().slice(0, 16)
    };
  };

  const initial = getInitialDates(7);
  const [startDate, setStartDate] = useState(initial.start);
  const [endDate, setEndDate] = useState(initial.end);
  const [activePeriod, setActivePeriod] = useState("7d");
  const [cinemaId, setCinemaId] = useState("");
  const navigate = useNavigate();

  const { data: cinemas } = useQuery({
    queryKey: ["admin", "cinemas"],
    queryFn: () => cinemaApi.getAll(),
  });

  const handlePeriodChange = (period) => {
    setActivePeriod(period);
    let days = 7;
    if (period === "1c") days = 1;
    else if (period === "7d") days = 7;
    else if (period === "30d") days = 30;
    
    if (period !== "custom") {
      const { start, end } = getInitialDates(days);
      setStartDate(start);
      setEndDate(end);
    }
  };

  const { data: stats, isLoading } = useQuery({
    queryKey: ["admin", "dashboard", startDate, endDate, cinemaId],
    queryFn: () => dashboardApi.getStats({ startDate, endDate, cinemaId: cinemaId || undefined }),
    refetchInterval: 60_000,
  });

  const { data: analytics, isLoading: analyticsLoading } = useQuery({
    queryKey: ["admin", "analytics", startDate, endDate, cinemaId],
    queryFn: () => dashboardApi.getAnalytics({ startDate, endDate, cinemaId }),
    enabled: !!startDate && !!endDate,
    refetchInterval: 120_000,
  });

  const PEAK_COLORS = ["#374151", "#4b5563", "#6b7280", "#9ca3af", "#E50914", "#dc2626", "#ef4444", "#f87171"];
  const BAR_COLORS = ["#E50914", "#f97316", "#eab308", "#22c55e", "#3b82f6", "#8b5cf6", "#ec4899"];

  const STATS = stats
    ? [
        {
          title: "Doanh thu",
          value: formatCompactCurrency(stats.netTotalRevenue || 0),
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

      {/* Date Filters */}
      <div className="flex flex-wrap items-center gap-4 bg-white p-4 rounded-2xl border border-gray-100 shadow-sm">
        <div className="flex items-center bg-gray-100 p-1 rounded-xl mr-2">
          {[
            { id: "1c", label: "Hôm nay" },
            { id: "7d", label: "7 ngày" },
            { id: "30d", label: "30 ngày" },
            { id: "custom", label: "Tùy chỉnh" },
          ].map((p) => (
            <button
              key={p.id}
              onClick={() => handlePeriodChange(p.id)}
              className={cn(
                "px-4 py-1.5 text-xs font-medium rounded-lg transition-all",
                activePeriod === p.id
                  ? "bg-white text-gray-900 shadow-sm"
                  : "text-gray-500 hover:text-gray-700",
              )}
            >
              {p.label}
            </button>
          ))}
        </div>
        
        {/* Cinema Selector */}
        <select
          value={cinemaId}
          onChange={(e) => setCinemaId(e.target.value)}
          className="select select-sm bg-gray-100 border-none rounded-xl h-9 px-4 text-xs font-medium focus:ring-2 focus:ring-brand-500/20"
        >
          <option value="">Tất cả rạp</option>
          {cinemas?.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>

        {activePeriod === "custom" && (
          <motion.div 
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            className="flex items-center gap-4"
          >
            <div className="flex items-center gap-2">
              <label className="text-sm font-medium text-gray-700">Từ:</label>
              <input
                type="datetime-local"
                className="input text-sm py-1.5 h-auto w-auto"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>
            <div className="flex items-center gap-2">
              <label className="text-sm font-medium text-gray-700">Đến:</label>
              <input
                type="datetime-local"
                className="input text-sm py-1.5 h-auto w-auto"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>
          </motion.div>
        )}
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
            Doanh thu {activePeriod === "1c" ? "hôm nay" : activePeriod === "7d" ? "7 ngày qua" : activePeriod === "30d" ? "30 ngày qua" : "tùy chỉnh"}
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
                          <span className={cn(b.movieTitle?.includes("🍿") && "text-primary italic")}>
                            {b.movieTitle || "Hóa đơn F&B"}
                          </span>
                        </td>
                        <td className="text-gray-600">{b.cinemaName}</td>
                        <td className="text-gray-500 text-xs">
                          {b.startTime 
                            ? formatDate(b.startTime, "dd/MM HH:mm")
                            : "Giao dịch F&B"}
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

      {/* ── Revenue Breakdowns ─────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Ticket Breakdown */}
        <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm">
          <h2 className="font-bold text-gray-900 mb-1">Cơ cấu doanh thu vé</h2>
          <p className="text-sm text-gray-400 mb-6">Tỉ lệ theo loại ghế</p>
          {isLoading ? (
            <div className="skeleton h-52 rounded-xl" />
          ) : (
            <div className="flex items-center justify-around h-52">
              <ResponsiveContainer width="50%" height="100%">
                <PieChart>
                  <Pie
                    data={Object.entries(stats?.grossTicketRevenue?.breakdown || {}).map(
                      ([name, value]) => ({ name, value }),
                    )}
                    innerRadius={60}
                    outerRadius={80}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {Object.entries(stats?.grossTicketRevenue?.breakdown || {}).map(
                      (_, i) => (
                        <Cell
                          key={i}
                          fill={BAR_COLORS[i % BAR_COLORS.length]}
                        />
                      ),
                    )}
                  </Pie>
                  <Tooltip formatter={(v) => formatCurrency(v)} />
                </PieChart>
              </ResponsiveContainer>
              <div className="space-y-2">
                {Object.entries(stats?.grossTicketRevenue?.breakdown || {}).map(
                  ([name, value], i) => (
                    <div key={name} className="flex items-center gap-2">
                      <div
                        className="w-3 h-3 rounded-full"
                        style={{ backgroundColor: BAR_COLORS[i % BAR_COLORS.length] }}
                      />
                      <span className="text-sm font-medium text-gray-700">
                        {name}:
                      </span>
                      <span className="text-sm text-gray-500">
                        {formatCompactCurrency(value)}
                      </span>
                    </div>
                  ),
                )}
                <div className="pt-2 mt-2 border-t border-gray-100">
                  <p className="text-sm font-bold text-gray-900">
                    Tổng: {formatCurrency(stats?.grossTicketRevenue?.total || 0)}
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Concession Breakdown */}
        <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm">
          <h2 className="font-bold text-gray-900 mb-1">Cơ cấu doanh thu Combo</h2>
          <p className="text-sm text-gray-400 mb-6">Top Combo bán chạy nhất</p>
          {isLoading ? (
            <div className="skeleton h-52 rounded-xl" />
          ) : (
            <div className="flex items-center justify-around h-52">
              <ResponsiveContainer width="50%" height="100%">
                <PieChart>
                  <Pie
                    data={Object.entries(
                      stats?.grossConcessionRevenue?.breakdown || {},
                    ).map(([name, value]) => ({ name, value }))}
                    innerRadius={60}
                    outerRadius={80}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {Object.entries(
                      stats?.grossConcessionRevenue?.breakdown || {},
                    ).map((_, i) => (
                      <Cell
                        key={i}
                        fill={PEAK_COLORS[i % PEAK_COLORS.length]}
                      />
                    ))}
                  </Pie>
                  <Tooltip formatter={(v) => formatCurrency(v)} />
                </PieChart>
              </ResponsiveContainer>
              <div className="space-y-1 max-h-48 overflow-y-auto pr-2 custom-scrollbar">
                {Object.entries(
                  stats?.grossConcessionRevenue?.breakdown || {},
                ).map(([name, value], i) => (
                  <div key={name} className="flex items-center gap-2">
                    <div
                      className="w-3 h-3 rounded-full"
                      style={{
                        backgroundColor: PEAK_COLORS[i % PEAK_COLORS.length],
                      }}
                    />
                    <span className="text-xs font-medium text-gray-700 truncate w-24">
                      {name}:
                    </span>
                    <span className="text-xs text-gray-500">
                      {formatCompactCurrency(value)}
                    </span>
                  </div>
                ))}
                <div className="pt-2 mt-2 border-t border-gray-100">
                  <p className="text-sm font-bold text-gray-900">
                    Tổng: {formatCurrency(stats?.grossConcessionRevenue?.total || 0)}
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ── Advanced Analytics ─────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Peak Hours */}
        <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm">
          <div className="flex items-center gap-2 mb-1">
            <Clock className="w-4 h-4 text-brand-500" />
            <h2 className="font-bold text-gray-900">Giờ vàng (Golden Hours)</h2>
          </div>
          <p className="text-sm text-gray-400 mb-5">Khung giờ có nhiều lượt đặt vé nhất</p>
          {analyticsLoading ? (
            <div className="skeleton h-52 rounded-xl" />
          ) : (
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={analytics?.peakHours?.filter(h => h.bookingCount > 0)}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="hour" tickFormatter={h => `${h}h`} tick={{ fontSize: 11, fill: "#9ca3af" }} />
                <YAxis tick={{ fontSize: 11, fill: "#9ca3af" }} axisLine={false} tickLine={false} />
                <Tooltip labelFormatter={h => `${h}:00`} formatter={v => [v, "Lượt đặt"]} 
                  contentStyle={{ borderRadius: 12, border: "none", boxShadow: "0 4px 24px rgba(0,0,0,0.1)" }} />
                <Bar dataKey="bookingCount" radius={[6, 6, 0, 0]}>
                  {analytics?.peakHours?.filter(h => h.bookingCount > 0).map((_, i) => (
                    <Cell key={i} fill={BAR_COLORS[i % BAR_COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Revenue by Cinema */}
        <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm">
          <div className="flex items-center gap-2 mb-1">
            <Building2 className="w-4 h-4 text-brand-500" />
            <h2 className="font-bold text-gray-900">Doanh thu theo rạp</h2>
          </div>
          <p className="text-sm text-gray-400 mb-5">So sánh doanh thu giữa các rạp chiếu</p>
          {analyticsLoading ? (
            <div className="skeleton h-52 rounded-xl" />
          ) : (
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={analytics?.revenueByCinema} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis type="number" tickFormatter={v => formatCompactCurrency(v)} tick={{ fontSize: 11, fill: "#9ca3af" }} />
                <YAxis type="category" dataKey="cinemaName" width={120} tick={{ fontSize: 11, fill: "#374151" }} />
                <Tooltip formatter={v => [formatCurrency(v), "Doanh thu"]} 
                  contentStyle={{ borderRadius: 12, border: "none", boxShadow: "0 4px 24px rgba(0,0,0,0.1)" }} />
                <Bar dataKey="revenue" radius={[0, 6, 6, 0]}>
                  {analytics?.revenueByCinema?.map((_, i) => (
                    <Cell key={i} fill={BAR_COLORS[i % BAR_COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* Occupancy */}
      <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm">
        <div className="flex items-center gap-2 mb-1">
          <BarChart3 className="w-4 h-4 text-brand-500" />
          <h2 className="font-bold text-gray-900">Tỷ lệ lấp đầy phòng chiếu</h2>
        </div>
        <p className="text-sm text-gray-400 mb-5">Occupancy rate theo từng rạp</p>
        {analyticsLoading ? (
          <div className="skeleton h-32 rounded-xl" />
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {analytics?.occupancyByCinema?.map((item, i) => (
              <div key={i} className="p-4 rounded-xl bg-gray-50 border border-gray-100">
                <p className="font-semibold text-gray-800 text-sm mb-2">{item.cinemaName}</p>
                <div className="w-full bg-gray-200 rounded-full h-3 mb-2">
                  <div
                    className="h-3 rounded-full transition-all"
                    style={{
                      width: `${Math.min(Math.round(item.occupancyRate || 0), 100)}%`,
                      backgroundColor: item.occupancyRate > 80 ? '#E50914' : item.occupancyRate > 50 ? '#f97316' : '#22c55e'
                    }}
                  />
                </div>
                <div className="flex justify-between text-xs text-gray-500">
                  <span>{item.bookedSeats}/{item.totalSeats} ghế</span>
                  <span className="font-bold" style={{
                    color: item.occupancyRate > 80 ? '#E50914' : item.occupancyRate > 50 ? '#f97316' : '#22c55e'
                  }}>{Math.round(item.occupancyRate || 0)}%</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
