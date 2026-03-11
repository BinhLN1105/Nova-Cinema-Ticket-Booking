import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Eye, Calendar, Film, MapPin, CreditCard, Ticket } from 'lucide-react'
import { bookingApi } from '@/api/endpoints'
import { AdminCard, PageHeader, Table, Pagination, StatusBadge } from '@/components/common/ui/AdminTable'
import { SearchInput, Select } from '@/components/common/ui/FormElements'
import { Modal } from '@/components/common/ui/Modal'
import { formatDate, formatDateTime, formatCurrency, cn } from '@/utils'

const STATUS_MAP = {
  PENDING:    { label: 'Chờ thanh toán', color: 'yellow' },
  PAID:       { label: 'Đã thanh toán',  color: 'green' },
  CANCELLED:  { label: 'Đã huỷ',         color: 'red' },
  EXPIRED:    { label: 'Hết hạn',        color: 'gray' },
  CHECKED_IN: { label: 'Đã check-in',    color: 'blue' },
}

const STATUS_OPTIONS = [
  { value: 'PENDING',    label: 'Chờ thanh toán' },
  { value: 'PAID',       label: 'Đã thanh toán' },
  { value: 'CANCELLED',  label: 'Đã huỷ' },
  { value: 'EXPIRED',    label: 'Hết hạn' },
  { value: 'CHECKED_IN', label: 'Đã check-in' },
]

