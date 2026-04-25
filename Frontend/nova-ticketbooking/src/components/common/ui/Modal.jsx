import { useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { X } from 'lucide-react'
import { cn } from '@/utils'


const SIZE_MAP = {
  sm:  'max-w-sm',
  md:  'max-w-md',
  lg:  'max-w-lg',
  xl:  'max-w-xl',
  '2xl': 'max-w-2xl',
}

export function Modal({ open, onClose, title, description, children, size = 'md', theme = 'light', className }) {
  const isDark = theme === 'dark'

  // Close on Escape
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose() }
    if (open) window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [open, onClose])

  // Prevent body scroll
  useEffect(() => {
    if (open) document.body.style.overflow = 'hidden'
    else document.body.style.overflow = ''
    return () => { document.body.style.overflow = '' }
  }, [open])

  return (
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={onClose}
          />
          {/* Dialog */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 16 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 8 }}
            transition={{ duration: 0.25, ease: [0.34, 1.1, 0.64, 1] }}
            className={cn(
              'relative w-full rounded-2xl shadow-2xl flex flex-col max-h-[90vh]',
              isDark ? 'bg-cinema-900 border border-white/10 shadow-black/50' : 'bg-white',
              SIZE_MAP[size], className
            )}
          >
            {/* Header */}
            {title && (
              <div className={cn(
                "flex items-start justify-between gap-4 px-6 py-5 border-b flex-shrink-0",
                isDark ? "border-white/5" : "border-gray-100"
              )}>
                <div>
                  <h2 className={cn("text-lg font-bold font-display", isDark ? "text-white" : "text-gray-900")}>{title}</h2>
                  {description && <p className={cn("text-sm mt-0.5", isDark ? "text-cinema-400" : "text-gray-500")}>{description}</p>}
                </div>
                <button onClick={onClose}
                  className={cn("p-1.5 rounded-lg transition-all flex-shrink-0",
                    isDark ? "text-cinema-400 hover:text-white hover:bg-white/10" : "text-gray-400 hover:text-gray-600 hover:bg-gray-100"
                  )}>
                  <X className="w-5 h-5" />
                </button>
              </div>
            )}
            {!title && (
              <button onClick={onClose}
                className={cn("absolute top-4 right-4 z-10 p-1.5 rounded-lg transition-all",
                  isDark ? "text-cinema-400 hover:text-white hover:bg-white/10" : "text-gray-400 hover:text-gray-600 hover:bg-gray-100"
                )}>
                <X className="w-5 h-5" />
              </button>
            )}
            {/* Body */}
            <div className="p-6 overflow-y-auto custom-scrollbar">{children}</div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  )
}

// Confirm dialog

export function ConfirmDialog({
  open, onClose, onConfirm, title, message,
  confirmLabel = 'Xác nhận', confirmVariant = 'danger', loading
}) {
  return (
    <Modal open={open} onClose={onClose} size="sm">
      <div className="text-center">
        <div className={cn(
          'w-14 h-14 rounded-2xl mx-auto mb-4 flex items-center justify-center text-2xl',
          confirmVariant === 'danger' ? 'bg-red-50' : 'bg-blue-50'
        )}>
          {confirmVariant === 'danger' ? '⚠️' : '✅'}
        </div>
        <h3 className="font-bold text-gray-900 text-lg mb-2">{title}</h3>
        <p className="text-gray-500 text-sm mb-6">{message}</p>
        <div className="flex gap-3">
          <button onClick={onClose} disabled={loading}
            className="flex-1 py-2.5 rounded-xl border border-gray-200 text-gray-600 text-sm font-medium hover:bg-gray-50 transition-all disabled:opacity-50">
            Hủy
          </button>
          <button onClick={onConfirm} disabled={loading}
            className={cn(
              'flex-1 py-2.5 rounded-xl text-white text-sm font-semibold transition-all disabled:opacity-50',
              confirmVariant === 'danger'
                ? 'bg-red-500 hover:bg-red-600'
                : 'bg-brand-500 hover:bg-brand-600'
            )}>
            {loading ? (
              <span className="flex items-center justify-center gap-2">
                <div className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                Đang xử lý...
              </span>
            ) : confirmLabel}
          </button>
        </div>
      </div>
    </Modal>
  )
}
