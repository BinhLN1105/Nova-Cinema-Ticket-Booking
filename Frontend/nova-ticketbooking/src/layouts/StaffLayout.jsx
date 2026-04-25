import { Outlet, Link, useLocation } from "react-router-dom";
import { motion } from "framer-motion";
import { useQuery } from "@tanstack/react-query";
import { cinemaApi } from "@/api/endpoints";
import {
  LayoutDashboard,
  CalendarCheck,
  QrCode,
  LogOut,
  Film,
} from "lucide-react";
import { useAuthStore } from "@/stores/authStore";
import { useAuth } from "@/hooks";
import { cn } from "@/utils";

const NAV_ITEMS = [
  { href: "/staff/dashboard", icon: LayoutDashboard, label: "Dashboard" },
  { href: "/staff/pos", icon: Film, label: "Quầy bán vé (POS)" },
  { href: "/staff/bookings", icon: CalendarCheck, label: "Đặt vé hôm nay" },
  { href: "/staff/checkin", icon: QrCode, label: "Check-in QR" },
];

export function StaffLayout() {
  const location = useLocation();
  const { user } = useAuthStore();
  const { logout } = useAuth();

  const { data: cinema } = useQuery({
    queryKey: ['cinema', user?.cinemaId],
    queryFn: () => cinemaApi.getById(user.cinemaId),
    enabled: !!user?.cinemaId,
    staleTime: 1000 * 60 * 60, // 1 hour
  });

  return (
    <div
      className="flex h-screen bg-gray-50 overflow-hidden"
      data-portal="staff"
    >
      <aside className="w-60 flex-shrink-0 bg-slate-900 flex flex-col">
        <div className="flex items-center gap-3 px-5 py-5 border-b border-white/5">
          <div className="w-9 h-9 rounded-xl bg-blue-500 flex items-center justify-center">
            <Film className="w-5 h-5 text-white" />
          </div>
          <span className="font-display text-lg font-bold text-white">
            Nova<span className="text-blue-400">Staff</span>
          </span>
        </div>
        <nav className="flex-1 py-4 px-3 space-y-1">
          {NAV_ITEMS.map(({ href, icon: Icon, label }) => {
            const active = location.pathname.startsWith(href);
            return (
              <Link
                key={href}
                to={href}
                className={cn(
                  "flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all text-sm font-medium",
                  active
                    ? "bg-blue-500/10 text-blue-400 border-l-2 border-blue-500"
                    : "text-slate-400 hover:bg-white/5 hover:text-white",
                )}
              >
                <Icon className="w-5 h-5" />
                {label}
              </Link>
            );
          })}
        </nav>
        <div className="border-t border-white/5 p-3 space-y-2">
          <div className="flex items-center gap-3 px-3 py-2">
            <div
              className="w-8 h-8 rounded-full bg-blue-500/30 flex items-center
              justify-center text-blue-300 text-sm font-bold"
            >
              {user?.fullName?.[0] ?? "S"}
            </div>
            <div className="min-w-0">
              <p className="text-sm font-medium text-white truncate">
                {user?.fullName}
              </p>
              <p className="text-xs text-blue-400">Nhân viên</p>
              <p className="text-[11px] text-slate-500 truncate mt-0.5" title={cinema?.name || "Tất cả rạp"}>
                Chi nhánh: <span className="text-slate-400">{cinema ? cinema.name : (user?.cinemaId ? "Đang tải dữ liệu..." : "Tất cả rạp")}</span>
              </p>
            </div>
          </div>
          <button
            onClick={logout}
            className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl
            text-slate-400 hover:bg-red-500/10 hover:text-red-400 transition-all text-sm"
          >
            <LogOut className="w-4 h-4" /> Đăng xuất
          </button>
        </div>
      </aside>
      <main className="flex-1 overflow-y-auto bg-gray-50">
        <motion.div
          key={location.pathname}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          className="p-6"
        >
          <Outlet />
        </motion.div>
      </main>
    </div>
  );
}
