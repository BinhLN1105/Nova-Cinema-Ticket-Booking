import { useQuery } from '@tanstack/react-query'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Ticket, CheckCircle2, Clock, XCircle, CalendarDays, 
  QrCode, Search, Filter, ShoppingBag, Smartphone, Banknote, RefreshCw
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { useState } from 'react'
import { bookingApi } from '@/api/endpoints'
import { formatCurrency, formatDateTime, getStatusBadge, cn } from '@/utils'
import { StatusBadge } from '@/components/common/ui/AdminTable'
import BookingDetailModal from '@/components/staff/BookingDetailModal'
import toast from 'react-hot-toast'

export default function StaffDashboard() {
  const [filters, setFilters] = useState({
    startDate: new Date().toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0],
    status: '',
    paymentMethod: '',
    search: ''
  })
  const [selectedBookingId, setSelectedBookingId] = useState(null)
  const [isPrinting, setIsPrinting] = useState(false)

  const { data: bookingsResp, isLoading, refetch } = useQuery({
    queryKey: ['staff', 'bookings', filters],
    queryFn: () => bookingApi.getAll({ 
      startDate: filters.startDate ? `${filters.startDate}T00:00:00` : undefined,
      endDate: filters.endDate ? `${filters.endDate}T23:59:59` : undefined,
      status: filters.status || undefined,
      paymentMethod: filters.paymentMethod || undefined,
      search: filters.search || undefined,
      size: 50 
    }),
    refetchInterval: 30_000,
  })

  const content = bookingsResp?.content ?? []
  const stats = {
    total:    content.length,
    paid:     content.filter(b => b.status === 'PAID' || b.status === 'CHECKED_IN').length,
    pending:  content.filter(b => b.status === 'PENDING').length,
    cancelled:content.filter(b => b.status === 'CANCELLED' || b.status === 'EXPIRED').length,
  }

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }))
  }

  const handlePrintDirectly = (id) => {
    // Logic sẽ được triển khai sau khi có BookingDetailModal
    setSelectedBookingId(id)
    setTimeout(() => {
        window.print()
    }, 500)
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
        <div className="px-6 py-5 border-b border-gray-100 bg-gray-50/30">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-center gap-2">
              <div className="p-2 bg-blue-500 rounded-lg shadow-sm shadow-blue-200">
                <CalendarDays className="w-5 h-5 text-white" />
              </div>
              <h2 className="font-bold text-gray-900 text-lg">Danh sách đơn hàng</h2>
            </div>
            
            <div className="flex items-center gap-2">
              <button 
                onClick={() => refetch()}
                className="p-2 hover:bg-gray-100 rounded-lg transition-colors text-gray-500"
                title="Làm mới"
              >
                <RefreshCw className={cn("w-4 h-4", isLoading && "animate-spin")} />
              </button>
              <Link to="/staff/pos" className="text-sm px-4 py-2 bg-brand-50 text-brand-600 rounded-lg hover:bg-brand-100 font-bold transition-all flex items-center gap-2">
                 <ShoppingBag className="w-4 h-4" /> Bán tại quầy
              </Link>
            </div>
          </div>

          {/* New Filter Bar */}
          <div className="grid grid-cols-2 md:grid-cols-5 gap-3 mt-6">
            <div className="relative group">
              <CalendarDays className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 group-focus-within:text-blue-500 transition-colors" />
              <input 
                type="date"
                value={filters.startDate}
                onChange={(e) => handleFilterChange('startDate', e.target.value)}
                className="w-full pl-9 pr-3 py-2 bg-white border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all outline-none"
              />
            </div>
            <div className="relative group">
              <CalendarDays className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 group-focus-within:text-blue-500 transition-colors" />
              <input 
                type="date"
                value={filters.endDate}
                onChange={(e) => handleFilterChange('endDate', e.target.value)}
                className="w-full pl-9 pr-3 py-2 bg-white border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all outline-none"
              />
            </div>
            <select
              value={filters.status}
              onChange={(e) => handleFilterChange('status', e.target.value)}
              className="px-3 py-2 bg-white border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all outline-none"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="PAID">Đã thanh toán</option>
              <option value="CHECKED_IN">Đã Check-in</option>
              <option value="PENDING">Chờ thanh toán</option>
              <option value="CANCELLED">Đã hủy</option>
              <option value="EXPIRED">Vừa hết hạn</option>
            </select>
            <select
              value={filters.paymentMethod}
              onChange={(e) => handleFilterChange('paymentMethod', e.target.value)}
              className="px-3 py-2 bg-white border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all outline-none"
            >
              <option value="">Tất cả nguồn</option>
              <option value="CASH">🛒 Bán tại quầy</option>
              <option value="VNPAY">📱 Thanh toán Online</option>
              <option value="WALLET">💳 Ví Nova</option>
            </select>
            <div className="relative col-span-2 md:col-span-1 group">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 group-focus-within:text-blue-500 transition-colors" />
              <input 
                type="text"
                placeholder="Mã đơn / Phim..."
                value={filters.search}
                onChange={(e) => handleFilterChange('search', e.target.value)}
                className="w-full pl-9 pr-3 py-2 bg-white border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all outline-none"
              />
            </div>
          </div>
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
            <AnimatePresence mode="popLayout">
              {content.map(booking => {
                const s = getStatusBadge(booking.status)
                const isCancelled = booking.status === 'CANCELLED' || booking.status === 'EXPIRED'
                const isPOS = booking.paymentMethod === 'CASH'
                const isFandB = booking.movieTitle?.includes('Hóa đơn F&B')
                
                return (
                  <motion.div 
                    layout
                    key={booking.id} 
                    initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                    className={cn(
                      "flex items-center gap-4 px-6 py-4 hover:bg-gray-50/80 transition-all",
                      isCancelled && "bg-gray-50/50 grayscale-[0.6] opacity-60"
                    )}
                  >
                    <div className="relative">
                      {isFandB ? (
                        <div className="w-10 h-14 bg-amber-50 flex items-center justify-center rounded-lg text-amber-500 border border-amber-100">
                          <ShoppingBag className="w-6 h-6 shrink-0" />
                        </div>
                      ) : (
                        <img src={booking.moviePosterUrl || 'https://via.placeholder.com/150'} alt={booking.movieTitle}
                          className="w-10 h-14 object-cover rounded-lg flex-shrink-0 shadow-sm border border-gray-100" />
                      )}
                      
                      {/* Brand Badge */}
                      <div className={cn(
                        "absolute -bottom-1 -right-1 p-1 rounded-md border-2 border-white shadow-sm",
                        isPOS ? "bg-emerald-500" : "bg-sky-500"
                      )}>
                        {isPOS ? <Banknote className="w-2.5 h-2.5 text-white" /> : <Smartphone className="w-2.5 h-2.5 text-white" />}
                      </div>
                    </div>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                         <span className="text-[10px] font-mono font-bold px-1.5 py-0.5 bg-gray-100 text-gray-500 rounded border border-gray-200">
                           {booking.bookingCode}
                         </span>
                         {isPOS ? (
                           <span className="text-[10px] font-bold text-emerald-600 uppercase tracking-tighter">🛒 Tại quầy</span>
                         ) : (
                           <span className="text-[10px] font-bold text-sky-600 uppercase tracking-tighter">📱 App/Web</span>
                         )}
                      </div>
                      <p className={cn(
                        "font-bold text-gray-900 text-sm mt-1 line-clamp-1",
                        isFandB && "text-amber-600 italic"
                      )}>
                        {booking.movieTitle}
                      </p>
                      <div className="flex items-center gap-3 mt-0.5">
                        <p className="text-[11px] text-gray-400 flex items-center gap-1">
                           <Clock className="w-3 h-3" /> {formatDateTime(booking.createdAt)}
                        </p>
                        {!isFandB && (
                          <p className="text-[11px] font-medium text-blue-500">
                            Ghế: {booking.seats || 'N/A'}
                          </p>
                        )}
                      </div>
                    </div>

                    <div className="text-right flex-shrink-0 min-w-24">
                      <StatusBadge label={s.label} color={s.color} />
                      <p className={cn(
                        "text-sm font-black mt-1",
                        isCancelled ? "text-gray-400 line-through" : "text-gray-900"
                      )}>
                        {formatCurrency(booking.totalAmount)}
                      </p>
                    </div>

                    {/* Actions Column */}
                    <div className="flex items-center gap-2 ml-4">
                      <button 
                        onClick={() => setSelectedBookingId(booking.id)}
                        className="p-2 hover:bg-blue-50 text-blue-600 rounded-lg transition-colors shadow-sm bg-white border border-gray-100"
                        title="Xem chi tiết"
                      >
                        <Search className="w-4 h-4" />
                      </button>
                      
                      {(booking.status === 'PAID' || booking.status === 'CHECKED_IN') && (
                        <button 
                          onClick={() => handlePrintDirectly(booking.id)}
                          className="p-2 hover:bg-emerald-50 text-emerald-600 rounded-lg transition-colors shadow-sm bg-white border border-gray-100"
                          title="In vé"
                        >
                          <Ticket className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  </motion.div>
                )
              })}
            </AnimatePresence>
          </div>
        )}
      </div>

      {/* Booking Detail & Print Modal */}
      <BookingDetailModal 
        bookingId={selectedBookingId} 
        onClose={() => setSelectedBookingId(null)} 
      />
    </div>
  )
}
