import React from 'react';
import { motion } from 'framer-motion';
import { Armchair, Sparkles, RefreshCcw } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { FeedbackStateContainer } from '../FeedbackStateContainer';

export function SeatConflictState({
  conflictedSeats = [],
  suggestedSeats = [],
  onSelectAlternative,
  onReloadSeats,
  onReselect,
  className,
  size = 'default'
}) {
  const { t } = useTranslation();

  const seatText = conflictedSeats.length > 0
    ? conflictedSeats.join(', ')
    : t('feedback.conflicted_seats_fallback', 'Vị trí ghế');

  const handleAction = onReloadSeats || onReselect;

  const conflictVisual = (
    <div className="relative w-24 h-20 flex items-center justify-center">
      <div className="w-20 h-16 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-center gap-1 shadow-[0_0_30px_rgba(239,68,68,0.15)] relative overflow-hidden">
        {/* Left Seat */}
        <motion.div
          animate={{ x: [-6, 0, -6] }}
          transition={{ repeat: Infinity, duration: 2, ease: "easeInOut" }}
        >
          <Armchair className="w-6 h-6 text-red-500" />
        </motion.div>
        {/* Center Alert Spark */}
        <motion.div
          animate={{ scale: [0.8, 1.2, 0.8], opacity: [0.6, 1, 0.6] }}
          transition={{ repeat: Infinity, duration: 1.5 }}
          className="text-amber-500 font-bold text-xs"
        >
          ⚡
        </motion.div>
        {/* Right Seat */}
        <motion.div
          animate={{ x: [6, 0, 6] }}
          transition={{ repeat: Infinity, duration: 2, ease: "easeInOut" }}
        >
          <Armchair className="w-6 h-6 text-red-400" />
        </motion.div>
      </div>
    </div>
  );

  return (
    <FeedbackStateContainer
      variant="danger"
      size={size}
      className={className}
      customVisual={conflictVisual}
      badge={t('feedback.seat_conflict_badge', 'Xung đột ghế ngồi (409)')}
      title={t('feedback.seat_conflict_title', 'Ghế vừa có người khác đặt mất')}
      description={t(
        'feedback.seat_conflict_desc',
        `Rất tiếc! Ghế (${seatText}) vừa được khán giả khác hoàn tất thanh toán trước ít giây. Vui lòng cập nhật lại sơ đồ ghế.`
      )}
      primaryAction={{
        label: t('feedback.reload_seatmap', 'Cập nhật sơ đồ ghế'),
        icon: RefreshCcw,
        onClick: handleAction
      }}
    >
      {/* Fallback & Alternative Adjacent Seats (Aligned with AI Suggestion Engine) */}
      {suggestedSeats && suggestedSeats.length > 0 && (
        <div className="p-4 rounded-2xl bg-white/[0.04] border border-white/10 text-left">
          <div className="flex items-center gap-2 mb-2 text-xs font-semibold text-gold-400 uppercase tracking-wider">
            <Sparkles className="w-4 h-4 text-gold-400" />
            <span>{t('feedback.suggested_seats_label', 'Ghế trống gần nhất gợi ý:')}</span>
          </div>
          <div className="flex flex-wrap gap-2">
            {suggestedSeats.map((seat, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => onSelectAlternative && onSelectAlternative(seat)}
                className="px-3 py-1.5 rounded-xl bg-gold-500/10 hover:bg-gold-500/20 border border-gold-500/30 text-gold-300 font-mono font-bold text-xs transition-all flex items-center gap-1.5"
              >
                <Armchair className="w-3.5 h-3.5" />
                <span>{typeof seat === 'object' ? `${seat.rowLabel}${seat.colNumber}` : seat}</span>
              </button>
            ))}
          </div>
        </div>
      )}
    </FeedbackStateContainer>
  );
}
