import { useEffect, useState } from 'react'
import { useSearchParams, useNavigate, Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { CheckCircle2, XCircle, ArrowRight, Gift } from 'lucide-react'

export default function GiftCardResultPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [status, setStatus] = useState('processing') // processing, success, failed

  useEffect(() => {
    const code = searchParams.get('vnp_ResponseCode')
    if (code) {
      if (code === '00') {
        setStatus('success')
      } else {
        setStatus('failed')
      }
    } else {
      // Nếu không có param, chuyển hướng về trang mua thẻ
      navigate('/gift-cards')
    }
  }, [searchParams, navigate])

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-12 flex items-center justify-center">
      <div className="max-w-md w-full mx-auto px-4">
        
        {status === 'processing' && (
          <div className="text-center">
            <div className="w-16 h-16 border-4 border-brand-500 border-t-transparent rounded-full animate-spin mx-auto mb-6"></div>
            <h2 className="text-2xl font-bold text-white mb-2">Đang xử lý kết quả...</h2>
            <p className="text-cinema-200">Vui lòng đợi trong giây lát</p>
          </div>
        )}

        {status === 'success' && (
          <motion.div 
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="glass-dark rounded-3xl p-8 text-center border border-green-500/20 shadow-[0_0_50px_rgba(34,197,94,0.1)]"
          >
            <div className="w-20 h-20 bg-green-500/20 rounded-full flex items-center justify-center mx-auto mb-6">
              <CheckCircle2 className="w-10 h-10 text-green-400" />
            </div>
            
            <h2 className="font-display text-3xl font-bold text-white mb-4">
              Mua thẻ thành công!
            </h2>
            <p className="text-cinema-200 mb-8 leading-relaxed">
              Bạn đã mua thành công Thẻ Quà Tặng CinePoint. Mã thẻ đã được lưu trong tài khoản của bạn.
            </p>

            <div className="space-y-3">
              <Link to="/profile" className="btn-primary w-full py-3.5 justify-center flex items-center gap-2">
                <Gift className="w-5 h-5" />
                Xem tủ thẻ của tôi
              </Link>
              <Link to="/" className="btn-ghost w-full py-3.5 justify-center flex items-center gap-2">
                Về trang chủ
              </Link>
            </div>
          </motion.div>
        )}

        {status === 'failed' && (
          <motion.div 
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="glass-dark rounded-3xl p-8 text-center border border-red-500/20 shadow-[0_0_50px_rgba(239,68,68,0.1)]"
          >
            <div className="w-20 h-20 bg-red-500/20 rounded-full flex items-center justify-center mx-auto mb-6">
              <XCircle className="w-10 h-10 text-red-400" />
            </div>
            
            <h2 className="font-display text-3xl font-bold text-white mb-4">
              Giao dịch thất bại
            </h2>
            <p className="text-cinema-200 mb-8 leading-relaxed">
              Rất tiếc, giao dịch mua thẻ quà tặng của bạn không thành công hoặc đã bị hủy.
              Vui lòng thử lại sau.
            </p>

            <div className="space-y-3">
              <Link to="/gift-cards" className="btn-primary w-full py-3.5 justify-center flex items-center gap-2">
                Thử lại
                <ArrowRight className="w-4 h-4 ml-1" />
              </Link>
              <Link to="/" className="btn-ghost w-full py-3.5 justify-center flex items-center gap-2">
                Về trang chủ
              </Link>
            </div>
          </motion.div>
        )}

      </div>
    </div>
  )
}
