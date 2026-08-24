import React from 'react';
import { Film } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { cn } from '@/utils';

export function CinemaLoadingState({
  variant = 'fullscreen',
  message,
  className
}) {
  const { t } = useTranslation();

  const displayMessage =
    message || t('feedback.loading_message', 'Đang chuẩn bị phòng chiếu...');

  if (variant === 'inline') {
    return (
      <div className={cn('flex items-center justify-center gap-3 py-4 text-slate-600 dark:text-cinema-300 text-sm', className)}>
        <Film className="w-5 h-5 text-brand-500 animate-spin" />
        <span>{displayMessage}</span>
      </div>
    );
  }

  if (variant === 'card') {
    return (
      <div
        className={cn(
          'w-full py-16 px-6 rounded-3xl bg-slate-50 dark:bg-cinema-900/60 border border-slate-200 dark:border-white/10 backdrop-blur-md shadow-sm',
          'flex flex-col items-center justify-center text-center',
          className
        )}
      >
        <div className="relative mb-4">
          <div className="w-14 h-14 rounded-2xl bg-brand-500/10 border border-brand-500/20 flex items-center justify-center animate-pulse">
            <Film className="w-7 h-7 text-brand-500 animate-spin" style={{ animationDuration: '6s' }} />
          </div>
        </div>
        <p className="text-sm font-medium text-slate-700 dark:text-cinema-200">{displayMessage}</p>
        <div className="flex gap-1.5 mt-3">
          {[0, 1, 2].map((i) => (
            <div
              key={i}
              className="w-1.5 h-1.5 rounded-full bg-brand-500 animate-bounce"
              style={{ animationDelay: `${i * 0.15}s` }}
            />
          ))}
        </div>
      </div>
    );
  }

  // Fullscreen default
  return (
    <div
      className={cn(
        'min-h-[70vh] flex flex-col items-center justify-center p-6 text-center text-slate-900 dark:text-white',
        className
      )}
    >
      <div className="relative mb-6">
        <div className="w-20 h-20 rounded-3xl bg-brand-500/10 border border-brand-500/25 flex items-center justify-center animate-pulse shadow-[0_0_40px_rgba(229,9,20,0.15)]">
          <Film className="w-10 h-10 text-brand-500 animate-spin" style={{ animationDuration: '6s' }} />
        </div>
      </div>
      <h3 className="text-lg font-bold font-display text-slate-900 dark:text-white mb-2">{displayMessage}</h3>
      <div className="flex gap-1.5 mt-2">
        {[0, 1, 2].map((i) => (
          <div
            key={i}
            className="w-2 h-2 rounded-full bg-brand-500 animate-bounce"
            style={{ animationDelay: `${i * 0.15}s` }}
          />
        ))}
      </div>
    </div>
  );
}
