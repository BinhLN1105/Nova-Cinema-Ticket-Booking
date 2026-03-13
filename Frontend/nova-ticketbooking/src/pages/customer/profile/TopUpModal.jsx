import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { X, CreditCard, Loader2 } from 'lucide-react'
import { walletApi } from '@/api/endpoints'
import toast from 'react-hot-toast'
import { formatCurrency } from '@/utils'

export function TopUpModal({ isOpen, onClose }) {
  const [amount, setAmount] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const handleTopUp = async (e) => {
    e.preventDefault()
    const numAmount = Number(amount)
    if (isNaN(numAmount) || numAmount < 10000) {
      toast.error('Số tiền tối thiểu là 10.000 VNĐ')
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

  const quickAmounts = [50000, 100000, 200000, 500000]

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

              <form onSubmit={handleTopUp} className="p-6">
                <div className="mb-6">
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

                <div className="grid grid-cols-2 gap-3 mb-6">
                  {quickAmounts.map((amt) => (
                    <button
                      key={amt}
                      type="button"
                      onClick={() => setAmount(String(amt))}
                      className={`py-2 rounded-xl text-sm font-medium transition-colors ${
                        amount === String(amt)
                          ? 'bg-brand-500 text-white'
                          : 'bg-cinema-800 text-cinema-300 hover:bg-white/5 border border-white/5'
                      }`}
                    >
                      {formatCurrency(amt)}
                    </button>
                  ))}
                </div>

                <div className="flex justify-end gap-3 pt-4 border-t border-white/5">
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
                    Thanh toán qua VNPay
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        </>
      )}
    </AnimatePresence>
  )
}
