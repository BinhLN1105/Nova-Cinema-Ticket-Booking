import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Edit2, Trash2, MapPin, Phone, Building2, ImageIcon, MonitorPlay } from 'lucide-react'
import { cinemaApi } from '@/api/endpoints'
import { AdminCard, PageHeader, Table, StatusBadge } from '@/components/common/ui/AdminTable'
import { SearchInput, Button, Field, Input, Select } from '@/components/common/ui/FormElements'
import { Modal, ConfirmDialog } from '@/components/common/ui/Modal'
import { cn } from '@/utils'
import toast from 'react-hot-toast'
import ScreensModal from './ScreensModal'

const CITIES = [
  { value: 'Ho Chi Minh', label: 'TP. Hồ Chí Minh' },
  { value: 'Ha Noi',      label: 'Hà Nội' },
  { value: 'Da Nang',     label: 'Đà Nẵng' },
  { value: 'Can Tho',     label: 'Cần Thơ' },
  { value: 'Hai Phong',   label: 'Hải Phòng' },
]

const EMPTY_FORM = { name: '', address: '', city: '', phone: '', imageUrl: '' }

export default function CinemasPage() {
  const [search, setSearch]       = useState('')
  const [cityFilter, setCityFilter] = useState('')
  const [formOpen, setFormOpen]   = useState(false)
  const [editing, setEditing]     = useState(null)     // cinema object or null
  const [form, setForm]           = useState(EMPTY_FORM)
  const [deleteTarget, setDeleteTarget] = useState(null)
  
  // Screens modal state
  const [screensCinema, setScreensCinema] = useState(null)
  const qc = useQueryClient()

  // Fetch all cinemas
  const { data: cinemas = [], isLoading } = useQuery({
    queryKey: ['admin', 'cinemas', cityFilter],
    queryFn: () => cinemaApi.getAll(cityFilter || undefined),
  })

  // Filter locally by search
  const filtered = cinemas.filter(c =>
    !search || c.name?.toLowerCase().includes(search.toLowerCase()) ||
    c.address?.toLowerCase().includes(search.toLowerCase())
  )

  // Create / Update
  const saveMutation = useMutation({
    mutationFn: () => editing
      ? cinemaApi.update(editing.id, form)
      : cinemaApi.create(form),
    onSuccess: () => {
      toast.success(editing ? 'Cập nhật rạp thành công' : 'Thêm rạp thành công')
      qc.invalidateQueries({ queryKey: ['admin', 'cinemas'] })
      closeForm()
    },
  })

  // Delete
  const deleteMutation = useMutation({
    mutationFn: () => cinemaApi.delete(deleteTarget?.id),
    onSuccess: () => {
      toast.success('Đã vô hiệu hoá rạp')
      qc.invalidateQueries({ queryKey: ['admin', 'cinemas'] })
      setDeleteTarget(null)
    },
  })

  const openCreate = () => {
    setEditing(null)
    setForm(EMPTY_FORM)
    setFormOpen(true)
  }

  const openEdit = (cinema) => {
    setEditing(cinema)
    setForm({
      name: cinema.name ?? '',
      address: cinema.address ?? '',
      city: cinema.city ?? '',
      phone: cinema.phone ?? '',
      imageUrl: cinema.imageUrl ?? '',
    })
    setFormOpen(true)
  }

  const closeForm = () => {
    setFormOpen(false)
    setEditing(null)
    setForm(EMPTY_FORM)
  }

  const handleSave = () => {
    if (!form.name.trim()) return toast.error('Tên rạp không được để trống')
    if (!form.address.trim()) return toast.error('Địa chỉ không được để trống')
    if (!form.city.trim()) return toast.error('Chọn thành phố')
    saveMutation.mutate()
  }

  const set = (key, value) => setForm(f => ({ ...f, [key]: value }))

  const columns = [
    {
      key: 'name', header: 'Tên rạp',
      render: (c) => (
        <div className="flex items-center gap-3">
          {c.imageUrl ? (
            <img src={c.imageUrl} alt={c.name}
              className="w-10 h-10 rounded-xl object-cover flex-shrink-0 border border-gray-100" />
          ) : (
            <div className="w-10 h-10 rounded-xl bg-brand-50 flex items-center justify-center flex-shrink-0">
              <Building2 className="w-5 h-5 text-brand-400" />
            </div>
          )}
          <div>
            <p className="font-semibold text-gray-900 text-sm">{c.name}</p>
            <p className="text-xs text-gray-400 flex items-center gap-1 mt-0.5">
              <MapPin className="w-3 h-3" /> {c.city}
            </p>
          </div>
        </div>
      ),
    },
    {
      key: 'address', header: 'Địa chỉ',
      render: (c) => <span className="text-gray-600 text-sm line-clamp-1 max-w-xs">{c.address}</span>,
    },
    {
      key: 'phone', header: 'SĐT',
      render: (c) => (
        <span className="text-gray-600 text-sm flex items-center gap-1.5">
          <Phone className="w-3.5 h-3.5 text-gray-400" /> {c.phone ?? '—'}
        </span>
      ),
    },
    {
      key: 'status', header: 'Trạng thái',
      render: (c) => (
        <StatusBadge
          label={c.isActive !== false ? 'Hoạt động' : 'Đã tắt'}
          color={c.isActive !== false ? 'green' : 'red'}
        />
      ),
    },
    {
      key: 'actions', header: '',
      render: (c) => (
        <div className="flex items-center justify-end gap-1">
          <button onClick={() => openEdit(c)}
            className="p-2 rounded-lg hover:bg-blue-50 text-gray-400 hover:text-blue-500 transition-all" title="Chỉnh sửa">
            <Edit2 className="w-4 h-4" />
          </button>
          <button onClick={() => setScreensCinema(c)}
            className="p-2 rounded-lg hover:bg-emerald-50 text-gray-400 hover:text-emerald-500 transition-all" title="Quản lý phòng chiếu">
            <MonitorPlay className="w-4 h-4" />
          </button>
          <button onClick={() => setDeleteTarget(c)}
            className="p-2 rounded-lg hover:bg-red-50 text-gray-400 hover:text-red-500 transition-all" title="Vô hiệu hoá">
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader title="Quản lý rạp chiếu" subtitle={`${filtered.length} rạp`}
        action={
          <Button leftIcon={<Plus className="w-4 h-4" />} onClick={openCreate}>
            Thêm rạp
          </Button>
        }
      />

      <AdminCard>
        <div className="p-4 border-b border-gray-100 flex flex-col sm:flex-row gap-3">
          <SearchInput value={search} onChange={setSearch}
            placeholder="Tìm theo tên, địa chỉ..." className="flex-1 max-w-xs" />
          <Select value={cityFilter}
            onChange={e => setCityFilter(e.target.value)}
            options={CITIES} placeholder="Tất cả thành phố" className="w-48"
          />
        </div>
        <Table columns={columns} data={filtered} loading={isLoading}
          rowKey={c => c.id} emptyMessage="Chưa có rạp nào" emptyIcon="🏢" />
      </AdminCard>

      {/* Create / Edit Modal */}
      <Modal open={formOpen} onClose={closeForm}
        title={editing ? 'Cập nhật rạp' : 'Thêm rạp mới'} size="md">
        <div className="p-6 space-y-4">
          <Field label="Tên rạp" required>
            <Input value={form.name} onChange={e => set('name', e.target.value)}
              placeholder="VD: Nova Cinema Quận 1" />
          </Field>
          <Field label="Địa chỉ" required>
            <Input value={form.address} onChange={e => set('address', e.target.value)}
              placeholder="VD: 123 Nguyễn Huệ, Quận 1" />
          </Field>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Thành phố" required>
              <Select value={form.city}
                onChange={e => set('city', e.target.value)}
                options={CITIES} placeholder="Chọn thành phố"
              />
            </Field>
            <Field label="Số điện thoại">
              <Input value={form.phone} onChange={e => set('phone', e.target.value)}
                placeholder="VD: 0281234567" type="tel" />
            </Field>
          </div>
          <Field label="Ảnh rạp (URL)">
            <Input value={form.imageUrl} onChange={e => set('imageUrl', e.target.value)}
              placeholder="https://..." type="url"
              leftIcon={<ImageIcon className="w-4 h-4" />} />
          </Field>
          <div className="flex gap-3 pt-2">
            <Button variant="ghost" onClick={closeForm} className="flex-1">Huỷ</Button>
            <Button onClick={handleSave} loading={saveMutation.isPending} className="flex-1">
              {editing ? 'Cập nhật' : 'Thêm rạp'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Delete confirm */}
      <ConfirmDialog
        open={!!deleteTarget} onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteMutation.mutate()} loading={deleteMutation.isPending}
        title="Vô hiệu hoá rạp?" confirmLabel="Vô hiệu hoá"
        message={`Bạn có chắc muốn vô hiệu hoá rạp "${deleteTarget?.name}"?`}
      />

      <ScreensModal 
        open={!!screensCinema} 
        onClose={() => setScreensCinema(null)} 
        cinema={screensCinema} 
      />
    </div>
  )
}
