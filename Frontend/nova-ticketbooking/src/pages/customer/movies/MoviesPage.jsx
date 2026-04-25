import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useQuery } from '@tanstack/react-query'
import { Search, Filter, Star, Clock, Ticket, X } from 'lucide-react'
import { movieApi } from '@/api/endpoints'
import { useDebounce } from '@/hooks'
import { cn, getRatedColor } from '@/utils'

const STATUS_TABS = [
  { value: '',             label: 'Tất cả' },
  { value: 'NOW_SHOWING',  label: 'Đang chiếu' },
  { value: 'COMING_SOON',  label: 'Sắp ra mắt' },
]

function MovieCard({ movie }) {
  const navigate = useNavigate()
  return (
    <motion.div layout
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.95 }}
      transition={{ duration: 0.3 }}
      className="card-cinema cursor-pointer overflow-hidden group"
      onClick={() => navigate(`/movies/${movie.id}`)}>

      {/* Poster */}
      <div className="relative overflow-hidden" style={{ aspectRatio: '2/3' }}>
        <img src={movie.posterUrl || '/placeholder-movie.jpg'} alt={movie.title}
          className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
          loading="lazy" />
        <div className="absolute inset-0 bg-gradient-to-t from-cinema-800/80 to-transparent
          opacity-0 group-hover:opacity-100 transition-opacity duration-300" />

        {/* Rated */}
        <span className={cn('absolute top-2 left-2 badge text-xs', getRatedColor(movie.rated))}>
          {movie.rated}
        </span>

        {/* Status */}
        {movie.status === 'NOW_SHOWING' && (
          <span className="absolute top-2 right-2 badge badge-green text-xs">Đang chiếu</span>
        )}
        {movie.status === 'COMING_SOON' && (
          <span className="absolute top-2 right-2 badge badge-blue text-xs">Sắp ra mắt</span>
        )}

        {/* Hover CTA */}
        <div className="absolute inset-x-3 bottom-3 opacity-0 group-hover:opacity-100
          translate-y-2 group-hover:translate-y-0 transition-all duration-300">
          {movie.status === 'NOW_SHOWING' && (
            <button onClick={(e) => { e.stopPropagation(); navigate(`/booking/showtime/${movie.id}`) }}
              className="btn-primary w-full text-xs py-2">
              <Ticket className="w-3 h-3" /> Đặt vé
            </button>
          )}
        </div>
      </div>

      {/* Info */}
      <div className="p-3">
        <h3 className="font-display font-bold text-white text-sm leading-snug mb-2 line-clamp-2">
          {movie.title}
        </h3>
        <div className="flex items-center justify-between text-xs text-cinema-300">
          <span className="flex items-center gap-1">
            <Star className="w-3 h-3 text-gold-400 fill-current" />
            {movie.avgRating > 0 ? movie.avgRating.toFixed(1) : 'Chưa có'}
          </span>
          <span className="flex items-center gap-1">
            <Clock className="w-3 h-3" /> {movie.duration} phút
          </span>
        </div>
        {movie.genres?.length > 0 && (
          <div className="flex flex-wrap gap-1 mt-2">
            {movie.genres.slice(0, 2).map(g => (
              <span key={g.id} className="text-xs px-1.5 py-0.5 rounded bg-cinema-700 text-cinema-300">
                {g.name}
              </span>
            ))}
          </div>
        )}
      </div>
    </motion.div>
  )
}

function MovieSkeleton() {
  return (
    <div className="rounded-2xl overflow-hidden">
      <div className="skeleton" style={{ aspectRatio: '2/3' }} />
      <div className="p-3 space-y-2 bg-cinema-800">
        <div className="skeleton h-4 rounded w-3/4" />
        <div className="skeleton h-3 rounded w-1/2" />
      </div>
    </div>
  )
}

export default function MoviesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [search, setSearch] = useState(searchParams.get('q') ?? '')
  const [status, setStatus] = useState(searchParams.get('status') ?? '')
  const debouncedSearch = useDebounce(search, 400)

  const { data, isLoading } = useQuery({
    queryKey: ['movies', 'list', status, debouncedSearch],
    queryFn: () => movieApi.getAll({
      status: status || undefined,
      search: debouncedSearch || undefined,
      size: 24,
    }),
  })

  const updateFilter = (key, val) => {
    setSearchParams(p => { p.set(key, val); return p })
  }

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-16">
      <div className="max-w-7xl mx-auto px-4 sm:px-6">

        {/* Header */}
        <div className="mb-10">
          <motion.h1 initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
            className="font-display text-4xl font-bold text-white mb-2">
            Khám phá phim
          </motion.h1>
          <p className="text-cinema-300">
            {data?.totalElements ?? 0} bộ phim đang có
          </p>
        </div>

        {/* Filters */}
        <div className="flex flex-col sm:flex-row gap-4 mb-8">
          {/* Search */}
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-cinema-400" />
            <input
              value={search}
              onChange={e => { setSearch(e.target.value); updateFilter('q', e.target.value) }}
              placeholder="Tìm kiếm phim..."
              className="input-cinema pl-11 pr-10"
            />
            {search && (
              <button onClick={() => { setSearch(''); updateFilter('q', '') }}
                className="absolute right-3.5 top-1/2 -translate-y-1/2 text-cinema-400 hover:text-white">
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          {/* Status tabs */}
          <div className="flex items-center gap-1 p-1 rounded-xl bg-cinema-800 border border-white/5">
            {STATUS_TABS.map(tab => (
              <button key={tab.value}
                onClick={() => { setStatus(tab.value); updateFilter('status', tab.value) }}
                className={cn(
                  'px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200',
                  status === tab.value
                    ? 'bg-brand-500 text-white shadow-glow-red'
                    : 'text-cinema-300 hover:text-white'
                )}>
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        {/* Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
          {isLoading
            ? Array.from({ length: 12 }).map((_, i) => <MovieSkeleton key={i} />)
            : data?.content?.map(movie => <MovieCard key={movie.id} movie={movie} />)
          }
        </div>

        {!isLoading && data?.content?.length === 0 && (
          <div className="text-center py-24">
            <div className="text-6xl mb-4">🎬</div>
            <p className="text-cinema-300 text-lg">Không tìm thấy phim nào</p>
            <button onClick={() => { setSearch(''); setStatus('') }}
              className="btn-ghost mt-4 text-sm">Xóa bộ lọc</button>
          </div>
        )}
      </div>
    </div>
  )
}
