import { useEffect } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { CheckCircle2, XCircle, Ticket, Home } from 'lucide-react'
import { useBookingStore } from '@/stores/bookingStore'
import { useAuthStore } from '@/stores/authStore'
import { authApi } from '@/api/endpoints'

export default function BookingResult() {
  const [params] = useSearchParams()
  const { reset } = useBookingStore()
  const status = params.get('status') ?? 'success'
  const { setUser } = useAuthStore()
  const isSuccess = status === 'success' || status === '00'

  useEffect(() => { 
    if (isSuccess) {
      reset()
      // Refresh profile to update CP balance - Delay 500ms to ensure DB commit is visible
      const timer = setTimeout(() => {
        authApi.me().then(user => {
          setUser(user)
        }).catch(err => console.error('Failed to refresh profile:', err))
      }, 500)
      return () => clearTimeout(timer)
    } 
  }, [isSuccess])

  return (
    <div className="min-h-screen bg-cinema-900 flex items-center justify-center px-4">
      <motion.div initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }}
        transition={{ type: 'spring', duration: 0.6 }}
        className="text-center max-w-sm">
        {isSuccess ? (
          <>
            <motion.div initial={{ scale: 0 }} animate={{ scale: 1 }}
              transition={{ delay: 0.2, type: 'spring', stiffness: 200 }}
              className="w-24 h-24 rounded-full bg-green-500/15 border-2 border-green-500/40
                flex items-center justify-center mx-auto mb-6">
              <CheckCircle2 className="w-12 h-12 text-green-400" />
            </motion.div>
            <h1 className="font-display text-3xl font-bold text-white mb-3">Đặt vé thành công!</h1>
            <p className="text-cinema-300 mb-8">Vé điện tử đã được gửi vào email của bạn. Chúc bạn xem phim vui vẻ!</p>
            <div className="flex gap-3 justify-center">
              <Link to="/tickets" className="btn-primary px-6 py-3">
                <Ticket className="w-4 h-4" /> Xem vé
              </Link>
              <Link to="/" className="btn-ghost px-6 py-3">
                <Home className="w-4 h-4" /> Trang chủ
              </Link>
            </div>
          </>
        ) : (
          <>
            <div className="w-24 h-24 rounded-full bg-brand-500/15 border-2 border-brand-500/40
              flex items-center justify-center mx-auto mb-6">
              <XCircle className="w-12 h-12 text-brand-400" />
            </div>
            <h1 className="font-display text-3xl font-bold text-white mb-3">Thanh toán thất bại</h1>
            <p className="text-cinema-300 mb-8">Giao dịch không thành công. Vé của bạn vẫn được giữ trong 10 phút.</p>
            <Link to="/tickets" className="btn-primary px-6 py-3">Thử lại</Link>
          </>
        )}
      </motion.div>
    </div>
  )
}
