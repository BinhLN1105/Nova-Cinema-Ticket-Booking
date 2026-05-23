import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { 
  X, Printer, Ticket, ShoppingBag, Clock, 
  MapPin, CreditCard, User, Calendar, Search, AlertCircle
} from 'lucide-react'
import { toast } from 'react-hot-toast'
import { bookingApi } from '@/api/endpoints'
import { formatCurrency, formatDateTime, getStatusBadge, cn } from '@/utils'
import { Modal } from '@/components/common/ui/Modal'
import { StatusBadge } from '@/components/common/ui/AdminTable'
import { QRCodeCanvas } from 'qrcode.react'

export default function BookingDetailModal({ bookingId, onClose }) {
  const queryClient = useQueryClient()
  
  const { data: booking, isLoading } = useQuery({
    queryKey: ['booking', 'detail', bookingId],
    queryFn: () => bookingApi.getById(bookingId),
    enabled: !!bookingId
  })

  const cancelMutation = useMutation({
    mutationFn: () => bookingApi.cancelDirect(bookingId),
    onSuccess: () => {
      toast.success('Hủy đơn hàng thành công')
      queryClient.invalidateQueries({ queryKey: ['booking', 'detail', bookingId] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'bookings'] })
      queryClient.invalidateQueries({ queryKey: ['staff', 'bookings'] })
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Có lỗi xảy ra khi hủy đơn hàng')
    }
  })

  if (!bookingId) return null

  const handlePrint = () => {
    window.print()
  }

  const handleCancel = () => {
    if (window.confirm('Bạn có chắc chắn muốn hủy đơn hàng này? Thao tác này sẽ hoàn tiền CP trực tiếp cho khách hàng.')) {
      cancelMutation.mutate()
    }
  }

  const isFandB = !booking?.showtimeId

  return (
    <Modal open={!!bookingId} onClose={onClose} title="Chi tiết đơn hàng" size="lg">
      <div className="relative">
        {!booking && isLoading ? (
          <div className="py-20 flex flex-col items-center justify-center space-y-4">
            <div className="w-10 h-10 border-4 border-blue-500 border-t-transparent rounded-full animate-spin" />
            <p className="text-gray-500 text-sm animate-pulse">Đang tải thông tin...</p>
          </div>
        ) : !booking ? (
            <div className="py-20 text-center text-gray-500">Không tìm thấy thông tin đơn hàng</div>
        ) : (
          <div className="space-y-6">
            {/* Header Info */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-4 bg-gray-50 rounded-2xl border border-gray-100">
              <div>
                <div className="flex items-center gap-2 mb-1">
                   <span className="text-xs font-mono font-bold px-2 py-0.5 bg-white border border-gray-200 rounded text-gray-600">
                     #{booking.bookingCode}
                   </span>
                   {booking.status && <StatusBadge {...getStatusBadge(booking.status)} />}
                </div>
                <div className="flex items-center gap-3 text-xs text-gray-500">
                   <p className="flex items-center gap-1"><Calendar className="w-3 h-3" /> {formatDateTime(booking.createdAt)}</p>
                   {booking.paymentMethod && (
                     <p className="flex items-center gap-1 uppercase font-bold text-blue-600">
                        <CreditCard className="w-3 h-3" /> {booking.paymentMethod}
                     </p>
                   )}
                </div>
              </div>
              
              <div className="flex gap-2">
                {booking.status === 'PAID' && (
                  <button 
                    onClick={handleCancel}
                    className="flex items-center justify-center gap-2 px-5 py-2.5 border border-red-200 bg-red-50 hover:bg-red-100 text-red-600 rounded-xl font-bold text-sm transition-all shadow-sm"
                  >
                    Hủy đơn hàng
                  </button>
                )}
                <button 
                  onClick={handlePrint}
                  className="flex items-center justify-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-bold text-sm transition-all shadow-md shadow-blue-200"
                >
                  <Printer className="w-4 h-4" /> In vé & Hóa đơn
                </button>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Movie/Ticket Info */}
              <div className="space-y-4">
                <h3 className="font-bold text-gray-900 flex items-center gap-2 px-1">
                  <Ticket className="w-4 h-4 text-blue-500" /> Thông tin vé
                </h3>
                
                {isFandB ? (
                  <div className="p-8 border-2 border-dashed border-gray-100 rounded-2xl flex flex-col items-center justify-center text-center">
                    <ShoppingBag className="w-8 h-8 text-amber-400 mb-2" />
                    <p className="text-sm font-medium text-gray-500 italic">Đơn hàng chỉ bao gồm Bắp nước</p>
                  </div>
                ) : (
                  <div className="bg-white border border-gray-100 rounded-2xl p-4 shadow-sm space-y-4">
                    <div className="flex gap-4">
                       <img src={booking.moviePosterUrl || 'https://via.placeholder.com/150'} className="w-20 h-28 object-cover rounded-xl shadow-sm" alt="Poster" />
                       <div className="flex-1 min-w-0">
                          <h4 className="font-black text-gray-900 leading-tight mb-1">{booking.movieTitle}</h4>
                          <p className="text-xs text-brand-600 font-bold uppercase tracking-wider mb-2">{booking.screenType} • {booking.screenName}</p>
                          <div className="space-y-1">
                             <p className="text-xs text-gray-500 flex items-center gap-1.5"><MapPin className="w-3 h-3" /> {booking.cinemaName}</p>
                             <p className="text-xs font-bold text-gray-700 flex items-center gap-1.5"><Clock className="w-3 h-3" /> {formatDateTime(booking.startTime)}</p>
                          </div>
                       </div>
                    </div>
                    <div className="pt-3 border-t border-gray-50">
                       <p className="text-xs text-gray-400 mb-1">Ghế đã chọn:</p>
                       <div className="flex flex-wrap gap-1.5">
                         {booking.seats?.map(s => (
                           <span key={s.showtimeSeatId} className="px-2 py-1 bg-blue-50 text-blue-700 rounded-md text-[11px] font-bold border border-blue-100">
                             {s.rowLabel}{s.colNumber}
                           </span>
                         ))}
                       </div>
                    </div>
                  </div>
                )}
              </div>

              {/* Concessions / Combo Info */}
              <div className="space-y-4">
                <h3 className="font-bold text-gray-900 flex items-center gap-2 px-1">
                  <ShoppingBag className="w-4 h-4 text-amber-500" /> Bắp & Nước
                </h3>
                
                <div className="bg-white border border-gray-100 rounded-2xl p-4 shadow-sm min-h-[140px]">
                  {booking.combos?.length > 0 ? (
                    <div className="space-y-3">
                      {booking.combos.map(c => (
                        <div key={c.comboId} className="flex justify-between items-center text-sm">
                          <div className="flex-1">
                            <p className="font-bold text-gray-800">{c.comboName}</p>
                            <p className="text-[11px] text-gray-400">x{c.quantity} • {formatCurrency(c.unitPrice)}</p>
                          </div>
                          <p className="font-black text-gray-900">{formatCurrency(c.subtotal)}</p>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="h-24 flex items-center justify-center text-gray-400 text-xs italic">
                      Không mua bắp nước
                    </div>
                  )}
                </div>

                {/* Summary */}
                <div className="bg-gray-900 rounded-2xl p-5 text-white shadow-xl">
                    <div className="space-y-2 text-xs opacity-70">
                       <div className="flex justify-between"><span>Tạm tính</span><span>{formatCurrency(booking.subtotal || booking.totalAmount)}</span></div>
                       {booking.discountAmount > 0 && (
                         <div className="flex justify-between text-emerald-400">
                            <span>Voucher ({booking.voucherCode})</span>
                            <span>-{formatCurrency(booking.discountAmount)}</span>
                         </div>
                       )}
                       {booking.promotionDiscountAmount > 0 && (
                         <div className="flex justify-between text-amber-400">
                            <span>KM: {booking.appliedPromotionName}</span>
                            <span>-{formatCurrency(booking.promotionDiscountAmount)}</span>
                         </div>
                       )}
                       {booking.rankDiscountAmount > 0 && (
                         <div className="flex justify-between text-yellow-400">
                            <span>Ưu đãi Hạng thành viên</span>
                            <span>-{formatCurrency(booking.rankDiscountAmount)}</span>
                         </div>
                       )}
                    </div>
                    <div className="mt-4 pt-4 border-t border-white/10 flex justify-between items-end">
                       <span className="text-sm font-bold">Tổng thanh toán</span>
                       <span className="text-2xl font-black text-blue-400">{formatCurrency(booking.totalAmount)}</span>
                    </div>
                </div>
              </div>
            </div>

            {/* QR Code Section (Only if PAID) */}
            {booking.status === 'PAID' && booking.qrCode && (
               <div className="flex flex-col items-center py-4 border-t border-gray-100">
                  <div className="p-4 bg-white rounded-2xl shadow-inner border border-gray-100">
                    <QRCodeCanvas value={booking.qrCode} size={120} />
                  </div>
                  <p className="text-[10px] font-mono text-gray-400 mt-2 uppercase tracking-[0.2em]">Mục đích check-in</p>
               </div>
            )}
          </div>
        )}
      </div>

      {/* ──────────────────────────────────────────────────────────────────
          PRINT TEMPLATE (Hidden from UI, visible only on Print)
          ────────────────────────────────────────────────────────────────── */}
      <style dangerouslySetInnerHTML={{ __html: `
        @media print {
          body * { visibility: hidden; }
          #print-area, #print-area * { visibility: visible; }
          #print-area {
            position: absolute;
            left: 0;
            top: 0;
            width: 80mm;
            padding: 2mm;
            background: white;
            color: black;
            font-family: 'Courier New', Courier, monospace;
            font-size: 10pt;
            line-height: 1.2;
          }
          .page-break { page-break-after: always; min-height: 50mm; }
          .receipt-divider { border-top: 1px dashed #000; margin: 8px 0; }
          .ticket-header { text-align: center; border: 2px solid #000; padding: 5px; margin-bottom: 8px; font-weight: bold; font-size: 14pt; }
          .mb-8 { margin-bottom: 8px; }
          .text-center { text-align: center; }
          .flex-between { display: flex; justify-content: space-between; gap: 4px; }
          .font-title { font-size: 16pt; font-weight: bold; }
          .font-mono { font-family: 'Courier New', Courier, monospace; }
        }
      `}} />

      <div id="print-area" className="hidden">
        {/* 1. THE RECEIPT (1 COPY) */}
        <div className="page-break">
            <div className="text-center mb-8">
                <h1 className="font-title">NOVA CINEMA</h1>
                <p className="text-xs">{booking?.cinemaName}</p>
                <div className="receipt-divider" />
                <h2 style={{fontSize: '14pt', fontWeight: 'bold'}}>BIÊN LAI THANH TOÁN</h2>
                <p>Mã: {booking?.bookingCode}</p>
                <p>Ngày: {formatDateTime(new Date())}</p>
            </div>

            <div className="mb-8">
                <p><b>KH:</b> {booking?.userEmail || 'Khách vãng lai'}</p>
                <div className="receipt-divider" />
                
                {/* Items */}
                {!isFandB && (
                    <div className="mb-8">
                        <p><b>PHIM: {booking?.movieTitle}</b></p>
                        <p>Suất: {formatDateTime(booking?.startTime)}</p>
                        <p>Phòng: {booking?.screenName} | Ghế: {booking?.seats?.map(s => `${s.rowLabel}${s.colNumber}`).join(', ')}</p>
                    </div>
                )}

                {booking?.combos?.length > 0 && (
                    <div className="mb-8">
                        <p><b>BẮP NƯỚC:</b></p>
                        {booking.combos.map(c => (
                            <div key={c.comboId} className="flex-between">
                                <span className="flex-1">- {c.comboName} x{c.quantity}</span>
                                <span>{formatCurrency(c.subtotal)}</span>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            <div className="receipt-divider" />
            <div className="flex-between"><span>Tạm tính:</span> <span>{formatCurrency(booking?.subtotal || booking?.totalAmount)}</span></div>
            {booking?.discountAmount > 0 && (
                <div className="flex-between text-xs"><span>Voucher:</span> <span>-{formatCurrency(booking?.discountAmount)}</span></div>
            )}
            {booking?.rankDiscountAmount > 0 && (
                <div className="flex-between text-xs"><span>Ưu đãi Hạng:</span> <span>-{formatCurrency(booking?.rankDiscountAmount)}</span></div>
            )}
            <div className="flex-between" style={{fontSize: '12pt', fontWeight: 'bold', marginTop: '4px'}}>
                <span>TỔNG CỘNG:</span> <span>{formatCurrency(booking?.totalAmount)}</span>
            </div>
            
            <div className="receipt-divider" />
                <div className="text-center">
                    <p>Cảm ơn quý khách!</p>
                    <p>Vui lòng giữ lại hóa đơn này để nhận bắp nước.</p>
                    {booking?.qrCode && (
                        <div style={{marginTop: '10px', display: 'flex', justifyContent: 'center'}}>
                             <QRCodeCanvas value={booking.qrCode} size={80} />
                        </div>
                    )}
                </div>
        </div>

        {/* 2. THE TICKETS (ONE PER SEAT) */}
        {booking?.seats?.map((seat, index) => (
            <div key={seat.showtimeSeatId} className="page-break">
                <div className="ticket-header">VÉ XEM PHIM</div>
                <div className="text-center mb-8">
                    <p style={{fontSize: '12pt', fontWeight: 'bold'}}>{booking.movieTitle}</p>
                    <p className="text-xs">{booking.screenType} - {booking.screenName}</p>
                </div>
                
                <div className="receipt-divider" />
                <div>
                    <div className="flex-between text-xs"><span>Rạp:</span> <span>{booking.cinemaName}</span></div>
                    <div className="flex-between text-xs"><span>Suất:</span> <span>{formatDateTime(booking.startTime)}</span></div>
                    <div className="flex-between" style={{fontSize: '20pt', margin: '10px 0'}}>
                        <b>GHẾ:</b> <span style={{border: '2px solid black', padding: '0 8px'}}>{seat.rowLabel}{seat.colNumber}</span>
                    </div>
                </div>
                <div className="receipt-divider" />
                <div className="text-center">
                    <p className="text-xs">Mã vé: {booking.bookingCode}-{index + 1}</p>
                    <div style={{marginTop: '10px', display: 'flex', justifyContent: 'center', background: 'white', padding: '5px'}}>
                         <QRCodeCanvas value={booking.qrCode || booking.bookingCode} size={90} />
                    </div>
                    <p style={{fontSize: '7pt', marginTop: '10px'}}>Vé chỉ có giá trị sử dụng một lần.</p>
                </div>
            </div>
        ))}
      </div>
    </Modal>
  )
}
