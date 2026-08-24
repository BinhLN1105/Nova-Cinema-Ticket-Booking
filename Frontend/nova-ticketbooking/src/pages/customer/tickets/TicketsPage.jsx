import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useInfiniteQuery } from '@tanstack/react-query'
import { Ticket, Calendar, MapPin, ChevronRight, Loader2, Clock } from 'lucide-react'
import { bookingApi } from '@/api/endpoints'
import { formatDateTime, formatCurrency, getStatusBadge, cn } from '@/utils'

function TicketItemCountdown({ expiresAt, onExpired }) {
  const [timeLeft, setTimeLeft] = useState('')
  const [isExpired, setIsExpired] = useState(false)
  const [isLowTime, setIsLowTime] = useState(false)

  useEffect(() => {
    if (!expiresAt) return

    const targetTime = new Date(expiresAt).getTime()

    const update = () => {
      const diff = targetTime - Date.now()
      if (diff <= 0) {
        setTimeLeft('')
        setIsExpired(true)
        if (onExpired) onExpired()
        return false
      }
      const mins = Math.floor(diff / (1000 * 60))
      const secs = Math.floor((diff % (1000 * 60)) / 1000)
      setIsLowTime(mins < 2)
      setTimeLeft(`${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`)
      return true
    }

    if (!update()) return

    const interval = setInterval(() => {
      if (!update()) clearInterval(interval)
    }, 1000)

    return () => clearInterval(interval)
  }, [expiresAt, onExpired])

  if (isExpired || !timeLeft) return null

  return (
    <span className={cn(
      "inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-mono font-bold tracking-tight border shadow-sm transition-all",
      isLowTime 
        ? "bg-red-500/15 border-red-500/40 text-red-400 animate-pulse" 
        : "bg-amber-500/15 border-amber-500/35 text-amber-300"
    )}>
      <Clock className="w-3 h-3" />
      {timeLeft}
    </span>
  )
}

export default function TicketsPage() {
  const { 
    data, 
    isLoading, 
    hasNextPage, 
    fetchNextPage, 
    isFetchingNextPage,
    refetch 
  } = useInfiniteQuery({
    queryKey: ['bookings', 'my'],
    queryFn: ({ pageParam = 0 }) => bookingApi.getMyAll(pageParam, 10),
    getNextPageParam: (lastPage, allPages) => {
      if (!lastPage.last) {
        return allPages.length;
      }
      return undefined;
    }
  });

  const bookings = data?.pages.flatMap(page => page.content) || [];

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-cinema-900 text-slate-900 dark:text-white pt-24 pb-16 transition-colors duration-300">
      <div className="max-w-2xl mx-auto px-4 sm:px-6">
        <div className="mb-8">
          <h1 className="font-display text-3xl font-bold text-slate-900 dark:text-white mb-1">Vé của tôi</h1>
          <p className="text-slate-600 dark:text-cinema-300">Lịch sử đặt vé và vé đang có hiệu lực</p>
        </div>

        {isLoading ? (
          <div className="space-y-3">
            {[1,2,3].map(i => <div key={i} className="skeleton h-28 rounded-2xl" />)}
          </div>
        ) : bookings.length === 0 ? (
          <div className="text-center py-24">
            <Ticket className="w-16 h-16 mx-auto text-slate-400 dark:text-cinema-600 mb-4" />
            <p className="text-slate-600 dark:text-cinema-300 text-lg mb-6">Bạn chưa có vé nào</p>
            <Link to="/movies" className="btn-primary px-6 py-3">Đặt vé ngay</Link>
          </div>
        ) : (
          <>
            <div className="space-y-3">
              {bookings.map((booking, i) => {
                const badge = getStatusBadge(booking.status)
                const seatCount = booking.seats 
                  ? booking.seats.split(',').filter(Boolean).length 
                  : (booking.seatCount || 0)

                return (
                  <motion.div key={booking.id}
                    initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: i * 0.06 }}>
                    <Link to={`/tickets/${booking.id}`}
                      className="flex items-center gap-4 card-cinema p-4 group hover:bg-slate-100 dark:hover:bg-cinema-800 transition-colors">
                      <img src={booking.moviePosterUrl} alt={booking.movieTitle}
                        className="w-14 h-20 object-cover rounded-xl flex-shrink-0" />
                      <div className="flex-1 min-w-0">
                        <h3 className="font-display font-bold text-slate-900 dark:text-white text-base line-clamp-1 mb-1">
                          {booking.movieTitle}
                        </h3>
                        <p className="text-slate-500 dark:text-cinema-400 text-xs flex items-center gap-1 mb-1">
                          <MapPin className="w-3 h-3 flex-shrink-0" /> {booking.cinemaName}
                        </p>
                        <p className="text-slate-500 dark:text-cinema-400 text-xs flex items-center gap-1">
                          <Calendar className="w-3 h-3 flex-shrink-0" /> {formatDateTime(booking.startTime)}
                        </p>
                        <div className="flex items-center gap-2 mt-2">
                          <span className={cn('badge text-xs',
                            `badge-${badge.color}`)}>
                            {badge.label}
                          </span>
                          {seatCount > 0 && (
                            <span className="text-slate-500 dark:text-cinema-400 text-xs">
                              {seatCount} ghế ({booking.seats})
                            </span>
                          )}
                        </div>
                      </div>
                      <div className="text-right flex-shrink-0 flex flex-col items-end justify-between self-stretch py-0.5">
                        {booking.status === 'PENDING' && booking.expiresAt && (
                          <TicketItemCountdown 
                            expiresAt={booking.expiresAt} 
                            onExpired={() => refetch()} 
                          />
                        )}
                        <div className="mt-auto">
                          <p className="text-slate-900 dark:text-white font-bold text-sm">{formatCurrency(booking.totalAmount)}</p>
                          <ChevronRight className="w-4 h-4 text-slate-400 dark:text-cinema-500 mt-1 ml-auto
                            group-hover:text-brand-500 dark:group-hover:text-brand-400 transition-colors" />
                        </div>
                      </div>
                    </Link>
                  </motion.div>
                )
              })}
            </div>
            
            {hasNextPage && (
              <div className="mt-8 text-center">
                <button 
                  onClick={() => fetchNextPage()} 
                  disabled={isFetchingNextPage}
                  className="btn-outline px-6 py-2.5 rounded-full inline-flex items-center gap-2 text-slate-700 dark:text-cinema-300 hover:text-slate-900 dark:hover:text-white"
                >
                  {isFetchingNextPage && <Loader2 className="w-4 h-4 animate-spin" />}
                  {isFetchingNextPage ? 'Đang tải...' : 'Xem thêm'}
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
