import { useEffect } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { Loader2, Smartphone, Globe } from 'lucide-react'

export default function AppRedirect() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const token = searchParams.get('token')
  const bookingId = searchParams.get('bookingId')

  const deepLink = `cinema://cancel-confirm?token=${token}&bookingId=${bookingId}`

  useEffect(() => {
    // Tự động kích hoạt App ngay khi vào trang
    if (token && bookingId) {
      window.location.href = deepLink
    }
  }, [token, bookingId, deepLink])

  const handleManualOpen = () => {
    window.location.href = deepLink
  }

  const handleWebContinue = () => {
    navigate(`/booking/cancel-confirm?token=${token}&bookingId=${bookingId}`)
  }

  return (
    <div className="min-h-screen bg-[#0D1B2A] flex items-center justify-center p-6 font-sans">
      <div className="max-w-md w-full text-center space-y-8">
        <div className="flex justify-center">
          <div className="p-4 rounded-full bg-[#F5C518]/10">
            <Loader2 className="w-12 h-12 text-[#F5C518] animate-spin" />
          </div>
        </div>

        <div className="space-y-3">
          <h1 className="text-2xl font-bold text-white">Đang mở NovaTicket...</h1>
          <p className="text-gray-400">Nếu ứng dụng không tự mở, vui lòng nhấn nút bên dưới.</p>
        </div>

        <div className="grid gap-4 pt-8">
          <button 
            onClick={handleManualOpen}
            className="flex items-center justify-center gap-3 p-4 rounded-xl bg-[#F5C518] hover:bg-[#E5B518] text-black font-bold transition-all shadow-[0_0_20px_rgba(245,197,24,0.3)]"
          >
            <Smartphone className="w-5 h-5" />
            MỞ TRONG ỨNG DỤNG
          </button>

          <button 
            onClick={handleWebContinue}
            className="flex items-center justify-center gap-3 p-4 rounded-xl bg-white/5 hover:bg-white/10 text-gray-300 font-medium transition-all border border-white/10"
          >
            <Globe className="w-5 h-5" />
            Tiếp tục trên Web
          </button>
        </div>

        <div className="pt-12 text-xs text-gray-500 border-t border-white/5">
          <p>NovaTicket - Trải nghiệm điện ảnh đỉnh cao</p>
        </div>
      </div>
    </div>
  )
}
