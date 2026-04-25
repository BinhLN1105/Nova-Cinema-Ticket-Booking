import { useState, useEffect } from 'react'
import { Search, X, Film, Check } from 'lucide-react'
import { movieApi } from '@/api/endpoints'
import { cn } from '@/utils'

export default function MoviePicker({ value = '', onChange }) {
  const [search, setSearch] = useState('')
  const [results, setResults] = useState([])
  const [isLoading, setIsLoading] = useState(false)
  const [selectedMovies, setSelectedMovies] = useState([])

  // Parse initial value (comma-separated IDs) to fetch movie objects
  useEffect(() => {
    if (value && selectedMovies.length === 0) {
      const ids = value.split(',').filter(id => id.trim() !== '')
      if (ids.length > 0) {
        Promise.all(ids.map(id => movieApi.getById(id)))
          .then(res => setSelectedMovies(res.filter(r => r !== null))) // res is already the movie object
          .catch(err => console.error("Error fetching selected movies:", err))
      }
    }
  }, [value])

  // Debounced search
  useEffect(() => {
    const timer = setTimeout(() => {
      if (search.trim().length >= 1) {
        handleSearch()
      } else {
        setResults([])
      }
    }, 500)

    return () => clearTimeout(timer)
  }, [search])

  const handleSearch = async () => {
    setIsLoading(true)
    try {
      const res = await movieApi.search(search)
      // res is already the PageResponse thanks to api helper
      const filtered = (res?.content || []).filter(
        m => !selectedMovies.some(s => s.id === m.id)
      )
      setResults(filtered)
    } catch (err) {
      console.error("Search error:", err)
    } finally {
      setIsLoading(false)
    }
  }

  const addMovie = (movie) => {
    const newSelected = [...selectedMovies, movie]
    setSelectedMovies(newSelected)
    setResults(prev => prev.filter(m => m.id !== movie.id))
    onChange(newSelected.map(m => m.id).join(','))
    setSearch('')
  }

  const removeMovie = (id) => {
    const newSelected = selectedMovies.filter(m => m.id !== id)
    setSelectedMovies(newSelected)
    onChange(newSelected.map(m => m.id).join(','))
  }

  return (
    <div className="space-y-3">
      {/* Selected tags */}
      <div className="flex flex-wrap gap-2">
        {selectedMovies.map(movie => (
          <div key={movie.id} 
            className="flex items-center gap-2 px-3 py-1.5 bg-brand-50 border border-brand-100 rounded-xl text-sm font-medium text-brand-700">
            {movie.posterUrl && (
              <img src={movie.posterUrl} alt="" className="w-5 h-5 rounded-md object-cover" />
            )}
            <span className="max-w-[150px] truncate">{movie.title}</span>
            <button type="button" onClick={() => removeMovie(movie.id)} className="hover:text-red-500 transition-colors">
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        ))}
        {selectedMovies.length === 0 && (
          <p className="text-sm text-gray-400 italic">Chưa chọn phim nào...</p>
        )}
      </div>

      {/* Search Input */}
      <div className="relative">
        <div className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">
          <Search className="w-4 h-4" />
        </div>
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Tìm tên phim để thêm..."
          className="w-full pl-10 pr-4 py-2.5 text-sm bg-white border border-gray-200 rounded-xl 
            focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-400 transition-all"
        />
        {isLoading && (
          <div className="absolute right-3 top-1/2 -translate-y-1/2">
            <div className="w-4 h-4 border-2 border-brand-500/30 border-t-brand-500 rounded-full animate-spin" />
          </div>
        )}
      </div>

      {/* Results Dropdown */}
      {search.trim().length >= 1 && (
        <div className="relative">
          {isLoading ? (
            <div className="absolute top-0 left-0 w-full bg-white border border-gray-100 rounded-xl shadow-xl p-4 text-center text-sm text-gray-500 z-50">
              Đang tìm kiếm...
            </div>
          ) : results.length > 0 ? (
            <div className="absolute top-0 left-0 w-full bg-white border border-gray-100 rounded-xl shadow-xl max-h-60 overflow-y-auto z-50 mt-1">
              {results.map(movie => (
                <button
                  key={movie.id}
                  type="button"
                  onClick={() => addMovie(movie)}
                  className="w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50 text-left transition-colors border-b border-gray-50 last:border-0"
                >
                <div className="w-10 h-14 bg-gray-100 rounded-lg overflow-hidden flex-shrink-0">
                    {movie.posterUrl ? (
                      <img src={movie.posterUrl} alt="" className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-gray-300">
                        <Film className="w-5 h-5" />
                      </div>
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-gray-900 text-sm truncate">{movie.title}</p>
                    <div className="flex items-center gap-2 mt-0.5">
                      {movie.rated && (
                        <span className="px-1 py-0.5 bg-gray-100 text-[10px] font-bold text-gray-600 rounded">
                          {movie.rated}
                        </span>
                      )}
                      <p className="text-[11px] text-gray-500 truncate">
                        {movie.director || 'Chưa rõ đạo diễn'} • {movie.releaseDate?.split('-')[0] || 'N/A'}
                      </p>
                    </div>
                  </div>
                  <PlusIcon className="w-4 h-4 text-brand-500" />
                </button>
              ))}
            </div>
          ) : (
            <div className="absolute top-0 left-0 w-full bg-white border border-gray-100 rounded-xl shadow-xl p-4 text-center text-sm text-gray-500 z-50">
              Không tìm thấy phim nào khớp với "{search}"
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function PlusIcon({ className }) {
  return (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
    </svg>
  )
}
