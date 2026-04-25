import { useState } from 'react'
import { motion } from 'framer-motion'
import { Gift, CreditCard, ChevronRight, Wallet, Building2, CheckCircle2 } from 'lucide-react'
import { useAuthStore } from '@/stores/authStore'
import { giftCardApi } from '@/api/endpoints'
import { toast } from 'react-hot-toast'
import { formatCurrency, cn } from '@/utils'

const PRESET_AMOUNTS = [50000, 100000, 200000, 500000, 1000000]

export default function GiftCardPage() {
  const { user } = useAuthStore()
  const [selectedAmount, setSelectedAmount] = useState(100000)
  const [selectedMethod, setSelectedMethod] = useState('vnpay')
  const [isProcessing, setIsProcessing] = useState(false)

  const paymentMethods = [
    { id: 'vnpay', name: 'VNPay', icon: CreditCard, color: 'text-blue-400', desc: 'Thanh toán qua ví VNPay hoặc quét mã QR' },
    { id: 'momo', name: 'MoMo', icon: Wallet, color: 'text-pink-500', desc: 'Thanh toán bằng ví điện tử MoMo' },
    { id: 'bank', name: 'Thẻ ngân hàng', icon: Building2, color: 'text-green-400', desc: 'Thẻ ATM nội địa / Visa / Mastercard' }
  ]

  const handleBuy = async () => {
    if (!user) {
      toast.error('Vui lòng đăng nhập để mua thẻ quà tặng')
      return
    }

    if (selectedAmount < 50000) {
      toast.error('Mệnh giá tối thiểu là 50.000đ')
      return
    }

    if (selectedMethod !== 'vnpay') {
      toast('Tính năng này đang được phát triển', { icon: '🚧' })
      return
    }

    setIsProcessing(true)
    try {
      const returnUrlBase = window.location.origin
      const data = await giftCardApi.buy(selectedAmount, returnUrlBase)
      
      if (data?.paymentUrl) {
        window.location.href = data.paymentUrl
      }
    } catch (error) {
      toast.error('Có lỗi xảy ra khi tạo giao dịch')
      console.error(error)
    } finally {
      setIsProcessing(false)
    }
  }

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-12">
      <div className="max-w-3xl mx-auto px-4 sm:px-6">
        
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-10"
        >
          <div className="w-16 h-16 rounded-2xl bg-brand-500/20 text-brand-400 font-bold mx-auto flex items-center justify-center mb-4 border border-brand-500/30">
            <Gift className="w-8 h-8" />
          </div>
          <h1 className="font-display text-3xl md:text-4xl font-bold text-white mb-4">
            Thẻ Quà Tặng CinePoint
          </h1>
          <p className="text-cinema-200 text-lg max-w-xl mx-auto">
            Mua thẻ e-voucher để tự nạp điểm CinePoint hoặc dành tặng bạn bè. Chiết khấu hấp dẫn, điểm không bao giờ hết hạn.
          </p>
        </motion.div>

        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="glass-dark rounded-3xl p-6 sm:p-10 border border-white/10"
        >
          {/* Card Preview */}
          <div className="relative aspect-[1.6/1] max-w-md mx-auto rounded-2xl overflow-hidden mb-10 shadow-[0_20px_50px_rgba(233,69,96,0.3)]">
            <div className="absolute inset-0 bg-gradient-to-br from-brand-600 to-brand-900"></div>
            <div className="absolute inset-0 bg-[url('https://www.transparenttextures.com/patterns/cubes.png')] opacity-20 mix-blend-overlay"></div>
            <div className="relative h-full flex flex-col justify-between p-6 sm:p-8">
              <div className="flex justify-between items-start">
                <div>
                  <h3 className="text-white/80 font-medium uppercase tracking-wider text-sm">E-Voucher</h3>
                  <p className="font-display text-2xl font-bold text-white mt-1">Nova Cinema</p>
                </div>
                <div className="w-12 h-8 rounded bg-white/20 backdrop-blur-sm border border-white/30 flex items-center justify-center">
                  <CreditCard className="w-5 h-5 text-white/80" />
                </div>
              </div>
              
              <div>
                <p className="text-white/60 text-sm mb-1">Mệnh giá & Điểm nhận được</p>
                <div className="flex items-end gap-2 text-white">
                  <span className="text-4xl font-bold font-display leading-none">
                    {formatCurrency(selectedAmount)}
                  </span>
                  <span className="text-lg font-medium text-white/80 mb-1">
                    = {Math.floor(selectedAmount / 1000)} CP
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div className="space-y-6 max-w-md mx-auto">
            <div>
              <label className="block text-sm font-medium text-cinema-200 mb-3">
                Chọn mệnh giá (VNĐ)
              </label>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                {PRESET_AMOUNTS.map(amount => (
                  <button
                    key={amount}
                    onClick={() => setSelectedAmount(amount)}
                    className={cn(
                      'py-3 rounded-xl border text-sm font-medium transition-all duration-200',
                      selectedAmount === amount
                        ? 'bg-brand-500/20 border-brand-500 text-brand-400 scale-[1.02]'
                        : 'bg-cinema-800/50 border-white/5 text-cinema-300 hover:bg-cinema-800 hover:border-white/10'
                    )}
                  >
                    {formatCurrency(amount)}
                  </button>
                ))}
              </div>
            </div>

            <div className="pt-4 border-t border-white/10">
              <label className="block text-sm font-medium text-cinema-200 mb-3">
                Phương thức thanh toán
              </label>
              <div className="space-y-2">
                {paymentMethods.map((method) => {
                  const Icon = method.icon
                  const isSelected = selectedMethod === method.id
                  return (
                    <button key={method.id} onClick={() => setSelectedMethod(method.id)}
                      className={`w-full flex items-center justify-between p-3 rounded-xl border transition-all
                        ${isSelected ? 'bg-brand-500/10 border-brand-500 shadow-glow-red' : 'glass-dark border-white/5 hover:border-white/20'}`}>
                      <div className="flex items-center gap-3">
                        <div className={`p-2 rounded-lg bg-white/5 ${method.color}`}>
                          <Icon className="w-5 h-5" />
                        </div>
                        <div className="text-left">
                          <p className={`font-semibold text-sm ${isSelected ? 'text-white' : 'text-gray-200'}`}>{method.name}</p>
                          <p className="text-xs text-gray-400">{method.desc}</p>
                        </div>
                      </div>
                      {isSelected && <CheckCircle2 className="w-5 h-5 text-brand-500" />}
                    </button>
                  )
                })}
              </div>
            </div>

            <div className="pt-4 mt-4 border-t border-white/10">
              <div className="flex justify-between items-center mb-4">
                <span className="text-cinema-200">Tổng thanh toán</span>
                <span className="text-2xl font-bold text-white">{formatCurrency(selectedAmount)}</span>
              </div>
              <button
                onClick={handleBuy}
                disabled={isProcessing}
                className="btn-primary w-full py-4 text-lg"
              >
                {isProcessing ? 'Đang xử lý...' : 'Thanh toán'}
              </button>
            </div>
          </div>

        </motion.div>
      </div>
    </div>
  )
}
