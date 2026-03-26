import { useState, useEffect, useRef } from 'react'
import { motion } from 'framer-motion'
import { useQuery, useMutation } from '@tanstack/react-query'
import { User, Ticket, Bell, Shield, Edit2, Save, Phone, Mail, LogOut, Loader2, Palette, Trophy, Star, CreditCard, Gift } from 'lucide-react'
import { bookingApi, notificationApi } from '@/api/endpoints'
import { api } from '@/api/client'
import { SecurityTab } from '@/components/customer/SecurityTab'
import { AppearanceTab } from '@/components/customer/AppearanceTab'
import { useAuth } from '@/hooks'
import { formatDate, formatDateTime, formatCurrency, getStatusBadge, cn } from '@/utils'
import { useAuthStore } from '@/stores/authStore'
import toast from 'react-hot-toast'
import { TopUpModal } from './TopUpModal'
import { GiftCardTab } from '@/components/customer/GiftCardTab'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { requestFirebaseToken } from '@/utils/firebase'

const TIER_THRESHOLDS = {
  BRONZE: { min: 0, max: 500, next: 'SILVER', label: 'Bạc' },
  SILVER: { min: 500, max: 3000, next: 'GOLD', label: 'Vàng' },
  GOLD: { min: 3000, max: 10000, next: 'DIAMOND', label: 'Kim Cương' },
  DIAMOND: { min: 10000, max: 10000, next: null, label: null }
}

