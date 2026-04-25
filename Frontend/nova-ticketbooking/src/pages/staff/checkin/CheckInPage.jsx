import { useState, useEffect, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useMutation } from '@tanstack/react-query'
import { Html5Qrcode } from 'html5-qrcode'
import { QrCode, CheckCircle2, XCircle, RotateCcw, Scan } from 'lucide-react'
import { bookingApi } from '@/api/endpoints'
import { formatDateTime, cn } from '@/utils'

export default function CheckInPage() {
  const [scanning, setScanning]   = useState(false)
  const [result, setResult]       = useState(null)
  const [error, setError]         = useState('')
  const scannerRef = useRef(null)

  const verifyMutation = useMutation({
    mutationFn: bookingApi.verifyQr,
    onSuccess: setResult,
    onError: () => setError('QR code không hợp lệ hoặc đã được sử dụng'),
  })

  const checkInMutation = useMutation({
    mutationFn: (id) => bookingApi.checkIn(id),
    onSuccess: (data) => setResult(data),
  })

  const startScan = async () => {
    setResult(null); setError('')
    const scanner = new Html5Qrcode('qr-reader')
    scannerRef.current = scanner
    try {
      await scanner.start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: { width: 250, height: 250 } },
        (text) => {
          scanner.stop()
          setScanning(false)
          verifyMutation.mutate(text)
        },
        undefined
      )
      setScanning(true)
    } catch {
      setError('Không thể truy cập camera')
    }
  }

  const stopScan = () => {
    scannerRef.current?.stop()
    setScanning(false)
  }

  const reset = () => {
    setResult(null); setError(''); stopScan()
  }

  return (
    <div className="max-w-lg mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 font-display">Check-in QR</h1>
        <p className="text-gray-500 text-sm mt-1">Quét mã QR trên vé để xác nhận khách vào rạp</p>
      </div>

      {/* Scanner */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        <div id="qr-reader" className={cn(!scanning && 'hidden')} />

        {!scanning && !result && !verifyMutation.isPending && (
          <div className="flex flex-col items-center justify-center p-12 gap-4">
            <div className="w-20 h-20 rounded-2xl bg-blue-50 flex items-center justify-center">
              <QrCode className="w-10 h-10 text-blue-500" />
            </div>
            <p className="text-gray-500 text-sm text-center">
              Nhấn nút bên dưới để bật camera và quét mã QR
            </p>
            <button onClick={startScan}
              className="flex items-center gap-2 px-6 py-3 rounded-xl bg-blue-500
                hover:bg-blue-600 text-white font-semibold transition-all shadow-glow-blue">
              <Scan className="w-5 h-5" /> Bắt đầu quét
            </button>
          </div>
        )}

        {verifyMutation.isPending && (
          <div className="flex items-center justify-center p-12">
            <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {scanning && (
          <div className="p-4 border-t border-gray-100 flex justify-center">
            <button onClick={stopScan}
              className="px-4 py-2 rounded-xl bg-red-50 text-red-500 text-sm hover:bg-red-100 transition-all">
              Dừng quét
            </button>
          </div>
        )}
      </div>

      {/* Error */}
      <AnimatePresence>
        {error && (
          <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}
            className="flex items-center gap-3 p-4 rounded-2xl bg-red-50 border border-red-200">
            <XCircle className="w-5 h-5 text-red-500 flex-shrink-0" />
            <p className="text-red-700 text-sm">{error}</p>
            <button onClick={reset} className="ml-auto"><RotateCcw className="w-4 h-4 text-red-400" /></button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Result */}
      <AnimatePresence>
        {result && (
          <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }}
            className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 space-y-4">
            <div className="flex items-center gap-3">
              <div className={cn('w-10 h-10 rounded-xl flex items-center justify-center',
                result.status === 'PAID' ? 'bg-green-100' : 'bg-amber-100')}>
                <CheckCircle2 className={cn('w-6 h-6',
                  result.status === 'PAID' ? 'text-green-500' : 'text-amber-500')} />
              </div>
              <div>
                <p className="font-bold text-gray-900">{result.movieTitle}</p>
                <p className="text-sm text-gray-500">{result.bookingCode}</p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 text-sm">
              {[
                ['Rạp', result.cinemaName],
                ['Phòng', result.screenName],
                ['Suất chiếu', formatDateTime(result.startTime)],
                ['Ghế', result.seats.map(s => `${s.rowLabel}${s.colNumber}`).join(', ')],
              ].map(([k, v]) => (
                <div key={k} className="p-3 rounded-xl bg-gray-50">
                  <p className="text-gray-400 text-xs mb-0.5">{k}</p>
                  <p className="text-gray-800 font-medium">{v}</p>
                </div>
              ))}
            </div>

            <div className="flex gap-3">
              {result.status === 'PAID' && (
                <button onClick={() => checkInMutation.mutate(result.id)}
                  disabled={checkInMutation.isPending}
                  className="flex-1 flex items-center justify-center gap-2 py-3
                    rounded-xl bg-green-500 hover:bg-green-600 text-white font-semibold
                    transition-all disabled:opacity-50">
                  {checkInMutation.isPending
                    ? <div className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />
                    : <CheckCircle2 className="w-5 h-5" />}
                  Xác nhận check-in
                </button>
              )}
              <button onClick={reset}
                className="px-4 py-3 rounded-xl border border-gray-200 text-gray-600
                  hover:bg-gray-50 transition-all text-sm">
                <RotateCcw className="w-4 h-4" />
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
