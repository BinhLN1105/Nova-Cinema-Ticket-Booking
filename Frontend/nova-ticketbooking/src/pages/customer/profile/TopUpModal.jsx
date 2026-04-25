import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { X, CreditCard, Loader2, Wallet, Building2, CheckCircle2, Shield } from 'lucide-react'
import { walletApi } from '@/api/endpoints'
import toast from 'react-hot-toast'
import { formatCurrency, cn } from '@/utils'

const QUICK_AMOUNTS = [50000, 100000, 200000, 500000]

const PAYMENT_METHODS = [
  { id: 'vnpay', name: 'VNPay', icon: CreditCard, color: 'text-blue-400', desc: 'Thanh toán qua ví VNPay hoặc quét mã QR' },
  { id: 'momo', name: 'MoMo', icon: Wallet, color: 'text-pink-500', desc: 'Thanh toán bằng ví điện tử MoMo' },
  { id: 'bank', name: 'Thẻ ngân hàng', icon: Building2, color: 'text-green-400', desc: 'Thẻ ATM nội địa / Visa / Mastercard' },
]

export function TopUpModal({ isOpen, onClose }) {
  const [amount, setAmount] = useState('')
  const [selectedMethod, setSelectedMethod] = useState('vnpay')
  const [isLoading, setIsLoading] = useState(false)

  const handleTopUp = async (e) => {
    e.preventDefault()
    const numAmount = Number(amount)
    if (isNaN(numAmount) || numAmount < 10000) {
      toast.error('Số tiền tối thiểu là 10.000 VNĐ')
      return
    }

    if (selectedMethod !== 'vnpay') {
      toast('Tính năng này đang được phát triển', { icon: '🚧' })
      return
    }

    try {
      setIsLoading(true)
      const res = await walletApi.topup(numAmount)
      if (res?.paymentUrl) {
        window.location.href = res.paymentUrl
      } else {
        toast.error('Không thể tạo URL thanh toán VNPay')
      }
    } catch (error) {
      toast.error('Nạp điểm thất bại')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm"
          />
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 pointer-events-none">
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 10 }}
              className="bg-cinema-900 border border-white/10 rounded-2xl shadow-xl w-full max-w-md pointer-events-auto overflow-hidden"
            >
              <div className="p-4 border-b border-white/5 flex items-center justify-between">
                <h3 className="font-display font-bold text-white text-lg flex items-center gap-2">
                  <CreditCard className="w-5 h-5 text-brand-400" />
                  Nạp CinePoint
                </h3>
                <button
                  onClick={onClose}
                  className="p-1.5 rounded-lg text-cinema-400 hover:text-white hover:bg-white/5 transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <form onSubmit={handleTopUp} className="p-6 space-y-5">
                {/* Amount input */}
                <div>
                  <label className="block text-sm font-medium text-cinema-300 mb-2">
                    Số tiền cần nạp (VNĐ)
                  </label>
                  <input
                    type="number"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="VD: 100000"
                    className="w-full px-4 py-3 bg-cinema-800 border border-white/10 rounded-xl text-white focus:outline-none focus:border-brand-500 transition-colors placeholder:text-cinema-500"
                    min="10000"
                    step="1000"
                    required
                  />
                  <p className="text-cinema-400 text-xs mt-2">
                    1.000 VNĐ = 1 CinePoint (CP)
                  </p>
                </div>

                {/* Quick amounts */}
                <div className="grid grid-cols-2 gap-3">
                  {QUICK_AMOUNTS.map((amt) => (
                    <button
                      key={amt}
                      type="button"
                      onClick={() => setAmount(String(amt))}
                      className={cn(
                        'py-2.5 rounded-xl text-sm font-medium transition-all',
                        amount === String(amt)
                          ? 'bg-brand-500 text-white scale-[1.02]'
                          : 'bg-cinema-800 text-cinema-300 hover:bg-white/5 border border-white/5'
                      )}
                    >
                      {formatCurrency(amt)}
                    </button>
                  ))}
                </div>

                {/* Payment methods */}
                <div>
                  <label className="block text-sm font-medium text-cinema-300 mb-2">
                    Phương thức thanh toán
                  </label>
                  <div className="space-y-2">
                    {PAYMENT_METHODS.map((method) => {
                      const Icon = method.icon
                      const isSelected = selectedMethod === method.id
                      return (
                        <button
                          key={method.id}
                          type="button"
                          onClick={() => setSelectedMethod(method.id)}
                          className={cn(
                            'w-full flex items-center justify-between p-3 rounded-xl border transition-all',
                            isSelected
                              ? 'bg-brand-500/10 border-brand-500 shadow-glow-red'
                              : 'glass-dark border-white/5 hover:border-white/20'
                          )}
                        >
                          <div className="flex items-center gap-3">
                            <div className={`p-2 rounded-lg bg-white/5 ${method.color}`}>
                              <Icon className="w-5 h-5" />
                            </div>
                            <div className="text-left">
                              <p className={`font-semibold text-sm ${isSelected ? 'text-white' : 'text-gray-200'}`}>
                                {method.name}
                              </p>
                              <p className="text-xs text-gray-400">{method.desc}</p>
                            </div>
                          </div>
                          {isSelected && <CheckCircle2 className="w-5 h-5 text-brand-500 flex-shrink-0" />}
                        </button>
                      )
                    })}
                  </div>
                </div>

                {/* Actions */}
                <div className="flex justify-end gap-3 pt-3 border-t border-white/5">
                  <button
                    type="button"
                    onClick={onClose}
                    className="px-5 py-2.5 rounded-xl text-white font-medium hover:bg-white/5 transition-colors"
                  >
                    Hủy
                  </button>
                  <button
                    type="submit"
                    disabled={isLoading || !amount || Number(amount) < 10000}
                    className="px-5 py-2.5 rounded-xl bg-brand-500 text-white font-medium hover:bg-brand-600 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                  >
                    {isLoading && <Loader2 className="w-4 h-4 animate-spin" />}
                    Thanh toán
                  </button>
                </div>

                <div className="flex items-center justify-center gap-2 text-cinema-400 text-xs">
                  <Shield className="w-3.5 h-3.5" />
                  Thanh toán được mã hóa và bảo mật
                </div>
              </form>
            </motion.div>
          </div>
        </>
      )}
    </AnimatePresence>
  )
}
