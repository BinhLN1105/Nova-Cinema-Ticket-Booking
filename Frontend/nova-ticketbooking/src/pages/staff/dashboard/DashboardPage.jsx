import { useQuery } from '@tanstack/react-query'
import { motion } from 'framer-motion'
import { Ticket, CheckCircle2, Clock, XCircle, CalendarDays, QrCode } from 'lucide-react'
import { Link } from 'react-router-dom'
import { bookingApi } from '@/api/endpoints'
import { formatCurrency, formatDateTime, getStatusBadge } from '@/utils'
import { StatusBadge } from '@/components/common/ui/AdminTable'

export default function StaffDashboard() {
  const today = new Date().toISOString().split('T')[0]

  const { data: bookings, isLoading } = useQuery({
    queryKey: ['staff', 'bookings', today],
    queryFn: () => bookingApi.getAll({ date: today, size: 20 }),
    refetchInterval: 30_000,
  })

  const content = bookings?.content ?? []
  const stats = {
    total:    content.length,
    paid:     content.filter(b => b.status === 'PAID').length,
    pending:  content.filter(b => b.status === 'PENDING').length,
    cancelled:content.filter(b => b.status === 'CANCELLED').length,
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 font-display">Dashboard nhân viên</h1>
          <p className="text-gray-500 text-sm mt-0.5">
            {new Date().toLocaleDateString('vi-VN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
          </p>
        </div>
        <Link to="/staff/checkin"
          className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-blue-500 hover:bg-blue-600
            text-white font-semibold text-sm transition-all shadow-sm">
          <QrCode className="w-5 h-5" /> Mở Check-in QR
        </Link>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: 'Tổng vé hôm nay', value: stats.total,    icon: Ticket,        bg: 'bg-blue-50',   text: 'text-blue-600' },
          { label: 'Đã thanh toán',   value: stats.paid,     icon: CheckCircle2,  bg: 'bg-green-50',  text: 'text-green-600' },
          { label: 'Chờ thanh toán',  value: stats.pending,  icon: Clock,         bg: 'bg-amber-50',  text: 'text-amber-600' },
          { label: 'Đã hủy',          value: stats.cancelled,icon: XCircle,       bg: 'bg-red-50',    text: 'text-red-600' },
        ].map(({ label, value, icon: Icon, bg, text }, i) => (
          <motion.div key={label}
            initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.07 }}
            className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
            <div className={`w-11 h-11 rounded-xl ${bg} flex items-center justify-center mb-3`}>
              <Icon className={`w-5 h-5 ${text}`} />
            </div>
            {isLoading ? (
              <div className="space-y-1.5">
                <div className="h-7 bg-gray-100 rounded animate-pulse w-1/2" />
                <div className="h-4 bg-gray-100 rounded animate-pulse w-3/4" />
              </div>
            ) : (
              <>
                <p className="text-3xl font-bold text-gray-900">{value}</p>
                <p className="text-sm text-gray-500 mt-0.5">{label}</p>
              </>
            )}
          </motion.div>
        ))}
      </div>

      {/* Today's bookings */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <div className="flex items-center gap-2">
            <CalendarDays className="w-5 h-5 text-gray-500" />
            <h2 className="font-bold text-gray-900">Đặt vé hôm nay</h2>
          </div>
          <Link to="/staff/bookings" className="text-sm text-blue-500 hover:text-blue-600 font-medium transition-colors">
            Xem tất cả →
          </Link>
        </div>

        {isLoading ? (
          <div className="divide-y divide-gray-50">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="flex items-center gap-4 px-6 py-4">
                <div className="w-10 h-14 bg-gray-100 rounded-lg animate-pulse" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 bg-gray-100 rounded animate-pulse w-1/2" />
                  <div className="h-3 bg-gray-100 rounded animate-pulse w-1/3" />
                </div>
              </div>
            ))}
          </div>
        ) : content.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-gray-400">
            <Ticket className="w-12 h-12 mb-3 opacity-40" />
            <p className="text-sm">Chưa có vé nào hôm nay</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-50">
            {content.slice(0, 8).map(booking => {
              const s = getStatusBadge(booking.status)
              return (
                <div key={booking.id} className="flex items-center gap-4 px-6 py-3.5 hover:bg-gray-50/50 transition-colors">
                  <img src={booking.moviePosterUrl} alt={booking.movieTitle}
                    className="w-10 h-14 object-cover rounded-lg flex-shrink-0" />
                  <div className="flex-1 min-w-0">
                    <p className="font-semibold text-gray-900 text-sm line-clamp-1">{booking.movieTitle}</p>
                    <p className="text-xs text-gray-400 mt-0.5">{booking.cinemaName}</p>
                    <p className="text-xs text-gray-400">{formatDateTime(booking.startTime)}</p>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <StatusBadge label={s.label} color={s.color} />
                    <p className="text-xs text-gray-500 mt-1">{formatCurrency(booking.totalAmount)}</p>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
