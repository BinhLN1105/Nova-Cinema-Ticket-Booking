import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { WifiOff, Wifi } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export function OfflineBanner({ onOnline }) {
  const { t } = useTranslation();
  const [isOnline, setIsOnline] = useState(
    typeof navigator !== 'undefined' ? navigator.onLine : true
  );
  const [showReconnected, setShowReconnected] = useState(false);

  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      setShowReconnected(true);
      if (typeof onOnline === 'function') {
        // Safe Read-only data refetch hook
        onOnline();
      }
      const timer = setTimeout(() => {
        setShowReconnected(false);
      }, 3500);
      return () => clearTimeout(timer);
    };

    const handleOffline = () => {
      setIsOnline(false);
      setShowReconnected(false);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, [onOnline]);

  return (
    <AnimatePresence>
      {(!isOnline || showReconnected) && (
        <motion.div
          initial={{ y: -50, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={{ y: -50, opacity: 0 }}
          transition={{ duration: 0.3 }}
          className={`fixed top-0 left-0 right-0 z-50 py-2.5 px-4 text-xs sm:text-sm font-medium flex items-center justify-center gap-2.5 shadow-lg backdrop-blur-md ${
            !isOnline
              ? 'bg-red-600/90 text-white border-b border-red-500/40'
              : 'bg-emerald-600/90 text-white border-b border-emerald-500/40'
          }`}
        >
          {!isOnline ? (
            <>
              <WifiOff className="w-4 h-4 animate-pulse" />
              <span>
                {t(
                  'feedback.offline_warning',
                  'Mất kết nối internet. Vui lòng kiểm tra lại đường truyền của bạn.'
                )}
              </span>
            </>
          ) : (
            <>
              <Wifi className="w-4 h-4" />
              <span>
                {t(
                  'feedback.reconnected_notice',
                  'Đã kết nối lại internet thành công.'
                )}
              </span>
            </>
          )}
        </motion.div>
      )}
    </AnimatePresence>
  );
}
