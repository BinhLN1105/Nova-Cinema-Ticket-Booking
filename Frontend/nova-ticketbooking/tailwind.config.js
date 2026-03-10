/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      // ── Brand Colors ──────────────────────────────────
      colors: {
        brand: {
          50:  '#fff1f1',
          100: '#ffe1e1',
          200: '#ffc7c7',
          300: '#ffa0a0',
          400: '#ff6b6b',
          500: '#E50914',   // Nova Red
          600: '#c40612',
          700: '#a30510',
          800: '#87080f',
          900: '#710b10',
          950: '#3d0105',
        },
        gold: {
          50:  '#fffbeb',
          100: '#fef3c7',
          200: '#fde68a',
          300: '#fcd34d',
          400: '#F5A623',   // Nova Gold
          500: '#f59e0b',
          600: '#d97706',
          700: '#b45309',
        },
        cinema: {
          950: '#060608',
          900: '#0A0A0F',   // Nền tối chính
          800: '#111118',
          700: '#1a1a24',
          600: '#232330',
          500: '#2d2d3d',
          400: '#3d3d52',
          300: '#5a5a78',
          200: '#8888aa',
          100: '#bbbbcc',
        },
        // Admin/Staff surface
        slate: {
          950: '#020617',
        },
      },

      // ── Typography ────────────────────────────────────
      fontFamily: {
        display: ['"Playfair Display"', 'Georgia', 'serif'],
        body:    ['"DM Sans"', 'system-ui', 'sans-serif'],
        mono:    ['"JetBrains Mono"', 'monospace'],
      },
      fontSize: {
        '2xs': ['0.625rem', { lineHeight: '0.875rem' }],
      },

      // ── Spacing & Sizing ──────────────────────────────
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
        '128': '32rem',
      },

      // ── Border Radius ─────────────────────────────────
      borderRadius: {
        '4xl': '2rem',
        '5xl': '2.5rem',
      },

      // ── Shadows ───────────────────────────────────────
      boxShadow: {
        'glow-red':    '0 0 20px rgba(229, 9, 20, 0.4), 0 0 60px rgba(229, 9, 20, 0.15)',
        'glow-gold':   '0 0 20px rgba(245, 166, 35, 0.4)',
        'glow-blue':   '0 0 20px rgba(59, 130, 246, 0.4)',
        'card-dark':   '0 4px 24px rgba(0,0,0,0.6)',
        'card-float':  '0 20px 60px rgba(0,0,0,0.4)',
        'inner-glow':  'inset 0 1px 0 rgba(255,255,255,0.06)',
      },

      // ── Backdrop Blur ─────────────────────────────────
      backdropBlur: {
        xs: '2px',
      },

      // ── Background Images / Gradients ─────────────────
      backgroundImage: {
        'gradient-radial':   'radial-gradient(var(--tw-gradient-stops))',
        'gradient-conic':    'conic-gradient(from 180deg at 50% 50%, var(--tw-gradient-stops))',
        'noise':             "url(\"data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.04'/%3E%3C/svg%3E\")",
        'cinema-hero':       'linear-gradient(to bottom, rgba(10,10,15,0) 0%, rgba(10,10,15,0.8) 60%, rgba(10,10,15,1) 100%)',
        'card-shine':        'linear-gradient(135deg, rgba(255,255,255,0.05) 0%, transparent 50%)',
      },

      // ── Animations ────────────────────────────────────
      keyframes: {
        'fade-in': {
          from: { opacity: '0' },
          to:   { opacity: '1' },
        },
        'fade-up': {
          from: { opacity: '0', transform: 'translateY(24px)' },
          to:   { opacity: '1', transform: 'translateY(0)' },
        },
        'fade-down': {
          from: { opacity: '0', transform: 'translateY(-16px)' },
          to:   { opacity: '1', transform: 'translateY(0)' },
        },
        'slide-in-right': {
          from: { opacity: '0', transform: 'translateX(32px)' },
          to:   { opacity: '1', transform: 'translateX(0)' },
        },
        'slide-in-left': {
          from: { opacity: '0', transform: 'translateX(-32px)' },
          to:   { opacity: '1', transform: 'translateX(0)' },
        },
        'scale-in': {
          from: { opacity: '0', transform: 'scale(0.92)' },
          to:   { opacity: '1', transform: 'scale(1)' },
        },
        'shimmer': {
          from: { backgroundPosition: '-200% 0' },
          to:   { backgroundPosition: '200% 0' },
        },
        'pulse-red': {
          '0%, 100%': { boxShadow: '0 0 0 0 rgba(229,9,20,0.5)' },
          '50%':      { boxShadow: '0 0 0 12px rgba(229,9,20,0)' },
        },
        'float': {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%':      { transform: 'translateY(-8px)' },
        },
        'spin-slow': {
          from: { transform: 'rotate(0deg)' },
          to:   { transform: 'rotate(360deg)' },
        },
        'marquee': {
          from: { transform: 'translateX(0)' },
          to:   { transform: 'translateX(-50%)' },
        },
        'count-up': {
          from: { opacity: '0', transform: 'translateY(8px)' },
          to:   { opacity: '1', transform: 'translateY(0)' },
        },
      },
      animation: {
        'fade-in':         'fade-in 0.4s ease-out',
        'fade-up':         'fade-up 0.5s ease-out',
        'fade-up-delay-1': 'fade-up 0.5s ease-out 0.1s both',
        'fade-up-delay-2': 'fade-up 0.5s ease-out 0.2s both',
        'fade-up-delay-3': 'fade-up 0.5s ease-out 0.3s both',
        'fade-down':       'fade-down 0.4s ease-out',
        'slide-in-right':  'slide-in-right 0.4s ease-out',
        'slide-in-left':   'slide-in-left 0.4s ease-out',
        'scale-in':        'scale-in 0.3s ease-out',
        'shimmer':         'shimmer 2s linear infinite',
        'pulse-red':       'pulse-red 2s ease-in-out infinite',
        'float':           'float 4s ease-in-out infinite',
        'spin-slow':       'spin-slow 8s linear infinite',
        'marquee':         'marquee 30s linear infinite',
      },

      // ── Transitions ───────────────────────────────────
      transitionTimingFunction: {
        'spring': 'cubic-bezier(0.34, 1.56, 0.64, 1)',
        'smooth': 'cubic-bezier(0.4, 0, 0.2, 1)',
      },
      transitionDuration: {
        '400': '400ms',
        '600': '600ms',
        '800': '800ms',
      },
    },
  },
  plugins: [
    // Plugin tùy chỉnh
    function({ addUtilities, addComponents, theme }) {
      addUtilities({
        // Glass morphism
        '.glass': {
          background: 'rgba(255, 255, 255, 0.05)',
          backdropFilter: 'blur(12px)',
          border: '1px solid rgba(255, 255, 255, 0.08)',
        },
        '.glass-dark': {
          background: 'rgba(0, 0, 0, 0.4)',
          backdropFilter: 'blur(16px)',
          border: '1px solid rgba(255, 255, 255, 0.06)',
        },
        // Text gradient
        '.text-gradient-red': {
          background: 'linear-gradient(135deg, #E50914 0%, #FF6B35 100%)',
          '-webkit-background-clip': 'text',
          '-webkit-text-fill-color': 'transparent',
          backgroundClip: 'text',
        },
        '.text-gradient-gold': {
          background: 'linear-gradient(135deg, #F5A623 0%, #FFD700 100%)',
          '-webkit-background-clip': 'text',
          '-webkit-text-fill-color': 'transparent',
          backgroundClip: 'text',
        },
        // Scrollbar tối
        '.scrollbar-cinema': {
          scrollbarWidth: 'thin',
          scrollbarColor: '#E50914 #1a1a24',
        },
        '.scrollbar-hide': {
          '-ms-overflow-style': 'none',
          'scrollbar-width': 'none',
          '&::-webkit-scrollbar': { display: 'none' },
        },
        // Seat styles
        '.seat-available': {
          background: 'rgba(22, 163, 74, 0.2)',
          border: '1px solid rgba(22, 163, 74, 0.5)',
          cursor: 'pointer',
          transition: 'all 0.2s',
          '&:hover': {
            background: 'rgba(22, 163, 74, 0.4)',
            transform: 'scale(1.1)',
          },
        },
        '.seat-selected': {
          background: 'rgba(229, 9, 20, 0.8)',
          border: '1px solid #E50914',
          boxShadow: '0 0 8px rgba(229,9,20,0.5)',
        },
        '.seat-booked': {
          background: 'rgba(107, 114, 128, 0.3)',
          border: '1px solid rgba(107, 114, 128, 0.3)',
          cursor: 'not-allowed',
          opacity: '0.5',
        },
        '.seat-vip': {
          background: 'rgba(124, 58, 237, 0.2)',
          border: '1px solid rgba(124, 58, 237, 0.5)',
        },
      })

      addComponents({
        // Card variants
        '.card-cinema': {
          background: 'rgba(17, 17, 24, 0.8)',
          border: '1px solid rgba(255, 255, 255, 0.06)',
          borderRadius: '1rem',
          backdropFilter: 'blur(8px)',
          boxShadow: '0 4px 24px rgba(0,0,0,0.5)',
          transition: 'all 0.3s ease',
          '&:hover': {
            border: '1px solid rgba(229, 9, 20, 0.3)',
            boxShadow: '0 8px 40px rgba(0,0,0,0.6)',
            transform: 'translateY(-2px)',
          },
        },
        // Button variants
        '.btn-primary': {
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '0.5rem',
          padding: '0.75rem 1.5rem',
          background: 'linear-gradient(135deg, #E50914 0%, #c40612 100%)',
          color: 'white',
          fontFamily: '"DM Sans", sans-serif',
          fontWeight: '600',
          borderRadius: '0.75rem',
          border: 'none',
          cursor: 'pointer',
          transition: 'all 0.3s ease',
          boxShadow: '0 4px 16px rgba(229,9,20,0.3)',
          '&:hover': {
            boxShadow: '0 8px 32px rgba(229,9,20,0.5)',
            transform: 'translateY(-1px)',
          },
          '&:active': { transform: 'translateY(0)' },
          '&:disabled': {
            opacity: '0.5',
            cursor: 'not-allowed',
            transform: 'none',
          },
        },
        '.btn-ghost': {
          display: 'inline-flex',
          alignItems: 'center',
          gap: '0.5rem',
          padding: '0.75rem 1.5rem',
          background: 'transparent',
          color: 'rgba(255,255,255,0.8)',
          border: '1px solid rgba(255,255,255,0.12)',
          borderRadius: '0.75rem',
          cursor: 'pointer',
          transition: 'all 0.2s',
          '&:hover': {
            background: 'rgba(255,255,255,0.06)',
            borderColor: 'rgba(255,255,255,0.24)',
            color: 'white',
          },
        },
        // Input cinema
        '.input-cinema': {
          width: '100%',
          padding: '0.875rem 1rem',
          background: 'rgba(255,255,255,0.04)',
          border: '1px solid rgba(255,255,255,0.1)',
          borderRadius: '0.75rem',
          color: 'white',
          fontFamily: '"DM Sans", sans-serif',
          fontSize: '0.9375rem',
          transition: 'all 0.2s',
          '&::placeholder': { color: 'rgba(255,255,255,0.3)' },
          '&:focus': {
            outline: 'none',
            borderColor: '#E50914',
            background: 'rgba(229,9,20,0.05)',
            boxShadow: '0 0 0 3px rgba(229,9,20,0.15)',
          },
        },
        // Badge
        '.badge': {
          display: 'inline-flex',
          alignItems: 'center',
          padding: '0.25rem 0.625rem',
          borderRadius: '9999px',
          fontSize: '0.75rem',
          fontWeight: '600',
          letterSpacing: '0.025em',
        },
        '.badge-red':    { background: 'rgba(229,9,20,0.15)',  color: '#ff6b6b',  border: '1px solid rgba(229,9,20,0.3)' },
        '.badge-green':  { background: 'rgba(22,163,74,0.15)', color: '#4ade80',  border: '1px solid rgba(22,163,74,0.3)' },
        '.badge-gold':   { background: 'rgba(245,166,35,0.15)',color: '#fbbf24',  border: '1px solid rgba(245,166,35,0.3)' },
        '.badge-blue':   { background: 'rgba(59,130,246,0.15)',color: '#60a5fa',  border: '1px solid rgba(59,130,246,0.3)' },
        '.badge-gray':   { background: 'rgba(107,114,128,0.15)',color: '#9ca3af', border: '1px solid rgba(107,114,128,0.3)' },
      })
    }
  ],
}
