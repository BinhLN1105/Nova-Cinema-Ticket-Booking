import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Eye, Printer, ShoppingBag, Calendar, Film, MapPin, Ticket } from 'lucide-react'
import { bookingApi } from '@/api/endpoints'
import { AdminCard, PageHeader, Table, Pagination, StatusBadge } from '@/components/common/ui/AdminTable'
import { SearchInput, Select, Input, Button } from '@/components/common/ui/FormElements'
import { formatDate, formatDateTime, formatCurrency, cn } from '@/utils'
import BookingDetailModal from '@/components/staff/BookingDetailModal'

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
  const [page, setPage] = useState(0)
  const [selectedBookingId, setSelectedBookingId] = useState(null)
  const [isModalOpen, setIsModalOpen] = useState(false)

  const [filters, setFilters] = useState({
    startDate: '',
    endDate: '',
    cinemaId: '',
    status: '',
    paymentMethod: '',
    search: ''
  });

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'bookings', filters, page],
    queryFn: () => bookingApi.getAll({
      search: filters.search || undefined,
      status: filters.status || undefined,
      startDate: filters.startDate ? `${filters.startDate}T00:00:00` : undefined,
      endDate: filters.endDate ? `${filters.endDate}T23:59:59` : undefined,
      page,
      size: 15,
    }),
  })

  const updateFilter = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
    setPage(0);
  };

  const handleClearFilters = () => {
    setFilters({
      startDate: '',
      endDate: '',
      cinemaId: '',
      status: '',
      paymentMethod: '',
      search: ''
    });
    setPage(0);
  };

  const handleViewDetails = (booking) => {
    setSelectedBookingId(booking.id);
    setIsModalOpen(true);
  };

  const handlePrint = (booking) => {
    setSelectedBookingId(booking.id);
    setIsModalOpen(true);
  };

  const columns = [
    {
      key: 'bookingCode', header: 'Mã đơn',
      render: (b) => (
        <span className="font-mono font-semibold text-gray-900 text-sm">
          {b.bookingCode}
        </span>
      ),
    },
    {
      key: 'movie', header: 'Phim/Dịch vụ',
      render: (b) => {
        const isFandB = !b.startTime
        return (
          <div className="flex items-center gap-2.5">
            {isFandB ? (
              <div className="w-8 h-11 rounded-lg bg-emerald-500/10 flex items-center justify-center flex-shrink-0 border border-emerald-500/20">
                <ShoppingBag className="w-4 h-4 text-emerald-500" />
              </div>
            ) : b.moviePosterUrl ? (
              <img src={b.moviePosterUrl} alt={b.movieTitle}
                className="w-8 h-11 rounded-lg object-cover flex-shrink-0" />
            ) : (
              <div className="w-8 h-11 rounded-lg bg-gray-100 flex items-center justify-center flex-shrink-0">
                <Film className="w-4 h-4 text-gray-400" />
              </div>
            )}
            <div className="min-w-0">
              <p className="font-medium text-gray-900 text-sm line-clamp-1">
                {isFandB ? 'Bắp nước & Combo' : b.movieTitle}
              </p>
              {isFandB && <p className="text-[10px] text-emerald-600 font-medium uppercase tracking-wider">F&B Only</p>}
            </div>
          </div>
        )
      },
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
          <Calendar className="w-3.5 h-3.5 text-gray-400" /> {b.startTime ? formatDateTime(b.startTime) : 'N/A'}
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
        <div className="flex items-center gap-1">
          <button onClick={() => setSelectedBookingId(b.id)}
            className="p-2 rounded-lg hover:bg-gray-100 text-gray-400 hover:text-indigo-600 transition-all" title="Xem chi tiết">
            <Eye className="w-4 h-4" />
          </button>
          <button onClick={() => { setSelectedBookingId(b.id); setTimeout(() => window.print(), 100) }}
            className="p-2 rounded-lg hover:bg-gray-100 text-gray-400 hover:text-emerald-600 transition-all" title="In vé/Hóa đơn">
            <Printer className="w-4 h-4" />
          </button>
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader title="Quản lý đặt vé" subtitle={`${data?.totalElements ?? 0} đơn`} />

      <AdminCard>
        <div className="p-4 border-b border-gray-100 flex flex-wrap gap-4 items-end">
          <div className="flex-1 min-w-[240px]">
            <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5 ml-1">Tìm kiếm</label>
            <SearchInput value={filters.search} onChange={v => updateFilter('search', v)}
              placeholder="Mã vé, tên, email..." className="w-full" />
          </div>
          
          <div className="w-48">
            <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5 ml-1">Trạng thái</label>
            <Select value={filters.status}
              onChange={e => updateFilter('status', e.target.value)}
              options={STATUS_OPTIONS} placeholder="Tất cả trạng thái"
            />
          </div>

          <div className="w-44">
            <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5 ml-1">Từ ngày</label>
            <Input type="date" value={filters.startDate} onChange={e => updateFilter('startDate', e.target.value)} />
          </div>

          <div className="w-44">
            <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5 ml-1">Đến ngày</label>
            <Input type="date" value={filters.endDate} onChange={e => updateFilter('endDate', e.target.value)} />
          </div>

          <Button variant="ghost" onClick={handleClearFilters} className="px-4">
            Xoá bộ lọc
          </Button>
        </div>
        
        <Table columns={columns} data={data?.content ?? []} loading={isLoading}
          rowKey={b => b.id} emptyMessage="Không có đơn đặt vé" emptyIcon="🎬" />
          
        {data && <Pagination page={page} totalPages={data.totalPages}
          totalElements={data.totalElements} pageSize={15} onPageChange={setPage} />}
      </AdminCard>

      <BookingDetailModal 
        bookingId={selectedBookingId} 
        onClose={() => setSelectedBookingId(null)} 
      />
    </div>
  )
}
