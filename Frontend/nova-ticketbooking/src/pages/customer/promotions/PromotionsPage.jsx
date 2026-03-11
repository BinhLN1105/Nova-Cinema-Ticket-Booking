import { useState } from 'react'
import { motion } from 'framer-motion'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Tag, Ticket, Copy, CheckCircle2, Clock, ArrowRight, Percent, DollarSign, Gift } from 'lucide-react'
import { promotionApi, voucherApi } from '@/api/endpoints'
import { formatDate, formatCurrency, cn } from '@/utils'

// ─── Mock data (thay bằng API) ────────────────
const ACTIVE_PROMOTIONS = [
  {
    id: '1',
    title: 'Mùa hè rực rỡ',
    subtitle: 'Giảm đến 30% tất cả suất chiếu',
    description: 'Chào mừng mùa hè! Đặt vé bất kỳ suất chiếu nào từ 01/06 – 31/08 và nhận ngay ưu đãi giảm giá hấp dẫn. Áp dụng cho tất cả thành viên NovaTicket.',
    imageUrl: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&q=80',
    endDate: '2024-08-31',
    voucherCode: 'SUMMER30',
    gradient: 'from-orange-600/80 to-red-800/80',
    tag: 'Nổi bật',
    tagColor: 'bg-amber-400 text-amber-900',
  },
  {
    id: '2',
    title: 'Combo gia đình cuối tuần',
    subtitle: 'Đặt 4 vé + tặng ngay 1 combo bắp nước',
    description: 'Mỗi cuối tuần là thời gian của gia đình! Đặt 4 vé trở lên cho 1 suất chiếu bất kỳ, hệ thống sẽ tự động cộng thêm 1 combo bắp nước vào đơn hàng của bạn.',
    imageUrl: 'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800&q=80',
    endDate: '2024-12-31',
    voucherCode: null,
    gradient: 'from-blue-800/80 to-indigo-900/80',
    tag: 'Cuối tuần',
    tagColor: 'bg-blue-400 text-blue-900',
  },
  {
    id: '3',
    title: 'Flashsale thứ 3',
    subtitle: 'Mỗi thứ 3 hàng tuần — Giảm 25%',
    description: 'Thứ 3 là ngày vui của tín đồ điện ảnh NovaTicket! Áp dụng tự động khi đặt vé vào thứ 3 bất kỳ trong tuần, không cần nhập mã.',
    imageUrl: 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=800&q=80',
    endDate: '2024-12-31',
    voucherCode: null,
    gradient: 'from-purple-700/80 to-pink-800/80',
    tag: 'Mỗi tuần',
    tagColor: 'bg-purple-400 text-purple-900',
  },
]

const PUBLIC_VOUCHERS = [
  { id: '1', code: 'NOVA20',     discountType: 'PERCENTAGE', discountValue: 20, minOrder: 100000, maxDiscount: 50000, description: 'Giảm 20% tối đa 50.000đ cho mọi đơn hàng từ 100.000đ', applicableTo: 'ALL',           endDate: '2024-12-31', usageRemaining: 377 },
  { id: '2', code: 'WELCOME50K', discountType: 'FIXED',      discountValue: 50000, minOrder: 100000, maxDiscount: null, description: 'Giảm 50.000đ — Chỉ dùng cho lần đặt vé đầu tiên', applicableTo: 'FIRST_BOOKING',  endDate: '2024-06-30', usageRemaining: 544 },
  { id: '3', code: 'VIP30',      discountType: 'PERCENTAGE', discountValue: 30, minOrder: 200000, maxDiscount: 100000, description: 'Giảm 30% tối đa 100.000đ — Ưu đãi thành viên VIP', applicableTo: 'ALL',           endDate: '2024-12-31', usageRemaining: 88 },
  { id: '4', code: 'FIRSTAPP',   discountType: 'FIXED',      discountValue: 30000, minOrder: 80000, maxDiscount: null, description: 'Giảm 30.000đ khi đặt vé lần đầu qua ứng dụng',    applicableTo: 'FIRST_BOOKING',  endDate: '2024-09-30', usageRemaining: 215 },
]

