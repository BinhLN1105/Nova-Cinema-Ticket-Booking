import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Edit2, Trash2, MapPin, Phone, Building2, MonitorPlay, Map, Sparkles, Loader2 } from 'lucide-react'
import { cinemaApi } from '@/api/endpoints'
import { AdminCard, PageHeader, Table, StatusBadge } from '@/components/common/ui/AdminTable'
import { SearchInput, Button, Field, Input, Select, Label } from '@/components/common/ui/FormElements'
import { Modal, ConfirmDialog } from '@/components/common/ui/Modal'
import { cn } from '@/utils'
import toast from 'react-hot-toast'
import ScreensModal from './ScreensModal'
import ImageUploader from '@/components/admin/ImageUploader'
import MapLocationPickerModal from '@/components/admin/MapLocationPickerModal'
import MiniMapPreview from '@/components/admin/MiniMapPreview'

const CITIES = [
  { value: 'Ho Chi Minh', label: 'TP. Hồ Chí Minh' },
  { value: 'Ha Noi',      label: 'Hà Nội' },
  { value: 'Da Nang',     label: 'Đà Nẵng' },
  { value: 'Can Tho',     label: 'Cần Thơ' },
  { value: 'Hai Phong',   label: 'Hải Phòng' },
  { value: 'OTHER',       label: '+ Thêm thành phố khác...' },
]

