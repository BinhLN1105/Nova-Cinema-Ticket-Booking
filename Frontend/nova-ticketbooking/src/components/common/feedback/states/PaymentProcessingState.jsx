import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { ShieldCheck, PhoneCall, Ticket, Loader2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FeedbackStateContainer } from '../FeedbackStateContainer';

export function PaymentProcessingState({
  timeoutSeconds = 45,
  bookingCode,
  onTimeout,
  className,
  size = 'default'
}) {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [secondsElapsed, setSecondsElapsed] = useState(0);
  const isTimedOut = secondsElapsed >= timeoutSeconds;

  useEffect(() => {
    const timer = setInterval(() => {
      setSecondsElapsed((prev) => {
        const next = prev + 1;
        if (next === timeoutSeconds && typeof onTimeout === 'function') {
          onTimeout();
        }
        return next;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [timeoutSeconds, onTimeout]);

  const customVisual = (
    <div className="relative w-24 h-24 flex items-center justify-center">
      <div className="w-20 h-20 rounded-3xl bg-amber-500/10 dark:bg-gold-500/10 border border-amber-500/20 dark:border-gold-500/20 flex items-center justify-center relative overflow-hidden shadow-[0_0_30px_rgba(245,197,24,0.15)]">
        <ShieldCheck className="w-10 h-10 text-amber-500 dark:text-gold-400" />
        {/* Laser scanner */}
        <motion.div
          animate={{ y: [-30, 30, -30] }}
          transition={{ repeat: Infinity, duration: 2, ease: "easeInOut" }}
          className="absolute left-0 right-0 h-0.5 bg-gradient-to-r from-transparent via-amber-400 dark:via-gold-400 to-transparent"
        />
      </div>
    </div>
  );

  return (
    <FeedbackStateContainer
      variant="warning"
      size={size}
      className={className}
      customVisual={customVisual}
      badge={t('feedback.payment_processing_badge', 'Bảo mật thanh toán')}
      title={
        !isTimedOut
          ? t('feedback.payment_processing_title', 'Đang xác thực giao dịch...')
          : t('feedback.payment_timeout_title', 'Đang kiểm tra kết quả ngân hàng')
      }
      description={
        !isTimedOut
          ? t(
              'feedback.payment_processing_desc',
              'Giao dịch đang được xử lý an toàn. Vui lòng không đóng trình duyệt hoặc làm mới trang trong giây lát!'
            )
          : t(
              'feedback.payment_timeout_desc',
              'Thời gian phản hồi ngân hàng kéo dài hơn dự kiến. Đừng lo lắng, nếu tài khoản đã trừ tiền, vé sẽ tự động được ghi nhận.'
            )
      }
      primaryAction={
        isTimedOut
          ? {
              label: t('feedback.view_my_tickets', 'Kiểm tra vé của tôi'),
              icon: Ticket,
              onClick: () => navigate('/tickets')
            }
          : undefined
      }
      secondaryAction={
        isTimedOut
          ? {
              label: t('feedback.back_home', 'Về trang chủ'),
              onClick: () => navigate('/')
            }
          : undefined
      }
    >
      <div className="flex flex-col items-center gap-3">
        {!isTimedOut ? (
          <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-white/5 border border-white/10 text-xs text-cinema-300">
            <Loader2 className="w-4 h-4 text-gold-400 animate-spin" />
            <span>
              {t('feedback.elapsed_time', 'Thời gian chờ:')}{' '}
              <strong className="text-gold-400 font-mono text-sm">{secondsElapsed}s</strong> / {timeoutSeconds}s
            </span>
          </div>
        ) : (
          <div className="w-full p-4 rounded-2xl bg-gold-500/10 border border-gold-500/30 text-xs text-gold-200 flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <PhoneCall className="w-4 h-4 text-gold-400 flex-shrink-0" />
              <span>
                {t('feedback.hotline_support', 'Hotline hỗ trợ 24/7:')}{' '}
                <strong className="text-white font-bold">1900 6789</strong>
              </span>
            </div>
            {bookingCode && (
              <span className="font-mono text-cinema-300">
                Mã: <strong className="text-white">{bookingCode}</strong>
              </span>
            )}
          </div>
        )}
      </div>
    </FeedbackStateContainer>
  );
}
