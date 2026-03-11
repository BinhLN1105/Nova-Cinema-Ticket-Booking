import { useState } from 'react'
import { motion } from 'framer-motion'
import { useQuery, useMutation } from '@tanstack/react-query'
import { User, Ticket, Bell, Shield, Edit2, Save, Phone, Mail, LogOut, Loader2, Palette } from 'lucide-react'
import { bookingApi, notificationApi } from '@/api/endpoints'
import { api } from '@/api/client'
import { SecurityTab } from '@/components/customer/SecurityTab'
import { AppearanceTab } from '@/components/customer/AppearanceTab'
import { useAuth } from '@/hooks'
import { formatDate, formatDateTime, formatCurrency, getStatusBadge, cn } from '@/utils'
import { useAuthStore } from '@/stores/authStore'
import toast from 'react-hot-toast'

const TABS = [
  { id: 'profile',       label: 'Thông tin',    icon: User },
  { id: 'tickets',       label: 'Vé của tôi',   icon: Ticket },
  { id: 'notifications', label: 'Thông báo',    icon: Bell },
  { id: 'security',      label: 'Bảo mật',      icon: Shield },
  { id: 'appearance',    label: 'Giao diện',    icon: Palette },
]

/** Mask phone: 0123456789 → 0123****89 */
function maskPhone(phone) {
  if (!phone || phone.length < 6) return phone
  const head = phone.slice(0, 4)
  const tail = phone.slice(-2)
  return `${head}${'*'.repeat(phone.length - 6)}${tail}`
}

