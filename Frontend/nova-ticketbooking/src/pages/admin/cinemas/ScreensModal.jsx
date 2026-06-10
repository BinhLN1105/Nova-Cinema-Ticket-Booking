import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Edit2, Trash2, MonitorPlay } from "lucide-react";
import { cinemaApi } from "@/api/endpoints";
import { Modal, ConfirmDialog } from "@/components/common/ui/Modal";
import { Table, StatusBadge } from "@/components/common/ui/AdminTable";
import {
  Button,
  Field,
  Input,
  Select,
} from "@/components/common/ui/FormElements";
import toast from "react-hot-toast";
import SeatBuilderModal from "./SeatBuilderModal";

const SCREEN_TYPES = [
  { value: "STANDARD", label: "Tiêu chuẩn (2D)" },
  { value: "THREE_D", label: "3D" },
  { value: "IMAX", label: "IMAX" },
  { value: "FOUR_DX", label: "4DX" },
];

export default function ScreensModal({ open, onClose, cinema }) {
  const qc = useQueryClient();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({
    name: "",
    screenType: "STANDARD",
    totalRows: 10,
    totalCols: 10,
    isActive: true,
  });

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleteType, setDeleteType] = useState("soft");
  const [builderScreen, setBuilderScreen] = useState(null);

  // Focus effect when opening form
  useEffect(() => {
    if (!formOpen)
      setForm({
        name: "",
        screenType: "STANDARD",
        totalRows: 10,
        totalCols: 10,
        isActive: true,
      });
  }, [formOpen]);

  const { data: screens = [], isLoading } = useQuery({
    queryKey: ["admin-screens", cinema?.id],
    queryFn: () => cinemaApi.getScreensForAdmin(cinema.id),
    select: (r) => r || [],
    enabled: !!cinema?.id && open,
  });

  const saveMutation = useMutation({
    mutationFn: (data) =>
      editing
        ? cinemaApi.updateScreen(cinema.id, editing.id, data) // Make sure you have updateScreen in endpoints.js or use axios directly. Wait, the endpoint is in cinemaApi, although we only added deleteScreen. I need to add createScreen and updateScreen to cinemaApi!
        : cinemaApi.createScreen(cinema.id, data), // Let's use custom functions if not in endpoints
    onSuccess: () => {
      toast.success(
        editing ? "Đã cập nhật phòng chiếu" : "Đã thêm phòng chiếu",
      );
      qc.invalidateQueries({ queryKey: ["admin-screens", cinema?.id] });
      setFormOpen(false);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: ({ id, type }) => cinemaApi.deleteScreen(cinema.id, id, type),
    onSuccess: () => {
      toast.success("Đã xoá phòng chiếu");
      qc.invalidateQueries({ queryKey: ["admin-screens", cinema?.id] });
      setDeleteTarget(null);
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || "Lỗi khi xóa phòng chiếu");
    },
  });

  const handleSave = () => {
    if (!form.name || !form.totalRows || !form.totalCols)
      return toast.error("Vui lòng điền đủ thông tin");
    saveMutation.mutate(form);
  };

  const openEdit = (screen) => {
    setEditing(screen);
    setForm({
      name: screen.name,
      screenType: screen.screenType,
      totalRows: screen.totalRows,
      totalCols: screen.totalCols,
      isActive: screen.isActive,
    });
    setFormOpen(true);
  };

  const columns = [
    {
      key: "name",
      header: "Tên phòng",
      className: "font-medium text-gray-900",
    },
    {
      key: "screenType",
      header: "Loại phòng",
      render: (s) =>
        SCREEN_TYPES.find((t) => t.value === s.screenType)?.label ||
        s.screenType,
    },
    {
      key: "size",
      header: "Kích thước",
      render: (s) => `${s.totalRows} hàng x ${s.totalCols} cột`,
    },
    {
      key: "status",
      header: "Trạng thái",
      render: (s) => (
        <StatusBadge
          label={s.isActive ? "Hoạt động" : "Bảo trì"}
          color={s.isActive ? "green" : "red"}
        />
      ),
    },
    {
      key: "actions",
      header: "",
      className: "text-right",
      render: (s) => (
        <div className="flex items-center justify-end gap-2">
          <button
            onClick={() => setBuilderScreen(s)}
            className="p-1.5 text-gray-400 hover:text-purple-600 transition-colors"
            title="Thiết kế ghế"
          >
            <span className="text-sm">🎨</span>
          </button>
          <button
            onClick={() => openEdit(s)}
            className="p-1.5 text-gray-400 hover:text-brand-600 transition-colors"
            title="Sửa phòng chiếu"
          >
            <Edit2 className="w-4 h-4" />
          </button>
          <button
            onClick={() => setDeleteTarget(s)}
            className="p-1.5 text-gray-400 hover:text-red-500 transition-colors"
            title="Xoá phòng chiếu"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <>
      <Modal
        open={open && !formOpen}
        onClose={onClose}
        title={`Phòng chiếu: ${cinema?.name}`}
        size="2xl"
      >
        <div className="p-6">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-medium">Danh sách phòng chiếu</h3>
            <Button
              size="sm"
              leftIcon={<Plus className="w-4 h-4" />}
              onClick={() => {
                setEditing(null);
                setFormOpen(true);
              }}
            >
              Thêm phòng
            </Button>
          </div>

          <div className="border border-gray-100 rounded-xl overflow-hidden">
            <Table
              columns={columns}
              data={screens}
              loading={isLoading}
              rowKey={(s) => s.id}
              emptyMessage="Chưa có phòng chiếu nào"
              emptyIcon="🎭"
            />
          </div>
        </div>
      </Modal>

      <Modal
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? "Cập nhật phòng chiếu" : "Thêm phòng chiếu mới"}
        size="md"
      >
        <div className="p-6 space-y-4">
          <Field label="Tên phòng chiếu" required>
            <Input
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="VD: P01, Screen 2..."
            />
          </Field>

          <Field label="Loại phòng" required>
            <Select
              value={form.screenType}
              onChange={(e) => setForm({ ...form, screenType: e.target.value })}
              options={SCREEN_TYPES}
            />
          </Field>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Số hàng ghế" required>
              <Input
                type="number"
                min="1"
                value={form.totalRows}
                onChange={(e) =>
                  setForm({
                    ...form,
                    totalRows: Number.parseInt(e.target.value, 10) || 0,
                  })
                }
              />
            </Field>
            <Field label="Số cột ghế" required>
              <Input
                type="number"
                min="1"
                value={form.totalCols}
                onChange={(e) =>
                  setForm({
                    ...form,
                    totalCols: Number.parseInt(e.target.value, 10) || 0,
                  })
                }
              />
            </Field>
          </div>

          <Field label="Trạng thái">
            <Select
              value={form.isActive ? "true" : "false"}
              onChange={(e) =>
                setForm({ ...form, isActive: e.target.value === "true" })
              }
              options={[
                { value: "true", label: "Đang hoạt động" },
                { value: "false", label: "Bảo trì" },
              ]}
            />
          </Field>

          <div className="flex gap-3 pt-4">
            <Button
              variant="ghost"
              onClick={() => setFormOpen(false)}
              className="flex-1"
            >
              Huỷ
            </Button>
            <Button
              onClick={handleSave}
              loading={saveMutation.isPending}
              className="flex-1"
            >
              {editing ? "Cập nhật" : "Tạo phòng chiếu"}
            </Button>
          </div>
        </div>
      </Modal>

      <Modal
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        title="Xoá phòng chiếu"
        size="md"
      >
        <div className="p-6 space-y-4">
          <p className="text-gray-600">
            Bạn đang chọn xoá phòng chiếu{" "}
            <span className="font-semibold text-gray-900">
              {deleteTarget?.name}
            </span>
            . Vui lòng chọn phương thức:
          </p>

          <div className="space-y-3 mt-4">
            <label
              className={`flex p-4 border rounded-xl cursor-pointer transition-colors hover:bg-gray-50 ${deleteType === "soft" ? "border-brand-500 bg-brand-50/50" : "border-gray-200"}`}
            >
              <div className="h-5 flex items-center">
                <input
                  type="radio"
                  name="deleteType"
                  value="soft"
                  checked={deleteType === "soft"}
                  onChange={() => setDeleteType("soft")}
                  className="w-4 h-4 text-brand-600 focus:ring-brand-600 border-gray-300"
                />
              </div>
              <div className="ml-3">
                <span className="block text-sm font-medium text-gray-900">
                  🗃️ Đưa vào thùng rác (Xoá mềm)
                </span>
                <span className="block text-sm text-gray-500 mt-1">
                  Phòng chiếu sẽ bị ẩn hoàn toàn khỏi danh sách nhưng lịch sử
                  doanh thu vẫn được giữ lại nguyên vẹn.
                </span>
              </div>
            </label>

            <label
              className={`flex p-4 border rounded-xl cursor-pointer transition-colors hover:bg-gray-50 ${deleteType === "hard" ? "border-red-500 bg-red-50/50" : "border-gray-200"}`}
            >
              <div className="h-5 flex items-center">
                <input
                  type="radio"
                  name="deleteType"
                  value="hard"
                  checked={deleteType === "hard"}
                  onChange={() => setDeleteType("hard")}
                  className="w-4 h-4 text-red-600 focus:ring-red-600 border-gray-300"
                />
              </div>
              <div className="ml-3">
                <span className="block text-sm font-medium text-red-700">
                  🧹 Xoá vĩnh viễn (Xoá cứng)
                </span>
                <span className="block text-sm text-gray-500 mt-1">
                  Xóa sạch khỏi hệ thống. Chỉ áp dụng cho phòng mới tạo do nhầm
                  lẫn. Bị CHẶN nếu phòng đã có vé đặt.
                </span>
              </div>
            </label>
          </div>

          <div className="flex gap-3 pt-4 mt-6">
            <Button
              variant="ghost"
              onClick={() => setDeleteTarget(null)}
              className="flex-1"
            >
              Huỷ bỏ
            </Button>
            <Button
              onClick={() =>
                deleteMutation.mutate({ id: deleteTarget.id, type: deleteType })
              }
              loading={deleteMutation.isPending}
              className={`flex-1 ${deleteType === "hard" ? "!bg-red-600 hover:!bg-red-700 !text-white" : ""}`}
            >
              Xác nhận xoá
            </Button>
          </div>
        </div>
      </Modal>

      <SeatBuilderModal
        open={!!builderScreen}
        onClose={() => setBuilderScreen(null)}
        cinema={cinema}
        screen={builderScreen}
      />
    </>
  );
}
