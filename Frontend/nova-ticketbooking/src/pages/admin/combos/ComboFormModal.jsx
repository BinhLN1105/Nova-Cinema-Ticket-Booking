import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { X, Loader2, Save } from "lucide-react";
import { comboApi } from "@/api/endpoints";
import toast from "react-hot-toast";

const TYPE_OPTIONS = [
  { value: "COMBO", label: "Combo" },
  { value: "FOOD", label: "Đồ ăn" },
  { value: "DRINK", label: "Đồ uống" },
];

const emptyForm = {
  name: "",
  description: "",
  price: "",
  type: "COMBO",
  isAvailable: true,
};

export default function ComboFormModal({ isOpen, onClose, combo }) {
  const [form, setForm] = useState(emptyForm);
  const qc = useQueryClient();
  const isEdit = !!combo;

  useEffect(() => {
    if (combo) {
      setForm({
        name: combo.name ?? "",
        description: combo.description ?? "",
        price: combo.price ?? "",
        type: combo.type ?? "COMBO",
        isAvailable: combo.isAvailable ?? true,
      });
    } else {
      setForm(emptyForm);
    }
  }, [combo, isOpen]);

  const mutation = useMutation({
    mutationFn: (data) =>
      isEdit ? comboApi.update(combo.id, data) : comboApi.create(data),
    onSuccess: () => {
      toast.success(isEdit ? "Cập nhật combo thành công" : "Thêm combo thành công");
      qc.invalidateQueries({ queryKey: ["admin", "combos"] });
      onClose();
    },
    onError: (err) =>
      toast.error(err.response?.data?.message || "Lỗi khi lưu combo"),
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!form.name.trim()) return toast.error("Tên combo không được để trống");
    if (!form.price || Number(form.price) <= 0) return toast.error("Giá phải lớn hơn 0");
    mutation.mutate({
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      price: Number(form.price),
      type: form.type,
      isAvailable: form.isAvailable,
    });
  };

  const set = (field) => (e) =>
    setForm((f) => ({ ...f, [field]: e.target.value }));

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => !mutation.isPending && onClose()}
          />

          {/* Modal */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 16 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 16 }}
            transition={{ duration: 0.22 }}
            className="relative bg-white rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden"
          >
            {/* Header */}
            <div className="flex items-center justify-between px-6 py-5 border-b border-gray-100">
              <h2 className="text-lg font-bold text-gray-900 font-display">
                {isEdit ? "Chỉnh sửa Combo" : "Thêm Combo mới"}
              </h2>
              <button
                onClick={() => !mutation.isPending && onClose()}
                className="p-2 rounded-full text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Form */}
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              {/* Name */}
              <div>
                <label htmlFor="combo-name" className="block text-sm font-semibold text-gray-700 mb-1.5">
                  Tên combo <span className="text-red-500">*</span>
                </label>
                <input
                  id="combo-name"
                  value={form.name}
                  onChange={set("name")}
                  placeholder="VD: Combo Bắp Vừa + Nước Ngọt"
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 text-sm text-gray-800
                    focus:outline-none focus:ring-2 focus:ring-brand-400 focus:border-transparent transition-all"
                />
              </div>

              {/* Description */}
              <div>
                <label htmlFor="combo-description" className="block text-sm font-semibold text-gray-700 mb-1.5">
                  Mô tả
                </label>
                <textarea
                  id="combo-description"
                  value={form.description}
                  onChange={set("description")}
                  rows={3}
                  placeholder="Mô tả ngắn về combo..."
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 text-sm text-gray-800
                    focus:outline-none focus:ring-2 focus:ring-brand-400 focus:border-transparent resize-none transition-all"
                />
              </div>

              {/* Price & Type row */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label htmlFor="combo-price" className="block text-sm font-semibold text-gray-700 mb-1.5">
                    Giá (₫) <span className="text-red-500">*</span>
                  </label>
                  <input
                    id="combo-price"
                    type="number"
                    min={1000}
                    value={form.price}
                    onChange={set("price")}
                    placeholder="VD: 75000"
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 text-sm text-gray-800
                      focus:outline-none focus:ring-2 focus:ring-brand-400 focus:border-transparent transition-all"
                  />
                </div>
                <div>
                  <label htmlFor="combo-type" className="block text-sm font-semibold text-gray-700 mb-1.5">
                    Loại
                  </label>
                  <select
                    id="combo-type"
                    value={form.type}
                    onChange={set("type")}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 text-sm text-gray-800
                      focus:outline-none focus:ring-2 focus:ring-brand-400 focus:border-transparent transition-all bg-white"
                  >
                    {TYPE_OPTIONS.map((o) => (
                      <option key={o.value} value={o.value}>
                        {o.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Available toggle */}
              <div className="flex items-center justify-between py-3 px-4 bg-gray-50 rounded-xl border border-gray-100">
                <div>
                  <p className="text-sm font-semibold text-gray-700">Hiển thị bán</p>
                  <p className="text-xs text-gray-400">Combo sẽ xuất hiện cho khách đặt vé</p>
                </div>
                <button
                  type="button"
                  onClick={() =>
                    setForm((f) => ({ ...f, isAvailable: !f.isAvailable }))
                  }
                  className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                    form.isAvailable ? "bg-brand-500" : "bg-gray-300"
                  }`}
                >
                  <span
                    className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform ${
                      form.isAvailable ? "translate-x-6" : "translate-x-1"
                    }`}
                  />
                </button>
              </div>

              {/* Actions */}
              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => !mutation.isPending && onClose()}
                  className="flex-1 py-2.5 rounded-xl font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition-colors text-sm"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={mutation.isPending}
                  className="flex-1 py-2.5 rounded-xl font-medium text-white bg-brand-500 hover:bg-brand-600 transition-colors text-sm flex items-center justify-center gap-2"
                >
                  {mutation.isPending ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <Save className="w-4 h-4" />
                  )}
                  {mutation.isPending ? "Đang lưu..." : isEdit ? "Lưu thay đổi" : "Tạo combo"}
                </button>
              </div>
            </form>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}
