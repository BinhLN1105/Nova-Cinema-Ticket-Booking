import { useRef, useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion, useScroll, useTransform, AnimatePresence } from 'framer-motion'
import { Play, Star, Clock, ChevronRight, Ticket, Zap } from 'lucide-react'
import { useMovies } from '@/hooks'
import { cn, formatDate, getRatedColor } from '@/utils'
import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import { promotionApi, movieApi } from '@/api/endpoints'

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
    <motion.div
      initial={{ opacity: 0, y: 32 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: index * 0.08, ease: [0.4,0,0.2,1] }}
      className="movie-card group"
      onClick={() => navigate(`/movies/${movie.id}`)}>

      {/* Poster */}
      <img
        src={movie.posterUrl || '/placeholder-movie.jpg'}
        alt={movie.title}
        className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
        loading="lazy"
      />

      {/* Gradient overlay */}
      <div className="movie-card-overlay" />

      {/* Rated badge */}
      <div className={cn(
        'absolute top-3 left-3 badge text-xs',
        getRatedColor(movie.rated)
      )}>
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
          {movie.genres?.slice(0, 2).map(g => (
            <span key={g.id} className="text-xs px-2 py-0.5 rounded-full
              bg-white/10 text-cinema-100">
              {g.name}
            </span>
          ))}
        </div>
        <button
          onClick={(e) => {
            e.stopPropagation()
            navigate(`/booking/showtime/${movie.id}`)
          }}
          className="mt-3 w-full btn-primary text-xs py-2">
          <Ticket className="w-3 h-3" /> Đặt vé
        </button>
      </div>
    </motion.div>
  )
}

// ── Section ───────────────────────────────────
function SectionHeader({ title, subtitle, href }) {
  return (
    <div className="flex items-end justify-between mb-8">
      <div>
        <h2 className="font-display text-3xl font-bold text-white mb-1">{title}</h2>
        {subtitle && <p className="text-cinema-300 text-sm">{subtitle}</p>}
      </div>
      <Link to={href}
        className="flex items-center gap-1.5 text-sm text-brand-400
        hover:text-brand-300 transition-colors group font-medium">
        Xem tất cả
        <ChevronRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
      </Link>
    </div>
  )
}

