import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts'
import { TrendingUp, Calendar, Film, DollarSign, Ticket } from 'lucide-react'
import { AdminCard, PageHeader } from '@/components/common/ui/AdminTable'
import { formatCurrency, formatCompactCurrency, formatDate, cn } from '@/utils'
import { motion, AnimatePresence } from 'framer-motion'
import { cinemaApi, dashboardApi } from '@/api/endpoints'
import { useNavigate } from 'react-router-dom'

const PIE_COLORS = ['#E50914', '#3B82F6', '#10B981', '#F59E0B', '#8B5CF6']

export default function ReportsPage() {
  const getInitialDates = (days) => {
    const vnOffset = 7 * 60 * 60 * 1000;
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

  const navigate = useNavigate();
  const [selectedCinemaId, setSelectedCinemaId] = useState("");

  const handlePeriodChange = (p) => {
    setActivePeriod(p);
    let days = 7;
    if (p === "1c") days = 1;
    else if (p === "7d") days = 7;
    else if (p === "30d") days = 30;
    
    if (p !== "custom") {
      const { start, end } = getInitialDates(days);
      setStartDate(start);
      setEndDate(end);
    }
  };

  const { data: cinemas } = useQuery({
    queryKey: ['admin', 'cinemas'],
    queryFn: () => cinemaApi.getAll(),
  })

  const { data: stats, isLoading } = useQuery({
    queryKey: ['admin', 'dashboard', startDate, endDate, selectedCinemaId],
    queryFn: () => dashboardApi.getStats({ 
      startDate, 
      endDate, 
      cinemaId: selectedCinemaId || undefined 
    }),
    enabled: !!startDate && !!endDate,
    refetchOnWindowFocus: false
  })

  const revenue = stats?.revenueByDay || []

  return (
    <div className="space-y-8 pb-10">
      {/* 🌟 TIER 1: GLOBAL FILTERS */}
      <div className="bg-white rounded-3xl border border-gray-100 shadow-sm p-4 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="bg-brand-50 p-2.5 rounded-2xl">
            <Calendar className="w-5 h-5 text-brand-600" />
          </div>
          <div>
            <h1 className="text-lg font-bold text-gray-900 leading-tight">Bộ lọc báo cáo</h1>
            <p className="text-xs text-gray-500">Toàn hệ thống & Theo cụm rạp</p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {/* Cinema Selector */}
          <select 
            value={selectedCinemaId}
            onChange={(e) => setSelectedCinemaId(e.target.value)}
            className="select select-sm bg-gray-50 border-none rounded-xl h-10 px-4 text-xs font-medium focus:ring-2 focus:ring-brand-500/20"
          >
            <option value="">Tất cả rạp</option>
            {cinemas?.map(c => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>

          {/* Date Presets */}
          <div className="flex items-center bg-gray-50 p-1 rounded-xl h-10">
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
                  "px-4 py-1.5 h-8 text-[11px] font-bold rounded-lg transition-all",
                  activePeriod === p.id
                    ? "bg-white text-gray-900 shadow-sm"
                    : "text-gray-500 hover:text-gray-700",
                )}
              >
                {p.label}
              </button>
            ))}
          </div>

          <AnimatePresence>
            {activePeriod === "custom" && (
              <motion.div 
                initial={{ opacity: 0, width: 0 }}
                animate={{ opacity: 1, width: 'auto' }}
                exit={{ opacity: 0, width: 0 }}
                className="flex items-center gap-2 overflow-hidden"
              >
                <input
                  type="datetime-local"
                  className="input input-sm border-gray-100 bg-gray-50 text-[11px] h-10 rounded-xl"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
                <span className="text-gray-300 text-xs">—</span>
                <input
                  type="datetime-local"
                  className="input input-sm border-gray-100 bg-gray-50 text-[11px] h-10 rounded-xl"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                />
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* 🌟 TIER 2: KPI SUMMARY CARDS */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5">
        {[
          { label: 'Doanh thu thuần', value: formatCurrency(stats?.netTotalRevenue ?? 0), icon: DollarSign, color: 'brand', change: stats?.revenueChange ?? 0, sub: 'Sau giảm giá' },
          { label: 'Số vé bán ra', value: (stats?.totalBookings ?? 0).toLocaleString(), icon: Ticket, color: 'blue', change: stats?.bookingChange ?? 0, sub: 'Paid & Checked' },
          { label: 'Doanh thu Combo', value: formatCurrency(stats?.grossConcessionRevenue?.total ?? 0), icon: Film, color: 'purple', change: 0, sub: 'Bắp nước & Đồ ăn' },
          { label: 'Tiền Khuyến mãi', value: formatCurrency(stats?.totalDiscountGiven ?? 0), icon: TrendingUp, color: 'orange', change: 0, sub: 'Promo + Voucher' },
        ].map(({ label, value, icon: Icon, color, change, sub }) => (
          <div key={label} className="bg-white rounded-3xl border border-gray-50 shadow-md p-6 relative overflow-hidden group hover:shadow-xl transition-all duration-300">
            <div className={`absolute top-0 right-0 w-24 h-24 -mr-8 -mt-8 rounded-full bg-${color}-50/50 group-hover:scale-110 transition-transform duration-500`} />
            
            <div className="flex items-center justify-between mb-4 relative z-10">
              <div className={`w-12 h-12 rounded-2xl bg-${color === 'brand' ? 'brand' : color}-50 flex items-center justify-center`}>
                <Icon className={`w-6 h-6 text-${color === 'brand' ? 'brand' : color}-600`} />
              </div>
              <div className="text-right">
                {change !== 0 && (
                  <span className={`text-[10px] font-bold px-2.5 py-1 rounded-full ${
                    change >= 0 ? 'bg-green-50 text-green-600' : 'bg-red-50 text-red-500'
                  }`}>
                    {change >= 0 ? '↑' : '↓'} {Math.abs(change)}%
                  </span>
                )}
                <p className="text-[10px] text-gray-400 mt-1 uppercase tracking-wider font-bold">{sub}</p>
              </div>
            </div>

            {isLoading ? (
              <div className="space-y-2 relative z-10">
                <div className="h-8 bg-gray-100 rounded-lg animate-pulse w-3/4" />
                <div className="h-4 bg-gray-50 rounded-lg animate-pulse w-1/2" />
              </div>
            ) : (
              <div className="relative z-10">
                <p className="text-2xl font-black text-gray-900 tracking-tight">{value}</p>
                <p className="text-xs font-bold text-gray-400 mt-0.5">{label}</p>
              </div>
            )}
          </div>
        ))}
      </div>

      {/* 🌟 TIER 3: TREND CHARTS */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Doanh thu Xu hướng */}
        <div className="bg-white rounded-3xl border border-gray-50 shadow-md p-8">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h2 className="text-xl font-black text-gray-900 font-display italic uppercase tracking-tight">Xu hướng doanh thu</h2>
              <p className="text-xs font-bold text-gray-400 mt-1">Sự tương quan giữa Vé & Combo</p>
            </div>
            <div className="flex gap-4">
              <div className="flex items-center gap-1.5">
                <div className="w-2 h-2 rounded-full bg-brand-500" />
                <span className="text-[10px] font-bold text-gray-500 uppercase">Tiền vé</span>
              </div>
              <div className="flex items-center gap-1.5">
                <div className="w-2 h-2 rounded-full bg-blue-500" />
                <span className="text-[10px] font-bold text-gray-500 uppercase">Bắp nước</span>
              </div>
            </div>
          </div>
          
          {isLoading ? (
            <div className="h-72 bg-gray-50 rounded-2xl animate-pulse" />
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <AreaChart data={revenue}>
                <defs>
                  <linearGradient id="ticketGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#E50914" stopOpacity={0.1}/>
                    <stop offset="95%" stopColor="#E50914" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="conGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.1}/>
                    <stop offset="95%" stopColor="#3B82F6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F3F4F6" />
                <XAxis 
                  dataKey="date" 
                  tickFormatter={d => formatDate(d, activePeriod === 'year' ? 'MM/yy' : 'dd/MM')}
                  tick={{ fontSize: 10, fontWeight: 700, fill: '#9CA3AF' }}
                  axisLine={false}
                  tickLine={false}
                  dy={10}
                />
                <YAxis 
                  tickFormatter={v => formatCompactCurrency(v)}
                  tick={{ fontSize: 10, fontWeight: 700, fill: '#9CA3AF' }}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip 
                  contentStyle={{ borderRadius: 20, border: 'none', boxShadow: '0 10px 40px rgba(0,0,0,0.1)', padding: '16px' }}
                  itemStyle={{ fontSize: 12, fontWeight: 800 }}
                  labelStyle={{ marginBottom: 8, fontWeight: 900, color: '#111827' }}
                  labelFormatter={d => formatDate(d, 'PPPP')}
                />
                <Area 
                  type="monotone" 
                  dataKey="ticketRevenue" 
                  name="Tiền vé"
                  stroke="#E50914" 
                  strokeWidth={4} 
                  fill="url(#ticketGrad)"
                  dot={{ r: 0 }}
                  activeDot={{ r: 6, fill: '#E50914', strokeWidth: 0 }}
                />
                <Area 
                  type="monotone" 
                  dataKey="concessionRevenue" 
                  name="Bắp nước"
                  stroke="#3B82F6" 
                  strokeWidth={4} 
                  fill="url(#conGrad)"
                  dot={{ r: 0 }}
                  activeDot={{ r: 6, fill: '#3B82F6', strokeWidth: 0 }}
                />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Lượng khách Xu hướng */}
        <div className="bg-white rounded-3xl border border-gray-50 shadow-md p-8">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h2 className="text-xl font-black text-gray-900 font-display italic uppercase tracking-tight">Mật độ khách hàng</h2>
              <p className="text-xs font-bold text-gray-400 mt-1">Số lượng vé bán theo ngày</p>
            </div>
            <div className="w-12 h-12 bg-brand-50 rounded-2xl flex items-center justify-center">
              <TrendingUp className="w-6 h-6 text-brand-500" />
            </div>
          </div>

          {isLoading ? (
            <div className="h-72 bg-gray-50 rounded-2xl animate-pulse" />
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={revenue}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F3F4F6" />
                <XAxis 
                  dataKey="date" 
                  tickFormatter={d => formatDate(d, 'dd/MM')}
                  tick={{ fontSize: 10, fontWeight: 700, fill: '#9CA3AF' }}
                  axisLine={false}
                  tickLine={false}
                  dy={10}
                />
                <YAxis 
                  tick={{ fontSize: 10, fontWeight: 700, fill: '#9CA3AF' }}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip 
                  cursor={{ fill: '#F9FAFB' }}
                  contentStyle={{ borderRadius: 20, border: 'none', boxShadow: '0 10px 40px rgba(0,0,0,0.1)' }}
                />
                <Bar 
                  dataKey="bookingCount" 
                  name="Số vé"
                  fill="#E50914" 
                  radius={[8, 8, 0, 0]} 
                  barSize={selectedCinemaId ? 40 : 20}
                />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* 🌟 TIER 4: BREAKDOWN/PIE CHARTS */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Doanh thu vé theo loại ghế */}
        <div className="bg-white rounded-3xl border border-gray-50 shadow-md p-6">
          <h2 className="text-sm font-black text-gray-900 uppercase tracking-widest mb-6 border-l-4 border-brand-500 pl-4">Cơ cấu Ghế ngồi</h2>
          {isLoading ? (
             <div className="h-60 bg-gray-50 rounded-2xl animate-pulse" />
          ) : (
            <div className="h-64 relative">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={Object.entries(stats?.grossTicketRevenue?.breakdown || {}).map(([name, value]) => ({ name, value }))}
                    innerRadius={60}
                    outerRadius={85}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {PIE_COLORS.map((color, i) => <Cell key={i} fill={color} strokeWidth={0} />)}
                  </Pie>
                  <Tooltip formatter={(v) => formatCurrency(v)} />
                </PieChart>
              </ResponsiveContainer>
              <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-center pointer-events-none">
                <p className="text-[10px] font-bold text-gray-400 uppercase">Phòng vé</p>
                <p className="text-sm font-black text-gray-900 italic">Ticket</p>
              </div>
            </div>
          )}
        </div>

        {/* Doanh thu Combo */}
        <div className="bg-white rounded-3xl border border-gray-50 shadow-md p-6">
          <h2 className="text-sm font-black text-gray-900 uppercase tracking-widest mb-6 border-l-4 border-blue-500 pl-4">Cơ cấu Bắp nước</h2>
          {isLoading ? (
             <div className="h-60 bg-gray-50 rounded-2xl animate-pulse" />
          ) : (
            <div className="h-64 relative">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={Object.entries(stats?.grossConcessionRevenue?.breakdown || {}).map(([name, value]) => ({ name, value }))}
                    innerRadius={60}
                    outerRadius={85}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {PIE_COLORS.map((color, i) => <Cell key={i} fill={color} strokeWidth={0} />)}
                  </Pie>
                  <Tooltip formatter={(v) => formatCurrency(v)} />
                </PieChart>
              </ResponsiveContainer>
              <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-center pointer-events-none">
                <p className="text-[10px] font-bold text-gray-400 uppercase">Combo</p>
                <p className="text-sm font-black text-gray-900 italic">F&B</p>
              </div>
            </div>
          )}
        </div>

        {/* Box Office vs F&B */}
        <div className="bg-white rounded-3xl border border-gray-50 shadow-md p-6">
          <h2 className="text-sm font-black text-gray-900 uppercase tracking-widest mb-6 border-l-4 border-emerald-500 pl-4">Nguồn doanh thu</h2>
          {isLoading ? (
             <div className="h-60 bg-gray-50 rounded-2xl animate-pulse" />
          ) : (
            <div className="h-64 relative">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={[
                      { name: 'Phòng vé', value: Number(stats?.grossTicketRevenue?.total || 0) },
                      { name: 'Bắp nước', value: Number(stats?.grossConcessionRevenue?.total || 0) }
                    ]}
                    innerRadius={60}
                    outerRadius={85}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    <Cell fill="#E50914" strokeWidth={0} />
                    <Cell fill="#3B82F6" strokeWidth={0} />
                  </Pie>
                  <Tooltip formatter={(v) => formatCurrency(v)} />
                </PieChart>
              </ResponsiveContainer>
              <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-center pointer-events-none">
                 <p className="text-[10px] font-bold text-gray-400 uppercase">Tổng thu</p>
                 <p className="text-sm font-black text-gray-900 italic">Revenue</p>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 🌟 TIER 5: LEADERBOARDS & TABLES */}
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">
        {/* Top Phim Ăn Khách */}
        <div className="xl:col-span-2 bg-white rounded-[40px] border border-gray-100 shadow-xl overflow-hidden">
          <div className="p-8 border-b border-gray-50 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-amber-50 rounded-2xl flex items-center justify-center">
                <Film className="w-6 h-6 text-amber-600" />
              </div>
              <div>
                <h3 className="text-xl font-black text-gray-900 font-display italic uppercase tracking-tight">🏆 Top Phim Ăn Khách</h3>
                <p className="text-xs font-bold text-gray-400 uppercase tracking-widest">Bảng vàng doanh thu</p>
              </div>
            </div>
          </div>
          
          <div className="p-4">
            <div className="overflow-x-auto">
              <table className="w-full border-separate border-spacing-y-3">
                <thead>
                  <tr className="text-left text-[10px] font-black text-gray-400 uppercase tracking-widest">
                    <th className="px-6 pb-2 italic">Phim</th>
                    <th className="px-6 pb-2 italic">Số vé đã bán</th>
                    <th className="px-6 pb-2 italic text-right">Tổng doanh thu</th>
                  </tr>
                </thead>
                <tbody>
                  {stats?.topMovies?.map((m, i) => (
                    <tr key={m.movie.id} className="group hover:bg-gray-50/50 transition-colors">
                      <td className="px-4 py-3 bg-gray-50/30 rounded-l-3xl first:rounded-l-3xl border-y border-l border-gray-50 shadow-sm first:border-l last:rounded-r-3xl last:border-r">
                        <div className="flex items-center gap-4">
                          <div className="w-8 h-8 rounded-full bg-white flex items-center justify-center font-black italic text-brand-600 border border-brand-100 shadow-sm">
                            #{i+1}
                          </div>
                          <img src={m.movie.posterUrl} className="w-12 h-16 object-cover rounded-xl shadow-lg border-2 border-white group-hover:scale-105 transition-transform" />
                          <span className="font-bold text-gray-900 group-hover:text-brand-600 transition-colors">{m.movie.title}</span>
                        </div>
                      </td>
                      <td className="px-6 py-3 bg-gray-50/30 border-y border-gray-50 shadow-sm">
                        <div className="flex items-center gap-2">
                           <Ticket className="w-4 h-4 text-gray-400" />
                           <span className="text-sm font-black text-gray-900">{m.bookings.toLocaleString()} vé</span>
                        </div>
                      </td>
                      <td className="px-6 py-3 bg-gray-50/30 rounded-r-3xl border-y border-r border-gray-50 shadow-sm text-right">
                        <span className="text-base font-black text-gray-900 italic">{formatCurrency(m.revenue)}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Giao dịch gần đây */}
        <div className="bg-gray-50/50 rounded-[40px] border border-gray-100 shadow-sm p-8">
           <div className="flex items-center gap-4 mb-8">
              <div className="w-10 h-10 bg-white rounded-xl shadow-sm flex items-center justify-center">
                 <div className="w-2 h-2 rounded-full bg-brand-600 animate-ping" />
              </div>
              <div>
                <h4 className="text-lg font-black text-gray-900 font-display italic uppercase tracking-tight">🎯 Giao dịch mới</h4>
                <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Thời gian thực</p>
              </div>
           </div>

           <div className="space-y-4">
              {stats?.recentBookings?.map(b => (
                <div key={b.id} className="bg-white p-4 rounded-3xl shadow-sm border border-gray-50 hover:shadow-md transition-shadow group">
                   <div className="flex items-center justify-between mb-2">
                      <span className="text-[10px] font-black text-brand-600 uppercase tracking-wider">{b.bookingCode}</span>
                      <span className="text-[10px] font-bold text-gray-400">{formatDate(b.startTime, 'HH:mm dd/MM')}</span>
                   </div>
                   <h5 className="text-sm font-bold text-gray-900 line-clamp-1 group-hover:text-brand-600 transition-colors uppercase tracking-tight">{b.movieTitle}</h5>
                   <div className="flex items-center justify-between mt-3">
                      <span className="text-xs font-bold text-gray-400 italic line-clamp-1 max-w-[120px]">{b.status}</span>
                      <span className="text-sm font-black text-gray-900">{formatCurrency(b.totalAmount)}</span>
                   </div>
                </div>
              ))}
           </div>
           
           <button 
             onClick={() => navigate('/admin/bookings')}
             className="w-full mt-8 py-3 rounded-2xl bg-white border border-gray-100 text-xs font-black text-gray-500 uppercase tracking-widest hover:bg-brand-600 hover:text-white transition-all shadow-sm"
           >
             Xem tất cả giao dịch
           </button>
        </div>
      </div>
    </div>
  )
}
