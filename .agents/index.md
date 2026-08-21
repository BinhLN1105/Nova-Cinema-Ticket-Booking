# NovaTicket Agent Skills & Workflows Index

Tài liệu này tổng hợp toàn bộ các Skill và Workflow đang được thiết lập trong dự án NovaTicket.

## 🛠️ Skills Catalog

1. **`plan-first`** (`.agents/skills/plan-first/SKILL.md`):
   - **Mục đích**: Bắt buộc khảo sát (Research), lập bản kế hoạch (`implementation_plan.md`) và chờ User duyệt (Confirm) trước khi chỉnh sửa file đối với mọi task phức tạp (>= 2 file, tính năng mới, Auth, Payment, DB Schema).
2. **`ui-ux-pro-max`** (`.agents/skills/ui-ux-pro-max/SKILL.md`):
   - **Mục đích**: Thiết kế giao diện Web/Mobile chuẩn điện ảnh Dark Navy & Gold, tuân thủ `MASTER.md`, không dùng template chung chung.
3. **`api-design`** (`.agents/skills/api-design/SKILL.md`):
   - **Mục đích**: Quy chuẩn REST API, `ApiResponse<T>`, DTO mapping và error handling thống nhất giữa Backend Spring Boot & FastAPI.
4. **`db-migration`** (`.agents/skills/db-migration/SKILL.md`):
   - **Mục đích**: Quy chuẩn migration PostgreSQL, Flyway script và Entity JPA.
5. **`security`** (`.agents/skills/security/SKILL.md`):
   - **Mục đích**: Quy chuẩn JWT, Spring Security, BCrypt, phân quyền Role và bảo mật đa tầng.

## 🔄 Workflows Catalog

- **`create_pr.md`**: Template tạo PR chuẩn.
- **`debug.md`**: Quy trình truy vết và fix bug logic / hệ thống.
- **`write_api.md`**: Quy trình triển khai endpoint API mới.
- **`write_tests.md`**: Quy trình viết unit test và integration test.
