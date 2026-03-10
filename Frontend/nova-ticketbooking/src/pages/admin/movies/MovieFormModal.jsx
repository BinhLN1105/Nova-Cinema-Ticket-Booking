import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, Loader2, UploadCloud } from "lucide-react";
import { useMutation, useQueryClient, useQuery } from "@tanstack/react-query";
import { movieApi } from "@/api/endpoints";
import toast from "react-hot-toast";

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

  const handleSubmit = (e) => {
    e.preventDefault();
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
              className="flex-1 overflow-y-auto p-6 space-y-6"
            >
            {/* Cột chính */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-4">
                <div>
                  <label className="text-sm font-medium text-gray-700 block mb-1">
                    Tên phim (Tiếng Việt) *
                  </label>
                  <input
                    name="title"
                    required
                    value={formData.title}
                    onChange={handleChange}
                    className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:border-brand-500 focus:ring-1 focus:ring-brand-500 outline-none transition-all"
                  />
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-700 block mb-1">
                    Tên gốc (Original)
                  </label>
                  <input
                    name="originalTitle"
                    value={formData.originalTitle}
                    onChange={handleChange}
                    className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:border-brand-500 focus:ring-1 focus:ring-brand-500 outline-none transition-all"
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium text-gray-700 block mb-1">
                      Thời lượng (Phút) *
                    </label>
                    <input
                      name="duration"
                      type="number"
                      required
                      value={formData.duration}
                      onChange={handleChange}
                      className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:border-brand-500 focus:ring-1 focus:ring-brand-500 outline-none transition-all"
                    />
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-700 block mb-1">
                      Phân loại *
                    </label>
                    <select
                      name="rated"
                      value={formData.rated}
                      onChange={handleChange}
                      className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:border-brand-500 outline-none"
                    >
                      {RATED_OPTIONS.map((opt) => (
                        <option key={opt} value={opt}>
                          {opt}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>

              {/* Poster Preview */}
              <div>
                <label className="text-sm font-medium text-gray-700 block mb-1">
                  Poster URL *
                </label>
                <input
                  name="posterUrl"
                  required
                  value={formData.posterUrl}
                  onChange={handleChange}
                  placeholder="https://..."
                  className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:border-brand-500 outline-none mb-3"
                />
                <div className="h-44 w-full bg-gray-50 border-2 border-dashed border-gray-200 rounded-2xl overflow-hidden flex inset-0 items-center justify-center relative">
                  {formData.posterUrl ? (
                    <img
                      src={formData.posterUrl}
                      alt="Preview"
                      className="w-full h-full object-cover absolute"
                    />
                  ) : (
                    <div className="flex flex-col items-center justify-center text-gray-400 p-4 font-medium text-sm">
                      <UploadCloud className="w-8 h-8 mb-2 opacity-50" />
                      Nhập link ảnh phía trên để xem trước
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Thông tin chiếu */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 bg-gray-50 p-4 rounded-2xl border border-gray-100">
              <div>
                <label className="text-sm font-medium text-gray-700 block mb-1">
                  Ngày phát hành *
                </label>
                <input
                  name="releaseDate"
                  type="date"
                  required
                  value={formData.releaseDate}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm"
                />
              </div>
              <div>
                <label className="text-sm font-medium text-gray-700 block mb-1">
                  Ngày kết thúc (Optional)
                </label>
                <input
                  name="endDate"
                  type="date"
                  value={formData.endDate}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm"
                />
              </div>
              <div>
                <label className="text-sm font-medium text-gray-700 block mb-1">
                  Trạng thái
                </label>
                <select
                  name="status"
                  value={formData.status}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm font-medium text-brand-600"
                >
                  {STATUS_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Chi tiết phim */}
            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-gray-700 block mb-1">
                    Đạo diễn
                  </label>
                  <input
                    name="director"
                    value={formData.director}
                    onChange={handleChange}
                    className="w-full px-4 py-2 border border-gray-200 rounded-xl"
                  />
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-700 block mb-1">
                    Diễn viên
                  </label>
                  <input
                    name="cast"
                    value={formData.cast}
                    onChange={handleChange}
                    className="w-full px-4 py-2 border border-gray-200 rounded-xl"
                  />
                </div>
              </div>
              
              <div>
                <label className="text-sm font-medium text-gray-700 block mb-1">
                  Trailer URL (Youtube)
                </label>
                <input
                 name="trailerUrl"
                 value={formData.trailerUrl}
                 onChange={handleChange}
                 className="w-full px-4 py-2 border border-gray-200 rounded-xl"
                />
              </div>

              <div>
                <label className="text-sm font-medium text-gray-700 block mb-2">
                  Mô tả nội dung
                </label>
                <textarea
                  name="description"
                  rows={4}
                  value={formData.description}
                  onChange={handleChange}
                  className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:border-brand-500 outline-none resize-none"
                />
              </div>

              {/* Thể loại */}
              <div>
                <label className="text-sm font-medium text-gray-700 block mb-2">
                  Thể loại
                </label>
                <div className="flex flex-wrap gap-2">
                  {genres?.map((g) => (
                    <button
                      key={g.id}
                      type="button"
                      onClick={() => handleGenreChange(g.id)}
                      className={`px-3 py-1.5 rounded-full text-xs font-medium border transition-colors ${
                        formData.genreIds.includes(g.id)
                          ? "bg-brand-50 border-brand-200 text-brand-600"
                          : "bg-white border-gray-200 text-gray-600 hover:border-gray-300"
                      }`}
                    >
                      {g.name}
                    </button>
                  ))}
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
              disabled={mutation.isPending}
              className="px-6 py-2.5 rounded-xl font-medium text-white bg-brand-500 hover:bg-brand-600 transition-colors flex items-center gap-2"
            >
              {mutation.isPending && (
                <Loader2 className="w-4 h-4 animate-spin" />
              )}
              {movie ? "Lưu thay đổi" : "Thêm mới"}
            </button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
