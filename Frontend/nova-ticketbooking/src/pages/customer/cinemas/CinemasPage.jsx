import React, { useState, useMemo, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  MapPin,
  Phone,
  Navigation,
  Search,
  Calendar,
  Clock,
  Sparkles,
  ExternalLink,
  LocateFixed,
  Film,
  Building2,
  X,
  ChevronRight,
  ChevronLeft,
  Tv,
  Volume2,
  Layers,
  Award,
  ArrowRight
} from 'lucide-react';
import { cinemaApi, showtimeApi } from '@/api/endpoints';
import { useBookingStore } from '@/stores/bookingStore';
import { formatDate, getNext7Days, getDayLabel, cn } from '@/utils';
import { EmptyState, CinemaLoadingState } from '@/components/common/feedback';
import toast from 'react-hot-toast';

// Haversine formula to calculate distance in kilometers
function calculateDistance(lat1, lon1, lat2, lon2) {
  if (!lat1 || !lon1 || !lat2 || !lon2) return null;
  const R = 6371; // Earth's radius in km
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return (R * c).toFixed(1);
}

const CITY_LABELS = {
  'Ho Chi Minh': 'TP. Hồ Chí Minh',
  'Ha Noi': 'Hà Nội',
  'Da Nang': 'Đà Nẵng',
  'Can Tho': 'Cần Thơ',
  'Hai Phong': 'Hải Phòng',
  'Binh Duong': 'Bình Dương',
};

const AMENITIES = [
  { icon: Layers, label: 'IMAX Laser' },
  { icon: Tv, label: '4DX Dynamic' },
  { icon: Volume2, label: 'Dolby Atmos' },
  { icon: Award, label: 'VIP Lounge' },
];

const ITEMS_PER_PAGE = 6;

