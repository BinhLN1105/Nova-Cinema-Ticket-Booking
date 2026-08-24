import { useRef, useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion, useScroll, useTransform, AnimatePresence } from 'framer-motion'
import { Play, Star, Clock, ChevronRight, Ticket, Zap, Popcorn, CreditCard, ShieldCheck, Gift, Sparkles } from 'lucide-react'
import { useMovies } from '@/hooks'
import { cn, getRatedColor } from '@/utils'
import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import { promotionApi, movieApi } from '@/api/endpoints'
import { useAuthStore } from '@/stores/authStore'
import PropTypes from 'prop-types'

// ── Skeleton ──────────────────────────────────
function MovieSkeleton() {
  return (
    <div className="skeleton rounded-2xl" style={{ aspectRatio: '2/3' }} />
  )
}

// ── Movie Card ────────────────────────────────
function MovieCard({ movie, index }) {
  const navigate = useNavigate()

  return (
    <motion.button
      type="button"
      initial={{ opacity: 0, y: 32 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{
        duration: 0.5,
        delay: index * 0.08,
        ease: [0.4, 0, 0.2, 1],
      }}
      className="movie-card group"
      onClick={() => navigate(`/movies/${movie.id}`)}
    >
      {/* Poster */}
      <img
        src={movie.posterUrl || "/placeholder-movie.jpg"}
        alt={movie.title}
        className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
        loading="lazy"
      />

      {/* Gradient overlay */}
      <div className="movie-card-overlay" />

      {/* Rated badge */}
      <div
        className={cn(
          "absolute top-3 left-3 badge text-xs",
          getRatedColor(movie.rated)
        )}
      >
        {movie.rated}
      </div>

      {/* Info on hover */}
      <div className="movie-card-info">
        <h3 className="font-display font-bold text-white text-base leading-tight mb-2 line-clamp-2">
          {movie.title}
        </h3>

        <div className="flex items-center gap-3 text-xs text-cinema-200">
          <span className="flex items-center gap-1">
            <Star className="w-3 h-3 text-gold-400 fill-current" />
            {movie.avgRating.toFixed(1)}
          </span>

          <span className="flex items-center gap-1">
            <Clock className="w-3 h-3" />
            {movie.duration} phút
          </span>
        </div>

        <div className="mt-3 flex flex-wrap gap-1">
          {movie.genres?.slice(0, 2).map((g) => (
            <span
              key={g.id}
              className="text-xs px-2 py-0.5 rounded-full bg-white/10 text-cinema-100"
            >
              {g.name}
            </span>
          ))}
        </div>

        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation()
            navigate(`/booking/showtime/${movie.id}`)
          }}
          className="mt-3 w-full btn-primary text-xs py-2"
        >
          <Ticket className="w-3 h-3" />
          Đặt vé
        </button>
      </div>
    </motion.button>
  )
}
MovieCard.propTypes = {
  movie: PropTypes.shape({
    id: PropTypes.any,
    posterUrl: PropTypes.string,
    title: PropTypes.string,
    rated: PropTypes.string,
    avgRating: PropTypes.number,
    duration: PropTypes.number,
    genres: PropTypes.arrayOf(
      PropTypes.shape({
        id: PropTypes.any,
        name: PropTypes.string,
      })
    ),
  }).isRequired,
  index: PropTypes.number.isRequired,
}

// ── Section ───────────────────────────────────
function SectionHeader({ title, subtitle, href }) {
  return (
    <div className="flex items-end justify-between mb-8">
      <div>
        <h2 className="font-display text-3xl font-bold text-slate-900 dark:text-white mb-1">{title}</h2>
        {subtitle && <p className="text-slate-600 dark:text-cinema-300 text-sm">{subtitle}</p>}
      </div>
      <Link to={href}
        className="flex items-center gap-1.5 text-sm text-brand-600 dark:text-brand-400
        hover:text-brand-500 dark:hover:text-brand-300 transition-colors group font-semibold">
        Xem tất cả
        <ChevronRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
      </Link>
    </div>
  )
}

