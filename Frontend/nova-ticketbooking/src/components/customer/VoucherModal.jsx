import { useState, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { X, Gift } from 'lucide-react'
import { formatCurrency, formatDate, calculateActualDiscount, cn } from '@/utils'

export function VoucherModal({ isOpen, onClose, vouchers, cartTotal, onSelectVoucher, onManualClaim }) {
  const [manualCode, setManualCode] = useState('')

  const { availableVouchers, disabledVouchers } = useMemo(() => {
    if (!vouchers?.length) return { availableVouchers: [], disabledVouchers: [] }

    const available = []
    const disabled = []

    vouchers.forEach(v => {
      // Check base status
      if (v.status !== 'AVAILABLE') {
        disabled.push({ ...v, reason: 'Chỉ áp dụng với mã khả dụng / Còn hạn.' })
        return
      }

      // Check min order
      if (v.minOrder && cartTotal < v.minOrder) {
        disabled.push({ ...v, reason: `Đơn tối thiểu ${formatCurrency(v.minOrder)}.` })
        return
      }

      // Khả dụng - tính discount thực tế luôn
      const actualDiscount = calculateActualDiscount(cartTotal, v);
      available.push({ ...v, actualDiscount })
    })

    // Sort by actual discount DESC
    available.sort((a, b) => b.actualDiscount - a.actualDiscount)

    return { availableVouchers: available, disabledVouchers: disabled }
  }, [vouchers, cartTotal])

  const handleManualClaim = () => {
    if (manualCode.trim()) {
      onManualClaim(manualCode.trim())
      setManualCode('')
    }
  }

  if (!isOpen) return null

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex flex-col justify-end sm:justify-center items-center bg-black/60 backdrop-blur-sm p-4">
        <motion.div
          initial={{ opacity: 0, y: 50 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: 20 }}
          className="bg-cinema-900 w-full max-w-md rounded-3xl overflow-hidden border border-white/10 shadow-2xl flex flex-col max-h-[90vh]"
        >
          {/* Header */}
          <div className="p-5 border-b border-white/10 flex items-center justify-between bg-cinema-800">
            <h3 className="font-display font-bold text-lg text-white flex items-center gap-2">
              <Gift className="w-5 h-5 text-brand-400" /> Chọn Voucher
            </h3>
            <button onClick={onClose} className="p-2 -mr-2 text-cinema-400 hover:text-white transition-colors">
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Input Section */}
          <div className="p-5 bg-cinema-900/50">
            <div className="flex gap-3">
              <input
                value={manualCode}
                onChange={e => setManualCode(e.target.value.toUpperCase())}
                placeholder="Nhập mã ưu đãi..."
                className="flex-1 bg-cinema-800 rounded-xl px-4 text-sm text-white placeholder-cinema-500 border border-white/5 focus:border-brand-500/50 focus:outline-none"
              />
              <button
                onClick={handleManualClaim}
                className="bg-brand-500 hover:bg-brand-600 px-4 py-2 rounded-xl text-white text-sm font-bold transition-colors"
              >
                Áp dụng
              </button>
            </div>
          </div>

          {/* Lists */}
          <div className="flex-1 overflow-y-auto p-5 scrollbar-hide space-y-6">

            {/* Khả Dụng*/}
            <div>
              <p className="text-xs font-bold uppercase tracking-wider text-green-400 mb-3">Khả dụng ({availableVouchers.length})</p>
              <div className="space-y-3">
                {/* Sửa lỗi interactive element (thay thẻ tĩnh bằng nút).- từ dòng 92 -> 114 */}
                {availableVouchers.map(v => (
                  <button
                    key={v.id}
                    type="button"
                    onClick={() => { onSelectVoucher(v); onClose() }}
                    className="w-full text-left block group border border-green-500/30 bg-green-500/5 hover:bg-green-500/10 rounded-xl p-4 cursor-pointer transition-all relative overflow-hidden outline-none"
                  >
                    <div className="absolute top-0 right-0 bottom-0 w-1 bg-green-500" />
                    <div className="flex justify-between items-start gap-4">
                      <div>
                        <p className="text-white font-bold">{v.description}</p>
                        <span className="text-[10px] text-green-400 font-mono tracking-widest uppercase bg-green-500/20 px-1.5 py-0.5 rounded mt-1 inline-block">
                          {v.code}
                        </span>
                      </div>
                      <div className="text-right flex-shrink-0">
                        <p className="text-green-400 font-bold text-lg leading-none">-{formatCurrency(v.actualDiscount)}</p>
                        <p className="text-[10px] text-cinema-400 mt-1">HSD: {formatDate(v.endDate)}</p>
                      </div>
                    </div>
                  </button>
                ))}
                {availableVouchers.length === 0 && (
                  <p className="text-sm text-cinema-500 text-center italic py-2">Không có mã nào khả dụng cho đơn hàng này.</p>
                )}
              </div>
            </div>

            {/* Không đủ điều kiện */}
            {disabledVouchers.length > 0 && (
              <div>
                <p className="text-xs font-bold uppercase tracking-wider text-cinema-500 mb-3">Chưa đủ điều kiện ({disabledVouchers.length})</p>
                <div className="space-y-3">
                  {disabledVouchers.map(v => (
                    <div key={v.id} className="border border-white/5 bg-cinema-800/50 rounded-xl p-4 opacity-50 relative overflow-hidden">
                      <div className="flex justify-between items-start gap-4 mb-2">
                        <div>
                          <p className="text-cinema-300 font-bold">{v.description}</p>
                          <span className="text-[10px] text-cinema-400 font-mono tracking-widest uppercase mt-1 inline-block">
                            {v.code}
                          </span>
                        </div>
                        <div className="text-right flex-shrink-0">
                          <p className="text-cinema-400 font-bold text-sm">
                            {v.discountType === 'PERCENTAGE' ? `${v.discountValue}%` : formatCurrency(v.discountValue)}
                          </p>
                        </div>
                      </div>
                      <p className="text-[11px] text-brand-400/80 bg-brand-500/10 px-2 py-1 rounded inline-block">
                        {v.reason}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            )}

          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
