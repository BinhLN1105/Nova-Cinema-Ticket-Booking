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
    targetType: 'TICKET',
    minTicketQty: 0,
    minComboQty: 0,
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
        targetType: rule.targetType || 'TICKET',
        minTicketQty: rule.minTicketQty || 0,
        minComboQty: rule.minComboQty || 0,
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
            ) : formData.ruleType === 'DAY_OF_WEEK' ? (
              <Select 
                value={formData.conditionValue}
                onChange={e => setFormData({ ...formData, conditionValue: e.target.value })}
                options={[
                  { value: 'MONDAY',    label: 'Thứ Hai' },
                  { value: 'TUESDAY',   label: 'Thứ Ba' },
                  { value: 'WEDNESDAY', label: 'Thứ Tư' },
                  { value: 'THURSDAY',  label: 'Thứ Năm' },
                  { value: 'FRIDAY',    label: 'Thứ Sáu' },
                  { value: 'SATURDAY',  label: 'Thứ Bảy' },
                  { value: 'SUNDAY',    label: 'Chủ Nhật' },
                ]}
              />
            ) : formData.ruleType === 'SEAT_TYPE' ? (
              <Select 
                value={formData.conditionValue}
                onChange={e => setFormData({ ...formData, conditionValue: e.target.value })}
                options={[
                  { value: 'STANDARD', label: 'Ghế Thường' },
                  { value: 'VIP',      label: 'Ghế VIP' },
                  { value: 'COUPLE',   label: 'Ghế Đôi' },
                ]}
              />
            ) : formData.ruleType === 'DATE_RANGE' ? (
              <div className="flex flex-col gap-2">
                <div className="flex items-center gap-2">
                  <span className="text-[10px] uppercase font-bold text-gray-400 w-8">Từ:</span>
                  <Input 
                    type="date"
                    className="flex-1"
                    value={formData.conditionValue.split(',')[0] || ''} 
                    onChange={e => {
                      const parts = formData.conditionValue.split(',')
                      setFormData({ ...formData, conditionValue: `${e.target.value},${parts[1] || ''}` })
                    }}
                  />
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-[10px] uppercase font-bold text-gray-400 w-8">Đến:</span>
                  <Input 
                    type="date"
                    className="flex-1"
                    value={formData.conditionValue.split(',')[1] || ''} 
                    onChange={e => {
                      const parts = formData.conditionValue.split(',')
                      setFormData({ ...formData, conditionValue: `${parts[0] || ''},${e.target.value}` })
                    }}
                  />
                </div>
              </div>
            ) : formData.ruleType === 'TIME_FRAME' ? (
              <div className="flex flex-col gap-2">
                <div className="flex items-center gap-2">
                  <span className="text-[10px] uppercase font-bold text-gray-400 w-8">Từ:</span>
                  <Input 
                    type="time" 
                    className="flex-1"
                    value={formData.conditionValue.split('-')[0] || ''} 
                    onChange={e => {
                      const parts = formData.conditionValue.split('-')
                      setFormData({ ...formData, conditionValue: `${e.target.value}-${parts[1] || ''}` })
                    }}
                  />
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-[10px] uppercase font-bold text-gray-400 w-8">Đến:</span>
                  <Input 
                    type="time"
                    className="flex-1"
                    value={formData.conditionValue.split('-')[1] || ''} 
                    onChange={e => {
                      const parts = formData.conditionValue.split('-')
                      setFormData({ ...formData, conditionValue: `${parts[0] || ''}-${e.target.value}` })
                    }}
                  />
                </div>
              </div>
            ) : (
              <Input 
                value={formData.conditionValue} 
                onChange={e => setFormData({ ...formData, conditionValue: e.target.value })}
                placeholder="Nhập giá trị điều kiện..." 
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
        
        {formData.ruleType === 'PROMOTION' && (
          <div className="p-4 bg-orange-50/50 rounded-2xl border border-orange-100 space-y-4">
             <Field label="🎯 Mục tiêu áp dụng" required info="Quy tắc này sẽ giảm giá cho đối tượng nào?">
                <Select 
                  value={formData.targetType} 
                  onChange={e => setFormData({ ...formData, targetType: e.target.value })}
                  options={[
                    { value: 'TICKET',      label: '🎟️ Chỉ giảm trên Vé' },
                    { value: 'COMBO',       label: '🍿 Chỉ giảm trên Combo' },
                    { value: 'ORDER_TOTAL', label: '💰 Giảm trên Tổng đơn' },
                  ]} 
                />
              </Field>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {(formData.targetType === 'TICKET' || formData.targetType === 'ORDER_TOTAL') && (
                  <Field label="💺 Số lượng Ghế tối thiểu" info="Số lượng chỗ ngồi (vé lẻ) trong đơn hàng để được áp dụng.">
                    <Input 
                      type="number" 
                      min="0"
                      value={formData.minTicketQty} 
                      onChange={e => setFormData({ ...formData, minTicketQty: Number(e.target.value) })} 
                    />
                  </Field>
                )}
                {(formData.targetType === 'COMBO' || formData.targetType === 'ORDER_TOTAL') && (
                  <Field label="🍿 Số lượng Combo tối thiểu" info="Tổng số lượng các phần bắp nước trong đơn hàng để được áp dụng.">
                    <Input 
                      type="number" 
                      min="0"
                      value={formData.minComboQty} 
                      onChange={e => setFormData({ ...formData, minComboQty: Number(e.target.value) })} 
                    />
                  </Field>
                )}
              </div>
              {formData.targetType === 'ORDER_TOTAL' && (
                <p className="text-[10px] text-orange-600 font-medium italic">
                  * Hệ thống sẽ áp dụng giảm giá khi đơn hàng thỏa mãn ĐỒNG THỜI cả số lượng Ghế và Combo trên.
                </p>
              )}
          </div>
        )}

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