// ── Hero ──────────────────────────────────────
function Hero({ featured }) {
  const { t } = useTranslation()
  const ref = useRef(null)
  const { scrollYProgress } = useScroll({ target: ref })
  const y = useTransform(scrollYProgress, [0, 1], [0, 120])
  const opacity = useTransform(scrollYProgress, [0, 0.8], [1, 0])

  return (
    <section ref={ref} className="relative min-h-[92vh] flex items-end overflow-hidden bg-cinema-900">
      {/* Parallax bg */}
      <motion.div style={{ y }} className="absolute inset-0">
        <div className="absolute inset-0 bg-gradient-to-br
          from-cinema-900 via-cinema-800/80 to-cinema-900" />
        {featured && (
          <img
            src={featured.backdropUrl || featured.posterUrl}
            alt=""
            className="w-full h-full object-cover opacity-20"
            style={{ objectPosition: 'center 20%' }}
          />
        )}
        {/* Glowing orbs */}
        <div className="absolute top-1/3 right-1/4 w-[500px] h-[500px] rounded-full
          bg-brand-500/8 blur-[100px]" />
        <div className="absolute bottom-1/3 left-1/4 w-[300px] h-[300px] rounded-full
          bg-gold-400/6 blur-[80px]" />
        <div className="noise-overlay absolute inset-0" />
      </motion.div>

      {/* Bottom fade */}
      <div className="absolute bottom-0 left-0 right-0 h-48
        bg-gradient-to-t from-cinema-900 to-transparent" />

      {/* Content */}
      <motion.div style={{ opacity }}
        className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 pb-20 pt-32 w-full">
        <motion.div
          initial={{ opacity: 0, y: 40 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: [0.4,0,0.2,1] }}>

          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full
            glass border border-white/10 text-sm text-cinema-200 mb-6">
            <Zap className="w-4 h-4 text-gold-400" />
            Phim nổi bật tuần này
          </div>

          <h1 className="font-display text-5xl sm:text-6xl md:text-7xl font-bold
            text-white leading-tight max-w-2xl mb-6">
            {t('home.hero_title_1', 'Trải nghiệm điện ảnh')}{' '}
            <span className="text-gradient-red">{t('home.hero_title_2', 'đỉnh cao')}</span>
          </h1>

          <p className="text-cinema-200 text-lg max-w-lg mb-10 leading-relaxed">
            {t('home.hero_subtitle', 'Hàng trăm bộ phim hấp dẫn, đặt vé nhanh chóng, chọn chỗ ngồi yêu thích — mọi lúc, mọi nơi.')}
          </p>

          <div className="flex flex-wrap gap-4">
            <Link to="/movies" className="btn-primary text-base px-7 py-3.5">
              <Play className="w-5 h-5 fill-current" />
              {t("home.view_schedule", "Khám phá phim")}
            </Link>
            <Link to="/movies?status=NOW_SHOWING"
              className="btn-ghost text-base px-7 py-3.5">
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
                <div className="font-display text-3xl font-bold text-white">{value}</div>
                <div className="text-cinema-300 text-sm mt-1">{label}</div>
              </motion.div>
            ))}
          </div>
        </motion.div>
      </motion.div>
    </section>
  )
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
      <div 
        className="relative overflow-hidden rounded-3xl aspect-[3/1] md:aspect-[4/1] bg-surface-lowest group cursor-pointer"
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
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
            onClick={() => {
              if (promotions[currentIndex].targetUrl) {
                window.location.href = promotions[currentIndex].targetUrl;
              }
            }}
          />
        </AnimatePresence>

        {/* Navigation Dots */}
        {promotions.length > 1 && (
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2 z-10">
            {promotions.map((_, i) => (
              <button
                key={i}
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
      </div>
    </section>
  )
}

// ── Main Page ─────────────────────────────────
export default function HomePage() {
  const { t } = useTranslation()
  const { nowShowing, comingSoon } = useMovies()

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
            {Array.from({ length: 6 }).map((_, i) => <MovieSkeleton key={i} />)}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-cinema-900 min-h-screen">
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
            ? Array.from({ length: 6 }).map((_, i) => <MovieSkeleton key={i} />)
            : nowShowing.data?.content?.slice(0, 6).map((movie, i) => (
                <MovieCard key={movie.id} movie={movie} index={i} />
              ))
          }
        </div>
      </section>

      {/* Divider */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6">
        <div className="h-px bg-gradient-to-r from-transparent via-white/8 to-transparent" />
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
            ? Array.from({ length: 6 }).map((_, i) => <MovieSkeleton key={i} />)
            : comingSoon.data?.content?.slice(0, 6).map((movie, i) => (
                <MovieCard key={movie.id} movie={movie} index={i} />
              ))
          }
        </div>
      </section>

      {/* CTA Banner */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 pb-16">
        <motion.div
          initial={{ opacity: 0, scale: 0.97 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5 }}
          className="relative overflow-hidden rounded-3xl
            bg-gradient-to-br from-brand-600 via-brand-500 to-brand-700 p-12 text-center">
          <div className="absolute inset-0 noise-overlay" />
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-64 h-64
            rounded-full bg-white/10 blur-[60px]" />
          <div className="relative z-10">
            <h2 className="font-display text-4xl font-bold text-white mb-4">
              Đặt vé ngay hôm nay
            </h2>
            <p className="text-red-100 mb-8 text-lg">
              Nhiều ưu đãi hấp dẫn đang chờ bạn khám phá
            </p>
            <Link to="/auth/register"
              className="inline-flex items-center gap-2 px-8 py-4 rounded-2xl
                bg-white text-brand-600 font-bold text-base hover:bg-red-50
                transition-all duration-300 shadow-lg hover:shadow-xl hover:-translate-y-0.5">
              <Ticket className="w-5 h-5" />
              Đăng ký ngay
            </Link>
          </div>
        </motion.div>
      </section>
    </div>
  )
}