const APPLICABLE_LABELS = {
  ALL:           { label: 'Tất cả',           color: 'bg-blue-50 text-blue-600 border-blue-200' },
  FIRST_BOOKING: { label: 'Đặt vé lần đầu', color: 'bg-purple-50 text-purple-600 border-purple-200' },
  MOVIE:         { label: 'Phim cụ thể',      color: 'bg-amber-50 text-amber-600 border-amber-200' },
}

// ─── Copy Button ──────────────────────────────
function CopyButton({ code }) {
  const [copied, setCopied] = useState(false)

  const handleCopy = () => {
    navigator.clipboard.writeText(code)
    setCopied(true)
    setTimeout(() => setCopied(false), 2500)
  }

  return (
    <button onClick={handleCopy}
      className={cn(
        'flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold transition-all duration-300',
        copied
          ? 'bg-green-500/20 text-green-400 border border-green-500/40'
          : 'bg-white/10 text-white hover:bg-white/20 border border-white/20'
      )}>
      {copied ? <CheckCircle2 className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
      {copied ? 'Đã copy!' : 'Copy mã'}
    </button>
  )
}

// ─── Countdown timer ──────────────────────────
function Countdown({ endDate }) {
  const end  = new Date(endDate)
  const now  = new Date()
  const diff = end - now
  if (diff <= 0) return <span className="text-red-400 text-xs">Đã kết thúc</span>

  const days  = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))

  return (
    <span className="flex items-center gap-1 text-xs text-cinema-300">
      <Clock className="w-3 h-3" />
      Còn {days > 0 ? `${days} ngày` : `${hours} giờ`}
    </span>
  )
}

// ─── Voucher card ─────────────────────────────
function VoucherCard({ voucher, index }) {
  const [copied, setCopied] = useState(false)
  const isPercentage = voucher.discountType === 'PERCENTAGE'
  const applicable   = APPLICABLE_LABELS[voucher.applicableTo]

  const copy = () => {
    navigator.clipboard.writeText(voucher.code)
    setCopied(true)
    setTimeout(() => setCopied(false), 2500)
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.07 }}
      className="relative overflow-hidden"
    >
      {/* Ticket shape with notches */}
      <div className="flex rounded-2xl overflow-hidden border border-white/8 bg-cinema-800/70 backdrop-blur-sm shadow-lg">

        {/* Left accent stripe */}
        <div className={cn(
          'w-3 flex-shrink-0',
          isPercentage ? 'bg-gradient-to-b from-brand-500 to-brand-700' : 'bg-gradient-to-b from-gold-400 to-amber-600'
        )} />

        {/* Value section */}
        <div className={cn(
          'flex-shrink-0 w-28 flex flex-col items-center justify-center p-4',
          'border-r border-dashed border-white/15'
        )}>
          <div className={cn(
            'w-12 h-12 rounded-2xl flex items-center justify-center mb-1',
            isPercentage ? 'bg-brand-500/20' : 'bg-gold-400/20'
          )}>
            {isPercentage
              ? <Percent className={cn('w-6 h-6', 'text-brand-400')} />
              : <DollarSign className="w-6 h-6 text-gold-400" />
            }
          </div>
          <p className={cn(
            'text-2xl font-black leading-none',
            isPercentage ? 'text-brand-400' : 'text-gold-400'
          )}>
            {isPercentage ? `${voucher.discountValue}%` : formatCurrency(voucher.discountValue)}
          </p>
          <p className="text-cinema-400 text-[10px] mt-0.5 text-center leading-tight">
            {isPercentage ? 'phần trăm' : 'giảm trực tiếp'}
          </p>
        </div>

        {/* Info section */}
        <div className="flex-1 p-4 min-w-0">
          <div className="flex items-start justify-between gap-2 mb-2">
            <div>
              <span className={cn(
                'inline-flex items-center text-[10px] font-semibold px-2 py-0.5 rounded-full border mb-1.5',
                applicable.color
              )}>
                {applicable.label}
              </span>
              <p className="text-cinema-200 text-xs leading-snug line-clamp-2">{voucher.description}</p>
            </div>
          </div>

          <div className="flex items-center gap-3 text-[11px] text-cinema-400 mt-2 mb-3">
            {voucher.minOrder > 0 && <span>Đơn từ {formatCurrency(voucher.minOrder)}</span>}
            {voucher.maxDiscount && <span>• Tối đa {formatCurrency(voucher.maxDiscount)}</span>}
          </div>

          <div className="flex items-center justify-between gap-2">
            {/* Code pill */}
            <button onClick={copy}
              className={cn(
                'flex items-center gap-2 px-3 py-1.5 rounded-xl border transition-all duration-200',
                copied
                  ? 'bg-green-500/20 border-green-500/40'
                  : 'bg-cinema-700/60 border-white/10 hover:border-white/30'
              )}>
              <span className={cn(
                'font-mono text-sm font-bold tracking-widest',
                copied ? 'text-green-400' : 'text-white'
              )}>
                {voucher.code}
              </span>
              {copied
                ? <CheckCircle2 className="w-3.5 h-3.5 text-green-400" />
                : <Copy className="w-3.5 h-3.5 text-cinema-400" />
              }
            </button>

            <div className="text-right flex-shrink-0">
              <Countdown endDate={voucher.endDate} />
              <p className="text-cinema-500 text-[10px] mt-0.5">
                Còn {voucher.usageRemaining.toLocaleString()} lượt
              </p>
            </div>
          </div>
        </div>
      </div>
    </motion.div>
  )
}

