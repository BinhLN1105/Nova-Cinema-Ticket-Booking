import { useState, useEffect } from 'react'
import { Bell, Shield, Palette, Globe, Save, ChevronRight, Layout } from 'lucide-react'
import { AdminCard, PageHeader } from '@/components/common/ui/AdminTable'
import { Field, Input, Switch, Button, Select } from '@/components/common/ui/FormElements'
import MoviePicker from '@/components/admin/MoviePicker'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { systemConfigApi } from '@/api/endpoints'
import toast from 'react-hot-toast'

const TABS = [
  { id: 'general',       label: 'Chung',          icon: Globe },
  { id: 'notifications', label: 'Thông báo',      icon: Bell },
  { id: 'security',      label: 'Bảo mật',        icon: Shield },
  { id: 'appearance',    label: 'Giao diện',      icon: Palette },
  { id: 'home',          label: 'Trang chủ',      icon: Layout },
]

export default function SettingsPage() {
  const [activeTab, setTab] = useState('general')
  const queryClient = useQueryClient()

  // Lấy system configs từ backend
  const { data: configsRes, isLoading: isLoadingConfigs } = useQuery({
    queryKey: ['system-configs'],
    queryFn: () => systemConfigApi.getAll()
  })

  const configs = configsRes?.data || {}

  const updateConfigMutation = useMutation({
    mutationFn: ({ key, value, description }) => systemConfigApi.update(key, value, description),
    onSuccess: () => {
      toast.success('Đã lưu cấu hình thành công')
      queryClient.invalidateQueries(['system-configs'])
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Có lỗi xảy ra khi lưu')
  })

  // Local state form để edit
  const [localConfigs, setLocalConfigs] = useState({})

  // Update local config when data is fetched
  useEffect(() => {
    if (configsRes) {
      setLocalConfigs(configsRes)
    }
  }, [configsRes])

  const handleConfigChange = (key, value) => {
    setLocalConfigs(prev => ({ ...prev, [key]: value }))
  }

  const handleSaveConfigs = () => {
    // Save altered configs
    Object.keys(localConfigs).forEach(key => {
      if (configs[key] !== localConfigs[key]) {
        updateConfigMutation.mutate({ key, value: localConfigs[key], description: "" })
      }
    })
  }

  const [notifSettings, setNotif] = useState({
    emailBooking:  true,
    emailPromo:    false,
    pushBooking:   true,
    pushPromo:     true,
  })

  const save = () => toast.success('Tính năng lưu đang được phát triển')

  return (
    <div className="space-y-6">
      <PageHeader title="Cài đặt hệ thống" subtitle="Quản lý cấu hình ứng dụng" />

      <div className="flex gap-6">
        {/* Sidebar */}
        <div className="w-56 flex-shrink-0">
          <AdminCard>
            <nav className="p-2 space-y-0.5">
              {TABS.map(({ id, label, icon: Icon }) => (
                <button key={id} onClick={() => setTab(id)}
                  className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all text-left ${
                    activeTab === id
                      ? 'bg-brand-50 text-brand-600 border border-brand-100'
                      : 'text-gray-600 hover:bg-gray-50'
                  }`}>
                  <Icon className="w-4 h-4 flex-shrink-0" />
                  {label}
                  {activeTab === id && <ChevronRight className="w-4 h-4 ml-auto" />}
                </button>
              ))}
            </nav>
          </AdminCard>
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          {activeTab === 'general' && (
            <AdminCard className="p-6 space-y-5">
              <h2 className="font-bold text-gray-900 text-lg font-display">Cài đặt chung</h2>
              <Field label="Tên ứng dụng">
                <Input defaultValue="NovaTicket" placeholder="Tên thương hiệu" />
              </Field>
              <Field label="Email hỗ trợ">
                <Input defaultValue="support@novaticket.vn" type="email" />
              </Field>
              <Field label="Hotline">
                <Input defaultValue="1900 1234" type="tel" />
              </Field>
              
              <h2 className="font-bold text-gray-900 text-lg font-display pt-4 border-t border-gray-100 mt-4">Cấu hình tham số hệ thống</h2>
              {isLoadingConfigs ? <div className="text-gray-500">Đang tải cấu hình...</div> : (
                <div className="space-y-4">
                  <Field label="Thời gian giữ ghế (Phút)" info="Thời gian tối đa để khách hàng thanh toán sau khi chọn ghế. Sau thời gian này ghế sẽ bị nhả ra.">
                    <Input value={localConfigs['DEFAULT_SEAT_HOLD_TIME'] || ''} type="number" onChange={(e) => handleConfigChange('DEFAULT_SEAT_HOLD_TIME', e.target.value)} />
                  </Field>
                  <Field label="Giữ ghế sát giờ (Phút)" info="Thời gian giữ ghế khi suất chiếu bắt đầu trong vòng 15 phút tới. Ngắn hơn để tránh lãng phí ghế.">
                    <Input value={localConfigs['LATE_SEAT_HOLD_TIME'] || ''} type="number" onChange={(e) => handleConfigChange('LATE_SEAT_HOLD_TIME', e.target.value)} />
                  </Field>
                  <Field label="Cho phép đặt vé trễ (Phút)" info="Thời gian tối đa sau khi phim đã chiếu mà khách vẫn có thể mua vé (ví dụ: đang chiếu trailer).">
                    <Input value={localConfigs['LATE_BOOKING_ALLOWANCE_MINS'] || ''} type="number" onChange={(e) => handleConfigChange('LATE_BOOKING_ALLOWANCE_MINS', e.target.value)} />
                  </Field>
                  <Field label="Thời gian hủy vé tối thiểu (Giờ)" info="Khách hàng phải hủy vé trước giờ chiếu ít nhất X giờ mới được chấp nhận hoàn tiền.">
                    <Input value={localConfigs['CANCEL_MIN_HOURS_BEFORE'] || ''} type="number" onChange={(e) => handleConfigChange('CANCEL_MIN_HOURS_BEFORE', e.target.value)} />
                  </Field>
                  <Field label="Tỉ lệ hoàn tiền (%)" info="Phần trăm số tiền sẽ được hoàn lại dưới dạng CinePoint khi khách hàng hủy vé thành công.">
                    <Input value={localConfigs['REFUND_PERCENT_CINEPOINT'] || ''} type="number" onChange={(e) => handleConfigChange('REFUND_PERCENT_CINEPOINT', e.target.value)} />
                  </Field>
                </div>
              )}
              <div className="pt-2 border-t border-gray-100">
                <Button onClick={handleSaveConfigs} disabled={updateConfigMutation.isLoading} leftIcon={<Save className="w-4 h-4" />}>
                  {updateConfigMutation.isLoading ? 'Đang lưu...' : 'Lưu cấu hình hệ thống'}
                </Button>
              </div>
            </AdminCard>
          )}

          {activeTab === 'notifications' && (
            <AdminCard className="p-6">
              <h2 className="font-bold text-gray-900 text-lg font-display mb-6">Cài đặt thông báo</h2>
              <div className="space-y-6">
                <div>
                  <h3 className="text-sm font-semibold text-gray-700 mb-3 uppercase tracking-wider">Email</h3>
                  <div className="space-y-4 pl-1">
                    {[
                      { key: 'emailBooking', label: 'Xác nhận đặt vé', desc: 'Gửi email xác nhận khi đặt vé thành công' },
                      { key: 'emailPromo', label: 'Khuyến mãi', desc: 'Gửi email về ưu đãi và chương trình đặc biệt' },
                    ].map(({ key, label, desc }) => (
                      <Switch key={key} checked={notifSettings[key]}
                        onChange={v => setNotif(n => ({ ...n, [key]: v }))}
                        label={label} description={desc} />
                    ))}
                  </div>
                </div>
                <div className="border-t border-gray-100 pt-6">
                  <h3 className="text-sm font-semibold text-gray-700 mb-3 uppercase tracking-wider">Push Notification</h3>
                  <div className="space-y-4 pl-1">
                    {[
                      { key: 'pushBooking', label: 'Cập nhật đặt vé', desc: 'Thông báo trạng thái vé theo thời gian thực' },
                      { key: 'pushPromo', label: 'Ưu đãi mới', desc: 'Thông báo khi có ưu đãi hoặc phim mới' },
                    ].map(({ key, label, desc }) => (
                      <Switch key={key} checked={notifSettings[key]}
                        onChange={v => setNotif(n => ({ ...n, [key]: v }))}
                        label={label} description={desc} />
                    ))}
                  </div>
                </div>
                <div className="pt-2 border-t border-gray-100">
                  <Button onClick={save} leftIcon={<Save className="w-4 h-4" />}>Lưu cài đặt</Button>
                </div>
              </div>
            </AdminCard>
          )}

          {activeTab === 'security' && (
            <AdminCard className="p-6 space-y-5">
              <h2 className="font-bold text-gray-900 text-lg font-display">Bảo mật</h2>
              <Field label="Mật khẩu hiện tại">
                <Input type="password" placeholder="••••••••" />
              </Field>
              <Field label="Mật khẩu mới">
                <Input type="password" placeholder="Ít nhất 8 ký tự" />
              </Field>
              <Field label="Xác nhận mật khẩu mới">
                <Input type="password" placeholder="Nhập lại mật khẩu mới" />
              </Field>
              <div className="border-t border-gray-100 pt-4 space-y-3">
                <Switch label="Xác thực 2 bước" description="Tăng cường bảo mật tài khoản bằng OTP" checked={false} onChange={() => {}} />
                <Switch label="Đăng nhập đáng ngờ" description="Nhận cảnh báo khi có đăng nhập từ thiết bị lạ" checked={true} onChange={() => {}} />
              </div>
              <Button onClick={save} leftIcon={<Save className="w-4 h-4" />}>Đổi mật khẩu</Button>
            </AdminCard>
          )}

          {activeTab === 'appearance' && (
            <AdminCard className="p-6 space-y-5">
              <h2 className="font-bold text-gray-900 text-lg font-display">Giao diện</h2>
              <Field label="Màu thương hiệu chính">
                <div className="flex items-center gap-3">
                  <input type="color" defaultValue="#E50914"
                    className="w-12 h-10 rounded-lg border border-gray-200 cursor-pointer p-1" />
                  <Input defaultValue="#E50914" className="max-w-[140px]" />
                </div>
              </Field>
              <Field label="Màu phụ (Gold)">
                <div className="flex items-center gap-3">
                  <input type="color" defaultValue="#F5A623"
                    className="w-12 h-10 rounded-lg border border-gray-200 cursor-pointer p-1" />
                  <Input defaultValue="#F5A623" className="max-w-[140px]" />
                </div>
              </Field>
              <div className="border-t border-gray-100 pt-4 space-y-3">
                <Switch label="Hiệu ứng animation" description="Bật/tắt các hiệu ứng chuyển động trên website" checked={true} onChange={() => {}} />
                <Switch label="Chế độ compact" description="Thu gọn khoảng cách để hiển thị nhiều nội dung hơn" checked={false} onChange={() => {}} />
              </div>
              <Button onClick={save} leftIcon={<Save className="w-4 h-4" />}>Lưu giao diện</Button>
            </AdminCard>
          )}

          {activeTab === 'home' && (
            <AdminCard className="p-6 space-y-6">
              <h2 className="font-bold text-gray-900 text-lg font-display">Cấu hình Trang chủ (Android)</h2>
              <div className="p-4 bg-blue-50 border border-blue-100 rounded-2xl text-sm text-blue-700 leading-relaxed">
                Thiết lập cách hiển thị phim trên Banner nổi bật (Hero Banner) của ứng dụng di động.
              </div>

              <Field label="Chế độ hiển thị Banner" info="Lựa chọn tiêu chí để hệ thống tự động lọc 5 phim lên đầu trang chủ.">
                <Select 
                  value={localConfigs['HERO_SECTION_MODE'] || 'TOP_RATING'} 
                  onChange={(e) => handleConfigChange('HERO_SECTION_MODE', e.target.value)}
                  options={[
                    { value: 'TOP_SALES',   label: '🔥 Top Sales (Bán chạy nhất)' },
                    { value: 'TOP_RATING',   label: '⭐ Top Rating (Đánh giá cao nhất)' },
                    { value: 'NEW_RELEASE', label: '🆕 New Release (Phim mới nhất)' },
                    { value: 'MANUAL',      label: '🖱️ Manual (Admin tự chọn phim)' },
                  ]} 
                />
              </Field>

              {localConfigs['HERO_SECTION_MODE'] === 'MANUAL' && (
                <Field label="Danh sách phim tự chọn" info="Tìm kiếm và chọn các bộ phim bạn muốn hiển thị cố định.">
                  <MoviePicker 
                    value={localConfigs['HERO_SECTION_IDS'] || ''} 
                    onChange={(val) => handleConfigChange('HERO_SECTION_IDS', val)} 
                  />
                </Field>
              )}

              <div className="pt-4 border-t border-gray-100 flex items-center justify-between">
                <p className="text-xs text-gray-400 max-w-xs">
                  * Thay đổi sẽ có hiệu lực ngay lập tức khi người dùng làm mới trang chủ trên ứng dụng.
                </p>
                <Button onClick={handleSaveConfigs} disabled={updateConfigMutation.isLoading} leftIcon={<Save className="w-4 h-4" />}>
                  {updateConfigMutation.isLoading ? 'Đang lưu...' : 'Lưu cấu hình trang chủ'}
                </Button>
              </div>
            </AdminCard>
          )}
        </div>
      </div>
    </div>
  )
}
