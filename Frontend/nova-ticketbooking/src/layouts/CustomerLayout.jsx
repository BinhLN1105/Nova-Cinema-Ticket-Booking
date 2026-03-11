import { Outlet, Link, useLocation, useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { useState, useEffect } from "react";
import {
  Film,
  Search,
  Bell,
  User,
  Ticket,
  Menu,
  X,
  ChevronDown,
} from "lucide-react";
import { useAuthStore } from "@/stores/authStore";
import { useAuth } from "@/hooks";
import { cn } from "@/utils";

export function CustomerLayout() {
  const location = useLocation();
  const { isAuthenticated, user } = useAuthStore();
  const { logout } = useAuth();
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [userMenu, setUserMenu] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener("scroll", onScroll);
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  // Close menus and scroll to top on route change
  useEffect(() => {
    setMenuOpen(false);
    setUserMenu(false);
    window.scrollTo(0, 0);
  }, [location.pathname]);

  const navLinks = [
    { href: "/", label: "Trang chủ" },
    { href: "/movies", label: "Phim" },
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
          {/* Logo */}
          <Link
            to="/"
            className="flex items-center gap-2.5 flex-shrink-0 group"
          >
            <div
              className="w-9 h-9 rounded-xl bg-gradient-to-br from-brand-500 to-brand-700
              flex items-center justify-center shadow-glow-red group-hover:scale-110 transition-transform duration-300"
            >
              <Film className="w-5 h-5 text-white" />
            </div>
            <span className="font-display text-xl font-bold">
              Nova<span className="text-brand-500">Ticket</span>
            </span>
          </Link>

          {/* Desktop nav */}
          <nav className="hidden md:flex items-center gap-1 ml-4">
            {navLinks.map(({ href, label }) => (
              <Link
                key={href}
                to={href}
                className={cn(
                  "px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200",
                  location.pathname === href
                    ? "text-white bg-white/8"
                    : "text-cinema-200 hover:text-white hover:bg-white/5",
                )}
              >
                {label}
              </Link>
            ))}
          </nav>

          <div className="flex items-center gap-2 ml-auto">
            {/* Search */}
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
                  Vé của tôi
                </Link>

                {/* User menu */}
                <div className="relative">
                  <button
                    onClick={() => setUserMenu(!userMenu)}
                    className="flex items-center gap-2 px-3 py-2 rounded-xl
                    hover:bg-white/8 transition-all duration-200 group"
                  >
                    <div
                      className="w-8 h-8 rounded-full bg-brand-500/20 border
                      border-brand-500/40 flex items-center justify-center"
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
                    <span className="hidden sm:block text-sm font-medium max-w-[100px] truncate">
                      {user?.fullName}
                    </span>
                    <ChevronDown
                      className={cn(
                        "w-4 h-4 text-cinema-300 transition-transform duration-200",
                        userMenu && "rotate-180",
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
                        className="absolute right-0 top-full mt-2 w-48 rounded-2xl
                          glass-dark border border-white/8 overflow-hidden shadow-card-float"
                      >
                        <Link
                          to="/profile"
                          className="flex items-center gap-3 px-4 py-3 text-sm
                          text-cinema-100 hover:bg-white/6 transition-colors"
                        >
                          <User className="w-4 h-4" /> Tài khoản
                        </Link>
                        <Link
                          to="/tickets"
                          className="flex items-center gap-3 px-4 py-3 text-sm
                          text-cinema-100 hover:bg-white/6 transition-colors"
                        >
                          <Ticket className="w-4 h-4" /> Vé của tôi
                        </Link>
                        <div className="border-t border-white/6 my-1" />
                        <button
                          onClick={logout}
                          className="w-full flex items-center gap-3 px-4 py-3 text-sm
                          text-brand-400 hover:bg-brand-500/10 transition-colors"
                        >
                          Đăng xuất
                        </button>
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
                  Đăng nhập
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
              <div className="flex items-center gap-2.5 mb-4">
                <div className="w-8 h-8 rounded-xl bg-brand-500 flex items-center justify-center">
                  <Film className="w-4 h-4 text-white" />
                </div>
                <span className="font-display text-lg font-bold">
                  Nova<span className="text-brand-500">Ticket</span>
                </span>
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
                        href="#"
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
              © 2024 NovaTicket. All rights reserved.
            </p>
            <div className="flex items-center gap-4">
              {["Chính sách bảo mật", "Điều khoản sử dụng"].map((t) => (
                <a
                  key={t}
                  href="#"
                  className="text-sm text-cinema-400 hover:text-white
                  transition-colors"
                >
                  {t}
                </a>
              ))}
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