export default function ProfilePage() {
  const { user, logout } = useAuth()
  const [activeTab, setActiveTab] = useState('profile')
  const [isEditing, setIsEditing] = useState(false)
  const [fullName, setFullName] = useState(user?.fullName ?? '')
  const [phone, setPhone] = useState(user?.phone ?? '')
  const setUser = useAuthStore(s => s.setUser)

  // Tickets
  const { data: ticketsData, isLoading: ticketsLoading } = useQuery({
    queryKey: ['my-tickets'],
    queryFn: () => bookingApi.getMyAll(0, 20),
    enabled: activeTab === 'tickets',
  })

  // Notifications
  const { data: notifData, isLoading: notifLoading } = useQuery({
    queryKey: ['my-notifications'],
    queryFn: () => notificationApi.getAll(0),
    enabled: activeTab === 'notifications',
  })

  // Update profile via API
  const updateMutation = useMutation({
    mutationFn: (data) => api.patch('/users/me', data),
    onSuccess: (res) => {
      setUser({ ...user, fullName: res.fullName ?? fullName, phone: res.phone ?? phone })
      setIsEditing(false)
      toast.success('Đã cập nhật thông tin')
    },
    onError: () => toast.error('Cập nhật thất bại'),
  })

  const handleSaveProfile = () => {
    if (!fullName.trim()) {
      toast.error('Tên không được để trống')
      return
    }
    updateMutation.mutate({ fullName, phone: phone || null })
  }

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-16">
      <div className="max-w-4xl mx-auto px-4 sm:px-6">

        {/* Header */}
        <div className="flex items-center gap-5 mb-8">
          <div className="relative">
            <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-brand-400 to-brand-600
              flex items-center justify-center text-white font-bold text-3xl font-display shadow-glow-red">
              {user?.fullName?.[0]?.toUpperCase() ?? '?'}
            </div>
          </div>
          <div>
            <h1 className="font-display text-2xl font-bold text-white">{user?.fullName}</h1>
            <p className="text-cinema-300 text-sm">{user?.email}</p>
          </div>
        </div>

        {/* Tabs */}
        <div className="flex gap-1 mb-8 overflow-x-auto scrollbar-hide">
          {TABS.map(({ id, label, icon: Icon }) => (
            <button key={id} onClick={() => setActiveTab(id)}
              className={cn(
                'flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium transition-all whitespace-nowrap',
                activeTab === id
                  ? 'bg-brand-500/15 text-brand-400 border border-brand-500/30'
                  : 'text-cinema-300 hover:text-white border border-transparent'
              )}>
              <Icon className="w-4 h-4" />
              {label}
            </button>
          ))}
        </div>

        {/* Tab Content */}
        <motion.div key={activeTab} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>

          {/* ── Profile ─────────────────── */}
          {activeTab === 'profile' && (
            <div className="card-cinema p-6 space-y-5">
              <div className="flex items-center justify-between">
                <h2 className="font-display font-bold text-white text-lg">Thông tin cá nhân</h2>
                {!isEditing ? (
                  <button onClick={() => setIsEditing(true)}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-brand-400
                      border border-brand-500/30 hover:bg-brand-500/10 text-sm transition-all">
                    <Edit2 className="w-3.5 h-3.5" /> Chỉnh sửa
                  </button>
                ) : (
                  <button onClick={handleSaveProfile} disabled={updateMutation.isPending}
                    className="flex items-center gap-1.5 px-4 py-1.5 rounded-lg bg-brand-500
                      text-white text-sm font-medium hover:bg-brand-600 transition-all disabled:opacity-50">
                    {updateMutation.isPending
                      ? <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      : <Save className="w-3.5 h-3.5" />}
                    {updateMutation.isPending ? 'Đang lưu...' : 'Lưu'}
                  </button>
                )}
              </div>

              <div className="space-y-5">
                {/* Họ và tên */}
                <div>
                  <label className="text-cinema-200 text-sm font-semibold mb-2 block">Họ và tên</label>
                  {isEditing ? (
                    <input value={fullName} onChange={e => setFullName(e.target.value)}
                      className="w-full px-4 py-2.5 rounded-xl bg-cinema-800 border border-white/10
                        text-white placeholder-cinema-500 focus:border-brand-500/50 focus:outline-none transition-all" />
                  ) : (
                    <p className="text-white text-base font-medium flex items-center gap-2.5 py-1">
                      <User className="w-4.5 h-4.5 text-brand-400" /> {user?.fullName}
                    </p>
                  )}
                </div>

                {/* Email */}
                <div>
                  <label className="text-cinema-200 text-sm font-semibold mb-2 block">Email</label>
                  <p className="text-white text-base font-medium flex items-center gap-2.5 py-1">
                    <Mail className="w-4.5 h-4.5 text-brand-400" /> {user?.email}
                  </p>
                </div>

                {/* Số điện thoại */}
                <div>
                  <label className="text-cinema-200 text-sm font-semibold mb-2 block">Số điện thoại</label>
                  {isEditing ? (
                    <input value={phone} onChange={e => setPhone(e.target.value)}
                      placeholder="Nhập số điện thoại"
                      type="tel"
                      className="w-full px-4 py-2.5 rounded-xl bg-cinema-800 border border-white/10
                        text-white placeholder-cinema-500 focus:border-brand-500/50 focus:outline-none transition-all" />
                  ) : (
                    <p className="text-white text-base font-medium flex items-center gap-2.5 py-1">
                      <Phone className="w-4.5 h-4.5 text-brand-400" />
                      {user?.phone ? maskPhone(user.phone) : <span className="text-cinema-400 italic">Chưa cập nhật</span>}
                    </p>
                  )}
                </div>
              </div>

              <div className="pt-4 border-t border-white/5">
                <button onClick={logout}
                  className="flex items-center gap-2 px-4 py-2.5 rounded-xl text-red-400
                    border border-red-500/20 hover:bg-red-500/10 transition-all text-sm">
                  <LogOut className="w-4 h-4" /> Đăng xuất
                </button>
              </div>
            </div>
          )}

          {/* ── Tickets ─────────────────── */}
          {activeTab === 'tickets' && (
            <div className="space-y-3">
              {ticketsLoading ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="skeleton h-24 rounded-2xl" />
                ))
              ) : !ticketsData?.content?.length ? (
                <div className="card-cinema p-10 text-center">
                  <Ticket className="w-12 h-12 text-cinema-600 mx-auto mb-3" />
                  <p className="text-cinema-400">Bạn chưa đặt vé nào</p>
                </div>
              ) : (
                ticketsData.content.map(booking => {
                  const badge = getStatusBadge(booking.status)
                  return (
                    <div key={booking.id} className="card-cinema p-4 flex items-center gap-4">
                      <img src={booking.moviePosterUrl} alt={booking.movieTitle}
                        className="w-14 h-20 object-cover rounded-xl flex-shrink-0" />
                      <div className="flex-1 min-w-0">
                        <p className="font-display font-bold text-white text-sm line-clamp-1">
                          {booking.movieTitle}
                        </p>
                        <p className="text-cinema-400 text-xs mt-1">
                          {formatDateTime(booking.startTime)}
                        </p>
                        <div className="flex items-center gap-2 mt-1.5">
                          <span className={cn('badge text-xs', `badge-${badge.color}`)}>
                            {badge.label}
                          </span>
                          <span className="text-brand-400 text-sm font-semibold">
                            {formatCurrency(booking.totalAmount)}
                          </span>
                        </div>
                      </div>
                    </div>
                  )
                })
              )}
            </div>
          )}

          {/* ── Notifications ───────────── */}
          {activeTab === 'notifications' && (
            <div className="space-y-3">
              {notifLoading ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="skeleton h-16 rounded-2xl" />
                ))
              ) : !notifData?.content?.length ? (
                <div className="card-cinema p-10 text-center">
                  <Bell className="w-12 h-12 text-cinema-600 mx-auto mb-3" />
                  <p className="text-cinema-400">Chưa có thông báo nào</p>
                </div>
              ) : (
                notifData.content.map(n => (
                  <div key={n.id} className={cn(
                    'card-cinema p-4',
                    !n.read && 'border-l-2 border-l-brand-500'
                  )}>
                    <p className="text-white text-sm font-medium">{n.title}</p>
                    <p className="text-cinema-400 text-xs mt-1">{n.message}</p>
                    <p className="text-cinema-500 text-xs mt-1.5">{formatDate(n.createdAt)}</p>
                  </div>
                ))
              )}
            </div>
          )}

          {/* ── Security ────────────────── */}
          {activeTab === 'security' && (
            <SecurityTab user={user} />
          )}

          {/* ── Appearance ────────────────── */}
          {activeTab === 'appearance' && (
            <AppearanceTab />
          )}

        </motion.div>
      </div>
    </div>
  )
}