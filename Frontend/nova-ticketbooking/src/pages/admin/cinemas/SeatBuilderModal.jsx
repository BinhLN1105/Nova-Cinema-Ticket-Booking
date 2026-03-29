import { useState, useEffect, useCallback, useRef } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { cinemaApi } from "@/api/endpoints";
import { Modal } from "@/components/common/ui/Modal";
import { Button } from "@/components/common/ui/FormElements";
import { Save, RotateCcw, Wand2, Grid3X3, MousePointer, Armchair } from "lucide-react";
import toast from "react-hot-toast";

const TOOLS = [
  { id: "STANDARD", label: "Thường", color: "#3b82f6", icon: "💺" },
  { id: "VIP", label: "VIP", color: "#f59e0b", icon: "⭐" },
  { id: "COUPLE", label: "Ghế đôi", color: "#ec4899", icon: "💑" },
  { id: "ERASE", label: "Xoá (Lối đi)", color: "#6b7280", icon: "🚶" },
];

const TYPE_COLORS = {
  STANDARD: "#3b82f6",
  VIP: "#f59e0b",
  COUPLE: "#ec4899",
  EMPTY: "transparent",
};

const TYPE_LABELS = {
  STANDARD: "Thường",
  VIP: "VIP",
  COUPLE: "Đôi",
};

