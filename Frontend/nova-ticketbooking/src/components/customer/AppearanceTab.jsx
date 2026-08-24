import { useState } from "react";
import { Save, RotateCcw, Palette, Moon, Sun, Check, Sparkles } from "lucide-react";
import { useThemeStore } from "@/stores/themeStore";
import toast from "react-hot-toast";
import { cn } from "@/utils";

export function AppearanceTab() {
  const { mode, setMode, brandColor, accentColor, animations, compact, setTheme, resetTheme } =
    useThemeStore();

  const [form, setForm] = useState({
    mode,
    brandColor,
    accentColor,
    animations,
    compact,
  });

  const set = (key, value) => setForm((f) => ({ ...f, [key]: value }));

  const handleSelectMode = (newMode) => {
    set("mode", newMode);
    setMode(newMode);
  };

  const handleSave = () => {
    setTheme(form);
    toast.success("Đã lưu giao diện thành công");
  };

  const handleReset = () => {
    resetTheme();
    setForm({
      mode: "dark",
      brandColor: "#E50914",
      accentColor: "#F5A623",
      animations: true,
      compact: false,
    });
    toast.success("Đã khôi phục giao diện mặc định");
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3 mb-2">
        <div
          className="w-10 h-10 rounded-xl bg-gradient-to-br from-brand-500 to-gold-500
          flex items-center justify-center shadow-md shadow-brand-500/20"
        >
          <Palette className="w-5 h-5 text-white" />
        </div>
        <div>
          <h3 className="font-bold text-lg text-slate-900 dark:text-white">Tuỳ chỉnh giao diện</h3>
          <p className="text-xs text-slate-500 dark:text-cinema-400">
            Cá nhân hóa chế độ hiển thị, màu sắc và hiệu ứng điện ảnh
          </p>
        </div>
      </div>

      {/* ── 1. Chế độ hiển thị (Theme Mode) ── */}
      <div className="bg-slate-100/90 dark:bg-white/[0.04] border border-slate-200 dark:border-white/10 rounded-2xl p-4 sm:p-5 space-y-3">
        <label className="block text-sm font-bold text-slate-800 dark:text-white">
          Chế độ hiển thị
        </label>
        <p className="text-xs text-slate-500 dark:text-cinema-400 -mt-1 mb-3">
          Chọn không gian phòng chiếu tối điện ảnh hoặc sảnh rạp sáng rực rỡ
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
          {/* Dark Mode Card */}
          <button
            type="button"
            onClick={() => handleSelectMode("dark")}
            className={cn(
              "relative p-4 rounded-xl border text-left transition-all duration-300 flex items-center gap-3.5 group",
              form.mode === "dark"
                ? "bg-slate-900 border-gold-500/80 shadow-[0_0_20px_rgba(245,197,24,0.15)] ring-2 ring-gold-500/30 text-white"
                : "bg-white/60 dark:bg-cinema-900/60 border-slate-200 dark:border-white/10 hover:border-slate-300 dark:hover:border-white/20 text-slate-700 dark:text-cinema-300"
            )}
          >
            <div className="w-10 h-10 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center flex-shrink-0 text-gold-400">
              <Moon className="w-5 h-5" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <span className="font-bold text-sm text-slate-900 dark:text-white group-hover:text-gold-400 transition-colors">
                  Tối (Phòng chiếu)
                </span>
                {form.mode === "dark" && (
                  <span className="w-2 h-2 rounded-full bg-gold-400 animate-pulse" />
                )}
              </div>
              <p className="text-xs text-slate-500 dark:text-cinema-400 mt-0.5">
                Nền đen điện ảnh, tập trung xem phim
              </p>
            </div>
            {form.mode === "dark" && (
              <div className="w-6 h-6 rounded-full bg-gold-500 text-cinema-950 flex items-center justify-center flex-shrink-0 font-bold">
                <Check className="w-3.5 h-3.5 stroke-[3]" />
              </div>
            )}
          </button>

          {/* Light Mode Card */}
          <button
            type="button"
            onClick={() => handleSelectMode("light")}
            className={cn(
              "relative p-4 rounded-xl border text-left transition-all duration-300 flex items-center gap-3.5 group",
              form.mode === "light"
                ? "bg-white border-brand-500 shadow-[0_0_20px_rgba(229,9,20,0.15)] ring-2 ring-brand-500/30 text-slate-900"
                : "bg-white/60 dark:bg-cinema-900/60 border-slate-200 dark:border-white/10 hover:border-slate-300 dark:hover:border-white/20 text-slate-700 dark:text-cinema-300"
            )}
          >
            <div className="w-10 h-10 rounded-xl bg-amber-50 dark:bg-slate-800 border border-amber-200 dark:border-slate-700 flex items-center justify-center flex-shrink-0 text-brand-600 dark:text-brand-400">
              <Sun className="w-5 h-5" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <span className="font-bold text-sm text-slate-900 dark:text-white group-hover:text-brand-600 transition-colors">
                  Sáng (Sảnh rạp)
                </span>
                {form.mode === "light" && (
                  <span className="w-2 h-2 rounded-full bg-brand-500 animate-pulse" />
                )}
              </div>
              <p className="text-xs text-slate-500 dark:text-cinema-400 mt-0.5">
                Nền sáng thanh lịch, dễ đọc ban ngày
              </p>
            </div>
            {form.mode === "light" && (
              <div className="w-6 h-6 rounded-full bg-brand-500 text-white flex items-center justify-center flex-shrink-0 font-bold">
                <Check className="w-3.5 h-3.5 stroke-[3]" />
              </div>
            )}
          </button>
        </div>
      </div>

      {/* ── 2. Màu chủ đạo ── */}
      <div className="bg-slate-100/90 dark:bg-white/[0.04] border border-slate-200 dark:border-white/10 rounded-2xl p-4 sm:p-5 space-y-3">
        <label
          htmlFor="brandColorPicker"
          className="block text-sm font-bold text-slate-800 dark:text-white"
        >
          Màu chủ đạo
        </label>

        <div className="flex flex-wrap items-center gap-3">
          <input
            id="brandColorPicker"
            type="color"
            value={form.brandColor}
            onChange={(e) => set("brandColor", e.target.value)}
            className="w-12 h-10 rounded-xl border border-slate-300 dark:border-white/15 cursor-pointer p-1 bg-white dark:bg-cinema-800"
          />

          <input
            value={form.brandColor}
            onChange={(e) => set("brandColor", e.target.value)}
            aria-label="Mã màu chủ đạo"
            className="w-28 px-3 py-2 text-sm bg-white dark:bg-cinema-800 border border-slate-300 dark:border-white/15 rounded-xl text-slate-800 dark:text-white
            focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-400 transition-all uppercase font-mono font-bold"
          />

          <div className="flex flex-wrap gap-2">
            {[
              "#E50914",
              "#6366F1",
              "#10B981",
              "#F59E0B",
              "#EC4899",
              "#8B5CF6",
            ].map((c) => (
              <button
                key={c}
                type="button"
                onClick={() => set("brandColor", c)}
                aria-label={`Chọn màu ${c}`}
                className={cn(
                  "w-8 h-8 rounded-full border-2 transition-all hover:scale-110 shadow-sm",
                  form.brandColor === c ? "border-slate-900 dark:border-white scale-110" : "border-transparent"
                )}
                style={{ backgroundColor: c }}
              />
            ))}
          </div>
        </div>
      </div>

      {/* ── 3. Màu phụ ── */}
      <div className="bg-slate-100/90 dark:bg-white/[0.04] border border-slate-200 dark:border-white/10 rounded-2xl p-4 sm:p-5 space-y-3">
        <label
          htmlFor="accentColorPicker"
          className="block text-sm font-bold text-slate-800 dark:text-white"
        >
          Màu phụ
        </label>

        <div className="flex flex-wrap items-center gap-3">
          <input
            id="accentColorPicker"
            type="color"
            value={form.accentColor}
            onChange={(e) => set("accentColor", e.target.value)}
            className="w-12 h-10 rounded-xl border border-slate-300 dark:border-white/15 cursor-pointer p-1 bg-white dark:bg-cinema-800"
          />

          <input
            value={form.accentColor}
            onChange={(e) => set("accentColor", e.target.value)}
            aria-label="Mã màu phụ"
            className="w-28 px-3 py-2 text-sm bg-white dark:bg-cinema-800 border border-slate-300 dark:border-white/15 rounded-xl text-slate-800 dark:text-white
              focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-400 transition-all uppercase font-mono font-bold"
          />

          <div className="flex flex-wrap gap-2">
            {[
              "#F5A623",
              "#EF4444",
              "#3B82F6",
              "#14B8A6",
              "#F97316",
              "#A855F7",
            ].map((c) => (
              <button
                key={c}
                type="button"
                onClick={() => set("accentColor", c)}
                aria-label={`Chọn màu phụ ${c}`}
                className={cn(
                  "w-8 h-8 rounded-full border-2 transition-all hover:scale-110 shadow-sm",
                  form.accentColor === c ? "border-slate-900 dark:border-white scale-110" : "border-transparent"
                )}
                style={{ backgroundColor: c }}
              />
            ))}
          </div>
        </div>
      </div>

      {/* ── 4. Toggles (Hiệu ứng & Gọn gàng) ── */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
        <button
          type="button"
          onClick={() => set("animations", !form.animations)}
          aria-pressed={form.animations}
          className={cn(
            "flex items-center justify-between p-4 rounded-xl border transition-all text-left",
            form.animations
              ? "bg-brand-500/10 border-brand-500/40 text-slate-900 dark:text-white"
              : "bg-slate-100 dark:bg-white/[0.04] border-slate-200 dark:border-white/10 text-slate-700 dark:text-cinema-300"
          )}
        >
          <div>
            <span className="text-sm font-bold block">Hiệu ứng chuyển động</span>
            <span className="text-xs text-slate-500 dark:text-cinema-400">Micro-animations & transitions</span>
          </div>
          <span
            className={cn(
              "w-11 h-6 rounded-full transition-all flex items-center p-0.5",
              form.animations ? "bg-brand-500 justify-end" : "bg-slate-300 dark:bg-slate-700 justify-start"
            )}
          >
            <span className="w-5 h-5 bg-white rounded-full shadow-md" />
          </span>
        </button>

        <button
          type="button"
          onClick={() => set("compact", !form.compact)}
          aria-pressed={form.compact}
          className={cn(
            "flex items-center justify-between p-4 rounded-xl border transition-all text-left",
            form.compact
              ? "bg-brand-500/10 border-brand-500/40 text-slate-900 dark:text-white"
              : "bg-slate-100 dark:bg-white/[0.04] border-slate-200 dark:border-white/10 text-slate-700 dark:text-cinema-300"
          )}
        >
          <div>
            <span className="text-sm font-bold block">Chế độ gọn gàng</span>
            <span className="text-xs text-slate-500 dark:text-cinema-400">Tối ưu khoảng cách hiển thị</span>
          </div>
          <span
            className={cn(
              "w-11 h-6 rounded-full transition-all flex items-center p-0.5",
              form.compact ? "bg-brand-500 justify-end" : "bg-slate-300 dark:bg-slate-700 justify-start"
            )}
          >
            <span className="w-5 h-5 bg-white rounded-full shadow-md" />
          </span>
        </button>
      </div>

      {/* ── 5. Preview trực quan ── */}
      <div className="bg-slate-100/90 dark:bg-white/[0.04] border border-slate-200 dark:border-white/10 rounded-2xl p-4 sm:p-5">
        <p className="text-xs text-slate-500 dark:text-cinema-400 uppercase tracking-wider font-bold mb-3 flex items-center gap-1.5">
          <Sparkles className="w-3.5 h-3.5 text-gold-400" /> Xem trước màu sắc
        </p>
        <div className="flex flex-wrap items-center gap-3">
          <div
            className="h-10 px-5 rounded-xl flex items-center justify-center text-white text-sm font-bold shadow-md"
            style={{ backgroundColor: form.brandColor }}
          >
            Nút chính
          </div>
          <div
            className="h-10 px-5 rounded-xl flex items-center justify-center text-cinema-950 text-sm font-bold shadow-md"
            style={{ backgroundColor: form.accentColor }}
          >
            Điểm nhấn
          </div>
          <div
            className="h-10 flex-1 min-w-[140px] rounded-xl border-2 bg-white dark:bg-cinema-900 flex items-center px-3.5 text-sm text-slate-700 dark:text-cinema-200"
            style={{ borderColor: form.brandColor }}
          >
            Khung viền mẫu
          </div>
        </div>
      </div>

      {/* ── 6. Actions ── */}
      <div className="flex flex-wrap gap-3 pt-2">
        <button
          type="button"
          onClick={handleReset}
          className="flex items-center gap-2 px-5 py-2.5 text-sm font-semibold text-slate-700 dark:text-cinema-200 bg-white dark:bg-white/5 border border-slate-300 dark:border-white/10
            rounded-xl hover:bg-slate-100 dark:hover:bg-white/10 transition-all focus:outline-none"
        >
          <RotateCcw className="w-4 h-4" /> Khôi phục mặc định
        </button>
        <button
          type="button"
          onClick={handleSave}
          className="flex items-center gap-2 px-6 py-2.5 text-sm font-bold text-white rounded-xl shadow-lg transition-all
            hover:opacity-95 focus:outline-none"
          style={{ backgroundColor: form.brandColor }}
        >
          <Save className="w-4 h-4" /> Lưu giao diện
        </button>
      </div>
    </div>
  );
}
