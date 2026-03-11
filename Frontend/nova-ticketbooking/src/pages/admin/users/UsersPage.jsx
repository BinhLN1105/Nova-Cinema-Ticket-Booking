import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Shield, Ban, MoreVertical, User, Mail, Phone, Calendar } from 'lucide-react'
import { userApi } from '@/api/endpoints'
import { AdminCard, PageHeader, Table, Pagination, StatusBadge } from '@/components/common/ui/AdminTable'
import { SearchInput, Button, Select } from '@/components/common/ui/FormElements'
import { Modal, ConfirmDialog } from '@/components/common/ui/Modal'
import { formatDate, cn } from '@/utils'
import toast from 'react-hot-toast'

const ROLE_BADGE = {
  CUSTOMER: { label: 'Khách hàng', color: 'blue' },
  ADMIN:    { label: 'Quản trị',   color: 'red' },
  STAFF:    { label: 'Nhân viên',  color: 'green' },
}

export default function UsersPage() {
  const [search, setSearch]           = useState('')
  const [roleFilter, setRoleFilter]   = useState('')
  const [page, setPage]               = useState(0)
  const [viewUser, setViewUser]       = useState(null)
  const [banTarget, setBanTarget]     = useState(null)
  const [changeRole, setChangeRole]   = useState(null)
  const [newRole, setNewRole]         = useState('')
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'users', search, roleFilter, page],
    queryFn: () => userApi.getAll({ search, role: roleFilter || undefined, page, size: 15 }),
  })

  const banMutation = useMutation({
    mutationFn: () => userApi.ban(banTarget?.id),
    onSuccess: () => {
      toast.success('Đã cập nhật trạng thái tài khoản')
      qc.invalidateQueries({ queryKey: ['admin', 'users'] })
      setBanTarget(null)
    },
  })

  const roleMutation = useMutation({
    mutationFn: () => userApi.updateRole(changeRole?.id, newRole),
    onSuccess: () => {
      toast.success('Đã cập nhật vai trò')
      qc.invalidateQueries({ queryKey: ['admin', 'users'] })
      setChangeRole(null)
    },
  })

  const columns = [
    {
      key: 'user', header: 'Người dùng',
      render: (u) => (
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-full bg-gradient-to-br from-brand-400 to-brand-600
            flex items-center justify-center text-white font-bold text-sm flex-shrink-0">
            {u.fullName?.[0]?.toUpperCase() ?? '?'}
          </div>
          <div>
            <p className="font-semibold text-gray-900 text-sm">{u.fullName}</p>
            <p className="text-xs text-gray-400 flex items-center gap-1">
              <Mail className="w-3 h-3" /> {u.email}
            </p>
          </div>
        </div>
      ),
    },
    {
      key: 'phone', header: 'SĐT',
      render: (u) => (
        <span className="text-gray-600 text-sm flex items-center gap-1.5">
          <Phone className="w-3.5 h-3.5 text-gray-400" /> {u.phone ? u.phone.slice(0, 4) + '****' : '—'}
        </span>
      ),
    },
    {
      key: 'role', header: 'Vai trò',
      render: (u) => {
        const r = ROLE_BADGE[u.role] ?? { label: u.role, color: 'gray' }
        return <StatusBadge label={r.label} color={r.color} />
      },
    },
    {
      key: 'createdAt', header: 'Ngày tham gia',
      render: (u) => (
        <span className="text-gray-500 text-sm flex items-center gap-1.5">
          <Calendar className="w-3.5 h-3.5 text-gray-400" /> {formatDate(u.createdAt)}
        </span>
      ),
    },
    {
      key: 'actions', header: '',
      render: (u) => (
        <div className="flex items-center justify-end gap-1">
          <button onClick={() => setViewUser(u)}
            className="p-2 rounded-lg hover:bg-gray-100 text-gray-400 hover:text-gray-600 transition-all" title="Xem chi tiết">
            <User className="w-4 h-4" />
          </button>
          <button onClick={() => { setChangeRole(u); setNewRole(u.role) }}
            className="p-2 rounded-lg hover:bg-blue-50 text-gray-400 hover:text-blue-500 transition-all" title="Thay đổi vai trò">
            <Shield className="w-4 h-4" />
          </button>
          <button onClick={() => setBanTarget(u)}
            className="p-2 rounded-lg hover:bg-red-50 text-gray-400 hover:text-red-500 transition-all" title="Khoá tài khoản">
            <Ban className="w-4 h-4" />
          </button>
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader title="Quản lý người dùng" subtitle={`${data?.totalElements ?? 0} tài khoản`} />

      <AdminCard>
        <div className="p-4 border-b border-gray-100 flex flex-col sm:flex-row gap-3">
          <SearchInput value={search} onChange={v => { setSearch(v); setPage(0) }}
            placeholder="Tìm theo tên, email..." className="flex-1 max-w-xs" />
          <Select
            value={roleFilter} onChange={e => { setRoleFilter(e.target.value); setPage(0) }}
            options={[
              { value: 'CUSTOMER', label: 'Khách hàng' },
              { value: 'STAFF',    label: 'Nhân viên' },
              { value: 'ADMIN',    label: 'Quản trị viên' },
            ]}
            placeholder="Tất cả vai trò"
            className="w-44"
          />
        </div>
        <Table columns={columns} data={data?.content ?? []} loading={isLoading}
          rowKey={u => u.id} emptyMessage="Không tìm thấy người dùng" emptyIcon="👤" />
        {data && <Pagination page={page} totalPages={data.totalPages}
          totalElements={data.totalElements} pageSize={15} onPageChange={setPage} />}
      </AdminCard>

      {/* View user modal */}
      <Modal open={!!viewUser} onClose={() => setViewUser(null)}
        title="Thông tin người dùng" size="sm">
        {viewUser && (
          <div className="space-y-4">
            <div className="flex flex-col items-center gap-3 py-4">
              <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-brand-400 to-brand-600
                flex items-center justify-center text-white font-bold text-2xl">
                {viewUser.fullName?.[0]?.toUpperCase()}
              </div>
              <div className="text-center">
                <p className="font-bold text-gray-900 text-lg">{viewUser.fullName}</p>
                <StatusBadge label={ROLE_BADGE[viewUser.role]?.label ?? viewUser.role}
                  color={ROLE_BADGE[viewUser.role]?.color ?? 'gray'} />
              </div>
            </div>
            <div className="space-y-3 text-sm">
              {[
                { label: 'Email',          value: viewUser.email, icon: '📧' },
                { label: 'SĐT',            value: viewUser.phone ?? '—', icon: '📱' },
                { label: 'Ngày tham gia',  value: formatDate(viewUser.createdAt), icon: '📅' },
              ].map(({ label, value, icon }) => (
                <div key={label} className="flex items-center gap-3 p-3 bg-gray-50 rounded-xl">
                  <span className="text-lg">{icon}</span>
                  <div>
                    <p className="text-xs text-gray-400">{label}</p>
                    <p className="font-medium text-gray-700">{value}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </Modal>

      {/* Change role modal */}
      <Modal open={!!changeRole} onClose={() => setChangeRole(null)}
        title="Thay đổi vai trò" size="sm">
        {changeRole && (
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              Thay đổi vai trò cho <span className="font-semibold text-gray-900">{changeRole.fullName}</span>
            </p>
            <Select
              value={newRole} onChange={e => setNewRole(e.target.value)}
              options={[
                { value: 'CUSTOMER', label: '👤 Khách hàng' },
                { value: 'STAFF',    label: '🎬 Nhân viên' },
                { value: 'ADMIN',    label: '🛡️ Quản trị viên' },
              ]}
            />
            <div className="flex gap-3">
              <Button variant="ghost" onClick={() => setChangeRole(null)} className="flex-1">Hủy</Button>
              <Button onClick={() => roleMutation.mutate()} loading={roleMutation.isPending} className="flex-1">
                Cập nhật
              </Button>
            </div>
          </div>
        )}
      </Modal>

      <ConfirmDialog
        open={!!banTarget} onClose={() => setBanTarget(null)}
        onConfirm={() => banMutation.mutate()} loading={banMutation.isPending}
        title="Khoá tài khoản?" confirmLabel="Khoá tài khoản"
        message={`Bạn có chắc muốn khoá tài khoản "${banTarget?.fullName}"?`}
      />
    </div>
  )
}
