import React, { useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Film, Sun, Moon } from 'lucide-react';
import { useThemeStore } from '@/stores/themeStore';
import { useTranslation } from 'react-i18next';
import { cn } from '@/utils';

export function CinematicThemeToggle({ className }) {
  const { mode, toggleMode } = useThemeStore();
  const { t } = useTranslation();
  const [isAnimating, setIsAnimating] = useState(false);

  const isDark = mode === 'dark';

  const handleToggle = useCallback(
    (e) => {
      if (isAnimating) return;
      setIsAnimating(true);
      setTimeout(() => setIsAnimating(false), 450);

      // Check if user prefers reduced motion
      const prefersReducedMotion =
        typeof window !== 'undefined' &&
        window.matchMedia &&
        window.matchMedia('(prefers-reduced-motion: reduce)').matches;

      // Graceful fallback if View Transitions API is not supported or reduced motion enabled
      if (!document.startViewTransition || prefersReducedMotion) {
        toggleMode();
        return;
      }

      // Calculate origin coordinates for radial ripple
      const rect = e.currentTarget.getBoundingClientRect();
      const x = rect.left + rect.width / 2;
      const y = rect.top + rect.height / 2;
      const endRadius = Math.hypot(
        Math.max(x, window.innerWidth - x),
        Math.max(y, window.innerHeight - y)
      );

      const transition = document.startViewTransition(() => {
        toggleMode();
      });

      transition.ready.then(() => {
        const clipPath = [
          `circle(0px at ${x}px ${y}px)`,
          `circle(${endRadius}px at ${x}px ${y}px)`
        ];

        document.documentElement.animate(
          {
            clipPath: isDark ? clipPath : [...clipPath].reverse()
          },
          {
            duration: 400,
            easing: 'cubic-bezier(0.16, 1, 0.3, 1)',
            pseudoElement: isDark
              ? '::view-transition-new(root)'
              : '::view-transition-old(root)'
          }
        );
      });
    },
    [isAnimating, isDark, toggleMode]
  );

  return (
    <button
      type="button"
      onClick={handleToggle}
      aria-label={
        isDark
          ? t('theme.toggle_light', 'Bật đèn sảnh rạp (Chế độ sáng)')
          : t('theme.toggle_dark', 'Tắt đèn phòng chiếu (Chế độ tối)')
      }
      title={
        isDark
          ? t('theme.dark_active', '🎬 Đèn phòng chiếu: TẮT')
          : t('theme.light_active', '🍿 Đèn sảnh rạp: BẬT')
      }
      className={cn(
        'relative p-2.5 rounded-xl transition-all duration-300 group overflow-hidden flex items-center justify-center',
        isDark
          ? 'text-gold-400 hover:text-gold-300 hover:bg-white/10 bg-white/5 border border-gold-500/20'
          : 'text-brand-600 hover:text-brand-500 hover:bg-gray-100 bg-gray-50 border border-brand-500/20',
        className
      )}
    >
      {/* Subtle Projector Spotlight Cone on Hover */}
      <div
        className={cn(
          'absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300 pointer-events-none',
          isDark
            ? 'bg-gradient-to-tr from-gold-500/10 via-transparent to-transparent'
            : 'bg-gradient-to-tr from-brand-500/10 via-transparent to-transparent'
        )}
      />

      <AnimatePresence mode="wait" initial={false}>
        {isDark ? (
          <motion.div
            key="cinema-dark"
            initial={{ rotate: -90, scale: 0.7, opacity: 0 }}
            animate={{ rotate: 0, scale: 1, opacity: 1 }}
            exit={{ rotate: 90, scale: 0.7, opacity: 0 }}
            transition={{ duration: 0.25, ease: 'easeOut' }}
            className="flex items-center justify-center"
          >
            <Film className="w-5 h-5 text-gold-400 drop-shadow-[0_0_8px_rgba(245,197,24,0.5)]" />
          </motion.div>
        ) : (
          <motion.div
            key="cinema-light"
            initial={{ rotate: 90, scale: 0.7, opacity: 0 }}
            animate={{ rotate: 0, scale: 1, opacity: 1 }}
            exit={{ rotate: -90, scale: 0.7, opacity: 0 }}
            transition={{ duration: 0.25, ease: 'easeOut' }}
            className="flex items-center justify-center"
          >
            <Sun className="w-5 h-5 text-brand-500 drop-shadow-[0_0_8px_rgba(229,9,20,0.4)]" />
          </motion.div>
        )}
      </AnimatePresence>
    </button>
  );
}
