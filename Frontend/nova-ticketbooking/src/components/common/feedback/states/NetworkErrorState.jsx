import React from 'react';
import { motion } from 'framer-motion';
import { WifiOff, RefreshCw } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { FeedbackStateContainer } from '../FeedbackStateContainer';

export function NetworkErrorState({
  onRetry,
  isRetrying = false,
  className,
  size = 'default'
}) {
  const { t } = useTranslation();

  const customVisual = (
    <div className="relative w-20 h-20 flex items-center justify-center">
      {/* Radar waves */}
      <motion.div
        animate={{ scale: [0.9, 1.3, 0.9], opacity: [0.5, 0, 0.5] }}
        transition={{ repeat: Infinity, duration: 2.2, ease: "easeOut" }}
        className="absolute inset-0 rounded-2xl border border-red-500/30"
      />
      <div className="w-16 h-16 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-center relative shadow-[0_0_30px_rgba(239,68,68,0.15)]">
        <motion.div
          animate={{ opacity: [1, 0.4, 1] }}
          transition={{ repeat: Infinity, duration: 1.8 }}
        >
          <WifiOff className="w-8 h-8 text-red-500" />
        </motion.div>
      </div>
    </div>
  );

  return (
    <FeedbackStateContainer
      variant="danger"
      size={size}
      className={className}
      customVisual={customVisual}
      badge={t('feedback.network_error_badge', 'Lỗi kết nối')}
      title={t('feedback.network_error_title', 'Không thể kết nối đến máy chủ')}
      description={t(
        'feedback.network_error_desc',
        'Đường truyền internet của bạn đang bị gián đoạn hoặc máy chủ phản hồi chậm. Vui lòng kiểm tra lại Wifi/4G của bạn.'
      )}
      primaryAction={
        onRetry
          ? {
              label: isRetrying
                ? t('feedback.retrying', 'Đang kết nối lại...')
                : t('feedback.retry_now', 'Thử lại kết nối'),
              icon: RefreshCw,
              disabled: isRetrying,
              onClick: onRetry
            }
          : undefined
      }
    />
  );
}
