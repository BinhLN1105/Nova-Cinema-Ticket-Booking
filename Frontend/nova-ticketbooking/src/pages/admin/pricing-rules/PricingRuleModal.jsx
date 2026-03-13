import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useMutation } from '@tanstack/react-query'
import { X, Loader2 } from 'lucide-react'
import { ruleApi } from '@/api/endpoints'
import toast from 'react-hot-toast'

export default function PricingRuleModal({ rule, onClose, onSuccess }) {
  const [formData, setFormData] = useState({
    name: '',
    ruleType: 'DAY_OF_WEEK',
    conditionValue: 'MONDAY',
    adjustmentType: 'PERCENTAGE',
    adjustmentValue: 0,
    priority: 0,
    isActive: true
  })

  useEffect(() => {
    if (rule) {
      setFormData({
        name: rule.name,
        ruleType: rule.ruleType,
        conditionValue: rule.conditionValue,
        adjustmentType: rule.adjustmentType,
        adjustmentValue: rule.adjustmentValue,
        priority: rule.priority,
        isActive: rule.isActive
      })
    }
  }, [rule])

  const mutation = useMutation({
    mutationFn: (data) => rule ? ruleApi.update(rule.id, data) : ruleApi.create(data),
    onSuccess: () => {
      toast.success(rule ? 'Cập nhật thành công' : 'Thêm mới thành công')
      onSuccess()
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Có lỗi xảy ra')
    }
  })

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!formData.name.trim()) return toast.error('Vui lòng nhập tên quy tắc')
    if (!formData.conditionValue.trim()) return toast.error('Vui lòng nhập giá trị điều kiện')
    mutation.mutate(formData)
  }

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
          className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />
        
        <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }}
          className="card-cinema w-full max-w-lg relative z-10 p-6">
          <button onClick={onClose} className="absolute right-4 top-4 p-2 text-cinema-400 hover:text-white rounded-xl hover:bg-white/10 transition-colors">
            <X className="w-5 h-5" />
          </button>

          <h3 className="text-xl font-bold text-white mb-6">
            {rule ? 'Cập nhật quy tắc' : 'Thêm quy tắc mới'}
          </h3>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-cinema-300 mb-1.5">Tên quy tắc</label>
              <input type="text" value={formData.name} onChange={e => setFormData({ ...formData, name: e.target.value })}
                className="input-cinema w-full" placeholder="VD: Thứ 3 vui vẻ..." />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-cinema-300 mb-1.5">Loại điều kiện</label>
                <select value={formData.ruleType} onChange={e => setFormData({ ...formData, ruleType: e.target.value })}
                  className="input-cinema w-full">
                  <option value="DAY_OF_WEEK">Thứ trong tuần</option>
                  <option value="TIME_FRAME">Khung giờ</option>
                  <option value="DATE_RANGE">Sự kiện / Khoảng ngày</option>
                  <option value="SEAT_TYPE">Loại ghế</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-cinema-300 mb-1.5">Giá trị điều kiện</label>
                <input type="text" value={formData.conditionValue} onChange={e => setFormData({ ...formData, conditionValue: e.target.value })}
                  className="input-cinema w-full" placeholder={
                    formData.ruleType === 'DAY_OF_WEEK' ? 'MONDAY, TUESDAY...' :
                    formData.ruleType === 'TIME_FRAME' ? '22:00-23:59' :
                    formData.ruleType === 'SEAT_TYPE' ? 'VIP, COUPLE...' :
                    '2024-04-30,2024-05-01'
                  } />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-cinema-300 mb-1.5">Loại điều chỉnh</label>
                <select value={formData.adjustmentType} onChange={e => setFormData({ ...formData, adjustmentType: e.target.value })}
                  className="input-cinema w-full">
                  <option value="PERCENTAGE">Phần trăm (%)</option>
                  <option value="FIXED_AMOUNT">Khoản tiền (VND)</option>
                  <option value="MULTIPLIER">Hệ số nhân (x)</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-cinema-300 mb-1.5">Mức điều chỉnh</label>
                <input type="number" step="0.01" value={formData.adjustmentValue} onChange={e => setFormData({ ...formData, adjustmentValue: Number(e.target.value) })}
                  className="input-cinema w-full" placeholder="VD: -10, 1.5, 20000" />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-cinema-300 mb-1.5">Độ ưu tiên (Số nhỏ ưu tiên xử lý trước)</label>
              <input type="number" value={formData.priority} onChange={e => setFormData({ ...formData, priority: Number(e.target.value) })}
                className="input-cinema w-full" />
            </div>

            <div className="flex items-center gap-2 mt-2">
              <input type="checkbox" id="isActive" checked={formData.isActive} onChange={e => setFormData({ ...formData, isActive: e.target.checked })}
                className="w-4 h-4 rounded border-white/20 bg-cinema-800 text-brand-500 focus:ring-brand-500 focus:ring-offset-cinema-900" />
              <label htmlFor="isActive" className="text-sm text-cinema-300 cursor-pointer">Kích hoạt quy tắc</label>
            </div>

            <div className="flex justify-end gap-3 pt-6 border-t border-white/5">
              <button type="button" onClick={onClose}
                className="px-5 py-2.5 rounded-xl font-medium text-cinema-300 hover:text-white hover:bg-white/5 transition-colors">
                Huỷ
              </button>
              <button type="submit" disabled={mutation.isPending}
                className="btn bg-brand-500 hover:bg-brand-600 text-white px-6 py-2.5 rounded-xl disabled:opacity-50 flex items-center gap-2">
                {mutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                {rule ? 'Cập nhật' : 'Thêm mới'}
              </button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
