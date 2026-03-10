import { motion, AnimatePresence } from "framer-motion";
import { X, PlayCircle, Star, Calendar, Clock, Loader2, Globe, Film, User, Users, Hash, CalendarCheck, CalendarX, Info, Tag } from "lucide-react";
import { formatDate, getRatedColor, cn } from "@/utils";
import { useQuery } from "@tanstack/react-query";
import { movieApi } from "@/api/endpoints";

const STATUS_LABEL = {
  NOW_SHOWING: "Đang chiếu",
  COMING_SOON: "Sắp ra mắt",
  ENDED: "Đã kết thúc",
};

const STATUS_COLOR = {
  NOW_SHOWING: "bg-emerald-100 text-emerald-700 border-emerald-200",
  COMING_SOON: "bg-blue-100 text-blue-700 border-blue-200",
  ENDED: "bg-gray-100 text-gray-500 border-gray-200",
};

function formatDateValue(dateVal) {
  if (!dateVal) return null;
  if (Array.isArray(dateVal)) {
    return `${String(dateVal[2]).padStart(2, "0")}/${String(dateVal[1]).padStart(2, "0")}/${dateVal[0]}`;
  }
  return formatDate(dateVal);
}

function InfoRow({ icon: Icon, label, value, className = "" }) {
  if (!value && value !== 0) return null;
  return (
    <div className={cn("flex items-start gap-3 py-3", className)}>
      <div className="w-8 h-8 rounded-lg bg-gray-100 flex items-center justify-center flex-shrink-0 mt-0.5">
        <Icon className="w-4 h-4 text-gray-500" />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs text-gray-400 font-semibold uppercase tracking-wider mb-0.5">{label}</p>
        <p className="text-gray-800 font-medium text-sm leading-relaxed">{value}</p>
      </div>
    </div>
  );
}

