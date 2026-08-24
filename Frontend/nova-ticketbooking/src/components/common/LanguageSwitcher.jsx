import { useTranslation } from 'react-i18next';
import { Globe } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '@/utils';

export function LanguageSwitcher({ direction = 'down', className }) {
  const { i18n } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

  const languages = [
    { code: 'vi', label: 'Tiếng Việt', flag: '🇻🇳' },
    { code: 'en', label: 'English', flag: '🇬🇧' }
  ];

  const currentLangCode = i18n.language || 'vi';
  // Sometimes i18n.language can be 'vi-VN' or 'en-US' because of language detector
  const currentLang = languages.find(l => currentLangCode.startsWith(l.code)) || languages[0];

  const toggleDropdown = () => setIsOpen(!isOpen);

  const changeLanguage = (code) => {
    i18n.changeLanguage(code);
    setIsOpen(false);
  };

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={toggleDropdown}
        className={cn(
          "flex items-center gap-2 p-2.5 rounded-xl transition-all duration-200",
          className || "text-slate-600 dark:text-cinema-200 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/10"
        )}
        title="Language / Ngôn ngữ"
      >
        <Globe className="w-5 h-5" />
        <span className="text-sm font-medium hidden sm:inline-block uppercase">
          {currentLang.code}
        </span>
      </button>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: direction === 'up' ? -8 : 8, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: direction === 'up' ? -8 : 8, scale: 0.96 }}
            transition={{ duration: 0.2 }}
            className={cn(
              "absolute right-0 w-44 rounded-2xl bg-white dark:bg-[#13141F] border border-slate-200 dark:border-white/15 overflow-hidden shadow-[0_20px_50px_rgba(0,0,0,0.15)] dark:shadow-[0_25px_60px_rgba(0,0,0,0.95)] z-50 p-1.5",
              direction === 'up' ? "bottom-full mb-2" : "top-full mt-2"
            )}
          >
            <div className="py-1">
              {languages.map((lang) => (
                <button
                  key={lang.code}
                  onClick={() => changeLanguage(lang.code)}
                  className={cn(
                    "w-full flex items-center gap-3 px-4 py-3 text-sm transition-colors rounded-xl font-medium",
                    currentLang.code === lang.code 
                      ? "text-brand-600 dark:text-brand-400 bg-brand-500/10 font-bold" 
                      : "text-slate-700 dark:text-cinema-100 hover:bg-slate-100 dark:hover:bg-white/6 hover:text-slate-900 dark:hover:text-white"
                  )}
                >
                  <span className="text-base">{lang.flag}</span>
                  {lang.label}
                </button>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
