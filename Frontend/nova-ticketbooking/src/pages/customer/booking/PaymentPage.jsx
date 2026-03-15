import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Shield, CreditCard, Wallet, Building2, CheckCircle2, Coins, Loader2 } from 'lucide-react'
import { bookingApi } from '@/api/endpoints'
import { useAuth } from '@/hooks'
import { formatCurrency } from '@/utils'
import toast from 'react-hot-toast'

export default function PaymentPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [selectedMethod, setSelectedMethod] = useState('wallet')
  const [isPaying, setIsPaying] = useState(false)

  const { data: booking } = useQuery({
    queryKey: ['booking', id],
    queryFn: () => bookingApi.getById(id),
    enabled: !!id,
  })

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [])

  const handlePayment = async () => {
    if (!id || isPaying) return
    setIsPaying(true)
    try {
      if (selectedMethod === 'vnpay') {
        const payment = await bookingApi.createPayment(id)
        window.location.href = payment.paymentUrl
      } else if (selectedMethod === 'wallet') {
        await bookingApi.payWithWallet(id)
        toast.success('Thanh toán bằng CinePoint thành công!')
        navigate('/booking/result?status=success')
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
  const pointsNeeded = booking ? Math.floor(booking.totalAmount / 1000) : 0
  const hasEnoughBalance = walletBalance >= pointsNeeded

  const paymentMethods = [
    { id: 'wallet', name: 'Ví CinePoint', icon: Coins, color: 'text-yellow-400',
      desc: `Số dư: ${walletBalance?.toLocaleString('vi-VN') || 0} CP${!hasEnoughBalance ? ' (Không đủ)' : ''}`,
      disabled: !hasEnoughBalance },
    { id: 'vnpay', name: 'VNPay', icon: CreditCard, color: 'text-blue-400', desc: 'Thanh toán qua ví VNPay hoặc quét mã QR' },
    { id: 'momo', name: 'MoMo', icon: Wallet, color: 'text-pink-500', desc: 'Thanh toán bằng ví điện tử MoMo' },
    { id: 'bank', name: 'Thẻ ngân hàng', icon: Building2, color: 'text-green-400', desc: 'Thẻ ATM nội địa / Visa / Mastercard' }
  ]

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-16 flex items-start justify-center">
      <div className="max-w-md w-full px-4 sm:px-6">
        <h1 className="font-display text-3xl font-bold text-white mb-2 text-center">Thanh toán</h1>
        <p className="text-cinema-300 text-center mb-8">Chọn phương thức thanh toán</p>

        <div className="card-cinema p-5 mb-4 space-y-3">
          {booking && (
            <>
              <div className="flex justify-between text-sm">
                <span className="text-cinema-400">Mã đặt vé</span>
                <span className="text-white font-mono font-bold">{booking.bookingCode}</span>
              </div>
              <div className="flex justify-between font-bold text-lg">
                <span className="text-white">Tổng tiền</span>
                <span className="text-brand-400">
                  {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(booking.totalAmount)}
                </span>
              </div>
            </>
          )}
        </div>

        <div className="card-cinema p-3 mb-6 space-y-2">
          <p className="px-2 text-sm text-gray-400 font-medium mb-2">Chọn phương thức thanh toán</p>
          {paymentMethods.map((method) => {
            const Icon = method.icon
            const isSelected = selectedMethod === method.id
            return (
              <button key={method.id} onClick={() => !method.disabled && setSelectedMethod(method.id)}
                disabled={method.disabled}
                className={`w-full flex items-center justify-between p-4 rounded-xl border transition-all
                  ${method.disabled ? 'opacity-50 cursor-not-allowed glass-dark border-white/5' :
                    isSelected ? 'bg-brand-500/10 border-brand-500 shadow-glow-red' : 'glass-dark border-white/5 hover:border-white/20'}`}>
                <div className="flex items-center gap-4">
                  <div className={`p-2 rounded-lg bg-white/5 ${method.color}`}>
                    <Icon className="w-6 h-6" />
                  </div>
                  <div className="text-left">
                    <p className={`font-semibold ${isSelected ? 'text-white' : 'text-gray-200'}`}>{method.name}</p>
                    <p className="text-xs text-gray-400">{method.desc}</p>
                  </div>
                </div>
                {isSelected && <CheckCircle2 className="w-5 h-5 text-brand-500" />}
              </button>
            )
          })}
        </div>

        <button onClick={handlePayment} disabled={isPaying}
          className="w-full flex items-center justify-center gap-3 p-5 rounded-2xl
          bg-brand-600 hover:bg-brand-500 border border-brand-500 transition-all
          text-white font-semibold text-base shadow-glow-red disabled:opacity-60 disabled:cursor-not-allowed">
          {isPaying ? (
            <><Loader2 className="w-5 h-5 animate-spin" /> Đang xử lý...</>
          ) : (
            `Tiến hành thanh toán${selectedMethod === 'wallet' ? ` (${pointsNeeded} điểm)` : ''}`
          )}
        </button>

        <div className="flex items-center justify-center gap-2 mt-6 text-cinema-400 text-xs">
          <Shield className="w-4 h-4" />
          Thanh toán được mã hóa và bảo mật
        </div>
      </div>
    </div>
  )
}
