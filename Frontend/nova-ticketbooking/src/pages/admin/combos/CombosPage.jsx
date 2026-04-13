import { useState } from "react";
import { motion } from "framer-motion";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Plus,
  Edit2,
  Trash2,
  X,
  AlertTriangle,
  Loader2,
  PackageOpen,
  ToggleLeft,
  ToggleRight,
  ImagePlus,
} from "lucide-react";
import { comboApi } from "@/api/endpoints";
import { cn } from "@/utils";
import toast from "react-hot-toast";
import ComboFormModal from "./ComboFormModal";
import ComboImageModal from "./ComboImageModal";

export default function AdminCombosPage() {
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [selectedCombo, setSelectedCombo] = useState(null);

  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [comboToDelete, setComboToDelete] = useState(null);

  const [isImageOpen, setIsImageOpen] = useState(false);
  const [comboForImage, setComboForImage] = useState(null);

  const qc = useQueryClient();

  const { data: combos = [], isLoading } = useQuery({
    queryKey: ["admin", "combos"],
    queryFn: () => comboApi.getAvailable().then((r) => r.data ?? r),
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => comboApi.delete(id),
    onSuccess: () => {
      toast.success("Đã xóa combo");
      qc.invalidateQueries({ queryKey: ["admin", "combos"] });
      setIsDeleteOpen(false);
      setComboToDelete(null);
    },
    onError: (err) =>
      toast.error(err.response?.data?.message || "Lỗi khi xóa combo"),
  });

  const confirmDelete = (combo) => {
    setComboToDelete(combo);
    setIsDeleteOpen(true);
  };

  const openEdit = (combo) => {
    setSelectedCombo(combo);
    setIsFormOpen(true);
  };

  const openImage = (combo) => {
    setComboForImage(combo);
    setIsImageOpen(true);
  };

  const TYPE_BADGE = {
    COMBO: "badge-blue",
    FOOD: "badge-green",
    DRINK: "badge-gray",
  };
  const TYPE_LABEL = {
    COMBO: "Combo",
    FOOD: "Đồ ăn",
    DRINK: "Đồ uống",
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 font-display">
            Quản lý Combo &amp; Bắp nước
          </h1>
          <p className="text-gray-500 text-sm">
            {combos.length} combo đang có
          </p>
        </div>
        <button
          onClick={() => {
            setSelectedCombo(null);
            setIsFormOpen(true);
          }}
          className="btn-primary text-sm py-2.5 px-5 bg-brand-500
          hover:bg-brand-600 border-0"
        >
          <Plus className="w-4 h-4" /> Thêm combo
        </button>
      </div>

      {/* Grid cards */}
      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-5">
          {Array.from({ length: 8 }).map((_, i) => (
            <div
              key={i}
              className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden"
            >
              <div className="skeleton h-48 w-full" />
              <div className="p-4 space-y-2">
                <div className="skeleton h-4 w-3/4 rounded" />
                <div className="skeleton h-3 w-1/2 rounded" />
                <div className="skeleton h-3 w-full rounded" />
              </div>
            </div>
          ))}
        </div>
      ) : combos.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-24 text-gray-400">
          <PackageOpen className="w-16 h-16 mb-4 opacity-30" />
          <p className="text-lg font-medium">Chưa có combo nào</p>
          <p className="text-sm mt-1">Nhấn "Thêm combo" để tạo mới</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-5">
          {combos.map((combo) => (
            <motion.div
              key={combo.id}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden group hover:shadow-md transition-shadow"
            >
              {/* Image */}
              <div className="relative h-48 bg-gray-100">
                {combo.imageUrl ? (
                  <img
                    src={combo.imageUrl}
                    alt={combo.name}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-gray-300">
                    <PackageOpen className="w-16 h-16" />
                  </div>
                )}
                {/* Overlay actions on hover */}
                <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                  <button
                    onClick={() => openImage(combo)}
                    className="p-2 bg-white/90 rounded-xl text-gray-700 hover:bg-white hover:scale-105 transition-all"
                    title="Cập nhật ảnh"
                  >
                    <ImagePlus className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => openEdit(combo)}
                    className="p-2 bg-white/90 rounded-xl text-blue-600 hover:bg-white hover:scale-105 transition-all"
                    title="Chỉnh sửa"
                  >
                    <Edit2 className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => confirmDelete(combo)}
                    className="p-2 bg-white/90 rounded-xl text-red-500 hover:bg-white hover:scale-105 transition-all"
                    title="Xóa"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
                {/* Available badge */}
                <div className="absolute top-2 right-2">
                  {combo.isAvailable ? (
                    <span className="flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold bg-green-100 text-green-700">
                      <ToggleRight className="w-3.5 h-3.5" /> Đang bán
                    </span>
                  ) : (
                    <span className="flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold bg-gray-100 text-gray-500">
                      <ToggleLeft className="w-3.5 h-3.5" /> Ẩn
                    </span>
                  )}
                </div>
              </div>

              {/* Info */}
              <div className="p-4">
                <div className="flex items-start justify-between gap-2 mb-1">
                  <p className="font-semibold text-gray-800 text-sm leading-tight line-clamp-2">
                    {combo.name}
                  </p>
                  <span
                    className={cn(
                      "badge text-xs flex-shrink-0",
                      TYPE_BADGE[combo.type] ?? "badge-gray"
                    )}
                  >
                    {TYPE_LABEL[combo.type] ?? combo.type}
                  </span>
                </div>
                {combo.description && (
                  <p className="text-xs text-gray-400 line-clamp-2 mb-2">
                    {combo.description}
                  </p>
                )}
                <p className="text-brand-600 font-bold text-base">
                  {Number(combo.price).toLocaleString("vi-VN")}₫
                </p>
              </div>
            </motion.div>
          ))}
        </div>
      )}

      {/* Form Modal (Create / Edit) */}
      <ComboFormModal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        combo={selectedCombo}
      />

      {/* Image Modal */}
      <ComboImageModal
        isOpen={isImageOpen}
        onClose={() => setIsImageOpen(false)}
        combo={comboForImage}
      />

      {/* Delete Confirmation */}
      {isDeleteOpen && comboToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => !deleteMutation.isPending && setIsDeleteOpen(false)}
          />
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="relative bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden"
          >
            <div className="p-6 text-center">
              <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <AlertTriangle className="w-8 h-8 text-red-500" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-2">
                Xác nhận xóa combo
              </h3>
              <p className="text-gray-500 mb-6">
                Bạn có chắc muốn xóa combo{" "}
                <span className="font-semibold text-gray-900">
                  "{comboToDelete.name}"
                </span>
                ? Hành động này không thể hoàn tác.
              </p>
              <div className="flex gap-3 justify-center">
                <button
                  onClick={() => setIsDeleteOpen(false)}
                  disabled={deleteMutation.isPending}
                  className="px-6 py-2.5 rounded-xl font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition-colors"
                >
                  Hủy bỏ
                </button>
                <button
                  onClick={() => deleteMutation.mutate(comboToDelete.id)}
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
              onClick={() =>
                !deleteMutation.isPending && setIsDeleteOpen(false)
              }
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
