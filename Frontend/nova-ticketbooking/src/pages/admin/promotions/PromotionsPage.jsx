import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  Tag, Ticket, Plus, Edit2, Trash2, ToggleLeft, ToggleRight,
  Copy, CheckCircle2, Calendar, Percent, DollarSign, Users, TrendingUp
} from 'lucide-react'
import { adminVoucherApi, promotionApi } from '@/api/endpoints'
import { Modal, ConfirmDialog } from '@/components/common/ui/Modal'
import { Table, AdminCard, PageHeader, Pagination, StatusBadge } from '@/components/common/ui/AdminTable'
import { Field, Input, Textarea, Select, Button, SearchInput, Switch } from '@/components/common/ui/FormElements'
import { formatDate, formatCurrency, cn } from '@/utils'
import toast from 'react-hot-toast'
import ImageUploader from '@/components/admin/ImageUploader'

// ─── Voucher Schema ───────────────────────────
const voucherSchema = z.object({
  code:          z.string().min(3, 'Mã ít nhất 3 ký tự').toUpperCase(),
  description:   z.string().min(5, 'Mô tả ít nhất 5 ký tự'),
  discountType:  z.enum(['PERCENTAGE', 'FIXED_AMOUNT']),
  discountValue: z.coerce.number().min(1, 'Giá trị phải > 0'),
  minOrder:      z.coerce.number().min(0),
  maxDiscount:   z.coerce.number().optional(),
  usageLimit:    z.coerce.number().min(1, 'Số lần dùng phải > 0'),
  startDate:     z.string().min(1, 'Chọn ngày bắt đầu'),
  endDate:       z.string().min(1, 'Chọn ngày kết thúc'),
  applicableTo:  z.enum(['ALL', 'MOVIE', 'FIRST_BOOKING']),
})

// ─── Promotion Schema ─────────────────────────
const promoSchema = z.object({
  title:       z.string().min(2, 'Tiêu đề ít nhất 2 ký tự'),
  description: z.string().min(10, 'Mô tả ít nhất 10 ký tự'),
  imageUrl:    z.string().url('URL không hợp lệ').optional().or(z.literal('')),
  startDate:   z.string().min(1, 'Chọn ngày bắt đầu'),
  endDate:     z.string().min(1, 'Chọn ngày kết thúc'),
  targetUrl:   z.string().optional(),
  priority:    z.coerce.number().min(0).max(100),
  isPopup:     z.boolean().default(false),
})

// ─── Voucher type badge ───────────────────────
function VoucherTypeBadge({ type, value }) {
  return (
    <span className={cn(
      'inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold border',
      type === 'PERCENTAGE'
        ? 'bg-purple-50 text-purple-700 border-purple-200'
        : 'bg-green-50 text-green-700 border-green-200'
    )}>
      {type === 'PERCENTAGE'
        ? <><Percent className="w-3 h-3" /> {value}%</>
        : <><DollarSign className="w-3 h-3" /> {formatCurrency(value)}</>
      }
    </span>
  )
}

// ─── Copy code button ─────────────────────────
function CopyCode({ code }) {
  const [copied, setCopied] = useState(false)
  const copy = (e) => {
    e.stopPropagation()
    navigator.clipboard.writeText(code)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }
  return (
    <button onClick={copy}
      className="flex items-center gap-1.5 font-mono text-sm font-bold text-gray-700
        bg-gray-100 hover:bg-gray-200 px-2.5 py-1 rounded-lg transition-all group">
      <span>{code}</span>
      {copied
        ? <CheckCircle2 className="w-3.5 h-3.5 text-green-500" />
        : <Copy className="w-3.5 h-3.5 text-gray-400 group-hover:text-gray-600" />}
    </button>
  )
}

