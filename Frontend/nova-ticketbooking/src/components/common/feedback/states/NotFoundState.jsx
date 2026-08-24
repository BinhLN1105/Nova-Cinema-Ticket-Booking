import React from 'react';
import { motion } from 'framer-motion';
import { Clapperboard, Home, Compass } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FeedbackStateContainer } from '../FeedbackStateContainer';

export function NotFoundState({
  title,
  description,
  backUrl = '/',
  backLabel,
  exploreUrl = '/movies',
  exploreLabel,
  onHome,
  className,
  size = 'default'
}) {
  const navigate = useNavigate();
  const { t } = useTranslation();

  const handleHome = () => {
    if (typeof onHome === 'function') {
      onHome();
    } else {
      navigate(backUrl);
    }
  };

  const notFoundVisual = (
    <div className="relative w-20 h-20 flex items-center justify-center">
      <div className="w-16 h-16 rounded-2xl bg-brand-500/10 border border-brand-500/20 flex items-center justify-center shadow-[0_0_30px_rgba(229,9,20,0.15)]">
        <motion.div
          animate={{ rotate: [-6, 6, -6] }}
          transition={{ repeat: Infinity, duration: 3, ease: "easeInOut" }}
        >
          <Clapperboard className="w-8 h-8 text-brand-500" />
        </motion.div>
      </div>
    </div>
  );

  return (
    <FeedbackStateContainer
      variant="default"
      size={size}
      className={className}
      customVisual={notFoundVisual}
      badge={t('feedback.not_found_badge', '404 - Không tìm thấy')}
      title={title || t('feedback.not_found_title', 'Trang hoặc nội dung không tồn tại')}
      description={
        description ||
        t(
          'feedback.not_found_desc',
          'Rất tiếc! Phim, suất chiếu hoặc đường dẫn bạn truy cập không tồn tại hoặc đã ngừng phục vụ trên hệ thống.'
        )
      }
      primaryAction={{
        label: backLabel || t('feedback.back_home', 'Về trang chủ'),
        icon: Home,
        onClick: () => navigate(backUrl)
      }}
      secondaryAction={{
        label: exploreLabel || t('feedback.explore_movies', 'Khám phá phim mới'),
        icon: Compass,
        onClick: () => navigate(exploreUrl)
      }}
    />
  );
}
