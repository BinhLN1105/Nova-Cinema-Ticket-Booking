import { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Tag, Plus, Minus, ShoppingBag, ArrowRight } from 'lucide-react'
import { showtimeApi, voucherApi, userApi } from '@/api/endpoints'
import { useBooking } from '@/hooks'
import { formatCurrency, formatDateTime, cn, calculateActualDiscount } from '@/utils'
import BookingTimer from '@/components/common/ui/BookingTimer'
import { VoucherModal } from '@/components/customer/VoucherModal'

export default function ConfirmBooking() {
  const navigate = useNavigate()
  const booking = useBooking()
  const [voucherInput, setVoucherInput] = useState('')
  const [voucherError, setVoucherError] = useState('')
  const [isValidating, setIsValidating] = useState(false)
  const [isLoadingQuote, setIsLoadingQuote] = useState(true)
  const [isVoucherModalOpen, setIsVoucherModalOpen] = useState(false)

  // Auto-calculated fields
  const cartTotalAmount = booking.originalTotal || 0

  // Load user's vouchers
  const { data: userVouchers } = useQuery({
    queryKey: ['my-vouchers-checkout'],
    queryFn: () => userApi.getMyVouchers(),
    staleTime: 0
  })

  // Auto Select Best Voucher when loaded
  useEffect(() => {
    if (userVouchers && userVouchers.length > 0 && cartTotalAmount > 0 && !booking.appliedVoucher) {
      let maxDiscount = 0;
      let bestVoucher = null;

      userVouchers.forEach(v => {
        if (v.status !== 'AVAILABLE') return;
        if (v.minOrder && cartTotalAmount < v.minOrder) return;
        
        const actual = calculateActualDiscount(cartTotalAmount, v);
        if (actual > maxDiscount) {
          maxDiscount = actual;
          bestVoucher = { ...v, code: v.code }; // Map properly
        }
      });

      if (bestVoucher) {
        // Assume bestVoucher has enough info, wait, we need to format it to match what validate does, 
        // or just apply it directly since backend quote endpoint will recalculate it securely.
        booking.applyVoucher(bestVoucher);
      }
    }
  }, [userVouchers, cartTotalAmount]);

  useEffect(() => {
    window.scrollTo(0, 0)
    if (!booking.selectedShowtime || booking.selectedSeats.length === 0) {
      navigate('/')
    }
  }, [])

  const { data: combos } = useQuery({
    queryKey: ['combos'],
    queryFn: showtimeApi.getCombos,
  })

  const applyVoucher = async () => {
    if (!voucherInput.trim()) return
    try {
      const v = await voucherApi.validate(voucherInput.trim())
      booking.applyVoucher(v)
      setVoucherError('')
    } catch {
      setVoucherError('Mã không hợp lệ hoặc đã hết hạn')
    }
  }

  // ── Final Server-side Quote (Push-based Verification) ──
  useEffect(() => {
    const fetchQuote = async () => {
      setIsLoadingQuote(true);
      try {
        await booking.fetchServerQuote();
      } catch (err) {
        console.error("Failed to fetch quote:", err);
      } finally {
        setIsLoadingQuote(false);
      }
    };
    fetchQuote();
  }, [booking.appliedVoucher]);

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-32">
      <div className="max-w-2xl mx-auto px-4 sm:px-6">
        {/* Header */}
        <div className="flex items-center gap-4 mb-8">
          <button onClick={() => navigate(-1)}
            className="p-2.5 rounded-xl glass border border-white/8 text-cinema-200 hover:text-white transition-all">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <h1 className="font-display text-2xl font-bold text-white">Xác nhận đặt vé</h1>
        </div>

        {/* Timer */}
        <div className="mb-8">
          <BookingTimer />
        </div>

        <div className="space-y-4">
          {/* Combos Summary */}
          {Object.keys(booking.selectedCombos).length > 0 && combos && (
            <div className="card-cinema p-5">
              <div className="flex items-center gap-2 mb-4">
                <ShoppingBag className="w-4 h-4 text-gold-400" />
                <h3 className="text-sm font-semibold text-white uppercase tracking-wider">Bắp / Nước</h3>
              </div>
              <div className="space-y-3 text-sm">
                {Object.entries(booking.selectedCombos).map(([id, data]) => {
                  const combo = combos.find(c => c.id === id)
                  if (!combo) return null
                  const qty = data.quantity;
                  return (
                    <div key={id} className="flex justify-between gap-4 text-xs">
                      <span className="text-gray-300 font-medium">{combo.name} <span className="text-white ml-2">x{qty}</span></span>
                      <span className="text-white text-right font-medium">{formatCurrency(combo.price * qty)}</span>
                    </div>
                  )
                })}
              </div>
            </div>
          )}
          {/* Booking summary */}
          {booking.selectedShowtime && (
            <div className="card-cinema p-5">
              <h3 className="text-sm font-semibold text-white uppercase tracking-wider mb-4">Thông tin suất chiếu</h3>
              <div className="space-y-2 text-sm">
                {[
                  ['Phim', booking.selectedMovie?.title],
                  ['Rạp', booking.selectedShowtime.cinemaName],
                  ['Phòng', `${booking.selectedShowtime.screenName} · ${booking.selectedShowtime.screenType}`],
                  ['Giờ chiếu', formatDateTime(booking.selectedShowtime.startTime)],
                  ['Ghế', booking.selectedSeats.map(s => `${s.rowLabel}${s.colNumber}`).join(', ')],
                ].map(([label, value]) => value && (
                  <div key={label} className="flex justify-between gap-4">
                    <span className="text-gray-400 font-medium">{label}</span>
                    <span className="text-white text-right font-medium">{value}</span>
                  </div>
                ))}
              </div>
            </div>
          )}


          <div className="card-cinema p-5">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Tag className="w-4 h-4 text-gold-400" />
                <h3 className="text-sm font-semibold text-white uppercase tracking-wider">Mã ưu đãi</h3>
              </div>
              <button 
                onClick={() => setIsVoucherModalOpen(true)}
                className="text-xs font-bold text-brand-400 bg-brand-500/10 px-3 py-1.5 rounded hover:bg-brand-500/20 transition"
              >
                Chọn mã khác {userVouchers && userVouchers.length > 0 ? `(${userVouchers.length})` : ''}
              </button>
            </div>
            
            {booking.appliedVoucher ? (
              <div className="flex items-center justify-between p-3 rounded-xl
                bg-green-500/10 border border-green-500/30">
                <div>
                  <p className="text-green-400 font-bold text-sm">✓ Mã Tốt Nhất: {booking.appliedVoucher.code}</p>
                  <p className="text-green-300/80 text-xs mt-1">{booking.appliedVoucher.description || 'Đã áp dụng ưu đãi'}</p>
                </div>
                <button onClick={() => booking.clearVoucher()}
                  className="text-gray-400 hover:text-white text-xs px-2 py-1 transition-colors">Xóa</button>
              </div>
            ) : (
              <div className="flex justify-between items-center bg-cinema-800 p-3 rounded-xl border border-white/5">
                <p className="text-cinema-400 text-sm italic">Chưa chọn mã giảm giá nào</p>
                <button 
                  onClick={() => setIsVoucherModalOpen(true)}
                  className="text-sm text-brand-400 font-bold"
                >
                  Chọn ngay
                </button>
              </div>
            )}
            {voucherError && <p className="text-red-400 text-xs mt-3 bg-red-400/10 p-2 rounded">{voucherError}</p>}
            {booking.warningMessage && <p className="text-amber-400 text-xs mt-3 bg-amber-400/10 p-2 rounded">{booking.warningMessage}</p>}
          </div>

          {/* Price breakdown */}
          <div className="card-cinema p-5 space-y-3">
            {/* Tiền vé gốc */}
            <div className="flex justify-between text-sm text-gray-400 font-medium">
              <span>Tiền vé (Gốc)</span>
              {isLoadingQuote ? (
                <div className="w-20 h-4 bg-white/10 rounded animate-pulse" />
              ) : (
                <span className="text-white">{formatCurrency(booking.originalTotal || 0)}</span>
              )}
            </div>

            {/* Tiền bắp nước gốc (Chỉ hiện nếu có chọn) */}
            {Object.keys(booking.selectedCombos).length > 0 && (
              <div className="flex justify-between text-sm text-gray-400 font-medium">
                <span>Bắp nước (Gốc)</span>
                {isLoadingQuote ? (
                   <div className="w-20 h-4 bg-white/10 rounded animate-pulse" />
                ) : (
                  <span className="text-white">
                    {formatCurrency(
                      Object.entries(booking.selectedCombos).reduce((acc, [id, data]) => {
                        const combo = combos?.find(c => c.id === id)
                        return acc + (combo ? combo.price * data.quantity : 0)
                      }, 0)
                    )}
                  </span>
                )}
              </div>
            )}

            {/* Khuyến mãi hệ thống (Nếu có) */}
            {booking.promotionDiscount > 0 && (
              <div className="flex justify-between text-sm font-semibold">
                <span className="text-brand-400">
                  🎁 {booking.appliedPromotionName || 'Khuyến mãi hệ thống'}
                </span>
                <span className="text-brand-400">- {formatCurrency(booking.promotionDiscount)}</span>
              </div>
            )}

            {/* Ưu đãi Hạng thành viên (Nếu có) */}
            {booking.rankDiscount > 0 && (
              <div className="flex justify-between text-sm font-semibold">
                <span className="text-gold-400">
                  ⭐ Ưu đãi Hạng thành viên
                </span>
                <span className="text-gold-400">- {formatCurrency(booking.rankDiscount)}</span>
              </div>
            )}

            {/* Voucher (Nếu có mã giảm giá được áp dụng) */}
            {booking.discount > 0 && (
              <div className="flex justify-between text-sm font-semibold">
                <span className="text-green-400">🎫 Giảm giá Voucher</span>
                <span className="text-green-400">- {formatCurrency(booking.discount)}</span>
              </div>
            )}

            <div className="h-px bg-white/5 my-2" />
            
            <div className="flex justify-between items-end">
              <div>
                <p className="text-[10px] text-gray-500 uppercase tracking-wider font-bold mb-0.5">Tổng cộng</p>
                <p className="text-white text-xs font-medium opacity-50">
                  Đã bao gồm thuế GTGT
                </p>
              </div>
              <div className="text-right">
                {isLoadingQuote ? (
                   <div className="w-32 h-8 bg-brand-500/20 rounded-lg animate-pulse ml-auto" />
                ) : (
                  <span className="text-gold-400 text-2xl font-display font-bold">
                    {formatCurrency(booking.total)}
                  </span>
                )}
              </div>
            </div>
          </div>

        </div>
      </div>

      {/* Bottom CTA */}
      <div className="fixed bottom-0 left-0 right-0 p-4 glass-dark border-t border-white/8">
        <div className="max-w-2xl mx-auto">
          <button onClick={booking.confirmBooking} disabled={booking.isConfirming || isLoadingQuote}
            className="btn-primary w-full py-4 text-base disabled:opacity-60">
            {booking.isConfirming ? (
              <span className="flex items-center gap-2">
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                Đang xử lý...
              </span>
            ) : isLoadingQuote ? (
              <span className="flex items-center gap-2">
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                Đang kiểm tra giá...
              </span>
            ) : (
              <span className="flex items-center gap-2">
                Thanh toán {formatCurrency(booking.total)}
                <ArrowRight className="w-5 h-5" />
              </span>
            )}
          </button>
        </div>
      </div>

      <VoucherModal 
        isOpen={isVoucherModalOpen}
        onClose={() => setIsVoucherModalOpen(false)}
        vouchers={userVouchers}
        cartTotal={cartTotalAmount}
        onSelectVoucher={(v) => { booking.applyVoucher(v); setVoucherError(''); }}
        onManualClaim={async (code) => {
          try {
             // Directly validate with Backend and apply
             const v = await voucherApi.validate(code);
             booking.applyVoucher(v);
             setVoucherError('');
             setIsVoucherModalOpen(false);
          } catch (err) {
             setVoucherError(err?.response?.data?.message || 'Mã không hợp lệ hoặc đã hết hạn');
             setIsVoucherModalOpen(false);
          }
        }}
      />
    </div>
  )
}
