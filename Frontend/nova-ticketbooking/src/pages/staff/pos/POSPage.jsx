import React, { useState, useEffect } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { 
  Search, 
  Monitor, 
  CreditCard, 
  Banknote, 
  ChevronRight, 
  Ticket, 
  ShoppingBag,
  RefreshCw,
  CheckCircle2,
  XCircle
} from 'lucide-react';
import { QRCodeCanvas } from 'qrcode.react';
import { showtimeApi, bookingApi, comboApi, cinemaApi } from '@/api/endpoints';
import { useAuthStore } from '@/stores/authStore';
import { formatCurrency, cn } from '@/utils';
import { PageLoader } from '@/components/common/feedback/PageLoader';
import { toast } from 'react-hot-toast';
import { motion, AnimatePresence } from 'framer-motion';

// --- Sub-components ---

const SeatButton = ({ seat, isSelected, onToggle }) => {
  const isBooked = seat.status === 'BOOKED' || seat.status === 'LOCKED';
  const colors = {
    STANDARD: 'border-green-500/50 bg-green-500/10 hover:bg-green-500/30',
    VIP:      'border-purple-500/50 bg-purple-500/10 hover:bg-purple-500/30',
    COUPLE:   'border-orange-500/50 bg-orange-500/10 hover:bg-orange-500/30 w-16',
  };
  const selectedColors = {
    STANDARD: 'bg-brand-500 border-brand-400 text-white shadow-lg',
    VIP:      'bg-purple-500 border-purple-400 text-white shadow-lg',
    COUPLE:   'bg-orange-500 border-orange-400 text-white shadow-lg w-16',
  };

  return (
    <button
      disabled={isBooked}
      onClick={onToggle}
      className={cn(
        'h-9 w-9 rounded-md border text-[10px] font-bold transition-all flex items-center justify-center',
        isBooked ? 'bg-slate-800 border-slate-700 text-slate-600 cursor-not-allowed opacity-50' : 
        isSelected ? selectedColors[seat.seatType] : colors[seat.seatType]
      )}
    >
      {seat.rowLabel}{seat.colNumber}
    </button>
  );
};

