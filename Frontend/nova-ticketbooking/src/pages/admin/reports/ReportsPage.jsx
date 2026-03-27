import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts'
import { TrendingUp, Calendar, Film, DollarSign, Ticket } from 'lucide-react'
import { dashboardApi } from '@/api/endpoints'
import { AdminCard, PageHeader } from '@/components/common/ui/AdminTable'
import { formatCurrency, formatCompactCurrency, formatDate } from '@/utils'

const PERIOD_OPTS = [
  { value: 'week',  label: '7 ngày' },
  { value: 'month', label: '30 ngày' },
  { value: 'year',  label: '12 tháng' },
]

const PIE_COLORS = ['#E50914', '#3B82F6', '#10B981', '#F59E0B', '#8B5CF6']

export default function ReportsPage() {
  const [period, setPeriod] = useState('month')

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['admin', 'dashboard'],
    queryFn: dashboardApi.getStats,
  })
  const { data: revenue, isLoading: revLoading } = useQuery({
    queryKey: ['admin', 'revenue', period],
    queryFn: () => dashboardApi.getRevenue(period),
  })

  const isLoading = statsLoading || revLoading

  return (
    <div className="space-y-6">
      <PageHeader title="Báo cáo & Thống kê" subtitle="Phân tích dữ liệu kinh doanh chi tiết"
        action={
          <div className="flex items-center gap-1 bg-white border border-gray-200 rounded-xl p-1">
            {PERIOD_OPTS.map(o => (
              <button key={o.value} onClick={() => setPeriod(o.value)}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                  period === o.value ? 'bg-brand-500 text-white' : 'text-gray-600 hover:bg-gray-50'
                }`}>
                {o.label}
              </button>
            ))}
          </div>
        }
      />

      {/* KPI cards */}
      <div className="grid grid-cols-2 xl:grid-cols-4 gap-4">
        {[
          { label: 'Tổng doanh thu', value: formatCompactCurrency(stats?.totalRevenue ?? 0), icon: DollarSign, color: 'text-green-600', bg: 'bg-green-50', change: stats?.revenueChange ?? 0 },
          { label: 'Vé đã bán',      value: (stats?.totalBookings ?? 0).toLocaleString(),    icon: Calendar,   color: 'text-blue-600',  bg: 'bg-blue-50',  change: stats?.bookingChange ?? 0 },
          { label: 'Phim đang chiếu',value: String(stats?.totalMovies ?? 0),                 icon: Film,       color: 'text-purple-600',bg: 'bg-purple-50',change: 0 },
          { label: 'Người dùng',     value: (stats?.totalUsers ?? 0).toLocaleString(),       icon: TrendingUp, color: 'text-orange-600',bg: 'bg-orange-50',change: 0 },
        ].map(({ label, value, icon: Icon, color, bg, change }) => (
          <div key={label} className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
            <div className="flex items-center justify-between mb-4">
              <div className={`w-10 h-10 rounded-xl ${bg} flex items-center justify-center`}>
                <Icon className={`w-5 h-5 ${color}`} />
              </div>
              {change !== 0 && (
                <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                  change >= 0 ? 'bg-green-50 text-green-600' : 'bg-red-50 text-red-500'
                }`}>
                  {change >= 0 ? '+' : ''}{change}%
                </span>
              )}
            </div>
            {isLoading ? (
              <div className="space-y-2">
                <div className="h-7 bg-gray-100 rounded animate-pulse w-2/3" />
                <div className="h-4 bg-gray-100 rounded animate-pulse w-1/2" />
              </div>
            ) : (
              <>
                <p className="text-2xl font-bold text-gray-900">{value}</p>
                <p className="text-sm text-gray-500 mt-0.5">{label}</p>
              </>
            )}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Revenue chart */}
        <AdminCard className="lg:col-span-2 p-6">
          <h2 className="font-bold text-gray-900 mb-1 font-display">Doanh thu theo thời gian</h2>
          <p className="text-sm text-gray-400 mb-5">Biểu đồ doanh thu theo ngày/tháng</p>
          {revLoading ? (
            <div className="h-64 bg-gray-50 rounded-xl animate-pulse" />
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <AreaChart data={revenue ?? []}>
                <defs>
                  <linearGradient id="revGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor="#E50914" stopOpacity={0.12} />
                    <stop offset="95%" stopColor="#E50914" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="date" tickFormatter={d => formatDate(d, period === 'year' ? 'MM/yy' : 'dd/MM')}
                  tick={{ fontSize: 11, fill: '#9ca3af' }} axisLine={false} tickLine={false} />
                <YAxis tickFormatter={v => formatCompactCurrency(v)}
                  tick={{ fontSize: 11, fill: '#9ca3af' }} axisLine={false} tickLine={false} />
                <Tooltip formatter={(v) => [formatCurrency(v), 'Doanh thu']}
                  contentStyle={{ borderRadius: 12, border: 'none', boxShadow: '0 4px 24px rgba(0,0,0,0.1)', fontSize: 13 }} />
                <Area type="monotone" dataKey="revenue" stroke="#E50914" strokeWidth={2.5}
                  fill="url(#revGrad)" dot={false} activeDot={{ r: 5, fill: '#E50914' }} />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </AdminCard>

        {/* Top movies pie */}
        <AdminCard className="p-6">
          <h2 className="font-bold text-gray-900 mb-1 font-display">Phim ăn khách</h2>
          <p className="text-sm text-gray-400 mb-5">Tỷ lệ doanh thu theo phim</p>
          {statsLoading ? (
            <div className="h-64 bg-gray-50 rounded-xl animate-pulse" />
          ) : (
            <>
              <ResponsiveContainer width="100%" height={180}>
                <PieChart>
                  <Pie data={stats?.topMovies?.slice(0, 5).map(m => ({
                      name: m.movie.title, value: m.revenue
                    })) ?? []}
                    cx="50%" cy="50%" innerRadius={50} outerRadius={80}
                    paddingAngle={3} dataKey="value">
                    {stats?.topMovies?.slice(0, 5).map((_, i) => (
                      <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(v) => formatCurrency(v)}
                    contentStyle={{ borderRadius: 10, border: 'none', fontSize: 12 }} />
                </PieChart>
              </ResponsiveContainer>
              <div className="space-y-2 mt-2">
                {stats?.topMovies?.slice(0, 5).map((m, i) => (
                  <div key={m.movie.id} className="flex items-center gap-2 text-xs">
                    <div className="w-2.5 h-2.5 rounded-full flex-shrink-0"
                      style={{ background: PIE_COLORS[i % PIE_COLORS.length] }} />
                    <span className="text-gray-600 flex-1 line-clamp-1">{m.movie.title}</span>
                    <span className="font-semibold text-gray-800">{formatCompactCurrency(m.revenue)}</span>
                  </div>
                ))}
              </div>
            </>
          )}
        </AdminCard>
      </div>

      {/* Bookings bar chart */}
      <AdminCard className="p-6 shadow-glow-red/5">
        <div className="flex items-center gap-2 mb-1">
          <Ticket className="w-4 h-4 text-brand-500" />
          <h2 className="font-bold text-gray-900 font-display">Số vé bán theo thời gian</h2>
        </div>
        <p className="text-sm text-gray-400 mb-5">Thống kê lượng vé theo từng ngày/tháng trong kỳ</p>
        {revLoading ? (
          <div className="h-48 bg-gray-50 rounded-xl animate-pulse" />
        ) : (
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={revenue ?? []} barSize={24}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" vertical={false} />
              <XAxis dataKey="date" tickFormatter={d => formatDate(d, period === 'year' ? 'MM/yy' : 'dd/MM')}
                tick={{ fontSize: 11, fill: '#9ca3af' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11, fill: '#9ca3af' }} axisLine={false} tickLine={false} />
              <Tooltip 
                formatter={(v) => [v, 'Số vé']}
                contentStyle={{ borderRadius: 10, border: 'none', fontSize: 12, boxShadow: '0 4px 20px rgba(0,0,0,0.08)' }} 
              />
              <Bar dataKey="bookingCount" fill="#E50914" fillOpacity={0.85} radius={[6, 6, 0, 0]} name="Số vé" />
            </BarChart>
          </ResponsiveContainer>
        )}
      </AdminCard>
    </div>
  )
}
