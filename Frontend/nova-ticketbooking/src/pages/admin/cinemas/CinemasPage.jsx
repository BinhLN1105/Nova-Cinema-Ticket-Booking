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
import ImageUploader from '@/components/admin/ImageUploader'

const CITIES = [
  { value: 'Ho Chi Minh', label: 'TP. Hồ Chí Minh' },
  { value: 'Ha Noi',      label: 'Hà Nội' },
  { value: 'Da Nang',     label: 'Đà Nẵng' },
  { value: 'Can Tho',     label: 'Cần Thơ' },
  { value: 'Hai Phong',   label: 'Hải Phòng' },
  { value: 'OTHER',       label: '+ Thêm thành phố khác...' },
]

const EMPTY_FORM = { name: '', address: '', city: '', phone: '', imageUrl: '' }

export default function CinemasPage() {
  const [search, setSearch]       = useState('')
  const [cityFilter, setCityFilter] = useState('')
  const [formOpen, setFormOpen]   = useState(false)
  const [editing, setEditing]     = useState(null)     // cinema object or null
  const [form, setForm]           = useState(EMPTY_FORM)
  const [isUploadingImage, setIsUploadingImage] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [toggleTarget, setToggleTarget] = useState(null)
  
  const [isOtherCity, setIsOtherCity] = useState(false)

  // Screens modal state
  const [screensCinema, setScreensCinema] = useState(null)
  const qc = useQueryClient()

  // Fetch all cinemas (including inactive ones)
  const { data: cinemas = [], isLoading } = useQuery({
    queryKey: ['admin', 'cinemas'],
    queryFn: () => cinemaApi.adminGetAll(),
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

  // Toggle Status
  const toggleMutation = useMutation({
    mutationFn: (id) => cinemaApi.toggleStatus(id),
    onSuccess: (res) => {
      toast.success(res.isActive ? 'Đã kích hoạt rạp' : 'Đã vô hiệu hoá rạp')
      qc.invalidateQueries({ queryKey: ['admin', 'cinemas'] })
      setToggleTarget(null)
    },
  })

  // Hard Delete
  const deleteMutation = useMutation({
    mutationFn: () => cinemaApi.delete(deleteTarget?.id),
    onSuccess: () => {
      toast.success('Đã xoá rạp vĩnh viễn')
      qc.invalidateQueries({ queryKey: ['admin', 'cinemas'] })
      setDeleteTarget(null)
    },
    onError: (err) => {
      // Backend handles the logic: if has data, throw error
      const msg = err.response?.data?.message || 'Không thể xoá rạp này'
      toast.error(msg)
      setDeleteTarget(null)
    }
  })

  const openCreate = () => {
    setEditing(null)
    setForm(EMPTY_FORM)
    setFormOpen(true)
  }

  const openEdit = (cinema) => {
    setEditing(cinema)
    const currentCity = cinema.city ?? '';
    const isStandardCity = CITIES.some(c => c.value === currentCity && c.value !== 'OTHER');
    setForm({
      name: cinema.name ?? '',
      address: cinema.address ?? '',
      city: cinema.city ?? '',
      phone: cinema.phone ?? '',
      imageUrl: cinema.imageUrl ?? '',
    })
    setIsOtherCity(!!currentCity && !isStandardCity)
    setFormOpen(true)
  }

  const closeForm = () => {
    setFormOpen(false)
    setEditing(null)
    setForm(EMPTY_FORM)
    setIsOtherCity(false)
  }

  const handleSave = () => {
    if (!form.name.trim()) return toast.error('Tên rạp không được để trống')
    if (!form.address.trim()) return toast.error('Địa chỉ không được để trống')
    if (!form.city.trim()) return toast.error('Chọn thành phố')
    saveMutation.mutate()
  }

  const handleImageUpload = async (source, type) => {
    if (!editing?.id) {
      toast.error('Vui lòng lưu thông tin rạp trước khi tải ảnh')
      return
    }

    setIsUploadingImage(true)
    try {
      let res;
      if (type === 'file') {
        res = await cinemaApi.uploadImage(editing.id, source)
      } else {
        res = await cinemaApi.uploadImageUrl(editing.id, source)
      }
      setForm(prev => ({ ...prev, imageUrl: res.imageUrl }))
      qc.invalidateQueries({ queryKey: ['admin', 'cinemas'] })
    } finally {
      setIsUploadingImage(false)
    }
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
        <button 
          onClick={() => setToggleTarget(c)}
          disabled={toggleMutation.isPending}
          className="group transition-all"
          title={c.isActive ? "Bấm để vô hiệu hóa" : "Bấm để kích hoạt lại"}
        >
          <StatusBadge
            label={c.isActive ? 'Hoạt động' : 'Vô hiệu'}
            color={c.isActive ? 'green' : 'gray'}
            className="group-hover:ring-2 group-hover:ring-brand-200 cursor-pointer"
          />
        </button>
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
            className="p-2 rounded-lg hover:bg-red-50 text-gray-400 hover:text-red-500 transition-all" title="Xoá vĩnh viễn">
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
        title={editing ? 'Cập nhật rạp' : 'Thêm rạp mới'} size="lg">
        <div className="p-6 space-y-6">
          <div className="space-y-4">
            <Field label="Tên rạp" required>
              <Input value={form.name} onChange={e => set('name', e.target.value)}
                placeholder="VD: Nova Cinema Quận 1" />
            </Field>
            <Field label="Địa chỉ" required>
              <Input value={form.address} onChange={e => set('address', e.target.value)}
                placeholder="VD: 123 Nguyễn Huệ, Quận 1" />
            </Field>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-3">
                <Field label="Thành phố" required>
                  <Select 
                    value={isOtherCity ? 'OTHER' : form.city}
                    onChange={e => {
                      const val = e.target.value;
                      if (val === 'OTHER') {
                        setIsOtherCity(true);
                        set('city', ''); 
                      } else {
                        setIsOtherCity(false);
                        set('city', val);
                      }
                    }}
                    options={CITIES} 
                    placeholder="Chọn thành phố"
                  />
                </Field>
                {isOtherCity && (
                  <Input 
                    value={form.city} 
                    onChange={e => set('city', e.target.value)}
                    placeholder="VD: Nha Trang" 
                    autoFocus
                  />
                )}
              </div>
              
              <Field label="Số điện thoại">
                <Input value={form.phone} onChange={e => set('phone', e.target.value)}
                  placeholder="VD: 0281234567" type="tel" />
              </Field>
            </div>
          </div>

          <div className="w-full">
            <ImageUploader 
              label="Hình ảnh rạp"
              value={form.imageUrl}
              onUpload={handleImageUpload}
              isLoading={isUploadingImage}
              aspectRatio="16:9"
              helperText="Ảnh ngang (16:9). Sẽ hiển thị ở trang chọn rạp."
            />
          </div>

          <div className="flex gap-3 pt-4 border-t border-gray-100">
            <Button variant="ghost" onClick={closeForm} className="flex-1">Huỷ</Button>
            <Button 
              onClick={handleSave} 
              loading={saveMutation.isPending || isUploadingImage} 
              className="flex-1"
              disabled={isUploadingImage}
            >
              {isUploadingImage ? 'Đang tải ảnh...' : editing ? 'Cập nhật' : 'Thêm rạp'}
            </Button>
          </div>
        </div>
      </Modal>


      {/* Delete confirm (Hard delete logic) */}
      <ConfirmDialog
        open={!!deleteTarget} onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteMutation.mutate()} loading={deleteMutation.isPending}
        variant="danger"
        title="Xoá rạp vĩnh viễn?" 
        confirmLabel="Xoá ngay"
        message={`Hệ thống chỉ cho phép xoá nếu rạp "${deleteTarget?.name}" chưa có dữ liệu vận hành. Bạn có chắc chắn muốn thực hiện?`}
      />

      {/* Toggle Status confirm */}
      <ConfirmDialog
        open={!!toggleTarget} 
        onClose={() => setToggleTarget(null)}
        onConfirm={() => toggleMutation.mutate(toggleTarget?.id)} 
        loading={toggleMutation.isPending}
        variant={toggleTarget?.isActive ? "warning" : "primary"}
        title={toggleTarget?.isActive ? "Vô hiệu hoá rạp?" : "Kích hoạt lại rạp?"}
        confirmLabel={toggleTarget?.isActive ? "Vô hiệu hoá" : "Kích hoạt"}
        message={
          toggleTarget?.isActive
            ? `Rạp "${toggleTarget?.name}" sẽ bị ẩn khỏi ứng dụng khách hàng. Suất chiếu của rạp cũng sẽ không hiển thị.`
            : `Rạp "${toggleTarget?.name}" sẽ xuất hiện trở lại trên ứng dụng để khách hàng đặt vé.`
        }
      />

      <ScreensModal 
        open={!!screensCinema} 
        onClose={() => setScreensCinema(null)} 
        cinema={screensCinema} 
      />
    </div>
  )
}
