import { useState, useEffect } from 'react'

/**
 * Custom hook to calculate and format countdown timer for pending bookings
 * @param {string|Date|number} expiresAt - Expiration timestamp
 * @param {Function} [onExpired] - Callback invoked when timer hits 00:00
 * @returns {{ timeLeft: string, isExpired: boolean, isLowTime: boolean }}
 */
export function useBookingCountdown(expiresAt, onExpired) {
  const [timeLeft, setTimeLeft] = useState('')
  const [isExpired, setIsExpired] = useState(false)
  const [isLowTime, setIsLowTime] = useState(false)

  useEffect(() => {
    if (!expiresAt) return

    const targetTime = typeof expiresAt === 'string' ? new Date(expiresAt).getTime() : expiresAt

    const updateTimer = () => {
      const diff = targetTime - Date.now()
      if (diff <= 0) {
        setTimeLeft('00:00')
        setIsExpired(true)
        setIsLowTime(true)
        if (onExpired) onExpired()
        return false
      }
      const mins = Math.floor(diff / (1000 * 60))
      const secs = Math.floor((diff % (1000 * 60)) / 1000)
      setIsLowTime(mins < 2)
      setTimeLeft(`${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`)
      return true
    }

    if (!updateTimer()) return

    const timer = setInterval(() => {
      if (!updateTimer()) clearInterval(timer)
    }, 1000)

    return () => clearInterval(timer)
  }, [expiresAt])

  return { timeLeft, isExpired, isLowTime }
}
