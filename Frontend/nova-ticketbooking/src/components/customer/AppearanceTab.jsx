import { useState } from "react";
import { Save, RotateCcw, Palette } from "lucide-react";
import { useThemeStore } from "@/stores/themeStore";
import toast from "react-hot-toast";

export function AppearanceTab() {
  const { brandColor, accentColor, animations, compact, setTheme, resetTheme } =
    useThemeStore();

  const [form, setForm] = useState({
    brandColor,
    accentColor,
    animations,
    compact,
  });

  const set = (key, value) => setForm((f) => ({ ...f, [key]: value }));

  const handleSave = () => {
    setTheme(form);
    toast.success("Đã lưu giao diện");
  };

  const handleReset = () => {
    resetTheme();
    setForm({
      brandColor: "#E50914",
      accentColor: "#F5A623",
      animations: true,
      compact: false,
    });
    toast.success("Đã khôi phục giao diện mặc định");
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3 mb-2">
        <div
          className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-400 to-pink-500
          flex items-center justify-center"
        >
          <Palette className="w-5 h-5 text-white" />
        </div>
        <div>
          <h3 className="font-bold text-gray-900">Tuỳ chỉnh giao diện</h3>
          <p className="text-xs text-gray-400">
            Thay đổi màu sắc và hiệu ứng theo ý thích của bạn
          </p>
        </div>
      </div>

      {/* Brand color */}
      <div className="bg-gray-50 rounded-xl p-4 space-y-3">
        <label
          htmlFor="brandColorPicker"
          className="block text-sm font-semibold text-gray-700"
        >
          Màu chủ đạo
        </label>

        <div className="flex items-center gap-3">
          <input
            id="brandColorPicker"
            type="color"
            value={form.brandColor}
            onChange={(e) => set("brandColor", e.target.value)}
            className="w-12 h-10 rounded-lg border border-gray-200 cursor-pointer p-1"
          />

          <input
            value={form.brandColor}
            onChange={(e) => set("brandColor", e.target.value)}
            aria-label="Mã màu chủ đạo"
            className="w-28 px-3 py-2 text-sm bg-white border border-gray-200 rounded-xl text-gray-700
        focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-400 transition-all uppercase"
          />

          <div className="flex gap-1.5">
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
                className="w-7 h-7 rounded-full border-2 transition-transform hover:scale-110"
                style={{
                  backgroundColor: c,
                  borderColor:
                    form.brandColor === c ? "#1f2937" : "transparent",
                }}
              />
            ))}
          </div>
        </div>
      </div>

      {/* Accent color */}
      <div className="bg-gray-50 rounded-xl p-4 space-y-3">
        <label
          htmlFor="accentColorPicker"
          className="block text-sm font-semibold text-gray-700"
        >
          Màu phụ
        </label>

        <div className="flex items-center gap-3">
          <input
            id="accentColorPicker"
            type="color"
            value={form.accentColor}
            onChange={(e) => set("accentColor", e.target.value)}
            className="w-12 h-10 rounded-lg border border-gray-200 cursor-pointer p-1"
          />

          <input
            value={form.accentColor}
            onChange={(e) => set("accentColor", e.target.value)}
            aria-label="Mã màu phụ"
            className="w-28 px-3 py-2 text-sm bg-white border border-gray-200 rounded-xl text-gray-700
              focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-400 transition-all uppercase"
          />

          <div className="flex gap-1.5">
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
                className="w-7 h-7 rounded-full border-2 transition-transform hover:scale-110"
                style={{
                  backgroundColor: c,
                  borderColor:
                    form.accentColor === c ? "#1f2937" : "transparent",
                }}
              />
            ))}
          </div>
        </div>
      </div>

      {/* Toggles */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <button
          type="button"
          onClick={() => set("animations", !form.animations)}
          aria-pressed={form.animations}
          className={`flex items-center justify-between p-4 rounded-xl border transition-all ${
            form.animations
              ? "bg-brand-50 border-brand-300"
              : "bg-gray-50 border-gray-200"
          }`}
        >
          <span className="text-sm font-semibold text-gray-700">
            Hiệu ứng động
          </span>
          <span
            className={`w-10 h-5 rounded-full transition-all ${
              form.animations ? "bg-brand-500" : "bg-gray-300"
            }`}
          >
            <span
              className={`block w-4 h-4 bg-white rounded-full mt-0.5 transition-transform ${
                form.animations ? "translate-x-5 ml-0.5" : "translate-x-0.5"
              }`}
            />
          </span>
        </button>

        <button
          type="button"
          onClick={() => set("compact", !form.compact)}
          aria-pressed={form.compact}
          className={`flex items-center justify-between p-4 rounded-xl border transition-all ${
            form.compact
              ? "bg-brand-50 border-brand-300"
              : "bg-gray-50 border-gray-200"
          }`}
        >
          <span className="text-sm font-semibold text-gray-700">
            Chế độ gọn
          </span>
          <span
            className={`w-10 h-5 rounded-full transition-all ${
              form.compact ? "bg-brand-500" : "bg-gray-300"
            }`}
          >
            <span
              className={`block w-4 h-4 bg-white rounded-full mt-0.5 transition-transform ${
                form.compact ? "translate-x-5 ml-0.5" : "translate-x-0.5"
              }`}
            />
          </span>
        </button>
      </div>

      {/* Preview */}
      <div className="bg-gray-50 rounded-xl p-4">
        <p className="text-xs text-gray-400 uppercase tracking-wider font-semibold mb-3">
          Xem trước
        </p>
        <div className="flex items-center gap-3">
          <div
            className="h-10 w-24 rounded-xl flex items-center justify-center text-white text-sm font-bold shadow-sm"
            style={{ backgroundColor: form.brandColor }}
          >
            Button
          </div>
          <div
            className="h-10 w-24 rounded-xl flex items-center justify-center text-white text-sm font-bold shadow-sm"
            style={{ backgroundColor: form.accentColor }}
          >
            Accent
          </div>
          <div
            className="h-10 flex-1 rounded-xl border-2 flex items-center px-3 text-sm text-gray-500"
            style={{ borderColor: form.brandColor }}
          >
            Input border
          </div>
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-3 pt-2">
        <button
          onClick={handleReset}
          className="flex items-center gap-2 px-4 py-2.5 text-sm font-semibold text-gray-600 bg-white border border-gray-200
            rounded-xl hover:bg-gray-50 transition-all focus:outline-none focus:ring-2 focus:ring-gray-200"
        >
          <RotateCcw className="w-4 h-4" /> Khôi phục mặc định
        </button>
        <button
          onClick={handleSave}
          className="flex items-center gap-2 px-6 py-2.5 text-sm font-bold text-white rounded-xl shadow-sm transition-all
            hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-offset-2"
          style={{ backgroundColor: form.brandColor }}
        >
          <Save className="w-4 h-4" /> Lưu giao diện
        </button>
      </div>
    </div>
  );
}
