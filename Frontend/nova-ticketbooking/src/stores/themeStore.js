import { create } from 'zustand'
import { persist } from 'zustand/middleware'

const DEFAULTS = {
  brandColor:  '#E50914',
  accentColor: '#F5A623',
  animations:  true,
  compact:     false,
}

/** Parse hex → { r, g, b } */
function hexToRgb(hex) {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return { r, g, b }
}

/** Generate lighter/darker shades from a base hex */
function generatePalette(hex) {
  const { r, g, b } = hexToRgb(hex)
  const mix = (base, target, pct) =>
    Math.round(base + (target - base) * pct)
  const lighter = (pct) =>
    `rgb(${mix(r, 255, pct)}, ${mix(g, 255, pct)}, ${mix(b, 255, pct)})`
  const darker = (pct) =>
    `rgb(${mix(r, 0, pct)}, ${mix(g, 0, pct)}, ${mix(b, 0, pct)})`

  return {
    50:  lighter(0.95),
    100: lighter(0.88),
    200: lighter(0.75),
    300: lighter(0.55),
    400: lighter(0.3),
    500: hex,
    600: darker(0.15),
    700: darker(0.3),
    800: darker(0.45),
    900: darker(0.55),
    950: darker(0.75),
  }
}

const STYLE_ID = 'nova-theme-overrides'

