import { useEffect, useState, useRef } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { CheckCircle, XCircle, Loader2 } from 'lucide-react'
import { bookingApi } from '@/api/endpoints'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/authStore'
import { useAuth } from '@/hooks'

export default function CancelConfirmPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const token = searchParams.get('token')
  const bookingId = searchParams.get('bookingId')
  const { user } = useAuth()
  const setUser = useAuthStore(s => s.setUser)

  const [status, setStatus] = useState('loading') // loading, success, error
  const [errorMessage, setErrorMessage] = useState('')
  const hasCalled = useRef(false)

  useEffect(() => {
    if (!token || !bookingId || hasCalled.current) return
    hasCalled.current = true

    const confirmCancel = async () => {
      try {
        await bookingApi.cancelConfirm(token, bookingId)
        
        // Fetch latest user profile to update CinePoints
        try {
          const updatedUser = await api.get('/auth/me')
          setUser(updatedUser)
        } catch (profileErr) {
          console.error('Failed to refresh profile:', profileErr)
        }

        setStatus('success')
      } catch (err) {
        setStatus('error')
        setErrorMessage(err.response?.data?.message || 'Có lỗi xảy ra khi xác nhận huỷ vé')
      }
    }

    confirmCancel()
  }, [token, bookingId, setUser])

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-16 flex items-center justify-center">
      <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }}
        className="card-cinema p-8 max-w-md w-full text-center mx-4">
        
        {status === 'loading' && (
          <div className="flex flex-col items-center">
            <Loader2 className="w-16 h-16 text-brand-500 animate-spin mb-4" />
            <h2 className="text-xl font-bold text-white mb-2">Đang xác nhận...</h2>
            <p className="text-cinema-400">Vui lòng chờ trong giây lát</p>
          </div>
        )}

        {status === 'success' && (
          <div className="flex flex-col items-center">
            <div className="w-20 h-20 rounded-full bg-green-500/20 flex items-center justify-center mb-6">
              <CheckCircle className="w-10 h-10 text-green-500" />
            </div>
            <h2 className="text-2xl font-bold text-white mb-3">Huỷ vé thành công!</h2>
            <p className="text-cinema-300 mb-8">
              Vé của bạn đã được huỷ. CinePoint tương ứng đã được hoàn vào tài khoản của bạn.
            </p>
            <button onClick={() => navigate('/customer/profile')}
              className="btn bg-brand-500 hover:bg-brand-600 text-white w-full py-3 rounded-xl font-medium">
              Kiểm tra CinePoint
            </button>
          </div>
        )}

        {status === 'error' && (
          <div className="flex flex-col items-center">
            <div className="w-20 h-20 rounded-full bg-red-500/20 flex items-center justify-center mb-6">
              <XCircle className="w-10 h-10 text-red-500" />
            </div>
            <h2 className="text-2xl font-bold text-white mb-3">Xác nhận thất bại</h2>
            <p className="text-cinema-300 mb-8">{errorMessage}</p>
            <button onClick={() => navigate('/customer/profile')}
              className="btn border border-cinema-700 hover:bg-cinema-800 text-white w-full py-3 rounded-xl font-medium">
              Về trang cá nhân
            </button>
          </div>
        )}

      </motion.div>
    </div>
  )
}
