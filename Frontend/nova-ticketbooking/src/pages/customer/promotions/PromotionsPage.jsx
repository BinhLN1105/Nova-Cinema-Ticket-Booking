import { useState, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { 
  Tag, Ticket, Copy, CheckCircle2, Clock, ArrowRight, Percent, 
  DollarSign, Gift, Search, Sparkles, Film, ChevronDown, 
  ChevronUp, HelpCircle, Flame, Calendar
} from 'lucide-react'
import { promotionApi, voucherApi } from '@/api/endpoints'
import { formatDate, formatCurrency, cn } from '@/utils'
import toast from 'react-hot-toast'

// ─── Default fallback styles for promotions ──
const DEFAULT_PROMO_STYLES = [
  { gradient: 'from-amber-600/80 via-red-700/60 to-cinema-900', tag: 'Nổi bật', tagColor: 'bg-amber-400 text-amber-950' },
  { gradient: 'from-blue-600/80 via-indigo-700/60 to-cinema-900', tag: 'Mới nhất', tagColor: 'bg-blue-400 text-blue-950' },
  { gradient: 'from-purple-600/80 via-pink-700/60 to-cinema-900', tag: 'Hot Deal', tagColor: 'bg-purple-400 text-purple-950' },
]

const APPLICABLE_LABELS = {
  ALL:           { label: 'Tất cả vé',        color: 'bg-blue-500/15 text-blue-400 border-blue-500/30' },
  FIRST_BOOKING: { label: 'Đặt vé lần đầu',  color: 'bg-purple-500/15 text-purple-400 border-purple-500/30' },
  MOVIE:         { label: 'Phim cụ thể',      color: 'bg-amber-500/15 text-amber-400 border-amber-500/30' },
}

// ─── Compact Cinema Ticket Card ──────────────
function CompactVoucherCard({ voucher, index }) {
  const [copied, setCopied] = useState(false)
  const isPercentage = voucher.discountType === 'PERCENTAGE'
  const applicable = APPLICABLE_LABELS[voucher.applicableTo] || APPLICABLE_LABELS.ALL

  const handleCopy = () => {
    navigator.clipboard.writeText(voucher.code)
    setCopied(true)
    toast.success(`Đã copy mã: ${voucher.code}`)
    setTimeout(() => setCopied(false), 2500)
  }

  // Calculate remaining time
  const isExpired = voucher.endDate && new Date(voucher.endDate) < new Date()

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: (index % 6) * 0.05 }}
      className="group relative flex flex-col sm:flex-row rounded-2xl overflow-hidden border border-slate-200 dark:border-white/10 bg-white dark:bg-cinema-800/90 hover:border-brand-500/40 shadow-sm hover:shadow-xl transition-all duration-300"
    >
      {/* Left Voucher Badge Section */}
      <div className={cn(
        'sm:w-36 p-4 flex sm:flex-col items-center justify-between sm:justify-center text-center relative border-b sm:border-b-0 sm:border-r border-dashed border-slate-200 dark:border-white/10 flex-shrink-0',
        isPercentage 
          ? 'bg-gradient-to-br from-rose-500/15 via-rose-500/5 to-transparent dark:from-rose-500/20 dark:via-rose-950/40 dark:to-transparent' 
          : 'bg-gradient-to-br from-amber-500/15 via-amber-500/5 to-transparent dark:from-amber-500/20 dark:via-yellow-950/40 dark:to-transparent'
      )}>
        {/* Ticket notch holes (Clean cutout without outer border) */}
        <div className="hidden sm:block absolute -top-3 -right-3 w-6 h-6 rounded-full bg-slate-50 dark:bg-cinema-900 z-10" />
        <div className="hidden sm:block absolute -bottom-3 -right-3 w-6 h-6 rounded-full bg-slate-50 dark:bg-cinema-900 z-10" />

        <div className="flex sm:flex-col items-center gap-2 sm:gap-1.5">
          <div className={cn(
            'w-11 h-11 rounded-2xl flex items-center justify-center shadow-inner',
            isPercentage 
              ? 'bg-rose-500/20 text-rose-500 dark:text-rose-400 border border-rose-500/30' 
              : 'bg-amber-500/20 text-amber-500 dark:text-amber-400 border border-amber-500/30'
          )}>
            {isPercentage ? <Percent className="w-5 h-5 font-bold" /> : <DollarSign className="w-5 h-5 font-bold" />}
          </div>
          <div>
            <p className={cn(
              'font-display font-black text-xl sm:text-2xl tracking-tight leading-none',
              isPercentage ? 'text-rose-600 dark:text-rose-400' : 'text-amber-600 dark:text-amber-400'
            )}>
              {isPercentage ? `${voucher.discountValue}%` : formatCurrency(voucher.discountValue)}
            </p>
            <p className="text-[10px] text-slate-500 dark:text-slate-400 mt-1 uppercase font-bold tracking-wider">
              {isPercentage ? 'Giảm tối đa' : 'Giảm trực tiếp'}
            </p>
          </div>
        </div>

        <span className={cn(
          'text-[10px] font-bold px-2.5 py-0.5 rounded-full border sm:mt-3 shadow-xs',
          applicable.color
        )}>
          {applicable.label}
        </span>
      </div>

      {/* Right Content Section */}
      <div className="flex-1 p-4 sm:p-5 flex flex-col justify-between min-w-0">
        <div>
          <div className="flex items-center justify-between gap-2 mb-2">
            <span className="font-mono text-xs font-extrabold text-slate-900 dark:text-white tracking-wider px-2.5 py-1 rounded-lg bg-slate-100 dark:bg-cinema-900 border border-slate-200 dark:border-white/10 shadow-xs">
              {voucher.code}
            </span>
            {voucher.endDate && (
              <span className="text-[11px] text-slate-500 dark:text-slate-400 flex items-center gap-1 font-medium">
                <Clock className="w-3.5 h-3.5 text-slate-400 dark:text-slate-400" />
                HSD: {formatDate(voucher.endDate)}
              </span>
            )}
          </div>

          <p className="text-slate-700 dark:text-slate-200 text-xs font-medium line-clamp-2 leading-relaxed mb-3">
            {voucher.description || `Mã giảm giá ${voucher.code} áp dụng khi mua vé xem phim tại NovaTicket.`}
          </p>

          <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500 dark:text-slate-400 mb-3">
            {voucher.minOrder > 0 && (
              <span>Đơn tối thiểu: <strong className="text-slate-900 dark:text-white font-bold">{formatCurrency(voucher.minOrder)}</strong></span>
            )}
            {voucher.maxDiscount && (
              <span>• Giảm tối đa: <strong className="text-slate-900 dark:text-white font-bold">{formatCurrency(voucher.maxDiscount)}</strong></span>
            )}
          </div>
        </div>

        {/* Action Row */}
        <div className="flex items-center justify-between gap-2 pt-3 border-t border-slate-100 dark:border-white/5 mt-auto">
          <span className="text-[11px] text-slate-500 dark:text-slate-400 font-medium">
            {voucher.usageRemaining !== null && voucher.usageRemaining !== undefined
              ? voucher.usageRemaining <= 0
                ? 'Đã hết lượt sử dụng'
                : `Còn ${voucher.usageRemaining.toLocaleString()} lượt`
              : voucher.usageLimit != null
                ? `Còn ${Math.max(0, voucher.usageLimit - (voucher.usedCount || 0)).toLocaleString()} lượt`
                : 'Không giới hạn lượt'}
          </span>

          <div className="flex items-center gap-2">
            <button
              onClick={handleCopy}
              className={cn(
                'flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all duration-200 cursor-pointer',
                copied
                  ? 'bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border border-emerald-500/40'
                  : 'bg-slate-100 dark:bg-cinema-700 hover:bg-slate-200 dark:hover:bg-cinema-600 text-slate-800 dark:text-white border border-slate-200 dark:border-white/10'
              )}
            >
              {copied ? <CheckCircle2 className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copied ? 'Đã copy' : 'Sao chép'}</span>
            </button>

            <Link
              to="/profile?tab=vouchers"
              className="flex items-center gap-1 px-3 py-1.5 rounded-xl text-xs font-bold bg-gradient-to-r from-brand-600 to-brand-500 hover:from-brand-500 hover:to-brand-400 text-white transition-all shadow-md shadow-brand-500/20"
            >
              <span>Ví voucher</span>
              <ArrowRight className="w-3 h-3" />
            </Link>
          </div>
        </div>
      </div>
    </motion.div>
  )
}

