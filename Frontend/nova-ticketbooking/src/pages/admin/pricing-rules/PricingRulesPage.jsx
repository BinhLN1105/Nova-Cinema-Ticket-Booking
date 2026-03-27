import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Edit2, Trash2, Power, PowerOff } from 'lucide-react'
import { ruleApi } from '@/api/endpoints'
import { formatCurrency, cn } from '@/utils'
import { AdminCard, PageHeader, Table, StatusBadge } from '@/components/common/ui/AdminTable'
import { Button } from '@/components/common/ui/FormElements'
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
      key: 'conditionValue',
      header: 'Giá trị đ/k',
      render: (r) => <code className="text-xs bg-gray-100 px-1.5 py-0.5 rounded text-gray-600">{r.conditionValue}</code>
    },
    {
      key: 'adjustment',
      header: 'Mức điều chỉnh',
      render: (r) => (
        <span className={cn(
          "font-bold",
          r.adjustmentValue > 0 && r.adjustmentType !== 'PERCENTAGE' && r.adjustmentType !== 'MULTIPLIER' ? "text-green-600" : "text-brand-500"
        )}>
          {r.adjustmentType === 'FIXED_AMOUNT' && (r.adjustmentValue > 0 ? '+' : '')}
          {r.adjustmentType === 'FIXED_AMOUNT' ? formatCurrency(r.adjustmentValue) : r.adjustmentValue}
          {r.adjustmentType === 'PERCENTAGE' && '%'}
          {r.adjustmentType === 'MULTIPLIER' && 'x'}
        </span>
      )
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
        title="Cấu hình giá (Dynamic Pricing)" 
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