const TABS = [
  { id: 'profile',       label: 'Thông tin',    icon: User },
  { id: 'tickets',       label: 'Vé của tôi',   icon: Ticket },
  { id: 'giftcards',     label: 'Thẻ quà tặng', icon: Gift },
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
  const [isTopUpOpen, setIsTopUpOpen] = useState(false)
  const setUser = useAuthStore(s => s.setUser)
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const hasHandledTopup = useRef(false)
  const [isPushEnabled, setIsPushEnabled] = useState(!!user?.fcmToken)
  const [isTogglingPush, setIsTogglingPush] = useState(false)

  useEffect(() => {
    const topupStatus = searchParams.get('topup')
    if (!topupStatus || hasHandledTopup.current) return

    if (topupStatus === 'success') {
      hasHandledTopup.current = true
      toast.success('Nạp CinePoint thành công!')
      // Xóa query param để không hiện toast liên tục khi reload
      setSearchParams({})
      api.get('/auth/me').then(res => setUser(res))
    } else if (topupStatus === 'failed') {
      hasHandledTopup.current = true
      toast.error('Nạp CinePoint thất bại')
      setSearchParams({})
    }
  }, [searchParams, setSearchParams, setUser])

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

  // Handle Push Toggle
  const handleTogglePush = async () => {
    if (isTogglingPush) return
    setIsTogglingPush(true)
    try {
      if (!isPushEnabled) {
        const token = await requestFirebaseToken()
        if (token) {
          setIsPushEnabled(true)
          setUser({ ...user, fcmToken: token })
          toast.success('Đã bật thông báo push')
        }
      } else {
        await api.patch('/users/me/fcm-token', { fcmToken: null })
        setIsPushEnabled(false)
        setUser({ ...user, fcmToken: null })
        toast.success('Đã tắt thông báo push')
      }
    } catch (err) {
      toast.error('Có lỗi xảy ra khi thay đổi trạng thái thông báo')
    } finally {
      setIsTogglingPush(false)
    }
  }

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
            <h1 className="font-display text-2xl font-bold text-white flex items-center gap-3">
              {user?.fullName}
              <span className={cn(
                "px-2.5 py-1 text-xs font-bold rounded-lg uppercase tracking-wider backdrop-blur-sm border",
                user?.membershipTier === 'DIAMOND' ? "bg-cyan-500/20 text-cyan-400 border-cyan-500/30 shadow-[0_0_10px_rgba(34,211,238,0.3)]" :
                user?.membershipTier === 'GOLD' ? "bg-yellow-500/20 text-yellow-400 border-yellow-500/30" :
                user?.membershipTier === 'SILVER' ? "bg-slate-400/20 text-slate-300 border-slate-400/30" :
                "bg-orange-500/20 text-orange-400 border-orange-500/30"
              )}>
                {user?.membershipTier || 'BRONZE'}
              </span>
            </h1>
            <p className="text-cinema-300 text-sm">{user?.email}</p>
            
            <div className="flex flex-wrap items-center gap-4 mt-3">
              <div className="flex items-center gap-2 bg-brand-500/10 border border-brand-500/20 rounded-xl px-3 py-1.5">
                <Ticket className="w-4 h-4 text-brand-400" />
                <span className="text-white text-sm font-medium">{user?.rewardPoints?.toLocaleString('vi-VN') || 0} CP</span>
                <button 
                  onClick={() => setIsTopUpOpen(true)}
                  className="ml-2 text-xs bg-brand-500 text-white px-2 py-1 rounded-md hover:bg-brand-600 transition-colors font-semibold"
                >
                  Nạp điểm
                </button>
                <button 
                  onClick={() => setActiveTab('giftcards')}
                  className="ml-1 text-xs bg-white/10 text-brand-300 border border-brand-500/30 px-2 py-1 rounded-md hover:bg-white/20 transition-colors font-semibold"
                >
                  Đổi thẻ
                </button>
              </div>
              <div className="flex items-center gap-2 bg-yellow-500/10 border border-yellow-500/20 rounded-xl px-3 py-1.5">
                <Star className="w-4 h-4 text-yellow-400" />
                <span className="text-yellow-400 text-sm font-medium">{user?.availableExp || 0} EXP</span>
              </div>
            </div>

            {/* Rank Progress Bar */}
            <div className="mt-5 max-w-sm">
              {(() => {
                const tier = user?.membershipTier || 'BRONZE'
                const config = TIER_THRESHOLDS[tier]
                const currentExp = user?.availableExp || 0
                
                if (tier === 'DIAMOND') {
                  return (
                    <div className="space-y-1.5">
                      <div className="flex justify-between text-[11px] font-bold tracking-wide">
                        <span className="text-cyan-400 uppercase">Rank Tối Đa</span>
                        <span className="text-cinema-400">{currentExp} EXP</span>
                      </div>
                      <div className="h-2 w-full bg-white/5 rounded-full overflow-hidden border border-white/5">
                        <motion.div 
                          initial={{ width: 0 }}
                          animate={{ width: '100%' }}
                          className="h-full bg-cyan-500 shadow-[0_0_15px_rgba(34,211,238,0.5)]"
                        />
                      </div>
                      <p className="text-[10px] text-cinema-400 italic">Chúc mừng! Bạn đã đạt cấp độ cao nhất.</p>
                    </div>
                  )
                }

                const progress = Math.min(100, Math.max(0, ((currentExp - config.min) / (config.max - config.min)) * 100))
                const remaining = config.max - currentExp

                return (
                  <div className="space-y-1.5">
                    <div className="flex justify-between text-[11px] font-bold tracking-wide uppercase">
                      <span className="text-cinema-300">Tiến trình lên hạng {config.label}</span>
                      <span className="text-white">{currentExp} / {config.max} <span className="text-cinema-400 ml-0.5 whitespace-nowrap">EXP</span></span>
                    </div>
                    <div className="h-2 w-full bg-white/5 rounded-full overflow-hidden border border-white/10 p-[0.5px]">
                      <motion.div 
                        initial={{ width: 0 }}
                        animate={{ width: `${progress}%` }}
                        transition={{ duration: 1, ease: "easeOut" }}
                        className={cn(
                          "h-full rounded-full relative overflow-hidden",
                          tier === 'BRONZE' ? "bg-orange-500" :
                          tier === 'SILVER' ? "bg-slate-400" :
                          "bg-yellow-500 shadow-[0_0_10px_rgba(234,179,8,0.3)]"
                        )}
                      >
                        <motion.div 
                          animate={{ x: ['-100%', '100%'] }}
                          transition={{ repeat: Infinity, duration: 2, ease: "linear" }}
                          className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent"
                        />
                      </motion.div>
                    </div>
                    <div className="flex items-center gap-1.5 text-[10px] text-cinema-400">
                      <span className="flex-shrink-0 font-medium">Còn {remaining} EXP nữa</span>
                      <div className="h-px flex-grow bg-white/5" />
                    </div>
                  </div>
                )
              })()}
            </div>
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

          {/* ── Gift Cards ─────────────────── */}
          {activeTab === 'giftcards' && (
            <div className="space-y-4">
              <div className="flex justify-between items-center mb-2">
                <p className="text-cinema-200 text-sm">Quản lý thẻ quà tặng và điểm tích lũy của bạn</p>
                <button 
                  onClick={() => navigate('/gift-cards')}
                  className="btn-ghost py-1.5 px-3 text-sm text-brand-400"
                >
                  Mua thẻ mới
                </button>
              </div>
              <GiftCardTab />
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
              ) : (
                <div className="space-y-4">
                  {/* Push Permission Toggle - Moved outside condition so it's always visible */}
                  <div className="card-cinema p-4 flex items-center justify-between border-brand-500/20 bg-brand-500/5">
                    <div>
                      <p className="text-sm font-bold text-white">Tính năng thông báo</p>
                      <p className="text-xs text-cinema-400">Nhận nhắc nhở lịch chiếu và ưu đãi ngay trên trình duyệt</p>
                    </div>
                    <button type="button" 
                      onClick={handleTogglePush}
                      disabled={isTogglingPush}
                      className={cn(
                        "relative inline-flex h-6 w-11 items-center rounded-full transition-colors duration-200 focus:outline-none",
                        isPushEnabled ? 'bg-brand-500' : 'bg-cinema-700',
                        isTogglingPush && 'opacity-50 cursor-not-allowed'
                      )}>
                      {isTogglingPush ? (
                        <span className="flex items-center justify-center w-full">
                          <Loader2 className="w-3 h-3 text-white animate-spin" />
                        </span>
                      ) : (
                        <span className={cn(
                          "inline-block h-4 w-4 rounded-full bg-white shadow-md transition-transform duration-200",
                          isPushEnabled ? 'translate-x-6' : 'translate-x-1'
                        )} />
                      )}
                    </button>
                  </div>

                  {isPushEnabled && (
                    <div className="flex justify-end">
                      <button 
                        onClick={async () => {
                          try {
                            await api.post('/notifications/test')
                            toast.success('Yêu cầu gửi thông báo test thành công!')
                          } catch (err) {
                            toast.error('Không thể gửi thông báo test')
                          }
                        }}
                        className="text-xs bg-white/5 border border-brand-500/30 text-brand-400 px-3 py-1.5 rounded-lg hover:bg-brand-500/10 transition-all font-medium"
                      >
                        Gửi thử thông báo
                      </button>
                    </div>
                  )}

                  {!notifData?.content?.length ? (
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

      <TopUpModal 
        isOpen={isTopUpOpen} 
        onClose={() => setIsTopUpOpen(false)} 
      />
    </div>
  )
}