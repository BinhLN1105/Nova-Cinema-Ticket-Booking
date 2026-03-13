import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Edit2, Trash2, Power, PowerOff } from 'lucide-react'
import { ruleApi } from '@/api/endpoints'
import { formatCurrency, cn } from '@/utils'
import toast from 'react-hot-toast'
import RuleModal from './PricingRuleModal'

const RULE_TYPES = {
  DAY_OF_WEEK: 'Thứ trong tuần',
  TIME_FRAME: 'Khung giờ',
  DATE_RANGE: 'Lễ / Sự kiện',
  SEAT_TYPE: 'Loại ghế'
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

  if (isLoading) return <div className="p-6">Đang tải...</div>

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-white">Cấu hình Quy tắc giá (Dynamic Pricing)</h1>
          <p className="text-cinema-400 text-sm mt-1">Quản lý các sự kiện giảm giá, cộng phí hay hệ số giá vé.</p>
        </div>
        <button 
          onClick={() => setModalData({ isOpen: true, rule: null })}
          className="btn bg-brand-500 hover:bg-brand-600 text-white rounded-xl flex items-center gap-2">
          <Plus className="w-4 h-4" /> Thêm mới
        </button>
      </div>

      <div className="card-cinema overflow-hidden">
        <table className="w-full text-left text-sm text-cinema-300">
          <thead className="text-xs uppercase bg-cinema-800 text-cinema-400">
            <tr>
              <th className="px-5 py-3 rounded-tl-xl border-r border-white/5">Tên quy tắc</th>
              <th className="px-5 py-3 border-r border-white/5">Loại điều kiện</th>
              <th className="px-5 py-3 border-r border-white/5">Giá trị đ/k</th>
              <th className="px-5 py-3 border-r border-white/5">Mức điều chỉnh</th>
              <th className="px-5 py-3 border-r border-white/5">Ưu tiên</th>
              <th className="px-5 py-3 border-r border-white/5 text-center">Trạng thái</th>
              <th className="px-5 py-3 rounded-tr-xl text-center">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {!rules.length ? (
              <tr>
                <td colSpan={7} className="px-5 py-8 text-center text-cinema-400 italic">
                  Chưa có quy tắc nào
                </td>
              </tr>
            ) : rules.map(rule => (
              <tr key={rule.id} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                <td className="px-5 py-3 font-medium text-white">{rule.name}</td>
                <td className="px-5 py-3"><span className="badge badge-blue text-xs">{RULE_TYPES[rule.ruleType]}</span></td>
                <td className="px-5 py-3 font-mono text-xs">{rule.conditionValue}</td>
                <td className="px-5 py-3">
                  <span className={cn(
                    "font-medium",
                    rule.adjustmentValue > 0 && rule.adjustmentType !== 'PERCENTAGE' && rule.adjustmentType !== 'MULTIPLIER' ? "text-green-400" : "text-brand-400"
                  )}>
                    {rule.adjustmentType === 'FIXED_AMOUNT' && (rule.adjustmentValue > 0 ? '+' : '')}
                    {rule.adjustmentType === 'FIXED_AMOUNT' ? formatCurrency(rule.adjustmentValue) : rule.adjustmentValue}
                    {rule.adjustmentType === 'PERCENTAGE' && '%'}
                    {rule.adjustmentType === 'MULTIPLIER' && 'x'}
                  </span>
                </td>
                <td className="px-5 py-3 text-center">{rule.priority}</td>
                <td className="px-5 py-3 text-center">
                  <button onClick={() => toggleMutation.mutate(rule.id)}
                    className={cn(
                      "flex items-center gap-1.5 px-3 py-1.5 rounded-lg mx-auto text-xs font-medium border transition-all",
                      rule.isActive ? 'bg-green-500/10 text-green-400 border-green-500/20' : 'bg-red-500/10 text-red-500 border-red-500/20'
                    )}>
                    {rule.isActive ? <Power className="w-3.5 h-3.5" /> : <PowerOff className="w-3.5 h-3.5" />}
                    {rule.isActive ? 'BẬT' : 'TẮT'}
                  </button>
                </td>
                <td className="px-5 py-3">
                  <div className="flex items-center justify-center gap-2">
                    <button onClick={() => setModalData({ isOpen: true, rule })}
                      className="p-1.5 rounded-lg text-cinema-400 hover:text-white hover:bg-white/10 transition-colors">
                      <Edit2 className="w-4 h-4" />
                    </button>
                    <button onClick={() => { if(confirm('Xoá quy tắc này?')) deleteMutation.mutate(rule.id) }}
                      className="p-1.5 rounded-lg text-red-400/70 hover:text-red-400 hover:bg-red-400/10 transition-colors">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

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
