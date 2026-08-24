import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Clock, Flame, RefreshCw } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { FeedbackStateContainer } from '../FeedbackStateContainer';

export function RateLimitedState({
  retryAfterSeconds = 30,
  isDailyQuota = false,
  onRetry,
  onReady,
  className,
  size = 'default'
}) {
  const { t } = useTranslation();
  const [countdown, setCountdown] = useState(retryAfterSeconds);

  useEffect(() => {
    setCountdown(retryAfterSeconds);
  }, [retryAfterSeconds]);

  useEffect(() => {
    if (isDailyQuota || countdown <= 0) return;

    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          if (typeof onReady === 'function') onReady();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [countdown, isDailyQuota, onReady]);

  const flameVisual = (
    <div className="relative w-20 h-20 flex items-center justify-center">
      <div className="w-16 h-16 rounded-2xl bg-amber-500/10 dark:bg-gold-500/10 border border-amber-500/20 dark:border-gold-500/20 flex items-center justify-center shadow-[0_0_30px_rgba(245,197,24,0.15)]">
        <motion.div
          animate={{ scale: [1, 1.15, 1], rotate: [-6, 6, -6] }}
          transition={{ repeat: Infinity, duration: 2, ease: "easeInOut" }}
        >
          <Flame className="w-8 h-8 text-amber-500 dark:text-gold-400" />
        </motion.div>
      </div>
    </div>
  );

  return (
    <FeedbackStateContainer
      variant="warning"
      size={size}
      className={className}
      customVisual={flameVisual}
      badge={t('feedback.rate_limit_badge', 'Giới hạn tần suất (429)')}
      title={
        isDailyQuota
          ? t('feedback.quota_exceeded_title', 'Đã đạt hạn mức trong ngày')
          : t('feedback.rate_limit_title', 'Bạn thao tác hơi nhanh rồi!')
      }
      description={
        isDailyQuota
          ? t(
              'feedback.quota_exceeded_desc',
              'Hạn mức sử dụng tính năng này của bạn hôm nay đã hết. Hệ thống sẽ tự động làm mới vào 00:00 ngày mai.'
            )
          : t(
              'feedback.rate_limit_desc',
              'Hệ thống đang tạm thời làm mát để tránh quá tải. Vui lòng nghỉ ngơi trong giây lát trước khi thực hiện lại.'
            )
      }
      primaryAction={
        !isDailyQuota && onRetry
          ? {
              label:
                countdown > 0
                  ? `${t('feedback.retry_after', 'Thử lại sau')} (${countdown}s)`
                  : t('feedback.retry_now', 'Thử lại ngay'),
              icon: RefreshCw,
              disabled: countdown > 0,
              onClick: onRetry
            }
          : undefined
      }
    >
      {!isDailyQuota && countdown > 0 && (
        <div className="inline-flex items-center gap-2 px-4 py-2 rounded-2xl bg-gold-500/10 border border-gold-500/25 text-gold-300 font-mono text-sm font-semibold">
          <Clock className="w-4 h-4 text-gold-400 animate-pulse" />
          <span>{countdown}s</span>
        </div>
      )}
    </FeedbackStateContainer>
  );
}
