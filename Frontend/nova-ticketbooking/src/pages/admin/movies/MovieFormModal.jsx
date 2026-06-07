import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, Loader2, UploadCloud } from "lucide-react";
import { useMutation, useQueryClient, useQuery } from "@tanstack/react-query";
import { movieApi } from "@/api/endpoints";
import toast from "react-hot-toast";
import ImageUploader from "@/components/admin/ImageUploader";
import { CheckCircle2 } from "lucide-react";

const INITIAL_STATE = {
  title: "",
  originalTitle: "",
  description: "",
  duration: 120,
  releaseDate: "",
  endDate: "",
  director: "",
  cast: "",
  language: "Vietnamese",
  rated: "C13",
  posterUrl: "",
  backdropUrl: "",
  trailerUrl: "",
  status: "COMING_SOON",
  genreIds: [],
};

const RATED_OPTIONS = ["P", "K", "C13", "C16", "C18"];
const STATUS_OPTIONS = [
  { value: "NOW_SHOWING", label: "Đang chiếu" },
  { value: "COMING_SOON", label: "Sắp chiếu" },
  { value: "ENDED", label: "Ngừng chiếu" },
];

export default function MovieFormModal({ isOpen, onClose, movie = null }) {
  const [formData, setFormData] = useState(INITIAL_STATE);
  const [isUploadingPoster, setIsUploadingPoster] = useState(false);
  const [isUploadingBackdrop, setIsUploadingBackdrop] = useState(false);
  const qc = useQueryClient();

  const { data: genres } = useQuery({
    queryKey: ["genres"],
    queryFn: movieApi.getGenres,
  });

  const { data: detailedMovie, isLoading: isLoadingMovie } = useQuery({
    queryKey: ["admin", "movie", movie?.id],
    queryFn: () => movieApi.getById(movie.id),
    enabled: !!movie?.id && isOpen,
  });

  const formatDateForInput = (dateVal) => {
    if (!dateVal) return "";
    if (Array.isArray(dateVal)) {
      const [y, m, d] = dateVal;
      return `${y}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
    }
    return String(dateVal).split("T")[0];
  };

  useEffect(() => {
    if (!isOpen) return;

    if (movie?.id) {
      if (detailedMovie) {
        setFormData({
          title: detailedMovie.title || "",
          originalTitle: detailedMovie.originalTitle || "",
          description: detailedMovie.description || "",
          duration: detailedMovie.duration || 120,
          releaseDate: formatDateForInput(detailedMovie.releaseDate),
          endDate: formatDateForInput(detailedMovie.endDate),
          director: detailedMovie.director || "",
          cast: detailedMovie.cast || "",
          language: detailedMovie.language || "Vietnamese",
          rated: detailedMovie.rated || "C13",
          posterUrl: detailedMovie.posterUrl || "",
          backdropUrl: detailedMovie.backdropUrl || "",
          trailerUrl: detailedMovie.trailerUrl || "",
          status: detailedMovie.status || "COMING_SOON",
          genreIds: detailedMovie.genres ? detailedMovie.genres.map((g) => g.id) : [],
        });
      }
    } else {
      setFormData(INITIAL_STATE);
    }
  }, [movie, detailedMovie, isOpen]);

  const mutation = useMutation({
    mutationFn: (data) =>
      movie ? movieApi.update(movie.id, data) : movieApi.create(data),
    onSuccess: () => {
      toast.success(movie ? "Cập nhật thành công!" : "Thêm phim thành công!");
      qc.invalidateQueries({ queryKey: ["admin", "movies"] });
      onClose();
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || "Có lỗi xảy ra");
    },
  });

  const handleChange = (e) => {
    const { name, value, type } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === "number" ? Number(value) : value,
    }));
  };

  const handleGenreChange = (genreId) => {
    setFormData((prev) => ({
      ...prev,
      genreIds: prev.genreIds.includes(genreId)
        ? prev.genreIds.filter((id) => id !== genreId)
        : [...prev.genreIds, genreId],
    }));
  };

  const handlePosterUpload = async (source, type) => {
    if (!movie?.id) {
      toast.error("Vui lòng lưu thông tin phim cơ bản trước khi tải ảnh");
      return;
    }
    
    setIsUploadingPoster(true);
    try {
      let res;
      if (type === 'file') {
        res = await movieApi.uploadPoster(movie.id, source);
      } else {
        res = await movieApi.uploadPosterUrl(movie.id, source);
      }
      setFormData(prev => ({ ...prev, posterUrl: res.posterUrl }));
      qc.invalidateQueries({ queryKey: ["admin", "movie", movie.id] });
    } finally {
      setIsUploadingPoster(false);
    }
  };

  const handleBackdropUpload = async (source, type) => {
    if (!movie?.id) {
      toast.error("Vui lòng lưu thông tin phim cơ bản trước khi tải ảnh");
      return;
    }

    setIsUploadingBackdrop(true);
    try {
      let res;
      if (type === 'file') {
        res = await movieApi.uploadBackdrop(movie.id, source);
      } else {
        res = await movieApi.uploadBackdropUrl(movie.id, source);
      }
      setFormData(prev => ({ ...prev, backdropUrl: res.backdropUrl }));
      qc.invalidateQueries({ queryKey: ["admin", "movie", movie.id] });
    } finally {
      setIsUploadingBackdrop(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    
    // Kiểm tra logic ngày tháng
    if (formData.endDate && formData.releaseDate) {
      const release = new Date(formData.releaseDate);
      const end = new Date(formData.endDate);
      
      if (end < release) {
        toast.error("Ngày kết thúc không được trước ngày phát hành");
        return;
      }
    }

    mutation.mutate(formData);
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="absolute inset-0 bg-black/60 backdrop-blur-sm"
          onClick={onClose}
        />
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 20 }}
          className="relative bg-white rounded-3xl shadow-2xl w-full max-w-2xl max-h-[90vh] flex flex-col overflow-hidden"
        >
          <div className="flex items-center justify-between p-6 border-b border-gray-100">
            <h2 className="text-xl font-bold font-display text-gray-900">
              {movie ? "Cập nhật Phim" : "Thêm Phim Mới"}
            </h2>
            <button
              onClick={onClose}
              className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {isLoadingMovie ? (
            <div className="flex-1 flex items-center justify-center p-12">
              <Loader2 className="w-8 h-8 animate-spin text-brand-500" />
            </div>
          ) : (
            <form
              onSubmit={handleSubmit}
              className="flex-1 overflow-y-auto p-0 space-y-0"
            >
              <div className="p-6 space-y-8">
                {/* Khối 1: Thông tin tiêu đề (Full width) */}
                <div className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label htmlFor="movie-title" className="text-sm font-semibold text-gray-700 block mb-1">Tên phim (Tiếng Việt) *</label>
                      <input id="movie-title" name="title" required value={formData.title} onChange={handleChange}
                        className="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl focus:bg-white focus:border-brand-500 focus:ring-1 focus:ring-brand-500 outline-none transition-all" />
                    </div>
                    <div>
                      <label htmlFor="movie-original-title" className="text-sm font-semibold text-gray-700 block mb-1">Tên gốc (Original)</label>
                      <input id="movie-original-title" name="originalTitle" value={formData.originalTitle} onChange={handleChange}
                        className="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl focus:bg-white focus:border-brand-500 outline-none transition-all" />
                    </div>
                  </div>
                </div>

                {/* Khối 2: Thông số & Trạng thái (Grid) */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 bg-gray-50 p-5 rounded-3xl border border-gray-100">
                  <div>
                    <label htmlFor="movie-duration" className="text-sm font-semibold text-gray-700 block mb-1">Thời lượng (Phút) *</label>
                    <input id="movie-duration" name="duration" type="number" required value={formData.duration} onChange={handleChange}
                      className="w-full px-4 py-2 bg-white border border-gray-200 rounded-xl" />
                  </div>
                  <div>
                    <label htmlFor="movie-rated" className="text-sm font-semibold text-gray-700 block mb-1">Phân loại (Rated) *</label>
                    <select id="movie-rated" name="rated" value={formData.rated} onChange={handleChange}
                      className="w-full px-4 py-2 bg-white border border-gray-200 rounded-xl outline-none">
                      {RATED_OPTIONS.map(opt => <option key={opt} value={opt}>{opt}</option>)}
                    </select>
                  </div>
                  <div>
                    <label htmlFor="movie-status" className="text-sm font-semibold text-gray-700 block mb-1">Trạng thái</label>
                    <select id="movie-status" name="status" value={formData.status} onChange={handleChange}
                      className="w-full px-4 py-2 bg-white border border-gray-200 rounded-xl outline-none font-bold text-brand-600">
                      {STATUS_OPTIONS.map(opt => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
                    </select>
                  </div>
                  <div className="sm:col-span-1">
                    <label htmlFor="movie-release-date" className="text-sm font-semibold text-gray-700 block mb-1">Ngày phát hành *</label>
                    <input id="movie-release-date" name="releaseDate" type="date" required value={formData.releaseDate} onChange={handleChange}
                      className="w-full px-4 py-2 bg-white border border-gray-200 rounded-xl" />
                  </div>
                  <div className="sm:col-span-1">
                    <label htmlFor="movie-end-date" className="text-sm font-semibold text-gray-700 block mb-1">Ngày kết thúc</label>
                    <input 
                      id="movie-end-date"
                      name="endDate" 
                      type="date" 
                      value={formData.endDate} 
                      onChange={handleChange}
                      min={formData.releaseDate}
                      className="w-full px-4 py-2 bg-white border border-gray-200 rounded-xl" 
                    />
                  </div>
                  <div className="sm:col-span-1">
                     <label htmlFor="movie-trailer-url" className="text-sm font-semibold text-gray-700 block mb-1">Trailer (Youtube Link)</label>
                     <input id="movie-trailer-url" name="trailerUrl" value={formData.trailerUrl} onChange={handleChange} placeholder="https://youtube.com/..."
                      className="w-full px-4 py-2 bg-white border border-gray-200 rounded-xl" />
                  </div>
                </div>

                {/* Khối 3: Nhân sự (Director/Cast) */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="movie-director" className="text-sm font-semibold text-gray-700 block mb-1">Đạo diễn</label>
                    <input id="movie-director" name="director" value={formData.director} onChange={handleChange}
                      className="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl" />
                  </div>
                  <div>
                    <label htmlFor="movie-cast" className="text-sm font-semibold text-gray-700 block mb-1">Diễn viên chính</label>
                    <input id="movie-cast" name="cast" value={formData.cast} onChange={handleChange}
                      className="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl" />
                  </div>
                </div>

                {/* Khối 4: Media (Full width blocks) */}
                <div className="space-y-8 pt-4 border-t border-gray-100">
                  <ImageUploader 
                    label="Poster chính thức (Ảnh dọc)"
                    value={formData.posterUrl}
                    onUpload={handlePosterUpload}
                    isLoading={isUploadingPoster}
                    aspectRatio="2:3"
                    helperText="Ảnh hiển thị tại danh sách phim (Ratio 2:3)."
                  />

                  <ImageUploader 
                    label="Backdrop / Banner (Ảnh ngang)"
                    value={formData.backdropUrl}
                    onUpload={handleBackdropUpload}
                    isLoading={isUploadingBackdrop}
                    aspectRatio="16:9"
                    helperText="Ảnh làm nền cho trang chi tiết phim (Ratio 16:9)."
                  />
                </div>

                {/* Khối 5: Thể loại & Mô tả */}
                <div className="space-y-6 pt-4 border-t border-gray-100">
                  <div>
                    <label className="text-sm font-semibold text-gray-700 block mb-3">Thể loại phim</label>
                    <div className="flex flex-wrap gap-2">
                      {genres?.map((g) => (
                        <button key={g.id} type="button" onClick={() => handleGenreChange(g.id)}
                          className={`px-4 py-2 rounded-xl text-xs font-bold border transition-all ${
                            formData.genreIds.includes(g.id)
                              ? "bg-brand-500 border-brand-500 text-white shadow-md shadow-brand-500/20 scale-105"
                              : "bg-white border-gray-200 text-gray-500 hover:border-brand-300"
                          }`}>
                          {g.name}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div>
                    <label htmlFor="movie-description" className="text-sm font-semibold text-gray-700 block mb-2">Tóm tắt nội dung</label>
                    <textarea id="movie-description" name="description" rows={5} value={formData.description} onChange={handleChange}
                      placeholder="Nhập nội dung phim..."
                      className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-brand-500 outline-none resize-none transition-all" />
                  </div>
                </div>
              </div>
            </form>

          )}

          <div className="p-6 border-t border-gray-100 bg-gray-50 flex justify-end gap-3">
            <button
              onClick={onClose}
              type="button"
              className="px-6 py-2.5 rounded-xl font-medium text-gray-700 hover:bg-gray-200 bg-white border border-gray-200 transition-colors"
            >
              Hủy
            </button>
            <button
              onClick={handleSubmit}
              disabled={mutation.isPending || isUploadingPoster || isUploadingBackdrop}
              className="px-6 py-2.5 rounded-xl font-medium text-white bg-brand-500 hover:bg-brand-600 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
            >
              {mutation.isPending ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : isUploadingPoster || isUploadingBackdrop ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <CheckCircle2 className="w-4 h-4" />
              )}
              {mutation.isPending ? "Đang xử lý..." : isUploadingPoster || isUploadingBackdrop ? "Đang tải ảnh..." : movie ? "Lưu thay đổi" : "Thêm mới"}
            </button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