const EMPTY_FORM = { name: '', address: '', city: '', phone: '', imageUrl: '', latitude: '', longitude: '' }

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
  const [mapPickerOpen, setMapPickerOpen] = useState(false)
  const [isAutoGeocoding, setIsAutoGeocoding] = useState(false)
  
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
    mutationFn: async () => {
      // Split basic info for target request (keeps backward compatibility & saves testcases)
      const basicForm = {
        name: form.name,
        address: form.address,
        city: form.city,
        phone: form.phone,
        imageUrl: form.imageUrl,
      }
      
      let res;
      if (editing) {
        res = await cinemaApi.update(editing.id, basicForm)
        if (form.latitude !== '' || form.longitude !== '') {
          await cinemaApi.updateCoordinates(editing.id, {
            latitude: parseFloat(form.latitude) || 0.0,
            longitude: parseFloat(form.longitude) || 0.0,
          })
        }
      } else {
        res = await cinemaApi.create(basicForm)
        if (form.latitude !== '' || form.longitude !== '') {
          await cinemaApi.updateCoordinates(res.id, {
            latitude: parseFloat(form.latitude) || 0.0,
            longitude: parseFloat(form.longitude) || 0.0,
          })
        }
      }
      return res;
    },
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
      latitude: cinema.latitude ?? '',
      longitude: cinema.longitude ?? '',
    })
    setIsOtherCity(!!currentCity && !isStandardCity)
    setFormOpen(true)
  }

  const closeForm = () => {
    setFormOpen(false)
    setEditing(null)
    setForm(EMPTY_FORM)
    setIsOtherCity(false)
    setMapPickerOpen(false)
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

  // Auto geocode coordinates from address
  const handleAutoGeocodeFromAddress = async () => {
    const query = (form.address + (form.city ? `, ${form.city}` : '')).trim()
    if (!query) {
      toast.error('Vui lòng nhập địa chỉ trước khi tìm tọa độ')
      return
    }
    setIsAutoGeocoding(true)
    try {
      const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=1&countrycodes=vn`)
      const data = await res.json()
      if (data && data.length > 0) {
        const lat = parseFloat(parseFloat(data[0].lat).toFixed(7))
        const lng = parseFloat(parseFloat(data[0].lon).toFixed(7))
        setForm(prev => ({ ...prev, latitude: lat, longitude: lng }))
        toast.success(`Đã tìm thấy và tự động điền tọa độ: ${lat}, ${lng}`)
      } else {
        toast.error('Không tìm thấy tọa độ tự động. Vui lòng bấm "Chọn trên bản đồ" để ghim trực quan.')
      }
    } catch {
      toast.error('Lỗi kết nối khi tìm tọa độ tự động')
    } finally {
      setIsAutoGeocoding(false)
    }
  }

  // Handle smart paste: e.g. "10.86513906366627, 106.61717311425997"
  const handleCoordinateInput = (key, value) => {
    const match = String(value).trim().match(/^(-?\d+(\.\d+)?)[,\s]+(-?\d+(\.\d+)?)$/)
    if (match) {
      const lat = match[1]
      const lng = match[3]
      setForm(prev => ({ ...prev, latitude: lat, longitude: lng }))
      toast.success(`Đã tự động tách và điền tọa độ: ${lat}, ${lng}`)
      return
    }
    set(key, value)
  }

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
      key: 'address', header: 'Địa chỉ & Vị trí',
      render: (c) => (
        <div>
          <span className="text-gray-600 text-sm line-clamp-1 max-w-xs">{c.address}</span>
          {c.latitude && c.longitude ? (
            <span className="text-[11px] text-amber-600 font-mono flex items-center gap-1 mt-0.5" title="Đã có tọa độ GPS cho AI dự báo thời tiết">
              <MapPin className="w-3 h-3 text-amber-500" />
              {Number(c.latitude).toFixed(4)}, {Number(c.longitude).toFixed(4)}
            </span>
          ) : (
            <span className="text-[11px] text-gray-400 italic block mt-0.5">
              Chưa ghim tọa độ GPS
            </span>
          )}
        </div>
      ),
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
            className="p-2 rounded-lg hover:bg-blue-50 text-gray-400 hover:text-blue-500 transition-all cursor-pointer" title="Chỉnh sửa">
            <Edit2 className="w-4 h-4" />
          </button>
          <button onClick={() => setScreensCinema(c)}
            className="p-2 rounded-lg hover:bg-emerald-50 text-gray-400 hover:text-emerald-500 transition-all cursor-pointer" title="Quản lý phòng chiếu">
            <MonitorPlay className="w-4 h-4" />
          </button>
          <button onClick={() => setDeleteTarget(c)}
            className="p-2 rounded-lg hover:bg-red-50 text-gray-400 hover:text-red-500 transition-all cursor-pointer" title="Xoá vĩnh viễn">
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
        <div className="p-6 space-y-6 max-h-[82vh] overflow-y-auto">
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

            {/* Coordinates & Interactive Map Box */}
            <div className="space-y-3 p-4 bg-slate-50 rounded-2xl border border-slate-200/80">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <Label className="text-gray-900 font-semibold flex items-center gap-1.5 mb-0">
                    <MapPin className="w-4 h-4 text-amber-500" />
                    Vị trí tọa độ GPS (Dành cho AI dự báo thời tiết)
                  </Label>
                  <p className="text-xs text-gray-500 mt-0.5">
                    Nhập tọa độ hoặc chọn trên bản đồ để tự động ghim vị trí
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  {form.address && (
                    <button
                      type="button"
                      onClick={handleAutoGeocodeFromAddress}
                      disabled={isAutoGeocoding}
                      className="px-3 py-1.5 bg-white hover:bg-amber-50 text-amber-700 border border-amber-200 rounded-xl text-xs font-semibold flex items-center gap-1.5 shadow-sm transition-all cursor-pointer hover:border-amber-300 disabled:opacity-50"
                      title="Tự động tìm kiếm tọa độ dựa theo địa chỉ và thành phố"
                    >
                      {isAutoGeocoding ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin text-amber-600" />
                      ) : (
                        <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                      )}
                      <span>Tìm theo địa chỉ</span>
                    </button>
                  )}

                  <button
                    type="button"
                    onClick={() => setMapPickerOpen(true)}
                    className="px-3.5 py-1.5 bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-600 hover:to-amber-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm shadow-amber-500/20 active:scale-95 transition-all cursor-pointer"
                  >
                    <Map className="w-3.5 h-3.5" />
                    <span>Chọn trên bản đồ</span>
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-1">
                <Field label="Vĩ độ (Latitude)">
                  <Input 
                    value={form.latitude} 
                    onChange={e => handleCoordinateInput('latitude', e.target.value)}
                    placeholder="VD: 10.8651 (hoặc dán cả cặp tọa độ)" 
                    type="text" 
                  />
                </Field>
                <Field label="Kinh độ (Longitude)">
                  <Input 
                    value={form.longitude} 
                    onChange={e => handleCoordinateInput('longitude', e.target.value)}
                    placeholder="VD: 106.6171" 
                    type="text" 
                  />
                </Field>
              </div>

              {/* Live Interactive Mini Map Preview (Automatically shown below when both valid coordinates are present) */}
              {form.latitude && form.longitude && !isNaN(parseFloat(form.latitude)) && !isNaN(parseFloat(form.longitude)) ? (
                <MiniMapPreview
                  lat={form.latitude}
                  lng={form.longitude}
                  onLocationChange={({ lat, lng }) => {
                    setForm(prev => ({ ...prev, latitude: lat, longitude: lng }))
                  }}
                  onOpenFullMap={() => setMapPickerOpen(true)}
                  onClear={() => {
                    setForm(prev => ({ ...prev, latitude: '', longitude: '' }))
                    toast.success('Đã xóa tọa độ')
                  }}
                />
              ) : (
                <div className="text-[11px] text-gray-500 flex items-center justify-between px-3 py-2 bg-gray-100/70 rounded-xl border border-gray-200/60 mt-2">
                  <span>💡 Khi nhập đủ Vĩ độ và Kinh độ hợp lệ, bản đồ và vị trí ghim sẽ <strong>tự động hiển thị ngay bên dưới</strong>.</span>
                </div>
              )}
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

      {/* Interactive Map Location Picker Modal */}
      <MapLocationPickerModal
        open={mapPickerOpen}
        onClose={() => setMapPickerOpen(false)}
        initialLat={form.latitude}
        initialLng={form.longitude}
        initialAddress={form.address}
        initialCity={form.city}
        cinemaName={form.name}
        onSelectLocation={({ latitude, longitude }) => {
          setForm(prev => ({
            ...prev,
            latitude,
            longitude,
          }))
        }}
      />

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


