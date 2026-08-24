import React from 'react';
import { motion } from 'framer-motion';
import { Ticket, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FeedbackStateContainer } from '../FeedbackStateContainer';

export function EmptyState({
  icon = Ticket,
  badge,
  title,
  description,
  actionLabel,
  onAction,
  actionLink,
  secondaryLabel,
  onSecondaryAction,
  className,
  size = 'default'
}) {
  const navigate = useNavigate();
  const { t } = useTranslation();

  const handlePrimary = () => {
    if (typeof onAction === 'function') {
      onAction();
    } else if (actionLink) {
      navigate(actionLink);
    }
  };

  const emptyVisual = (
    <div className="relative w-20 h-20 flex items-center justify-center">
      <div className="w-16 h-16 rounded-2xl bg-brand-500/10 border border-brand-500/20 flex items-center justify-center shadow-[0_0_30px_rgba(229,9,20,0.15)]">
        <motion.div
          animate={{ y: [0, -6, 0] }}
          transition={{ repeat: Infinity, duration: 2.5, ease: "easeInOut" }}
        >
          {React.createElement(icon, { className: "w-8 h-8 text-brand-500" })}
        </motion.div>
      </div>
    </div>
  );

  return (
    <FeedbackStateContainer
      variant="default"
      size={size}
      className={className}
      customVisual={emptyVisual}
      badge={badge || t('feedback.empty_badge', 'Trống')}
      title={title || t('feedback.empty_title', 'Chưa có dữ liệu')}
      description={
        description ||
        t(
          'feedback.empty_desc',
          'Hiện tại chưa có dữ liệu nào để hiển thị trong mục này.'
        )
      }
      primaryAction={
        actionLabel || actionLink
          ? {
              label: actionLabel || t('feedback.explore_now', 'Khám phá ngay'),
              icon: ArrowRight,
              onClick: handlePrimary
            }
          : undefined
      }
      secondaryAction={
        secondaryLabel && onSecondaryAction
          ? {
              label: secondaryLabel,
              onClick: onSecondaryAction
            }
          : undefined
      }
    />
  );
}
