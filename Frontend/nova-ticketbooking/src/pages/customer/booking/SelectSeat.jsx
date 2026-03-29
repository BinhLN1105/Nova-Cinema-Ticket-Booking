import { useParams, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, ArrowRight, Monitor } from 'lucide-react'
import { useState, useEffect } from 'react'
import { showtimeApi } from '@/api/endpoints'
import { useBookingStore } from '@/stores/bookingStore'
import { formatCurrency, cn } from '@/utils'
import { PageLoader } from '@/components/common/feedback/PageLoader'
import BookingTimer from '@/components/common/ui/BookingTimer'

const SEAT_TYPE_COLORS = {
  STANDARD: 'border-green-500/50 bg-green-500/10 hover:bg-green-500/30',
  VIP:      'border-purple-500/50 bg-purple-500/10 hover:bg-purple-500/30',
  COUPLE:   'border-gold-400/50 bg-gold-400/10 hover:bg-gold-400/30 w-14',
}

const SEAT_TYPE_SELECTED = {
  STANDARD: 'bg-brand-500 border-brand-400 shadow-glow-red',
  VIP:      'bg-purple-500 border-purple-400',
  COUPLE:   'bg-gold-400 border-gold-300',
}

function SeatButton({ seat, isSelected, onToggle }) {
  const isBooked = seat.status === 'BOOKED' || seat.status === 'LOCKED'

  return (
    <button
      onClick={!isBooked ? onToggle : undefined}
      title={`${seat.rowLabel}${seat.colNumber} — ${formatCurrency(seat.price)}`}
      className={cn(
        'h-8 w-8 rounded-md border text-xs font-bold transition-all duration-150',
        'flex items-center justify-center',
        isBooked
          ? 'bg-cinema-700 border-cinema-600 text-cinema-600 cursor-not-allowed opacity-40'
          : isSelected
            ? SEAT_TYPE_SELECTED[seat.seatType]
            : SEAT_TYPE_COLORS[seat.seatType],
        seat.seatType === 'COUPLE' && 'w-14',
        !isBooked && !isSelected && 'cursor-pointer'
      )}
    >
      {!isBooked && (
        <span className="text-[9px]">
          {seat.rowLabel}{seat.colNumber}
        </span>
      )}
    </button>
  )
}

export default function SelectSeatPage() {
  const { showtimeId } = useParams()
  const navigate = useNavigate()
  const { selectedSeats, toggleSeat, selectedShowtime, total, expiryTime, setExpiryTime } = useBookingStore()

  const { data: seatMap, isLoading } = useQuery({
    queryKey: ['seatmap', showtimeId],
    queryFn: () => showtimeApi.getSeatMap(showtimeId),
    enabled: !!showtimeId,
  })

  useEffect(() => {
    if (seatMap?.seatHoldMins && !expiryTime) {
      setExpiryTime(Date.now() + seatMap.seatHoldMins * 60 * 1000)
    }
  }, [seatMap, expiryTime, setExpiryTime])

  if (isLoading || !seatMap) return <PageLoader />

  // Group seats by row
  const rows = seatMap.seats.reduce((acc, seat) => {
    if (!acc[seat.rowLabel]) acc[seat.rowLabel] = []
    acc[seat.rowLabel].push(seat)
    return acc
  }, {})

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-16">
      <div className="max-w-5xl mx-auto px-4 sm:px-6">

        {/* Header */}
        <div className="flex items-center gap-4 mb-8">
          <button onClick={() => navigate(-1)}
            className="p-2.5 rounded-xl glass border border-white/8 text-cinema-200
              hover:text-white transition-all">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="font-display text-2xl font-bold text-white">Chọn ghế ngồi</h1>
            {selectedShowtime && (
              <p className="text-cinema-300 text-sm">{selectedShowtime.cinemaName} • {selectedShowtime.screenName}</p>
            )}
          </div>
        </div>

        {/* Timer */}
        <div className="mb-8">
          <BookingTimer />
        </div>

        {/* Screen indicator */}
        <div className="relative mb-10">
          <div className="h-1 rounded-full bg-gradient-to-r from-transparent via-brand-500/60 to-transparent mx-8 mb-2" />
          <div className="flex items-center justify-center gap-2 text-cinema-400 text-sm">
            <Monitor className="w-4 h-4" /> Màn hình chiếu
          </div>
        </div>

        {/* Seat map */}
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}
          className="overflow-x-auto pb-6 scrollbar-cinema">
          <div className="flex flex-col items-center min-w-max px-12">
            <div className="space-y-3">
              {Object.entries(rows).map(([rowLabel, seats]) => (
                <div key={rowLabel} className="flex items-center gap-4">
                  <span className="w-6 text-cinema-500 text-sm font-bold text-right shrink-0">{rowLabel}</span>
                  <div 
                    className="grid gap-2"
                    style={{ 
                      gridTemplateColumns: `repeat(${seatMap.totalCols}, 2.25rem)`,
                    }}
                  >
                    {seats.map(seat => (
                      <div 
                        key={seat.showtimeSeatId}
                        style={{ 
                          gridColumnStart: seat.colNumber,
                          gridColumnEnd: seat.seatType === 'COUPLE' ? `span 2` : 'auto'
                        }}
                      >
                        <SeatButton 
                          seat={seat}
                          isSelected={selectedSeats.some(s => s.showtimeSeatId === seat.showtimeSeatId)}
                          onToggle={() => toggleSeat(seat)}
                        />
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </motion.div>

        {/* Legend */}
        <div className="flex flex-wrap gap-6 mt-8 justify-center text-xs text-cinema-300">
          {[
            { color: 'bg-green-500/20 border border-green-500/40',   label: 'Ghế thường' },
            { color: 'bg-purple-500/20 border border-purple-500/40', label: 'Ghế VIP' },
            { color: 'bg-gold-400/20 border border-gold-400/40',     label: 'Ghế đôi' },
            { color: 'bg-brand-500 border border-brand-400',         label: 'Đang chọn' },
            { color: 'bg-cinema-700 border border-cinema-600 opacity-40', label: 'Đã đặt' },
          ].map(({ color, label }) => (
            <div key={label} className="flex items-center gap-2">
              <div className={cn('w-6 h-5 rounded', color)} />
              <span>{label}</span>
            </div>
          ))}
        </div>

        {/* Bottom CTA */}
        {selectedSeats.length > 0 && (
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
            className="fixed bottom-0 left-0 right-0 p-4 glass-dark border-t border-white/8">
            <div className="max-w-5xl mx-auto flex items-center justify-between gap-4">
              <div>
                <p className="text-cinema-300 text-sm">
                  {selectedSeats.length} ghế đã chọn:
                  <span className="text-white font-medium ml-1">
                    {selectedSeats.map(s => `${s.rowLabel}${s.colNumber}`).join(', ')}
                  </span>
                </p>
                <p className="text-white font-bold text-lg">{formatCurrency(total)}</p>
              </div>
              <button
                onClick={() => {
                  window.scrollTo(0, 0)
                  navigate('/booking/combo')
                }}
                className="btn-primary px-8 py-3">
                Tiếp theo <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </motion.div>
        )}
      </div>
    </div>
  )
}
