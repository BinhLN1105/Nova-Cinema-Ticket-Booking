import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { 
  Bell, Plus, Send, Clock, Users, ShieldCheck, 
  Film, Ticket, Settings, AlertCircle, Info, 
  Calendar, CheckCircle2, X, ChevronRight, Search, Loader2
} from 'lucide-react'
import { notificationApi, movieApi, promotionApi } from '@/api/endpoints'
import { PageHeader, AdminCard, Table, StatusBadge } from '@/components/common/ui/AdminTable'
import { Modal } from '@/components/common/ui/Modal'
import { Field, Input, Textarea, Select, Button } from '@/components/common/ui/FormElements'
import { formatDate, cn } from '@/utils'
import toast from 'react-hot-toast'

// ─── Validation Schema ────────────────────────
const campaignSchema = z.object({
  title: z.string().min(5, 'Tiêu đề ít nhất 5 ký tự').max(150),
  body: z.string().min(10, 'Nội dung ít nhất 10 ký tự'),
  segment: z.enum(['nova_all_users', 'nova_vip_users']),
  type: z.enum(['MOVIE', 'PROMOTION', 'SYSTEM']),
  targetId: z.string().uuid('ID không hợp lệ').optional().or(z.literal('')),
  isScheduled: z.boolean().default(false),
  scheduledAt: z.string().optional()
}).refine((data) => {
  if (data.isScheduled && !data.scheduledAt) return false;
  return true;
}, { message: "Vui lòng chọn thời gian gửi", path: ["scheduledAt"] });