export default function BookingsPage() {
  const [search, setSearch]         = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage]             = useState(0)
  const [viewBooking, setViewBooking] = useState(null)

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'bookings', search, statusFilter, page],
    queryFn: () => bookingApi.getAll({
      search: search || undefined,
      status: statusFilter || undefined,
      page,
      size: 15,
    }),
  })

  // Load full booking detail when viewing
  const { data: detail } = useQuery({
    queryKey: ['admin', 'booking-detail', viewBooking?.id],
    queryFn: () => bookingApi.getById(viewBooking.id),
    enabled: !!viewBooking?.id,
  })

  const columns = [
    {
      key: 'bookingCode', header: 'Mã vé',
      render: (b) => (
        <span className="font-mono font-semibold text-gray-900 text-sm">
          {b.bookingCode}
        </span>
      ),
    },
    {
      key: 'movie', header: 'Phim',
      render: (b) => (
        <div className="flex items-center gap-2.5">
          {b.moviePosterUrl ? (
            <img src={b.moviePosterUrl} alt={b.movieTitle}
              className="w-8 h-12 rounded-lg object-cover flex-shrink-0" />
          ) : (
            <div className="w-8 h-12 rounded-lg bg-gray-100 flex items-center justify-center flex-shrink-0">
              <Film className="w-4 h-4 text-gray-400" />
            </div>
          )}
          <p className="font-medium text-gray-900 text-sm line-clamp-1 max-w-[180px]">
            {b.movieTitle}
          </p>
        </div>
      ),
    },
    {
      key: 'cinema', header: 'Rạp',
      render: (b) => (
        <span className="text-gray-600 text-sm flex items-center gap-1.5">
          <MapPin className="w-3.5 h-3.5 text-gray-400" /> {b.cinemaName ?? '—'}
        </span>
      ),
    },
    {
      key: 'startTime', header: 'Suất chiếu',
      render: (b) => (
        <span className="text-gray-600 text-sm flex items-center gap-1.5">
          <Calendar className="w-3.5 h-3.5 text-gray-400" /> {formatDateTime(b.startTime)}
        </span>
      ),
    },
    {
      key: 'totalAmount', header: 'Tổng tiền',
      render: (b) => (
        <span className="font-semibold text-gray-900 text-sm">
          {formatCurrency(b.totalAmount)}
        </span>
      ),
    },
    {
      key: 'status', header: 'Trạng thái',
      render: (b) => {
        const s = STATUS_MAP[b.status] ?? { label: b.status, color: 'gray' }
        return <StatusBadge label={s.label} color={s.color} />
      },
    },
    {
      key: 'actions', header: '',
      render: (b) => (
        <button onClick={() => setViewBooking(b)}
          className="p-2 rounded-lg hover:bg-gray-100 text-gray-400 hover:text-gray-600 transition-all" title="Xem chi tiết">
          <Eye className="w-4 h-4" />
        </button>
      ),
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader title="Quản lý đặt vé" subtitle={`${data?.totalElements ?? 0} đơn`} />

      <AdminCard>
        <div className="p-4 border-b border-gray-100 flex flex-col sm:flex-row gap-3">
          <SearchInput value={search} onChange={v => { setSearch(v); setPage(0) }}
            placeholder="Tìm theo mã vé..." className="flex-1 max-w-xs" />
          <Select value={statusFilter}
            onChange={e => { setStatusFilter(e.target.value); setPage(0) }}
            options={STATUS_OPTIONS} placeholder="Tất cả trạng thái" className="w-48"
          />
        </div>
        <Table columns={columns} data={data?.content ?? []} loading={isLoading}
          rowKey={b => b.id} emptyMessage="Không có đơn đặt vé" emptyIcon="🎬" />
        {data && <Pagination page={page} totalPages={data.totalPages}
          totalElements={data.totalElements} pageSize={15} onPageChange={setPage} />}
      </AdminCard>

      {/* Booking detail modal */}
      <Modal open={!!viewBooking} onClose={() => setViewBooking(null)}
        title="Chi tiết đặt vé" size="lg">
        {(detail || viewBooking) && (() => {
          const b = detail || viewBooking
          const s = STATUS_MAP[b.status] ?? { label: b.status, color: 'gray' }
          return (
            <div className="p-6 space-y-5">
              {/* Header */}
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs text-gray-400 mb-1">Mã đặt vé</p>
                  <p className="font-mono font-bold text-gray-900 text-lg">{b.bookingCode}</p>
                </div>
                <StatusBadge label={s.label} color={s.color} />
              </div>

              {/* Movie info */}
              <div className="flex gap-4 p-4 bg-gray-50 rounded-xl">
                {b.moviePosterUrl && (
                  <img src={b.moviePosterUrl} alt={b.movieTitle}
                    className="w-16 h-24 rounded-xl object-cover flex-shrink-0" />
                )}
                <div className="flex-1 min-w-0">
                  <p className="font-bold text-gray-900">{b.movieTitle}</p>
                  <p className="text-sm text-gray-500 mt-1 flex items-center gap-1.5">
                    <MapPin className="w-3.5 h-3.5" /> {b.cinemaName} {b.screenName ? `• ${b.screenName}` : ''}
                  </p>
                  <p className="text-sm text-gray-500 mt-0.5 flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5" /> {formatDateTime(b.startTime)}
                  </p>
                </div>
              </div>

              {/* Seats */}
              {b.seats?.length > 0 && (
                <div>
                  <p className="text-xs text-gray-400 uppercase tracking-wider mb-2 font-semibold">Ghế đã đặt</p>
                  <div className="flex flex-wrap gap-2">
                    {b.seats.map((seat, i) => (
                      <span key={i} className="px-3 py-1.5 bg-brand-50 text-brand-600 rounded-lg text-sm font-semibold border border-brand-100">
                        {seat.rowLabel}{seat.colNumber}
                        <span className="text-brand-400 ml-1 font-normal text-xs">
                          ({formatCurrency(seat.price)})
                        </span>
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Combos */}
              {b.combos?.length > 0 && (
                <div>
                  <p className="text-xs text-gray-400 uppercase tracking-wider mb-2 font-semibold">Combo</p>
                  <div className="space-y-1.5">
                    {b.combos.map((c, i) => (
                      <div key={i} className="flex items-center justify-between text-sm p-2.5 bg-gray-50 rounded-lg">
                        <span className="text-gray-700">{c.comboName} × {c.quantity}</span>
                        <span className="font-semibold text-gray-900">{formatCurrency(c.subtotal)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Payment summary */}
              <div className="border-t border-gray-100 pt-4 space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">Tạm tính</span>
                  <span className="text-gray-700">{formatCurrency(b.subtotal)}</span>
                </div>
                {b.voucherCode && (
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-500">Voucher ({b.voucherCode})</span>
                    <span className="text-green-600">-{formatCurrency(b.discountAmount)}</span>
                  </div>
                )}
                <div className="flex justify-between text-base font-bold pt-2 border-t border-gray-100">
                  <span className="text-gray-900">Tổng cộng</span>
                  <span className="text-brand-500">{formatCurrency(b.totalAmount)}</span>
                </div>
              </div>

              {/* Meta */}
              <div className="text-xs text-gray-400 flex items-center gap-4">
                <span>Ngày đặt: {formatDateTime(b.createdAt)}</span>
                {b.expiresAt && <span>Hết hạn: {formatDateTime(b.expiresAt)}</span>}
              </div>
            </div>
          )
        })()}
      </Modal>
    </div>
  )
}
