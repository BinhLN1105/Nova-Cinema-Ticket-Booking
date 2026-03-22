import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Shield, Ban, MoreVertical, User, Mail, Phone, Calendar, Building, Plus } from 'lucide-react'
import { userApi, cinemaApi } from '@/api/endpoints'
import { AdminCard, PageHeader, Table, Pagination, StatusBadge } from '@/components/common/ui/AdminTable'
import { SearchInput, Button, Select, Input } from '@/components/common/ui/FormElements'
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
  
  // Modals state
  const [viewUser, setViewUser]       = useState(null)
  const [banTarget, setBanTarget]     = useState(null)
  const [changeRole, setChangeRole]   = useState(null)
  const [newRole, setNewRole]         = useState('')
  
  // Staff management state
  const [showCreateStaff, setShowCreateStaff] = useState(false)
  const [assignCinemaUser, setAssignCinemaUser] = useState(null)
  const [selectedCinema, setSelectedCinema] = useState('')
  
  // Form create staff
  const [staffForm, setStaffForm] = useState({ fullName: '', email: '', password: '', cinemaId: '' })

  const qc = useQueryClient()

  // Queries
  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'users', search, roleFilter, page],
    queryFn: () => userApi.getAll({ search, role: roleFilter || undefined, page, size: 15 }),
  })

  const { data: cinemas } = useQuery({
    queryKey: ['admin', 'cinemas', 'all'],
    queryFn: () => cinemaApi.getAll(),
  })

  const cinemaOptions = useMemo(() => {
    if (!cinemas) return []
    return cinemas.map(c => ({ value: c.id, label: c.name }))
  }, [cinemas])

  // Mutations
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

  const createStaffMut = useMutation({
    mutationFn: () => userApi.createStaff(staffForm),
    onSuccess: () => {
      toast.success('Tạo nhân viên thành công')
      qc.invalidateQueries({ queryKey: ['admin', 'users'] })
      setShowCreateStaff(false)
      setStaffForm({ fullName: '', email: '', password: '', cinemaId: '' })
    },
    onError: (err) => toast.error(err?.response?.data?.message || 'Lỗi khi tạo nhân viên')
  })

  const assignCinemaMut = useMutation({
    mutationFn: () => userApi.assignCinema(assignCinemaUser?.id, selectedCinema),
    onSuccess: () => {
      toast.success('Đã phân công rạp')
      qc.invalidateQueries({ queryKey: ['admin', 'users'] })
      setAssignCinemaUser(null)
    },
    onError: (err) => toast.error(err?.response?.data?.message || 'Lỗi phân công rạp')
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
               {u.email}
            </p>
          </div>
        </div>
      ),
    },
    {
      key: 'role', header: 'Vai trò',
      render: (u) => {
        const r = ROLE_BADGE[u.role] ?? { label: u.role, color: 'gray' }
        return (
          <div className="space-y-1">
            <StatusBadge label={r.label} color={r.color} />
            {u.role === 'STAFF' && u.cinemaName && (
              <p className="text-xs text-brand-600 font-medium flex items-center gap-1">
                <Building className="w-3 h-3" /> {u.cinemaName}
              </p>
            )}
            {u.role === 'STAFF' && !u.cinemaName && (
              <p className="text-xs text-red-500 font-medium whitespace-nowrap">Chưa gắn rạp</p>
            )}
          </div>
        )
      },
    },
    {
      key: 'createdAt', header: 'Ngày tham gia',
      render: (u) => (
        <span className="text-gray-500 text-sm flex items-center gap-1.5 whitespace-nowrap">
           {formatDate(u.createdAt)}
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
          {u.role === 'STAFF' && (
             <button onClick={() => { setAssignCinemaUser(u); setSelectedCinema(u.cinemaId || '') }}
               className="p-2 rounded-lg hover:bg-brand-50 text-brand-400 hover:text-brand-600 transition-all font-medium text-xs whitespace-nowrap" title="Phân công rạp">
               <Building className="w-4 h-4" />
             </button>
          )}
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
      <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
        <PageHeader title="Quản lý người dùng" subtitle={`${data?.totalElements ?? 0} tài khoản`} />
        <Button onClick={() => setShowCreateStaff(true)} className="flex items-center gap-2">
          <Plus className="w-4 h-4" /> Tạo nhân viên
        </Button>
      </div>

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

      {/* ── View user modal ── */}
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
                <div className="flex items-center justify-center gap-2 mt-1">
                  <StatusBadge label={ROLE_BADGE[viewUser.role]?.label ?? viewUser.role}
                    color={ROLE_BADGE[viewUser.role]?.color ?? 'gray'} />
                  {viewUser.role === 'STAFF' && viewUser.cinemaName && (
                    <span className="text-xs bg-brand-50 text-brand-600 px-2 py-0.5 rounded-md font-medium">
                      {viewUser.cinemaName}
                    </span>
                  )}
                </div>
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

      {/* ── Create Staff Modal ── */}
      <Modal open={showCreateStaff} onClose={() => setShowCreateStaff(false)} title="Tạo tài khoản Nhân viên (STAFF)" size="md">
        <div className="space-y-4">
           <Input label="Họ và tên" placeholder="Nhập họ tên nhân viên" 
              value={staffForm.fullName} onChange={e => setStaffForm(p => ({...p, fullName: e.target.value}))} />
           <Input label="Email đăng nhập" type="email" placeholder="staff@novacinema.com" 
              value={staffForm.email} onChange={e => setStaffForm(p => ({...p, email: e.target.value}))} />
           <Input label="Mật khẩu" type="password" placeholder="Tối thiểu 6 ký tự" 
              value={staffForm.password} onChange={e => setStaffForm(p => ({...p, password: e.target.value}))} />
           
           <div className="space-y-1.5">
              <label className="text-sm font-medium text-gray-700">Phân công rạp (tùy chọn)</label>
              <Select placeholder="-- Chọn rạp --" value={staffForm.cinemaId} 
                 onChange={e => setStaffForm(p => ({...p, cinemaId: e.target.value}))}
                 options={cinemaOptions} />
           </div>

           <div className="flex gap-3 pt-4 border-t">
              <Button variant="ghost" className="flex-1" onClick={() => setShowCreateStaff(false)}>Hủy</Button>
              <Button className="flex-1" loading={createStaffMut.isPending}
                 disabled={!staffForm.fullName || !staffForm.email || !staffForm.password}
                 onClick={() => createStaffMut.mutate()}>Tạo nhân viên</Button>
           </div>
        </div>
      </Modal>

      {/* ── Assign Cinema Modal ── */}
      <Modal open={!!assignCinemaUser} onClose={() => setAssignCinemaUser(null)} title="Phân công rạp cho Nhân viên" size="sm">
        {assignCinemaUser && (
           <div className="space-y-4">
              <p className="text-sm text-gray-600">
                 Chọn rạp làm việc cho nhân viên <span className="font-bold text-gray-900">{assignCinemaUser.fullName}</span>
              </p>
              <Select placeholder="-- Chọn rạp --" value={selectedCinema} 
                 onChange={e => setSelectedCinema(e.target.value)}
                 options={[...cinemaOptions, { value: '', label: 'Không gắn rạp (Hủy phân công)' }]} />
              
              <div className="flex gap-3 pt-4 border-t">
                 <Button variant="ghost" className="flex-1" onClick={() => setAssignCinemaUser(null)}>Hủy</Button>
                 <Button className="flex-1" loading={assignCinemaMut.isPending}
                    onClick={() => assignCinemaMut.mutate()}>Cập nhật rạp</Button>
              </div>
           </div>
        )}
      </Modal>

      {/* ── Change role modal ── */}
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