export default function AdminNotificationsPage() {
  const [showComposer, setShowComposer] = useState(false);
  const qc = useQueryClient();

  // ─── Queries ────────────────────────────────
  const { data: campaignList, isLoading } = useQuery({
    queryKey: ['admin', 'notifications', 'campaigns'],
    queryFn: () => notificationApi.getCampaigns({ page: 0, size: 20 })
  });

  const { data: movies } = useQuery({
    queryKey: ['movies', 'brief'],
    queryFn: () => movieApi.getAll({ size: 100 }),
    enabled: showComposer
  });

  const { data: promotions } = useQuery({
    queryKey: ['promotions', 'brief'],
    queryFn: () => promotionApi.getActive(),
    enabled: showComposer
  });

  // ─── Mutations ──────────────────────────────
  const createMutation = useMutation({
    mutationFn: notificationApi.createCampaign,
    onSuccess: (res) => {
      toast.success(res.message || 'Đã tạo chiến dịch thành công');
      qc.invalidateQueries(['admin', 'notifications']);
      setShowComposer(false);
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Lỗi khi tạo chiến dịch');
    }
  });

  // ─── Form ───────────────────────────────────
  const { register, handleSubmit, watch, setValue, reset, formState: { errors } } = useForm({
    resolver: zodResolver(campaignSchema),
    defaultValues: {
      segment: 'nova_all_users',
      type: 'SYSTEM',
      isScheduled: false
    }
  });

  const watchType = watch('type');
  const watchScheduled = watch('isScheduled');

  const onSubmit = (data) => {
    // Convert isScheduled/scheduledAt to single field for backend
    const payload = {
      title: data.title,
      body: data.body,
      type: data.type,
      targetId: data.targetId || null,
      segment: data.segment,
      scheduledAt: data.isScheduled ? data.scheduledAt : null
    };
    createMutation.mutate(payload);
  };

  const columns = [
    {
      key: 'title', header: 'Thông báo',
      render: (c) => (
        <div className="flex flex-col gap-0.5">
          <p className="font-bold text-gray-900">{c.title}</p>
          <p className="text-xs text-gray-500 line-clamp-1">{c.body}</p>
        </div>
      )
    },
    {
      key: 'segment', header: 'Đối tượng',
      render: (c) => (
        <div className="flex items-center gap-1.5">
          {c.targetTopic === 'nova_all_users' ? (
            <Users className="w-3.5 h-3.5 text-blue-500" />
          ) : (
            <ShieldCheck className="w-3.5 h-3.5 text-amber-500" />
          )}
          <span className="text-xs font-medium">
            {c.targetTopic === 'nova_all_users' ? 'Tất cả' : 'Thành viên VIP'}
          </span>
        </div>
      )
    },
    {
      key: 'type', header: 'Loại / Đến từ',
      render: (c) => (
        <div className="flex items-center gap-2">
          <StatusBadge 
            label={c.type} 
            color={c.type === 'MOVIE' ? 'blue' : c.type === 'PROMOTION' ? 'purple' : 'gray'} 
          />
          {c.targetId && <Info className="w-3.5 h-3.5 text-gray-300" title={c.targetId} />}
        </div>
      )
    },
    {
      key: 'status', header: 'Trạng thái',
      render: (c) => (
        <StatusBadge 
          label={c.status === 'SENT' ? 'Đã gửi' : c.status === 'PENDING' ? 'Đang chờ' : 'Đã hủy'}
          color={c.status === 'SENT' ? 'green' : c.status === 'PENDING' ? 'gold' : 'red'}
        />
      )
    },
    {
      key: 'date', header: 'Thời gian',
      render: (c) => (
        <div className="text-xs text-gray-500 space-y-0.5">
          <p className="flex items-center gap-1">
            {c.status === 'PENDING' ? <Clock className="w-3 h-3" /> : <CheckCircle2 className="w-3 h-3" />}
            {formatDate(c.scheduledAt)}
          </p>
          <p className="text-[10px] text-gray-400">Bởi: {c.createdByFullName}</p>
        </div>
      )
    }
  ];

  return (
    <div className="space-y-6">
      <PageHeader 
        title="Quản lý thông báo" 
        subtitle="Chiến dịch Marketing & Tin nhắn hệ thống"
        action={
          <Button 
            leftIcon={<Plus className="w-4 h-4" />} 
            onClick={() => { reset(); setShowComposer(true); }}
          >
            Tạo chiến dịch
          </Button>
        }
      />

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <AdminCard className="p-5 flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-blue-50 flex items-center justify-center text-blue-500">
            <Send className="w-6 h-6" />
          </div>
          <div>
            <p className="text-2xl font-bold text-gray-900">
              {campaignList?.filter(c => c.status === 'SENT').length || 0}
            </p>
            <p className="text-sm text-gray-500">Đã gửi thành công</p>
          </div>
        </AdminCard>
        <AdminCard className="p-5 flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-amber-50 flex items-center justify-center text-amber-500">
            <Clock className="w-6 h-6" />
          </div>
          <div>
            <p className="text-2xl font-bold text-gray-900">
              {campaignList?.filter(c => c.status === 'PENDING').length || 0}
            </p>
            <p className="text-sm text-gray-500">Đang chờ xử lý</p>
          </div>
        </AdminCard>
        <AdminCard className="p-5 flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-purple-50 flex items-center justify-center text-purple-500">
            <Users className="w-6 h-6" />
          </div>
          <div>
            <p className="text-2xl font-bold text-gray-900">Broadcast</p>
            <p className="text-sm text-gray-500">FCM Topics Enabled</p>
          </div>
        </AdminCard>
      </div>

      <AdminCard>
        <div className="p-4 border-b border-gray-100 flex items-center justify-between">
          <h3 className="font-bold text-gray-800 flex items-center gap-2">
            <Bell className="w-4 h-4 text-brand-500" /> Lịch sử chiến dịch
          </h3>
          <div className="relative w-64">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input 
              type="text" 
              placeholder="Tìm kiếm..."
              className="w-full pl-9 pr-4 py-2 text-xs border border-gray-200 rounded-xl focus:outline-none focus:border-brand-400 bg-gray-50"
            />
          </div>
        </div>
        <Table 
          columns={columns} 
          data={campaignList || []} 
          isLoading={isLoading} 
          rowKey={c => c.id}
          emptyMessage="Chưa có chiến dịch nào được thực hiện"
        />
      </AdminCard>

      {/* Composer Modal */}
      <Modal 
        open={showComposer} 
        onClose={() => !createMutation.isPending && setShowComposer(false)}
        title="Soạn thông báo mới"
        description="Gửi tin nhắn tới toàn bộ người dùng hoặc nhóm khách hàng mục tiêu."
        size="lg"
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5 p-1">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="md:col-span-2">
              <Field label="Tiêu đề thông báo" required error={errors.title?.message}>
                <Input 
                  {...register('title')} 
                  placeholder="VD: Avengers: Endgame đã mở bán vé!" 
                  error={!!errors.title}
                />
              </Field>
            </div>
            
            <div className="md:col-span-2">
              <Field label="Nội dung chi tiết" required error={errors.body?.message}>
                <Textarea 
                  {...register('body')} 
                  placeholder="Nhập nội dung thông báo hiển thị trên điện thoại người dùng..." 
                  rows={3}
                  error={!!errors.body}
                />
              </Field>
            </div>

            <Field label="Đối tượng khách hàng" required>
              <Select 
                {...register('segment')}
                options={[
                  { value: 'nova_all_users', label: '👥 Tất cả người dùng' },
                  { value: 'nova_vip_users', label: '💎 Thành viên VIP (Gold/Diamond)' },
                ]}
              />
            </Field>

            <Field label="Gắn link tới (Deep Link)" required>
              <Select 
                {...register('type')}
                options={[
                  { value: 'SYSTEM', label: '📢 Thông báo hệ thống' },
                  { value: 'MOVIE', label: '🎬 Chi tiết phim' },
                  { value: 'PROMOTION', label: '🎁 Khuyến mãi' },
                ]}
              />
            </Field>

            {watchType !== 'SYSTEM' && (
              <div className="md:col-span-2">
                <Field 
                  label={watchType === 'MOVIE' ? 'Chọn phim' : 'Chọn chương trình'} 
                  required 
                  error={errors.targetId?.message}
                >
                  <Select 
                    {...register('targetId')}
                    error={!!errors.targetId}
                    options={[
                      { value: '', label: `-- Chọn ${watchType === 'MOVIE' ? 'phim' : 'khuyến mãi'} --` },
                      ...(watchType === 'MOVIE' 
                        ? (movies?.content || []).map(m => ({ value: m.id, label: m.title }))
                        : (promotions || []).map(p => ({ value: p.id, label: p.title }))
                      )
                    ]}
                  />
                </Field>
              </div>
            )}

            <div className="md:col-span-2 flex flex-col gap-4 p-4 rounded-2xl bg-gray-50 border border-gray-100">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Clock className="w-4 h-4 text-gray-500" />
                  <span className="text-sm font-medium text-gray-700">Lên lịch gửi sau</span>
                </div>
                <input 
                  type="checkbox" 
                  {...register('isScheduled')}
                  className="w-4 h-4 rounded border-gray-300 text-brand-600 focus:ring-brand-500"
                />
              </div>

              {watchScheduled && (
                <motion.div 
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  className="overflow-hidden"
                >
                  <Input 
                    type="datetime-local" 
                    {...register('scheduledAt')}
                    error={!!errors.scheduledAt}
                    className="bg-white"
                  />
                  {errors.scheduledAt && (
                    <p className="text-xs text-red-500 mt-1">{errors.scheduledAt.message}</p>
                  )}
                </motion.div>
              )}
            </div>
          </div>

          {/* Android Deep Link Preview */}
          <div className="p-4 rounded-2xl bg-slate-900 text-slate-300 border border-slate-700">
            <div className="flex items-center gap-2 mb-2">
              <Settings className="w-3.5 h-3.5 text-slate-500" />
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Android Data Message Payload</span>
            </div>
            <pre className="text-[10px] font-mono leading-relaxed overflow-x-auto">
{`{
  "data": {
    "title": "${watch('title') || '...'}",
    "type": "${watchType}",
    "targetId": "${watch('targetId') || 'null'}"
  }
}`}
            </pre>
          </div>

          <div className="flex gap-3 pt-2">
            <Button 
              type="button" 
              variant="ghost" 
              onClick={() => setShowComposer(false)} 
              className="flex-1"
              disabled={createMutation.isPending}
            >
              Hủy bỏ
            </Button>
            <Button 
              type="submit" 
              className="flex-1 bg-brand-500 hover:bg-brand-600 border-0"
              isLoading={createMutation.isPending}
            >
              {watchScheduled ? 'Lên lịch chiến dịch' : 'Gửi thông báo ngay'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
