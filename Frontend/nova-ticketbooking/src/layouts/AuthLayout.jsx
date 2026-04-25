import { Outlet, Link } from "react-router-dom";
import { motion } from "framer-motion";
import { Film } from "lucide-react";

export function AuthLayout() {
  return (
    <div className="min-h-screen bg-cinema-900 flex">
      {/* Left — decorative */}
      <div className="hidden lg:flex lg:w-1/2 relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-brand-900/60 via-cinema-800 to-cinema-900" />
        <div
          className="absolute inset-0"
          style={{
            backgroundImage: "url(/hero-bg.jpg)",
            backgroundSize: "cover",
            backgroundPosition: "center",
            opacity: 0.2,
          }}
        />
        <div className="noise-overlay absolute inset-0" />
        {/* Glowing orbs */}
        <div
          className="absolute top-1/4 left-1/4 w-64 h-64 rounded-full
          bg-brand-500/20 blur-[80px] animate-float"
        />

        <div
          className="absolute bottom-1/4 right-1/4 w-48 h-48 rounded-full
          bg-gold-400/10 blur-[60px] animate-float"
          style={{ animationDelay: "2s" }}
        />
        <div className="relative z-10 flex flex-col justify-end p-12">
          <Link to="/" className="flex items-center gap-3 mb-auto mt-8">
            <div className="w-10 h-10 rounded-xl bg-brand-500 flex items-center justify-center">
              <Film className="w-6 h-6 text-white" />
            </div>
            <span className="font-display text-2xl font-bold text-white">
              Nova<span className="text-brand-400">Ticket</span>
            </span>
          </Link>
          <blockquote className="max-w-sm">
            <p className="font-display text-3xl font-bold text-white leading-tight mb-4">
              "Mỗi bộ phim là một hành trình cảm xúc."
            </p>
            <p className="text-cinema-300 text-sm">
              Đặt vé dễ dàng, trải nghiệm điện ảnh đỉnh cao.
            </p>
          </blockquote>
        </div>
      </div>

      {/* Right — form */}
      <div className="flex-1 flex items-center justify-center px-6 py-12">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="w-full max-w-sm"
        >
          {/* Mobile logo */}
          <Link to="/" className="flex items-center gap-2.5 mb-10 lg:hidden">
            <div className="w-9 h-9 rounded-xl bg-brand-500 flex items-center justify-center">
              <Film className="w-5 h-5 text-white" />
            </div>
            <span className="font-display text-xl font-bold">
              Nova<span className="text-brand-500">Ticket</span>
            </span>
          </Link>
          <Outlet />
        </motion.div>
      </div>
    </div>
  );
}
