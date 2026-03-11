import { useState } from 'react'
import { Bell, Shield, Palette, Globe, Save, ChevronRight } from 'lucide-react'
import { AdminCard, PageHeader } from '@/components/common/ui/AdminTable'
import { Field, Input, Switch, Button } from '@/components/common/ui/FormElements'
import toast from 'react-hot-toast'

const TABS = [
  { id: 'general',       label: 'Chung',          icon: Globe },
  { id: 'notifications', label: 'Thông báo',      icon: Bell },
  { id: 'security',      label: 'Bảo mật',        icon: Shield },
  { id: 'appearance',    label: 'Giao diện',      icon: Palette },
]

export default function SettingsPage() {
  const [activeTab, setTab] = useState('general')
  const [notifSettings, setNotif] = useState({
    emailBooking:  true,
    emailPromo:    false,
    pushBooking:   true,
    pushPromo:     true,
  })

  const save = () => toast.success('Đã lưu cài đặt')

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
              <Field label="URL website">
                <Input defaultValue="https://novaticket.vn" type="url" />
              </Field>
              <Field label="Số ghế tối đa / lần đặt">
                <Input defaultValue="8" type="number" min={1} max={20} />
              </Field>
              <Field label="Thời gian giữ ghế (phút)">
                <Input defaultValue="10" type="number" min={5} max={30} />
              </Field>
              <div className="pt-2 border-t border-gray-100">
                <Button onClick={save} leftIcon={<Save className="w-4 h-4" />}>Lưu cài đặt</Button>
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
        </div>
      </div>
    </div>
  )
}