export default function CinemasPage() {
  const navigate = useNavigate();
  const { setShowtime, setMovie, setDate } = useBookingStore();

  const [search, setSearch] = useState('');
  const [selectedCity, setSelectedCity] = useState('ALL');
  const [userLocation, setUserLocation] = useState(null);
  const [isLocating, setIsLocating] = useState(false);
  const [activeCinemaForShowtimes, setActiveCinemaForShowtimes] = useState(null);
  const [modalDate, setModalDate] = useState(new Date().toISOString().split('T')[0]);
  const [currentPage, setCurrentPage] = useState(1);

  const days = getNext7Days();

  // Reset page when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [search, selectedCity, userLocation]);

  // Fetch all active cinemas
  const { data: cinemas = [], isLoading } = useQuery({
    queryKey: ['cinemas', 'all'],
    queryFn: () => cinemaApi.getAll(),
  });

  // Fetch showtimes for selected cinema in modal
  const { data: cinemaShowtimes = [], isLoading: isShowtimesLoading } = useQuery({
    queryKey: ['showtimes', 'cinema', activeCinemaForShowtimes?.id, modalDate],
    queryFn: () => showtimeApi.getByCinema(activeCinemaForShowtimes.id, modalDate),
    enabled: !!activeCinemaForShowtimes?.id,
  });

  // Extract unique cities
  const cities = useMemo(() => {
    const set = new Set();
    cinemas.forEach((c) => {
      if (c.city) set.add(c.city);
    });
    return Array.from(set);
  }, [cinemas]);

  // Request GPS Location with friendly explanation
  const handleRequestLocation = () => {
    if (!navigator.geolocation) {
      toast.error('Trình duyệt của bạn không hỗ trợ định vị GPS');
      return;
    }

    if (userLocation) {
      // Toggle off
      setUserLocation(null);
      toast('Đã tắt tính năng định vị khoảng cách');
      return;
    }

    setIsLocating(true);
    toast.loading('Đang lấy vị trí của bạn để tìm rạp gần nhất...', { id: 'gps-loc' });

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setIsLocating(false);
        toast.dismiss('gps-loc');
        setUserLocation({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
        });
        toast.success('Đã xác định vị trí! Sắp xếp rạp gần bạn nhất.');
      },
      (error) => {
        setIsLocating(false);
        toast.dismiss('gps-loc');
        if (error.code === error.PERMISSION_DENIED) {
          toast('Bạn đã từ chối cấp quyền vị trí. Vui lòng chọn thành phố bên dưới.', { icon: '📍' });
        } else {
          toast.error('Không thể xác định vị trí hiện tại');
        }
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  };

  // Filter & Sort Cinemas
  const filteredCinemas = useMemo(() => {
    let result = cinemas.filter((c) => {
      const matchCity = selectedCity === 'ALL' || c.city === selectedCity;
      const query = search.toLowerCase().trim();
      const matchSearch =
        !query ||
        c.name?.toLowerCase().includes(query) ||
        c.address?.toLowerCase().includes(query) ||
        c.city?.toLowerCase().includes(query);
      return matchCity && matchSearch;
    });

    // If userLocation is available, calculate distance and sort ascending
    if (userLocation) {
      result = result.map((c) => ({
        ...c,
        distance: calculateDistance(
          userLocation.latitude,
          userLocation.longitude,
          c.latitude,
          c.longitude
        ),
      }));

      result.sort((a, b) => {
        if (!a.distance) return 1;
        if (!b.distance) return -1;
        return parseFloat(a.distance) - parseFloat(b.distance);
      });
    }

    return result;
  }, [cinemas, search, selectedCity, userLocation]);

  // Pagination Logic
  const totalPages = Math.ceil(filteredCinemas.length / ITEMS_PER_PAGE) || 1;
  const paginatedCinemas = useMemo(() => {
    const start = (currentPage - 1) * ITEMS_PER_PAGE;
    return filteredCinemas.slice(start, start + ITEMS_PER_PAGE);
  }, [filteredCinemas, currentPage]);

  // Group showtimes by movie in the modal
  const groupedShowtimesByMovie = useMemo(() => {
    if (!cinemaShowtimes || !Array.isArray(cinemaShowtimes)) return {};
    return cinemaShowtimes.reduce((acc, st) => {
      const movieId = st.movieId || st.movie?.id || 'unknown';
      if (!acc[movieId]) {
        acc[movieId] = {
          movie: st.movie || {
            id: movieId,
            title: st.movieTitle,
            posterUrl: st.moviePosterUrl,
            durationMinutes: st.durationMinutes,
            ageRating: st.ageRating,
          },
          showtimes: [],
        };
      }
      acc[movieId].showtimes.push(st);
      return acc;
    }, {});
  }, [cinemaShowtimes]);

  const handleSelectShowtime = (showtime, movie) => {
    setShowtime(showtime);
    if (movie) setMovie(movie);
    setDate(modalDate);
    setActiveCinemaForShowtimes(null);
    navigate(`/booking/seats/${showtime.id}`);
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-cinema-900 text-slate-900 dark:text-white pt-24 pb-20 transition-colors duration-300">
      <div className="max-w-7xl mx-auto px-4 sm:px-6">
        
        {/* ── Hero Banner Header ── */}
        <div className="text-center max-w-3xl mx-auto mb-10">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-brand-500/10 border border-brand-500/20 text-brand-600 dark:text-brand-400 text-xs font-bold uppercase tracking-wider mb-3">
            <Building2 className="w-4 h-4" />
            Hệ Thống Rạp Chiếu Phim Hiện Đại
          </div>
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-display font-extrabold text-slate-900 dark:text-white tracking-tight mb-4">
            Khám Phá Cụm Rạp <span className="text-brand-500">NovaCinema</span>
          </h1>
          <p className="text-sm sm:text-base text-slate-600 dark:text-cinema-300">
            Trải nghiệm chất lượng hình ảnh sắc nét chuẩn IMAX Laser, âm thanh vòm Dolby Atmos đa chiều cùng không gian ghế ngồi cao cấp tại mọi chi nhánh.
          </p>
        </div>

        {/* ── Search & Filter Controls ── */}
        <div className="bg-white dark:bg-cinema-800/90 border border-slate-200 dark:border-white/10 rounded-3xl p-4 sm:p-6 shadow-sm dark:shadow-card-dark backdrop-blur-md mb-8">
          <div className="flex flex-col md:flex-row items-center gap-3">
            
            {/* Search Input */}
            <div className="relative flex-1 w-full">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 dark:text-slate-400" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Tìm rạp theo tên, địa chỉ, quận/huyện..."
                className="w-full pl-11 pr-4 py-3 rounded-2xl bg-slate-50 dark:bg-cinema-900 border border-slate-200 dark:border-white/10 text-slate-900 dark:text-white text-sm placeholder:text-slate-400 dark:placeholder:text-slate-500 focus:outline-none focus:border-brand-500 transition-all"
              />
              {search && (
                <button
                  onClick={() => setSearch('')}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 p-1 rounded-full text-slate-400 hover:text-slate-600 dark:hover:text-white"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>

            {/* GPS Nearby Button */}
            <button
              onClick={handleRequestLocation}
              disabled={isLocating}
              className={cn(
                'flex items-center justify-center gap-2 px-5 py-3 rounded-2xl text-sm font-bold transition-all whitespace-nowrap w-full md:w-auto cursor-pointer',
                userLocation
                  ? 'bg-emerald-500 text-white shadow-lg shadow-emerald-500/25'
                  : 'bg-slate-100 dark:bg-cinema-700/80 hover:bg-slate-200 dark:hover:bg-cinema-700 text-slate-700 dark:text-white border border-slate-200 dark:border-white/10'
              )}
            >
              <LocateFixed className={cn('w-4 h-4', isLocating && 'animate-spin')} />
              <span>{userLocation ? 'Đang định vị gần bạn' : 'Tìm rạp gần tôi nhất'}</span>
            </button>
          </div>

          {/* City Filter Pills */}
          <div className="flex items-center gap-2 mt-4 overflow-x-auto pb-1 no-scrollbar">
            <button
              onClick={() => setSelectedCity('ALL')}
              className={cn(
                'px-4 py-2 rounded-xl text-xs font-bold transition-all whitespace-nowrap cursor-pointer border',
                selectedCity === 'ALL'
                  ? 'bg-brand-500 text-white border-brand-500 shadow-md shadow-brand-500/20'
                  : 'bg-slate-100 dark:bg-cinema-900 text-slate-700 dark:text-slate-300 border-slate-200 dark:border-white/10 hover:bg-slate-200 dark:hover:bg-cinema-950'
              )}
            >
              Tất cả ({cinemas.length})
            </button>

            {cities.map((city) => {
              const count = cinemas.filter((c) => c.city === city).length;
              const isSelected = selectedCity === city;
              return (
                <button
                  key={city}
                  onClick={() => setSelectedCity(city)}
                  className={cn(
                    'px-4 py-2 rounded-xl text-xs font-bold transition-all whitespace-nowrap cursor-pointer border',
                    isSelected
                      ? 'bg-brand-500 text-white border-brand-500 shadow-md shadow-brand-500/20'
                      : 'bg-slate-100 dark:bg-cinema-900 text-slate-700 dark:text-slate-300 border-slate-200 dark:border-white/10 hover:bg-slate-200 dark:hover:bg-cinema-950'
                  )}
                >
                  {CITY_LABELS[city] || city} ({count})
                </button>
              );
            })}
          </div>
        </div>

        {/* ── Cinemas Grid ── */}
        {isLoading ? (
          <CinemaLoadingState message="Đang tải danh sách cụm rạp NovaCinema..." />
        ) : filteredCinemas.length === 0 ? (
          <EmptyState
            title="Không tìm thấy cụm rạp phù hợp"
            description="Hãy thử thay đổi từ khóa tìm kiếm hoặc chọn tỉnh/thành phố khác nhé!"
            actionLabel="Xem tất cả rạp"
            onAction={() => {
              setSearch('');
              setSelectedCity('ALL');
            }}
          />
        ) : (
          <>
            {/* Header info */}
            <div className="flex items-center justify-between mb-4 px-1">
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Hiển thị <span className="font-bold text-slate-900 dark:text-white">{paginatedCinemas.length}</span> / {filteredCinemas.length} cụm rạp
              </p>
              {totalPages > 1 && (
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Trang <span className="font-bold text-slate-900 dark:text-white">{currentPage}</span> / {totalPages}
                </p>
              )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {paginatedCinemas.map((cinema) => {
                const googleMapsUrl =
                  cinema.latitude && cinema.longitude
                    ? `https://www.google.com/maps/search/?api=1&query=${cinema.latitude},${cinema.longitude}`
                    : `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(
                        `${cinema.name} ${cinema.address || ''}`
                      )}`;

                return (
                  <motion.div
                    key={cinema.id}
                    initial={{ opacity: 0, y: 15 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.3 }}
                    className="group flex flex-col justify-between overflow-hidden border border-slate-200 dark:border-white/10 hover:border-brand-500/40 hover:shadow-xl transition-all duration-300 bg-white dark:bg-cinema-800/90 rounded-3xl"
                  >
                    <div>
                      {/* Cinema Image Banner with Lazy Loading */}
                      <div className="relative h-48 w-full overflow-hidden bg-slate-100 dark:bg-cinema-950">
                        {cinema.imageUrl ? (
                          <img
                            src={cinema.imageUrl}
                            alt={cinema.name}
                            loading="lazy"
                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                          />
                        ) : (
                          <div className="w-full h-full flex flex-col items-center justify-center bg-gradient-to-br from-brand-900/30 to-cinema-900 text-slate-400 dark:text-cinema-400">
                            <Building2 className="w-12 h-12 text-brand-500/40 mb-2" />
                            <span className="text-xs font-semibold">Nova Cinema</span>
                          </div>
                        )}
                        
                        {/* Gradient overlay */}
                        <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/30 to-transparent" />

                        {/* City Badge & Distance */}
                        <div className="absolute top-3 left-3 right-3 flex items-center justify-between">
                          <span className="px-3 py-1 rounded-full bg-black/60 backdrop-blur-md text-white text-[11px] font-bold border border-white/15">
                            {CITY_LABELS[cinema.city] || cinema.city || 'NovaCinema'}
                          </span>
                          {cinema.distance && (
                            <span className="px-3 py-1 rounded-full bg-emerald-500 text-white text-[11px] font-bold shadow-lg flex items-center gap-1">
                              <Navigation className="w-3 h-3" />
                              {cinema.distance} km
                            </span>
                          )}
                        </div>

                        {/* Name on image */}
                        <div className="absolute bottom-3 left-4 right-4">
                          <h3 className="text-lg font-display font-extrabold text-white leading-tight line-clamp-1 drop-shadow-md">
                            {cinema.name}
                          </h3>
                        </div>
                      </div>

                      {/* Cinema Info Body */}
                      <div className="p-5">
                        {/* Address */}
                        <div className="flex items-start gap-2.5 text-xs text-slate-700 dark:text-slate-300 mb-3">
                          <MapPin className="w-4 h-4 text-brand-500 shrink-0 mt-0.5" />
                          <span className="line-clamp-2 leading-relaxed">{cinema.address || 'Đang cập nhật địa chỉ'}</span>
                        </div>

                        {/* Hotline */}
                        {cinema.phone && (
                          <div className="flex items-center gap-2.5 text-xs text-slate-700 dark:text-slate-300 mb-4">
                            <Phone className="w-4 h-4 text-emerald-500 shrink-0" />
                            <a
                              href={`tel:${cinema.phone}`}
                              className="hover:text-brand-500 font-mono font-medium transition-colors"
                            >
                              {cinema.phone}
                            </a>
                          </div>
                        )}

                        {/* Amenities Pills */}
                        <div className="flex flex-wrap gap-1.5 pt-3 border-t border-slate-100 dark:border-white/5">
                          {AMENITIES.map((am, i) => {
                            const Icon = am.icon;
                            return (
                              <span
                                key={i}
                                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-100 dark:bg-cinema-900 border border-slate-200 dark:border-white/5 text-slate-700 dark:text-slate-300 text-[10px] font-semibold"
                              >
                                <Icon className="w-3 h-3 text-brand-500" />
                                {am.label}
                              </span>
                            );
                          })}
                        </div>
                      </div>
                    </div>

                    {/* Actions Footer */}
                    <div className="p-5 pt-0 grid grid-cols-2 gap-2.5">
                      <button
                        onClick={() => setActiveCinemaForShowtimes(cinema)}
                        className="py-2.5 px-3 rounded-xl bg-gradient-to-r from-brand-600 to-brand-500 hover:from-brand-500 hover:to-brand-400 text-white text-xs font-bold flex items-center justify-center gap-1.5 shadow-md shadow-brand-500/20 transition-all cursor-pointer"
                      >
                        <Film className="w-3.5 h-3.5" />
                        <span>Xem lịch chiếu</span>
                      </button>

                      <a
                        href={googleMapsUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="py-2.5 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 bg-slate-100 dark:bg-cinema-700 hover:bg-slate-200 dark:hover:bg-cinema-600 text-slate-800 dark:text-white border border-slate-200 dark:border-white/10 transition-all"
                      >
                        <Navigation className="w-3.5 h-3.5" />
                        <span>Chỉ đường</span>
                      </a>
                    </div>
                  </motion.div>
                );
              })}
            </div>

            {/* ── Pagination Controls ── */}
            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 mt-10">
                <button
                  onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                  disabled={currentPage === 1}
                  className="p-2.5 rounded-xl border border-slate-200 dark:border-white/10 bg-white dark:bg-cinema-800 text-slate-700 dark:text-slate-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-100 dark:hover:bg-cinema-700 transition-colors"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>

                {Array.from({ length: totalPages }, (_, i) => i + 1).map((pageNum) => (
                  <button
                    key={pageNum}
                    onClick={() => setCurrentPage(pageNum)}
                    className={cn(
                      'w-10 h-10 rounded-xl text-xs font-bold transition-all cursor-pointer',
                      currentPage === pageNum
                        ? 'bg-brand-500 text-white shadow-md shadow-brand-500/25 scale-105'
                        : 'bg-white dark:bg-cinema-800 border border-slate-200 dark:border-white/10 text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-cinema-700'
                    )}
                  >
                    {pageNum}
                  </button>
                ))}

                <button
                  onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                  disabled={currentPage === totalPages}
                  className="p-2.5 rounded-xl border border-slate-200 dark:border-white/10 bg-white dark:bg-cinema-800 text-slate-700 dark:text-slate-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-100 dark:hover:bg-cinema-700 transition-colors"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            )}
          </>
        )}

        {/* ── Modal / Drawer Xem Lịch Chiếu Tại Rạp ── */}
        <AnimatePresence>
          {activeCinemaForShowtimes && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-md">
              <motion.div
                initial={{ opacity: 0, scale: 0.95, y: 15 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95, y: 15 }}
                transition={{ duration: 0.25 }}
                className="w-full max-w-3xl bg-white dark:bg-cinema-900 border border-slate-200 dark:border-white/10 rounded-3xl p-6 sm:p-8 shadow-2xl overflow-hidden max-h-[85vh] flex flex-col"
              >
                {/* Modal Header */}
                <div className="flex items-start justify-between pb-4 border-b border-slate-200 dark:border-white/10">
                  <div>
                    <span className="text-xs uppercase font-bold text-brand-600 dark:text-brand-400 tracking-wider">
                      Lịch chiếu phim tại rạp
                    </span>
                    <h2 className="text-2xl font-display font-extrabold text-slate-900 dark:text-white mt-1">
                      {activeCinemaForShowtimes.name}
                    </h2>
                    <p className="text-xs text-slate-600 dark:text-slate-400 mt-1 flex items-center gap-1.5">
                      <MapPin className="w-3.5 h-3.5 text-brand-500" />
                      {activeCinemaForShowtimes.address}
                    </p>
                  </div>
                  <button
                    onClick={() => setActiveCinemaForShowtimes(null)}
                    className="p-2 rounded-full text-slate-400 hover:text-slate-700 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/10 transition-colors"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                {/* Date Selector */}
                <div className="flex items-center gap-2 py-4 overflow-x-auto no-scrollbar border-b border-slate-200 dark:border-white/10">
                  {days.map((d) => {
                    const isSelected = modalDate === d;
                    return (
                      <button
                        key={d}
                        onClick={() => setModalDate(d)}
                        className={cn(
                          'flex flex-col items-center px-4 py-2.5 rounded-2xl text-xs transition-all whitespace-nowrap cursor-pointer shrink-0 border',
                          isSelected
                            ? 'bg-brand-500 text-white font-bold border-brand-500 shadow-lg shadow-brand-500/25 scale-[1.02]'
                            : 'bg-slate-100 dark:bg-cinema-800 border-slate-200 dark:border-white/10 text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-cinema-700'
                        )}
                      >
                        <span className="text-[10px] uppercase opacity-80">{getDayLabel(d)}</span>
                        <span className="text-sm font-extrabold mt-0.5">{formatDate(d)}</span>
                      </button>
                    );
                  })}
                </div>

                {/* Showtimes Content List */}
                <div className="flex-1 overflow-y-auto py-4 pr-1 space-y-4">
                  {isShowtimesLoading ? (
                    <CinemaLoadingState variant="inline" message="Đang tải suất chiếu..." />
                  ) : Object.keys(groupedShowtimesByMovie).length === 0 ? (
                    <EmptyState
                      title="Không có suất chiếu vào ngày này"
                      description="Rạp chưa có lịch chiếu cho ngày bạn chọn. Vui lòng chọn ngày khác nhé!"
                    />
                  ) : (
                    Object.values(groupedShowtimesByMovie).map(({ movie, showtimes }) => (
                      <div
                        key={movie.id}
                        className="p-4 rounded-2xl bg-slate-50 dark:bg-cinema-800/70 border border-slate-200 dark:border-white/10 flex flex-col sm:flex-row gap-4 items-start shadow-sm"
                      >
                        {/* Movie Poster with Lazy Loading */}
                        <div className="w-20 h-28 sm:w-24 sm:h-36 rounded-xl overflow-hidden bg-slate-200 dark:bg-cinema-950 shrink-0 shadow-md">
                          {movie.posterUrl ? (
                            <img
                              src={movie.posterUrl}
                              alt={movie.title}
                              loading="lazy"
                              className="w-full h-full object-cover"
                            />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-xs text-slate-400">
                              No Poster
                            </div>
                          )}
                        </div>

                        {/* Movie Details & Times */}
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-1">
                            {movie.ageRating && (
                              <span className="px-2 py-0.5 rounded-md bg-brand-500/10 text-brand-600 dark:text-brand-400 font-bold text-[10px] border border-brand-500/20">
                                {movie.ageRating}
                              </span>
                            )}
                            <span className="text-xs text-slate-500 dark:text-slate-400 font-medium">
                              {movie.durationMinutes ? `${movie.durationMinutes} phút` : '120 phút'}
                            </span>
                          </div>

                          <h4 className="text-base font-display font-extrabold text-slate-900 dark:text-white mb-3">
                            {movie.title}
                          </h4>

                          {/* Showtime Buttons */}
                          <div className="flex flex-wrap gap-2">
                            {showtimes.map((st) => {
                              const timeStr = st.startTime
                                ? new Date(st.startTime).toLocaleTimeString('vi-VN', {
                                    hour: '2-digit',
                                    minute: '2-digit',
                                  })
                                : st.time || '19:00';

                              return (
                                <button
                                  key={st.id}
                                  onClick={() => handleSelectShowtime(st, movie)}
                                  className="px-3.5 py-2 rounded-xl bg-white dark:bg-cinema-900 border border-slate-200 dark:border-white/10 hover:border-brand-500 hover:bg-brand-500 hover:text-white dark:hover:bg-brand-500 dark:hover:text-white text-slate-800 dark:text-slate-200 transition-all font-mono font-bold text-xs shadow-sm flex items-center gap-1.5 cursor-pointer group"
                                >
                                  <Clock className="w-3.5 h-3.5 text-brand-500 group-hover:text-white" />
                                  <span>{timeStr}</span>
                                  {st.screenType && (
                                    <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-100 dark:bg-white/10 group-hover:bg-white/20 font-sans">
                                      {st.screenType}
                                    </span>
                                  )}
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      </div>
                    ))
                  )}
                </div>

                {/* Modal Footer */}
                <div className="pt-4 border-t border-slate-200 dark:border-white/10 flex justify-end">
                  <button
                    onClick={() => setActiveCinemaForShowtimes(null)}
                    className="px-5 py-2 rounded-xl bg-slate-100 dark:bg-cinema-800 text-xs font-bold text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-cinema-700 transition-colors cursor-pointer"
                  >
                    Đóng
                  </button>
                </div>
              </motion.div>
            </div>
          )}
        </AnimatePresence>

      </div>
    </div>
  );
}

