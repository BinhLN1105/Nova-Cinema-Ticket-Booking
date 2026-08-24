import React from 'react';
import { motion } from 'framer-motion';
import { Hourglass, RefreshCw } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FeedbackStateContainer } from '../FeedbackStateContainer';

export function SeatHoldExpiredState({
  showtimeId,
  movieId,
  onRetry,
  onReselectSeats,
  className,
  size = 'default'
}) {
  const navigate = useNavigate();
  const { t } = useTranslation();

  const handleReselect = () => {
    if (typeof onRetry === 'function') {
      onRetry();
    } else if (typeof onReselectSeats === 'function') {
      onReselectSeats();
    } else if (showtimeId) {
      navigate(`/booking/seats/${showtimeId}`);
    } else if (movieId) {
      navigate(`/booking/showtime/${movieId}`);
    } else {
      navigate('/movies');
    }
  };

  const hourglassVisual = (
    <div className="relative w-20 h-20 flex items-center justify-center">
      <div className="w-16 h-16 rounded-2xl bg-amber-500/10 dark:bg-gold-500/10 border border-amber-500/20 dark:border-gold-500/20 flex items-center justify-center shadow-[0_0_30px_rgba(245,197,24,0.15)]">
        <motion.div
          animate={{ rotate: [0, 180, 180, 0] }}
          transition={{ repeat: Infinity, duration: 4, times: [0, 0.25, 0.75, 1], ease: "easeInOut" }}
        >
          <Hourglass className="w-8 h-8 text-amber-600 dark:text-gold-400" />
        </motion.div>
      </div>
    </div>
  );

  return (
    <FeedbackStateContainer
      variant="warning"
      size={size}
      className={className}
      customVisual={hourglassVisual}
      badge={t('feedback.seat_expired_badge', 'Hết giờ giữ chỗ')}
      title={t('feedback.seat_expired_title', 'Thời gian giữ ghế đã hết')}
      description={t(
        'feedback.seat_expired_desc',
        'Để đảm bảo công bằng cho các khán giả khác, vị trí ghế bạn chọn đã được tự động giải phóng. Vui lòng chọn lại ghế để tiếp tục đặt vé.'
      )}
      primaryAction={{
        label: t('feedback.reselect_seats', 'Chọn lại ghế'),
        icon: RefreshCw,
        onClick: handleReselect
      }}
      secondaryAction={{
        label: t('feedback.back_home', 'Về trang chủ'),
        onClick: () => navigate('/')
      }}
    />
  );
}
