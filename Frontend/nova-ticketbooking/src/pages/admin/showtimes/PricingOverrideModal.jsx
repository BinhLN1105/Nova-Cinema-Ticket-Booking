import { useState, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useQuery, useMutation } from '@tanstack/react-query'
import { X, Loader2, DollarSign } from 'lucide-react'
import { showtimeApi } from '@/api/endpoints'
import { formatCurrency, cn } from '@/utils'
import toast from 'react-hot-toast'

export default function PricingOverrideModal({ showtimeId, onClose, onSuccess }) {
  const [selectedSeats, setSelectedSeats] = useState([])
  const [newPrice, setNewPrice] = useState('')

  const { data: seatMap, isLoading } = useQuery({
    queryKey: ['admin-seatmap', showtimeId],
    queryFn: () => showtimeApi.getSeatMap(showtimeId),
    enabled: !!showtimeId
  })

  const mutation = useMutation({
    mutationFn: () => showtimeApi.overrideSeatPrices(showtimeId, selectedSeats, Number(newPrice)),
    onSuccess: () => {
      toast.success('Cập nhật giá ghế thành công')
      onSuccess()
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Có lỗi xảy ra')
  })

  // Group seats by row
  const rows = useMemo(() => {
    if (!seatMap?.seats) return []
    const map = new Map()
    seatMap.seats.forEach(seat => {
      if (!map.has(seat.rowLabel)) map.set(seat.rowLabel, [])
      map.get(seat.rowLabel).push(seat)
    })
    return Array.from(map.entries()).map(([rowLabel, seats]) => ({
      rowLabel,
      seats: seats.sort((a, b) => a.colNumber - b.colNumber)
    }))
  }, [seatMap])

  const toggleSeat = (seatId) => {
    setSelectedSeats(prev => 
      prev.includes(seatId) ? prev.filter(id => id !== seatId) : [...prev, seatId]
    )
  }

  const handleSelectAllRow = (rowSeats) => {
    const allIds = rowSeats.map(s => s.showtimeSeatId)
    const isAllSelected = allIds.every(id => selectedSeats.includes(id))
    if (isAllSelected) {
      setSelectedSeats(prev => prev.filter(id => !allIds.includes(id)))
    } else {
      setSelectedSeats(prev => [...new Set([...prev, ...allIds])])
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!selectedSeats.length) return toast.error('Vui lòng chọn ít nhất 1 ghế')
    if (!newPrice) return toast.error('Vui lòng nhập giá mới')
    mutation.mutate()
  }

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
          className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />
        
        <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }}
          className="card-cinema w-full max-w-4xl max-h-[90vh] flex flex-col relative z-10 p-6">
          <button onClick={onClose} className="absolute right-4 top-4 p-2 text-cinema-400 hover:text-white rounded-xl hover:bg-white/10 flex-shrink-0 transition-colors">
            <X className="w-5 h-5" />
          </button>

          <h3 className="text-xl font-bold text-white mb-2 shrink-0">
            Cập nhật giá ghế thủ công
          </h3>
          <p className="text-sm text-cinema-400 mb-6 shrink-0">Chọn các ghế cần đổi giá và nhập giá mới.</p>

          {isLoading ? (
            <div className="flex-1 flex justify-center items-center"><Loader2 className="w-8 h-8 animate-spin text-brand-500" /></div>
          ) : (
            <div className="flex gap-6 flex-1 min-h-0 min-w-0">
              {/* Seat Map */}
              <div className="flex-1 overflow-auto pr-4 border-r border-white/10 shrink-0 min-h-0">
                <div className="min-w-max mx-auto space-y-2 pb-6">
                  {/* Screen Curve */}
                  <div className="w-full max-w-md mx-auto h-8 border-t-4 border-brand-500 rounded-[50%] opacity-50 mb-8" />
                  
                  {rows.map(({ rowLabel, seats }) => (
                    <div key={rowLabel} className="flex items-center justify-center gap-2">
                      <div className="w-6 text-center text-sm font-bold text-cinema-400 cursor-pointer hover:text-white"
                        onClick={() => handleSelectAllRow(seats)} title="Chọn cả hàng">
                        {rowLabel}
                      </div>
                      <div className="flex gap-2">
                        {seats.map(seat => {
                          const isSelected = selectedSeats.includes(seat.showtimeSeatId)
                          const isLocked = seat.status === 'LOCKED'
                          const isBooked = seat.status === 'BOOKED'
                          const disabled = isLocked || isBooked
                          
                          return (
                            <button key={seat.showtimeSeatId}
                              disabled={disabled}
                              onClick={() => toggleSeat(seat.showtimeSeatId)}
                              className={cn(
                                "relative w-7 h-7 sm:w-8 sm:h-8 rounded-t-lg border transition-all text-[10px] sm:text-xs font-medium flex items-center justify-center",
                                isSelected ? "bg-brand-500 border-brand-400 text-white shadow-glow-red" :
                                isBooked ? "bg-cinema-600 border-cinema-500 text-white/50 cursor-not-allowed" :
                                isLocked ? "bg-yellow-500/50 border-yellow-500/50 text-white cursor-not-allowed" :
                                seat.seatType === 'VIP' ? "bg-yellow-500/10 border-yellow-500/30 text-yellow-400 hover:bg-yellow-500/20" :
                                seat.seatType === 'COUPLE' ? "w-16 sm:w-[72px] bg-pink-500/10 border-pink-500/30 text-pink-400 hover:bg-pink-500/20" :
                                "bg-white/5 border-white/10 text-cinema-100 hover:bg-white/10"
                              )}
                              title={`${rowLabel}${seat.colNumber} - ${formatCurrency(seat.price)}`}
                            >
                              {seat.colNumber}
                              {!disabled && !isSelected && (
                                <span className="absolute -bottom-5 w-[150%] left-1/2 -translate-x-1/2 text-[9px] text-cinema-500 opacity-0 group-hover:opacity-100 truncate pointer-events-none">
                                  {seat.price / 1000}k
                                </span>
                              )}
                            </button>
                          )
                        })}
                      </div>
                      <div className="w-6 text-center text-sm font-bold text-cinema-400" />
                    </div>
                  ))}
                  
                  <div className="flex items-center justify-center gap-6 mt-8 p-4 bg-cinema-800 rounded-xl">
                    <div className="flex items-center gap-2"><div className="w-4 h-4 rounded bg-brand-500" /><span className="text-xs text-cinema-300">Đang chọn ({selectedSeats.length})</span></div>
                    <div className="flex items-center gap-2"><div className="w-4 h-4 rounded bg-white/5 border-white/10" /><span className="text-xs text-cinema-300">Thường</span></div>
                    <div className="flex items-center gap-2"><div className="w-4 h-4 rounded bg-yellow-500/10 border-yellow-500/30" /><span className="text-xs text-cinema-300">VIP</span></div>
                    <div className="flex items-center gap-2"><div className="w-8 h-4 rounded bg-pink-500/10 border-pink-500/30" /><span className="text-xs text-cinema-300">Couple</span></div>
                    <div className="flex items-center gap-2"><div className="w-4 h-4 rounded bg-cinema-600 border-cinema-500" /><span className="text-xs text-cinema-300">Đã đặt/Khóa</span></div>
                  </div>
                </div>
              </div>

              {/* Form Sidebar */}
              <div className="w-64 flex flex-col shrink-0">
                <form onSubmit={handleSubmit} className="flex flex-col h-full gap-4">
                  <div>
                    <label className="block text-sm font-medium text-cinema-300 mb-1.5">Giá vé mới (VNĐ)</label>
                    <div className="relative">
                      <DollarSign className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-cinema-400" />
                      <input type="number" value={newPrice} onChange={e => setNewPrice(e.target.value)}
                        className="input-cinema w-full pl-9" placeholder="VD: 150000" min="0" step="1000" />
                    </div>
                    {newPrice && <p className="text-xs text-brand-400 mt-2 font-medium">Định mức: {formatCurrency(newPrice)}</p>}
                  </div>

                  <div className="flex-1" />

                  <div className="space-y-3 pt-6 border-t border-white/5">
                    <button type="button" onClick={() => setSelectedSeats([])}
                      className="w-full px-5 py-2.5 rounded-xl font-medium text-cinema-300 border border-white/10 hover:text-white hover:bg-white/5 transition-colors">
                      Bỏ chọn tất cả
                    </button>
                    <button type="submit" disabled={mutation.isPending || !selectedSeats.length || !newPrice}
                      className="w-full btn bg-brand-500 hover:bg-brand-600 text-white px-6 py-2.5 rounded-xl disabled:opacity-50 flex items-center justify-center gap-2">
                      {mutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                      Áp dụng giá mới
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
