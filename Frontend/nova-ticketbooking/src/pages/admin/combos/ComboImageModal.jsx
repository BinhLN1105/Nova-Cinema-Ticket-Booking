import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { X, Loader2, Link2, Upload, ImagePlus } from "lucide-react";
import { comboApi } from "@/api/endpoints";
import toast from "react-hot-toast";
import { cn } from "@/utils";

export default function ComboImageModal({ isOpen, onClose, combo }) {
  const [tab, setTab] = useState("url"); // "url" | "file"
  const [url, setUrl] = useState("");
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const qc = useQueryClient();

  const urlMutation = useMutation({
    mutationFn: () => comboApi.uploadImageUrl(combo.id, url.trim()),
    onSuccess: () => {
      toast.success("Cập nhật ảnh thành công");
      qc.invalidateQueries({ queryKey: ["admin", "combos"] });
      handleClose();
    },
    onError: (err) =>
      toast.error(err.response?.data?.message || "Lỗi khi cập nhật ảnh URL"),
  });

  const fileMutation = useMutation({
    mutationFn: () => comboApi.uploadImage(combo.id, file),
    onSuccess: () => {
      toast.success("Tải ảnh lên thành công");
      qc.invalidateQueries({ queryKey: ["admin", "combos"] });
      handleClose();
    },
    onError: (err) =>
      toast.error(err.response?.data?.message || "Lỗi khi tải ảnh lên"),
  });

  const isPending = urlMutation.isPending || fileMutation.isPending;

  const handleFileChange = (e) => {
    const f = e.target.files?.[0];
    if (!f) return;
    if (f.size > 10 * 1024 * 1024) {
      toast.error("Ảnh tối đa 10MB");
      return;
    }
    setFile(f);
    setPreview(URL.createObjectURL(f));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (tab === "url") {
      if (!url.trim()) return toast.error("Vui lòng nhập URL ảnh");
      urlMutation.mutate();
    } else {
      if (!file) return toast.error("Vui lòng chọn file ảnh");
      fileMutation.mutate();
    }
  };

  const handleClose = () => {
    setUrl("");
    setFile(null);
    setPreview(null);
    onClose();
  };

  if (!combo) return null;

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={() => !isPending && handleClose()}
          />

          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 16 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 16 }}
            transition={{ duration: 0.22 }}
            className="relative bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden"
          >
            {/* Header */}
            <div className="flex items-center justify-between px-6 py-5 border-b border-gray-100">
              <div className="flex items-center gap-2">
                <ImagePlus className="w-5 h-5 text-brand-500" />
                <h2 className="text-lg font-bold text-gray-900 font-display">
                  Cập nhật ảnh — {combo.name}
                </h2>
              </div>
              <button
                onClick={() => !isPending && handleClose()}
                className="p-2 rounded-full text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Tabs */}
            <div className="flex border-b border-gray-100">
              {[
                { key: "url", icon: Link2, label: "Nhập URL" },
                { key: "file", icon: Upload, label: "Upload file" },
              ].map(({ key, icon: Icon, label }) => (
                <button
                  key={key}
                  onClick={() => setTab(key)}
                  className={cn(
                    "flex-1 flex items-center justify-center gap-2 py-3 text-sm font-medium transition-colors border-b-2",
                    tab === key
                      ? "border-brand-500 text-brand-600"
                      : "border-transparent text-gray-500 hover:text-gray-700"
                  )}
                >
                  <Icon className="w-4 h-4" />
                  {label}
                </button>
              ))}
            </div>

            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              {tab === "url" ? (
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                    URL ảnh
                  </label>
                  <input
                    type="url"
                    value={url}
                    onChange={(e) => setUrl(e.target.value)}
                    placeholder="https://example.com/image.jpg"
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 text-sm text-gray-800
                      focus:outline-none focus:ring-2 focus:ring-brand-400 focus:border-transparent transition-all"
                  />
                  {url.trim() && (
                    <div className="mt-3 rounded-xl overflow-hidden border border-gray-100 h-40 bg-gray-50">
                      <img
                        src={url}
                        alt="Preview"
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.target.style.display = "none";
                        }}
                      />
                    </div>
                  )}
                </div>
              ) : (
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                    Chọn ảnh (tối đa 10MB)
                  </label>
                  <label className="flex flex-col items-center justify-center w-full h-36 border-2 border-dashed border-gray-200 rounded-xl cursor-pointer hover:border-brand-400 hover:bg-brand-50/30 transition-all">
                    <input
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={handleFileChange}
                    />
                    {preview ? (
                      <img
                        src={preview}
                        alt="Preview"
                        className="h-full w-full object-cover rounded-xl"
                      />
                    ) : (
                      <div className="flex flex-col items-center text-gray-400">
                        <Upload className="w-8 h-8 mb-2" />
                        <p className="text-sm">Kéo thả hoặc click để chọn</p>
                      </div>
                    )}
                  </label>
                </div>
              )}

              {/* Actions */}
              <div className="flex gap-3 pt-1">
                <button
                  type="button"
                  onClick={() => !isPending && handleClose()}
                  className="flex-1 py-2.5 rounded-xl font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition-colors text-sm"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isPending}
                  className="flex-1 py-2.5 rounded-xl font-medium text-white bg-brand-500 hover:bg-brand-600 transition-colors text-sm flex items-center justify-center gap-2"
                >
                  {isPending ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <ImagePlus className="w-4 h-4" />
                  )}
                  {isPending ? "Đang tải..." : "Cập nhật ảnh"}
                </button>
              </div>
            </form>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}
