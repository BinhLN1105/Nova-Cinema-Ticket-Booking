import { useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Search, Edit2, Trash2, Eye, X, AlertTriangle, Loader2 } from "lucide-react";
import { movieApi } from "@/api/endpoints";
import { formatDate, getRatedColor, cn } from "@/utils";
import toast from "react-hot-toast";
import MovieFormModal from "./MovieFormModal";
import MovieDetailsModal from "./MovieDetailsModal";

export default function AdminMoviesPage() {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  
  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedMovie, setSelectedMovie] = useState(null);

  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [movieToView, setMovieToView] = useState(null);

  // Delete Confirmation state
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [movieToDelete, setMovieToDelete] = useState(null);

  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "movies", search, statusFilter],
    queryFn: () =>
      movieApi.adminGetAll({
        search: search || undefined,
        status: statusFilter || undefined,
      }),
  });

  const deleteMutation = useMutation({
    mutationFn: movieApi.delete,
    onSuccess: () => {
      toast.success("Đã xóa phim");
      qc.invalidateQueries({ queryKey: ["admin", "movies"] });
      setIsDeleteModalOpen(false);
      setMovieToDelete(null);
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || "Lỗi khi xóa phim");
    }
  });

  const confirmDelete = (movie) => {
    setMovieToDelete(movie);
    setIsDeleteModalOpen(true);
  };

  const handleEdit = (movie) => {
    // For edit we need full movie detail to get genres properly. We use the summary from list,
    // but best is to hit getById if genres aren't fully populated in summary.
    // Assuming summary has genres for now.
    setSelectedMovie(movie);
    setIsModalOpen(true);
  };

  const STATUS_OPTS = [
    { value: "", label: "Tất cả" },
    { value: "NOW_SHOWING", label: "Đang chiếu" },
    { value: "COMING_SOON", label: "Sắp ra mắt" },
    { value: "ENDED", label: "Đã kết thúc" },
  ];

  const STATUS_BADGE = {
    NOW_SHOWING: "badge-green",
    COMING_SOON: "badge-blue",
    ENDED: "badge-gray",
  };
  const STATUS_LABEL = {
    NOW_SHOWING: "Đang chiếu",
    COMING_SOON: "Sắp ra mắt",
    ENDED: "Đã kết thúc",
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 font-display">
            Quản lý phim
          </h1>
          <p className="text-gray-500 text-sm">
            {data?.totalElements ?? 0} bộ phim
          </p>
        </div>
        <button
          onClick={() => {
            setSelectedMovie(null);
            setIsModalOpen(true);
          }}
          className="btn-primary text-sm py-2.5 px-5 bg-brand-500
          hover:bg-brand-600 border-0"
        >
          <Plus className="w-4 h-4" /> Thêm phim
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1 max-w-xs">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Tìm kiếm phim..."
            className="w-full pl-10 pr-4 py-2.5 text-sm border border-gray-200 rounded-xl
              bg-white text-gray-700 placeholder-gray-400 focus:outline-none
              focus:border-brand-400 transition-all"
          />
        </div>
        <div className="flex gap-2">
          {STATUS_OPTS.map((opt) => (
            <button
              key={opt.value}
              onClick={() => setStatusFilter(opt.value)}
              className={cn(
                "px-3 py-2 rounded-xl text-sm border transition-all",
                statusFilter === opt.value
                  ? "bg-brand-500 text-white border-brand-500"
                  : "border-gray-200 text-gray-600 hover:border-gray-300 bg-white",
              )}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Phim</th>
                <th>Thể loại</th>
                <th>Phân loại</th>
                <th>Thời lượng</th>
                <th>Ngày phát hành</th>
                <th>Trạng thái</th>
                <th className="text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {isLoading
                ? Array.from({ length: 8 }).map((_, i) => (
                    <tr key={i}>
                      {Array.from({ length: 7 }).map((_, j) => (
                        <td key={j}>
                          <div className="skeleton h-4 rounded w-24" />
                        </td>
                      ))}
                    </tr>
                  ))
                : data?.content?.map((movie) => (
                    <motion.tr
                      key={movie.id}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                    >
                      <td>
                        <div className="flex items-center gap-3">
                          <img
                            src={movie.posterUrl}
                            alt={movie.title}
                            className="w-10 h-14 object-cover rounded-lg flex-shrink-0"
                          />
                          <div>
                            <p className="font-semibold text-gray-800 text-sm max-w-[180px] truncate">
                              {movie.title}
                            </p>
                            <p className="text-xs text-gray-400 flex items-center gap-1 mt-0.5">
                              ⭐ {movie.avgRating.toFixed(1)}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td>
                        <div className="flex flex-wrap gap-1">
                          {movie.genres?.slice(0, 2).map((g) => (
                            <span
                              key={g.id}
                              className="badge badge-gray text-xs"
                            >
                              {g.name}
                            </span>
                          ))}
                          {movie.genres?.length > 2 && (
                            <span className="badge badge-gray text-xs font-semibold">
                              +{movie.genres.length - 2}
                            </span>
                          )}
                        </div>
                      </td>
                      <td>
                        <span
                          className={cn(
                            "badge text-xs",
                            getRatedColor(movie.rated),
                          )}
                        >
                          {movie.rated}
                        </span>
                      </td>
                      <td className="text-gray-600 text-sm">
                        {movie.duration} phút
                      </td>
                      <td className="text-gray-500 text-sm">
                        {Array.isArray(movie.releaseDate) 
                          ? `${String(movie.releaseDate[2]).padStart(2, '0')}/${String(movie.releaseDate[1]).padStart(2, '0')}/${movie.releaseDate[0]}`
                          : formatDate(movie.releaseDate)}
                      </td>
                      <td>
                        <span
                          className={cn(
                            "badge text-xs",
                            STATUS_BADGE[movie.status] ?? "badge-gray",
                          )}
                        >
                          {STATUS_LABEL[movie.status] ?? movie.status}
                        </span>
                      </td>
                      <td>
                        <div className="flex items-center justify-center gap-1">
                          <button
                            onClick={() => {
                              setMovieToView(movie.id);
                              setIsViewModalOpen(true);
                            }}
                            className="p-2 rounded-lg hover:bg-gray-100 text-gray-500
                        hover:text-gray-700 transition-colors"
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleEdit(movie)}
                            className="p-2 rounded-lg hover:bg-gray-100 text-gray-500
                        hover:text-blue-600 transition-colors"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => confirmDelete(movie)}
                            className="p-2 rounded-lg hover:bg-red-50 text-gray-500
                        hover:text-red-500 transition-colors"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </motion.tr>
                  ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between px-6 py-4 border-t border-gray-100">
            <p className="text-sm text-gray-500">
              Hiển thị {data.content.length} / {data.totalElements} phim
            </p>
            <div className="flex gap-1">
              {Array.from({ length: Math.min(data.totalPages, 5) }).map(
                (_, i) => (
                  <button
                    key={i}
                    className={cn(
                      "w-8 h-8 rounded-lg text-sm font-medium transition-all",
                      i === data.page
                        ? "bg-brand-500 text-white"
                        : "text-gray-600 hover:bg-gray-100",
                    )}
                  >
                    {i + 1}
                  </button>
                ),
              )}
            </div>
          </div>
        )}
      </div>

      {/* Modal Form */}
      <MovieFormModal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)} 
        movie={selectedMovie} 
      />

      {/* Details Modal */}
      <MovieDetailsModal
        isOpen={isViewModalOpen}
        onClose={() => setIsViewModalOpen(false)}
        movieId={movieToView}
      />

      {/* Delete Confirmation Modal */}
      {isDeleteModalOpen && movieToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => !deleteMutation.isPending && setIsDeleteModalOpen(false)}
          />
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className="relative bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden"
          >
            <div className="p-6 text-center">
              <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <AlertTriangle className="w-8 h-8 text-red-500" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-2">
                Xác nhận xóa phim
              </h3>
              <p className="text-gray-500 mb-6">
                Bạn có chắc chắn muốn xóa phim <span className="font-semibold text-gray-900">"{movieToDelete.title}"</span> không? Hành động này không thể hoàn tác.
              </p>
              <div className="flex gap-3 justify-center">
                <button
                  onClick={() => setIsDeleteModalOpen(false)}
                  disabled={deleteMutation.isPending}
                  className="px-6 py-2.5 rounded-xl font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition-colors"
                >
                  Hủy bỏ
                </button>
                <button
                  onClick={() => deleteMutation.mutate(movieToDelete.id)}
                  disabled={deleteMutation.isPending}
                  className="px-6 py-2.5 rounded-xl font-medium text-white bg-red-500 hover:bg-red-600 transition-colors flex items-center gap-2"
                >
                  {deleteMutation.isPending ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <Trash2 className="w-4 h-4" />
                  )}
                  {deleteMutation.isPending ? "Đang xóa..." : "Xóa"}
                </button>
              </div>
            </div>
            
            <button
              onClick={() => !deleteMutation.isPending && setIsDeleteModalOpen(false)}
              className="absolute top-4 right-4 p-2 text-gray-400 hover:text-gray-600 rounded-full hover:bg-gray-100 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </motion.div>
        </div>
      )}
    </div>
  );
}
