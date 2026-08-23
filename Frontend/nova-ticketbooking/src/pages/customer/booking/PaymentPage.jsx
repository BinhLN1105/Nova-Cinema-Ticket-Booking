import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Shield, CreditCard, Wallet, Building2, CheckCircle2, Coins, Loader2, MapPin, Clock, Monitor, Film } from 'lucide-react'
import { useAuth } from '@/hooks'
import { useAuthStore } from '@/stores/authStore'
import { authApi, bookingApi } from '@/api/endpoints'
import { formatCurrency, formatDateTime } from '@/utils'
import toast from 'react-hot-toast'

export default function PaymentPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [selectedMethod, setSelectedMethod] = useState('wallet')
  const [isPaying, setIsPaying] = useState(false)

  const { data: booking, refetch: refetchBooking } = useQuery({
    queryKey: ['booking', id],
    queryFn: () => bookingApi.getById(id),
    enabled: !!id,
  })

  // Đồng bộ lại thông tin User / số dư CinePoint thời gian thực khi vào trang
  useEffect(() => {
    window.scrollTo(0, 0)
    authApi.me().then(res => {
      const userData = res?.data || res
      if (userData) {
        useAuthStore.getState().setUser(userData)
      }
    }).catch(() => {})
  }, [id])

  // Đếm ngược thời gian hết hạn cho phiên thanh toán
  const [timeLeft, setTimeLeft] = useState('')
  const [isLowTime, setIsLowTime] = useState(false)
  const [isExpired, setIsExpired] = useState(false)

  useEffect(() => {
    if (!booking?.expiresAt) return

    const targetTime = new Date(booking.expiresAt).getTime()

    const updateTimer = () => {
      const diff = targetTime - Date.now()
      if (diff <= 0) {
        setTimeLeft('00:00')
        setIsExpired(true)
        setIsLowTime(true)
        toast.error('Đơn đặt vé đã hết hạn thanh toán!', { icon: '⏳' })
        setTimeout(() => navigate(`/tickets/${id}`), 1500)
        return false
      }
      const mins = Math.floor(diff / (1000 * 60))
      const secs = Math.floor((diff % (1000 * 60)) / 1000)
      setIsLowTime(mins < 2)
      setTimeLeft(`${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`)
      return true
    }

    if (!updateTimer()) return

    const timer = setInterval(() => {
      if (!updateTimer()) clearInterval(timer)
    }, 1000)

    return () => clearInterval(timer)
  }, [booking?.expiresAt, id, navigate])

  const handlePayment = async () => {
    if (!id || isPaying) return
    setIsPaying(true)
    try {
      if (selectedMethod === 'vnpay') {
        const payment = await bookingApi.createPayment(id)
        window.location.href = payment.paymentUrl
      } else if (selectedMethod === 'wallet') {
        const result = await bookingApi.payWithWallet(id)
        // Hybrid: CP trừ xong nhưng còn dư cần qua cổng
        if (result.remainingAmount && result.remainingAmount > 0) {
          toast.success(`Đã trừ ${result.pointsUsed?.toLocaleString('vi-VN')} CP. Chuyển sang thanh toán ${formatCurrency(result.remainingAmount)} còn lại...`)
          
          // Refresh lại balance trước khi chuyển cổng
          const updatedUser = await authApi.me().catch(() => null)
          if (updatedUser) useAuthStore.getState().setUser(updatedUser?.data || updatedUser)

          // Tạo URL thanh toán VNPay cho phần còn lại
          const vnpayPayment = await bookingApi.createPayment(id)
          window.location.href = vnpayPayment.paymentUrl
        } else {
          toast.success('Thanh toán bằng CinePoint thành công!')
          // Refresh profile to update CP balance
          const updatedUser = await authApi.me().catch(() => null)
          if (updatedUser) useAuthStore.getState().setUser(updatedUser?.data || updatedUser)
          navigate('/booking/result?status=success')
        }
      } else {
        toast('Tính năng này đang được phát triển', { icon: '🚧' })
      }
    } catch (error) {
      const msg = error.response?.data?.message || 'Thanh toán thất bại'
      toast.error(msg)
    } finally {
      setIsPaying(false)
    }
  }

  const walletBalance = user?.rewardPoints ?? 0
  const totalAmount = Number(booking?.totalAmount ?? 0)

  // Thuật toán tính CP đồng bộ với Backend:
  const cpNeededToCoverAll = Math.ceil(totalAmount / 1000)
  const MIN_GATEWAY = 10_000

  let displayCp = 0
  let displayRemaining = totalAmount

  if (walletBalance >= cpNeededToCoverAll && cpNeededToCoverAll > 0) {
    // Trường hợp 1: Đủ điểm "Mua đứt" (Buyout)
    displayCp = cpNeededToCoverAll
    displayRemaining = 0
  } else {
    // Trường hợp 2: Thanh toán lai (Hybrid)
    const maxCpApplicable = Math.floor(totalAmount / 1000)
    const actualCp = Math.min(maxCpApplicable, walletBalance)
    const remaining = totalAmount - (actualCp * 1000)

    displayCp = actualCp
    displayRemaining = remaining

    if (remaining > 0 && remaining < MIN_GATEWAY) {
      // Điều chỉnh: giảm CP để remaining >= 10.000đ (ngưỡng tối thiểu cổng)
      displayCp = Math.max(0, Math.floor((totalAmount - MIN_GATEWAY) / 1000))
      displayRemaining = totalAmount - displayCp * 1000
    }
  }

  const hasEnoughBalance = walletBalance > 0 && displayCp > 0

  // Tính toán chi tiết giá vé ban đầu và giảm giá
  const originalPrice = Number(booking?.totalOriginalAmount || booking?.subtotal || 0) || (
    totalAmount + Number(booking?.discountAmount || 0) + Number(booking?.promotionDiscountAmount || 0) + Number(booking?.rankDiscountAmount || 0)
  )
  const totalDiscount = (originalPrice > totalAmount) ? (originalPrice - totalAmount) : 0

  const paymentMethods = [
    { 
      id: 'wallet', 
      name: 'Ví CinePoint', 
      icon: Coins, 
      color: 'text-yellow-400',
      desc: hasEnoughBalance
        ? (displayRemaining === 0 
            ? `Dùng ${displayCp.toLocaleString('vi-VN')} CP để thanh toán toàn bộ đơn (Số dư: ${walletBalance.toLocaleString('vi-VN')} CP)`
            : `Dùng ${displayCp.toLocaleString('vi-VN')} CP = giảm ${formatCurrency(displayCp * 1000)} · Còn lại ${formatCurrency(displayRemaining)} qua cổng`)
        : `Số dư: ${walletBalance.toLocaleString('vi-VN')} CP (Không đủ)`,
      disabled: !hasEnoughBalance 
    },
    { id: 'vnpay', name: 'VNPay', icon: CreditCard, color: 'text-blue-400', desc: 'Thanh toán qua ví VNPay hoặc quét mã QR' },
    { id: 'momo', name: 'MoMo', icon: Wallet, color: 'text-pink-500', desc: 'Thanh toán bằng ví điện tử MoMo' },
    { id: 'bank', name: 'Thẻ ngân hàng', icon: Building2, color: 'text-green-400', desc: 'Thẻ ATM nội địa / Visa / Mastercard' }
  ]

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-16 flex items-start justify-center">
      <div className="max-w-md w-full px-4 sm:px-6">
        <h1 className="font-display text-3xl font-bold text-white mb-2 text-center">Thanh toán</h1>
        <p className="text-cinema-300 text-center mb-6 text-sm">Xác nhận thông tin đơn hàng và chọn phương thức thanh toán</p>

        {/* Countdown Timer Banner */}
        {booking?.expiresAt && !isExpired && (
          <div className={`p-4 rounded-2xl mb-5 border flex items-center justify-between transition-all duration-300 shadow-md
            ${isLowTime
              ? 'bg-red-500/15 border-red-500/40 shadow-red-500/10 animate-pulse'
              : 'bg-amber-500/10 border-amber-500/30 shadow-amber-500/5'}`}>
            <div className="flex items-center gap-3">
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0
                ${isLowTime ? 'bg-red-500/20 text-red-400' : 'bg-amber-500/20 text-amber-400'}`}>
                <Clock className="w-5 h-5 animate-pulse" />
              </div>
              <div>
                <p className="text-white text-xs font-semibold">Thời gian thanh toán còn lại</p>
                <p className="text-cinema-400 text-[11px]">Vui lòng hoàn tất trước khi hết giờ giữ ghế</p>
              </div>
            </div>
            <span className={`font-mono font-bold text-2xl tracking-wider flex-shrink-0
              ${isLowTime ? 'text-red-400' : 'text-amber-400'}`}>
              {timeLeft || '--:--'}
            </span>
          </div>
        )}

        {/* Booking Details & Pricing Breakdown Card */}
        {booking && (
          <div className="card-cinema p-5 mb-5 space-y-4">
            {/* Movie Info Header */}
            <div className="flex gap-3.5 items-start">
              {booking.moviePosterUrl ? (
                <img 
                  src={booking.moviePosterUrl} 
                  alt={booking.movieTitle} 
                  className="w-16 h-22 object-cover rounded-xl flex-shrink-0 border border-white/10 shadow-md"
                />
              ) : (
                <div className="w-16 h-22 rounded-xl bg-cinema-800 flex items-center justify-center text-cinema-500 border border-white/10 flex-shrink-0">
                  <Film className="w-6 h-6" />
                </div>
              )}
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between gap-2 mb-1">
                  <span className="text-[11px] font-mono text-cinema-400 bg-white/5 px-2 py-0.5 rounded border border-white/5">
                    {booking.bookingCode}
                  </span>
                </div>
                <h2 className="font-display font-bold text-white text-base leading-snug truncate">
                  {booking.movieTitle || 'Vé xem phim'}
                </h2>
                <div className="mt-2 space-y-1 text-xs text-cinema-300">
                  {booking.cinemaName && (
                    <p className="flex items-center gap-1.5 truncate">
                      <MapPin className="w-3.5 h-3.5 text-cinema-400 flex-shrink-0" />
                      <span>{booking.cinemaName}</span>
                    </p>
                  )}
                  {booking.screenName && (
                    <p className="flex items-center gap-1.5">
                      <Monitor className="w-3.5 h-3.5 text-cinema-400 flex-shrink-0" />
                      <span>{booking.screenName}</span>
                    </p>
                  )}
                  {booking.startTime && (
                    <p className="flex items-center gap-1.5">
                      <Clock className="w-3.5 h-3.5 text-cinema-400 flex-shrink-0" />
                      <span>{formatDateTime(booking.startTime)}</span>
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* Selected Seats & Combos */}
            {booking.seats && booking.seats.length > 0 && (
              <div className="p-3 rounded-xl bg-cinema-800/70 border border-white/5">
                <div className="flex justify-between items-center text-xs">
                  <span className="text-cinema-400 font-semibold uppercase tracking-wider">Ghế đã chọn:</span>
                  <div className="flex flex-wrap gap-1.5 justify-end">
                    {booking.seats.map((s, i) => (
                      <span key={i} className="px-2 py-0.5 rounded bg-brand-500/15 border border-brand-500/30 text-brand-400 font-mono font-bold text-xs">
                        {s.rowLabel}{s.colNumber}
                      </span>
                    ))}
                  </div>
                </div>
                {booking.combos && booking.combos.length > 0 && (
                  <div className="mt-2 pt-2 border-t border-white/5 flex justify-between items-center text-xs">
                    <span className="text-cinema-400 font-semibold uppercase tracking-wider">Bắp nước:</span>
                    <span className="text-cinema-200">
                      {booking.combos.map(c => `${c.comboName} (x${c.quantity})`).join(', ')}
                    </span>
                  </div>
                )}
              </div>
            )}

            {/* Price Breakdown */}
            <div className="space-y-1.5 pt-3 border-t border-white/8 text-sm">
              {totalDiscount > 0 && (
                <div className="flex justify-between text-cinema-400">
                  <span>Giá vé gốc</span>
                  <span className="text-cinema-200">{formatCurrency(originalPrice)}</span>
                </div>
              )}
              {totalDiscount > 0 && (
                <div className="flex justify-between text-cinema-400">
                  <span>{booking.pointDiscount > 0 ? 'Đã trừ CinePoint' : 'Khuyến mãi / Giảm giá'}</span>
                  <span className="text-green-400 font-medium">- {formatCurrency(totalDiscount)}</span>
                </div>
              )}
              <div className="flex justify-between items-center font-bold pt-2 border-t border-white/5">
                <span className="text-white text-base">Số tiền cần thanh toán</span>
                <span className="text-brand-400 text-xl font-bold">
                  {formatCurrency(totalAmount)}
                </span>
              </div>
            </div>
          </div>
        )}

        {/* Payment Methods Selection */}
        <div className="card-cinema p-3.5 mb-6 space-y-2">
          <p className="px-2 text-sm text-cinema-400 font-medium mb-1">Phương thức thanh toán</p>
          {paymentMethods.map((method) => {
            const Icon = method.icon
            const isSelected = selectedMethod === method.id
            return (
              <button key={method.id} onClick={() => !method.disabled && setSelectedMethod(method.id)}
                disabled={method.disabled}
                className={`w-full flex items-center justify-between p-4 rounded-xl border transition-all text-left
                  ${method.disabled ? 'opacity-40 cursor-not-allowed glass-dark border-white/5' :
                    isSelected ? 'bg-brand-500/10 border-brand-500 shadow-glow-red' : 'glass-dark border-white/5 hover:border-white/20'}`}>
                <div className="flex items-center gap-3.5 flex-1 min-w-0">
                  <div className={`p-2 rounded-lg bg-white/5 ${method.color} flex-shrink-0`}>
                    <Icon className="w-6 h-6" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className={`font-semibold text-sm ${isSelected ? 'text-white' : 'text-gray-200'}`}>{method.name}</p>
                    <p className="text-xs text-cinema-400 mt-0.5 leading-normal">{method.desc}</p>
                  </div>
                </div>
                {isSelected && <CheckCircle2 className="w-5 h-5 text-brand-500 flex-shrink-0 ml-2" />}
              </button>
            )
          })}
        </div>

        {/* Submit Payment Button */}
        <button onClick={handlePayment} disabled={isPaying}
          className="w-full flex items-center justify-center gap-3 p-4.5 rounded-2xl
          bg-gradient-to-r from-brand-600 to-brand-500 hover:from-brand-500 hover:to-brand-400
          border border-brand-500/30 transition-all
          text-white font-semibold text-base shadow-glow-red disabled:opacity-60 disabled:cursor-not-allowed">
          {isPaying ? (
            <><Loader2 className="w-5 h-5 animate-spin" /> Đang xử lý thanh toán...</>
          ) : selectedMethod === 'wallet' ? (
            displayRemaining > 0
              ? `Trừ ${displayCp.toLocaleString('vi-VN')} CP + ${formatCurrency(displayRemaining)} qua cổng`
              : `Thanh toán ${displayCp.toLocaleString('vi-VN')} CP (miễn phí)`
          ) : `Thanh toán ngay (${formatCurrency(totalAmount)})`}
        </button>

        <div className="flex items-center justify-center gap-2 mt-6 text-cinema-400 text-xs">
          <Shield className="w-4 h-4 text-cinema-500" />
          <span>Thanh toán được mã hóa và bảo vệ bởi NovaTicket Security</span>
        </div>
      </div>
    </div>
  )
}
