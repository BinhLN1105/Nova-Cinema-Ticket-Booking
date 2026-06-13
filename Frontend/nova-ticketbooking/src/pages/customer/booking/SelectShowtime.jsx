import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, MapPin, Clock, Monitor } from 'lucide-react'
import { movieApi, cinemaApi, showtimeApi } from '@/api/endpoints'
import { useBookingStore } from '@/stores/bookingStore'
import { formatDate, getNext7Days, getDayLabel, cn } from '@/utils'
import { PageLoader } from '@/components/common/feedback/PageLoader'

const SCREEN_TYPE_COLOR = {
  '2D':   'badge-gray',
  '3D':   'badge-blue',
  'IMAX': 'badge-gold',
  '4DX':  'badge-red',
}

export default function SelectShowtime() {
  const { movieId } = useParams()
  const navigate = useNavigate()
  const { setShowtime, setMovie, setDate, selectedDate } = useBookingStore()

  const [selectedCinema, setSelectedCinema] = useState('')
  const days = getNext7Days()

  const { data: movie, isLoading: isMovieLoading } = useQuery({
    queryKey: ['movies', 'detail', movieId],
    queryFn: () => movieApi.getById(movieId),
    enabled: !!movieId,
  })

  const { data: cinemas } = useQuery({
    queryKey: ['cinemas'],
    queryFn: () => cinemaApi.getAll(),
  })

  const { data: showtimes, isLoading } = useQuery({
    queryKey: ['showtimes', movieId, selectedCinema, selectedDate],
    queryFn: () => showtimeApi.getByMovie(movieId, selectedCinema || undefined, selectedDate),
    enabled: !!movieId,
  })

  if (isMovieLoading) return <PageLoader />

  // Group by cinema
  const grouped = showtimes?.reduce((acc, st) => {
    if (!acc[st.cinemaId]) acc[st.cinemaId] = []
    acc[st.cinemaId].push(st)
    return acc
  }, {}) ?? {}

  const handleSelect = (showtime) => {
    setShowtime(showtime)
    if (movie) setMovie(movie)
    navigate(`/booking/seats/${showtime.id}`)
  }

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-16">
      <div className="max-w-4xl mx-auto px-4 sm:px-6">

        {/* Header */}
        <div className="flex items-start gap-4 mb-8">
          <button onClick={() => navigate(-1)}
            className="p-2.5 mt-1 rounded-xl glass border border-white/8 text-cinema-200
              hover:text-white transition-all flex-shrink-0">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="font-display text-2xl font-bold text-white mb-1">Chọn suất chiếu</h1>
            {movie && <p className="text-brand-400 font-medium">{movie.title}</p>}
          </div>
        </div>

        {/* Date selector */}
        <div className="mb-8">
          <p className="text-gray-300 text-sm uppercase tracking-widest mb-3 font-semibold">Chọn ngày</p>
          <div className="flex gap-3 overflow-x-auto scrollbar-hide pb-2">
            {days.map(day => {
              const isToday = day === days[0]
              const isSelected = day === selectedDate
              return (
                <button key={day} onClick={() => setDate(day)}
                  className={cn(
                    'flex-shrink-0 flex flex-col items-center px-4 py-3 rounded-2xl',
                    'border transition-all duration-300 min-w-[80px]',
                    isSelected
                      ? 'bg-brand-500 border-brand-400 text-white shadow-lg shadow-brand-500/30 translate-y-[-2px]'
                      : 'border-white/10 bg-white/5 text-gray-400 hover:border-white/30 hover:text-white hover:bg-white/10'
                  )}>
                  <span className="text-xs mb-1 font-medium">
                    {isToday ? 'Hôm nay' : formatDate(day, 'EEE')}
                  </span>
                  <span className={cn('text-2xl font-bold font-display leading-none mb-1',
                    isSelected ? 'text-white' : 'text-gray-200')}>
                    {formatDate(day, 'dd')}
                  </span>
                  <span className={cn('text-[10px] uppercase font-bold tracking-wider',
                    isSelected ? 'text-brand-100' : 'text-gray-500')}>
                    {formatDate(day, 'MM/yyyy')}
                  </span>
                </button>
              )
            })}
          </div>
        </div>

        {/* Cinema filter */}
        <div className="mb-8">
          <p className="text-gray-300 text-sm uppercase tracking-widest mb-3 font-semibold">Lọc theo rạp</p>
          <div className="flex gap-2.5 overflow-x-auto scrollbar-hide pb-2 flex-nowrap">
            <button onClick={() => setSelectedCinema('')}
              className={cn('px-5 py-2.5 rounded-xl border text-sm font-semibold transition-all duration-300 flex-shrink-0',
                !selectedCinema
                  ? 'bg-brand-500 border-brand-400 text-white shadow-md shadow-brand-500/20'
                  : 'border-white/10 bg-white/5 text-gray-300 hover:bg-white/10 hover:text-white hover:border-white/30')}>
              Tất cả rạp
            </button>
            {cinemas?.map(c => (
              <button key={c.id} onClick={() => setSelectedCinema(c.id)}
                className={cn('px-5 py-2.5 rounded-xl border text-sm font-semibold transition-all duration-300 flex-shrink-0',
                  selectedCinema === c.id
                    ? 'bg-brand-500 border-brand-400 text-white shadow-md shadow-brand-500/20'
                    : 'border-white/10 bg-white/5 text-gray-300 hover:bg-white/10 hover:text-white hover:border-white/30')}>
                {c.name}
              </button>
            ))}
          </div>
        </div>

        {/* Showtimes by cinema */}
        {isLoading ? (
          <div className="space-y-6">
            {[1, 2].map(i => <div key={i} className="skeleton h-40 rounded-2xl bg-white/5 border border-white/5" />)}
          </div>
        ) : Object.keys(grouped).length === 0 ? (
          <div className="text-center py-20 text-gray-400 bg-white/5 rounded-3xl border border-white/10">
            <Clock className="w-16 h-16 mx-auto mb-4 opacity-20 text-white" />
            <p className="text-lg font-medium text-gray-300">Không có suất chiếu nào được tìm thấy</p>
            <p className="text-sm text-gray-500 mt-2">Vui lòng chọn ngày hoặc rạp khác</p>
          </div>
        ) : (
          <div className="space-y-6">
            {Object.entries(grouped).map(([cinemaId, sts]) => {
              const cinema = cinemas?.find(c => c.id === cinemaId)
              return (
                <motion.div key={cinemaId}
                  initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }}
                  className="bg-white/5 backdrop-blur-md border border-white/10 rounded-2xl p-6 shadow-xl">
                  {/* Cinema Header */}
                  <div className="flex items-start gap-4 mb-6 pb-5 border-b border-white/10">
                    <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-brand-500 to-brand-700 flex items-center justify-center flex-shrink-0 shadow-lg shadow-brand-500/20">
                      <MapPin className="w-6 h-6 text-white" />
                    </div>
                    <div>
                      <h3 className="font-display font-bold text-white text-xl mb-1">{sts[0].cinemaName}</h3>
                      <p className="text-gray-400 text-sm">{cinema?.address}</p>
                    </div>
                  </div>

                  {/* Group by screen */}
                  <div className="space-y-5">
                    {Object.entries(
                      sts.reduce((acc, st) => {
                        const key = `${st.screenName}__${st.screenType}`
                        if (!acc[key]) acc[key] = []
                        acc[key].push(st)
                        return acc
                      }, {})
                    ).map(([screenKey, screenSts]) => {
                      const [screenName, screenType] = screenKey.split('__')
                      return (
                        <div key={screenKey} className="bg-black/20 rounded-xl p-4 border border-white/5">
                          <div className="flex items-center gap-3 mb-4">
                            <Monitor className="w-4 h-4 text-brand-400" />
                            <span className="text-gray-100 font-semibold text-base">{screenName}</span>
                            <span className={cn('badge text-xs px-2 py-0.5', SCREEN_TYPE_COLOR[screenType] ?? 'badge-gray')}>
                              {screenType}
                            </span>
                          </div>
                          
                          <div className="flex flex-wrap gap-3">
                            {screenSts.map(st => {
                              const isFull = st.availableSeats === 0
                              const isLow  = st.availableSeats <= 10 && st.availableSeats > 0
                              return (
                                <button key={st.id}
                                  disabled={isFull}
                                  onClick={() => handleSelect(st)}
                                  className={cn(
                                    'relative flex flex-col items-center justify-center px-5 py-3 rounded-xl border',
                                    'transition-all duration-300 ease-out min-w-[100px] overflow-hidden group',
                                    isFull
                                      ? 'border-white/5 bg-white/5 text-gray-600 cursor-not-allowed'
                                      : 'border-brand-500/40 bg-brand-500/10 hover:bg-brand-500 hover:border-brand-400 hover:shadow-lg hover:shadow-brand-500/25 hover:-translate-y-1'
                                  )}>
                                  {/* Tiêu đề giờ chiếu */}
                                  <span className={cn(
                                    "font-display font-bold text-xl tracking-wider transition-colors",
                                    isFull ? "text-gray-500" : "text-white"
                                  )}>
                                    {st.startTime.slice(11, 16)}
                                  </span>
                                  
                                  {/* Text Đặt vé */}
                                  {!isFull && (
                                    <span className="text-[10px] font-bold uppercase tracking-widest text-brand-300 group-hover:text-white mt-1 transition-colors">
                                      Đặt vé
                                    </span>
                                  )}
                                  
                                  {/* Text Hết vé */}
                                  {isFull && (
                                    <span className="text-[10px] font-bold uppercase tracking-widest text-red-400 mt-1">
                                      Hết vé
                                    </span>
                                  )}

                                  {/* Badge số ghế ít */}
                                  {isLow && (
                                    <span className="absolute top-0 right-0 transform translate-x-1/4 -translate-y-1/4 text-[9px] px-1.5 py-0.5
                                      rounded-full bg-orange-500 text-white font-bold shadow-sm whitespace-nowrap z-10">
                                      {st.availableSeats} ghế
                                    </span>
                                  )}
                                </button>
                              )
                            })}
                          </div>
                        </div>
                      )
                    })}
                  </div>
                </motion.div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
