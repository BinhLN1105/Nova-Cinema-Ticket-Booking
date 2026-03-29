import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Edit2, Trash2, Power, PowerOff, Info, Gift } from 'lucide-react'
import { ruleApi } from '@/api/endpoints'
import { formatCurrency, formatDate, cn } from '@/utils'
import { AdminCard, PageHeader, Table, StatusBadge } from '@/components/common/ui/AdminTable'
import { Button } from '@/components/common/ui/FormElements'
import { Modal } from '@/components/common/ui/Modal'
import toast from 'react-hot-toast'
import RuleModal from './PricingRuleModal'

const RULE_TYPES = {
  DAY_OF_WEEK: 'Thứ trong tuần',
  TIME_FRAME: 'Khung giờ',
  DATE_RANGE: 'Lễ / Sự kiện',
  SEAT_TYPE: 'Loại ghế',
  PROMOTION: 'Chương trình khuyến mãi'
}

const ADJ_TYPES = {
  PERCENTAGE: 'Phần trăm (%)',
  FIXED_AMOUNT: 'Khoản tiền (VND)',
  MULTIPLIER: 'Nhân hệ số (x)'
}

export default function PricingRulesPage() {
  const queryClient = useQueryClient()
  const [modalData, setModalData] = useState({ isOpen: false, rule: null })
  const [showInfoModal, setShowInfoModal] = useState(false)

  const { data: rulesData, isLoading } = useQuery({
    queryKey: ['admin-pricing-rules'],
    queryFn: () => ruleApi.getAll({ page: 0, size: 50 })
  })

  const rules = rulesData?.content || []

  // Toggle Active
  const toggleMutation = useMutation({
    mutationFn: (id) => ruleApi.toggleActive(id),
    onSuccess: () => {
      toast.success('Đổi trạng thái thành công')
      queryClient.invalidateQueries(['admin-pricing-rules'])
    }
  })

  // Delete
  const deleteMutation = useMutation({
    mutationFn: (id) => ruleApi.delete(id),
    onSuccess: () => {
      toast.success('Đã xoá quy tắc')
      queryClient.invalidateQueries(['admin-pricing-rules'])
    }
  })

  const columns = [
    {
      key: 'name',
      header: 'Tên quy tắc',
      render: (r) => <span className="font-semibold text-gray-900">{r.name}</span>
    },
    {
      key: 'ruleType',
      header: 'Loại điều kiện',
      render: (r) => <StatusBadge label={RULE_TYPES[r.ruleType]} color="blue" />
    },
    {
      key: 'conditionDisplay',
      header: 'Điều kiện áp dụng',
      render: (r) => {
        if (r.ruleType === 'PROMOTION') {
          const targetLabels = { TICKET: '🎟️ Vé lẻ', COMBO: '🍿 Combo', ORDER_TOTAL: '💰 Tổng đơn' };
          return (
            <div className="flex flex-col gap-1.5 py-1">
              <div className="flex items-center gap-1.5 text-xs text-blue-700 bg-blue-50 w-fit px-2 py-0.5 rounded-md border border-blue-100">
                <span className="font-bold">{targetLabels[r.targetType] || 'Vé'}</span>
              </div>
              <div className="text-sm font-medium text-gray-700 leading-relaxed">
                📅 Từ {formatDate(r.startDate)} đến {formatDate(r.endDate)}
              </div>
              {(r.minTicketQty > 0 || r.minComboQty > 0) && (
                <div className="text-xs text-gray-500 flex flex-wrap gap-x-3 gap-y-1">
                  {r.minTicketQty > 0 && <span>💺 Tối thiểu: <b className="text-gray-900">{r.minTicketQty} Ghế</b></span>}
                  {r.minComboQty > 0 && <span>🍿 Tối thiểu: <b className="text-gray-900">{r.minComboQty} Combo</b></span>}
                </div>
              )}
            </div>
          )
        }

        let display = r.conditionDisplay || r.conditionValue;
        if (r.ruleType === 'DATE_RANGE') {
          const [start, end] = r.conditionValue.split(',');
          display = `Từ ${formatDate(start)} đến ${formatDate(end)}`;
        }

        return (
          <div className="text-sm text-gray-700 font-medium whitespace-pre-wrap max-w-md">
            {display}
          </div>
        )
      }
    },
    {
      key: 'adjustment',
      header: 'Mức điều chỉnh',
      render: (r) => {
        const isPromotion = r.ruleType === 'PROMOTION';
        const val = r.adjustmentValue;
        const isSurcharge = val > 0; // Số dương là Phụ thu
        const absValue = Math.abs(val);

        return (
          <div className="flex flex-col gap-1">
            <span className={cn(
              "font-extrabold px-2.5 py-1 rounded-lg text-xs w-fit shadow-sm",
              isSurcharge && !isPromotion
                ? "text-orange-700 bg-orange-50 border border-orange-100" 
                : "text-red-700 bg-red-50 border border-red-100"
            )}>
              {isPromotion ? '🎁 Giảm ' : (isSurcharge ? '➕ Phụ thu ' : '➖ Giảm ')}
              {r.adjustmentType === 'FIXED_AMOUNT' ? formatCurrency(absValue) : absValue}
              {r.adjustmentType === 'PERCENTAGE' && '%'}
              {r.adjustmentType === 'MULTIPLIER' && 'x'}
            </span>
          </div>
        )
      }
    },

    {
      key: 'priority',
      header: 'Ưu tiên',
      className: 'text-center',
      headerClassName: 'text-center'
    },
    {
      key: 'isActive',
      header: 'Trạng thái',
      render: (r) => (
        <div className="flex justify-center">
          <button onClick={() => toggleMutation.mutate(r.id)}
            disabled={toggleMutation.isPending}
            className={cn(
              "flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold border transition-all shadow-sm",
              r.isActive 
                ? 'bg-green-50 text-green-700 border-green-200 hover:bg-green-100' 
                : 'bg-gray-50 text-gray-500 border-gray-200 hover:bg-gray-100'
            )}>
            {r.isActive ? <Power className="w-3.5 h-3.5" /> : <PowerOff className="w-3.5 h-3.5" />}
            {r.isActive ? 'BẬT' : 'TẮT'}
          </button>
        </div>
      )
    },
    {
      key: 'actions',
      header: 'Thao tác',
      render: (r) => (
        <div className="flex items-center justify-center gap-1">
          <button onClick={() => setModalData({ isOpen: true, rule: r })}
            className="p-2 rounded-xl text-gray-400 hover:text-brand-500 hover:bg-brand-50 transition-all">
            <Edit2 className="w-4 h-4" />
          </button>
          <button onClick={() => { if(confirm('Xoá quy tắc này?')) deleteMutation.mutate(r.id) }}
            className="p-2 rounded-xl text-gray-400 hover:text-red-500 hover:bg-red-50 transition-all">
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      )
    }
  ]

  return (
    <div className="space-y-6">
      <PageHeader 
        title={
          <div className="flex items-center gap-2">
            <span>Cấu hình giá (Dynamic Pricing)</span>
            <button 
              className="p-1 rounded-lg text-blue-600 hover:bg-blue-50 hover:text-blue-700 transition-all"
              onClick={() => setShowInfoModal(true)}
            >
              <Info className="h-5 w-5" />
            </button>
          </div>
        } 
        subtitle="Quản lý các sự kiện giảm giá, cộng phí hay hệ số giá vé tự động."
        action={
          <Button leftIcon={<Plus className="w-4 h-4" />} onClick={() => setModalData({ isOpen: true, rule: null })}>
            Thêm mới
          </Button>
        }
      />

      <AdminCard>
        <Table 
          columns={columns} 
          data={rules} 
          loading={isLoading} 
          rowKey={r => r.id}
          emptyMessage="Chưa có quy tắc giá nào được thiết lập"
        />
      </AdminCard>

      {/* Modal Hướng dẫn quy tắc giá */}
      <Modal
        open={showInfoModal}
        onClose={() => setShowInfoModal(false)}
        title="Quy tắc tính giá hệ thống"
        size="lg"
      >
        <div className="space-y-5 text-sm leading-relaxed text-gray-600">
          <section className="bg-orange-50/50 p-4 rounded-xl border border-orange-100">
            <h3 className="font-bold text-orange-800 mb-2 flex items-center gap-2">
              <Plus className="h-4 w-4" /> 1. Phụ thu (Surcharges)
            </h3>
            <div className="space-y-1">
              <p>Áp dụng cho: <span className="font-medium">Loại ghế, Thứ trong tuần, Khung giờ, Lễ/Sự kiện.</span></p>
              <p>Quy tắc: <span className="text-orange-700 font-bold">Cộng dồn tất cả.</span> Mọi phụ thu thỏa mãn điều kiện đều được cộng vào giá vé gốc ngay khi chọn ghế.</p>
            </div>
          </section>

          <section className="bg-red-50/50 p-4 rounded-xl border border-red-100">
            <h3 className="font-bold text-red-800 mb-2 flex items-center gap-2">
              <Gift className="h-4 w-4" /> 2. Khuyến mãi (Promotions)
            </h3>
            <div className="space-y-2">
              <p>Áp dụng cho: <span className="font-medium">Chương trình khuyến mãi hệ thống.</span></p>
              <p>Quy tắc: <span className="text-red-700 font-bold">Tối ưu (Không cộng dồn).</span> Chỉ 1 chương trình tốt nhất được áp dụng dựa trên <b>Ưu tiên (Priority)</b>:</p>
              <ul className="list-disc ml-5 space-y-1">
                <li>Ưu tiên (1) được xét trước Ưu tiên (5), (10)...</li>
                <li>Nếu cùng mức ưu tiên, hệ thống chọn mức giảm giá cao nhất.</li>
              </ul>
            </div>
          </section>

          <div className="p-4 bg-blue-50 border border-blue-100 rounded-xl text-blue-800 italic">
            <span className="font-bold">Mẹo:</span> Khách hàng có thể sử dụng thêm <span className="font-bold">01 Voucher</span> cá nhân chồng lên Khuyến mãi của hệ thống.
          </div>
        </div>
      </Modal>

      {modalData.isOpen && (
        <RuleModal 
          rule={modalData.rule} 
          onClose={() => setModalData({ isOpen: false, rule: null })}
          onSuccess={() => {
            setModalData({ isOpen: false, rule: null })
            queryClient.invalidateQueries(['admin-pricing-rules'])
          }}
        />
      )}
    </div>
  )
}
