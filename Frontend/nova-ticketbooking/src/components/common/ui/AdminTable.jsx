
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { cn } from '@/utils'

// ── Table ──────────────────────────────────────


export function Table({
  columns, data, loading, skeletonRows = 6, rowKey,
  emptyMessage = 'Không có dữ liệu', emptyIcon = '📭', onRowClick
}) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-gray-50 border-b border-gray-200">
            {columns.map(col => (
              <th key={col.key}
                className={cn('px-4 py-3 text-left font-semibold text-gray-600 whitespace-nowrap', col.headerClassName)}>
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            Array.from({ length: skeletonRows }).map((_, i) => (
              <tr key={i} className="border-b border-gray-100">
                {columns.map(col => (
                  <td key={col.key} className="px-4 py-3">
                    <div className="h-4 bg-gray-100 rounded animate-pulse" style={{ width: `${60 + Math.random() * 40}%` }} />
                  </td>
                ))}
              </tr>
            ))
          ) : data.length === 0 ? (
            <tr>
              <td colSpan={columns.length}>
                <div className="flex flex-col items-center justify-center py-16 text-gray-400">
                  <span className="text-4xl mb-3">{emptyIcon}</span>
                  <p className="text-sm">{emptyMessage}</p>
                </div>
              </td>
            </tr>
          ) : (
            data.map(row => (
              <tr key={rowKey(row)}
                onClick={() => onRowClick?.(row)}
                className={cn(
                  'border-b border-gray-100 transition-colors',
                  onRowClick && 'cursor-pointer hover:bg-gray-50'
                )}>
                {columns.map(col => (
                  <td key={col.key} className={cn('px-4 py-3 text-gray-700', col.className)}>
                    {col.render ? col.render(row) : row[col.key]}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}

// ── Pagination ────────────────────────────────

export function Pagination({ page, totalPages, totalElements, pageSize, onPageChange }) {
  if (totalPages <= 1) return null

  const start = page * pageSize + 1
  const end   = Math.min((page + 1) * pageSize, totalElements)

  // Compute visible page range
  const range = []
  if (totalPages <= 7) {
    for (let i = 0; i < totalPages; i++) range.push(i)
  } else {
    range.push(0)
    if (page > 2) range.push('...')
    for (let i = Math.max(1, page - 1); i <= Math.min(totalPages - 2, page + 1); i++) range.push(i)
    if (page < totalPages - 3) range.push('...')
    range.push(totalPages - 1)
  }

  return (
    <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-white">
      <p className="text-sm text-gray-500">
        Hiển thị <span className="font-medium text-gray-700">{start}–{end}</span> / {totalElements}
      </p>
      <div className="flex items-center gap-1">
        <button onClick={() => onPageChange(page - 1)} disabled={page === 0}
          className="p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 disabled:opacity-30 disabled:cursor-not-allowed transition-all">
          <ChevronLeft className="w-4 h-4" />
        </button>
        {range.map((p, i) =>
          p === '...' ? (
            <span key={`ellipsis-${i}`} className="w-8 text-center text-gray-400 text-sm">…</span>
          ) : (
            <button key={p} onClick={() => onPageChange(p)}
              className={cn(
                'w-8 h-8 rounded-lg text-sm font-medium transition-all',
                p === page ? 'bg-brand-500 text-white' : 'text-gray-600 hover:bg-gray-100'
              )}>
              {p + 1}
            </button>
          )
        )}
        <button onClick={() => onPageChange(page + 1)} disabled={page >= totalPages - 1}
          className="p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 disabled:opacity-30 disabled:cursor-not-allowed transition-all">
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  )
}

// ── Page wrapper card ─────────────────────────
export function AdminCard({ children, className }) {
  return (
    <div className={cn('bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden', className)}>
      {children}
    </div>
  )
}

// ── Page Header ───────────────────────────────
export function PageHeader({ title, subtitle, action }) {
  return (
    <div className="flex items-start justify-between gap-4 mb-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 font-display">{title}</h1>
        {subtitle && <p className="text-gray-500 text-sm mt-0.5">{subtitle}</p>}
      </div>
      {action && <div className="flex-shrink-0">{action}</div>}
    </div>
  )
}

// ── Status Badge ──────────────────────────────
export function StatusBadge({ label, color }) {
  const colorMap = {
    green:  'bg-green-50 text-green-700 border border-green-200',
    red:    'bg-red-50 text-red-700 border border-red-200',
    gold:   'bg-amber-50 text-amber-700 border border-amber-200',
    blue:   'bg-blue-50 text-blue-700 border border-blue-200',
    gray:   'bg-gray-50 text-gray-600 border border-gray-200',
    purple: 'bg-purple-50 text-purple-700 border border-purple-200',
  }
  return (
    <span className={cn('inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium', colorMap[color] ?? colorMap.gray)}>
      {label}
    </span>
  )
}
