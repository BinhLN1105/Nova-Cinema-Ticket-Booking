import { Outlet, Link, useLocation } from "react-router-dom";
import { motion } from "framer-motion";
import { useState } from "react";
import {
  LayoutDashboard,
  Film,
  Building2,
  CalendarCheck,
  Users,
  BarChart3,
  Settings,
  LogOut,
  ChevronLeft,
  ChevronRight,
  Film as FilmIcon,
  Bell,
  Search,
  Clock,
  Tags,
  QrCode,
} from "lucide-react";
import { useAuthStore } from "@/stores/authStore";
import { useAuth } from "@/hooks";
import { cn } from "@/utils";

const NAV_ITEMS = [
  { href: "/admin/dashboard", icon: LayoutDashboard, label: "Dashboard" },
  { href: "/admin/movies", icon: Film, label: "Phim" },
  { href: "/admin/cinemas", icon: Building2, label: "Rạp chiếu" },
  { href: "/admin/showtimes", icon: Clock, label: "Suất chiếu" },
  { href: "/admin/pricing-rules", icon: Tags, label: "Quy tắc giá" },
  { href: "/admin/bookings", icon: CalendarCheck, label: "Đặt vé" },
  { href: "/admin/checkin", icon: QrCode, label: "Soát vé (QR)" },
  { href: "/admin/users", icon: Users, label: "Người dùng" },
  { href: "/admin/reports", icon: BarChart3, label: "Báo cáo" },
  { href: "/admin/settings", icon: Settings, label: "Cài đặt" },
];

export function AdminLayout() {
  const location = useLocation();
  const { user } = useAuthStore();
  const { logout } = useAuth();
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div
      className="flex h-screen bg-gray-50 overflow-hidden"
      data-portal="admin"
    >
      {/* ── Sidebar ── */}
      <motion.aside
        animate={{ width: collapsed ? 72 : 240 }}
        transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
        className="flex-shrink-0 bg-slate-900 flex flex-col overflow-hidden relative"
      >
        {/* Logo */}
        <div
          className={cn(
            "flex items-center gap-3 px-5 py-5 border-b border-white/5",
            collapsed && "justify-center px-0",
          )}
        >
          <div
            className="w-9 h-9 flex-shrink-0 rounded-xl bg-brand-500
            flex items-center justify-center shadow-glow-red"
          >
            <FilmIcon className="w-5 h-5 text-white" />
          </div>
          <motion.span
            animate={{
              opacity: collapsed ? 0 : 1,
              width: collapsed ? 0 : "auto",
            }}
            transition={{ duration: 0.2 }}
            className="font-display text-lg font-bold text-white overflow-hidden whitespace-nowrap"
          >
            Nova<span className="text-brand-500">Admin</span>
          </motion.span>
        </div>

        {/* Nav */}
        <nav className="flex-1 py-4 px-3 space-y-1 overflow-y-auto scrollbar-hide">
          {NAV_ITEMS.map(({ href, icon: Icon, label }) => {
            const active = location.pathname.startsWith(href);
            return (
              <Link
                key={href}
                to={href}
                title={collapsed ? label : undefined}
                className={cn(
                  "flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200",
                  collapsed ? "justify-center" : "",
                  active
                    ? "bg-brand-500/10 text-brand-400 border-l-2 border-brand-500"
                    : "text-slate-400 hover:bg-white/5 hover:text-white",
                )}
              >
                <Icon className="w-5 h-5 flex-shrink-0" />
                {!collapsed && (
                  <span className="text-sm font-medium">{label}</span>
                )}
                {active && !collapsed && (
                  <motion.div
                    layoutId="admin-indicator"
                    className="ml-auto w-1.5 h-1.5 rounded-full bg-brand-500"
                  />
                )}
              </Link>
            );
          })}
        </nav>

        {/* User & collapse */}
        <div className="border-t border-white/5 p-3 space-y-2">
          {!collapsed && (
            <div className="flex items-center gap-3 px-3 py-2 rounded-xl bg-white/4">
              <div
                className="w-8 h-8 rounded-full bg-brand-500/30 flex items-center
                justify-center text-brand-300 text-sm font-bold flex-shrink-0"
              >
                {user?.fullName?.[0] ?? "A"}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-white truncate">
                  {user?.fullName}
                </p>
                <p className="text-xs text-slate-500 truncate">{user?.email}</p>
              </div>
            </div>
          )}
          <button
            onClick={logout}
            className={cn(
              "w-full flex items-center gap-3 px-3 py-2.5 rounded-xl",
              "text-slate-400 hover:bg-red-500/10 hover:text-red-400 transition-all text-sm",
              collapsed && "justify-center",
            )}
          >
            <LogOut className="w-4 h-4 flex-shrink-0" />
            {!collapsed && "Đăng xuất"}
          </button>
        </div>

        {/* Collapse button */}
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="absolute -right-3 top-[72px] w-6 h-6 rounded-full
            bg-slate-700 border border-slate-600 flex items-center justify-center
            text-slate-400 hover:text-white hover:bg-slate-600 transition-all
            shadow-lg z-10"
        >
          {collapsed ? (
            <ChevronRight className="w-3 h-3" />
          ) : (
            <ChevronLeft className="w-3 h-3" />
          )}
        </button>
      </motion.aside>

      {/* ── Main ── */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Topbar */}
        <header
          className="h-16 bg-white border-b border-gray-200 flex items-center
          px-6 gap-4 flex-shrink-0 shadow-sm"
        >
          <div className="flex-1 flex items-center gap-3">
            <div className="relative max-w-xs w-full hidden md:block">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="Tìm kiếm..."
                className="w-full pl-9 pr-4 py-2 text-sm bg-gray-50 border border-gray-200
                  rounded-xl text-gray-700 placeholder-gray-400 focus:outline-none
                  focus:border-brand-400 focus:bg-white transition-all"
              />
            </div>
          </div>
          <div className="flex items-center gap-3">
            <button
              className="relative p-2 rounded-xl text-gray-500 hover:bg-gray-100
              hover:text-gray-700 transition-all"
            >
              <Bell className="w-5 h-5" />
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-brand-500 rounded-full" />
            </button>
            <div
              className="w-8 h-8 rounded-full bg-brand-500 flex items-center
              justify-center text-white text-sm font-bold"
            >
              {user?.fullName?.[0] ?? "A"}
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto bg-gray-50">
          <motion.div
            key={location.pathname}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3 }}
            className="p-6 max-w-screen-2xl mx-auto"
          >
            <Outlet />
          </motion.div>
        </main>
      </div>
    </div>
  );
}
