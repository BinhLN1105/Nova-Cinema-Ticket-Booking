import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Eye, EyeOff, Lock, KeyRound, CheckCircle, Shield, Loader2 } from 'lucide-react'
import { api } from '@/api/client'
import { cn } from '@/utils'
import toast from 'react-hot-toast'

export function SecurityTab({ user }) {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showCurrent, setShowCurrent] = useState(false)
  const [showNew, setShowNew] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)

  const isSocial = user?.authProvider && user.authProvider !== 'LOCAL'

  const changePwMutation = useMutation({
    mutationFn: (data) => api.patch('/users/me/password', data),
    onSuccess: () => {
      toast.success('Đổi mật khẩu thành công!')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    },
  })

  const handleChangePassword = () => {
    if (!currentPassword) return toast.error('Nhập mật khẩu hiện tại')
    if (newPassword.length < 6) return toast.error('Mật khẩu mới tối thiểu 6 ký tự')
    if (newPassword !== confirmPassword) return toast.error('Mật khẩu xác nhận không khớp')
    changePwMutation.mutate({ currentPassword, newPassword })
  }

  const inputCls = "w-full px-4 py-2.5 rounded-xl bg-cinema-800 border border-white/10 text-white placeholder-cinema-500 focus:border-brand-500/50 focus:outline-none transition-all pr-10"

  return (
    <div className="space-y-5">
      {/* Change Password */}
      <div className="card-cinema p-6">
        <div className="flex items-center gap-2.5 mb-5">
          <div className="w-9 h-9 rounded-xl bg-brand-500/15 flex items-center justify-center">
            <KeyRound className="w-4.5 h-4.5 text-brand-400" />
          </div>
          <h2 className="font-display font-bold text-white text-lg">Đổi mật khẩu</h2>
        </div>

        {isSocial ? (
          <div className="p-4 rounded-xl bg-cinema-800/60 border border-white/5 text-center">
            <p className="text-cinema-300 text-sm">
              Tài khoản đăng nhập bằng <span className="text-brand-400 font-semibold capitalize">{user.authProvider.toLowerCase()}</span>,
              không thể đổi mật khẩu.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            <div>
              <label className="text-cinema-200 text-sm font-semibold mb-2 block">Mật khẩu hiện tại</label>
              <div className="relative">
                <input type={showCurrent ? 'text' : 'password'} value={currentPassword}
                  onChange={e => setCurrentPassword(e.target.value)}
                  placeholder="Nhập mật khẩu hiện tại" className={inputCls} />
                <button type="button" onClick={() => setShowCurrent(!showCurrent)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-cinema-400 hover:text-white transition-colors">
                  {showCurrent ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>
            <div>
              <label className="text-cinema-200 text-sm font-semibold mb-2 block">Mật khẩu mới</label>
              <div className="relative">
                <input type={showNew ? 'text' : 'password'} value={newPassword}
                  onChange={e => setNewPassword(e.target.value)}
                  placeholder="Tối thiểu 6 ký tự" className={inputCls} />
                <button type="button" onClick={() => setShowNew(!showNew)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-cinema-400 hover:text-white transition-colors">
                  {showNew ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>
            <div>
              <label className="text-cinema-200 text-sm font-semibold mb-2 block">Xác nhận mật khẩu mới</label>
              <div className="relative">
                <input type={showConfirm ? 'text' : 'password'} value={confirmPassword}
                  onChange={e => setConfirmPassword(e.target.value)}
                  placeholder="Nhập lại mật khẩu mới" className={inputCls} />
                <button type="button" onClick={() => setShowConfirm(!showConfirm)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-cinema-400 hover:text-white transition-colors">
                  {showConfirm ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {confirmPassword && newPassword && (
                <p className={cn("text-xs mt-1.5", newPassword === confirmPassword ? 'text-green-400' : 'text-red-400')}>
                  {newPassword === confirmPassword ? '✓ Mật khẩu khớp' : '✗ Mật khẩu không khớp'}
                </p>
              )}
            </div>
            <button onClick={handleChangePassword} disabled={changePwMutation.isPending}
              className="w-full py-2.5 rounded-xl bg-brand-500 text-white font-semibold text-sm
                hover:bg-brand-600 transition-all disabled:opacity-50 flex items-center justify-center gap-2">
              {changePwMutation.isPending ? (
                <><Loader2 className="w-4 h-4 animate-spin" /> Đang xử lý...</>
              ) : (
                <><Lock className="w-4 h-4" /> Cập nhật mật khẩu</>
              )}
            </button>
          </div>
        )}
      </div>

      {/* Auth Provider */}
      <div className="card-cinema p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-green-500/15 flex items-center justify-center">
              <CheckCircle className="w-4.5 h-4.5 text-green-400" />
            </div>
            <div>
              <p className="text-white font-medium text-sm">Phương thức đăng nhập</p>
              <p className="text-cinema-300 text-xs capitalize mt-0.5">
                {user?.authProvider?.toLowerCase() || 'local'}
              </p>
            </div>
          </div>
          <Shield className="w-5 h-5 text-brand-400" />
        </div>
      </div>
    </div>
  )
}
