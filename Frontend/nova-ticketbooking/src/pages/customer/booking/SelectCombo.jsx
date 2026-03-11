import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, ArrowRight, Plus, Minus, Popcorn } from 'lucide-react'
import { showtimeApi } from '@/api/endpoints'
import { useBookingStore } from '@/stores/bookingStore'
import { formatCurrency, cn } from '@/utils'
import { useEffect } from 'react'

export default function SelectComboPage() {
  const navigate = useNavigate()
  const store = useBookingStore()
  const { selectedCombos, setComboQty, total } = store

  useEffect(() => {
    window.scrollTo(0, 0)
    if (!store.selectedShowtime || store.selectedSeats.length === 0) {
      navigate('/')
    }
  }, [])

  const { data: combos = [], isLoading } = useQuery({
    queryKey: ['combos'],
    queryFn: () => showtimeApi.getCombos(),
    select: (res) => res || []
  })

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-32">
      <div className="max-w-4xl mx-auto px-4 sm:px-6">

        {/* Header */}
        <div className="flex items-center gap-4 mb-8">
          <button onClick={() => navigate(-1)}
            className="p-2.5 rounded-xl glass border border-white/8 text-cinema-200
              hover:text-white transition-all">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="font-display text-2xl font-bold text-white">Chọn Bắp nước</h1>
            <p className="text-cinema-300 text-sm">Thêm hương vị cho buổi xem phim của bạn</p>
          </div>
        </div>

        {/* Combo List */}
        {isLoading ? (
          <div className="space-y-4">
            {[1, 2, 3].map(i => <div key={i} className="skeleton h-32 rounded-2xl" />)}
          </div>
        ) : combos.length === 0 ? (
          <div className="text-center py-20 bg-white/5 rounded-3xl border border-white/10">
            <Popcorn className="w-16 h-16 mx-auto mb-4 opacity-20 text-white" />
            <p className="text-lg font-medium text-gray-300">Hiện không có combo nào</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {combos.map((combo) => {
              const qty = selectedCombos[combo.id] || 0
              return (
                <motion.div key={combo.id}
                  initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }}
                  className={cn(
                    "flex flex-col sm:flex-row gap-4 p-4 rounded-2xl border transition-all duration-300 bg-white/5",
                    qty > 0 ? "border-brand-500 shadow-glow-red" : "border-white/10"
                  )}>
                  {/* Image */}
                  <div className="w-full sm:w-28 h-28 flex-shrink-0 bg-cinema-800 rounded-xl overflow-hidden">
                    <img src={combo.imageUrl || 'https://placehold.co/200x200/png?text=Combo'} 
                      alt={combo.name} className="w-full h-full object-cover" />
                  </div>
                  
                  {/* Info */}
                  <div className="flex-1 flex flex-col justify-between">
                    <div>
                      <h3 className="font-display font-bold text-white text-lg mb-1">{combo.name}</h3>
                      <p className="text-cinema-300 text-xs mb-3 line-clamp-2">{combo.description}</p>
                    </div>
                    
                    <div className="flex items-center justify-between mt-auto">
                      <span className="font-bold text-brand-400">
                        {formatCurrency(combo.price)}
                      </span>
                      
                      {/* Controls */}
                      <div className="flex items-center gap-3">
                        <button
                          disabled={qty === 0}
                          onClick={() => setComboQty(combo.id, qty - 1)}
                          className={cn(
                            "w-8 h-8 rounded-lg flex items-center justify-center transition-colors",
                            qty === 0 
                              ? "bg-white/5 text-cinema-500 cursor-not-allowed" 
                              : "bg-cinema-700 text-white hover:bg-brand-500 hover:text-white"
                          )}
                        >
                          <Minus className="w-4 h-4" />
                        </button>
                        <span className="w-4 text-center font-bold text-white">{qty}</span>
                        <button
                          disabled={qty >= 10}
                          onClick={() => setComboQty(combo.id, qty + 1)}
                          className={cn(
                            "w-8 h-8 rounded-lg flex items-center justify-center transition-colors",
                            qty >= 10 
                              ? "bg-white/5 text-cinema-500 cursor-not-allowed" 
                              : "bg-cinema-700 text-white hover:bg-brand-500 hover:text-white"
                          )}
                        >
                          <Plus className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  </div>
                </motion.div>
              )
            })}
          </div>
        )}

        {/* Bottom CTA */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
          className="fixed bottom-0 left-0 right-0 p-4 glass-dark border-t border-white/8 z-50">
          <div className="max-w-4xl mx-auto flex items-center justify-between gap-4">
            <div>
              <p className="text-cinema-300 text-sm">Tổng cộng</p>
              <p className="text-white font-bold text-xl">
                {formatCurrency(
                  total + Object.entries(selectedCombos).reduce((acc, [id, qty]) => {
                    const combo = combos?.find(c => c.id === id)
                    return acc + (combo ? combo.price * qty : 0)
                  }, 0)
                )}
              </p>
            </div>
            <button
              onClick={() => {
                window.scrollTo(0, 0)
                navigate('/booking/confirm')
              }}
              className="btn-primary px-8 py-3 w-full sm:w-auto mt-4 sm:mt-0"
            >Tiếp tục <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </motion.div>
      </div>
    </div>
  )
}
