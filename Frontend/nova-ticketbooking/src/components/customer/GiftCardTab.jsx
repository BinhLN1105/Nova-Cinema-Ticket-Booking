import { useState } from 'react'
import { motion } from 'framer-motion'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Gift, Loader2, Check, Copy, Clock } from 'lucide-react'
import { giftCardApi } from '@/api/endpoints'
import toast from 'react-hot-toast'
import { formatDateTime, formatCurrency } from '@/utils'
import { useAuthStore } from '@/stores/authStore'
import { api } from '@/api/client'

export function GiftCardTab() {
  const [code, setCode] = useState('')
  const queryClient = useQueryClient()
  const setUser = useAuthStore(s => s.setUser)

  const { data: giftCardsData, isLoading } = useQuery({
    queryKey: ['my-gift-cards'],
    queryFn: () => giftCardApi.getMyAll(0, 50),
  })

  const redeemMutation = useMutation({
    mutationFn: (cardCode) => giftCardApi.redeem(cardCode),
    onSuccess: (res) => {
      toast.success('Đổi thẻ thành công! CinePoint đã được cộng vào tài khoản.')
      setCode('')
      queryClient.invalidateQueries(['my-gift-cards'])
      // Reload user to update points
      api.get('/auth/me').then(userRes => setUser(userRes))
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Đổi thẻ thất bại')
    }
  })

  const handleRedeem = (e) => {
    e.preventDefault()
    if (!code.trim()) {
      toast.error('Vui lòng nhập mã thẻ')
      return
    }
    redeemMutation.mutate(code.trim())
  }

  return (
    <div className="space-y-6">
      {/* Redeem Form */}
      <div className="card-cinema p-6 border border-brand-500/20 shadow-sm">
        <h2 className="font-display font-bold text-slate-900 dark:text-white text-lg mb-4 flex items-center gap-2">
          <Gift className="w-5 h-5 text-brand-500 dark:text-brand-400" /> Đổi Thẻ Quà Tặng
        </h2>
        <form onSubmit={handleRedeem} className="flex gap-3">
          <input
            type="text"
            placeholder="Nhập mã thẻ (VD: GC-XXXX-XXXX)"
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            className="flex-1 input bg-slate-50 dark:bg-cinema-900 border-slate-200 dark:border-white/10 text-slate-900 dark:text-white uppercase"
          />
          <button
            type="submit"
            disabled={redeemMutation.isPending}
            className="btn-primary py-2 px-6 flex-shrink-0"
          >
            {redeemMutation.isPending ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              'Đổi điểm'
            )}
          </button>
        </form>
      </div>

      {/* My Purchased Cards */}
      <div className="card-cinema p-6">
        <h2 className="font-display font-bold text-slate-900 dark:text-white text-lg mb-4">Thẻ đã mua</h2>
        
        {isLoading ? (
          <div className="flex justify-center py-8">
            <Loader2 className="w-8 h-8 text-brand-500 animate-spin" />
          </div>
        ) : !giftCardsData?.content?.length ? (
          <div className="text-center py-8">
            <Gift className="w-12 h-12 text-slate-400 dark:text-cinema-600 mx-auto mb-3" />
            <p className="text-slate-500 dark:text-cinema-400">Bạn chưa mua thẻ quà tặng nào</p>
          </div>
        ) : (
          <div className="space-y-3">
            {giftCardsData.content.map(card => (
              <motion.div
                key={card.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex items-center justify-between p-4 rounded-xl bg-slate-50 dark:bg-cinema-800/50 border border-slate-200 dark:border-white/5 shadow-sm"
              >
                <div>
                  <p className="text-slate-900 dark:text-white font-medium flex items-center gap-2">
                    {card.code}
                    <button onClick={() => { navigator.clipboard.writeText(card.code); toast.success('Đã sao chép mã thẻ!') }}
                      className="text-slate-400 dark:text-cinema-400 hover:text-brand-500 dark:hover:text-brand-400 transition-colors" title="Sao chép mã">
                      <Copy className="w-3.5 h-3.5" />
                    </button>
                  </p>
                  <p className="text-slate-500 dark:text-cinema-400 text-xs mt-1">
                    Ngày mua: {formatDateTime(card.createdAt)}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-slate-900 dark:text-white font-display font-bold">
                    {formatCurrency(card.initialAmount)}
                  </p>
                  <span className={`inline-block text-[10px] px-2 py-0.5 rounded-full font-bold uppercase mt-1 ${
                    card.status === 'ACTIVE' 
                      ? 'bg-green-500/10 text-green-600 dark:text-green-400 border border-green-500/20' 
                      : 'bg-slate-200 dark:bg-cinema-700 text-slate-500 dark:text-cinema-400'
                  }`}>
                    {card.status === 'ACTIVE' ? 'Chưa nạp' : 'Đã nạp'}
                  </span>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