// ─── Voucher usage progress ───────────────────
function UsageBar({ used, limit }) {
  const pct = Math.min((used / limit) * 100, 100)
  return (
    <div className="min-w-[100px]">
      <div className="flex justify-between text-xs text-gray-500 mb-1">
        <span>{used}/{limit}</span>
        <span>{Math.round(pct)}%</span>
      </div>
      <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden">
        <div className={cn(
          'h-full rounded-full transition-all',
          pct >= 90 ? 'bg-red-500' : pct >= 60 ? 'bg-amber-500' : 'bg-green-500'
        )} style={{ width: `${pct}%` }} />
      </div>
    </div>
  )
}

const APPLICABLE_LABELS = { ALL: 'Tất cả', MOVIE: 'Phim cụ thể', FIRST_BOOKING: 'Đặt vé lần đầu' }

// ═══════════════════════════════════════════════
export default function PromotionsPage() {
  const [activeTab, setTab] = useState('vouchers')

  return (
    <div className="space-y-6">
      <PageHeader
        title="Khuyến mãi & Voucher"
        subtitle="Quản lý mã giảm giá và chương trình ưu đãi"
      />

      {/* Tab selector */}
      <div className="flex gap-1 p-1 bg-white border border-gray-200 rounded-2xl w-fit shadow-sm">
        {[
          { id: 'vouchers',    label: 'Mã Voucher',     icon: Ticket },
          { id: 'promotions',  label: 'Banner Khuyến mãi', icon: Tag },
        ].map(({ id, label, icon: Icon }) => (
          <button key={id} onClick={() => setTab(id)}
            className={cn(
              'flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-semibold transition-all',
              activeTab === id
                ? 'bg-brand-500 text-white shadow-sm'
                : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
            )}>
            <Icon className="w-4 h-4" /> {label}
          </button>
        ))}
      </div>

      <AnimatePresence mode="wait">
        {activeTab === 'vouchers' && (
          <motion.div key="vouchers" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
            <VouchersTab />
          </motion.div>
        )}
        {activeTab === 'promotions' && (
          <motion.div key="promotions" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
            <PromotionsTab />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

// ─── VOUCHERS TAB ─────────────────────────────
function VouchersTab() {
  const [search, setSearch]         = useState('')
  const [showForm, setShowForm]     = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [deleteTarget, setDelete]   = useState(null)
  const qc = useQueryClient()

  // API Queries
  const { data: voucherList, isLoading } = useQuery({
    queryKey: ['admin', 'vouchers'],
    queryFn: adminVoucherApi.getAll
  })

  const vouchers = (voucherList?.content ?? []).filter(v =>
    v.code.includes(search.toUpperCase()) ||
    v.description.toLowerCase().includes(search.toLowerCase())
  )

  // Mutations
  const createMutation = useMutation({
    mutationFn: adminVoucherApi.create,
    onSuccess: () => {
      qc.invalidateQueries(['admin', 'vouchers'])
      toast.success('Tạo voucher thành công')
      setShowForm(false)
    }
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => adminVoucherApi.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries(['admin', 'vouchers'])
      toast.success('Đã cập nhật voucher')
      setShowForm(false)
    }
  })

  const toggleMutation = useMutation({
    mutationFn: adminVoucherApi.toggleActive,
    onSuccess: () => qc.invalidateQueries(['admin', 'vouchers'])
  })

  const deleteMutation = useMutation({
    mutationFn: adminVoucherApi.delete,
    onSuccess: () => {
      qc.invalidateQueries(['admin', 'vouchers'])
      toast.success('Đã xóa voucher')
      setDelete(null)
    }
  })

  const { register, handleSubmit, reset, watch, formState: { errors } } = useForm({
    resolver: zodResolver(voucherSchema),
    defaultValues: { discountType: 'PERCENTAGE', applicableTo: 'ALL', minOrder: 0, usageLimit: 100 },
  })
  const discountType = watch('discountType')

  const openCreate = () => { setEditTarget(null); reset({ discountType: 'PERCENTAGE', applicableTo: 'ALL', minOrder: 0, usageLimit: 100 }); setShowForm(true) }
  const openEdit   = (v) => { 
    setEditTarget(v); 
    // Format dates for input[type="date"]
    reset({
      ...v,
      startDate: v.startDate?.split('T')[0],
      endDate: v.endDate?.split('T')[0]
    }); 
    setShowForm(true) 
  }

  const onSubmit = (data) => {
    if (editTarget) {
      updateMutation.mutate({ id: editTarget.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  // Stats
  const activeCount = (voucherList?.content ?? []).filter(v => v.isActive).length
  const totalUsed   = (voucherList?.content ?? []).reduce((s, v) => s + (v.usedCount || 0), 0)
  const nearExpiry  = (voucherList?.content ?? []).filter(v => v.usageLimit > 0 && (v.usedCount / v.usageLimit >= 0.9)).length

  const columns = [
    {
      key: 'code', header: 'Mã Voucher',
      render: (v) => (
        <div className="flex flex-col gap-1.5">
          <CopyCode code={v.code} />
          <p className="text-xs text-gray-400 max-w-[180px] line-clamp-1">{v.description}</p>
        </div>
      ),
    },
    {
      key: 'discount', header: 'Ưu đãi',
      render: (v) => (
        <div className="space-y-1">
          <VoucherTypeBadge type={v.discountType} value={v.discountValue} />
          <p className="text-xs text-gray-400">Đơn tối thiểu: {formatCurrency(v.minOrder)}</p>
          {v.maxDiscount && <p className="text-xs text-gray-400">Tối đa: {formatCurrency(v.maxDiscount)}</p>}
        </div>
      ),
    },
    {
      key: 'applicableTo', header: 'Áp dụng',
      render: (v) => (
        <span className="text-xs text-gray-600 bg-gray-100 px-2 py-1 rounded-lg">
          {APPLICABLE_LABELS[v.applicableTo]}
        </span>
      ),
    },
    {
      key: 'usage', header: 'Lượt dùng',
      render: (v) => <UsageBar used={v.usedCount} limit={v.usageLimit} />,
    },
    {
      key: 'validity', header: 'Thời hạn',
      render: (v) => (
        <div className="text-xs text-gray-500 space-y-0.5">
          <p className="flex items-center gap-1"><Calendar className="w-3 h-3" /> {formatDate(v.startDate)}</p>
          <p className="text-gray-400">→ {formatDate(v.endDate)}</p>
        </div>
      ),
    },
    {
      key: 'isActive', header: 'Trạng thái',
      render: (v) => (
        <StatusBadge label={v.isActive ? 'Đang hoạt động' : 'Tạm dừng'} color={v.isActive ? 'green' : 'gray'} />
      ),
    },
    {
      key: 'actions', header: '',
      render: (v) => (
        <div className="flex items-center justify-end gap-1">
          <button onClick={() => openEdit(v)}
            className="p-2 rounded-lg hover:bg-blue-50 text-gray-400 hover:text-blue-500 transition-all" title="Chỉnh sửa">
            <Edit2 className="w-4 h-4" />
          </button>
          <button onClick={() => toggleMutation.mutate(v.id)}
            disabled={toggleMutation.isPending}
            className={cn('p-2 rounded-lg transition-all', v.isActive ? 'hover:bg-amber-50 text-amber-400' : 'hover:bg-green-50 text-green-400')}
            title={v.isActive ? 'Tạm dừng' : 'Kích hoạt'}>
            {v.isActive ? <ToggleRight className="w-4 h-4" /> : <ToggleLeft className="w-4 h-4" />}
          </button>
          <button onClick={() => setDelete(v)}
            className="p-2 rounded-lg hover:bg-red-50 text-gray-400 hover:text-red-500 transition-all" title="Xóa">
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-5">
      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: 'Tổng voucher',   value: (voucherList?.content ?? []).length, icon: Ticket,     bg: 'bg-blue-50',   text: 'text-blue-500' },
          { label: 'Đang hoạt động', value: activeCount,          icon: CheckCircle2,bg: 'bg-green-50', text: 'text-green-500' },
          { label: 'Lượt sử dụng',  value: totalUsed.toLocaleString(), icon: Users, bg: 'bg-purple-50', text: 'text-purple-500' },
          { label: 'Gần hết lượt',  value: nearExpiry,            icon: TrendingUp,  bg: 'bg-red-50',   text: 'text-red-500' },
        ].map(({ label, value, icon: Icon, bg, text }) => (
          <div key={label} className="bg-white rounded-2xl border border-gray-100 shadow-sm p-4 flex items-center gap-3">
            <div className={`w-10 h-10 rounded-xl ${bg} flex items-center justify-center flex-shrink-0`}>
              <Icon className={`w-5 h-5 ${text}`} />
            </div>
            <div>
              <p className="text-xl font-bold text-gray-900">{value}</p>
              <p className="text-xs text-gray-500">{label}</p>
            </div>
          </div>
        ))}
      </div>

      <AdminCard>
        <div className="p-4 border-b border-gray-100 flex items-center justify-between gap-3">
          <SearchInput value={search} onChange={setSearch} placeholder="Tìm mã voucher..." className="max-w-xs" />
          <Button leftIcon={<Plus className="w-4 h-4" />} onClick={openCreate} size="sm">Tạo Voucher</Button>
        </div>
        <Table columns={columns} data={vouchers} rowKey={v => v.id}
          isLoading={isLoading}
          emptyMessage="Chưa có voucher nào" emptyIcon="🎟️" />
      </AdminCard>

      {/* Form Modal */}
      <Modal open={showForm} onClose={() => setShowForm(false)}
        title={editTarget ? 'Chỉnh sửa Voucher' : 'Tạo Voucher mới'}
        description="Cấu hình chi tiết mã giảm giá" size="xl">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Field label="Mã Voucher" required error={errors.code?.message}>
              <Input {...register('code')}
                onChange={e => e.target.value = e.target.value.toUpperCase()}
                error={!!errors.code} placeholder="VD: NOVA20" className="uppercase font-mono tracking-wider" />
            </Field>
            <Field label="Áp dụng cho" required error={errors.applicableTo?.message}>
              <Select {...register('applicableTo')} error={!!errors.applicableTo}
                options={[
                  { value: 'ALL',           label: '🎬 Tất cả đơn hàng' },
                  { value: 'MOVIE',         label: '🎞️ Phim cụ thể' },
                  { value: 'FIRST_BOOKING', label: '🎉 Đặt vé lần đầu' },
                ]} />
            </Field>

            <Field label="Mô tả" required error={errors.description?.message} className="col-span-2">
              <Input {...register('description')} error={!!errors.description} placeholder="Mô tả ngắn về voucher này..." />
            </Field>

            <Field label="Loại giảm giá" required>
              <Select {...register('discountType')} options={[
                { value: 'PERCENTAGE', label: '% Phần trăm' },
                { value: 'FIXED_AMOUNT',      label: '₫ Số tiền cố định' },
              ]} />
            </Field>
            <Field label={discountType === 'PERCENTAGE' ? 'Phần trăm giảm (%)' : 'Số tiền giảm (₫)'}
              required error={errors.discountValue?.message}>
              <Input {...register('discountValue')} type="number" min={1}
                error={!!errors.discountValue}
                placeholder={discountType === 'PERCENTAGE' ? '20' : '50000'} />
            </Field>

            <Field label="Đơn hàng tối thiểu (₫)" error={errors.minOrder?.message}>
              <Input {...register('minOrder')} type="number" min={0} placeholder="100000" />
            </Field>
            {discountType === 'PERCENTAGE' && (
              <Field label="Giảm tối đa (₫)" error={errors.maxDiscount?.message}>
                <Input {...register('maxDiscount')} type="number" min={0} placeholder="50000 (bỏ trống = không giới hạn)" />
              </Field>
            )}

            <Field label="Số lần dùng tối đa" required error={errors.usageLimit?.message}>
              <Input {...register('usageLimit')} type="number" min={1} placeholder="100" />
            </Field>

            <Field label="Ngày bắt đầu" required error={errors.startDate?.message}>
              <Input {...register('startDate')} type="date" error={!!errors.startDate} />
            </Field>
            <Field label="Ngày kết thúc" required error={errors.endDate?.message}>
              <Input {...register('endDate')} type="date" error={!!errors.endDate} />
            </Field>
          </div>

          {/* Preview */}
          {watch('code') && (
            <div className="p-4 rounded-xl bg-gradient-to-r from-brand-50 to-purple-50 border border-brand-100">
              <p className="text-xs text-gray-500 mb-2 font-medium">Preview voucher</p>
              <div className="flex items-center gap-3">
                <span className="font-mono text-lg font-black text-brand-600 bg-white px-3 py-1.5 rounded-lg border border-brand-200 tracking-widest">
                  {watch('code')}
                </span>
                <div className="text-sm text-gray-700">
                  <p className="font-semibold">
                    {watch('discountType') === 'PERCENTAGE'
                      ? `Giảm ${watch('discountValue') || 0}%`
                      : `Giảm ${formatCurrency(watch('discountValue') || 0)}`}
                    {watch('maxDiscount') ? ` (tối đa ${formatCurrency(watch('maxDiscount'))})` : ''}
                  </p>
                  <p className="text-gray-400 text-xs">Đơn từ {formatCurrency(watch('minOrder') || 0)}</p>
                </div>
              </div>
            </div>
          )}

          <div className="flex gap-3 pt-2">
            <Button type="button" variant="ghost" onClick={() => setShowForm(false)} className="flex-1">Hủy</Button>
            <Button type="submit" className="flex-1" isLoading={createMutation.isPending || updateMutation.isPending}>
              {editTarget ? 'Lưu thay đổi' : 'Tạo Voucher'}
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!deleteTarget} onClose={() => setDelete(null)}
        onConfirm={() => deleteMutation.mutate(deleteTarget.id)}
        isLoading={deleteMutation.isPending}
        title="Xóa Voucher?" confirmLabel="Xóa"
        message={`Bạn có chắc muốn xóa voucher "${deleteTarget?.code}"? Người dùng đang có mã này sẽ không dùng được nữa.`}
      />
    </div>
  )
}

// ─── PROMOTIONS TAB ───────────────────────────
function PromotionsTab() {
  const [showForm, setShowForm]         = useState(false)
  const [editTarget, setEditTarget]     = useState(null)
  const [deleteTarget, setDelete]       = useState(null)
  const [isUploadingImage, setIsUploadingImage] = useState(false)
  const qc = useQueryClient()

  // API Queries
  const { data: promoList, isLoading } = useQuery({
    queryKey: ['admin', 'promotions'],
    queryFn: promotionApi.getAll
  })

  const promotions = promoList?.content ?? []

  // Mutations
  const createMutation = useMutation({
    mutationFn: promotionApi.create,
    onSuccess: () => {
      qc.invalidateQueries(['admin', 'promotions'])
      toast.success('Tạo khuyến mãi thành công')
      setShowForm(false)
    }
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => promotionApi.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries(['admin', 'promotions'])
      toast.success('Đã cập nhật khuyến mãi')
      setShowForm(false)
    }
  })

  const toggleMutation = useMutation({
    mutationFn: promotionApi.toggleActive,
    onSuccess: () => qc.invalidateQueries(['admin', 'promotions'])
  })

  const deleteMutation = useMutation({
    mutationFn: promotionApi.delete,
    onSuccess: () => {
      qc.invalidateQueries(['admin', 'promotions'])
      toast.success('Đã xóa khuyến mãi')
      setDelete(null)
    }
  })

  const { register, handleSubmit, reset, watch, setValue, formState: { errors } } = useForm({
    resolver: zodResolver(promoSchema),
    defaultValues: { priority: 5 },
  })
  const isPopup = watch('isPopup')
  const imageUrl = watch('imageUrl')

  const handleImageUpload = async (source, type) => {
    if (!editTarget?.id) {
      toast.error('Vui lòng lưu thông tin khuyến mãi trước khi tải ảnh')
      return
    }

    setIsUploadingImage(true)
    try {
      let res;
      if (type === 'file') {
        res = await promotionApi.uploadImage(editTarget.id, source)
      } else {
        res = await promotionApi.uploadImageUrl(editTarget.id, source)
      }
      setValue('imageUrl', res.imageUrl)
      qc.invalidateQueries({ queryKey: ['admin', 'promotions'] })
    } finally {
      setIsUploadingImage(false)
    }
  }

  const openCreate = () => { setEditTarget(null); reset({ priority: 5 }); setShowForm(true) }
  const openEdit   = (p) => { 
    setEditTarget(p); 
    reset({
      ...p,
      startDate: p.startDate?.split('T')[0],
      endDate: p.endDate?.split('T')[0]
    }); 
    setShowForm(true) 
  }

  const onSubmit = (data) => {
    if (editTarget) {
      updateMutation.mutate({ id: editTarget.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex justify-between items-center">
        <p className="text-sm text-gray-500">{promotions.length} chương trình khuyến mãi</p>
        <Button leftIcon={<Plus className="w-4 h-4" />} onClick={openCreate} size="sm">
          Tạo khuyến mãi
        </Button>
      </div>

      {/* Promo cards grid */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {[1,2,3].map(i => <div key={i} className="skeleton h-64 rounded-2xl" />)}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {promotions.map((promo, i) => (
            <motion.div key={promo.id}
              initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.07 }}
              className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden group">

              {/* Image */}
              <div className="relative h-36 bg-gradient-to-br from-brand-50 to-purple-50 overflow-hidden">
                {promo.imageUrl ? (
                  <img src={promo.imageUrl} alt={promo.title}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                ) : (
                  <div className="w-full h-full flex items-center justify-center">
                    <Tag className="w-12 h-12 text-brand-200" />
                  </div>
                )}
                {/* Active badge */}
                <div className="absolute top-2 right-2">
                  <StatusBadge label={promo.isActive ? 'Đang chạy' : 'Tạm dừng'} color={promo.isActive ? 'green' : 'gray'} />
                </div>
                {/* Priority badge */}
                <div className="absolute top-2 left-2 bg-black/40 backdrop-blur-sm text-white text-xs
                  px-2 py-0.5 rounded-full font-medium">
                  Ưu tiên {promo.priority}
                </div>
                {/* Popup indicator */}
                {promo.isPopup && (
                  <div className="absolute bottom-2 right-2 bg-brand-500 text-white text-[10px]
                    px-2 py-1 rounded-full font-bold shadow-lg animate-pulse border border-white/20">
                    POPUP
                  </div>
                )}
              </div>

              {/* Content */}
              <div className="p-4">
                <h3 className="font-bold text-gray-900 text-sm line-clamp-1 mb-1">{promo.title}</h3>
                <p className="text-gray-400 text-xs line-clamp-2 mb-3">{promo.description}</p>
                <div className="flex items-center justify-between text-xs text-gray-400">
                  <span className="flex items-center gap-1">
                    <Calendar className="w-3 h-3" />
                    {formatDate(promo.startDate)} – {formatDate(promo.endDate)}
                  </span>
                </div>
                {/* Actions */}
                <div className="flex gap-2 mt-3 pt-3 border-t border-gray-100">
                  <button onClick={() => openEdit(promo)}
                    className="flex-1 flex items-center justify-center gap-1.5 py-1.5 rounded-lg
                    text-xs font-medium text-blue-600 hover:bg-blue-50 transition-all">
                    <Edit2 className="w-3.5 h-3.5" /> Sửa
                  </button>
                  <button onClick={() => toggleMutation.mutate(promo.id)}
                    disabled={toggleMutation.isPending}
                    className={cn(
                      'flex-1 flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-xs font-medium transition-all',
                      promo.isActive ? 'text-amber-600 hover:bg-amber-50' : 'text-green-600 hover:bg-green-50'
                    )}>
                    {promo.isActive ? <ToggleRight className="w-3.5 h-3.5" /> : <ToggleLeft className="w-3.5 h-3.5" />}
                    {promo.isActive ? 'Tắt' : 'Bật'}
                  </button>
                  <button onClick={() => setDelete(promo)}
                    className="flex-1 flex items-center justify-center gap-1.5 py-1.5 rounded-lg
                    text-xs font-medium text-red-500 hover:bg-red-50 transition-all">
                    <Trash2 className="w-3.5 h-3.5" /> Xóa
                  </button>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      )}

      {/* Form Modal */}
      <Modal open={showForm} onClose={() => setShowForm(false)}
        title={editTarget ? 'Chỉnh sửa khuyến mãi' : 'Tạo chương trình khuyến mãi'}
        size="lg">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6 p-6">
          {/* Khối 1: Thông tin cơ bản */}
          <div className="space-y-4">
            <Field label="Tiêu đề" required error={errors.title?.message}>
              <Input {...register('title')} error={!!errors.title} placeholder="VD: Mùa hè rực rỡ - Giảm 30%" />
            </Field>
            <Field label="Mô tả" required error={errors.description?.message}>
              <Textarea {...register('description')} error={!!errors.description} rows={3}
                placeholder="Nội dung chi tiết chương trình khuyến mãi..." />
            </Field>
          </div>

          {/* Khối 2: Cấu hình Timeline & Settings */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-gray-50 p-4 rounded-2xl border border-gray-100">
            <Field label="Ngày bắt đầu" required error={errors.startDate?.message}>
              <Input {...register('startDate')} type="date" error={!!errors.startDate} className="bg-white" />
            </Field>
            <Field label="Ngày kết thúc" required error={errors.endDate?.message}>
              <Input {...register('endDate')} type="date" error={!!errors.endDate} className="bg-white" />
            </Field>
            <Field label="Link đích (URL)" error={errors.targetUrl?.message} info="Dẫn người dùng đến phim/rạp cụ thể.">
              <Input {...register('targetUrl')} placeholder="/movies hoặc /movies?promo=xxx" className="bg-white" />
            </Field>
            <Field label="Độ ưu tiên (0–100)" error={errors.priority?.message} info="Số lớn hơn sẽ hiện trước.">
              <Input {...register('priority')} type="number" min={0} max={100} placeholder="5" className="bg-white" />
            </Field>
            <div className="md:col-span-2 pt-2 border-t border-gray-200 mt-2">
              <Switch checked={isPopup} onChange={v => setValue('isPopup', v)} label="Kích hoạt Popup quảng cáo khi mở App" />
            </div>
          </div>

          {/* Khối 3: Hình ảnh (Full width) */}
          <div className="w-full">
            <ImageUploader 
              label="Hình ảnh banner"
              value={imageUrl}
              onUpload={handleImageUpload}
              isLoading={isUploadingImage}
              aspectRatio={isPopup ? "2:3" : "16:9"}
              helperText={isPopup ? "Ảnh dọc (2:3) cho Popup." : "Ảnh ngang (16:9) cho Banner."}
            />
          </div>

          <div className="flex gap-3 pt-4 border-t border-gray-100">
            <Button type="button" variant="ghost" onClick={() => setShowForm(false)} className="flex-1">Hủy</Button>
            <Button type="submit" className="flex-1" isLoading={createMutation.isPending || updateMutation.isPending || isUploadingImage} disabled={isUploadingImage}>
              {editTarget ? 'Lưu thay đổi' : 'Tạo khuyến mãi'}
            </Button>
          </div>
        </form>

      </Modal>

      <ConfirmDialog
        open={!!deleteTarget} onClose={() => setDelete(null)}
        onConfirm={() => deleteMutation.mutate(deleteTarget.id)}
        isLoading={deleteMutation.isPending}
        title="Xóa khuyến mãi?" confirmLabel="Xóa"
        message={`Xóa khuyến mãi "${deleteTarget?.title}"?`}
      />
    </div>
  )
}
