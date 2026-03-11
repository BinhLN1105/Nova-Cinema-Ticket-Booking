import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Ticket, Search } from 'lucide-react'
import { bookingApi } from '@/api/endpoints'
import { AdminCard, PageHeader, Table, Pagination, StatusBadge } from '@/components/common/ui/AdminTable'
import { SearchInput, Select } from '@/components/common/ui/FormElements'
import { formatDateTime, formatCurrency, getStatusBadge } from '@/utils'

export default function StaffBookingsPage() {
  const [search, setSearch]   = useState('')
  const [status, setStatus]   = useState('PAID')
  const [page, setPage]       = useState(0)
  const today = new Date().toISOString().split('T')[0]

  const { data, isLoading } = useQuery({
    queryKey: ['staff', 'bookings', search, status, page],
    queryFn: () => bookingApi.getAll({ search, status: status || undefined, date: today, page, size: 20 }),
    refetchInterval: 30_000,
  })

  const columns = [
    {
      key: 'bookingCode', header: 'Mã vé',
      render: (b) => (
        <span className="font-mono text-xs font-bold text-gray-700 bg-gray-100 px-2 py-1 rounded-lg">{b.bookingCode}</span>
      ),
    },
    {
      key: 'movie', header: 'Phim',
      render: (b) => (
        <div className="flex items-center gap-2.5">
          <img src={b.moviePosterUrl} alt={b.movieTitle} className="w-8 h-11 object-cover rounded-lg flex-shrink-0" />
          <div>
            <p className="font-medium text-gray-800 text-sm line-clamp-1 max-w-[140px]">{b.movieTitle}</p>
            <p className="text-xs text-gray-400">{b.seatCount} ghế</p>
          </div>
        </div>
      ),
    },
    {
      key: 'startTime', header: 'Suất chiếu',
      render: (b) => <span className="text-gray-600 text-sm">{formatDateTime(b.startTime)}</span>,
    },
    {
      key: 'totalAmount', header: 'Tiền vé',
      render: (b) => <span className="font-semibold text-gray-800">{formatCurrency(b.totalAmount)}</span>,
    },
    {
      key: 'status', header: 'Trạng thái',
      render: (b) => {
        const s = getStatusBadge(b.status)
        return <StatusBadge label={s.label} color={s.color} />
      },
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader title="Vé chiếu hôm nay" subtitle="Danh sách đặt vé trong ngày" />
      <AdminCard>
        <div className="p-4 border-b border-gray-100 flex flex-col sm:flex-row gap-3">
          <SearchInput value={search} onChange={v => { setSearch(v); setPage(0) }}
            placeholder="Tìm mã vé, tên phim..." className="flex-1 max-w-xs" />
          <Select value={status} onChange={e => { setStatus(e.target.value); setPage(0) }}
            options={[
              { value: 'PAID',      label: '✅ Đã thanh toán' },
              { value: 'PENDING',   label: '⏳ Chờ thanh toán' },
              { value: 'CANCELLED', label: '❌ Đã hủy' },
            ]}
            placeholder="Tất cả" className="w-48" />
        </div>
        <Table columns={columns} data={data?.content ?? []} loading={isLoading}
          rowKey={b => b.id} emptyMessage="Không có vé nào" emptyIcon="🎟️" />
        {data && <Pagination page={page} totalPages={data.totalPages}
          totalElements={data.totalElements} pageSize={20} onPageChange={setPage} />}
      </AdminCard>
    </div>
  )
}
