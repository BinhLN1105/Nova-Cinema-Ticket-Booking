import { useState, useRef, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useMutation } from '@tanstack/react-query'
import {
  Upload, QrCode, CheckCircle2, XCircle,
  RotateCcw, ImageIcon, Keyboard,
} from 'lucide-react'
import jsQR from 'jsqr'
import { adminCheckInApi } from '@/api/endpoints'
import { formatDateTime, cn } from '@/utils'
import toast from 'react-hot-toast'

/* ── helpers ───────────────────────────────────────────────── */
function decodeQrFromFile(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const canvas = document.createElement('canvas')
        canvas.width = img.width
        canvas.height = img.height
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0)
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
        const code = jsQR(imageData.data, imageData.width, imageData.height)
        code ? resolve(code.data) : reject(new Error('Không đọc được QR code trong ảnh'))
      }
      img.onerror = () => reject(new Error('Ảnh không hợp lệ'))
      img.src = e.target.result
    }
    reader.readAsDataURL(file)
  })
}

/* ── component ──────────────────────────────────────────────── */
export default function AdminCheckInPage() {
  const [mode, setMode] = useState('upload')   // 'upload' | 'manual'
  const [preview, setPreview] = useState(null)
  const [manualCode, setManualCode] = useState('')
  const [result, setResult] = useState(null)
  const [decodeError, setDecodeError] = useState('')
  const fileInputRef = useRef(null)
  const dropRef = useRef(null)

  const checkInMutation = useMutation({
    mutationFn: adminCheckInApi.checkIn,
    onSuccess: (data) => {
      setResult({ ok: true, data: data?.data ?? data })
    },
    onError: (err) => {
      const msg = err?.response?.data?.message ?? 'QR code không hợp lệ hoặc đã được sử dụng'
      setResult({ ok: false, message: msg })
    },
  })

  const runCheckIn = useCallback((qrCode) => {
    setDecodeError('')
    setResult(null)
    checkInMutation.mutate(qrCode)
  }, [checkInMutation])

  const handleFile = useCallback(async (file) => {
    if (!file || !file.type.startsWith('image/')) {
      setDecodeError('Vui lòng chọn file ảnh hợp lệ')
      return
    }
    setPreview(URL.createObjectURL(file))
    setResult(null)
    setDecodeError('')
    try {
      const qrCode = await decodeQrFromFile(file)
      runCheckIn(qrCode)
    } catch (e) {
      setDecodeError(e.message)
    }
  }, [runCheckIn])

  const handleDrop = useCallback((e) => {
    e.preventDefault()
    const file = e.dataTransfer.files?.[0]
    handleFile(file)
  }, [handleFile])

  const reset = () => {
    setPreview(null); setResult(null)
    setDecodeError(''); setManualCode('')
    checkInMutation.reset()
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900 font-display">Check-in vé — Admin</h1>
        <p className="text-gray-500 text-sm mt-1">
          Tải ảnh QR code hoặc nhập mã booking để xác nhận khách vào rạp
        </p>
      </div>

      {/* Mode toggle */}
      <div className="flex gap-2 p-1 bg-gray-100 rounded-xl w-fit">
        {[
          { key: 'upload', icon: <Upload className="w-4 h-4" />, label: 'Tải ảnh QR' },
          { key: 'manual', icon: <Keyboard className="w-4 h-4" />, label: 'Nhập mã' },
        ].map(({ key, icon, label }) => (
          <button
            key={key}
            onClick={() => { setMode(key); reset() }}
            className={cn(
              'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
              mode === key
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-500 hover:text-gray-700'
            )}
          >
            {icon} {label}
          </button>
        ))}
      </div>

      {/* ── Upload mode ──────────────────────────────────── */}
      {mode === 'upload' && (
        <div className="space-y-4">
          {/* Drop zone */}
          <div
            ref={dropRef}
            onDrop={handleDrop}
            onDragOver={(e) => e.preventDefault()}
            onClick={() => fileInputRef.current?.click()}
            className={cn(
              'relative border-2 border-dashed rounded-2xl cursor-pointer transition-all',
              'hover:border-brand-400 hover:bg-brand-50/30',
              preview ? 'border-brand-300 bg-brand-50/20' : 'border-gray-200 bg-gray-50'
            )}
          >
            {preview ? (
              <div className="relative">
                <img
                  src={preview} alt="QR preview"
                  className="w-full max-h-96 object-contain rounded-2xl p-2"
                />
                <div className="absolute inset-0 flex items-center justify-center
                  opacity-0 hover:opacity-100 transition-all rounded-2xl bg-black/20">
                  <p className="text-white text-sm font-medium bg-black/50 px-3 py-1.5 rounded-lg">
                    Nhấn để đổi ảnh
                  </p>
                </div>
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center py-16 gap-3">
                <div className="w-16 h-16 rounded-2xl bg-brand-50 flex items-center justify-center">
                  <ImageIcon className="w-8 h-8 text-brand-400" />
                </div>
                <div className="text-center">
                  <p className="font-medium text-gray-700">Kéo & thả hoặc nhấn để chọn ảnh</p>
                  <p className="text-gray-400 text-xs mt-1">PNG, JPG, WEBP</p>
                </div>
              </div>
            )}
          </div>
          <input
            ref={fileInputRef} type="file" accept="image/*" className="hidden"
            onChange={(e) => handleFile(e.target.files?.[0])}
          />
        </div>
      )}

      {/* ── Manual mode ──────────────────────────────────── */}
      {mode === 'manual' && (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 space-y-4">
          <div className="flex items-center gap-3 p-4 rounded-xl bg-gray-50">
            <QrCode className="w-5 h-5 text-gray-400 flex-shrink-0" />
            <input
              value={manualCode}
              onChange={(e) => setManualCode(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && manualCode.trim() && runCheckIn(manualCode.trim())}
              placeholder="Nhập mã QR hoặc booking code..."
              className="flex-1 bg-transparent outline-none text-gray-800 placeholder:text-gray-400 text-sm"
              autoFocus
            />
          </div>
          <button
            onClick={() => manualCode.trim() && runCheckIn(manualCode.trim())}
            disabled={!manualCode.trim() || checkInMutation.isPending}
            className="w-full flex items-center justify-center gap-2 py-3 rounded-xl
              bg-brand-500 hover:bg-brand-600 text-white font-semibold transition-all
              disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {checkInMutation.isPending
              ? <div className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />
              : <CheckCircle2 className="w-5 h-5" />}
            Xác nhận check-in
          </button>
        </div>
      )}

      {/* ── Loading (image decode + api) ──────────────────── */}
      {checkInMutation.isPending && (
        <div className="flex items-center justify-center gap-3 p-6 bg-white rounded-2xl
          border border-gray-100 shadow-sm">
          <div className="w-6 h-6 border-2 border-brand-500 border-t-transparent rounded-full animate-spin" />
          <p className="text-sm text-gray-500">Đang xác thực vé...</p>
        </div>
      )}

      {/* ── Decode error ──────────────────────────────────── */}
      <AnimatePresence>
        {decodeError && (
          <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}
            className="flex items-center gap-3 p-4 rounded-2xl bg-amber-50 border border-amber-200">
            <QrCode className="w-5 h-5 text-amber-500 flex-shrink-0" />
            <p className="text-amber-700 text-sm">{decodeError}</p>
            <button onClick={reset} className="ml-auto">
              <RotateCcw className="w-4 h-4 text-amber-400" />
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Result ──────────────────────────────────────────── */}
      <AnimatePresence>
        {result && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0 }}
            className={cn(
              'rounded-2xl border shadow-sm p-6 space-y-4',
              result.ok ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'
            )}
          >
            <div className="flex items-center gap-3">
              <div className={cn(
                'w-10 h-10 rounded-xl flex items-center justify-center',
                result.ok ? 'bg-green-100' : 'bg-red-100'
              )}>
                {result.ok
                  ? <CheckCircle2 className="w-6 h-6 text-green-600" />
                  : <XCircle className="w-6 h-6 text-red-500" />}
              </div>
              <div>
                <p className={cn('font-bold text-lg', result.ok ? 'text-green-800' : 'text-red-700')}>
                  {result.ok ? '✅ Check-in thành công!' : '❌ Check-in thất bại'}
                </p>
                {!result.ok && (
                  <p className="text-red-600 text-sm">{result.message}</p>
                )}
              </div>
            </div>

            {result.ok && result.data && (
              <div className="grid grid-cols-2 gap-3 text-sm">
                {[
                  ['Mã vé', result.data.bookingCode],
                  ['Phim', result.data.movieTitle],
                  ['Rạp', result.data.cinemaName],
                  ['Phòng', result.data.screenName],
                  ['Suất chiếu', formatDateTime(result.data.startTime)],
                  ['Ghế', result.data.seats?.map(s => `${s.rowLabel}${s.colNumber}`).join(', ')],
                ].map(([k, v]) => v && (
                  <div key={k} className="p-3 rounded-xl bg-white/70">
                    <p className="text-gray-400 text-xs mb-0.5">{k}</p>
                    <p className="text-gray-800 font-medium break-all">{v}</p>
                  </div>
                ))}
              </div>
            )}

            <button
              onClick={reset}
              className={cn(
                'flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all',
                result.ok
                  ? 'bg-green-600 hover:bg-green-700 text-white'
                  : 'bg-red-500 hover:bg-red-600 text-white'
              )}
            >
              <RotateCcw className="w-4 h-4" /> Check-in tiếp
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