function applyTheme({ brandColor, accentColor, animations, compact }) {
  const root = document.documentElement
  const brand = generatePalette(brandColor)
  const accent = generatePalette(accentColor)
  const { r: br, g: bg, b: bb } = hexToRgb(brandColor)
  const { r: ar, g: ab, b: ac } = hexToRgb(accentColor)

  // Update CSS custom properties
  root.style.setProperty('--brand-red', brandColor)
  root.style.setProperty('--brand-gold', accentColor)

  // Inject dynamic style overrides for Tailwind brand classes
  let styleEl = document.getElementById(STYLE_ID)
  if (!styleEl) {
    styleEl = document.createElement('style')
    styleEl.id = STYLE_ID
    document.head.appendChild(styleEl)
  }

  styleEl.textContent = `
    /* ── Dynamic brand palette ─── */
    .bg-brand-50  { background-color: ${brand[50]}  !important; }
    .bg-brand-100 { background-color: ${brand[100]} !important; }
    .bg-brand-200 { background-color: ${brand[200]} !important; }
    .bg-brand-300 { background-color: ${brand[300]} !important; }
    .bg-brand-400 { background-color: ${brand[400]} !important; }
    .bg-brand-500 { background-color: ${brand[500]} !important; }
    .bg-brand-600 { background-color: ${brand[600]} !important; }
    .bg-brand-700 { background-color: ${brand[700]} !important; }
    .bg-brand-800 { background-color: ${brand[800]} !important; }
    .bg-brand-900 { background-color: ${brand[900]} !important; }
    .bg-brand-950 { background-color: ${brand[950]} !important; }

    .text-brand-50  { color: ${brand[50]}  !important; }
    .text-brand-100 { color: ${brand[100]} !important; }
    .text-brand-200 { color: ${brand[200]} !important; }
    .text-brand-300 { color: ${brand[300]} !important; }
    .text-brand-400 { color: ${brand[400]} !important; }
    .text-brand-500 { color: ${brand[500]} !important; }
    .text-brand-600 { color: ${brand[600]} !important; }
    .text-brand-700 { color: ${brand[700]} !important; }

    .border-brand-100 { border-color: ${brand[100]} !important; }
    .border-brand-200 { border-color: ${brand[200]} !important; }
    .border-brand-400 { border-color: ${brand[400]} !important; }
    .border-brand-500 { border-color: ${brand[500]} !important; }
    .border-brand-600 { border-color: ${brand[600]} !important; }

    .ring-brand-500\\/20 { --tw-ring-color: rgba(${br},${bg},${bb},0.2) !important; }
    .ring-brand-500\\/30 { --tw-ring-color: rgba(${br},${bg},${bb},0.3) !important; }
    .focus\\:ring-brand-500\\/20:focus { --tw-ring-color: rgba(${br},${bg},${bb},0.2) !important; }

    .hover\\:bg-brand-600:hover { background-color: ${brand[600]} !important; }
    .hover\\:bg-brand-700:hover { background-color: ${brand[700]} !important; }
    .hover\\:text-brand-400:hover { color: ${brand[400]} !important; }
    .hover\\:text-brand-500:hover { color: ${brand[500]} !important; }

    .bg-brand-500\\/10 { background-color: rgba(${br},${bg},${bb},0.1) !important; }
    .bg-brand-500\\/20 { background-color: rgba(${br},${bg},${bb},0.2) !important; }
    .bg-brand-50\\/80  { background-color: rgba(${br},${bg},${bb},0.04) !important; }
    .from-brand-400 { --tw-gradient-from: ${brand[400]} !important; }
    .from-brand-500 { --tw-gradient-from: ${brand[500]} !important; }
    .from-brand-600 { --tw-gradient-from: ${brand[600]} !important; }
    .to-brand-600   { --tw-gradient-to: ${brand[600]}   !important; }
    .to-brand-700   { --tw-gradient-to: ${brand[700]}   !important; }

    /* ── Dynamic gold/accent palette ─── */
    .bg-gold-400 { background-color: ${accent[400]} !important; }
    .bg-gold-500 { background-color: ${accent[500]} !important; }
    .text-gold-400 { color: ${accent[400]} !important; }
    .text-gold-500 { color: ${accent[500]} !important; }
    .from-gold-400 { --tw-gradient-from: ${accent[400]} !important; }

    /* ── Hardcoded CSS overrides ─── */
    :root {
      --brand-red:  ${brandColor};
      --brand-gold: ${accentColor};
    }
    :focus-visible { outline-color: ${brandColor}; }
    ::selection    { background: rgba(${br},${bg},${bb},0.3); }
    ::-webkit-scrollbar-thumb { background: rgba(${br},${bg},${bb},0.4); }
    ::-webkit-scrollbar-thumb:hover { background: rgba(${br},${bg},${bb},0.7); }

    .text-gradient-red {
      background: linear-gradient(135deg, ${brandColor} 0%, ${brand[400]} 100%) !important;
      -webkit-background-clip: text !important;
      -webkit-text-fill-color: transparent !important;
    }
    .text-gradient-gold {
      background: linear-gradient(135deg, ${accentColor} 0%, ${accent[300]} 100%) !important;
      -webkit-background-clip: text !important;
      -webkit-text-fill-color: transparent !important;
    }
    .btn-primary {
      background: linear-gradient(135deg, ${brandColor} 0%, ${brand[600]} 100%) !important;
      box-shadow: 0 4px 16px rgba(${br},${bg},${bb},0.3) !important;
    }
    .btn-primary:hover {
      box-shadow: 0 8px 32px rgba(${br},${bg},${bb},0.5) !important;
    }
    .border-gradient-red {
      background:
        linear-gradient(#1a1a24, #1a1a24) padding-box,
        linear-gradient(135deg, ${brandColor}, ${brand[400]}) border-box !important;
    }
    .shadow-glow-red {
      box-shadow: 0 0 20px rgba(${br},${bg},${bb},0.4), 0 0 60px rgba(${br},${bg},${bb},0.15) !important;
    }
    .shadow-glow-gold {
      box-shadow: 0 0 20px rgba(${ar},${ab},${ac},0.4) !important;
    }
    .input-cinema:focus {
      border-color: ${brandColor} !important;
      background: rgba(${br},${bg},${bb},0.05) !important;
      box-shadow: 0 0 0 3px rgba(${br},${bg},${bb},0.15) !important;
    }
    .card-cinema:hover {
      border-color: rgba(${br},${bg},${bb},0.3) !important;
    }
    .seat-selected {
      background: rgba(${br},${bg},${bb},0.8) !important;
      border-color: ${brandColor} !important;
      box-shadow: 0 0 8px rgba(${br},${bg},${bb},0.5) !important;
    }
    .scrollbar-cinema {
      scrollbar-color: ${brandColor} #1a1a24 !important;
    }
    .badge-red {
      background: rgba(${br},${bg},${bb},0.15) !important;
      color: ${brand[400]} !important;
      border-color: rgba(${br},${bg},${bb},0.3) !important;
    }
    .badge-gold {
      background: rgba(${ar},${ab},${ac},0.15) !important;
      color: ${accent[300]} !important;
      border-color: rgba(${ar},${ab},${ac},0.3) !important;
    }

    /* ── Animation toggle ─── */
    ${!animations ? `
    *, *::before, *::after {
      animation-duration: 0s !important;
      transition-duration: 0s !important;
    }
    ` : ''}

    /* ── Compact mode ─── */
    ${compact ? `
    .py-6 { padding-top: 1rem !important; padding-bottom: 1rem !important; }
    .py-8 { padding-top: 1.5rem !important; padding-bottom: 1.5rem !important; }
    .gap-6 { gap: 1rem !important; }
    .gap-8 { gap: 1.5rem !important; }
    .space-y-6 > * + * { margin-top: 1rem !important; }
    .space-y-8 > * + * { margin-top: 1.5rem !important; }
    ` : ''}
  `
}

export const useThemeStore = create(
  persist(
    (set, get) => ({
      ...DEFAULTS,

      setTheme: (updates) => {
        set(updates)
        applyTheme({ ...get(), ...updates })
      },

      resetTheme: () => {
        set(DEFAULTS)
        applyTheme(DEFAULTS)
      },

      init: () => applyTheme(get()),
    }),
    {
      name: 'nova-theme',
    }
  )
)