// ─── Main page ────────────────────────────────
export default function PromotionsPage() {
  const [activeCategory, setCategory] = useState('all')

  const CATEGORIES = [
    { id: 'all',        label: 'Tất cả' },
    { id: 'vouchers',   label: '🎟️ Mã Voucher' },
    { id: 'events',     label: '🎉 Sự kiện' },
    { id: 'combo',      label: '🍿 Combo ưu đãi' },
  ]

  const filteredPromos = activeCategory === 'all' ? ACTIVE_PROMOTIONS : ACTIVE_PROMOTIONS.slice(0, 1)

  return (
    <div className="min-h-screen bg-cinema-900 pt-20">

      {/* Hero banner */}
      <div className="relative overflow-hidden bg-gradient-to-br from-cinema-950 via-brand-950/30 to-cinema-900 py-16 px-4">
        {/* Decorative circles */}
        <div className="absolute top-0 right-0 w-96 h-96 rounded-full bg-brand-500/8 blur-[100px] pointer-events-none" />
        <div className="absolute bottom-0 left-0 w-64 h-64 rounded-full bg-gold-400/6 blur-[80px] pointer-events-none" />

        <div className="relative max-w-4xl mx-auto text-center">
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
            <span className="inline-flex items-center gap-2 bg-brand-500/15 border border-brand-500/30
              text-brand-400 text-sm font-medium px-4 py-1.5 rounded-full mb-5">
              <Gift className="w-4 h-4" /> Ưu đãi dành riêng cho bạn
            </span>
            <h1 className="font-display text-4xl md:text-5xl font-black text-white mb-4 leading-tight">
              Khuyến mãi &<br />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-brand-400 to-gold-400">
                Mã giảm giá
              </span>
            </h1>
            <p className="text-cinema-300 text-lg max-w-xl mx-auto">
              Tiết kiệm hơn khi xem phim với hàng loạt ưu đãi độc quyền từ NovaTicket
            </p>
          </motion.div>
        </div>
      </div>

      <div className="max-w-5xl mx-auto px-4 sm:px-6 py-10 space-y-12">

        {/* ── Chương trình nổi bật ───────────── */}
        <section>
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="font-display text-2xl font-bold text-white">Chương trình đang chạy</h2>
              <p className="text-cinema-400 text-sm mt-0.5">Ưu đãi có thời hạn, đừng bỏ lỡ!</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {ACTIVE_PROMOTIONS.map((promo, i) => (
              <motion.div key={promo.id}
                initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.1 }}
                className="group relative overflow-hidden rounded-2xl border border-white/8
                  hover:border-white/20 transition-all duration-500 cursor-pointer"
              >
                {/* Image */}
                <div className="relative h-44 overflow-hidden">
                  <img src={promo.imageUrl} alt={promo.title}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700" />
                  <div className={cn('absolute inset-0 bg-gradient-to-t', promo.gradient)} />

                  {/* Tag */}
                  <span className={cn('absolute top-3 left-3 text-xs font-bold px-2.5 py-1 rounded-full', promo.tagColor)}>
                    {promo.tag}
                  </span>

                  {/* Voucher code pill on image */}
                  {promo.voucherCode && (
                    <div className="absolute bottom-3 right-3">
                      <CopyButton code={promo.voucherCode} />
                    </div>
                  )}
                </div>

                {/* Content */}
                <div className="p-4 bg-cinema-800/80 backdrop-blur-sm">
                  <h3 className="font-display font-bold text-white text-base mb-0.5">{promo.title}</h3>
                  <p className="text-brand-400 text-sm font-medium mb-2">{promo.subtitle}</p>
                  <p className="text-cinema-400 text-xs line-clamp-2 mb-3">{promo.description}</p>

                  <div className="flex items-center justify-between">
                    <Countdown endDate={promo.endDate} />
                    <Link to="/movies"
                      className="flex items-center gap-1 text-xs text-brand-400 hover:text-brand-300 font-medium transition-colors">
                      Đặt vé ngay <ArrowRight className="w-3.5 h-3.5" />
                    </Link>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        </section>

        {/* ── Voucher section ─────────────────── */}
        <section>
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="font-display text-2xl font-bold text-white">Mã Voucher</h2>
              <p className="text-cinema-400 text-sm mt-0.5">Copy mã và dán vào ô voucher khi thanh toán</p>
            </div>
          </div>

          {/* How to use */}
          <div className="flex items-start gap-3 p-4 rounded-2xl bg-brand-500/8 border border-brand-500/20 mb-6">
            <Tag className="w-5 h-5 text-brand-400 flex-shrink-0 mt-0.5" />
            <div className="text-sm text-cinema-300 leading-relaxed">
              <span className="text-white font-medium">Cách dùng:</span> Chọn phim → Chọn suất → Chọn ghế → Tại bước
              {' '}<strong className="text-white">Xác nhận đặt vé</strong>{', '}
              dán mã vào ô "Mã giảm giá" và nhấn <strong className="text-white">Áp dụng</strong>.
            </div>
          </div>

          <div className="space-y-3">
            {PUBLIC_VOUCHERS.map((v, i) => (
              <VoucherCard key={v.id} voucher={v} index={i} />
            ))}
          </div>
        </section>

        {/* ── CTA ─────────────────────────────── */}
        <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-brand-600 to-brand-800 p-8 text-center">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(255,255,255,0.1),transparent_60%)]" />
          <div className="relative">
            <p className="text-white/70 text-sm mb-2">Chưa có tài khoản?</p>
            <h3 className="font-display text-2xl font-bold text-white mb-3">
              Đăng ký để nhận voucher chào mừng 50.000đ
            </h3>
            <p className="text-white/60 text-sm mb-6 max-w-md mx-auto">
              Thành viên mới NovaTicket nhận ngay mã <strong className="text-white">WELCOME50K</strong> giảm 50.000đ cho đơn hàng đầu tiên
            </p>
            <div className="flex items-center justify-center gap-3 flex-wrap">
              <Link to="/auth/register"
                className="px-6 py-3 rounded-xl bg-white text-brand-600 font-bold text-sm hover:bg-brand-50 transition-all shadow-lg">
                Đăng ký miễn phí
              </Link>
              <Link to="/movies"
                className="px-6 py-3 rounded-xl bg-white/10 border border-white/30 text-white font-semibold text-sm hover:bg-white/20 transition-all">
                Khám phá phim
              </Link>
            </div>
          </div>
        </section>

      </div>
    </div>
  )
}
