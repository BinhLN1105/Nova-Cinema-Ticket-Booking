---
description: Tạo Pull Request cho dự án NovaTicket — tóm tắt thay đổi, kiểm tra DB migration, test coverage, và điền PR description theo chuẩn.
---

## Steps

### 1. Đọc Git Diff
- Chạy `git diff main...HEAD --stat` để xem danh sách file thay đổi
- Chạy `git diff main...HEAD` để đọc nội dung thay đổi chi tiết
- Xác định scope ảnh hưởng: chỉ backend / chỉ frontend / chỉ mobile / nhiều service

### 2. Kiểm tra DB Migration (CRITICAL)
- Scan xem có file nào thay đổi liên quan đến Entity (`@Entity`, `@Table`, `@Column`) không
- Nếu CÓ thay đổi Entity → kiểm tra `src/main/resources/db/migration/` có file `V{n}__*.sql` tương ứng không
- Nếu KHÔNG có migration script → **DỪNG LẠI**, cảnh báo:
  ```
  ⚠️ CẢNH BÁO: Phát hiện thay đổi schema DB nhưng không có migration script.
  Hãy tạo file: src/main/resources/db/migration/V{n}__{mô_tả}.sql trước khi tạo PR.
  ```
- Nếu không có thay đổi Entity → bỏ qua bước này, ghi chú "No DB changes"

### 3. Kiểm tra Test Coverage
- Scan xem các Service/Controller bị thay đổi có file test tương ứng không
- Pattern kiểm tra: `{ClassName}Test.java` hoặc `{ClassName}Tests.java`
- Nếu thiếu test → ghi chú vào PR description phần "⚠️ Missing Tests"
- Không block PR vì thiếu test, chỉ cảnh báo

### 4. Xác định Label & Scope
Dựa trên diff, tự xác định:
- **Type**: `feat` / `fix` / `refactor` / `docs` / `test` / `chore`
- **Scope**: module bị ảnh hưởng → `booking` / `payment` / `auth` / `movie` / `loyalty` / `ai-chat` / `mobile` / `frontend`
- **Breaking change**: có thay đổi API contract không?

### 5. Điền PR Description
Tạo nội dung PR theo template sau:

```markdown
## 📋 Mô tả
[Tóm tắt 2-3 câu những thay đổi chính, viết bằng tiếng Việt]

## 🎯 Loại thay đổi
- [ ] feat: Tính năng mới
- [ ] fix: Sửa lỗi
- [ ] refactor: Cải thiện code không ảnh hưởng behavior
- [ ] docs: Cập nhật tài liệu
- [ ] test: Thêm/sửa test
- [ ] chore: Cấu hình, dependency

## 📦 Scope ảnh hưởng
- [ ] Backend (Spring Boot)
- [ ] AI Service (FastAPI)
- [ ] Frontend (ReactJS)
- [ ] Mobile (Android)

## 🗄️ Database
- [ ] Không có thay đổi DB
- [ ] Có thay đổi schema — migration script: `V{n}__{tên}.sql` ✅

## ✅ Checklist
- [ ] Code đã chạy được ở local
- [ ] Không có secret/credential trong code
- [ ] API response đúng chuẩn `ApiResponse<T>`
- [ ] Không có raw SQL (chỉ dùng Hibernate/JPQL)

## ⚠️ Lưu ý cho reviewer
[Ghi các điểm cần review kỹ, hoặc "Không có" nếu straightforward]
```

### 6. Xuất kết quả
- In ra PR description hoàn chỉnh đã điền sẵn
- Tóm tắt ngắn: số file thay đổi, có DB change không, có thiếu test không
- Nhắc commit message cuối cùng theo Conventional Commits format
