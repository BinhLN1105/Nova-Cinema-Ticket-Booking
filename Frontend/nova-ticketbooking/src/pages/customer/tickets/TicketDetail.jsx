import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useQuery, useMutation } from '@tanstack/react-query'
import { QRCodeSVG } from 'qrcode.react'
import { ArrowLeft, MapPin, Clock, Monitor, Ticket, X, Share2, Copy, CheckCheck, Star } from 'lucide-react'
import { bookingApi, reviewApi } from '@/api/endpoints'
import { formatDateTime, formatCurrency, getStatusBadge, cn } from '@/utils'
import { Modal } from '@/components/common/ui/Modal'
import { Button } from '@/components/common/ui/FormElements'
import toast from 'react-hot-toast'

export default function TicketDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [copied, setCopied] = useState(false)

  const { data: booking, refetch, isLoading } = useQuery({
    queryKey: ['booking', id],
    queryFn: () => bookingApi.getById(id),
    enabled: !!id,
  })

  const cancelMutation = useMutation({
    mutationFn: () => bookingApi.cancelRequest(id),
    onSuccess: () => { toast.success('Đã gửi yêu cầu huỷ vé. Vui lòng kiểm tra email của bạn để xác nhận.'); refetch() },
    onError: (err) => { toast.error(err.response?.data?.message || 'Có lỗi xảy ra khi huỷ vé'); }
  })

  // Review State
  const [isReviewOpen, setIsReviewOpen] = useState(false)
  const [rating, setRating] = useState(10)
  const [comment, setComment] = useState('')

  const reviewMutation = useMutation({
    mutationFn: () => reviewApi.create(booking.movieId, { rating, comment }),
    onSuccess: () => {
      toast.success('Cảm ơn bạn đã đánh giá phim!')
      setIsReviewOpen(false)
      // Optional: refetch to maybe hide the button if we know they reviewed, 
      // but API doesn't return that directly in booking. Just keeping simple.
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Có lỗi khi gửi đánh giá')
    }
  })

  const ticketUrl = `${window.location.origin}/tickets/${id}`

  const copyLink = async () => {
    await navigator.clipboard.writeText(ticketUrl)
    setCopied(true)
    toast.success('Đã sao chép link vé!')
    setTimeout(() => setCopied(false), 2000)
  }

  const shareNative = async () => {
    if (navigator.share) {
      await navigator.share({
        title: `Vé xem phim: ${booking?.movieTitle}`,
        text: `Mình vừa đặt vé xem ${booking?.movieTitle} tại ${booking?.cinemaName}. Xem vé tại link:`,
        url: ticketUrl,
      })
    } else {
      copyLink()
    }
  }

  const shareZalo = () => {
    window.open(`https://zalo.me/share/oa?url=${encodeURIComponent(ticketUrl)}`, '_blank')
  }

  const shareFacebook = () => {
    window.open(`https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(ticketUrl)}`, '_blank', 'width=600,height=400')
  }

  if (isLoading) return (
    <div className="min-h-screen bg-cinema-900 pt-24 flex items-center justify-center">
      <div className="w-8 h-8 border-2 border-brand-500 border-t-transparent rounded-full animate-spin" />
    </div>
  )

  if (!booking) return null
  const badge = getStatusBadge(booking.status)

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-16">
      <div className="max-w-md mx-auto px-4 sm:px-6">
        <div className="flex items-center gap-4 mb-8">
          <button onClick={() => navigate(-1)}
            className="p-2.5 rounded-xl glass border border-white/8 text-cinema-200 hover:text-white transition-all">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <h1 className="font-display text-2xl font-bold text-white">Chi tiết vé</h1>
        </div>

        {/* Ticket card */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
          className="relative">
          {/* Top part */}
          <div className="card-cinema p-5 rounded-b-none border-b-0">
            <div className="flex gap-4 mb-5">
              <img src={booking.moviePosterUrl} alt={booking.movieTitle}
                className="w-20 h-28 object-cover rounded-xl flex-shrink-0" />
              <div className="flex-1">
                <div className="flex items-start justify-between gap-2">
                  <h2 className="font-display font-bold text-white text-lg leading-tight">
                    {booking.movieTitle}
                  </h2>
                  <span className={cn('badge text-xs flex-shrink-0', `badge-${badge.color}`)}>
                    {badge.label}
                  </span>
                </div>
                <div className="mt-3 space-y-1.5 text-sm text-cinema-300">
                  <p className="flex items-center gap-1.5">
                    <MapPin className="w-3.5 h-3.5" /> {booking.cinemaName}
                  </p>
                  <p className="flex items-center gap-1.5">
                    <Monitor className="w-3.5 h-3.5" /> {booking.screenName}
                  </p>
                  <p className="flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5" /> {formatDateTime(booking.startTime)}
                  </p>
                </div>
              </div>
            </div>

            {/* Seats */}
            <div className="p-3 rounded-xl bg-cinema-800/60">
              <p className="text-cinema-400 text-xs mb-2 uppercase tracking-wider">Ghế</p>
              <div className="flex flex-wrap gap-2">
                {booking.seats.map((s, i) => (
                  <span key={i} className="px-3 py-1.5 rounded-lg bg-brand-500/15
                    border border-brand-500/30 text-brand-400 text-sm font-mono font-bold">
                    {s.rowLabel}{s.colNumber}
                  </span>
                ))}
              </div>
            </div>
          </div>

          {/* Ticket tear line */}
          <div className="relative h-5 overflow-hidden">
            <div className="absolute inset-0 bg-cinema-800 border-x border-white/6" />
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t-2 border-dashed border-cinema-600" />
            </div>
            <div className="absolute -left-3 top-1/2 -translate-y-1/2 w-5 h-5 rounded-full bg-cinema-900 border border-white/6" />
            <div className="absolute -right-3 top-1/2 -translate-y-1/2 w-5 h-5 rounded-full bg-cinema-900 border border-white/6" />
          </div>

          {/* Bottom part — QR */}
          <div className="card-cinema p-5 rounded-t-none border-t-0 flex flex-col items-center">
            {booking.status === 'PAID' && booking.qrCode ? (
              <>
                <div className="p-4 rounded-2xl bg-white mb-3">
                  <QRCodeSVG value={booking.qrCode} size={160} />
                </div>
                <p className="text-cinema-400 text-xs text-center">
                  Xuất trình mã QR này tại quầy để check-in
                </p>
                <p className="text-cinema-600 font-mono text-xs mt-1">{booking.bookingCode}</p>

                {/* Share buttons */}
                <div className="w-full mt-5 pt-4 border-t border-cinema-700/50">
                  <p className="text-cinema-400 text-xs text-center mb-3 uppercase tracking-wider flex items-center justify-center gap-1.5">
                    <Share2 className="w-3.5 h-3.5" /> Chia sẻ vé
                  </p>
                  <div className="grid grid-cols-4 gap-2">
                    {/* Copy Link */}
                    <button onClick={copyLink}
                      className="flex flex-col items-center gap-1.5 p-2.5 rounded-xl bg-cinema-800/80 hover:bg-cinema-700 transition-all">
                      {copied
                        ? <CheckCheck className="w-5 h-5 text-green-400" />
                        : <Copy className="w-5 h-5 text-cinema-300" />}
                      <span className="text-[10px] text-cinema-400">{copied ? 'Đã chép' : 'Sao chép'}</span>
                    </button>
                    {/* Zalo */}
                    <button onClick={shareZalo}
                      className="flex flex-col items-center gap-1.5 p-2.5 rounded-xl bg-cinema-800/80 hover:bg-cinema-700 transition-all">
                      <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none">
                        <rect width="24" height="24" rx="6" fill="#0068FF"/>
                        <text x="3" y="17" fontSize="11" fontWeight="bold" fill="white">Za</text>
                      </svg>
                      <span className="text-[10px] text-cinema-400">Zalo</span>
                    </button>
                    {/* Facebook */}
                    <button onClick={shareFacebook}
                      className="flex flex-col items-center gap-1.5 p-2.5 rounded-xl bg-cinema-800/80 hover:bg-cinema-700 transition-all">
                      <svg className="w-5 h-5" viewBox="0 0 24 24" fill="#1877F2">
                        <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
                      </svg>
                      <span className="text-[10px] text-cinema-400">Facebook</span>
                    </button>
                    {/* Native Share / More */}
                    <button onClick={shareNative}
                      className="flex flex-col items-center gap-1.5 p-2.5 rounded-xl bg-cinema-800/80 hover:bg-cinema-700 transition-all">
                      <Share2 className="w-5 h-5 text-cinema-300" />
                      <span className="text-[10px] text-cinema-400">Khác</span>
                    </button>
                  </div>
                </div>
              </>
            ) : (
              <div className="text-center py-4">
                <Ticket className="w-12 h-12 text-cinema-600 mx-auto mb-2" />
                <p className="text-cinema-400 text-sm">QR code chưa sẵn sàng</p>
              </div>
            )}
          </div>
        </motion.div>

        {/* Price */}
        <div className="card-cinema p-4 mt-4 space-y-2">
          {booking.discount > 0 && (
            <div className="flex justify-between text-sm">
              <span className="text-cinema-400">Giảm giá</span>
              <span className="text-green-400">- {formatCurrency(booking.discount)}</span>
            </div>
          )}
          <div className="flex justify-between font-bold">
            <span className="text-white">Tổng cộng</span>
            <span className="text-brand-400 text-lg">{formatCurrency(booking.totalAmount)}</span>
          </div>
        </div>

        {/* Review Button */}
        {booking.status === 'CHECKED_IN' && (
          <button onClick={() => setIsReviewOpen(true)}
            className="w-full mt-4 flex items-center justify-center gap-2 py-3
              rounded-xl bg-gradient-to-r from-brand-600 to-brand-500 text-white
              hover:from-brand-500 hover:to-brand-400 font-medium transition-all shadow-lg shadow-brand-500/25">
             <Star className="w-5 h-5 fill-white" /> Đánh giá phim
          </button>
        )}

        {/* Cancel */}
        {booking.status === 'PAID' && (
          <button onClick={() => cancelMutation.mutate()}
            disabled={cancelMutation.isPending}
            className="w-full mt-4 flex items-center justify-center gap-2 py-3
              rounded-xl border border-brand-500/30 text-brand-400
              hover:bg-brand-500/10 transition-all text-sm disabled:opacity-50">
            <X className="w-4 h-4" /> Hủy vé
          </button>
        )}
      </div>

      <Modal open={isReviewOpen} onClose={() => setIsReviewOpen(false)} title="Đánh giá phim">
        <div className="space-y-4 pt-2">
          <p className="text-sm font-medium text-gray-700">Chất lượng phim (Điểm: {rating}/10)</p>
          <div className="flex items-center gap-1 justify-center py-2">
            {[2, 4, 6, 8, 10].map((starValue) => (
              <button key={starValue} type="button" onClick={() => setRating(starValue)}
                className="p-1 transition-transform hover:scale-110">
                <Star className={cn("w-8 h-8", rating >= starValue ? "fill-yellow-400 text-yellow-400" : "text-gray-300")} />
              </button>
            ))}
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-700">Bình luận (tùy chọn)</label>
            <textarea
              className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all resize-none h-24 text-gray-800"
              placeholder="Chia sẻ cảm nhận của bạn về bộ phim này..."
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
          </div>

          <div className="pt-4 flex justify-end gap-3">
            <Button variant="ghost" onClick={() => setIsReviewOpen(false)}>Hủy</Button>
            <Button onClick={() => reviewMutation.mutate()} disabled={reviewMutation.isPending}>
              {reviewMutation.isPending ? 'Đang gửi...' : 'Gửi đánh giá'}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