// ─── Main Page ────────────────────────────────
export default function PromotionsPage() {
  const [activeTab, setActiveTab] = useState('all') // 'all' | 'vouchers' | 'promotions'
  const [discountTypeFilter, setDiscountTypeFilter] = useState('ALL') // 'ALL' | 'PERCENTAGE' | 'FIXED_AMOUNT'
  const [searchQuery, setSearchQuery] = useState('')
  const [visibleVouchersCount, setVisibleVouchersCount] = useState(6)

  const { data: promotionsResponse, isLoading: isLoadingPromos } = useQuery({
    queryKey: ['active-promotions'],
    queryFn: () => promotionApi.getActive(),
  })

  const { data: vouchersResponse, isLoading: isLoadingVouchers } = useQuery({
    queryKey: ['active-vouchers'],
    queryFn: () => voucherApi.getActive(),
  })

  const promotions = promotionsResponse || []
  const vouchers = vouchersResponse || []

  // Normalized Vouchers list
  const normalizedVouchers = useMemo(() => {
    return vouchers.map(v => {
      let usageRemaining = null
      if (v.usageLimit != null) {
        usageRemaining = Math.max(0, v.usageLimit - (v.usedCount || 0))
      }
      return {
        ...v,
        endDate: v.validTo || v.endDate,
        usageRemaining,
        applicableTo: v.applicableTo ?? 'ALL'
      }
    })
  }, [vouchers])

  // Filtered Vouchers
  const filteredVouchers = useMemo(() => {
    return normalizedVouchers.filter(v => {
      // Search filter
      const matchesSearch = !searchQuery || 
        v.code?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        v.description?.toLowerCase().includes(searchQuery.toLowerCase())

      // Type filter
      const matchesType = discountTypeFilter === 'ALL' || v.discountType === discountTypeFilter

      return matchesSearch && matchesType
    })
  }, [normalizedVouchers, searchQuery, discountTypeFilter])

  // Filtered Promotions
  const filteredPromos = useMemo(() => {
    return promotions.filter(p => {
      if (!searchQuery) return true
      return p.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
             p.description?.toLowerCase().includes(searchQuery.toLowerCase()) ||
             p.subtitle?.toLowerCase().includes(searchQuery.toLowerCase())
    })
  }, [promotions, searchQuery])

  const showVouchers = activeTab === 'all' || activeTab === 'vouchers'
  const showPromotions = activeTab === 'all' || activeTab === 'promotions'

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-cinema-900 text-slate-900 dark:text-white pt-20 pb-20 transition-colors duration-300">
      
      {/* ─── Hero Header ───────────────────────── */}
      <div className="relative overflow-hidden bg-slate-100 dark:bg-gradient-to-b dark:from-cinema-950 dark:via-cinema-900 dark:to-cinema-900 border-b border-slate-200 dark:border-white/5 py-14 px-4">
        {/* Glow decoration */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[600px] h-[250px] bg-brand-500/10 blur-[120px] pointer-events-none rounded-full" />

        <div className="relative max-w-4xl mx-auto text-center">
          <motion.div initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }}>
            <span className="inline-flex items-center gap-2 bg-brand-500/15 border border-brand-500/30 text-brand-500 dark:text-brand-400 text-xs font-semibold px-3.5 py-1.5 rounded-full mb-4">
              <Gift className="w-3.5 h-3.5" /> Kho Ưu Đãi & Khuyến Mãi NovaTicket
            </span>
            <h1 className="font-display text-3xl md:text-5xl font-black text-slate-900 dark:text-white mb-3 tracking-tight">
              Săn Deal Xem Phim{' '}
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-brand-500 via-amber-500 to-amber-600 dark:from-brand-400 dark:via-amber-300 dark:to-gold-400">
                Cực Đã
              </span>
            </h1>
            <p className="text-slate-600 dark:text-cinema-300 text-sm md:text-base max-w-xl mx-auto">
              Nhận ngay các mã giảm giá vé, voucher bắp nước và chương trình ưu đãi độc quyền mỗi ngày
            </p>
          </motion.div>
        </div>
      </div>

      <div className="max-w-5xl mx-auto px-4 sm:px-6 pt-8 space-y-10">

        {/* ─── Interactive Filter & Search Bar ─── */}
        <div className="p-4 sticky top-20 z-20 backdrop-blur-xl bg-white/90 dark:bg-cinema-900/90 border border-slate-200 dark:border-white/10 shadow-lg dark:shadow-2xl space-y-3 rounded-2xl">
          <div className="flex flex-col md:flex-row items-center justify-between gap-3">
            {/* Main Category Tabs */}
            <div className="flex items-center gap-1.5 p-1 rounded-xl bg-slate-100 dark:bg-cinema-800/80 border border-slate-200 dark:border-white/5 w-full md:w-auto">
              <button
                onClick={() => setActiveTab('all')}
                className={cn(
                  'flex-1 md:flex-initial px-4 py-2 rounded-lg text-xs font-bold transition-all',
                  activeTab === 'all'
                    ? 'bg-brand-600 text-white shadow-glow-red'
                    : 'text-slate-600 dark:text-cinema-300 hover:text-slate-900 dark:hover:text-white hover:bg-slate-200/50 dark:hover:bg-white/5'
                )}
              >
                Tất cả ({promotions.length + normalizedVouchers.length})
              </button>
              <button
                onClick={() => setActiveTab('vouchers')}
                className={cn(
                  'flex-1 md:flex-initial flex items-center justify-center gap-1.5 px-4 py-2 rounded-lg text-xs font-bold transition-all',
                  activeTab === 'vouchers'
                    ? 'bg-brand-600 text-white shadow-glow-red'
                    : 'text-slate-600 dark:text-cinema-300 hover:text-slate-900 dark:hover:text-white hover:bg-slate-200/50 dark:hover:bg-white/5'
                )}
              >
                <Ticket className="w-3.5 h-3.5" />
                Mã Voucher ({normalizedVouchers.length})
              </button>
              <button
                onClick={() => setActiveTab('promotions')}
                className={cn(
                  'flex-1 md:flex-initial flex items-center justify-center gap-1.5 px-4 py-2 rounded-lg text-xs font-bold transition-all',
                  activeTab === 'promotions'
                    ? 'bg-brand-600 text-white shadow-glow-red'
                    : 'text-slate-600 dark:text-cinema-300 hover:text-slate-900 dark:hover:text-white hover:bg-slate-200/50 dark:hover:bg-white/5'
                )}
              >
                <Flame className="w-3.5 h-3.5" />
                Sự kiện ({promotions.length})
              </button>
            </div>

            {/* Search Input */}
            <div className="relative w-full md:w-72">
              <Search className="w-4 h-4 text-slate-400 dark:text-cinema-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Tìm mã hoặc tên chương trình..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3.5 py-2 rounded-xl bg-slate-50 dark:bg-cinema-800/90 border border-slate-200 dark:border-white/10 text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-cinema-500 text-xs focus:outline-none focus:border-brand-500 transition-colors"
              />
            </div>
          </div>

          {/* Subfilter for Vouchers (when viewing vouchers or all) */}
          {showVouchers && (
            <div className="flex items-center gap-2 pt-2 border-t border-slate-100 dark:border-white/5 text-xs text-slate-500 dark:text-cinema-400 overflow-x-auto">
              <span className="font-medium flex-shrink-0">Lọc theo loại:</span>
              <button
                onClick={() => setDiscountTypeFilter('ALL')}
                className={cn(
                  'px-2.5 py-1 rounded-lg transition-colors flex-shrink-0',
                  discountTypeFilter === 'ALL' ? 'bg-slate-200 dark:bg-white/15 text-slate-900 dark:text-white font-bold' : 'hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/5'
                )}
              >
                Tất cả
              </button>
              <button
                onClick={() => setDiscountTypeFilter('PERCENTAGE')}
                className={cn(
                  'px-2.5 py-1 rounded-lg transition-colors flex-shrink-0',
                  discountTypeFilter === 'PERCENTAGE' ? 'bg-brand-500/20 text-brand-600 dark:text-brand-400 font-bold border border-brand-500/30' : 'hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/5'
                )}
              >
                Giảm theo %
              </button>
              <button
                onClick={() => setDiscountTypeFilter('FIXED_AMOUNT')}
                className={cn(
                  'px-2.5 py-1 rounded-lg transition-colors flex-shrink-0',
                  discountTypeFilter === 'FIXED_AMOUNT' ? 'bg-amber-500/20 text-amber-600 dark:text-amber-400 font-bold border border-amber-500/30' : 'hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/5'
                )}
              >
                Giảm số tiền
              </button>
            </div>
          )}
        </div>

        {/* ─── Section 1: Chương trình sự kiện ─── */}
        {showPromotions && (
          <section className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="font-display text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <Flame className="w-5 h-5 text-brand-500" />
                  Chương trình ưu đãi đang diễn ra
                </h2>
                <p className="text-slate-500 dark:text-cinema-400 text-xs mt-0.5">Các sự kiện và khuyến mãi độc quyền tại hệ thống rạp</p>
              </div>
            </div>

            {isLoadingPromos ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                {[1, 2, 3].map(i => (
                  <div key={i} className="h-64 bg-slate-200 dark:bg-white/5 animate-pulse rounded-2xl border border-slate-200 dark:border-white/10" />
                ))}
              </div>
            ) : filteredPromos.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                {filteredPromos.map((promo, i) => {
                  const style = DEFAULT_PROMO_STYLES[i % DEFAULT_PROMO_STYLES.length]
                  return (
                    <motion.div
                      key={promo.id}
                      initial={{ opacity: 0, y: 15 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: i * 0.08 }}
                      className="group relative flex flex-col overflow-hidden rounded-2xl border border-slate-200 dark:border-white/8 bg-white dark:bg-cinema-800/80 hover:border-brand-500/40 shadow-sm hover:shadow-md transition-all duration-300"
                    >
                      {/* Image Header */}
                      <div className="relative h-44 overflow-hidden bg-cinema-950">
                        {promo.imageUrl ? (
                          <img
                            src={promo.imageUrl}
                            alt={promo.title}
                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-slate-400 dark:text-cinema-600 bg-slate-100 dark:bg-cinema-900">
                            <Film className="w-12 h-12" />
                          </div>
                        )}
                        <div className={cn('absolute inset-0 bg-gradient-to-t', style.gradient)} />
                        <span className={cn('absolute top-3 left-3 text-[11px] font-bold px-2.5 py-0.5 rounded-full shadow-md', style.tagColor)}>
                          {style.tag}
                        </span>
                      </div>

                      {/* Info Body */}
                      <div className="p-4 flex-1 flex flex-col justify-between">
                        <div>
                          <h3 className="font-display font-bold text-slate-900 dark:text-white text-base leading-snug line-clamp-1 mb-1">
                            {promo.title}
                          </h3>
                          <p className="text-brand-600 dark:text-brand-400 text-xs font-semibold mb-2">
                            {promo.subtitle || 'Ưu đãi hấp dẫn'}
                          </p>
                          <p className="text-slate-600 dark:text-cinema-300 text-xs line-clamp-2 leading-relaxed mb-4">
                            {promo.description}
                          </p>
                        </div>

                        <div className="flex items-center justify-between pt-3 border-t border-slate-100 dark:border-white/5 text-xs">
                          {promo.endDate ? (
                            <span className="text-slate-500 dark:text-cinema-400 flex items-center gap-1 text-[11px]">
                              <Calendar className="w-3 h-3 text-slate-400 dark:text-cinema-500" />
                              HSD: {formatDate(promo.endDate)}
                            </span>
                          ) : (
                            <span className="text-green-600 dark:text-green-400 text-[11px] font-medium">Đang áp dụng</span>
                          )}

                          <Link
                            to={promo.targetUrl || '/movies'}
                            className="flex items-center gap-1 text-brand-600 dark:text-brand-400 hover:text-brand-500 font-semibold transition-colors"
                          >
                            <span>Khám phá</span>
                            <ArrowRight className="w-3 h-3" />
                          </Link>
                        </div>
                      </div>
                    </motion.div>
                  )
                })}
              </div>
            ) : (
              <div className="py-12 text-center text-slate-500 dark:text-cinema-400 bg-slate-100 dark:bg-cinema-800/30 rounded-2xl border border-dashed border-slate-200 dark:border-white/10 text-xs">
                Không tìm thấy chương trình sự kiện nào phù hợp.
              </div>
            )}
          </section>
        )}

        {/* ─── Section 2: Kho Mã Voucher ──────── */}
        {showVouchers && (
          <section className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="font-display text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <Ticket className="w-5 h-5 text-amber-500 dark:text-amber-400" />
                  Mã Voucher Giảm Giá
                </h2>
                <p className="text-slate-500 dark:text-cinema-400 text-xs mt-0.5">Sao chép mã và dán vào bước xác nhận đặt vé để được giảm trực tiếp</p>
              </div>
            </div>

            {isLoadingVouchers ? (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {[1, 2, 3, 4].map(i => (
                  <div key={i} className="h-32 bg-white/5 animate-pulse rounded-2xl border border-white/10" />
                ))}
              </div>
            ) : filteredVouchers.length > 0 ? (
              <>
                {/* 2-Column Responsive Voucher Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {filteredVouchers.slice(0, visibleVouchersCount).map((v, i) => (
                    <CompactVoucherCard key={v.id || i} voucher={v} index={i} />
                  ))}
                </div>

                {/* Show More / Show Less Button */}
                {filteredVouchers.length > 6 && (
                  <div className="text-center pt-2">
                    {visibleVouchersCount < filteredVouchers.length ? (
                      <button
                        onClick={() => setVisibleVouchersCount(prev => prev + 6)}
                        className="inline-flex items-center gap-2 px-6 py-2.5 rounded-full bg-slate-200 dark:bg-white/5 hover:bg-slate-300 dark:hover:bg-white/10 border border-slate-300 dark:border-white/10 text-slate-800 dark:text-white text-xs font-semibold transition-all"
                      >
                        <span>Xem thêm ({filteredVouchers.length - visibleVouchersCount} mã khác)</span>
                        <ChevronDown className="w-4 h-4" />
                      </button>
                    ) : (
                      <button
                        onClick={() => setVisibleVouchersCount(6)}
                        className="inline-flex items-center gap-2 px-6 py-2.5 rounded-full bg-slate-200 dark:bg-white/5 hover:bg-slate-300 dark:hover:bg-white/10 border border-slate-300 dark:border-white/10 text-slate-700 dark:text-cinema-300 hover:text-slate-900 dark:hover:text-white text-xs font-semibold transition-all"
                      >
                        <span>Thu gọn danh sách</span>
                        <ChevronUp className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                )}
              </>
            ) : (
              <div className="py-12 text-center text-slate-500 dark:text-cinema-400 bg-slate-100 dark:bg-cinema-800/30 rounded-2xl border border-dashed border-slate-200 dark:border-white/10 text-xs">
                Không tìm thấy mã voucher nào phù hợp với bộ lọc.
              </div>
            )}
          </section>
        )}

        {/* ─── Section 3: Hướng dẫn sử dụng ──── */}
        <section className="p-6 sm:p-8 rounded-3xl border border-slate-200 dark:border-white/10 bg-white dark:bg-cinema-800/90 shadow-md">
          <h3 className="font-display font-bold text-slate-900 dark:text-white text-base mb-5 flex items-center gap-2">
            <HelpCircle className="w-4 h-4 text-brand-500 dark:text-brand-400" />
            Cách sử dụng mã giảm giá tại NovaTicket
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="p-4 rounded-2xl bg-slate-50 dark:bg-cinema-900 border border-slate-200 dark:border-white/10 flex items-start gap-3.5 shadow-sm">
              <div className="w-8 h-8 rounded-xl bg-brand-500/15 text-brand-500 dark:text-brand-400 font-extrabold text-xs flex items-center justify-center flex-shrink-0 border border-brand-500/25 shadow-xs">
                1
              </div>
              <div>
                <p className="text-slate-900 dark:text-white text-xs font-bold mb-1">Chọn phim & ghế</p>
                <p className="text-slate-600 dark:text-slate-300 text-xs leading-relaxed">Chọn suất chiếu và các vị trí ghế ngồi yêu thích của bạn.</p>
              </div>
            </div>
            <div className="p-4 rounded-2xl bg-slate-50 dark:bg-cinema-900 border border-slate-200 dark:border-white/10 flex items-start gap-3.5 shadow-sm">
              <div className="w-8 h-8 rounded-xl bg-brand-500/15 text-brand-500 dark:text-brand-400 font-extrabold text-xs flex items-center justify-center flex-shrink-0 border border-brand-500/25 shadow-xs">
                2
              </div>
              <div>
                <p className="text-slate-900 dark:text-white text-xs font-bold mb-1">Dán mã Voucher</p>
                <p className="text-slate-600 dark:text-slate-300 text-xs leading-relaxed">Tại bước xác nhận, nhập mã vào ô "Mã giảm giá" và bấm Áp dụng.</p>
              </div>
            </div>
            <div className="p-4 rounded-2xl bg-slate-50 dark:bg-cinema-900 border border-slate-200 dark:border-white/10 flex items-start gap-3.5 shadow-sm">
              <div className="w-8 h-8 rounded-xl bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 font-extrabold text-xs flex items-center justify-center flex-shrink-0 border border-emerald-500/25 shadow-xs">
                3
              </div>
              <div>
                <p className="text-slate-900 dark:text-white text-xs font-bold mb-1">Thanh toán ưu đãi</p>
                <p className="text-slate-600 dark:text-slate-300 text-xs leading-relaxed">Hệ thống tự động trừ tiền giảm giá và bạn hoàn tất thanh toán.</p>
              </div>
            </div>
          </div>
        </section>

        {/* ─── Section 4: Membership Welcome Banner ─ */}
        <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-brand-600 via-brand-700 to-cinema-950 p-8 text-center border border-brand-500/30 shadow-2xl">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(255,255,255,0.12),transparent_60%)] pointer-events-none" />
          <div className="relative max-w-xl mx-auto space-y-3">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/15 text-white text-xs font-semibold">
              <Sparkles className="w-3.5 h-3.5 text-amber-300" /> Đặc quyền Hội viên mới
            </span>
            <h3 className="font-display text-2xl md:text-3xl font-black text-white">
              Đăng ký để nhận voucher chào mừng 50.000đ
            </h3>
            <p className="text-white/80 text-xs md:text-sm leading-relaxed">
              Tạo tài khoản NovaTicket để nhận mã <strong className="text-amber-300 font-mono">WELCOME50K</strong> và tích điểm CinePoint đổi vé xem phim miễn phí trọn đời!
            </p>
            <div className="pt-3 flex items-center justify-center gap-3">
              <Link
                to="/auth/register"
                className="px-6 py-2.5 rounded-xl bg-white text-brand-600 font-bold text-xs hover:bg-brand-50 transition-all shadow-lg"
              >
                Đăng ký ngay
              </Link>
              <Link
                to="/movies"
                className="px-6 py-2.5 rounded-xl bg-black/20 hover:bg-black/30 border border-white/20 text-white font-semibold text-xs transition-all"
              >
                Xem lịch chiếu
              </Link>
            </div>
          </div>
        </section>

      </div>
    </div>
  )
}