export default function POSPage() {
  const { user } = useAuthStore();
  const [selectedShowtime, setSelectedShowtime] = useState(null);
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [selectedCombos, setSelectedCombos] = useState({}); // comboId -> quantity
  const [paymentStep, setPaymentStep] = useState('selection'); // selection, payment, success
  const [vnpayUrl, setVnpayUrl] = useState(null);
  const [cinemaSelection, setCinemaSelection] = useState(null); // For Admin

  // Logic: Staff auto-binds to cinemaId. Admin picks.
  const activeCinemaId = user?.role === 'ADMIN' ? cinemaSelection : user?.cinemaId;

  // 1. Fetch Showtimes Today
  const { data: showtimesResp, isLoading: loadingShows } = useQuery({
    queryKey: ['pos-showtimes', activeCinemaId],
    queryFn: () => showtimeApi.getAll({ 
      cinemaId: activeCinemaId, 
      date: new Date().toISOString().split('T')[0],
      size: 100 
    }),
    enabled: !!activeCinemaId
  });

  // 2. Fetch Combos
  const { data: combosResp } = useQuery({
    queryKey: ['pos-combos'],
    queryFn: () => comboApi.getAvailable()
  });

  // 3. Fetch Seat Map when showtime selected
  const { data: seatMap, isLoading: loadingSeats } = useQuery({
    queryKey: ['pos-seatmap', selectedShowtime?.id],
    queryFn: () => showtimeApi.getSeatMap(selectedShowtime.id),
    enabled: !!selectedShowtime
  });

  // 4. Live Pricing (Quote)
  const [quote, setQuote] = useState(null);
  useEffect(() => {
    if (selectedShowtime && selectedSeats.length > 0) {
      const fetchQuote = async () => {
        try {
          const comboItems = Object.entries(selectedCombos).map(([id, qty]) => ({ comboId: id, quantity: qty }));
          const resp = await bookingApi.getQuote({
            showtimeId: selectedShowtime.id,
            showtimeSeatIds: selectedSeats.map(s => s.showtimeSeatId),
            combos: comboItems
          });
          setQuote(resp);
        } catch (e) {
          console.error(e);
        }
      };
      fetchQuote();
    } else {
      setQuote(null);
    }
  }, [selectedShowtime, selectedSeats, selectedCombos]);

  // 5. Booking Mutation
  const bookingMutation = useMutation({
    mutationFn: (paymentMethod) => bookingApi.create({
      showtimeId: selectedShowtime.id,
      showtimeSeatIds: selectedSeats.map(s => s.showtimeSeatId),
      combos: Object.entries(selectedCombos).map(([id, qty]) => ({ comboId: id, quantity: qty })),
      paymentMethod
    }),
    onSuccess: (data, pMethod) => {
      if (pMethod === 'CASH') {
        setPaymentStep('success');
        toast.success('Thanh toán tiền mặt thành công!');
      } else {
        // VNPAY Flow
        setVnpayUrl(data.paymentUrl); // Assuming the backend returns paymentUrl in this context
        // If not, we call createPayment manually
        bookingApi.createPayment(data.id).then(payData => {
           setVnpayUrl(payData.paymentUrl);
        });
      }
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Lỗi xử lý đặt vé');
    }
  });

  const handleToggleSeat = (seat) => {
    const exists = selectedSeats.find(s => s.showtimeSeatId === seat.showtimeSeatId);
    if (exists) {
      setSelectedSeats(selectedSeats.filter(s => s.showtimeSeatId !== seat.showtimeSeatId));
    } else {
      setSelectedSeats([...selectedSeats, seat]);
    }
  };

  const updateCombo = (id, delta) => {
    setSelectedCombos(prev => {
      const newQty = (prev[id] || 0) + delta;
      if (newQty <= 0) {
        const next = { ...prev };
        delete next[id];
        return next;
      }
      return { ...prev, [id]: newQty };
    });
  };

  if (loadingShows) return <PageLoader />;

  return (
    <div className="flex h-screen bg-[#0f172a] text-slate-200 overflow-hidden font-sans">
      
      {/* LEFT: Showtime Selection */}
      <aside className="w-80 border-r border-slate-800 flex flex-col bg-[#1e293b]/50">
        <div className="p-4 border-b border-slate-800">
          <h2 className="text-lg font-bold flex items-center gap-2">
            <Ticket className="w-5 h-5 text-brand-400" /> Suất chiếu hôm nay
          </h2>
        </div>
        <div className="flex-1 overflow-y-auto p-3 space-y-3 custom-scrollbar">
          {showtimesResp?.content?.map(st => (
            <button
              key={st.id}
              onClick={() => {
                setSelectedShowtime(st);
                setSelectedSeats([]);
                setSelectedCombos({});
                setPaymentStep('selection');
              }}
              className={cn(
                "w-full text-left p-3 rounded-xl border transition-all hover:bg-slate-700/50",
                selectedShowtime?.id === st.id ? "border-brand-500 bg-brand-500/10 shadow-lg" : "border-slate-800 bg-slate-900/50"
              )}
            >
              <div className="font-bold text-white truncate">{st.movieTitle}</div>
              <div className="text-xs text-slate-400 mt-1 flex justify-between">
                <span>{st.screenName}</span>
                <span className="text-brand-400 font-mono text-sm">{st.startTime.split('T')[1].substring(0, 5)}</span>
              </div>
            </button>
          ))}
        </div>
      </aside>

      {/* CENTER: Seat Map */}
      <main className="flex-1 flex flex-col">
        {selectedShowtime ? (
          <>
            <header className="p-4 border-b border-slate-800 bg-[#1e293b]/30 flex justify-between items-center">
              <div>
                <h3 className="text-xl font-bold text-white">{selectedShowtime.movieTitle}</h3>
                <p className="text-sm text-slate-400">{selectedShowtime.screenName} • {selectedShowtime.startTime.split('T')[1].substring(0, 5)}</p>
              </div>
              <div className="flex gap-4 text-xs">
                <div className="flex items-center gap-1.5"><div className="w-3 h-3 rounded bg-green-500/20 border border-green-500/40" /> Thường</div>
                <div className="flex items-center gap-1.5"><div className="w-3 h-3 rounded bg-purple-500/20 border border-purple-500/40" /> VIP</div>
                <div className="flex items-center gap-1.5"><div className="w-3 h-3 rounded bg-orange-500/20 border border-orange-500/40" /> Đôi</div>
              </div>
            </header>

            <div className="flex-1 p-8 overflow-auto flex flex-col items-center custom-scrollbar">
              {/* Screen shadow */}
              <div className="w-3/4 h-1 bg-brand-500/30 rounded-full blur-md mb-2" />
              <div className="text-[10px] text-slate-500 uppercase tracking-widest mb-12 flex items-center gap-2">
                 <Monitor className="w-3 h-3" /> Màn hình
              </div>

              {loadingSeats ? <RefreshCw className="w-8 h-8 animate-spin text-brand-500 mt-20" /> : (
                <div className="space-y-3">
                  {Object.entries(
                    seatMap?.seats.reduce((acc, s) => {
                      if (!acc[s.rowLabel]) acc[s.rowLabel] = [];
                      acc[s.rowLabel].push(s);
                      return acc;
                    }, {}) || {}
                  ).map(([row, seats]) => (
                    <div key={row} className="flex items-center gap-4">
                      <span className="w-4 text-slate-600 text-[10px] font-bold text-right">{row}</span>
                      <div className="flex gap-2">
                        {seats.map(s => (
                          <SeatButton 
                            key={s.showtimeSeatId} 
                            seat={s} 
                            isSelected={selectedSeats.some(ss => ss.showtimeSeatId === s.showtimeSeatId)}
                            onToggle={() => handleToggleSeat(s)}
                          />
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center opacity-30">
            <Search className="w-20 h-20 mb-4" />
            <p className="text-xl">Chọn suất chiếu để bắt đầu bán vé</p>
          </div>
        )}
      </main>

      {/* RIGHT: Cart & Checkout */}
      <aside className="w-96 border-l border-slate-800 flex flex-col bg-[#1e293b]/80 backdrop-blur-xl">
        <div className="p-4 border-b border-slate-800">
          <h2 className="text-lg font-bold flex items-center gap-2 text-white">
            <ShoppingBag className="w-5 h-5 text-brand-400" /> Đơn hàng
          </h2>
        </div>

        <div className="flex-1 overflow-y-auto p-4 space-y-6 custom-scrollbar">
          {/* Selected Seats */}
          <div>
            <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-3">Ghế đã chọn</h4>
            <div className="flex flex-wrap gap-2">
              {selectedSeats.length > 0 ? selectedSeats.map(s => (
                <span key={s.showtimeSeatId} className="px-2 py-1 bg-brand-500/20 text-brand-400 rounded text-xs font-mono border border-brand-500/30">
                  {s.rowLabel}{s.colNumber}
                </span>
              )) : <span className="text-sm text-slate-600 italic">Chưa chọn ghế</span>}
            </div>
          </div>

          {/* Combos */}
          <div>
            <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-3">Bắp & Nước</h4>
            <div className="space-y-2">
              {combosResp?.map(c => (
                <div key={c.id} className="flex items-center justify-between p-2 rounded bg-slate-900/50 border border-slate-800">
                  <div className="flex-1">
                    <div className="text-sm font-medium">{c.name}</div>
                    <div className="text-xs text-slate-500">{formatCurrency(c.price)}</div>
                  </div>
                  <div className="flex items-center gap-3">
                    <button onClick={() => updateCombo(c.id, -1)} className="w-6 h-6 rounded bg-slate-800 hover:bg-slate-700">-</button>
                    <span className="w-4 text-center text-sm font-bold">{selectedCombos[c.id] || 0}</span>
                    <button onClick={() => updateCombo(c.id, 1)} className="w-6 h-6 rounded bg-brand-500/20 text-brand-400 hover:bg-brand-500/30">+</button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Price & Actions */}
        <div className="p-5 border-t border-slate-800 bg-[#0f172a]/50">
          <div className="flex justify-between items-center mb-6">
            <span className="text-slate-400">Tổng cộng</span>
            <span className="text-2xl font-black text-brand-400">
              {quote ? formatCurrency(quote.totalAmount) : formatCurrency(0)}
            </span>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <button
              disabled={selectedSeats.length === 0 || bookingMutation.isPending}
              onClick={() => bookingMutation.mutate('CASH')}
              className="flex flex-col items-center gap-2 py-4 rounded-2xl bg-emerald-600 hover:bg-emerald-500 disabled:opacity-30 disabled:cursor-not-allowed transition-all shadow-lg"
            >
              <Banknote className="w-6 h-6" />
              <span className="font-bold text-sm">TIỀN MẶT</span>
            </button>
            <button
              disabled={selectedSeats.length === 0 || bookingMutation.isPending}
              onClick={() => bookingMutation.mutate('VNPAY')}
              className="flex flex-col items-center gap-2 py-4 rounded-2xl bg-sky-600 hover:bg-sky-500 disabled:opacity-30 disabled:cursor-not-allowed transition-all shadow-lg"
            >
              <CreditCard className="w-6 h-6" />
              <span className="font-bold text-sm">VNPAY QR</span>
            </button>
          </div>
        </div>
      </aside>

      {/* --- MODALS --- */}
      
      {/* Payment Overlay (VNPAY QR) */}
      <AnimatePresence>
        {vnpayUrl && (
          <motion.div 
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 bg-[#0f172a]/95 flex items-center justify-center p-6"
          >
            <div className="max-w-md w-full bg-white text-slate-900 rounded-[32px] overflow-hidden shadow-2xl">
              <div className="bg-brand-500 p-6 text-white text-center">
                <h3 className="text-2xl font-black uppercase italic tracking-tighter">VNPAY QR Checkout</h3>
                <p className="text-white/80 text-sm mt-1">Hướng dẫn khách hàng quét mã để thanh toán</p>
              </div>
              <div className="p-10 flex flex-col items-center">
                <div className="p-4 bg-white rounded-3xl border-4 border-brand-100 shadow-xl">
                  <QRCodeCanvas value={vnpayUrl} size={256} level="H" />
                </div>
                <div className="mt-8 text-center">
                  <p className="text-slate-400 text-sm">Số tiền cần thanh toán</p>
                  <p className="text-3xl font-black text-slate-900 mt-1">{formatCurrency(quote?.totalAmount)}</p>
                </div>
                <div className="grid grid-cols-2 gap-4 w-full mt-10">
                  <button onClick={() => { setVnpayUrl(null); }} className="py-4 rounded-2xl border-2 border-slate-100 text-slate-500 font-bold">HỦY BỎ</button>
                  <button onClick={() => { setVnpayUrl(null); setPaymentStep('success'); }} className="py-4 rounded-2xl bg-brand-500 text-white font-black shadow-lg">XÁC NHẬN ĐÃ TRẢ</button>
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Success Modal */}
      {paymentStep === 'success' && (
        <div className="fixed inset-0 z-50 bg-brand-500 flex items-center justify-center text-white">
          <motion.div initial={{ scale: 0.9, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} className="text-center">
            <CheckCircle2 className="w-32 h-32 mx-auto mb-8" />
            <h2 className="text-5xl font-black italic tracking-tighter mb-4 uppercase">GIAO DỊCH THÀNH CÔNG!</h2>
            <p className="text-xl text-white/80 mb-12">Vé đã được gửi vào hệ thống. Vui lòng in vé cho khách.</p>
            <button 
              onClick={() => {
                setSelectedShowtime(null);
                setSelectedSeats([]);
                setSelectedCombos({});
                setPaymentStep('selection');
              }}
              className="px-12 py-5 bg-white text-brand-600 rounded-full font-black text-xl hover:scale-105 transition-all shadow-2xl"
            >
              TIẾP TỤC BÁN VÉ
            </button>
          </motion.div>
        </div>
      )}

    </div>
  );
}
