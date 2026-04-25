import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";
import { format, formatDistanceToNow, parseISO } from "date-fns";
import { vi } from "date-fns/locale";

// ── Tailwind class merge ──────────────────────
export function cn(...inputs) {
  return twMerge(clsx(inputs));
}

// ── Currency ─────────────────────────────────
export function formatCurrency(amount) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(amount);
}

export function formatCompactCurrency(amount) {
  if (amount >= 1_000_000_000) return `${(amount / 1_000_000_000).toFixed(1)}B`;
  if (amount >= 1_000_000) return `${(amount / 1_000_000).toFixed(1)}M`;
  if (amount >= 1_000) return `${(amount / 1_000).toFixed(0)}K`;
  return formatCurrency(amount);
}

// ── Date / Time ───────────────────────────────
export function formatDate(date, fmt = "dd/MM/yyyy") {
  if (!date) return "";
  try {
    const d = typeof date === "string" ? parseISO(date) : date;
    return format(d, fmt, { locale: vi });
  } catch (err) {
    console.error("formatDate error:", err);
    return String(date);
  }
}

export function formatDateTime(dateStr) {
  return formatDate(dateStr, "HH:mm - dd/MM/yyyy");
}

export function formatShowtime(dateStr) {
  return formatDate(dateStr, "HH:mm");
}

export function formatRelative(dateStr) {
  try {
    return formatDistanceToNow(parseISO(dateStr), {
      addSuffix: true,
      locale: vi,
    });
  } catch {
    return dateStr;
  }
}

export function getDayLabel(dateStr) {
  return formatDate(dateStr, "EEEE, dd/MM");
}

export function getNext7Days() {
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() + i);
    return d.toISOString().split("T")[0];
  });
}

// ── Booking Status ────────────────────────────
export const BOOKING_STATUS = {
  PENDING: { label: "Chờ thanh toán", color: "gold" },
  PAID: { label: "Đã thanh toán", color: "green" },
  CHECKED_IN: { label: "Đã vào rạp", color: "blue" },
  CANCELLED: { label: "Đã hủy", color: "red" },
  EXPIRED: { label: "Hết hạn", color: "gray" },
};

export function getStatusBadge(status) {
  return BOOKING_STATUS[status] ?? { label: status, color: "gray" };
}

// ── Movie Rated ───────────────────────────────
export const RATED_COLORS = {
  P: "bg-green-500/20 text-green-400 border-green-500/30",
  K: "bg-blue-500/20 text-blue-400 border-blue-500/30",
  C13: "bg-yellow-500/20 text-yellow-400 border-yellow-500/30",
  C16: "bg-orange-500/20 text-orange-400 border-orange-500/30",
  C18: "bg-red-500/20 text-red-400 border-red-500/30",
};

export function getRatedColor(rated) {
  return (
    RATED_COLORS[rated] ?? "bg-gray-500/20 text-gray-400 border-gray-500/30"
  );
}

// ── Screen Type ───────────────────────────────
export const SCREEN_TYPE_COLORS = {
  "2D": "text-cinema-200",
  "3D": "text-blue-400",
  IMAX: "text-gold-400",
  "4DX": "text-brand-400",
};

// ── String utils ──────────────────────────────
export function truncate(str, max) {
  return str.length > max ? str.slice(0, max) + "..." : str;
}

export function slugify(str) {
  return str
    .toLowerCase()
    .replace(/[àáạảãâầấậẩẫăằắặẳẵ]/g, "a")
    .replace(/[èéẹẻẽêềếệểễ]/g, "e")
    .replace(/[ìíịỉĩ]/g, "i")
    .replace(/[òóọỏõôồốộổỗơờớợởỡ]/g, "o")
    .replace(/[ùúụủũưừứựửữ]/g, "u")
    .replace(/[ỳýỵỷỹ]/g, "y")
    .replace(/đ/g, "d")
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .trim();
}

// ── Image ─────────────────────────────────────
export function getImageUrl(path) {
  if (!path) return "/placeholder-movie.jpg";
  if (path.startsWith("http")) return path;
  return `${import.meta.env.VITE_CDN_URL ?? ""}${path}`;
}

// ── Local storage ─────────────────────────────
export const storage = {
  get: (key) => {
    try {
      return JSON.parse(localStorage.getItem(key) ?? "null");
    } catch {
      return null;
    }
  },
  set: (key, value) => {
    localStorage.setItem(key, JSON.stringify(value));
  },
  remove: (key) => localStorage.removeItem(key),
};

// ── Voucher Utility ───────────────────────────
export function calculateActualDiscount(cartTotal, voucher) {
  if (!voucher || !cartTotal || cartTotal <= 0) return 0;
  
  // Kiểm tra minOrder
  if (voucher.minOrder && cartTotal < voucher.minOrder) return 0;

  // Tính discount
  let discountAmount = 0;
  if (voucher.discountType === 'PERCENTAGE') {
    discountAmount = (cartTotal * voucher.discountValue) / 100;
  } else {
    // FIXED_AMOUNT
    discountAmount = voucher.discountValue;
  }
  
  // Giám sát Max Discount
  if (voucher.maxDiscount && voucher.maxDiscount > 0 && discountAmount > voucher.maxDiscount) {
    discountAmount = voucher.maxDiscount;
  }
  
  return discountAmount;
}
