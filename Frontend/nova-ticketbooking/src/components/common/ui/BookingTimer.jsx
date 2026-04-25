import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Timer, AlertCircle } from 'lucide-react'
import { useBookingStore } from '@/stores/bookingStore'
import { cn } from '@/utils'
import toast from 'react-hot-toast'

export default function BookingTimer() {
  const navigate = useNavigate()
  const { expiryTime, reset } = useBookingStore()
  const [timeLeft, setTimeLeft] = useState('')
  const [isLowTime, setIsLowTime] = useState(false)

  useEffect(() => {
    if (!expiryTime) return

    const calculateTimeLeft = () => {
      const difference = expiryTime - Date.now()
      
      if (difference <= 0) {
        // Hết hạn giữu ghế
        reset()
        toast.error('Hết thời gian giữ ghế. Vui lòng đặt lại!', {
          icon: '⏳',
          duration: 5000
        })
        navigate('/')
        return null
      }

      const minutes = Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60))
      const seconds = Math.floor((difference % (1000 * 60)) / 1000)

      setIsLowTime(minutes < 2)
      return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
    }

    const timer = setInterval(() => {
      const time = calculateTimeLeft()
      if (time) setTimeLeft(time)
    }, 1000)

    // Chạy lần đầu ngay lập tức
    const initialTime = calculateTimeLeft()
    if (initialTime) setTimeLeft(initialTime)

    return () => clearInterval(timer)
  }, [expiryTime, navigate, reset])

  if (!expiryTime || !timeLeft) return null

  return (
    <div className={cn(
      "w-full py-2 px-4 rounded-xl flex items-center justify-center gap-3 transition-all duration-300",
      isLowTime 
        ? "bg-brand-500/10 border border-brand-500/30 text-brand-400 animate-pulse" 
        : "bg-cinema-800/50 border border-white/5 text-cinema-200"
    )}>
      {isLowTime ? <AlertCircle className="w-4 h-4" /> : <Timer className="w-4 h-4" />}
      <span className="text-sm font-medium tracking-tight">
        Ghế của bạn sẽ được giữ trong: <span className="font-mono font-bold text-base ml-1">{timeLeft}</span>
      </span>
    </div>
  )
}
