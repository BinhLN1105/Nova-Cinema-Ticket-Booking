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
      <div className="card-cinema p-6 border border-brand-500/20 shadow-[0_0_20px_rgba(233,69,96,0.1)]">
        <h2 className="font-display font-bold text-white text-lg mb-4 flex items-center gap-2">
          <Gift className="w-5 h-5 text-brand-400" /> Đổi Thẻ Quà Tặng
        </h2>
        <form onSubmit={handleRedeem} className="flex gap-3">
          <input
            type="text"
            placeholder="Nhập mã thẻ (VD: GC-XXXX-XXXX)"
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            className="flex-1 input bg-cinema-900 border-white/10 uppercase"
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
        <h2 className="font-display font-bold text-white text-lg mb-4">Thẻ đã mua</h2>
        
        {isLoading ? (
          <div className="flex justify-center py-8">
            <Loader2 className="w-8 h-8 text-brand-500 animate-spin" />
          </div>
        ) : !giftCardsData?.content?.length ? (
          <div className="text-center py-8">
            <Gift className="w-12 h-12 text-cinema-600 mx-auto mb-3" />
            <p className="text-cinema-400">Bạn chưa mua thẻ quà tặng nào</p>
          </div>
        ) : (
          <div className="space-y-3">
            {giftCardsData.content.map(card => (
              <motion.div
                key={card.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex items-center justify-between p-4 rounded-xl bg-cinema-800/50 border border-white/5"
              >
                <div>
                  <p className="text-white font-medium flex items-center gap-2">
                    {card.code}
                    <button onClick={() => { navigator.clipboard.writeText(card.code); toast.success('Đã sao chép mã thẻ!') }}
                      className="text-cinema-400 hover:text-brand-400 transition-colors" title="Sao chép mã">
                      <Copy className="w-3.5 h-3.5" />
                    </button>
                    {card.isRedeemed ? (
                      <span className="text-xs bg-green-500/20 text-green-400 px-2 py-0.5 rounded flex items-center gap-1">
                        <Check className="w-3 h-3" /> Đã đổi
                      </span>
                    ) : (
                      <span className="text-xs bg-blue-500/20 text-blue-400 px-2 py-0.5 rounded flex items-center gap-1">
                        <Clock className="w-3 h-3" /> Chưa đổi
                      </span>
                    )}
                  </p>
                  <p className="text-cinema-400 text-sm mt-1">
                    {card.isRedeemed
                      ? `Đã đổi: ${formatDateTime(card.redeemedAt)}`
                      : `Hết hạn: ${formatDateTime(card.expiresAt)}`}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-brand-400 font-bold">{formatCurrency(card.pointValue, '')} CP</p>
                  <p className="text-cinema-500 text-xs mt-0.5">Mệnh giá: {formatCurrency(card.price)}</p>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
