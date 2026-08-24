import React from 'react';
import { motion } from 'framer-motion';
import { LogIn } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FeedbackStateContainer } from '../FeedbackStateContainer';

export function SessionExpiredState({
  hasDraft = false,
  draftMinutesLeft = 10,
  onLoginInPlace,
  className,
  size = 'default'
}) {
  const navigate = useNavigate();
  const { t } = useTranslation();

  const handleLogin = () => {
    if (typeof onLoginInPlace === 'function') {
      onLoginInPlace();
    } else {
      navigate('/auth/login?redirect=' + encodeURIComponent(window.location.pathname));
    }
  };

  const lockVisual = (
    <div className="relative w-20 h-20 flex items-center justify-center">
      <motion.div
        animate={{ scale: [0.9, 1.15, 0.9], opacity: [0.4, 0, 0.4] }}
        transition={{ repeat: Infinity, duration: 2.5, ease: "easeInOut" }}
        className="absolute inset-0 rounded-2xl bg-cyan-500/20"
      />
      <div className="w-16 h-16 rounded-2xl bg-cyan-500/10 border border-cyan-500/20 flex flex-col items-center justify-center shadow-[0_0_30px_rgba(6,182,212,0.15)] relative">
        {/* Animated Shackle (Quai khóa sập xuống) */}
        <motion.div
          initial={{ y: -8, rotate: 12 }}
          animate={{ y: 0, rotate: 0 }}
          transition={{ duration: 0.6, type: "spring", stiffness: 200, damping: 15, delay: 0.2 }}
          className="w-7 h-6 border-[3px] border-cyan-500 rounded-t-full -mt-2 bg-transparent"
        />
        {/* Lock Body */}
        <div className="w-10 h-7 rounded-lg bg-gradient-to-br from-cyan-500 to-cyan-600 flex items-center justify-center shadow-md -mt-1 z-10">
          <div className="w-1.5 h-2 rounded-full bg-white/80" />
        </div>
      </div>
    </div>
  );

  return (
    <FeedbackStateContainer
      variant="info"
      size={size}
      className={className}
      customVisual={lockVisual}
      badge={t('feedback.session_expired_badge', 'Hết hạn phiên (401)')}
      title={t('feedback.session_expired_title', 'Phiên đăng nhập đã kết thúc')}
      description={
        hasDraft
          ? t(
              'feedback.session_expired_draft_desc',
              `Đừng lo lắng! Ghế và vé bạn đang đặt đã được giữ tạm trên hệ thống (còn khoảng ${draftMinutesLeft} phút). Vui lòng đăng nhập lại để tiếp tục thanh toán ngay.`
            )
          : t(
              'feedback.session_expired_desc',
              'Để bảo vệ an toàn tài khoản và dữ liệu cá nhân, vui lòng đăng nhập lại để tiếp tục phiên làm việc.'
            )
      }
      primaryAction={{
        label: t('feedback.relogin_now', 'Đăng nhập ngay'),
        icon: LogIn,
        onClick: handleLogin
      }}
      secondaryAction={{
        label: t('feedback.back_home', 'Về trang chủ'),
        onClick: () => navigate('/')
      }}
    />
  );
}
