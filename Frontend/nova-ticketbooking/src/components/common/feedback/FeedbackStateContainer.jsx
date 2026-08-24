import React from 'react';
import { motion } from 'framer-motion';
import { cn } from '@/utils';

const VARIANT_STYLES = {
  default: {
    border: 'border-gray-200/90 dark:border-white/10',
    glow: 'shadow-[0_10px_40px_rgba(0,0,0,0.06)] dark:shadow-[0_0_50px_rgba(229,9,20,0.08)]',
    badge: 'bg-gray-100 dark:bg-white/10 text-gray-700 dark:text-cinema-200 border-gray-200 dark:border-white/15',
    iconBg: 'bg-gray-100 dark:bg-white/5 border-gray-200 dark:border-white/10 text-gray-700 dark:text-cinema-300',
    primaryBtn: 'bg-gradient-to-r from-brand-600 to-brand-500 hover:from-brand-500 hover:to-brand-400 text-white shadow-glow-red border-brand-500/30'
  },
  danger: {
    border: 'border-red-200 dark:border-red-500/20',
    glow: 'shadow-[0_10px_40px_rgba(239,68,68,0.08)] dark:shadow-[0_0_50px_rgba(239,68,68,0.12)]',
    badge: 'bg-red-50 dark:bg-red-500/15 text-red-700 dark:text-red-400 border-red-200 dark:border-red-500/30',
    iconBg: 'bg-red-50 dark:bg-red-500/10 border-red-200 dark:border-red-500/20 text-red-600 dark:text-red-400',
    primaryBtn: 'bg-gradient-to-r from-red-600 to-red-500 hover:from-red-500 hover:to-red-400 text-white shadow-[0_0_20px_rgba(239,68,68,0.4)] border-red-500/30'
  },
  warning: {
    border: 'border-amber-200 dark:border-gold-500/20',
    glow: 'shadow-[0_10px_40px_rgba(245,158,11,0.08)] dark:shadow-[0_0_50px_rgba(245,197,24,0.12)]',
    badge: 'bg-amber-50 dark:bg-gold-500/15 text-amber-800 dark:text-gold-400 border-amber-200 dark:border-gold-500/30',
    iconBg: 'bg-amber-50 dark:bg-gold-500/10 border-amber-200 dark:border-gold-500/20 text-amber-600 dark:text-gold-400',
    primaryBtn: 'bg-gradient-to-r from-gold-500 to-amber-500 hover:from-gold-400 hover:to-amber-400 text-cinema-900 font-bold shadow-[0_0_20px_rgba(245,197,24,0.35)] border-gold-500/30'
  },
  info: {
    border: 'border-cyan-200 dark:border-cyan-500/20',
    glow: 'shadow-[0_10px_40px_rgba(6,182,212,0.08)] dark:shadow-[0_0_50px_rgba(6,182,212,0.12)]',
    badge: 'bg-cyan-50 dark:bg-cyan-500/15 text-cyan-800 dark:text-cyan-400 border-cyan-200 dark:border-cyan-500/30',
    iconBg: 'bg-cyan-50 dark:bg-cyan-500/10 border-cyan-200 dark:border-cyan-500/20 text-cyan-600 dark:text-cyan-400',
    primaryBtn: 'bg-gradient-to-r from-cyan-600 to-cyan-500 hover:from-cyan-500 hover:to-cyan-400 text-white shadow-[0_0_20px_rgba(6,182,212,0.35)] border-cyan-500/30'
  },
  success: {
    border: 'border-emerald-200 dark:border-emerald-500/20',
    glow: 'shadow-[0_10px_40px_rgba(16,185,129,0.08)] dark:shadow-[0_0_50px_rgba(16,185,129,0.12)]',
    badge: 'bg-emerald-50 dark:bg-emerald-500/15 text-emerald-800 dark:text-emerald-400 border-emerald-200 dark:border-emerald-500/30',
    iconBg: 'bg-emerald-50 dark:bg-emerald-500/10 border-emerald-200 dark:border-emerald-500/20 text-emerald-600 dark:text-emerald-400',
    primaryBtn: 'bg-gradient-to-r from-emerald-600 to-emerald-500 hover:from-emerald-500 hover:to-emerald-400 text-white shadow-[0_0_20px_rgba(16,185,129,0.35)] border-emerald-500/30'
  }
};

