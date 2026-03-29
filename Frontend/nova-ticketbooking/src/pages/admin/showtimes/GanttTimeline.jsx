import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { showtimeApi, cinemaApi } from '@/api/endpoints'
import { Film } from 'lucide-react'

const HOUR_WIDTH = 100 // pixels per hour
const START_HOUR = 8  // 8 AM
const END_HOUR = 24   // Midnight
const TOTAL_HOURS = END_HOUR - START_HOUR
const ROW_HEIGHT = 52

const STATUS_COLORS = {
  SCHEDULED: { bg: 'bg-blue-500', text: 'text-white', border: 'border-blue-600' },
  ONGOING:   { bg: 'bg-green-500', text: 'text-white', border: 'border-green-600' },
  FINISHED:  { bg: 'bg-gray-200', text: 'text-gray-500', border: 'border-gray-300' },
  CANCELLED: { bg: 'bg-red-400', text: 'text-white', border: 'border-red-500' },
}

export default function GanttTimeline({ date }) {
  const { data: cinemas } = useQuery({
    queryKey: ['gantt-cinemas'],
    queryFn: () => cinemaApi.getAll(),
    select: r => r || [],
  })

  return (
    <div className="space-y-6">
      {cinemas?.map(cinema => (
        <CinemaGantt key={cinema.id} cinema={cinema} date={date} />
      ))}
    </div>
  )
}

function CinemaGantt({ cinema, date }) {
  const { data: showtimes, isLoading } = useQuery({
    queryKey: ['gantt-showtimes', cinema.id, date],
    queryFn: () => showtimeApi.getAll({ cinemaId: cinema.id, size: 500 }),
    select: r => r?.content || [],
  })

  // Group showtimes by screen
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
        map.set(key, { screenName: s.screenName || 'Phòng', screenType: s.screenType, items: [] })
      }
      map.get(key).items.push(s)
    })
    return Array.from(map.values())
  }, [showtimes, date])

  if (isLoading) {
    return <div className="skeleton h-24 rounded-xl" />
  }

  if (screenGroups.length === 0) return null

  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
      <div className="p-4 border-b border-gray-100 bg-gray-50">
        <h3 className="font-bold text-gray-900">{cinema.name}</h3>
        <p className="text-xs text-gray-500">{cinema.address}</p>
      </div>

      <div className="overflow-x-auto">
        <div style={{ minWidth: TOTAL_HOURS * HOUR_WIDTH + 160 }}>
          {/* Hour headers */}
          <div className="flex border-b border-gray-100">
            <div className="w-[160px] shrink-0 px-4 py-2 text-xs text-gray-400 font-medium border-r border-gray-100">
              Phòng chiếu
            </div>
            <div className="flex flex-1">
              {Array.from({ length: TOTAL_HOURS }).map((_, i) => (
                <div
                  key={i}
                  className="text-center text-xs text-gray-400 font-medium py-2 border-r border-gray-50"
                  style={{ width: HOUR_WIDTH, minWidth: HOUR_WIDTH }}
                >
                  {String(START_HOUR + i).padStart(2, '0')}:00
                </div>
              ))}
            </div>
          </div>

          {/* Screen rows */}
          {screenGroups.map((group, gi) => (
            <div key={gi} className="flex border-b border-gray-50 last:border-b-0" style={{ height: ROW_HEIGHT }}>
              <div className="w-[160px] shrink-0 px-4 flex items-center border-r border-gray-100">
                <div>
                  <p className="text-sm font-semibold text-gray-800">{group.screenName}</p>
                  <p className="text-[10px] text-gray-400 uppercase">{group.screenType}</p>
                </div>
              </div>
              <div className="relative flex-1" style={{ width: TOTAL_HOURS * HOUR_WIDTH }}>
                {/* Grid lines */}
                {Array.from({ length: TOTAL_HOURS }).map((_, i) => (
                  <div
                    key={i}
                    className="absolute top-0 bottom-0 border-r border-gray-50"
                    style={{ left: i * HOUR_WIDTH }}
                  />
                ))}
                {/* Showtime blocks */}
                {group.items.map(s => {
                  const st = new Date(s.startTime)
                  const et = s.endTime ? new Date(s.endTime) : new Date(st.getTime() + (s.movieDuration || 120) * 60000)
                  const startOffset = (st.getHours() + st.getMinutes() / 60 - START_HOUR) * HOUR_WIDTH
                  const durationHours = (et - st) / 3600000
                  const width = Math.max(durationHours * HOUR_WIDTH, 60)
                  const colors = STATUS_COLORS[s.status] || STATUS_COLORS.SCHEDULED

                  if (startOffset < 0) return null

                  return (
                    <div
                      key={s.id}
                      className={`absolute top-1.5 bottom-1.5 ${colors.bg} ${colors.text} rounded-lg border ${colors.border}
                        px-2 flex items-center gap-1.5 cursor-pointer hover:brightness-110 transition-all overflow-hidden shadow-sm`}
                      style={{ left: startOffset, width }}
                      title={`${s.movieTitle}\n${st.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })} — ${et.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}`}
                    >
                      <Film className="w-3.5 h-3.5 shrink-0 opacity-80" />
                      <span className="text-xs font-semibold truncate">{s.movieTitle}</span>
                      <span className="text-[10px] opacity-70 shrink-0">
                        {st.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                  )
                })}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
