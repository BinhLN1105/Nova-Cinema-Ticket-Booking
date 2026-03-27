import { forwardRef, InputHTMLAttributes, SelectHTMLAttributes, TextareaHTMLAttributes, ReactNode } from 'react'
import { Info } from 'lucide-react'
import { cn } from '@/utils'

// ── Label ─────────────────────────────────────
export function Label({ children, required, htmlFor }) {
  return (
    <label htmlFor={htmlFor} className="block text-sm font-medium text-gray-700 mb-1.5">
      {children}
      {required && <span className="text-red-500 ml-0.5">*</span>}
    </label>
  )
}

// ── Error ─────────────────────────────────────
export function FieldError({ message }) {
  if (!message) return null
  return <p className="mt-1.5 text-xs text-red-500">{message}</p>
}

// ── Field wrapper ─────────────────────────────
export function Field({ label, required, info, error, children, htmlFor, className }) {
  return (
    <div className={cn('space-y-1.5', className)}>
      {label && (
        <div className="flex items-center gap-1.5">
          <Label required={required} htmlFor={htmlFor}>{label}</Label>
          {info && (
            <div className="group relative flex items-center">
              <Info className="w-3.5 h-3.5 text-gray-400 cursor-help hover:text-brand-500 transition-colors" />
              <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:block z-50">
                <div className="bg-slate-900 shadow-2xl text-white text-[11px] px-3 py-2 rounded-xl w-48 text-center leading-relaxed">
                  {info}
                  <div className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-slate-900" />
                </div>
              </div>
            </div>
          )}
        </div>
      )}
      {children}
      <FieldError message={error} />
    </div>
  )
}

// ── Input ─────────────────────────────────────
export const Input = forwardRef(
  ({ className, error, leftIcon, ...props }, ref) => (
    <div className="relative">
      {leftIcon && (
        <div className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none">
          {leftIcon}
        </div>
      )}
      <input
        ref={ref}
        className={cn(
          'w-full px-3.5 py-2.5 text-sm bg-white border rounded-xl text-gray-800',
          'placeholder-gray-400 transition-all duration-200',
          'focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-400',
          error ? 'border-red-400 bg-red-50/30' : 'border-gray-200 hover:border-gray-300',
          leftIcon && 'pl-10',
          className
        )}
        {...props}
      />
    </div>
  )
)
Input.displayName = 'Input'

// ── Select ────────────────────────────────────
export const Select = forwardRef(
  ({ className, error, options, placeholder, ...props }, ref) => (
    <select
      ref={ref}
      className={cn(
        'w-full px-3.5 py-2.5 text-sm bg-white border rounded-xl text-gray-800',
        'transition-all duration-200 cursor-pointer appearance-none',
        'focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-400',
        error ? 'border-red-400' : 'border-gray-200 hover:border-gray-300',
        className
      )}
      {...props}
    >
      {placeholder && <option value="">{placeholder}</option>}
      {options.map(o => (
        <option key={o.value} value={o.value}>{o.label}</option>
      ))}
    </select>
  )
)
Select.displayName = 'Select'

// ── Textarea ──────────────────────────────────
export const Textarea = forwardRef(
  ({ className, error, ...props }, ref) => (
    <textarea
      ref={ref}
      rows={4}
      className={cn(
        'w-full px-3.5 py-2.5 text-sm bg-white border rounded-xl text-gray-800 resize-none',
        'placeholder-gray-400 transition-all duration-200',
        'focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-400',
        error ? 'border-red-400' : 'border-gray-200 hover:border-gray-300',
        className
      )}
      {...props}
    />
  )
)
Textarea.displayName = 'Textarea'

// ── Toggle / Switch ───────────────────────────
export function Switch({ checked, onChange, label, description, disabled }) {
  return (
    <div className="flex items-center justify-between gap-4">
      {(label || description) && (
        <div>
          {label && <p className="text-sm font-medium text-gray-700">{label}</p>}
          {description && <p className="text-xs text-gray-400 mt-0.5">{description}</p>}
        </div>
      )}
      <button
        type="button"
        onClick={() => !disabled && onChange(!checked)}
        disabled={disabled}
        className={cn(
          'relative inline-flex h-6 w-11 items-center rounded-full transition-colors duration-200',
          'focus:outline-none focus:ring-2 focus:ring-brand-500/30 focus:ring-offset-2',
          checked ? 'bg-brand-500' : 'bg-gray-200',
          disabled && 'opacity-50 cursor-not-allowed'
        )}
      >
        <span className={cn(
          'inline-block h-4 w-4 rounded-full bg-white shadow-sm transition-transform duration-200',
          checked ? 'translate-x-6' : 'translate-x-1'
        )} />
      </button>
    </div>
  )
}

// ── Search Input ──────────────────────────────
export function SearchInput({ value, onChange, placeholder = 'Tìm kiếm...', className }) {
  return (
    <div className={cn('relative', className)}>
      <svg className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400"
        fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
          d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
      <input
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full pl-10 pr-4 py-2.5 text-sm bg-white border border-gray-200
          rounded-xl text-gray-700 placeholder-gray-400 hover:border-gray-300
          focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-400 transition-all"
      />
      {value && (
        <button onClick={() => onChange('')}
          className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      )}
    </div>
  )
}

// ── Button variants ───────────────────────────
export function Button({ variant = 'primary', size = 'md', loading, isLoading, leftIcon, children, className, disabled, ...props }) {
  const isBtnLoading = loading || isLoading
  const VAR = {
    primary:   'bg-brand-500 hover:bg-brand-600 text-white shadow-sm',
    secondary: 'bg-blue-500 hover:bg-blue-600 text-white shadow-sm',
    danger:    'bg-red-500 hover:bg-red-600 text-white shadow-sm',
    ghost:     'bg-white hover:bg-gray-50 text-gray-700 border border-gray-200',
  }
  const SZ = {
    sm: 'px-3 py-1.5 text-xs rounded-lg gap-1.5',
    md: 'px-4 py-2.5 text-sm rounded-xl gap-2',
    lg: 'px-6 py-3 text-base rounded-xl gap-2',
  }
  return (
    <button
      disabled={disabled || isBtnLoading}
      className={cn(
        'inline-flex items-center justify-center font-semibold transition-all duration-200',
        'focus:outline-none focus:ring-2 focus:ring-brand-500/30 focus:ring-offset-2',
        'disabled:opacity-50 disabled:cursor-not-allowed',
        VAR[variant], SZ[size], className
      )}
      {...props}
    >
      {isBtnLoading ? (
        <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
      ) : leftIcon}
      {children}
    </button>
  )
}