SectionHeader.propTypes = {
  title: PropTypes.string.isRequired,
  subtitle: PropTypes.string,
  href: PropTypes.string.isRequired,
}
// ── Hero ──────────────────────────────────────
function Hero({ featured }) {
  const { t } = useTranslation()
  const ref = useRef(null)
  const { scrollYProgress } = useScroll({ target: ref })
  const y = useTransform(scrollYProgress, [0, 1], [0, 120])
  const opacity = useTransform(scrollYProgress, [0, 0.8], [1, 0])

  return (
    <section ref={ref} className="relative min-h-[92vh] flex items-end overflow-hidden bg-cinema-950">
      {/* Parallax bg */}
      <motion.div style={{ y }} className="absolute inset-0">
        <div className="absolute inset-0 bg-gradient-to-br
          from-cinema-950 via-cinema-900/90 to-cinema-950" />
        {featured && (
          <img
            src={featured.backdropUrl || featured.posterUrl}
            alt=""
            className="w-full h-full object-cover opacity-25"
            style={{ objectPosition: 'center 20%' }}
          />
        )}
        {/* Glowing orbs */}
        <div className="absolute top-1/3 right-1/4 w-[500px] h-[500px] rounded-full
          bg-brand-500/10 blur-[100px]" />
        <div className="absolute bottom-1/3 left-1/4 w-[300px] h-[300px] rounded-full
          bg-gold-400/8 blur-[80px]" />
        <div className="noise-overlay absolute inset-0" />
      </motion.div>

      {/* Bottom fade */}
      <div className="absolute bottom-0 left-0 right-0 h-48
        bg-gradient-to-t from-slate-50 dark:from-cinema-900 to-transparent" />

      {/* Content */}
      <motion.div style={{ opacity }}
        className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 pb-20 pt-32 w-full">
        <motion.div
          initial={{ opacity: 0, y: 40 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: [0.4,0,0.2,1] }}>

          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full
            bg-white/10 backdrop-blur-md border border-white/15 text-sm text-white/90 mb-6 font-medium shadow-sm">
            <Zap className="w-4 h-4 text-gold-400" />
            Phim nổi bật tuần này
          </div>

          <h1 className="font-display text-5xl sm:text-6xl md:text-7xl font-bold
            text-white leading-tight max-w-2xl mb-6 drop-shadow-md">
            {t('home.hero_title_1', 'Trải nghiệm điện ảnh')}{' '}
            <span className="text-gradient-red">{t('home.hero_title_2', 'đỉnh cao')}</span>
          </h1>

          <p className="text-slate-200 text-lg max-w-lg mb-10 leading-relaxed drop-shadow-sm font-medium">
            {t('home.hero_subtitle', 'Hàng trăm bộ phim hấp dẫn, đặt vé nhanh chóng, chọn chỗ ngồi yêu thích — mọi lúc, mọi nơi.')}
          </p>

          <div className="flex flex-wrap gap-4">
            <Link to="/movies" className="btn-primary text-base px-7 py-3.5 shadow-lg">
              <Play className="w-5 h-5 fill-current" />
              {t("home.view_schedule", "Khám phá phim")}
            </Link>
            <Link to="/movies?status=NOW_SHOWING"
              className="btn-ghost text-base px-7 py-3.5 bg-white/10 border-white/20 text-white hover:bg-white/20">
              <Ticket className="w-5 h-5" />
              {t("home.book_now", "Đặt vé ngay")}
            </Link>
          </div>

          {/* Stats */}
          <div className="flex flex-wrap gap-10 mt-16">
            {[
              { value: '500+', label: 'Bộ phim' },
              { value: '50+',  label: 'Rạp chiếu' },
              { value: '2M+',  label: 'Vé đã bán' },
            ].map(({ value, label }, i) => (
              <motion.div key={label}
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.4 + i * 0.1, duration: 0.5 }}>
                <div className="font-display text-3xl font-bold text-white drop-shadow-sm">{value}</div>
                <div className="text-slate-300 text-sm mt-1 font-medium">{label}</div>
              </motion.div>
            ))}
          </div>
        </motion.div>
      </motion.div>
    </section>
  )
}
Hero.propTypes = {
  featured: PropTypes.shape({
    backdropUrl: PropTypes.string,
    posterUrl: PropTypes.string,
  }),
}

