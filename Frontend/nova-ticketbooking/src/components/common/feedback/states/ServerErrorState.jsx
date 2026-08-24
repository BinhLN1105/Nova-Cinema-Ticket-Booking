import React from 'react';
import { motion } from 'framer-motion';
import { ServerCrash, RefreshCw, PhoneCall } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { FeedbackStateContainer } from '../FeedbackStateContainer';

export function ServerErrorState({
  errorCode = 500,
  errorId,
  onRetry,
  isRetrying = false,
  className,
  size = 'default'
}) {
  const { t } = useTranslation();

  const serverVisual = (
    <div className="relative w-20 h-20 flex items-center justify-center">
      <div className="w-16 h-16 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-center relative shadow-[0_0_30px_rgba(239,68,68,0.15)]">
        <motion.div
          animate={{ rotate: [-8, 8, -8] }}
          transition={{ repeat: Infinity, duration: 3, ease: "easeInOut" }}
        >
          <ServerCrash className="w-8 h-8 text-red-500" />
        </motion.div>
      </div>
    </div>
  );

  return (
    <FeedbackStateContainer
      variant="danger"
      size={size}
      className={className}
      customVisual={serverVisual}
      badge={`${t('feedback.server_error_badge', 'Lỗi máy chủ')} (${errorCode || 500})`}
      title={t('feedback.server_error_title', 'Hệ thống đang gặp sự cố kỹ thuật')}
      description={t(
        'feedback.server_error_desc',
        'Máy chủ tạm thời không thể hoàn thành yêu cầu. Đội ngũ kỹ thuật viên NovaCinema đang khẩn trương khắc phục.'
      )}
      primaryAction={
        onRetry
          ? {
              label: isRetrying
                ? t('feedback.reloading', 'Đang tải lại...')
                : t('feedback.reload_page', 'Tải lại trang'),
              icon: RefreshCw,
              disabled: isRetrying,
              onClick: onRetry
            }
          : {
              label: t('feedback.reload_page', 'Tải lại trang'),
              icon: RefreshCw,
              onClick: () => window.location.reload()
            }
      }
    >
      <div className="flex items-center justify-center gap-2 p-3 rounded-2xl bg-white/5 border border-white/10 text-xs text-cinema-300">
        <PhoneCall className="w-4 h-4 text-brand-400" />
        <span>
          {t('feedback.need_help', 'Cần trợ giúp khẩn cấp? Hotline:')}{' '}
          <strong className="text-white font-bold">1900 6789</strong>
        </span>
      </div>
    </FeedbackStateContainer>
  );
}
