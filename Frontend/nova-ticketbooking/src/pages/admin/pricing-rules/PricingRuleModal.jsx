import { useState, useEffect } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { ruleApi, promotionApi } from '@/api/endpoints'
import { Modal } from '@/components/common/ui/Modal'
import { Field, Input, Select, Button, Switch } from '@/components/common/ui/FormElements'
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

  // Lấy danh sách khuyến mãi
  const { data: promoData } = useQuery({
    queryKey: ['admin-all-promotions'],
    queryFn: () => promotionApi.getAll({ page: 0, size: 1000 }),
    enabled: formData.ruleType === 'PROMOTION'
  })
  const promotions = Array.isArray(promoData) ? promoData : (promoData?.content || [])

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!formData.name.trim()) return toast.error('Vui lòng nhập tên quy tắc')
    if (!formData.conditionValue.trim()) return toast.error('Vui lòng nhập giá trị điều kiện')
    mutation.mutate(formData)
  }

  return (
    <Modal 
      open={true} 
      onClose={onClose}
      title={rule ? 'Cập nhật quy tắc giá' : 'Thêm quy tắc mới'}
      description="Cấu hình tự động điều chỉnh giá vé dựa trên các điều kiện cụ thể."
      size="lg"
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        <Field label="Tên quy tắc" required>
          <Input 
            value={formData.name} 
            onChange={e => setFormData({ ...formData, name: e.target.value })}
            placeholder="VD: Thứ 3 vui vẻ, Lễ 30/4..." 
          />
        </Field>

        <div className="grid grid-cols-2 gap-4">
          <Field label="Loại điều kiện" required>
            <Select 
              value={formData.ruleType} 
              onChange={e => setFormData({ ...formData, ruleType: e.target.value })}
              options={[
                { value: 'DAY_OF_WEEK', label: '📅 Thứ trong tuần' },
                { value: 'TIME_FRAME',  label: '⏰ Khung giờ' },
                { value: 'DATE_RANGE',  label: '🎉 Sự kiện / Khoảng ngày' },
                { value: 'SEAT_TYPE',   label: '💺 Loại ghế' },
                { value: 'PROMOTION',   label: '🎁 Chương trình KM' },
              ]} 
            />
          </Field>
          <Field label="Giá trị điều kiện" required>
            {formData.ruleType === 'PROMOTION' ? (
              <Select 
                value={formData.conditionValue}
                onChange={e => setFormData({ ...formData, conditionValue: e.target.value })}
                options={[
                  { value: '', label: '-- Chọn khuyến mãi --' },
                  ...promotions.map(p => ({ value: p.id, label: p.title }))
                ]}
              />
            ) : (
              <Input 
                value={formData.conditionValue} 
                onChange={e => setFormData({ ...formData, conditionValue: e.target.value })}
                placeholder={
                  formData.ruleType === 'DAY_OF_WEEK' ? 'MONDAY, TUESDAY...' :
                  formData.ruleType === 'TIME_FRAME' ? '22:00-23:59' :
                  formData.ruleType === 'SEAT_TYPE' ? 'VIP, COUPLE...' :
                  'YYYY-MM-DD'
                } 
              />
            )}
          </Field>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Field label="Loại điều chỉnh" required>
            <Select 
              value={formData.adjustmentType} 
              onChange={e => setFormData({ ...formData, adjustmentType: e.target.value })}
              options={[
                { value: 'PERCENTAGE',   label: '% Phần trăm' },
                { value: 'FIXED_AMOUNT', label: '₫ Số tiền (VND)' },
                { value: 'MULTIPLIER',   label: '✖️ Hệ số nhân' },
              ]} 
            />
          </Field>
          <Field label="Mức điều chỉnh" required>
            <Input 
              type="number" 
              step="0.01" 
              value={formData.adjustmentValue} 
              onChange={e => setFormData({ ...formData, adjustmentValue: Number(e.target.value) })}
              placeholder="VD: -10, 1.5, 20000" 
            />
          </Field>
        </div>

        <Field label="Độ ưu tiên" info="Số nhỏ hơn sẽ được ưu tiên áp dụng trước.">
          <Input 
            type="number" 
            value={formData.priority} 
            onChange={e => setFormData({ ...formData, priority: Number(e.target.value) })} 
          />
        </Field>

        <div className="flex items-center justify-between p-4 bg-gray-50 rounded-2xl border border-gray-100">
          <div>
            <p className="text-sm font-bold text-gray-900">Kích hoạt quy tắc</p>
            <p className="text-xs text-gray-500">Tắt để ngừng áp dụng quy tắc này ngay lập tức</p>
          </div>
          <Switch 
            checked={formData.isActive} 
            onChange={checked => setFormData({ ...formData, isActive: checked })} 
          />
        </div>

        <div className="flex gap-3 pt-2">
          <Button variant="ghost" className="flex-1" onClick={onClose} type="button">Hủy</Button>
          <Button 
            className="flex-1" 
            type="submit" 
            isLoading={mutation.isPending}
          >
            {rule ? 'Lưu thay đổi' : 'Thêm mới'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