export default function MovieDetailsModal({ isOpen, onClose, movieId }) {
  const { data: movie, isLoading } = useQuery({
    queryKey: ["admin", "movie-detail", movieId],
    queryFn: () => movieApi.getById(movieId),
    enabled: !!movieId && isOpen,
  });

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-[60] flex items-center justify-center p-4">
        {/* Backdrop */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="absolute inset-0 bg-black/60 backdrop-blur-sm"
          onClick={onClose}
        />

        {/* Modal */}
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 20 }}
          className="relative bg-white rounded-3xl shadow-2xl w-full max-w-5xl max-h-[90vh] flex flex-col overflow-hidden"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100 bg-white">
            <h2 className="text-xl font-bold font-display text-gray-900">
              Chi tiết phim
            </h2>
            <button
              onClick={onClose}
              className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto bg-gray-50/50">
            {isLoading ? (
              <div className="flex justify-center flex-col items-center py-20 min-h-[400px]">
                <Loader2 className="w-12 h-12 text-brand-500 animate-spin mb-4" />
                <p className="text-gray-500 font-medium text-lg">Đang tải dữ liệu...</p>
              </div>
            ) : !movie ? (
              <div className="flex items-center justify-center py-20 min-h-[400px]">
                <p className="text-gray-500 font-medium">Không tìm thấy thông tin phim.</p>
              </div>
            ) : (
              <div className="flex flex-col md:flex-row gap-6 p-6">
                {/* Left: Poster */}
                <div className="md:w-[280px] flex-shrink-0">
                  <div className="relative rounded-2xl overflow-hidden shadow-lg group sticky top-6">
                    <img
                      src={movie.posterUrl}
                      alt={movie.title}
                      className="w-full aspect-[2/3] object-cover transition-transform duration-500 group-hover:scale-105"
                    />
                    {movie.trailerUrl && (
                      <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                        <a
                          href={movie.trailerUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="w-16 h-16 bg-brand-500/90 rounded-full flex items-center justify-center text-white backdrop-blur-md hover:scale-110 transition-transform shadow-lg"
                        >
                          <PlayCircle className="w-8 h-8 ml-1" />
                        </a>
                      </div>
                    )}

                    {/* Rating badge */}
                    <div className="absolute top-3 right-3 bg-black/60 backdrop-blur-md text-white px-3 py-1.5 rounded-xl border border-white/10 flex items-center gap-1.5 shadow-lg font-medium text-sm">
                      <Star className="w-4 h-4 text-yellow-400 fill-current" />
                      {movie.avgRating?.toFixed(1) || "0.0"}
                    </div>
                  </div>
                </div>

                {/* Right: All details */}
                <div className="flex-1 min-w-0 flex flex-col gap-5">
                  {/* Title & Original Title */}
                  <div>
                    <h1 className="text-2xl md:text-3xl font-display font-bold text-gray-900 mb-1 leading-tight">
                      {movie.title}
                    </h1>
                    {movie.originalTitle && (
                      <p className="text-base text-gray-500 font-medium italic">{movie.originalTitle}</p>
                    )}
                  </div>

                  {/* Quick badges */}
                  <div className="flex flex-wrap items-center gap-2">
                    <span className={cn("px-3 py-1 rounded-lg text-sm font-bold border shadow-sm", getRatedColor(movie.rated))}>
                      {movie.rated}
                    </span>
                    {movie.status && (
                      <span className={cn("px-3 py-1 rounded-lg text-sm font-semibold border", STATUS_COLOR[movie.status] ?? "bg-gray-100 text-gray-600 border-gray-200")}>
                        {STATUS_LABEL[movie.status] ?? movie.status}
                      </span>
                    )}
                    <span className="flex items-center gap-1.5 px-3 py-1 bg-white border border-gray-100 shadow-sm rounded-lg text-sm font-medium text-gray-600">
                      <Clock className="w-3.5 h-3.5 text-gray-400" />
                      {movie.duration} phút
                    </span>
                  </div>

                  {/* Genres */}
                  {movie.genres && movie.genres.length > 0 && (
                    <div>
                      <p className="text-xs text-gray-400 font-semibold uppercase tracking-wider mb-2">Thể loại</p>
                      <div className="flex flex-wrap gap-2">
                        {movie.genres.map((g) => (
                          <span
                            key={g.id}
                            className="px-3 py-1 bg-gray-100 text-gray-700 rounded-lg text-sm font-medium"
                          >
                            {g.name}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Info grid */}
                  <div className="bg-white rounded-2xl border border-gray-100 shadow-sm divide-y divide-gray-50 px-4">
                    <InfoRow icon={User} label="Đạo diễn" value={movie.director} />
                    <InfoRow icon={Users} label="Diễn viên" value={movie.cast} />
                    <InfoRow icon={Globe} label="Ngôn ngữ" value={movie.language} />
                    <InfoRow icon={CalendarCheck} label="Ngày phát hành" value={formatDateValue(movie.releaseDate)} />
                    <InfoRow icon={CalendarX} label="Ngày kết thúc" value={formatDateValue(movie.endDate)} />
                    <InfoRow icon={Film} label="Trailer URL" value={
                      movie.trailerUrl ? (
                        <a href={movie.trailerUrl} target="_blank" rel="noopener noreferrer" className="text-brand-500 hover:underline break-all">
                          {movie.trailerUrl}
                        </a>
                      ) : null
                    } />
                    <InfoRow icon={Tag} label="Poster URL" value={
                      movie.posterUrl ? (
                        <a href={movie.posterUrl} target="_blank" rel="noopener noreferrer" className="text-brand-500 hover:underline break-all">
                          {movie.posterUrl}
                        </a>
                      ) : null
                    } />
                    <InfoRow icon={Hash} label="ID" value={movie.id} />
                    <InfoRow icon={Info} label="Ngày tạo" value={
                      movie.createdAt
                        ? Array.isArray(movie.createdAt)
                          ? `${String(movie.createdAt[2]).padStart(2, "0")}/${String(movie.createdAt[1]).padStart(2, "0")}/${movie.createdAt[0]} ${String(movie.createdAt[3] ?? 0).padStart(2, "0")}:${String(movie.createdAt[4] ?? 0).padStart(2, "0")}`
                          : new Date(movie.createdAt).toLocaleString("vi-VN")
                        : null
                    } />
                  </div>

                  {/* Description */}
                  <div className="bg-white rounded-2xl p-5 border border-gray-100 shadow-sm">
                    <h4 className="text-sm font-bold text-gray-900 mb-2 font-display">Nội dung phim</h4>
                    <p className="text-gray-600 text-sm leading-relaxed whitespace-pre-line">
                      {movie.description || "Nội dung phim đang được cập nhật."}
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="px-6 py-4 border-t border-gray-100 bg-white flex justify-end">
            <button
              onClick={onClose}
              className="px-6 py-2.5 rounded-xl font-medium text-gray-700 hover:bg-gray-100 transition-colors"
            >
              Đóng
            </button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