// ── Promotions Carousel ───────────────────────
function PromotionsCarousel() {
  const { data: response, isLoading, error } = useQuery({
    queryKey: ['promotions', 'active'],
    queryFn: () => promotionApi.getActive()
  });

  if (error) {
    console.error("Promotion Error:", error);
  }

  const promotions = response || [];
  
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isHovered, setIsHovered] = useState(false);

  // Auto-play logic
  useEffect(() => {
    if (promotions.length <= 1 || isHovered) return;
    
    const timer = setInterval(() => {
      setCurrentIndex((current) => (current + 1) % promotions.length);
    }, 4000); // Change banner every 4 seconds

    return () => clearInterval(timer);
  }, [promotions.length, isHovered]);

  if (isLoading || promotions.length === 0) return null;

  return (
    <section className="max-w-7xl mx-auto px-4 sm:px-6 py-8">
      <button
  type="button"
  className="relative overflow-hidden rounded-3xl aspect-[3/1] md:aspect-[4/1] bg-surface-lowest group cursor-pointer w-full"
  onMouseEnter={() => setIsHovered(true)}
  onMouseLeave={() => setIsHovered(false)}
  onClick={() => {
    if (promotions[currentIndex].targetUrl) {
      globalThis.location.href = promotions[currentIndex].targetUrl
    }
  }}
>
        <AnimatePresence mode="wait">
          <motion.img
            key={currentIndex}
            src={promotions[currentIndex].imageUrl}
            alt={promotions[currentIndex].title}
            className="w-full h-full object-cover"
            initial={{ opacity: 0, x: 50 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -50 }}
            transition={{ duration: 0.5, ease: "easeInOut" }}
          />
        </AnimatePresence>

        {/* Navigation Dots */}
        {promotions.length > 1 && (
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2 z-10">
            {promotions.map((promotion, i) => (
              <button
                 key={promotion.id}
                onClick={() => setCurrentIndex(i)}
                className={cn(
                  "w-2 h-2 rounded-full transition-all duration-300",
                  currentIndex === i ? "w-6 bg-brand-500" : "bg-white/50 hover:bg-white/80"
                )}
                aria-label={`Go to slide ${i + 1}`}
              />
            ))}
          </div>
        )}
      </button>
    </section>
  )
}

const TIER_CONFIG = {
  DIAMOND: {
    name: "DIAMOND MEMBER",
    gradient: "from-[#0c2237] via-[#081829] to-[#040e17]",
    border: "border-cyan-400/50",
    shadow: "shadow-[0_0_30px_rgba(34,211,238,0.25)]",
    titleColor: "text-cyan-300",
    starColor: "text-cyan-400",
    rate: "10%",
    discount: "Giảm 30K/vé (10 vé/tháng)",
    heroBorder: "border-cyan-500/30",
    heroGlow: "from-cyan-500/15 via-brand-500/5 to-transparent",
  },
  GOLD: {
    name: "GOLD MEMBER",
    gradient: "from-[#2a1d0f] via-[#1a140a] to-[#0d0a05]",
    border: "border-amber-500/50",
    shadow: "shadow-[0_0_30px_rgba(245,197,24,0.2)]",
    titleColor: "text-[#F5C518]",
    starColor: "text-[#F5C518]",
    rate: "7%",
    discount: "Giảm 20K/vé (5 vé/tháng)",
    heroBorder: "border-amber-500/30",
    heroGlow: "from-amber-500/15 via-brand-500/5 to-transparent",
  },
  SILVER: {
    name: "SILVER MEMBER",
    gradient: "from-[#1e2530] via-[#151b24] to-[#0d1017]",
    border: "border-slate-300/40",
    shadow: "shadow-[0_0_30px_rgba(203,213,225,0.15)]",
    titleColor: "text-slate-200",
    starColor: "text-slate-300",
    rate: "5%",
    discount: "Giảm 10K/vé (2 vé/tháng)",
    heroBorder: "border-slate-400/30",
    heroGlow: "from-slate-400/15 via-brand-500/5 to-transparent",
  },
  BRONZE: {
    name: "BRONZE MEMBER",
    gradient: "from-[#281c15] via-[#1d120d] to-[#0e0906]",
    border: "border-amber-700/50",
    shadow: "shadow-[0_0_25px_rgba(180,83,9,0.15)]",
    titleColor: "text-amber-500",
    starColor: "text-amber-600",
    rate: "3%",
    discount: "Tích điểm đổi vé & quà",
    heroBorder: "border-amber-700/30",
    heroGlow: "from-amber-700/15 via-brand-500/5 to-transparent",
  },
  GUEST: {
    name: "NOVAPASS GUEST",
    gradient: "from-[#152233] via-[#0d1724] to-[#070b12]",
    border: "border-white/15",
    shadow: "shadow-[0_0_20px_rgba(255,255,255,0.05)]",
    titleColor: "text-cinema-200",
    starColor: "text-[#F5C518]",
    rate: "5 - 10%",
    discount: "Đăng ký nhận voucher 50K",
    heroBorder: "border-white/10",
    heroGlow: "from-brand-500/10 via-gold-400/5 to-transparent",
  }
}

