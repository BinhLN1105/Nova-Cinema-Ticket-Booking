import { useState, useMemo, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2, Calendar, Clock, Film, MapPin, Users, DollarSign, ChevronDown, ChevronUp, LayoutGrid, GanttChart, Search, X, Filter, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react'
import { showtimeApi, movieApi, cinemaApi } from '@/api/endpoints'
import { PageHeader } from '@/components/common/ui/AdminTable'
import { Modal } from '@/components/common/ui/Modal'
import { Button } from '@/components/common/ui/FormElements'
import { formatCurrency, formatDateTime } from '@/utils'
import { useDebounce } from '@/hooks/useDebounce'
import toast from 'react-hot-toast'
import PricingOverrideModal from './PricingOverrideModal'
import GanttTimeline from './GanttTimeline'

const STATUS_BADGE = {
  SCHEDULED:  { label: 'Sắp chiếu',   cls: 'bg-blue-50 text-blue-600 border-blue-100' },
  ONGOING:    { label: 'Đang chiếu',   cls: 'bg-green-50 text-green-600 border-green-100' },
  FINISHED:    { label: 'Đã chiếu',     cls: 'bg-gray-100 text-gray-400 border-gray-200' },
  CANCELLED:   { label: 'Đã huỷ',       cls: 'bg-red-50 text-red-500 border-red-100' },
}

const MOVIES_PAGE_SIZE = 4

function CinemaShowtimesList({ cinema, onOpenCreate, onOpenDelete }) {
  const [expanded, setExpanded] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const debouncedSearch = useDebounce(searchTerm, 300)
  
  const [selectedDate, setSelectedDate] = useState('')
  const [selectedStatus, setSelectedStatus] = useState('ALL')
  const [currentPage, setCurrentPage] = useState(1)

  const todayStr = useMemo(() => new Date().toISOString().split('T')[0], [])
  const tomorrowStr = useMemo(() => {
    const d = new Date()
    d.setDate(d.getDate() + 1)
    return d.toISOString().split('T')[0]
  }, [])
  
  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['admin-showtimes', cinema.id, selectedDate],
    queryFn: () => showtimeApi.getAll({ 
      cinemaId: cinema.id, 
      date: selectedDate || undefined, 
      size: 500 
    }),
    select: r => r?.content || [],
    enabled: expanded,
    staleTime: 30000,
  })

  // Filter and Group showtimes by movie
  const { filteredGroups, totalShowtimesCount } = useMemo(() => {
    if (!data) return { filteredGroups: [], totalShowtimesCount: 0 }
    
    // 1. Lọc theo status
    let showtimes = data
    if (selectedStatus !== 'ALL') {
      showtimes = showtimes.filter(s => s.status === selectedStatus)
    }

    // 2. Sắp xếp theo thời gian tăng dần
    const sortedData = [...showtimes].sort(
      (a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
    )
    
    // 3. Gom nhóm theo phim
    const map = new Map()
    sortedData.forEach(s => {
      const mId = s.movieId || 'unknown'
      if (!map.has(mId)) {
        map.set(mId, {
          movieId: mId,
          movieTitle: s.movieTitle || 'Phim không xác định',
          moviePosterUrl: s.moviePosterUrl,
          movieDuration: s.movieDuration,
          movieRated: s.movieRated,
          showtimes: []
        })
      }
      map.get(mId).showtimes.push(s)
    })

    let groups = Array.from(map.values())

    // 4. Lọc theo tên phim (Debounced Search)
    if (debouncedSearch.trim()) {
      const query = debouncedSearch.toLowerCase().trim()
      groups = groups.filter(g => g.movieTitle.toLowerCase().includes(query))
    }

    const count = groups.reduce((acc, g) => acc + g.showtimes.length, 0)
    return { filteredGroups: groups, totalShowtimesCount: count }
  }, [data, selectedStatus, debouncedSearch])

  // Pagination for movie groups to keep rendering fast & responsive
  const totalPages = Math.max(1, Math.ceil(filteredGroups.length / MOVIES_PAGE_SIZE))
  const paginatedGroups = useMemo(() => {
    const start = (currentPage - 1) * MOVIES_PAGE_SIZE
    return filteredGroups.slice(start, start + MOVIES_PAGE_SIZE)
  }, [filteredGroups, currentPage])

  // Reset to page 1 on filter changes
  useEffect(() => {
    setCurrentPage(1)
  }, [debouncedSearch, selectedDate, selectedStatus])

  const handleResetFilters = () => {
    setSearchTerm('')
    setSelectedDate('')
    setSelectedStatus('ALL')
    setCurrentPage(1)
  }

  const hasActiveFilters = Boolean(searchTerm || selectedDate || selectedStatus !== 'ALL')

  return (
    <div className="border border-gray-200 rounded-xl mb-4 overflow-hidden bg-white shadow-sm transition-all">
      <button
        type="button"
        className="w-full p-4 flex items-center justify-between cursor-pointer hover:bg-gray-50 transition-colors text-left"
        onClick={() => setExpanded(!expanded)}
        aria-expanded={expanded}
      >
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-brand-50 flex items-center justify-center text-brand-600 shrink-0">
            <MapPin className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-bold text-gray-900 flex items-center gap-2">
              {cinema.name}
              {expanded && (
                <span className="text-xs font-normal text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">
                  {totalShowtimesCount} suất chiếu
                </span>
              )}
            </h3>
            <p className="text-sm text-gray-500">{cinema.address}, {cinema.city}</p>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <Button 
            size="sm" 
            variant="ghost" 
            onClick={(e) => { e.stopPropagation(); onOpenCreate(cinema.id) }}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            Thêm suất
          </Button>
          <div className="p-1 rounded-full hover:bg-gray-200 transition-colors">
            {expanded ? <ChevronUp className="w-5 h-5 text-gray-500" /> : <ChevronDown className="w-5 h-5 text-gray-500" />}
          </div>
        </div>
      </button>
      
      {expanded && (
        <div className="p-5 bg-gray-50/50 border-t border-gray-100 space-y-4">
          {/* Controls Bar: Search + Quick Date Filter + Status */}
          <div className="bg-white p-3 rounded-xl border border-gray-200 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-3">
            {/* Search Input (Debounced) */}
            <div className="relative flex-1 min-w-[220px]">
              <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Tìm phim theo tên (độ trễ 300ms)..."
                className="w-full pl-9 pr-8 py-2 bg-gray-50/70 border border-gray-200 rounded-xl text-xs focus:bg-white focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all"
              />
              {searchTerm && (
                <button 
                  onClick={() => setSearchTerm('')} 
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 p-0.5"
                  title="Xóa tìm kiếm"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>

            {/* Quick Date Pills & Date Picker */}
            <div className="flex items-center gap-1.5 flex-wrap">
              <button
                type="button"
                onClick={() => setSelectedDate('')}
                className={`px-2.5 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  !selectedDate ? 'bg-brand-500 text-white shadow-sm font-semibold' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                Tất cả ngày
              </button>
              <button
                type="button"
                onClick={() => setSelectedDate(todayStr)}
                className={`px-2.5 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  selectedDate === todayStr ? 'bg-brand-500 text-white shadow-sm font-semibold' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                Hôm nay
              </button>
              <button
                type="button"
                onClick={() => setSelectedDate(tomorrowStr)}
                className={`px-2.5 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  selectedDate === tomorrowStr ? 'bg-brand-500 text-white shadow-sm font-semibold' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                Ngày mai
              </button>
              <div className="flex items-center gap-1 bg-gray-100 px-2 py-1 rounded-lg border border-gray-200">
                <Calendar className="w-3.5 h-3.5 text-gray-500" />
                <input
                  type="date"
                  value={selectedDate}
                  onChange={(e) => setSelectedDate(e.target.value)}
                  className="bg-transparent text-xs text-gray-700 focus:outline-none cursor-pointer"
                />
              </div>
            </div>

            {/* Status Select */}
            <div className="flex items-center gap-2">
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="px-3 py-1.5 bg-gray-50 border border-gray-200 rounded-xl text-xs text-gray-700 focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 font-medium"
              >
                <option value="ALL">Tất cả trạng thái</option>
                <option value="SCHEDULED">Sắp chiếu</option>
                <option value="ONGOING">Đang chiếu</option>
                <option value="FINISHED">Đã chiếu</option>
                <option value="CANCELLED">Đã huỷ</option>
              </select>

              {hasActiveFilters && (
                <button
                  type="button"
                  onClick={handleResetFilters}
                  className="px-2.5 py-1.5 text-xs text-red-600 bg-red-50 hover:bg-red-100 rounded-xl font-medium transition-colors flex items-center gap-1"
                  title="Đặt lại bộ lọc"
                >
                  <RefreshCw className="w-3.5 h-3.5" /> Xóa lọc
                </button>
              )}
            </div>
          </div>

          {/* Showtimes List */}
          {isLoading ? (
            <div className="flex flex-col items-center justify-center p-8 bg-white rounded-xl border border-gray-200">
              <div className="w-7 h-7 border-2 border-brand-500 border-t-transparent rounded-full animate-spin mb-2"></div>
              <p className="text-xs text-gray-500">Đang tải danh sách suất chiếu...</p>
            </div>
          ) : !data || data.length === 0 ? (
            <div className="text-center text-gray-500 py-8 bg-white rounded-xl border border-gray-200 border-dashed">
              <Film className="w-8 h-8 text-gray-300 mx-auto mb-2" />
              <p className="font-medium text-gray-700">Chưa có suất chiếu nào tại rạp này</p>
              <p className="text-xs text-gray-400 mt-1">Bấm nút "Thêm suất" bên trên để tạo suất chiếu mới.</p>
            </div>
          ) : filteredGroups.length === 0 ? (
            <div className="text-center text-gray-500 py-8 bg-white rounded-xl border border-gray-200 border-dashed">
              <Filter className="w-8 h-8 text-gray-300 mx-auto mb-2" />
              <p className="font-medium text-gray-700">Không tìm thấy suất chiếu nào phù hợp</p>
              <p className="text-xs text-gray-400 mt-1">Hãy thử thay đổi từ khóa tìm kiếm hoặc ngày chiếu.</p>
              <button
                onClick={handleResetFilters}
                className="mt-3 px-3 py-1.5 text-xs font-semibold text-brand-600 bg-brand-50 hover:bg-brand-100 rounded-lg transition-colors inline-flex items-center gap-1"
              >
                <RefreshCw className="w-3 h-3" /> Đặt lại bộ lọc
              </button>
            </div>
          ) : (
            <div className="space-y-6">
              {/* Header summary */}
              <div className="flex items-center justify-between text-xs text-gray-500 px-1">
                <span>
                  Hiển thị <strong>{filteredGroups.length}</strong> phim (<strong>{totalShowtimesCount}</strong> suất chiếu)
                  {totalPages > 1 && ` — Trang ${currentPage}/${totalPages}`}
                </span>
                {isFetching && <span className="text-[11px] text-brand-600 animate-pulse">Đang đồng bộ...</span>}
              </div>

              {paginatedGroups.map((mg) => (
                <div key={mg.movieId} className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm transition-all hover:border-gray-300">
                  <div className="flex items-start gap-4 border-b border-gray-100 pb-4 mb-4">
                    {mg.moviePosterUrl ? (
                      <img src={mg.moviePosterUrl} alt={mg.movieTitle} className="w-16 h-24 object-cover rounded-lg shadow-sm border border-gray-100 shrink-0" />
                    ) : (
                      <div className="w-16 h-24 bg-gray-100 rounded-lg flex items-center justify-center border border-gray-200 shrink-0">
                        <Film className="w-6 h-6 text-gray-400" />
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <h4 className="font-bold text-gray-900 text-lg flex items-center gap-2 flex-wrap">
                        <span className="truncate">{mg.movieTitle}</span>
                        <span className="text-xs font-normal text-brand-600 bg-brand-50 px-2 py-0.5 rounded-full border border-brand-100 shrink-0">
                          {mg.showtimes.length} suất
                        </span>
                      </h4>
                      <p className="text-sm text-gray-500 mt-1.5 flex items-center gap-3">
                        {mg.movieDuration > 0 && <span><Clock className="w-3.5 h-3.5 inline mr-1 text-gray-400"/> {mg.movieDuration} phút</span>}
                        {mg.movieRated && <span className="bg-orange-100 text-orange-700 text-[10px] px-1.5 py-0.5 rounded font-bold border border-orange-200">{mg.movieRated}</span>}
                      </p>
                    </div>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
                    {mg.showtimes.map(s => {
                      const badge = STATUS_BADGE[s.status] || STATUS_BADGE.SCHEDULED
                      const st = new Date(s.startTime)
                      const et = s.endTime ? new Date(s.endTime) : null
                      return (
                        <div key={s.id} className="relative p-3 border border-gray-100 rounded-lg hover:border-brand-300 hover:shadow-md transition-all group bg-gray-50/50 hover:bg-white">
                          <div className="flex justify-between items-start mb-2">
                            <div className="flex flex-col">
                              <span className="font-bold text-gray-900 text-lg leading-tight">
                                {st.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                              </span>
                              {et && (
                                <span className="text-xs text-gray-400 font-medium mt-0.5">
                                  ~ {et.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                                </span>
                              )}
                            </div>
                            <span className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold border uppercase tracking-wider ${badge.cls} mt-0.5`}>
                              {badge.label}
                            </span>
                          </div>
                          <div className="space-y-1 text-xs text-gray-600">
                            <p className="flex items-center gap-1.5"><Calendar className="w-3.5 h-3.5 text-gray-400"/> {st.toLocaleDateString('vi-VN')}</p>
                            <p className="flex items-center gap-1.5"><MapPin className="w-3.5 h-3.5 text-gray-400"/> {s.screenName}</p>
                            <p className="flex items-center gap-1.5"><DollarSign className="w-3.5 h-3.5 text-gray-400"/> {formatCurrency(s.basePrice)}</p>
                          </div>
                          
                          {/* Action overlay */}
                          <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity flex flex-col gap-1">
                            <button 
                              onClick={() => window.dispatchEvent(new CustomEvent('open-override-price', { detail: s.id }))}
                              className="w-8 h-8 flex items-center justify-center bg-blue-100 text-blue-600 rounded-lg hover:bg-blue-500 hover:text-white transition-colors shadow-sm"
                              title="Tùy chỉnh giá ghế"
                            >
                              <DollarSign className="w-4 h-4" />
                            </button>
                            <button 
                              onClick={() => onOpenDelete(s)}
                              className="w-8 h-8 flex items-center justify-center bg-red-100 text-red-600 rounded-lg hover:bg-red-500 hover:text-white transition-colors shadow-sm"
                              title="Xoá suất chiếu"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                </div>
              ))}

              {/* Pagination Controls */}
              {totalPages > 1 && (
                <div className="flex items-center justify-center gap-2 pt-2 pb-1">
                  <button
                    type="button"
                    onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                    disabled={currentPage === 1}
                    className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-gray-200 bg-white hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors flex items-center gap-1"
                  >
                    <ChevronLeft className="w-4 h-4" /> Trước
                  </button>
                  
                  <div className="flex items-center gap-1">
                    {Array.from({ length: totalPages }, (_, i) => i + 1).map((num) => (
                      <button
                        key={num}
                        type="button"
                        onClick={() => setCurrentPage(num)}
                        className={`w-7 h-7 text-xs font-semibold rounded-lg transition-colors ${
                          currentPage === num ? 'bg-brand-500 text-white' : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'
                        }`}
                      >
                        {num}
                      </button>
                    ))}
                  </div>

                  <button
                    type="button"
                    onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                    disabled={currentPage === totalPages}
                    className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-gray-200 bg-white hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors flex items-center gap-1"
                  >
                    Sau <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default function ShowtimesPage() {
  const qc = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)
  const [delTarget, setDelTarget] = useState(null)
  const [overrideShowtimeId, setOverrideShowtimeId] = useState(null)
  const [viewMode, setViewMode] = useState('cards') // 'cards' | 'gantt'
  const [ganttDate, setGanttDate] = useState(new Date().toISOString().split('T')[0])

  // Listen to custom event from child components
  useEffect(() => {
    const handleOpen = (e) => setOverrideShowtimeId(e.detail)
    window.addEventListener('open-override-price', handleOpen)
    return () => window.removeEventListener('open-override-price', handleOpen)
  }, [])

  // Form state
  const [movieId, setMovieId] = useState('')
  const [cinemaId, setCinemaId] = useState('')
  const [screenId, setScreenId] = useState('')
  const [startTime, setStartTime] = useState('')
  const [basePrice, setBasePrice] = useState('75000')
  const [modalMovieSearch, setModalMovieSearch] = useState('')
  const debouncedModalMovieSearch = useDebounce(modalMovieSearch, 250)

  const { data: cinemasData, isLoading: loadingCinemas } = useQuery({
    queryKey: ['admin-cinemas-list'],
    queryFn: () => cinemaApi.getAll(),
    select: r => r || [],
  })

  const { data: moviesData, isLoading: loadingMovies } = useQuery({
    queryKey: ['admin-movies-now-showing'],
    queryFn: () => movieApi.getAll({ status: 'NOW_SHOWING', page: 0, size: 100 }),
    select: r => (r?.content || []).filter(m => m.status === 'NOW_SHOWING'),
  })

  const filteredMoviesForModal = useMemo(() => {
    if (!moviesData) return []
    if (!debouncedModalMovieSearch.trim()) return moviesData
    const q = debouncedModalMovieSearch.toLowerCase().trim()
    return moviesData.filter(m => m.title.toLowerCase().includes(q))
  }, [moviesData, debouncedModalMovieSearch])

  const { data: screensData } = useQuery({
    queryKey: ['admin-screens', cinemaId],
    queryFn: () => cinemaApi.getScreens(cinemaId),
    select: r => r || [],
    enabled: !!cinemaId,
  })

  const createMut = useMutation({
    mutationFn: (data) => showtimeApi.create(data),
    onSuccess: () => {
      toast.success('Tạo suất chiếu thành công')
      setShowCreate(false)
      resetForm()
      qc.invalidateQueries({ queryKey: ['admin-showtimes'] })
    },
    onError: (e) => toast.error(e?.response?.data?.message || 'Lỗi tạo suất chiếu'),
  })

  const deleteMut = useMutation({
    mutationFn: (id) => showtimeApi.delete(id),
    onSuccess: () => {
      toast.success('Đã xoá suất chiếu')
      setDelTarget(null)
      qc.invalidateQueries({ queryKey: ['admin-showtimes'] })
    },
    onError: () => toast.error('Lỗi khi xoá suất chiếu'),
  })

  const resetForm = () => {
    setMovieId(''); setCinemaId(''); setScreenId(''); setStartTime(''); setBasePrice('75000'); setModalMovieSearch('')
  }

  const handleOpenCreate = (cid = '') => {
    setCinemaId(cid)
    setShowCreate(true)
  }

  const handleCreate = (e) => {
    e.preventDefault()
    if (!movieId || !screenId || !startTime || !basePrice) {
      toast.error('Vui lòng điền đầy đủ thông tin')
      return
    }
    createMut.mutate({ movieId, screenId, startTime, basePrice: Number(basePrice) })
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Quản lý suất chiếu"
        subtitle="Hiển thị theo rạp"
        action={
          <div className="flex items-center gap-3">
            <div className="flex bg-gray-100 p-0.5 rounded-lg">
              <button
                onClick={() => setViewMode('cards')}
                className={`px-3 py-1.5 rounded-md text-sm font-medium transition-all flex items-center gap-1.5 ${
                  viewMode === 'cards' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
                }`}
              >
                <LayoutGrid className="w-4 h-4" /> Thẻ
              </button>
              <button
                onClick={() => setViewMode('gantt')}
                className={`px-3 py-1.5 rounded-md text-sm font-medium transition-all flex items-center gap-1.5 ${
                  viewMode === 'gantt' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
                }`}
              >
                <GanttChart className="w-4 h-4" /> Timeline
              </button>
            </div>
            {viewMode === 'gantt' && (
              <input
                type="date"
                value={ganttDate}
                onChange={(e) => setGanttDate(e.target.value)}
                className="px-3 py-1.5 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/20"
              />
            )}
            <Button onClick={() => handleOpenCreate('')} leftIcon={<Plus className="w-4 h-4" />}>
              Thêm suất chiếu
            </Button>
          </div>
        }
      />

      {/* Main Content */}
      <div className="max-w-7xl mx-auto">
        {viewMode === 'gantt' ? (
          <GanttTimeline date={ganttDate} />
        ) : (
          loadingCinemas ? (
            <div className="py-12 text-center text-gray-500">Đang tải danh sách rạp...</div>
          ) : cinemasData?.length === 0 ? (
            <div className="py-12 text-center text-gray-500">Chưa có rạp chiếu phim nào.</div>
          ) : (
            cinemasData.map(cinema => (
              <CinemaShowtimesList 
                key={cinema.id} 
                cinema={cinema} 
                onOpenCreate={handleOpenCreate}
                onOpenDelete={setDelTarget}
              />
            ))
          )
        )}
      </div>

      {/* Create Modal */}
      <Modal open={showCreate} onClose={() => { setShowCreate(false); resetForm() }} title="Thêm suất chiếu mới" size="lg">
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label htmlFor="showtime-movie" className="block text-sm font-medium text-gray-700 mb-1">
              <Film className="w-4 h-4 inline mr-1" /> Phim đang chiếu (NOW_SHOWING)
            </label>

            {/* Quick search input for modal */}
            <div className="relative mb-2">
              <Search className="w-3.5 h-3.5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={modalMovieSearch}
                onChange={(e) => setModalMovieSearch(e.target.value)}
                placeholder="Gõ để tìm nhanh phim..."
                className="w-full pl-8 pr-7 py-1.5 bg-gray-50 border border-gray-200 rounded-lg text-xs focus:bg-white focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-400"
              />
              {modalMovieSearch && (
                <button
                  type="button"
                  onClick={() => setModalMovieSearch('')}
                  className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 p-0.5"
                >
                  <X className="w-3 h-3" />
                </button>
              )}
            </div>

            <select 
              id="showtime-movie" 
              value={movieId} 
              onChange={(e) => setMovieId(e.target.value)}
              className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-400"
            >
              <option value="">Chọn phim đang chiếu ({filteredMoviesForModal.length} phim)...</option>
              {loadingMovies ? (
                <option disabled>Đang tải danh sách phim đang chiếu...</option>
              ) : filteredMoviesForModal.length === 0 ? (
                <option disabled>Không tìm thấy phim phù hợp với từ khóa '{modalMovieSearch}'</option>
              ) : (
                filteredMoviesForModal.map(m => (
                  <option key={m.id} value={m.id}>
                    {m.title} {m.duration ? `(${m.duration} phút)` : ''} {m.rated ? `[${m.rated}]` : ''}
                  </option>
                ))
              )}
            </select>
            <p className="text-[11px] text-gray-500 mt-1">
              Lưu ý: Chỉ những phim ở trạng thái <strong>Đang chiếu</strong> mới được phép tạo suất chiếu và mở bán vé.
            </p>
          </div>

          <div>
            <label htmlFor="showtime-cinema" className="block text-sm font-medium text-gray-700 mb-1">
              <MapPin className="w-4 h-4 inline mr-1" /> Rạp
            </label>
            <select id="showtime-cinema" value={cinemaId} onChange={(e) => { setCinemaId(e.target.value); setScreenId('') }}
              className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-400">
              <option value="">Chọn rạp...</option>
              {(cinemasData || []).map(c => (
                <option key={c.id} value={c.id}>{c.name} — {c.city}</option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="showtime-screen" className="block text-sm font-medium text-gray-700 mb-1">Phòng chiếu</label>
            <select id="showtime-screen" value={screenId} onChange={(e) => setScreenId(e.target.value)}
              disabled={!cinemaId}
              className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-400 disabled:bg-gray-50 disabled:text-gray-300">
              <option value="">{cinemaId ? 'Chọn phòng...' : 'Chọn rạp trước'}</option>
              {(screensData || []).map(s => (
                <option key={s.id} value={s.id}>{s.name} ({s.screenType})</option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="showtime-start-time" className="block text-sm font-medium text-gray-700 mb-1">
                <Clock className="w-4 h-4 inline mr-1" /> Giờ chiếu
              </label>
              <input id="showtime-start-time" type="datetime-local" value={startTime} onChange={(e) => setStartTime(e.target.value)}
                className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-400" />
            </div>
            <div>
              <label htmlFor="showtime-base-price" className="block text-sm font-medium text-gray-700 mb-1">
                <DollarSign className="w-4 h-4 inline mr-1" /> Giá vé (VNĐ)
              </label>
              <input id="showtime-base-price" type="number" value={basePrice} onChange={(e) => setBasePrice(e.target.value)}
                min="0" step="5000"
                className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-400" />
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-gray-100">
            <button type="button" onClick={() => { setShowCreate(false); resetForm() }}
              className="px-4 py-2 text-sm font-semibold text-gray-600 bg-gray-100 rounded-xl hover:bg-gray-200 transition-colors">
              Huỷ
            </button>
            <Button type="submit" loading={createMut.isPending}>
              Tạo suất chiếu
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirm */}
      <Modal open={!!delTarget} onClose={() => setDelTarget(null)} title="Xác nhận xoá" size="sm">
        <p className="text-sm text-gray-600 mb-6">
          Xoá suất chiếu <strong>{delTarget?.movieTitle}</strong> lúc{' '}
          <strong>{delTarget?.startTime ? formatDateTime(delTarget.startTime) : ''}</strong>?
          Hành động này không thể hoàn tác.
        </p>
        <div className="flex justify-end gap-3">
          <button onClick={() => setDelTarget(null)}
            className="px-4 py-2 text-sm font-semibold text-gray-600 bg-gray-100 rounded-xl hover:bg-gray-200 transition-colors">
            Huỷ
          </button>
          <button onClick={() => deleteMut.mutate(delTarget.id)}
            disabled={deleteMut.isPending}
            className="px-4 py-2 text-sm font-bold text-white bg-red-500 rounded-xl hover:bg-red-600 transition-colors disabled:opacity-50">
            {deleteMut.isPending ? 'Đang xoá...' : 'Xoá'}
          </button>
        </div>
      </Modal>

      {overrideShowtimeId && (
        <PricingOverrideModal 
          showtimeId={overrideShowtimeId}
          onClose={() => setOverrideShowtimeId(null)}
          onSuccess={() => {
            setOverrideShowtimeId(null)
            qc.invalidateQueries({ queryKey: ['admin-showtimes'] })
          }}
        />
      )}
    </div>
  )
}
