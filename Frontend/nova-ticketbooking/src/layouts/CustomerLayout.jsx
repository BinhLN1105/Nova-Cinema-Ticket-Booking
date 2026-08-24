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
import { CinematicThemeToggle } from "@/components/common/CinematicThemeToggle";
import { TopUpModal } from "@/pages/customer/profile/TopUpModal";
import { AiChatbot } from "@/components/customer/AiChatbot";
import { OfflineBanner } from "@/components/common/feedback";
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
    { href: "/cinemas", label: t("nav.cinemas", "Rạp chiếu") },
    { href: "/promotions", label: t("nav.promotions", "Khuyến mãi") },
    { href: "/gift-cards", label: t("nav.gift_cards", "Thẻ quà tặng") },
  ];

  const hasDarkHero = (location.pathname === "/" || /^\/movies\/[^/]+$/.test(location.pathname)) && !scrolled;

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-cinema-900 text-slate-900 dark:text-white transition-colors duration-300">
      {/* ── Global Offline Connectivity Banner ── */}
      <OfflineBanner />

      {/* ── Navbar ── */}
      <header
        className={cn(
          "fixed top-0 left-0 right-0 z-50 transition-all duration-500",
          scrolled || !hasDarkHero
            ? "bg-white/95 dark:bg-cinema-900/95 backdrop-blur-lg border-b border-slate-200 dark:border-white/5 py-3 shadow-sm dark:shadow-card-dark"
            : "bg-transparent py-5",
        )}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 flex items-center gap-6">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2.5 group">
            <Logo
              size="md"
              textClassName={hasDarkHero ? "text-white" : "text-slate-900 dark:text-white"}
              showText={true}
              glowEffect={true}
              interactive={true}
            />
          </Link>

          {/* Nav links (Desktop) */}
          <nav className="hidden md:flex items-center gap-1">
            {navLinks.map((link) => {
              const active =
                link.href === "/"
                  ? location.pathname === "/"
                  : location.pathname.startsWith(link.href);
              return (
                <Link
                  key={link.href}
                  to={link.href}
                  className={cn(
                    "relative px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200",
                    hasDarkHero
                      ? active
                        ? "text-white font-bold"
                        : "text-white/80 hover:text-white hover:bg-white/10"
                      : active
                        ? "text-brand-600 dark:text-white font-semibold"
                        : "text-slate-600 dark:text-cinema-200 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/10",
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
            <LanguageSwitcher
              className={hasDarkHero ? "text-white/80 hover:text-white hover:bg-white/10" : undefined}
            />

            {/* Search button */}
            <button
              onClick={() => navigate("/movies")}
              aria-label="Tìm kiếm phim"
              className={cn(
                "p-2.5 rounded-xl transition-all duration-200",
                hasDarkHero
                  ? "text-white/80 hover:text-white hover:bg-white/10"
                  : "text-slate-600 dark:text-cinema-200 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/10"
              )}
            >
              <Search className="w-5 h-5" />
            </button>

            {isAuthenticated ? (
              <>
                {/* Notifications */}
                <button
                  onClick={() => navigate('/profile?tab=notifications')}
                  aria-label="Thông báo của bạn"
                  className={cn(
                    "relative p-2.5 rounded-xl transition-all",
                    hasDarkHero
                      ? "text-white/80 hover:text-white hover:bg-white/10"
                      : "text-slate-600 dark:text-cinema-200 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/10"
                  )}
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
                  className={cn(
                    "hidden sm:flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200",
                    hasDarkHero
                      ? "text-white/80 hover:text-white hover:bg-white/10"
                      : "text-slate-600 dark:text-cinema-200 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/10"
                  )}
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
                      hasDarkHero
                        ? userMenu
                          ? "bg-white/20 border-white/30 text-white shadow-sm"
                          : "bg-white/10 border-white/15 hover:bg-white/20 hover:border-white/30 text-white"
                        : userMenu
                          ? "bg-slate-200/70 dark:bg-white/10 border-slate-300 dark:border-white/20 shadow-sm"
                          : "bg-slate-100 dark:bg-white/[0.04] border-slate-200 dark:border-white/10 hover:bg-slate-200/60 dark:hover:bg-white/[0.08] hover:border-slate-300 dark:hover:border-white/20"
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
                        "absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full border-2",
                        hasDarkHero ? "border-slate-900" : "border-white dark:border-cinema-900",
                        user?.membershipTier === "DIAMOND" ? "bg-cyan-400" :
                        user?.membershipTier === "GOLD" ? "bg-amber-400" :
                        user?.membershipTier === "SILVER" ? "bg-slate-300" : "bg-orange-400"
                      )} />
                    </div>
                    <div className="hidden sm:flex flex-col items-start justify-center text-left">
                      <span className={cn(
                        "text-sm font-semibold max-w-[120px] truncate leading-tight transition-colors",
                        hasDarkHero
                          ? "text-white group-hover:text-brand-300"
                          : "text-slate-900 dark:text-white group-hover:text-brand-600 dark:group-hover:text-brand-300"
                      )}>
                        {user?.fullName || "Khách hàng"}
                      </span>
                      <div className="flex items-center gap-1.5 mt-0.5">
                        <span
                          className={cn(
                            "text-[9px] font-extrabold px-1.5 py-[0.5px] rounded uppercase border tracking-wider",
                            user?.membershipTier === "DIAMOND"
                              ? "bg-cyan-500/20 text-cyan-600 dark:text-cyan-300 border-cyan-500/40"
                              : user?.membershipTier === "GOLD"
                                ? hasDarkHero ? "bg-amber-400/20 text-amber-300 border-amber-400/40" : "bg-amber-500/20 text-amber-700 dark:text-amber-300 border-amber-500/40"
                                : user?.membershipTier === "SILVER"
                                  ? "bg-slate-500/20 text-slate-700 dark:text-slate-300 border-slate-500/40"
                                  : "bg-orange-500/20 text-orange-700 dark:text-orange-300 border-orange-500/40",
                          )}
                        >
                          {user?.membershipTier || "MEMBER"}
                        </span>
                        <span className={cn(
                          "text-[11px] font-bold flex items-center gap-1",
                          hasDarkHero ? "text-amber-300" : "text-amber-600 dark:text-amber-400"
                        )}>
                          {user?.rewardPoints || 0} CP
                        </span>
                      </div>
                    </div>
                    <ChevronDown
                      className={cn(
                        "w-4 h-4 transition-transform duration-200",
                        hasDarkHero
                          ? "text-white/80 group-hover:text-white"
                          : "text-slate-500 dark:text-cinema-300 group-hover:text-slate-900 dark:group-hover:text-white",
                        userMenu && (hasDarkHero ? "rotate-180 text-white" : "rotate-180 text-slate-900 dark:text-white"),
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
                          bg-white dark:bg-[#13141F] border border-slate-200 dark:border-white/15 shadow-[0_20px_50px_rgba(0,0,0,0.15)] dark:shadow-[0_25px_60px_rgba(0,0,0,0.95)]
                          overflow-hidden z-50 p-2.5 divide-y divide-slate-100 dark:divide-white/[0.08]"
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
                            <p className="text-sm font-bold text-slate-900 dark:text-white truncate leading-tight">
                              {user?.fullName || "Khách hàng"}
                            </p>
                            <p className="text-xs text-slate-500 dark:text-cinema-300 truncate mt-0.5">
                              {user?.email || "Thành viên NovaTicket"}
                            </p>
                          </div>
                        </div>

                        {/* CinePoint Card */}
                        <div className="py-2.5 px-1">
                          <div className="bg-slate-100 dark:bg-cinema-800/90 border border-slate-200 dark:border-white/10 rounded-xl p-3 flex items-center justify-between gap-2 shadow-inner">
                            <div>
                              <div className="text-[11px] font-medium text-slate-500 dark:text-slate-400">
                                Điểm CinePoint
                              </div>
                              <div className="flex items-baseline gap-1 mt-0.5">
                                <span className="text-xl font-extrabold text-amber-500 dark:text-amber-400">{user?.rewardPoints || 0}</span>
                                <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">CP</span>
                              </div>
                            </div>
                            <button
                              onClick={() => { setUserMenu(false); setIsTopUpOpen(true); }}
                              className="flex items-center gap-1.5 text-xs bg-gradient-to-r from-brand-500 to-brand-600 hover:from-brand-600 hover:to-brand-700 text-white px-3 py-2 rounded-xl shadow-md font-bold transition-all hover:scale-105 active:scale-95 shrink-0"
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
                            className="group flex items-center gap-3 px-3 py-2 rounded-xl text-sm font-medium text-slate-700 dark:text-cinema-100 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/[0.08] transition-all"
                          >
                            <div className="w-8 h-8 rounded-lg bg-slate-100 dark:bg-white/5 border border-slate-200 dark:border-white/5 flex items-center justify-center text-slate-600 dark:text-cinema-300 group-hover:text-brand-500 group-hover:bg-brand-500/10 group-hover:border-brand-500/20 transition-colors">
                              <User className="w-4 h-4" />
                            </div>
                            <span className="flex-1">{t("nav.profile", "Tài khoản cá nhân")}</span>
                          </Link>
                          <Link
                            to="/tickets"
                            onClick={() => setUserMenu(false)}
                            className="group flex items-center gap-3 px-3 py-2 rounded-xl text-sm font-medium text-slate-700 dark:text-cinema-100 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/[0.08] transition-all"
                          >
                            <div className="w-8 h-8 rounded-lg bg-slate-100 dark:bg-white/5 border border-slate-200 dark:border-white/5 flex items-center justify-center text-slate-600 dark:text-cinema-300 group-hover:text-amber-500 group-hover:bg-amber-500/10 group-hover:border-amber-500/20 transition-colors">
                              <Ticket className="w-4 h-4" />
                            </div>
                            <span className="flex-1">{t("nav.tickets", "Vé của tôi")}</span>
                          </Link>
                          <Link
                            to="/gift-cards"
                            onClick={() => setUserMenu(false)}
                            className="group flex items-center gap-3 px-3 py-2 rounded-xl text-sm font-medium text-slate-700 dark:text-cinema-100 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/[0.08] transition-all"
                          >
                            <div className="w-8 h-8 rounded-lg bg-slate-100 dark:bg-white/5 border border-slate-200 dark:border-white/5 flex items-center justify-center text-slate-600 dark:text-cinema-300 group-hover:text-purple-500 group-hover:bg-purple-500/10 group-hover:border-purple-500/20 transition-colors">
                              <Gift className="w-4 h-4" />
                            </div>
                            <span className="flex-1">Thẻ quà tặng</span>
                          </Link>
                        </div>

                        {/* Logout */}
                        <div className="pt-1.5">
                          <button
                            onClick={() => { setUserMenu(false); logout(); }}
                            className="group w-full flex items-center gap-3 px-3 py-2 rounded-xl text-sm font-medium text-red-500 dark:text-red-400 hover:text-red-600 dark:hover:text-red-300 hover:bg-red-50 dark:hover:bg-red-500/10 transition-all text-left"
                          >
                            <div className="w-8 h-8 rounded-lg bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/20 flex items-center justify-center text-red-500 dark:text-red-400 group-hover:text-red-600 transition-colors">
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
                  className={cn(
                    "px-4 py-2 rounded-xl text-sm font-medium transition-all",
                    hasDarkHero
                      ? "text-white/90 hover:text-white hover:bg-white/10"
                      : "text-slate-700 dark:text-cinema-100 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/10"
                  )}
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
              className={cn(
                "md:hidden p-2 rounded-xl transition-all",
                hasDarkHero
                  ? "text-white hover:bg-white/10"
                  : "text-slate-700 dark:text-cinema-200 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/10"
              )}
            >
              {menuOpen ? (
                <X className="w-6 h-6" />
              ) : (
                <Menu className="w-6 h-6" />
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
      <footer className="border-t border-slate-200 dark:border-white/5 bg-slate-100 dark:bg-cinema-900 mt-24 text-slate-700 dark:text-cinema-300 transition-colors duration-300">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-12">
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-8 mb-10">
            {/* Col 1: Brand & Contact Info */}
            <div className="sm:col-span-2 md:col-span-1">
              <div className="mb-4">
                <Logo textClassName="text-slate-900 dark:text-white" />
              </div>
              <p className="text-slate-600 dark:text-cinema-300 text-sm leading-relaxed mb-4">
                Trải nghiệm điện ảnh đỉnh cao, đặt vé chuẩn quốc tế chỉ trong vài giây.
              </p>
              <div className="space-y-1.5 text-xs text-slate-500 dark:text-slate-400">
                <p>Hotline: <strong className="text-brand-500 font-bold">1900 6789</strong> (24/7)</p>
                <p>Email: <strong className="text-slate-700 dark:text-slate-200">support@novaticket.vn</strong></p>
              </div>
            </div>

            {/* Col 2: Khám phá */}
            <div>
              <h4 className="text-sm font-bold text-slate-900 dark:text-white mb-4">
                Khám phá
              </h4>
              <ul className="space-y-2.5">
                {[
                  { label: "Phim đang chiếu", href: "/movies" },
                  { label: "Cụm rạp NovaCinema", href: "/cinemas" },
                  { label: "Ưu đãi & Khuyến mãi", href: "/promotions" },
                  { label: "Thẻ quà tặng CineGift", href: "/gift-cards" },
                ].map(({ label, href }) => (
                  <li key={label}>
                    <Link
                      to={href}
                      className="text-sm text-slate-600 dark:text-cinema-300 hover:text-brand-500 dark:hover:text-white transition-colors duration-200"
                    >
                      {label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>

            {/* Col 3: Dịch vụ & Tài khoản */}
            <div>
              <h4 className="text-sm font-bold text-slate-900 dark:text-white mb-4">
                Dịch vụ & Tài khoản
              </h4>
              <ul className="space-y-2.5">
                {[
                  { label: "Vé xem phim của tôi", href: "/tickets" },
                  { label: "Thông tin cá nhân", href: "/profile" },
                  { label: "Thẻ quà & Điểm thưởng", href: "/profile?tab=giftcards" },
                  { label: "Tùy chỉnh giao diện", href: "/profile?tab=appearance" },
                ].map(({ label, href }) => (
                  <li key={label}>
                    <Link
                      to={href}
                      className="text-sm text-slate-600 dark:text-cinema-300 hover:text-brand-500 dark:hover:text-white transition-colors duration-200"
                    >
                      {label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>

            {/* Col 4: Hỗ trợ & NovaTicket */}
            <div>
              <h4 className="text-sm font-bold text-slate-900 dark:text-white mb-4">
                Hỗ trợ & Pháp lý
              </h4>
              <ul className="space-y-2.5">
                {[
                  { label: "Chính sách hoàn vé", href: "#!" },
                  { label: "Điều khoản sử dụng", href: "#!" },
                  { label: "Bảo mật thông tin", href: "#!" },
                  { label: "Quy chế hoạt động", href: "#!" },
                ].map(({ label, href }) => (
                  <li key={label}>
                    <a
                      href={href}
                      onClick={(e) => href === "#!" && e.preventDefault()}
                      className="text-sm text-slate-600 dark:text-cinema-300 hover:text-brand-500 dark:hover:text-white transition-colors duration-200"
                    >
                      {label}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          {/* Bottom copyright */}
          <div
            className="border-t border-slate-200 dark:border-white/5 pt-6 flex flex-col sm:flex-row items-center justify-between gap-4"
          >
            <p className="text-slate-500 dark:text-cinema-400 text-xs">
              © {new Date().getFullYear()} NovaTicket Vietnam. Nền tảng đặt vé xem phim trực tuyến hàng đầu.
            </p>
            <div className="flex items-center gap-4">
              <a
                href="#!"
                onClick={(e) => e.preventDefault()}
                className="text-xs text-slate-500 dark:text-cinema-400 hover:text-slate-900 dark:hover:text-white transition-colors"
              >
                Chính sách bảo mật
              </a>
              <span className="text-slate-300 dark:text-white/10">•</span>
              <a
                href="#!"
                onClick={(e) => e.preventDefault()}
                className="text-xs text-slate-500 dark:text-cinema-400 hover:text-slate-900 dark:hover:text-white transition-colors"
              >
                Điều khoản dịch vụ
              </a>
              <div className="border-l border-slate-300 dark:border-white/10 h-4 mx-1" />
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
