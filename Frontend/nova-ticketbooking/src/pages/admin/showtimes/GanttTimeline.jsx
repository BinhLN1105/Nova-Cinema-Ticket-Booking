import { useMemo, useRef, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { showtimeApi, cinemaApi } from '@/api/endpoints'
import { Film, Clock, ArrowRight } from 'lucide-react'

const HOUR_WIDTH = 100 // pixels per hour
const START_HOUR = 0   // Start from Midnight
const END_HOUR = 24    // Current day end
const TOTAL_HOURS = END_HOUR - START_HOUR
const ROW_HEIGHT = 56

const STATUS_COLORS = {
  SCHEDULED: { bg: 'bg-blue-500', text: 'text-white', border: 'border-blue-600' },
  ONGOING:   { bg: 'bg-green-500', text: 'text-white', border: 'border-green-600' },
  FINISHED:  { bg: 'bg-gray-100', text: 'text-gray-400', border: 'border-gray-200' },
  CANCELLED: { bg: 'bg-red-400', text: 'text-white', border: 'border-red-500' },
}

export default function GanttTimeline({ date }) {
  const { data: cinemas } = useQuery({
    queryKey: ['gantt-cinemas'],
    queryFn: () => cinemaApi.getAll(),
    select: r => r || [],
  })

  return (
    <div className="space-y-6 pb-10">
      {cinemas?.map(cinema => (
        <CinemaGantt key={cinema.id} cinema={cinema} date={date} />
      ))}
    </div>
  )
}

function CinemaGantt({ cinema, date }) {
  const scrollRef = useRef(null)
  const isToday = !date || date === new Date().toISOString().split('T')[0]

  const { data: showtimes, isLoading } = useQuery({
    queryKey: ['gantt-showtimes', cinema.id, date],
    queryFn: () => showtimeApi.getAll({ cinemaId: cinema.id, size: 500 }),
    select: r => r?.content || [],
  })

  // Auto-scroll to 8 AM on mount
  useEffect(() => {
    if (scrollRef.current) {
      // Scroll to 8 AM (8 * 100px)
      scrollRef.current.scrollLeft = 8 * HOUR_WIDTH
    }
  }, [isLoading])

  const scrollToNow = () => {
    if (scrollRef.current) {
      const now = new Date()
      const currentHour = now.getHours() + now.getMinutes() / 60
      scrollRef.current.scroll({
        left: (currentHour - START_HOUR) * HOUR_WIDTH - scrollRef.current.clientWidth / 2,
        behavior: 'smooth'
      })
    }
  }

  const screenGroups = useMemo(() => {
    if (!showtimes) return []
    const dateStr = date || new Date().toISOString().split('T')[0]
    
    const filtered = showtimes.filter(s => {
      const stDate = s.startTime?.split('T')[0]
      return stDate === dateStr
    })

    const map = new Map()
    filtered.forEach(s => {
      const key = s.screenId || s.screenName || 'unknown'
      if (!map.has(key)) {
        map.set(key, { 
          id: s.screenId,
          screenName: s.screenName || 'Phòng', 
          screenType: s.screenType, 
          items: [] 
        })
      }
      map.get(key).items.push(s)
    })
    return Array.from(map.values())
  }, [showtimes, date])

  if (isLoading) {
    return <div className="skeleton h-32 rounded-2xl animate-pulse bg-gray-50" />
  }

  if (screenGroups.length === 0) return null

  // Calculate current time position
  const now = new Date()
  const currentPos = (now.getHours() + now.getMinutes() / 60 - START_HOUR) * HOUR_WIDTH

  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden group">
      <div className="p-4 border-b border-gray-100 bg-gray-50/50 flex items-center justify-between">
        <div>
          <h3 className="font-bold text-gray-900 flex items-center gap-2">
            {cinema.name}
            <span className="text-[10px] bg-indigo-50 text-indigo-600 px-1.5 py-0.5 rounded uppercase">{cinema.cinemaType || 'Premium'}</span>
          </h3>
          <p className="text-xs text-gray-500 mt-0.5">{cinema.address}</p>
        </div>
        <div className="flex gap-2">
          {isToday && (
            <button onClick={scrollToNow} className="text-[11px] font-bold text-emerald-600 bg-emerald-50 px-3 py-1.5 rounded-lg hover:bg-emerald-100 transition-colors flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5" /> Hiện tại
            </button>
          )}
          <button onClick={() => scrollRef.current.scroll({ left: 800, behavior: 'smooth' })} className="text-[11px] font-bold text-gray-500 bg-white border border-gray-200 px-3 py-1.5 rounded-lg hover:bg-gray-50 transition-colors">
            8:00 AM
          </button>
        </div>
      </div>

      <div className="overflow-x-auto scrollbar-thin scrollbar-thumb-gray-200 scrollbar-track-transparent" ref={scrollRef}>
        <div className="relative" style={{ minWidth: TOTAL_HOURS * HOUR_WIDTH + 160 }}>
          {/* Hour headers */}
          <div className="flex border-b border-gray-100 sticky top-0 z-20 bg-white/80 backdrop-blur-sm">
            <div className="w-[160px] shrink-0 px-4 py-3 text-[10px] text-gray-400 font-bold uppercase tracking-widest border-r border-gray-100 bg-gray-50/30">
              Phòng chiếu
            </div>
            <div className="flex flex-1">
              {Array.from({ length: TOTAL_HOURS }).map((_, i) => (
                <div
                  key={i}
                  className="text-center text-[11px] text-gray-400 font-bold py-3 border-r border-gray-50"
                  style={{ width: HOUR_WIDTH, minWidth: HOUR_WIDTH }}
                >
                  {String(START_HOUR + i).padStart(2, '0')}:00
                </div>
              ))}
            </div>
          </div>

          {/* Screen rows */}
          <div className="relative">
            {/* Current Time Marker */}
            {isToday && currentPos >= 0 && currentPos <= TOTAL_HOURS * HOUR_WIDTH && (
              <div 
                className="absolute top-0 bottom-0 w-px bg-red-500 z-10 pointer-events-none"
                style={{ left: currentPos + 160 }}
              >
                <div className="absolute -top-1 -left-1 w-2 h-2 rounded-full bg-red-500 shadow-sm shadow-red-500/50" />
              </div>
            )}

            {screenGroups.map((group, gi) => (
              <div key={gi} className="flex border-b border-gray-50 last:border-b-0 hover:bg-gray-50/30 transition-colors group/row" style={{ height: ROW_HEIGHT }}>
                <div className="w-[160px] shrink-0 px-4 flex items-center border-r border-gray-100 bg-white z-10 group-hover/row:bg-gray-50 transition-colors">
                  <div>
                    <p className="text-sm font-bold text-gray-800 line-clamp-1">{group.screenName}</p>
                    <p className="text-[9px] text-gray-400 font-bold uppercase tracking-wider">{group.screenType}</p>
                  </div>
                </div>
                <div className="relative flex-1" style={{ width: TOTAL_HOURS * HOUR_WIDTH }}>
                  {/* Grid lines */}
                  {Array.from({ length: TOTAL_HOURS }).map((_, i) => (
                    <div
                      key={i}
                      className="absolute top-0 bottom-0 border-r border-gray-50/50"
                      style={{ left: i * HOUR_WIDTH }}
                    />
                  ))}
                  
                  {/* Showtime blocks */}
                  {group.items.map(s => {
                    const st = new Date(s.startTime)
                    const et = s.endTime ? new Date(s.endTime) : new Date(st.getTime() + (s.movieDuration || 120) * 60000)
                    const startOffset = (st.getHours() + st.getMinutes() / 60 - START_HOUR) * HOUR_WIDTH
                    const durationHours = (et - st) / 3600000
                    const width = Math.max(durationHours * HOUR_WIDTH, 40)
                    const colors = STATUS_COLORS[s.status] || STATUS_COLORS.SCHEDULED

                    if (st.getHours() < START_HOUR || st.getHours() >= END_HOUR) return null

                    return (
                      <div
                        key={s.id}
                        className={`absolute top-2 bottom-2 ${colors.bg} ${colors.text} rounded-xl border-2 ${colors.border}
                          px-3 flex items-center gap-2 cursor-pointer hover:scale-[1.02] active:scale-95 transition-all overflow-hidden shadow-sm z-0 hover:z-10`}
                        style={{ left: startOffset, width }}
                        title={`${s.movieTitle}\n${st.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })} — ${et.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}`}
                      >
                        <Film className="w-3.5 h-3.5 shrink-0 opacity-80" />
                        <div className="min-w-0">
                          <p className="text-[10px] font-bold truncate leading-tight">{s.movieTitle}</p>
                          <p className="text-[9px] opacity-70 font-medium truncate leading-tight">
                            {st.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                          </p>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