// ── Main Page ─────────────────────────────────
export default function HomePage() {
  const { t } = useTranslation()
  const { nowShowing, comingSoon } = useMovies()
  const { user, isAuthenticated } = useAuthStore()

  const currentTierKey = isAuthenticated ? (user?.membershipTier || 'BRONZE') : 'GUEST'
  const currentTier = TIER_CONFIG[currentTierKey] || TIER_CONFIG.BRONZE

  const { data: featuredMoviesResponse } = useQuery({
    queryKey: ['featured-movies', 'WEB'],
    queryFn: () => movieApi.getFeaturedMovies("WEB")
  })
  
  const featured = featuredMoviesResponse?.[0] || nowShowing.data?.content?.[0]

  useEffect(() => {
  }, [featured, nowShowing.status]);

  if (nowShowing.isLoading) {
    return (
      <div className="pt-24 space-y-16 bg-cinema-900 min-h-screen">
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
           <div className="h-[60vh] rounded-3xl skeleton" />
        </div>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-16">
          <div className="h-8 w-48 skeleton mb-8" />
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
           {[
             "skeleton1",
             "skeleton2",
             "skeleton3",
             "skeleton4",
             "skeleton5",
             "skeleton6",
            ].map((id) => (
              <MovieSkeleton key={id} />
            ))}
          </div>
        </div> 
      </div>
    )
  }

  return (
    <div className="bg-slate-50 dark:bg-cinema-900 min-h-screen text-slate-900 dark:text-white transition-colors duration-300">
      <Hero featured={featured} />

      {/* Promotions Banner AutoPlay Carousel */}
      <PromotionsCarousel />

      {/* Now Showing */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 py-16">
        <SectionHeader
          title={t("home.now_showing", "Đang chiếu")}
          subtitle=""
          href="/movies?status=NOW_SHOWING"
        />
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 xl:grid-cols-6 gap-4">
          {nowShowing.isLoading
            ? [
                "skeleton1",
                "skeleton2",
                "skeleton3",
                "skeleton4",
                "skeleton5",
                "skeleton6",
              ].map((id) => (
                <MovieSkeleton key={id} />
              ))
            : nowShowing.data?.content?.slice(0, 6).map((movie, i) => (
               <MovieCard key={movie.id} movie={movie} index={i} />
              ))
          }
        </div>
      </section>

      {/* Divider */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6">
        <div className="h-px bg-gradient-to-r from-transparent via-slate-200 dark:via-white/8 to-transparent" />
      </div>

      {/* Coming Soon */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 py-16">
        <SectionHeader
          title="Sắp ra mắt"
          subtitle="Những tựa phim được mong chờ nhất"
          href="/movies?status=COMING_SOON"
        />
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 xl:grid-cols-6 gap-4">
          {comingSoon.isLoading
            ? [
                "skeleton1",
                "skeleton2",
                "skeleton3",
                "skeleton4",
                "skeleton5",
                "skeleton6",
              ].map((id) => (
                <MovieSkeleton key={id} />
              ))
            : comingSoon.data?.content?.slice(0, 6).map((movie, i) => (
                <MovieCard key={movie.id} movie={movie} index={i} />
              ))
          }
        </div>
      </section>

      {/* Experience & Special Privileges Section - Handcrafted Cinema Bento Grid */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 pb-24">
        <motion.div
          initial={{ opacity: 0, y: 28 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.7, ease: [0.4, 0, 0.2, 1] }}
          className="relative"
        >
          {/* Header */}
          <div className="flex flex-col md:flex-row md:items-end justify-between mb-10 gap-4 border-b border-slate-200 dark:border-white/5 pb-6">
            <div>
              <span className="font-mono text-xs tracking-[0.25em] text-amber-600 dark:text-[#F5C518] uppercase font-bold">
                ── [ SPECIAL PRIVILEGES ] ──
              </span>
              <h2 className="font-display text-3xl sm:text-4xl lg:text-5xl font-bold text-slate-900 dark:text-white tracking-tight mt-2.5">
                Đặc Quyền Điện Ảnh <span className="text-slate-900 dark:text-white">Nova<span className="text-brand-500">Ticket</span></span>
              </h2>
            </div>
            <p className="text-slate-600 dark:text-cinema-300 text-sm max-w-md font-body leading-relaxed md:text-right font-medium">
              Hơn cả một tấm vé xem phim — Khám phá hệ sinh thái tiện ích thông minh và đặc quyền hội viên thiết kế riêng cho bạn.
            </p>
          </div>

          {/* Asymmetrical Cinema Bento Grid */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* HERO CARD 1: CinePoint VIP Membership (Span 2 columns on desktop) */}
            <div className={cn(
              "md:col-span-2 relative overflow-hidden rounded-3xl bg-white dark:bg-gradient-to-br dark:from-[#121e2f] dark:via-[#0d1724] dark:to-[#070b12] p-8 sm:p-10 shadow-xl dark:shadow-2xl flex flex-col justify-between group border border-slate-200 dark:border-white/10 transition-all duration-300",
              currentTier.heroBorder
            )}>
              {/* Background ambient lighting */}
              <div className={cn("absolute top-0 right-0 w-96 h-96 bg-gradient-to-bl blur-3xl pointer-events-none opacity-40 dark:opacity-100", currentTier.heroGlow)} />
              <div className="absolute -bottom-10 -right-10 w-60 h-60 bg-gold-400/5 rounded-full blur-2xl pointer-events-none" />

              <div className="relative z-10 flex flex-col sm:flex-row sm:items-start justify-between gap-6 mb-8">
                <div className="max-w-md">
                  <div className="flex items-center gap-2 mb-3">
                    <span className="font-mono text-xs text-amber-600 dark:text-[#F5C518] font-extrabold tracking-wider">FEATURED REEL</span>
                    <span className="w-1.5 h-1.5 rounded-full bg-amber-500 dark:bg-[#F5C518] animate-pulse" />
                    <span className="text-xs font-mono font-bold text-amber-700 dark:text-amber-300 tracking-wider">01 / CINEPOINT VIP</span>
                  </div>
                  <h3 className="font-display text-2xl sm:text-3xl font-bold text-slate-900 dark:text-white mb-3 group-hover:text-amber-600 dark:group-hover:text-[#F5C518] transition-colors leading-tight">
                    Hội Viên VIP & Tích Lũy Điểm CinePoint
                  </h3>
                  <p className="text-slate-600 dark:text-cinema-100 text-sm leading-relaxed font-body">
                    {isAuthenticated ? (
                      <>
                        Chào <strong className="text-slate-900 dark:text-white font-bold">{user?.fullName || 'Hội viên'}</strong>! Hạng thẻ <strong className={cn("font-bold", currentTier.titleColor)}>{user?.membershipTier || 'BRONZE'}</strong> của bạn nhận hoàn <strong className="text-slate-900 dark:text-white font-bold">{currentTier.rate} điểm CinePoint</strong> cho mỗi đơn vé. Thỏa sức <strong className="text-slate-900 dark:text-white font-bold">đổi vé 2D/3D miễn phí</strong> và nhận <strong className="text-slate-900 dark:text-white font-bold">quà bắp nước sinh nhật</strong> độc quyền.
                      </>
                    ) : (
                      <>
                        Hội viên NovaTicket nhận hoàn từ <strong className="text-slate-900 dark:text-white font-bold">5% đến 10% điểm CinePoint</strong> mỗi đơn hàng. Thỏa sức <strong className="text-slate-900 dark:text-white font-bold">đổi vé 2D/3D miễn phí</strong>, nhận <strong className="text-slate-900 dark:text-white font-bold">quà bắp nước sinh nhật</strong> và đặc quyền vào rạp qua <strong className="text-slate-900 dark:text-white font-bold">lối ưu tiên</strong>.
                      </>
                    )}
                  </p>
                  {!isAuthenticated ? (
                    <Link to="/auth/login" className="inline-flex items-center gap-1.5 text-xs font-bold text-amber-600 dark:text-[#F5C518] hover:underline mt-3">
                      <span>Đăng nhập để xem thẻ & điểm CinePoint của bạn</span>
                      <ChevronRight className="w-3.5 h-3.5" />
                    </Link>
                  ) : (
                    <div className="text-xs text-slate-600 dark:text-cinema-200 mt-3 flex items-center gap-2 font-mono font-semibold">
                      <span className="text-slate-500 dark:text-cinema-300">QUYỀN LỢI HIỆN TẠI:</span>
                      <span className={cn("font-bold px-2 py-0.5 rounded bg-amber-500/10 dark:bg-white/10 border border-amber-500/20 dark:border-white/15", currentTier.titleColor)}>
                        {currentTier.discount}
                      </span>
                    </div>
                  )}
                </div>

                {/* Stylized Dynamic VIP Gold Cinema Card Illustration */}
                <div className={cn(
                  "shrink-0 w-60 sm:w-64 h-38 rounded-2xl bg-gradient-to-br p-4 shadow-xl flex flex-col justify-between transform group-hover:rotate-1 group-hover:scale-105 transition-all duration-500 relative overflow-hidden border",
                  currentTier.gradient,
                  currentTier.border,
                  currentTier.shadow
                )}>
                  <div className="flex items-center justify-between relative z-10">
                    <span className={cn("font-display text-sm font-bold tracking-widest", currentTier.titleColor)}>
                      {isAuthenticated ? "NOVAPASS VIP" : "NOVAPASS MEMBER"}
                    </span>
                    <Gift className={cn("w-5 h-5", currentTier.starColor)} />
                  </div>
                  <div className="relative z-10 flex items-center justify-between">
                    <div>
                      <div className="text-xs text-cinema-100 uppercase font-mono font-bold tracking-widest">
                        {isAuthenticated ? user?.fullName : "CHƯA ĐĂNG NHẬP"}
                      </div>
                      <div className={cn("text-sm sm:text-base font-extrabold tracking-wider flex items-center gap-1.5", currentTier.titleColor)}>
                        {currentTier.name} <span className="text-xs">★</span>
                      </div>
                    </div>
                    {isAuthenticated && (
                      <div className="text-right">
                        <div className="text-[10px] text-cinema-200 font-mono font-bold">ĐIỂM THƯỞNG</div>
                        <div className="text-xs sm:text-sm font-extrabold text-white font-mono">
                          {user?.rewardPoints || user?.cinePoints || 0} <span className="text-[10px] text-brand-300 font-bold">CP</span>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Bottom Key Badges */}
              <div className="relative z-10 grid grid-cols-3 gap-3 pt-6 border-t border-slate-200 dark:border-white/10">
                <div className="bg-slate-50 dark:bg-white/[0.04] p-3 rounded-xl border border-slate-200 dark:border-white/5 text-left">
                  <div className="font-mono text-base font-extrabold text-amber-600 dark:text-[#F5C518]">100%</div>
                  <div className="text-xs text-slate-600 dark:text-cinema-100 font-semibold mt-0.5">Đổi vé miễn phí</div>
                </div>
                <div className="bg-slate-50 dark:bg-white/[0.04] p-3 rounded-xl border border-slate-200 dark:border-white/5 text-left">
                  <div className="font-mono text-base font-extrabold text-slate-900 dark:text-white">{currentTier.rate}</div>
                  <div className="text-xs text-slate-600 dark:text-cinema-100 font-semibold mt-0.5">Tích lũy điểm mỗi đơn</div>
                </div>
                <div className="bg-slate-50 dark:bg-white/[0.04] p-3 rounded-xl border border-slate-200 dark:border-white/5 text-left">
                  <div className="font-mono text-base font-extrabold text-amber-600 dark:text-amber-400">BIRTHDAY</div>
                  <div className="text-xs text-slate-600 dark:text-cinema-100 font-semibold mt-0.5">Quà tặng sinh nhật</div>
                </div>
              </div>
            </div>

            {/* CARD 2: Fast Booking 60s */}
            <div className="relative overflow-hidden rounded-3xl bg-white dark:bg-[#0e1722]/90 border border-slate-200 dark:border-white/10 p-7 sm:p-8 flex flex-col justify-between hover:border-slate-300 dark:hover:border-white/20 shadow-lg dark:shadow-none transition-all duration-300 group hover:-translate-y-1">
              <div>
                <div className="flex items-center justify-between mb-4">
                  <span className="font-mono text-xs text-amber-600 dark:text-amber-300 font-extrabold tracking-wider">02 / SPEED TRACK</span>
                  <Zap className="w-6 h-6 text-amber-500 dark:text-[#F5C518] drop-shadow-[0_0_10px_rgba(245,197,24,0.4)]" />
                </div>
                <h3 className="font-display text-xl font-bold text-slate-900 dark:text-white mb-2.5 group-hover:text-amber-600 dark:group-hover:text-[#F5C518] transition-colors">
                  Đặt Vé Siêu Tốc 60s
                </h3>
                <p className="text-slate-600 dark:text-cinema-200 text-xs leading-relaxed font-body">
                  Sơ đồ phòng chiếu <strong className="text-slate-900 dark:text-white">tương tác thời gian thực</strong>. Chọn chỗ ngồi trung tâm ưng ý, thanh toán 1-chạm và nhận vé điện tử <strong className="text-slate-900 dark:text-white">mã QR vào rạp tức thì</strong>.
                </p>
              </div>
              <div className="mt-6 pt-3.5 border-t border-slate-100 dark:border-white/5 bg-slate-50 dark:bg-white/[0.04] p-3 rounded-xl border border-slate-200 dark:border-white/5 font-mono text-xs text-slate-700 dark:text-cinema-100 font-semibold flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                Giữ ghế 10 phút · Mã QR vào rạp
              </div>
            </div>

            {/* CARD 3: Gourmet Combo Bắp Nước */}
            <div className="relative overflow-hidden rounded-3xl bg-white dark:bg-[#0e1722]/90 border border-slate-200 dark:border-white/10 p-7 sm:p-8 flex flex-col justify-between hover:border-slate-300 dark:hover:border-white/20 shadow-lg dark:shadow-none transition-all duration-300 group hover:-translate-y-1">
              <div>
                <div className="flex items-center justify-between mb-4">
                  <span className="font-mono text-xs text-red-600 dark:text-red-300 font-extrabold tracking-wider">03 / GOURMET COMBO</span>
                  <Popcorn className="w-6 h-6 text-brand-500 dark:text-[#ff6b6b] drop-shadow-[0_0_10px_rgba(229,9,20,0.4)]" />
                </div>
                <div className="flex items-center gap-2 mb-2">
                  <h3 className="font-display text-xl font-bold text-slate-900 dark:text-white group-hover:text-brand-500 dark:group-hover:text-[#ff6b6b] transition-colors">
                    Combo Bắp Nước Online
                  </h3>
                  <span className="px-2 py-0.5 rounded-full bg-red-500/15 text-brand-600 dark:text-red-400 border border-red-500/30 text-[10px] font-mono font-bold">
                    -30%
                  </span>
                </div>
                <p className="text-slate-600 dark:text-cinema-200 text-xs leading-relaxed font-body">
                  Tiết kiệm đến <strong className="text-slate-900 dark:text-white">30%</strong> khi đặt trước Solo Combo hoặc Couple Combo kèm vé. Thưởng thức bắp rang bơ phô mai & caramel nóng hổi tại <strong className="text-slate-900 dark:text-white">quầy ưu tiên</strong>.
                </p>
              </div>
              <div className="mt-6 pt-3.5 border-t border-slate-100 dark:border-white/5 bg-slate-50 dark:bg-white/[0.04] p-3 rounded-xl border border-slate-200 dark:border-white/5 font-mono text-xs text-slate-700 dark:text-cinema-100 font-semibold flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-red-500" />
                Tiết kiệm 30% · Nhận tại quầy ưu tiên
              </div>
            </div>

            {/* CARD 4: Bảo Mật & Hoàn Vé (Span 2 columns on desktop) */}
            <div className="md:col-span-2 relative overflow-hidden rounded-3xl bg-white dark:bg-[#0e1722]/90 border border-slate-200 dark:border-white/10 p-7 sm:p-8 flex flex-col sm:flex-row sm:items-center justify-between gap-6 hover:border-slate-300 dark:hover:border-white/20 shadow-lg dark:shadow-none transition-all duration-300 group hover:-translate-y-1">
              <div className="max-w-md">
                <div className="flex items-center gap-2 mb-2">
                  <span className="font-mono text-xs text-emerald-600 dark:text-emerald-300 font-extrabold tracking-wider">04 / SECURE & FLEXIBLE</span>
                  <ShieldCheck className="w-5 h-5 text-emerald-500 dark:text-emerald-400 drop-shadow-[0_0_10px_rgba(52,211,153,0.4)]" />
                </div>
                <h3 className="font-display text-xl sm:text-2xl font-bold text-slate-900 dark:text-white mb-2 group-hover:text-emerald-600 dark:group-hover:text-emerald-400 transition-colors">
                  Thanh Toán Bảo Mật Đa Tầng & Đổi Trả Linh Hoạt
                </h3>
                <p className="text-slate-600 dark:text-cinema-200 text-xs leading-relaxed font-body">
                  Mã hóa chuẩn <strong className="text-slate-900 dark:text-white">PCI-DSS</strong> với VNPay, thẻ ATM nội địa, Visa/Mastercard và QR Pay. Hỗ trợ <strong className="text-slate-900 dark:text-white">hủy vé trước giờ chiếu</strong> linh hoạt & minh bạch.
                </p>
              </div>
              <div className="shrink-0 flex items-center gap-2.5 flex-wrap">
                <div className="px-3.5 py-2 rounded-xl bg-slate-50 dark:bg-white/[0.06] border border-slate-200 dark:border-white/10 font-mono text-xs text-slate-700 dark:text-cinema-100 font-semibold shadow-sm">
                  🛡️ 256-BIT SSL
                </div>
                <div className="px-3.5 py-2 rounded-xl bg-slate-50 dark:bg-white/[0.06] border border-slate-200 dark:border-white/10 font-mono text-xs text-slate-700 dark:text-cinema-100 font-semibold shadow-sm">
                  💳 VNPAY · VISA · ATM
                </div>
                <div className="px-3.5 py-2 rounded-xl bg-emerald-500/10 border border-emerald-500/30 font-mono text-xs text-emerald-600 dark:text-emerald-400 font-bold shadow-sm">
                  ✓ INSTANT REFUND
                </div>
              </div>
            </div>
          </div>
        </motion.div>
      </section>
    </div>
  )
}