export function FeedbackStateContainer({
  icon: Icon,
  customVisual,
  badge,
  title,
  description,
  children,
  primaryAction,
  secondaryAction,
  variant = 'default',
  size = 'default',
  className
}) {
  const currentVariant = VARIANT_STYLES[variant] || VARIANT_STYLES.default;

  return (
    <div
      className={cn(
        'w-full flex items-center justify-center p-4',
        size === 'full' && 'min-h-[70vh]',
        size === 'compact' && 'py-6',
        size === 'default' && 'min-h-[450px]',
        className
      )}
    >
      <motion.div
        initial={{ opacity: 0, y: 16, scale: 0.96 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        exit={{ opacity: 0, y: -12, scale: 0.96 }}
        transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
        className={cn(
          'relative w-full max-w-lg mx-auto p-6 sm:p-8 rounded-3xl',
          'bg-white/95 dark:bg-cinema-900/85 backdrop-blur-xl border',
          currentVariant.border,
          currentVariant.glow,
          'text-center flex flex-col items-center overflow-hidden'
        )}
      >
        {/* Subtle Ambient Background Light */}
        <div
          className={cn(
            'absolute -top-24 left-1/2 -translate-x-1/2 w-48 h-48 rounded-full blur-3xl opacity-15 dark:opacity-20 pointer-events-none',
            variant === 'danger' && 'bg-red-500',
            variant === 'warning' && 'bg-amber-500 dark:bg-gold-500',
            variant === 'info' && 'bg-cyan-500',
            variant === 'success' && 'bg-emerald-500',
            variant === 'default' && 'bg-brand-500'
          )}
        />

        {/* Visual Hero / Icon */}
        {customVisual ? (
          <div className="mb-5 relative z-10">{customVisual}</div>
        ) : Icon ? (
          <div className="relative mb-5 z-10">
            <div
              className={cn(
                'w-16 h-16 sm:w-20 sm:h-20 rounded-2xl border flex items-center justify-center transition-all duration-300',
                currentVariant.iconBg
              )}
            >
              <Icon className="w-8 h-8 sm:w-10 sm:h-10" />
            </div>
          </div>
        ) : null}

        {/* Optional Badge */}
        {badge && (
          <span
            className={cn(
              'inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold uppercase tracking-wider mb-3 border z-10',
              currentVariant.badge
            )}
          >
            {badge}
          </span>
        )}

        {/* Title */}
        {title && (
          <h2 className="text-xl sm:text-2xl font-bold font-display text-gray-900 dark:text-white mb-2 leading-snug z-10">
            {title}
          </h2>
        )}

        {/* Description */}
        {description && (
          <p className="text-sm sm:text-base text-gray-600 dark:text-cinema-300 max-w-md mb-5 leading-relaxed z-10">
            {description}
          </p>
        )}

        {/* Extra Interactive Children (Countdown, Seat Suggestions, Forms...) */}
        {children && <div className="w-full mb-6 z-10">{children}</div>}

        {/* Action Buttons */}
        {(primaryAction || secondaryAction) && (
          <div className="w-full flex flex-col sm:flex-row items-center justify-center gap-3 z-10">
            {secondaryAction && (
              <button
                type="button"
                onClick={secondaryAction.onClick}
                disabled={secondaryAction.disabled}
                className={cn(
                  'w-full sm:w-auto px-6 py-3 rounded-xl border border-gray-200 dark:border-white/10',
                  'bg-gray-100 dark:bg-white/5 hover:bg-gray-200/80 dark:hover:bg-white/10 hover:border-gray-300 dark:hover:border-white/20',
                  'text-gray-700 dark:text-cinema-200 hover:text-gray-900 dark:hover:text-white font-medium text-sm transition-all duration-200',
                  'disabled:opacity-40 disabled:cursor-not-allowed',
                  secondaryAction.className
                )}
              >
                {secondaryAction.label}
              </button>
            )}

            {primaryAction && (
              <button
                type="button"
                onClick={primaryAction.onClick}
                disabled={primaryAction.disabled}
                className={cn(
                  'w-full sm:w-auto px-7 py-3 rounded-xl font-semibold text-sm transition-all duration-200 border flex items-center justify-center gap-2',
                  currentVariant.primaryBtn,
                  'disabled:opacity-50 disabled:cursor-not-allowed',
                  primaryAction.className
                )}
              >
                {primaryAction.icon && <primaryAction.icon className="w-4 h-4" />}
                {primaryAction.label}
              </button>
            )}
          </div>
        )}
      </motion.div>
    </div>
  );
}
