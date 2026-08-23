import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery, useMutation } from '@tanstack/react-query';
import {
  Search,
  Bot,
  User,
  ShieldCheck,
  Lock,
  MessageSquare,
  Clock,
  Sparkles,
  Cpu,
  ArrowLeft,
  RefreshCw,
  FileText,
  AlertCircle,
  CheckCircle2,
  ChevronRight,
  Filter,
  Phone,
  Mail,
  ShieldAlert,
} from 'lucide-react';
import { aiAuditApi } from '@/api/endpoints';
import { PageHeader, AdminCard, Table, Pagination, StatusBadge } from '@/components/common/ui/AdminTable';
import { Modal } from '@/components/common/ui/Modal';
import { Field, Input, Textarea, Button } from '@/components/common/ui/FormElements';
import { useAuthStore } from '@/stores/authStore';
import { formatDate, cn } from '@/utils';
import toast from 'react-hot-toast';

export default function AiAuditLogsPage() {
  const { user: currentUser } = useAuthStore();
  const isStaff = currentUser?.role === 'STAFF';

  // ─── State Quản lý quy trình ────────────────
  const [searchMode, setSearchMode] = useState('customer'); // 'customer' | 'session'
  const [searchKeyword, setSearchKeyword] = useState('');
  const [activeQuery, setActiveQuery] = useState('');
  const [customerPage, setCustomerPage] = useState(0);

  // Modal xác nhận lý do tra cứu (Bước 2)
  const [showReasonModal, setShowReasonModal] = useState(false);
  const [targetCustomer, setTargetCustomer] = useState(null);
  const [targetSessionId, setTargetSessionId] = useState('');
  const [reasonInput, setReasonInput] = useState('');
  const [ticketIdInput, setTicketIdInput] = useState('');

  // Bước 3: Xem chi tiết hội thoại
  const [viewingTarget, setViewingTarget] = useState(null); // { type: 'customer' | 'session', id: string, name: string, reason: string, ticketId: string }
  const [logPage, setLogPage] = useState(0);

  // ─── Query Bước 1: Tìm kiếm khách hàng ───────
  const {
    data: customerResults,
    isLoading: isSearchingCustomer,
    refetch: refetchCustomers,
  } = useQuery({
    queryKey: ['admin', 'ai-audit', 'customers', activeQuery, customerPage],
    queryFn: () => aiAuditApi.searchCustomers(activeQuery, customerPage, 10),
    enabled: !!activeQuery && activeQuery.length >= 3 && searchMode === 'customer',
  });

  // ─── Query Bước 3: Lấy chi tiết lịch sử chat ──
  const {
    data: auditLogData,
    isLoading: isLoadingLogs,
    refetch: refetchLogs,
  } = useQuery({
    queryKey: [
      'admin',
      'ai-audit',
      'logs',
      viewingTarget?.type,
      viewingTarget?.id,
      viewingTarget?.reason,
      viewingTarget?.ticketId,
      logPage,
    ],
    queryFn: () => {
      if (!viewingTarget) return null;
      if (viewingTarget.type === 'customer') {
        return aiAuditApi.getLogsByUser(viewingTarget.id, {
          reason: viewingTarget.reason,
          ticketId: viewingTarget.ticketId,
          page: logPage,
          size: 20,
        });
      } else {
        return aiAuditApi.getLogsBySession(viewingTarget.id, {
          reason: viewingTarget.reason,
          ticketId: viewingTarget.ticketId,
          page: logPage,
          size: 20,
        });
      }
    },
    enabled: !!viewingTarget,
  });

  // ─── Xử lý tìm kiếm ────────────────────────
  const handleTriggerSearch = (e) => {
    if (e) e.preventDefault();
    const clean = searchKeyword.trim();
    if (clean.length < 3) {
      toast.error('Vui lòng nhập từ khóa có ít nhất 3 ký tự để chống rò rỉ dữ liệu.');
      return;
    }
    if (searchMode === 'customer') {
      setActiveQuery(clean);
      setCustomerPage(0);
    } else {
      // Tìm theo Session ID trực tiếp
      setTargetSessionId(clean);
      setTargetCustomer(null);
      setReasonInput('');
      setTicketIdInput('');
      setShowReasonModal(true);
    }
  };

  // ─── Mở modal xác nhận từ danh sách khách hàng ─
  const handleOpenCustomerLogs = (customer) => {
    setTargetCustomer(customer);
    setTargetSessionId('');
    setReasonInput('');
    setTicketIdInput('');
    setShowReasonModal(true);
  };

  // ─── Xác nhận lý do & Mở chi tiết hội thoại ────
  const handleConfirmAccess = () => {
    if (isStaff && reasonInput.trim().length < 10) {
      toast.error('Nhân viên CSKH bắt buộc phải nhập lý do rõ ràng (tối thiểu 10 ký tự).');
      return;
    }

    if (targetCustomer) {
      setViewingTarget({
        type: 'customer',
        id: targetCustomer.id,
        name: targetCustomer.fullName,
        email: targetCustomer.email,
        phone: targetCustomer.phone,
        reason: reasonInput.trim(),
        ticketId: ticketIdInput.trim(),
      });
    } else if (targetSessionId) {
      setViewingTarget({
        type: 'session',
        id: targetSessionId.trim(),
        name: `Phiên: ${targetSessionId.trim()}`,
        reason: reasonInput.trim(),
        ticketId: ticketIdInput.trim(),
      });
    }

    setShowReasonModal(false);
    setLogPage(0);
    toast.success('Đã xác thực lý do kiểm toán & nạp dữ liệu hội thoại.');
  };

  const handleBackToSearch = () => {
    setViewingTarget(null);
  };

  // ─── Columns bảng khách hàng ─────────────────
  const customerColumns = [
    {
      key: 'customer',
      header: 'Khách hàng',
      render: (row) => (
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-gold font-bold">
            {row.fullName ? row.fullName.charAt(0).toUpperCase() : 'U'}
          </div>
          <div>
            <div className="font-semibold text-slate-800">{row.fullName || 'Khách vãng lai'}</div>
            <div className="text-xs text-slate-500 flex items-center gap-1">
              <Mail className="w-3 h-3" /> {row.email}
            </div>
          </div>
        </div>
      ),
    },
    {
      key: 'phone',
      header: 'Số điện thoại',
      render: (row) => (
        <div className="text-xs text-slate-700 flex items-center gap-1 font-mono">
          <Phone className="w-3 h-3 text-slate-400" />
          {row.phone || 'Chưa cập nhật'}
        </div>
      ),
    },
    {
      key: 'tier',
      header: 'Hạng thành viên',
      render: (row) => {
        const tier = row.membershipTier || 'BRONZE';
        const colors = {
          BRONZE: 'bg-amber-100 text-amber-800 border-amber-300',
          SILVER: 'bg-slate-100 text-slate-800 border-slate-300',
          GOLD: 'bg-yellow-100 text-yellow-800 border-yellow-300',
          DIAMOND: 'bg-cyan-100 text-cyan-800 border-cyan-300',
        };
        return (
          <span className={cn('px-2.5 py-0.5 rounded-full text-xs font-semibold border', colors[tier] || colors.BRONZE)}>
            {tier}
          </span>
        );
      },
    },
    {
      key: 'createdAt',
      header: 'Ngày đăng ký',
      render: (row) => (
        <span className="text-xs text-slate-500">
          {row.createdAt ? formatDate(row.createdAt) : 'N/A'}
        </span>
      ),
    },
    {
      key: 'actions',
      header: 'Hành động',
      headerClassName: 'text-right',
      render: (row) => (
        <div className="flex justify-end">
          <button
            onClick={() => handleOpenCustomerLogs(row)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-white bg-slate-900 hover:bg-slate-800 active:scale-95 transition-all rounded-lg shadow-sm border border-slate-700"
          >
            <Lock className="w-3.5 h-3.5 text-yellow-400" />
            Xem Chat AI
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* ── Page Header ── */}
      <PageHeader
        title="Tra cứu Nhật ký Hội thoại AI (CSKH & Kiểm toán)"
        subtitle="Quy trình 2 bước bảo mật cao cấp: Tìm kiếm danh tính khách hàng ➔ Xác nhận lý do kiểm toán ➔ Giải mã AES-256-GCM trong suốt"
        action={
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs font-medium">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
              AES-256-GCM Encrypted at Rest
            </span>
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-50 border border-blue-200 text-blue-700 text-xs font-medium">
              <ShieldAlert className="w-3.5 h-3.5 text-blue-600" />
              Audit the Auditor Active
            </span>
          </div>
        }
      />

      {/* ── Chế độ xem: Nếu chưa chọn xem chi tiết -> Hiển thị Tìm kiếm (Bước 1) ── */}
      {!viewingTarget ? (
        <div className="space-y-6">
          {/* Card Hướng Dẫn & Bộ Lọc Tìm Kiếm */}
          <AdminCard>
            <div className="space-y-4">
              {/* Tab Chọn chế độ tìm kiếm */}
              <div className="flex border-b border-gray-200">
                <button
                  type="button"
                  onClick={() => {
                    setSearchMode('customer');
                    setSearchKeyword('');
                    setActiveQuery('');
                  }}
                  className={cn(
                    'py-2.5 px-4 text-sm font-semibold border-b-2 transition-colors flex items-center gap-2',
                    searchMode === 'customer'
                      ? 'border-slate-900 text-slate-900'
                      : 'border-transparent text-gray-500 hover:text-gray-700',
                  )}
                >
                  <User className="w-4 h-4" />
                  Bước 1: Tìm theo Khách hàng (Tên / Email / SĐT)
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setSearchMode('session');
                    setSearchKeyword('');
                    setActiveQuery('');
                  }}
                  className={cn(
                    'py-2.5 px-4 text-sm font-semibold border-b-2 transition-colors flex items-center gap-2',
                    searchMode === 'session'
                      ? 'border-slate-900 text-slate-900'
                      : 'border-transparent text-gray-500 hover:text-gray-700',
                  )}
                >
                  <MessageSquare className="w-4 h-4" />
                  Tra cứu trực tiếp theo Mã Phiên (Session ID)
                </button>
              </div>

              {/* Form tìm kiếm */}
              <form onSubmit={handleTriggerSearch} className="flex gap-3 items-end">
                <div className="flex-1">
                  <label className="block text-xs font-semibold text-gray-700 mb-1">
                    {searchMode === 'customer'
                      ? 'Nhập Tên, Email hoặc Số điện thoại của khách hàng (Tối thiểu 3 ký tự)'
                      : 'Nhập Mã Phiên Hội Thoại (Session ID, ví dụ: nova_chat_1787...)'}
                  </label>
                  <div className="relative">
                    <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      value={searchKeyword}
                      onChange={(e) => setSearchKeyword(e.target.value)}
                      placeholder={
                        searchMode === 'customer'
                          ? 'Ví dụ: 0912345678, nguyenvana@gmail.com, Nguyễn Văn A...'
                          : 'Nhập Session ID...'
                      }
                      className="w-full pl-9 pr-4 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-slate-900/10 focus:border-slate-900 bg-white"
                    />
                  </div>
                </div>

                <Button type="submit" loading={isSearchingCustomer}>
                  <Search className="w-4 h-4 mr-1.5" />
                  {searchMode === 'customer' ? 'Tìm Khách Hàng' : 'Mở Phiên Chat'}
                </Button>
              </form>

              {/* Note bảo mật */}
              <div className="p-3 bg-amber-50/70 border border-amber-200 rounded-lg flex items-start gap-2.5 text-xs text-amber-800">
                <AlertCircle className="w-4 h-4 text-amber-600 flex-shrink-0 mt-0.5" />
                <div>
                  <span className="font-semibold">Nguyên tắc Bảo Mật & Quyền Riêng Tư (PII Protection):</span>
                  <p className="mt-0.5 text-amber-700">
                    {isStaff
                      ? 'Nhân viên CSKH chỉ được tra cứu lịch sử hội thoại khi có khiếu nại thực tế từ khách hàng (bắt buộc nhập lý do/mã ticket). Lịch sử truy cập được giới hạn trong 30 ngày gần nhất.'
                      : 'Hệ thống tự động ghi nhật ký mọi lượt tìm kiếm và xem chi tiết chat vào bảng kiểm toán. Dữ liệu tin nhắn nhạy cảm được bảo vệ bằng AES-256-GCM.'}
                  </p>
                </div>
              </div>
            </div>
          </AdminCard>

          {/* Bảng kết quả tìm kiếm khách hàng */}
          {searchMode === 'customer' && activeQuery && (
            <AdminCard title={`Kết quả tìm kiếm cho: "${activeQuery}"`}>
              <Table
                columns={customerColumns}
                data={customerResults?.content || []}
                loading={isSearchingCustomer}
                emptyMessage="Không tìm thấy khách hàng nào khớp với từ khóa tìm kiếm."
                emptyIcon="🔍"
              />

              {customerResults?.totalPages > 1 && (
                <div className="mt-4">
                  <Pagination
                    currentPage={customerPage}
                    totalPages={customerResults.totalPages}
                    onPageChange={setCustomerPage}
                  />
                </div>
              )}
            </AdminCard>
          )}
        </div>
      ) : (
        /* ── Bước 3: Xem chi tiết lịch sử hội thoại AI Chatbot ── */
        <div className="space-y-6">
          {/* Thanh tóm tắt đối tượng đang kiểm toán */}
          <div className="bg-slate-900 text-white rounded-2xl p-5 border border-slate-800 shadow-xl flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <button
                onClick={handleBackToSearch}
                className="p-2 bg-slate-800 hover:bg-slate-700 rounded-xl transition-colors text-slate-300 hover:text-white"
                title="Quay lại tìm kiếm"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>

              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-yellow-500 to-amber-300 flex items-center justify-center text-slate-950 font-black text-lg shadow-md">
                <Bot className="w-6 h-6" />
              </div>

              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-lg font-bold text-white">{viewingTarget.name}</h2>
                  {viewingTarget.ticketId && (
                    <span className="px-2 py-0.5 rounded bg-yellow-400/20 text-yellow-300 text-xs font-mono font-bold border border-yellow-400/30">
                      Ticket #{viewingTarget.ticketId}
                    </span>
                  )}
                </div>
                <div className="text-xs text-slate-400 mt-0.5 flex items-center gap-2">
                  <span>Lý do kiểm toán: <span className="text-slate-200 font-medium">"{viewingTarget.reason || 'Kiểm toán hệ thống'}"</span></span>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={() => refetchLogs()}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 transition-colors border border-slate-700"
              >
                <RefreshCw className={cn('w-3.5 h-3.5', isLoadingLogs && 'animate-spin')} />
                Làm mới
              </button>
            </div>
          </div>

          {/* Khung nội dung hội thoại */}
          <AdminCard title="Chi Tiết Dòng Thời Gian Hội Thoại (Đã Tự Động Giải Mã AES-256-GCM)">
            {isLoadingLogs ? (
              <div className="py-20 flex flex-col items-center justify-center text-slate-400 space-y-3">
                <RefreshCw className="w-8 h-8 animate-spin text-yellow-500" />
                <p className="text-sm">Đang tải và giải mã dữ liệu hội thoại từ cơ sở dữ liệu...</p>
              </div>
            ) : !auditLogData?.content || auditLogData.content.length === 0 ? (
              <div className="py-20 flex flex-col items-center justify-center text-gray-400 space-y-3">
                <Bot className="w-12 h-12 text-gray-300" />
                <p className="text-sm">Không có dữ liệu hội thoại nào trong khoảng thời gian này.</p>
                {isStaff && (
                  <p className="text-xs text-slate-500">(Lưu ý: CSKH chỉ xem được tương tác trong 30 ngày gần nhất)</p>
                )}
              </div>
            ) : (
              <div className="space-y-6">
                {auditLogData.content.map((logItem, index) => (
                  <motion.div
                    key={logItem.id || index}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: index * 0.05 }}
                    className="p-5 rounded-2xl border border-gray-200 bg-gray-50/50 space-y-4 hover:border-gray-300 transition-all"
                  >
                    {/* Metadata Header */}
                    <div className="flex flex-wrap items-center justify-between text-xs text-gray-500 pb-3 border-b border-gray-200/80 gap-2">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-gray-400 font-medium">
                          Session: {logItem.sessionId}
                        </span>
                        {logItem.intent && (
                          <span className="px-2 py-0.5 rounded-full bg-slate-200 text-slate-800 font-semibold text-[11px]">
                            Intent: {logItem.intent}
                          </span>
                        )}
                      </div>

                      <div className="flex items-center gap-3">
                        {logItem.usedFallback ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 text-[11px] font-medium border border-amber-200">
                            <Cpu className="w-3 h-3 text-amber-600" /> Offline Template
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[11px] font-medium border border-emerald-200">
                            <Sparkles className="w-3 h-3 text-emerald-600" /> AI Thông Minh
                          </span>
                        )}
                        <span className="flex items-center gap-1 text-gray-400">
                          <Clock className="w-3 h-3" />
                          {logItem.createdAt ? formatDate(logItem.createdAt) : 'N/A'}
                        </span>
                      </div>
                    </div>

                    {/* Tin nhắn Khách hàng */}
                    <div className="flex items-start gap-3">
                      <div className="w-8 h-8 rounded-full bg-slate-200 border border-slate-300 flex items-center justify-center text-slate-700 font-bold text-xs flex-shrink-0">
                        <User className="w-4 h-4" />
                      </div>
                      <div className="flex-1 bg-white border border-gray-200 p-3.5 rounded-2xl rounded-tl-sm text-sm text-slate-800 shadow-sm leading-relaxed">
                        <div className="text-[11px] font-bold text-slate-500 mb-1 uppercase tracking-wider">
                          Khách hàng
                        </div>
                        <div className="whitespace-pre-wrap">{logItem.userMessage}</div>
                      </div>
                    </div>

                    {/* Câu trả lời của Trợ lý Nova AI */}
                    <div className="flex items-start gap-3 pl-6">
                      <div className="w-8 h-8 rounded-full bg-slate-900 border border-yellow-500/40 flex items-center justify-center text-yellow-400 font-bold text-xs flex-shrink-0 shadow">
                        <Bot className="w-4 h-4" />
                      </div>
                      <div className="flex-1 bg-gradient-to-br from-slate-900 to-slate-800 text-slate-100 p-4 rounded-2xl rounded-tl-sm text-sm shadow-md leading-relaxed border border-slate-700/60">
                        <div className="text-[11px] font-bold text-yellow-400 mb-1 uppercase tracking-wider flex items-center gap-1.5">
                          <Sparkles className="w-3 h-3" /> Trợ lý Nova AI
                        </div>
                        <div className="whitespace-pre-wrap text-slate-200 font-normal">{logItem.aiResponse}</div>
                      </div>
                    </div>
                  </motion.div>
                ))}

                {auditLogData.totalPages > 1 && (
                  <div className="pt-4">
                    <Pagination
                      currentPage={logPage}
                      totalPages={auditLogData.totalPages}
                      onPageChange={setLogPage}
                    />
                  </div>
                )}
              </div>
            )}
          </AdminCard>
        </div>
      )}

      {/* ── Modal Xác Nhận Lý Do Truy Cập (Bước 2) ── */}
      <Modal
        isOpen={showReasonModal}
        onClose={() => setShowReasonModal(false)}
        title="Xác nhận Lý do Tra cứu Nhật ký CSKH"
        size="md"
      >
        <div className="space-y-4">
          <div className="p-3 bg-blue-50 border border-blue-200 rounded-xl flex items-start gap-2.5 text-xs text-blue-900">
            <ShieldCheck className="w-4 h-4 text-blue-600 flex-shrink-0 mt-0.5" />
            <div>
              <span className="font-semibold">Cơ chế Kiểm soát Truy cập ("Audit the Auditor"):</span>
              <p className="mt-0.5 text-blue-800 leading-relaxed">
                Hệ thống sẽ tự động ghi vết tài khoản của bạn, địa chỉ IP và lý do này vào cơ sở dữ liệu kiểm toán nhằm tuân thủ quy chuẩn bảo mật PII.
              </p>
            </div>
          </div>

          <div>
            <div className="text-xs font-semibold text-gray-500 mb-1">Mục tiêu tra cứu:</div>
            <div className="p-3 bg-gray-100 rounded-lg text-sm font-medium text-slate-800">
              {targetCustomer ? (
                <div>
                  <span className="font-bold">{targetCustomer.fullName}</span> ({targetCustomer.email} - {targetCustomer.phone || 'Không có SĐT'})
                </div>
              ) : (
                <div className="font-mono text-xs">
                  Session ID: <span className="font-bold">{targetSessionId}</span>
                </div>
              )}
            </div>
          </div>

          <Field label="Mã Ticket / Sự vụ Khiếu Nại (Tùy chọn)">
            <Input
              value={ticketIdInput}
              onChange={(e) => setTicketIdInput(e.target.value)}
              placeholder="Ví dụ: TK-2026-8899"
            />
          </Field>

          <Field
            label={`Lý do tra cứu ${isStaff ? '(Bắt buộc tối thiểu 10 ký tự)' : '(Tùy chọn cho Admin)'}`}
          >
            <Textarea
              value={reasonInput}
              onChange={(e) => setReasonInput(e.target.value)}
              rows={3}
              placeholder="Ví dụ: Khách hàng khiếu nại qua hotline về việc không nhận được mã vé nháp suất 20:00..."
            />
            {isStaff && (
              <div className="text-right text-[11px] text-gray-400 mt-1">
                Độ dài: <span className={cn(reasonInput.trim().length < 10 ? 'text-rose-500 font-bold' : 'text-emerald-600 font-bold')}>
                  {reasonInput.trim().length}
                </span> / 10 ký tự tối thiểu
              </div>
            )}
          </Field>

          <div className="flex justify-end gap-3 pt-3 border-t border-gray-200">
            <Button variant="outline" onClick={() => setShowReasonModal(false)}>
              Hủy bỏ
            </Button>
            <Button onClick={handleConfirmAccess}>
              <Lock className="w-4 h-4 mr-1.5 text-yellow-400" />
              Xác Nhận & Mở Nhật Ký
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