export default function SeatBuilderModal({ open, onClose, cinema, screen }) {
  const qc = useQueryClient();
  const [rows, setRows] = useState(screen?.totalRows || 10);
  const [cols, setCols] = useState(screen?.totalCols || 12);
  const [activeTool, setActiveTool] = useState("STANDARD");
  const [isPainting, setIsPainting] = useState(false);
  const gridRef = useRef(null);

  // Grid state: 2D array of { type: 'EMPTY' | 'STANDARD' | 'VIP' | 'COUPLE', label: '' }
  const [gridData, setGridData] = useState([]);

  // Initialize grid
  const initEmptyGrid = useCallback(
    (r, c) =>
      Array(r)
        .fill(null)
        .map(() =>
          Array(c)
            .fill(null)
            .map(() => ({ type: "EMPTY", label: "" }))
        ),
    []
  );

  // Load existing seats from API
  const { data: existingSeats, isLoading } = useQuery({
    queryKey: ["screen-seats", cinema?.id, screen?.id],
    queryFn: () => cinemaApi.getScreenSeats(cinema.id, screen.id),
    enabled: !!cinema?.id && !!screen?.id && open,
  });

  // Populate grid from loaded seats
  useEffect(() => {
    if (!open) return;

    const r = screen?.totalRows || 10;
    const c = screen?.totalCols || 12;
    setRows(r);
    setCols(c);

    const grid = initEmptyGrid(r, c);

    // Fill in existing seats
    if (existingSeats && Array.isArray(existingSeats)) {
      for (const seat of existingSeats) {
        const gr = seat.gridRow;
        const gc = seat.gridCol;
        if (gr >= 0 && gr < r && gc >= 0 && gc < c) {
          grid[gr][gc] = {
            type: seat.seatType || "STANDARD",
            label: seat.seatLabel || "",
          };
        }
      }
    }

    setGridData(grid);
  }, [open, existingSeats, screen, initEmptyGrid]);

  // Resize grid
  const handleResize = (newRows, newCols) => {
    const nr = Math.max(1, Math.min(26, newRows));
    const nc = Math.max(1, Math.min(30, newCols));

    setGridData((prev) => {
      const newGrid = initEmptyGrid(nr, nc);
      for (let r = 0; r < Math.min(prev.length, nr); r++) {
        for (let c = 0; c < Math.min(prev[r]?.length || 0, nc); c++) {
          newGrid[r][c] = prev[r][c];
        }
      }
      return newGrid;
    });

    setRows(nr);
    setCols(nc);
  };

  // Paint cell
  const paintCell = useCallback(
    (r, c) => {
      setGridData((prev) => {
        const newGrid = prev.map((row) => row.map((cell) => ({ ...cell })));
        if (activeTool === "ERASE") {
          newGrid[r][c] = { type: "EMPTY", label: "" };
        } else {
          newGrid[r][c] = { type: activeTool, label: newGrid[r][c].label };
        }
        return newGrid;
      });
    },
    [activeTool]
  );

  // Auto-label algorithm
  const autoLabel = () => {
    setGridData((prev) => {
      const newGrid = prev.map((row) => row.map((cell) => ({ ...cell })));
      let currentRowCharCode = 65; // 'A'

      for (let r = 0; r < newGrid.length; r++) {
        const hasSeats = newGrid[r].some((cell) => cell.type !== "EMPTY");
        if (!hasSeats) continue;

        const rowLetter = String.fromCharCode(currentRowCharCode);
        for (let c = 0; c < newGrid[r].length; c++) {
          if (newGrid[r][c].type !== "EMPTY") {
            newGrid[r][c].label = `${rowLetter}${c + 1}`;
          } else {
            newGrid[r][c].label = ""; // Xoá nhãn nếu là ô trống
          }
        }
        currentRowCharCode++;
      }
      return newGrid;
    });
    toast.success("Đã đánh số tự động thành công!");
  };

  // Reset grid
  const resetGrid = () => {
    setGridData(initEmptyGrid(rows, cols));
    toast.success("Đã xoá toàn bộ bố trí");
  };

  // Stats
  const stats = gridData.flat().reduce(
    (acc, cell) => {
      if (cell.type !== "EMPTY") acc.total++;
      if (cell.type === "STANDARD") acc.standard++;
      if (cell.type === "VIP") acc.vip++;
      if (cell.type === "COUPLE") acc.couple++;
      return acc;
    },
    { total: 0, standard: 0, vip: 0, couple: 0 }
  );

  // Save mutation
  const saveMutation = useMutation({
    mutationFn: () => {
      const seats = [];
      for (let r = 0; r < gridData.length; r++) {
        for (let c = 0; c < gridData[r].length; c++) {
          const cell = gridData[r][c];
          if (cell.type === "EMPTY") continue;
          seats.push({
            gridRow: r,
            gridCol: c,
            seatLabel: cell.label || `${String.fromCharCode(65 + r)}${c + 1}`,
            seatType: cell.type,
          });
        }
      }
      return cinemaApi.saveCustomLayout(cinema.id, screen.id, { screenId: screen.id, seats });
    },
    onSuccess: () => {
      toast.success("Đã lưu bố trí ghế thành công!");
      qc.invalidateQueries({ queryKey: ["screen-seats", cinema?.id, screen?.id] });
      qc.invalidateQueries({ queryKey: ["admin-screens", cinema?.id] });
      onClose();
    },
    onError: (err) => {
      toast.error(err?.response?.data?.message || "Lỗi khi lưu bố trí ghế");
    },
  });

  if (!open) return null;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={`🎨 Thiết kế ghế: ${screen?.name}`}
      size="full"
    >
      <div className="flex flex-col h-[80vh]">
        {/* ── Toolbar ── */}
        <div className="flex flex-wrap items-center gap-3 p-4 border-b border-gray-200 bg-gray-50">
          {/* Tools */}
          <div className="flex items-center gap-1.5 bg-white rounded-lg p-1 shadow-sm border">
            {TOOLS.map((tool) => (
              <button
                key={tool.id}
                onClick={() => setActiveTool(tool.id)}
                className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-sm font-medium transition-all ${
                  activeTool === tool.id
                    ? "text-white shadow-md"
                    : "text-gray-600 hover:bg-gray-100"
                }`}
                style={
                  activeTool === tool.id
                    ? { backgroundColor: tool.color }
                    : {}
                }
              >
                <span>{tool.icon}</span>
                <span className="hidden sm:inline">{tool.label}</span>
              </button>
            ))}
          </div>

          <div className="w-px h-8 bg-gray-300" />

          {/* Grid size controls */}
          <div className="flex items-center gap-2 text-sm text-gray-600">
            <Grid3X3 className="w-4 h-4" />
            <input
              type="number"
              min="1"
              max="26"
              value={rows}
              onChange={(e) =>
                handleResize(parseInt(e.target.value) || 1, cols)
              }
              className="w-14 px-2 py-1 border rounded text-center text-sm"
            />
            <span>×</span>
            <input
              type="number"
              min="1"
              max="30"
              value={cols}
              onChange={(e) =>
                handleResize(rows, parseInt(e.target.value) || 1)
              }
              className="w-14 px-2 py-1 border rounded text-center text-sm"
            />
          </div>

          <div className="w-px h-8 bg-gray-300" />

          {/* Actions */}
          <button
            onClick={autoLabel}
            className="flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-purple-700 bg-purple-50 hover:bg-purple-100 rounded-lg transition-colors"
          >
            <Wand2 className="w-4 h-4" />
            Auto-label
          </button>
          <button
            onClick={resetGrid}
            className="flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-red-600 bg-red-50 hover:bg-red-100 rounded-lg transition-colors"
          >
            <RotateCcw className="w-4 h-4" />
            Reset
          </button>
        </div>

        {/* ── Grid Area ── */}
        <div className="flex-1 overflow-auto bg-gray-900 p-6" ref={gridRef}>
          {isLoading ? (
            <div className="flex items-center justify-center h-full text-gray-400">
              Đang tải bố trí ghế...
            </div>
          ) : (
            <div className="flex flex-col items-center gap-0.5 select-none">
              {/* Screen indicator */}
              <div className="w-3/4 max-w-md h-2 bg-gradient-to-r from-transparent via-blue-400 to-transparent rounded-full mb-1 opacity-60" />
              <p className="text-xs text-gray-500 mb-4 tracking-widest uppercase">
                Màn hình
              </p>

              {gridData.map((row, r) => (
                <div key={r} className="flex items-center gap-0.5">
                  {/* Row label */}
                  <span className="w-6 text-xs text-gray-500 text-right mr-1.5 font-mono">
                    {String.fromCharCode(65 + r)}
                  </span>

                  {row.map((cell, c) => {
                    const isOccupied = cell.type !== "EMPTY";
                    return (
                      <button
                        key={`${r}-${c}`}
                        className={`relative flex items-center justify-center rounded transition-all duration-100 ${
                          isOccupied
                            ? "text-white text-[9px] font-bold shadow-sm hover:scale-110 hover:shadow-md cursor-pointer"
                            : "border border-dashed border-gray-700 hover:border-gray-500 cursor-pointer"
                        }`}
                        style={{
                          width: 36,
                          height: 32,
                          backgroundColor: isOccupied
                            ? TYPE_COLORS[cell.type]
                            : "transparent",
                        }}
                        title={
                          isOccupied
                            ? `${cell.label || "?"} (${TYPE_LABELS[cell.type] || cell.type})`
                            : `Ô trống [${r},${c}]`
                        }
                        onMouseDown={(e) => {
                          e.preventDefault();
                          setIsPainting(true);
                          paintCell(r, c);
                        }}
                        onMouseEnter={() => {
                          if (isPainting) paintCell(r, c);
                        }}
                        onMouseUp={() => setIsPainting(false)}
                      >
                        {isOccupied && (
                          <span className="leading-none">{cell.label || "·"}</span>
                        )}
                      </button>
                    );
                  })}
                </div>
              ))}

              {/* Column numbers */}
              <div className="flex items-center mt-1">
                <span className="w-6 mr-1.5" />
                {Array.from({ length: cols }, (_, c) => (
                  <span
                    key={c}
                    className="text-[9px] text-gray-600 font-mono text-center"
                    style={{ width: 36, marginRight: 2 }}
                  >
                    {c + 1}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* ── Bottom Stats & Save ── */}
        <div className="flex items-center justify-between p-4 border-t border-gray-200 bg-white">
          <div className="flex items-center gap-4 text-sm">
            <span className="font-semibold text-gray-700">
              Tổng: <span className="text-blue-600">{stats.total}</span> ghế
            </span>
            <span className="flex items-center gap-1">
              <span
                className="w-3 h-3 rounded-sm inline-block"
                style={{ backgroundColor: TYPE_COLORS.STANDARD }}
              />
              Thường: {stats.standard}
            </span>
            <span className="flex items-center gap-1">
              <span
                className="w-3 h-3 rounded-sm inline-block"
                style={{ backgroundColor: TYPE_COLORS.VIP }}
              />
              VIP: {stats.vip}
            </span>
            <span className="flex items-center gap-1">
              <span
                className="w-3 h-3 rounded-sm inline-block"
                style={{ backgroundColor: TYPE_COLORS.COUPLE }}
              />
              Đôi: {stats.couple}
            </span>
          </div>

          <div className="flex gap-3">
            <Button variant="ghost" onClick={onClose}>
              Huỷ
            </Button>
            <Button
              onClick={() => saveMutation.mutate()}
              loading={saveMutation.isPending}
              leftIcon={<Save className="w-4 h-4" />}
              disabled={stats.total === 0}
            >
              Lưu bố trí ({stats.total} ghế)
            </Button>
          </div>
        </div>
      </div>

      {/* Global mouseUp listener */}
      {isPainting && (
        <div
          className="fixed inset-0 z-50 cursor-crosshair"
          style={{ pointerEvents: "all", background: "transparent" }}
          onMouseUp={() => setIsPainting(false)}
        />
      )}
    </Modal>
  );
}
