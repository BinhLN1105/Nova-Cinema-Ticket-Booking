import { useParams, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useQuery } from '@tanstack/react-query'
import { Star, Clock, Play, Ticket, Calendar, Globe, Users, ChevronLeft, MessageSquare } from 'lucide-react'
import { movieApi, reviewApi, bookingApi } from '@/api/endpoints'
import { formatDate, getRatedColor, getImageUrl, cn } from '@/utils'
import { useAuthStore } from '@/stores/authStore'
import { toast } from 'react-hot-toast'
import { useState } from 'react'
import { Modal } from '@/components/common/ui/Modal'

export default function MovieDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const { data: movie, isLoading } = useQuery({
    queryKey: ['movies', 'detail', id],
    queryFn: () => movieApi.getById(id),
    enabled: !!id,
  })

  const [ratingFilter, setRatingFilter] = useState(null)
  const [sort, setSort] = useState('highest')
  const [reviewSize, setReviewSize] = useState(5)

  const { data: reviews, refetch: refetchReviews } = useQuery({
    queryKey: ['reviews', id, ratingFilter, sort, reviewSize],
    queryFn: () => reviewApi.getByMovie(id, { rating: ratingFilter, sort, size: reviewSize }).then(res => res.data || res),
    enabled: !!id,
  })

  const { data: reviewStats } = useQuery({
    queryKey: ['reviewStats', id],
    queryFn: () => reviewApi.getStats(id).then(res => res.data || res),
    enabled: !!id,
  })

  const { user } = useAuthStore()

  const { data: canReviewObj } = useQuery({
    queryKey: ['canReview', id],
    queryFn: () => movieApi.canReview(id).then(res => res.data),
    enabled: !!user && !!id,
  })
  const canReviewBookingId = canReviewObj?.data || null
  const canReview = !!canReviewBookingId

  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false)
  const [rating, setRating] = useState(5)
  const [comment, setComment] = useState('')
  const [isSubmittingReview, setIsSubmittingReview] = useState(false)

  const handleSubmitReview = async (e) => {
    e.preventDefault()
    if (!comment.trim()) {
      return toast.error('Vui lòng nhập nhận xét')
    }

    if (!canReviewBookingId) {
      return toast.error('Không tìm thấy giao dịch hợp lệ để đánh giá.')
    }

    setIsSubmittingReview(true)
    try {
      await reviewApi.create({
        movieId: id,
        bookingId: canReviewBookingId,
        rating,
        comment
      })
      toast.success('Gửi đánh giá thành công')
      refetchReviews()
      setIsReviewModalOpen(false)
      setComment('')
      setRating(5)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Có lỗi xảy ra')
    } finally {
      setIsSubmittingReview(false)
    }
  }

  if (isLoading) return (
    <div className="min-h-screen bg-cinema-900 pt-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8">
        <div className="flex flex-col md:flex-row gap-8">
          <div className="skeleton rounded-2xl w-full md:w-72 h-[420px] flex-shrink-0" />
          <div className="flex-1 space-y-4">
            <div className="skeleton h-10 rounded w-2/3" />
            <div className="skeleton h-4 rounded w-1/3" />
            <div className="skeleton h-24 rounded w-full" />
          </div>
        </div>
      </div>
    </div>
  )

  if (!movie) return null

  return (
    <div className="min-h-screen bg-cinema-900">
      {/* Backdrop Hero */}
      <div className="relative h-[65vh] overflow-hidden">
        <div className="absolute inset-0">
          <img
            src={movie.backdropUrl || movie.posterUrl}
            alt=""
            className="w-full h-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-cinema-900 via-cinema-900/70 to-transparent" />
          <div className="absolute inset-0 bg-gradient-to-t from-cinema-900 via-transparent to-cinema-900/40" />
        </div>

        {/* Back button */}
        <button onClick={() => navigate(-1)}
          className="absolute top-24 left-6 flex items-center gap-2 glass px-4 py-2
            rounded-xl text-sm text-cinema-200 hover:text-white transition-all">
          <ChevronLeft className="w-4 h-4" /> Quay lại
        </button>

        {/* Hero content */}
        <div className="absolute bottom-0 left-0 right-0">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 pb-10 pt-20">
            <motion.div initial={{ opacity: 0, y: 24 }} animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6 }}>
              {/* Genres */}
              <div className="flex flex-wrap gap-2 mb-4">
                {movie.genres?.map(g => (
                  <span key={g.id} className="badge badge-gray text-xs">{g.name}</span>
                ))}
                <span className={cn('badge text-xs', getRatedColor(movie.rated))}>
                  {movie.rated}
                </span>
              </div>

              <h1 className="font-display text-5xl font-bold text-white mb-4 leading-tight max-w-2xl">
                {movie.title}
              </h1>

              <div className="flex flex-wrap items-center gap-5 text-sm text-cinema-200">
                <span className="flex items-center gap-1.5">
                  <Star className="w-4 h-4 text-gold-400 fill-current" />
                  <span className="text-white font-semibold">{movie.avgRating.toFixed(1)}</span>
                  /10
                </span>
                <span className="flex items-center gap-1.5">
                  <Clock className="w-4 h-4" /> {movie.duration} phút
                </span>
                <span className="flex items-center gap-1.5">
                  <Calendar className="w-4 h-4" /> {formatDate(movie.releaseDate)}
                </span>
                <span className="flex items-center gap-1.5">
                  <Globe className="w-4 h-4" /> {movie.language}
                </span>
              </div>
            </motion.div>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-10">
        <div className="flex flex-col lg:flex-row gap-10">

          {/* Left */}
          <div className="flex-1 space-y-8">
            {/* Description */}
            <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}>
              <h2 className="font-display text-xl font-bold text-white mb-3">Nội dung phim</h2>
              <p className="text-cinema-200 leading-relaxed">{movie.description}</p>
            </motion.div>

            {/* Cast & Crew */}
            <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}>
              <h2 className="font-display text-xl font-bold text-white mb-4">Đội ngũ sản xuất</h2>
              <div className="grid grid-cols-2 gap-4">
                <div className="p-4 rounded-xl bg-cinema-800/60 border border-white/5">
                  <p className="text-cinema-400 text-xs mb-1">Đạo diễn</p>
                  <p className="text-white font-medium text-sm">{movie.director}</p>
                </div>
                <div className="p-4 rounded-xl bg-cinema-800/60 border border-white/5">
                  <p className="text-cinema-400 text-xs mb-1">Diễn viên</p>
                  <p className="text-white font-medium text-sm line-clamp-2">{movie.cast}</p>
                </div>
              </div>
            </motion.div>

            {/* Reviews */}
            {reviews?.content && (
              <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 }}>
                <div className="flex items-center justify-between mb-4">
                  <h2 className="font-display text-xl font-bold text-white">
                    Đánh giá ({reviewStats ? Object.values(reviewStats).reduce((a, b) => a + b, 0) : reviews.totalElements})
                  </h2>
                  {user && canReview && (
                    <button onClick={() => setIsReviewModalOpen(true)}
                      className="btn-ghost py-1.5 px-3 text-sm text-brand-400 border-brand-500/30">
                      <MessageSquare className="w-4 h-4" /> Viết đánh giá
                    </button>
                  )}
                  {user && !canReview && (
                    <div className="text-sm text-cinema-400 italic">
                      Đã mua vé & xem phim để đánh giá
                    </div>
                  )}
                </div>

                {/* Shopee-style review stats & filters */}
                <div className="bg-cinema-800/40 border border-white/5 p-5 rounded-2xl mb-6">
                  <div className="flex flex-col md:flex-row gap-6 items-center">
                    {/* Overall Rating */}
                    <div className="text-center md:border-r border-white/10 md:pr-6 md:w-32">
                      <div className="text-4xl font-bold text-gold-400">{movie.avgRating?.toFixed(1) || '0.0'}</div>
                      <div className="flex justify-center my-1">
                        {Array.from({ length: 5 }).map((_, i) => (
                          <Star key={i} className={cn('w-3.5 h-3.5', i < Math.round(movie.avgRating || 0) ? 'text-gold-400 fill-current' : 'text-cinema-600')} />
                        ))}
                      </div>
                      <div className="text-xs text-cinema-400">{reviewStats ? Object.values(reviewStats).reduce((a, b) => a + b, 0) : reviews.totalElements} đánh giá</div>
                    </div>
                    {/* Progress bars */}
                    <div className="flex-1 space-y-1.5 w-full">
                      {[5, 4, 3, 2, 1].map(star => {
                        const total = reviewStats ? Object.values(reviewStats).reduce((a, b) => a + b, 0) : 1;
                        const count = reviewStats ? reviewStats[star] || 0 : 0;
                        const percent = total > 0 ? (count / total) * 100 : 0;
                        return (
                          <div key={star} className="flex items-center gap-2 text-sm cursor-pointer" onClick={() => { setRatingFilter(star); setReviewSize(5); }}>
                            <div className="flex items-center gap-1 w-8 text-cinema-200">
                              {star}<Star className="w-3 h-3 text-gold-400 fill-current" />
                            </div>
                            <div className="flex-1 h-2 bg-cinema-700/50 rounded-full overflow-hidden">
                              <div className="h-full bg-gold-400 rounded-full" style={{ width: `${percent}%` }}></div>
                            </div>
                            <div className="w-8 text-right text-cinema-400 text-xs">{count}</div>
                          </div>
                        )
                      })}
                    </div>
                  </div>
                  
                  {/* Filters */}
                  <div className="mt-5 pt-5 border-t border-white/10 flex flex-col sm:flex-row gap-3 sm:items-center justify-between">
                    <div className="flex flex-wrap gap-2">
                       <button onClick={() => { setRatingFilter(null); setReviewSize(5); }} 
                         className={cn("px-4 py-1.5 rounded-full text-sm font-medium transition-colors border", 
                           ratingFilter === null ? "bg-brand-500 text-white border-brand-500" : "bg-transparent text-cinema-200 border-white/20 hover:border-white/40")}>
                         Tất cả
                       </button>
                       {[5, 4, 3, 2, 1].map(star => (
                         <button key={star} onClick={() => { setRatingFilter(star); setReviewSize(5); }} 
                           className={cn("px-4 py-1.5 rounded-full text-sm font-medium transition-colors border flex items-center justify-center gap-1", 
                             ratingFilter === star ? "bg-brand-500 text-white border-brand-500" : "bg-transparent text-cinema-200 border-white/20 hover:border-white/40")}>
                           {star} <Star className="w-3.5 h-3.5" />
                         </button>
                       ))}
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm text-cinema-300">Sắp xếp:</span>
                      <select className="bg-cinema-800 border border-white/10 text-white text-sm rounded-lg px-2 py-1.5 focus:outline-none"
                        value={sort} onChange={e => { setSort(e.target.value); setReviewSize(5); }}>
                         <option value="highest">Cao nhất</option>
                         <option value="lowest">Thấp nhất</option>
                         <option value="newest">Mới nhất</option>
                      </select>
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
                  {reviews.content.length === 0 ? (
                    <div className="text-center py-8 text-cinema-400">Không có đánh giá nào phù hợp.</div>
                  ) : (
                    reviews.content.map(review => (
                      <div key={review.id} className="p-4 rounded-xl bg-cinema-800/60 border border-white/5">
                        <div className="flex items-center gap-3 mb-2">
                          <div className="w-10 h-10 rounded-full bg-brand-500/20 flex items-center
                            justify-center text-brand-400 font-bold overflow-hidden border border-brand-500/10">
                            {review.userAvatarUrl ? (
                              <img
                                src={review.userAvatarUrl}
                                alt={review.userFullName}
                                className="w-full h-full object-cover"
                              />
                            ) : (
                              (review.userFullName || review.userName || 'U')[0]
                            )}
                          </div>
                          <div className="flex-1">
                            <p className="text-white text-sm font-medium">{review.userFullName || review.userName || 'Người dùng'}</p>
                            <div className="flex items-center gap-2">
                              <div className="flex gap-0.5 mt-0.5">
                                {Array.from({ length: 5 }).map((_, i) => (
                                  <Star key={i} className={cn('w-3.5 h-3.5',
                                    i < review.rating ? 'text-gold-400 fill-current' : 'text-cinema-600')} />
                                ))}
                              </div>
                              <span className="text-xs text-cinema-400 mt-0.5 whitespace-nowrap">
                                {review.createdAt ? new Date(review.createdAt).toLocaleDateString('vi-VN') : ''}
                              </span>
                            </div>
                          </div>
                        </div>
                        <p className="text-cinema-200 text-sm mt-3 ml-13 pl-1 leading-relaxed">{review.comment}</p>
                      </div>
                    ))
                  )}
                  
                  {/* Load more button */}
                  {!reviews.last && (
                    <div className="pt-4 text-center">
                      <button onClick={() => setReviewSize(s => s + 5)}
                        className="px-6 py-2 rounded-full border border-white/10 text-cinema-200 text-sm hover:bg-white/5 transition-colors">
                        Xem thêm đánh giá
                      </button>
                    </div>
                  )}
                </div>
              </motion.div>
            )}
          </div>

          {/* Right — Booking panel */}
          <motion.div initial={{ opacity: 0, x: 24 }} animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
            className="lg:w-80 flex-shrink-0">
            <div className="sticky top-24 space-y-4">
              {/* Poster */}
              <div className="rounded-2xl overflow-hidden shadow-card-float">
                <img src={movie.posterUrl} alt={movie.title} className="w-full object-cover" />
              </div>

              {/* Actions */}
              <div className="space-y-3">
                {movie.status === 'NOW_SHOWING' && (
                  <button onClick={() => navigate(`/booking/showtime/${movie.id}`)}
                    className="btn-primary w-full py-3.5 text-base">
                    <Ticket className="w-5 h-5" /> Đặt vé ngay
                  </button>
                )}
                {movie.trailerUrl && (
                  <a href={movie.trailerUrl} target="_blank" rel="noopener noreferrer"
                    className="btn-ghost w-full py-3.5 text-base justify-center flex items-center gap-2">
                    <Play className="w-5 h-5 fill-current" /> Xem trailer
                  </a>
                )}
              </div>

              {/* Stats */}
              <div className="p-4 rounded-xl bg-cinema-800/60 border border-white/5 space-y-3">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-cinema-400 flex items-center gap-2">
                    <Users className="w-4 h-4" /> Lượt đặt vé
                  </span>
                  <span className="text-white font-semibold">
                    {movie.totalBookings?.toLocaleString()}
                  </span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-cinema-400">Phân loại</span>
                  <span className={cn('badge text-xs', getRatedColor(movie.rated))}>
                    {movie.rated}
                  </span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-cinema-400">Ngôn ngữ</span>
                  <span className="text-white">{movie.language}</span>
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </div>

      {/* Review Modal */}
      <Modal open={isReviewModalOpen} onClose={() => setIsReviewModalOpen(false)} title="Đánh giá phim" theme="dark">
        <form onSubmit={handleSubmitReview} className="space-y-4 pt-2">
          <p className="text-sm font-medium text-cinema-200">Chất lượng phim (Điểm: {rating}/5)</p>
          <div className="flex items-center gap-1 justify-center py-2">
            {[1, 2, 3, 4, 5].map((starValue) => (
              <button
                key={starValue} type="button"
                onClick={() => setRating(starValue)}
                className="p-1 transition-transform hover:scale-110 focus:outline-none"
              >
                <Star className={cn('w-8 h-8 transition-colors', 
                  rating >= starValue ? 'text-gold-400 fill-current' : 'text-cinema-600'
                )} />
              </button>
            ))}
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-cinema-200">Bình luận (tùy chọn)</label>
            <textarea
              className="w-full px-4 py-3 bg-cinema-800/80 border border-white/10 rounded-xl outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all resize-none h-24 text-white"
              placeholder="Chia sẻ cảm nhận của bạn về bộ phim này..."
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              maxLength={1000}
            />
            <div className="text-right text-xs text-cinema-400 mt-1">
              {comment.length}/1000
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-white/10">
            <button type="button" onClick={() => setIsReviewModalOpen(false)} className="px-5 py-2.5 rounded-xl font-medium text-white hover:bg-white/5 transition-colors">
              Hủy
            </button>
            <button type="submit" disabled={isSubmittingReview} className="px-5 py-2.5 rounded-xl font-medium bg-brand-500 text-white hover:bg-brand-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
              {isSubmittingReview ? 'Đang gửi...' : 'Gửi đánh giá'}
            </button>
          </div>
        </form>
      </Modal>

    </div>
  )
}
