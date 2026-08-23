import { Outlet, Link, useLocation, useNavigate, Navigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { useState, useEffect, useRef } from "react";
import {
  Film,
  Search,
  Bell,
  User,
  Ticket,
  Menu,
  X,
  ChevronDown,
  Wallet,
  LogOut,
  Gift,
} from "lucide-react";
import { useAuthStore } from "@/stores/authStore";
import { useAuth } from "@/hooks";
import { cn } from "@/utils";
import { useTranslation } from "react-i18next";
import { LanguageSwitcher } from "@/components/common/LanguageSwitcher";
import { TopUpModal } from "@/pages/customer/profile/TopUpModal";
import { AiChatbot } from "@/components/customer/AiChatbot";
import Logo from "@/components/common/ui/Logo";

export function CustomerLayout() {
  const location = useLocation();
  const { isAuthenticated, user } = useAuthStore();
  const { logout } = useAuth();

  // Tự động chuyển hướng Admin / Staff sang đúng Portal chuyên biệt khi vào Customer Portal
  if (isAuthenticated) {
    if (user?.role === "ADMIN") {
      return <Navigate to="/admin/dashboard" replace />;
    }
    if (user?.role === "STAFF") {
      return <Navigate to="/staff/dashboard" replace />;
    }
  }
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [userMenu, setUserMenu] = useState(false);
  const [isTopUpOpen, setIsTopUpOpen] = useState(false);
  const userMenuRef = useRef(null);
  const navigate = useNavigate();
  const { t } = useTranslation();

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener("scroll", onScroll);
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  // Close menus when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target)) {
        setUserMenu(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Close menus and scroll to top on route change
  useEffect(() => {
    setMenuOpen(false);
    setUserMenu(false);
    window.scrollTo(0, 0);
  }, [location.pathname]);

  const navLinks = [
    { href: "/", label: t("nav.home", "Trang chủ") },
    { href: "/movies", label: t("nav.movies", "Phim") },
    { href: "/promotions", label: t("nav.promotions", "Khuyến mãi") },
    { href: "/gift-cards", label: t("nav.gift_cards", "Thẻ quà tặng") },
  ];

  return (
    <div className="min-h-screen bg-cinema-900 text-white">
      {/* ── Navbar ── */}
      <header
        className={cn(
          "fixed top-0 left-0 right-0 z-50 transition-all duration-500",
          scrolled
            ? "bg-cinema-900/95 backdrop-blur-lg border-b border-white/5 py-3 shadow-card-dark"
            : "bg-transparent py-5",
        )}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 flex items-center gap-6">
          <Logo />

          {/* Desktop nav */}
          <nav className="hidden md:flex items-center gap-1 ml-4">
            {navLinks.map((link) => {
              const active = location.pathname === link.href;
              return (
                <Link
                  key={link.href}
                  to={link.href}
                  className={cn(
                    "relative px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200",
                    active
                      ? "text-white font-semibold"
                      : "text-cinema-200 hover:text-white hover:bg-white/5",
                  )}
                >
                  {link.label}
                  {active && (
                    <motion.div
                      layoutId="activeNav"
                      className="absolute bottom-0 left-3 right-3 h-0.5 bg-brand-500 rounded-full"
                    />
                  )}
                </Link>
              );
            })}
          </nav>

          {/* Right actions */}
          <div className="ml-auto flex items-center gap-3">
            {/* Language Switcher */}
            <LanguageSwitcher />

            {/* Search button */}
            <button
              onClick={() => navigate("/movies")}
              className="p-2.5 rounded-xl text-cinema-200 hover:text-white
                hover:bg-white/8 transition-all duration-200"
            >
              <Search className="w-5 h-5" />
            </button>

            {isAuthenticated ? (
              <>
                {/* Notifications */}
                <button
                  onClick={() => navigate('/profile?tab=notifications')}
                  className="relative p-2.5 rounded-xl text-cinema-200
                  hover:text-white hover:bg-white/8 transition-all"
                >
                  <Bell className="w-5 h-5" />
                  <span
                    className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full
                    bg-brand-500 animate-pulse-red"
                  />
                </button>

                {/* My Tickets */}
                <Link
                  to="/tickets"
                  className="hidden sm:flex items-center gap-2 px-4 py-2 rounded-xl
                  text-sm font-medium text-cinema-200 hover:text-white hover:bg-white/8
                  transition-all duration-200"
                >
                  <Ticket className="w-4 h-4" />
                  {t("nav.tickets", "Vé của tôi")}
                </Link>

                {/* User menu */}
                <div className="relative" ref={userMenuRef}>
                  <button
                    onClick={() => setUserMenu(!userMenu)}
                    className={cn(
                      "flex items-center gap-2.5 px-3 py-1.5 rounded-2xl transition-all duration-200 group border",
                      userMenu
                        ? "bg-white/10 border-white/20 shadow-inner-glow"
                        : "bg-white/[0.04] border-white/10 hover:bg-white/[0.08] hover:border-white/20"
                    )}
                  >
                    <div className="relative">
                      <div
                        className="w-8 h-8 rounded-full bg-brand-500/20 border
                        border-brand-500/40 flex items-center justify-center overflow-hidden"
                      >
                        {user?.avatarUrl ? (
                          <img
                            src={user.avatarUrl}
                            alt=""
                            className="w-full h-full rounded-full object-cover"
                          />
                        ) : (
                          <User className="w-4 h-4 text-brand-400" />
                        )}
                      </div>
                      <span className={cn(
                        "absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full border-2 border-cinema-900",
                        user?.membershipTier === "DIAMOND" ? "bg-cyan-400" :
                        user?.membershipTier === "GOLD" ? "bg-amber-400" :
                        user?.membershipTier === "SILVER" ? "bg-slate-300" : "bg-orange-400"
                      )} />
                    </div>
                    <div className="hidden sm:flex flex-col items-start justify-center text-left">
                      <span className="text-sm font-semibold max-w-[120px] truncate leading-tight text-white group-hover:text-brand-300 transition-colors">
                        {user?.fullName || "Khách hàng"}
                      </span>
                      <div className="flex items-center gap-1.5 mt-0.5">
                        <span
                          className={cn(
                            "text-[9px] font-extrabold px-1.5 py-[0.5px] rounded uppercase border tracking-wider",
                            user?.membershipTier === "DIAMOND"
                              ? "bg-cyan-500/20 text-cyan-300 border-cyan-500/40"
                              : user?.membershipTier === "GOLD"
                                ? "bg-amber-500/20 text-amber-300 border-amber-500/40"
                                : user?.membershipTier === "SILVER"
                                  ? "bg-slate-300/20 text-slate-200 border-slate-300/40"
                                  : "bg-orange-500/20 text-orange-300 border-orange-500/40"
                          )}
                        >
                          {user?.membershipTier || "BRONZE"}
                        </span>
                        <span className="text-[11px] font-bold text-amber-400 flex items-center gap-1">
                          {user?.rewardPoints || 0} CP
                        </span>
                      </div>
                    </div>
                    <ChevronDown
                      className={cn(
                        "w-4 h-4 text-cinema-300 transition-transform duration-200 group-hover:text-white",
                        userMenu && "rotate-180 text-white",
                      )}
                    />
                  </button>

                  <AnimatePresence>
                    {userMenu && (
                      <motion.div
                        initial={{ opacity: 0, y: 8, scale: 0.96 }}
                        animate={{ opacity: 1, y: 0, scale: 1 }}
                        exit={{ opacity: 0, y: 8, scale: 0.96 }}
                        transition={{ duration: 0.2 }}
                        className="absolute right-0 top-full mt-2 w-72 rounded-2xl
                          bg-[#13141F] border border-white/15 shadow-[0_25px_60px_rgba(0,0,0,0.95),0_0_0_1px_rgba(255,255,255,0.08)]
                          overflow-hidden z-50 p-2.5 divide-y divide-white/[0.08]"
                      >
                        {/* User Header Summary */}
                        <div className="pb-3 px-2 pt-1 flex items-center gap-3">
                          <div className="w-10 h-10 rounded-full bg-brand-500/20 border border-brand-500/40 flex items-center justify-center overflow-hidden shrink-0">
                            {user?.avatarUrl ? (
                              <img src={user.avatarUrl} alt="" className="w-full h-full object-cover" />
                            ) : (
                              <User className="w-5 h-5 text-brand-400" />
                            )}
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="text-sm font-bold text-white truncate leading-tight">
                              {user?.fullName || "Khách hàng"}
                            </p>
                            <p className="text-xs text-cinema-300 truncate mt-0.5">
                              {user?.email || "Thành viên NovaTicket"}
                            </p>
                          </div>
                        </div>

                        {/* CinePoint Card */}
                        <div className="py-2.5 px-1">
                          <div className="bg-gradient-to-br from-white/[0.06] to-white/[0.02] border border-white/[0.08] rounded-xl p-3 flex items-center justify-between gap-2">
                            <div>
                              <div className="text-[11px] font-medium text-cinema-300">
                                Điểm CinePoint
                              </div>
                              <div className="flex items-baseline gap-1 mt-0.5">
                                <span className="text-xl font-extrabold text-amber-400">{user?.rewardPoints || 0}</span>
                                <span className="text-xs font-semibold text-cinema-400">CP</span>
                              </div>
                            </div>
                            <button
                              onClick={() => { setUserMenu(false); setIsTopUpOpen(true); }}
                              className="flex items-center gap-1.5 text-xs bg-gradient-to-r from-brand-500 to-brand-600 hover:from-brand-600 hover:to-brand-700 text-white px-3 py-2 rounded-xl shadow-lg shadow-brand-500/25 font-bold transition-all hover:scale-105 active:scale-95 shrink-0"
                            >
                              <Wallet className="w-3.5 h-3.5" />
                              Nạp điểm
                            </button>
                          </div>
                        </div>

                        {/* Menu Links */}
                        <div className="py-1.5 space-y-0.5">
                          <Link
                            to="/profile"
                            onClick={() => setUserMenu(false)}
                            className="group flex items-center gap-3 px-3 py-2 rounded-xl text-sm font-medium text-cinema-100 hover:text-white hover:bg-white/[0.08] transition-all"
                          >
                            <div className="w-8 h-8 rounded-lg bg-white/5 border border-white/5 flex items-center justify-center text-cinema-300 group-hover:text-brand-400 group-hover:bg-brand-500/10 group-hover:border-brand-500/20 transition-colors">
                              <User className="w-4 h-4" />
                            </div>
                            <span className="flex-1">{t("nav.profile", "Tài khoản cá nhân")}</span>
                          </Link>
                          <Link
                            to="/tickets"
                            onClick={() => setUserMenu(false)}
                            className="group flex items-center gap-3 px-3 py-2 rounded-xl text-sm font-medium text-cinema-100 hover:text-white hover:bg-white/[0.08] transition-all"
                          >
                            <div className="w-8 h-8 rounded-lg bg-white/5 border border-white/5 flex items-center justify-center text-cinema-300 group-hover:text-amber-400 group-hover:bg-amber-500/10 group-hover:border-amber-500/20 transition-colors">
                              <Ticket className="w-4 h-4" />
                            </div>
                            <span className="flex-1">{t("nav.tickets", "Vé của tôi")}</span>
                          </Link>
                          <Link
                            to="/gift-cards"
                            onClick={() => setUserMenu(false)}
                            className="group flex items-center gap-3 px-3 py-2 rounded-xl text-sm font-medium text-cinema-100 hover:text-white hover:bg-white/[0.08] transition-all"
                          >
                            <div className="w-8 h-8 rounded-lg bg-white/5 border border-white/5 flex items-center justify-center text-cinema-300 group-hover:text-purple-400 group-hover:bg-purple-500/10 group-hover:border-purple-500/20 transition-colors">
                              <Gift className="w-4 h-4" />
                            </div>
                            <span className="flex-1">Thẻ quà tặng</span>
                          </Link>
                        </div>

                        {/* Logout */}
                        <div className="pt-1.5">
                          <button
                            onClick={() => { setUserMenu(false); logout(); }}
                            className="group w-full flex items-center gap-3 px-3 py-2 rounded-xl text-sm font-medium text-red-400 hover:text-red-300 hover:bg-red-500/10 transition-all text-left"
                          >
                            <div className="w-8 h-8 rounded-lg bg-red-500/10 border border-red-500/20 flex items-center justify-center text-red-400 group-hover:text-red-300 transition-colors">
                              <LogOut className="w-4 h-4" />
                            </div>
                            <span>Đăng xuất</span>
                          </button>
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>
              </>
            ) : (
              <div className="flex items-center gap-2">
                <Link
                  to="/auth/login"
                  className="px-4 py-2 rounded-xl text-sm font-medium
                  text-cinema-100 hover:text-white hover:bg-white/8 transition-all"
                >
                  {t("nav.login", "Đăng nhập")}
                </Link>
                <Link
                  to="/auth/register"
                  className="btn-primary text-sm py-2 px-5"
                >
                  Đăng ký
                </Link>
              </div>
            )}

            {/* Mobile menu button */}
            <button
              onClick={() => setMenuOpen(!menuOpen)}
              className="md:hidden p-2.5 rounded-xl text-cinema-200
              hover:text-white hover:bg-white/8 transition-all"
            >
              {menuOpen ? (
                <X className="w-5 h-5" />
              ) : (
                <Menu className="w-5 h-5" />
              )}
            </button>
          </div>
        </div>

        {/* Mobile menu */}
        <AnimatePresence>
          {menuOpen && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: "auto", opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.3 }}
              className="md:hidden overflow-hidden border-t border-white/5 bg-cinema-900/98"
            >
              <div className="max-w-7xl mx-auto px-4 py-4 flex flex-col gap-1">
                {navLinks.map(({ href, label }) => (
                  <Link
                    key={href}
                    to={href}
                    className="px-4 py-3 rounded-xl text-cinema-100 hover:text-white
                    hover:bg-white/6 transition-colors font-medium"
                  >
                    {label}
                  </Link>
                ))}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </header>

      {/* ── Page Content ── */}
      <motion.main
        key={location.pathname}
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35, ease: [0.4, 0, 0.2, 1] }}
      >
        <Outlet />
      </motion.main>

      {/* ── Footer ── */}
      <footer className="border-t border-white/5 bg-cinema-900 mt-24">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-12">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 mb-10">
            <div className="col-span-2 md:col-span-1">
              <div className="mb-4">
                <Logo />
              </div>
              <p className="text-cinema-300 text-sm leading-relaxed">
                Trải nghiệm điện ảnh đỉnh cao, đặt vé chỉ trong vài giây.
              </p>
            </div>
            {[
              {
                title: "Khám phá",
                links: ["Phim đang chiếu", "Sắp ra mắt", "Rạp chiếu"],
              },
              {
                title: "Hỗ trợ",
                links: ["Trung tâm trợ giúp", "Chính sách hoàn vé", "Liên hệ"],
              },
              {
                title: "Công ty",
                links: ["Về chúng tôi", "Tuyển dụng", "Đối tác"],
              },
            ].map(({ title, links }) => (
              <div key={title}>
                <h4 className="text-sm font-semibold text-white mb-4">
                  {title}
                </h4>
                <ul className="space-y-2.5">
                  {links.map((l) => (
                    <li key={l}>
                      <a
                        href="#!"
                        onClick={(e) => e.preventDefault()}
                        className="text-sm text-cinema-300 hover:text-white
                        transition-colors duration-200"
                      >
                        {l}
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
          <div
            className="border-t border-white/5 pt-6 flex flex-col sm:flex-row
            items-center justify-between gap-4"
          >
            <p className="text-cinema-400 text-sm">
              © {new Date().getFullYear()} NovaTicket. All rights reserved.
            </p>
            <div className="flex items-center gap-4">
              {["Chính sách bảo mật", "Điều khoản sử dụng"].map((t) => (
                <a
                  key={t}
                  href="#!"
                  onClick={(e) => e.preventDefault()}
                  className="text-sm text-cinema-400 hover:text-white
                  transition-colors"
                >
                  {t}
                </a>
              ))}
              <div className="border-l border-white/10 h-4 mx-1" />
              <LanguageSwitcher direction="up" />
            </div>
          </div>
        </div>
      </footer>

      <TopUpModal isOpen={isTopUpOpen} onClose={() => setIsTopUpOpen(false)} />
      <AiChatbot />
    </div>
  );
}
