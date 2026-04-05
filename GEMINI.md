# NovaTicket — Antigravity IDE Configuration

## Agent Mode
- Dùng **Planning Mode** cho: VNPay flow, Auth/Security, thay đổi DB schema, refactor nhiều file
- Dùng **Fast Mode** cho: sửa typo, thêm comment, rename, format code

## Turbo Auto-run (chạy KHÔNG hỏi)
# ── Backend ──────────────────────────────
mvn compile
mvn test -pl backend
mvn test -pl backend -Dtest=*
mvn spring-boot:run

# ── Mobile ───────────────────────────────
./gradlew assembleDebug
./gradlew build
./gradlew test

# ── AI Service ───────────────────────────
pip install -r requirements.txt
uvicorn main:app --reload

# ── Frontend ─────────────────────────────
npm install
npm run dev
npm run build
npm run lint

# ── Database (READ ONLY) ─────────────────
psql -U postgres -d nova_db -c "SELECT ..."

## Luôn hỏi trước khi chạy (KHÔNG tự động)
- Bất kỳ lệnh có: DROP / DELETE / TRUNCATE / ALTER TABLE
- mvn flyway:migrate
- git push / git merge / git rebase
- Lệnh deploy bất kỳ môi trường nào
- Xóa file hoặc thư mục

## Artifact Preferences
- Tạo **Plan Artifact** khi task thay đổi > 3 file
- Tạo **Sequence Diagram Artifact** khi đụng đến VNPay flow hoặc Auth flow
- Tạo **API Doc Artifact** sau mỗi endpoint mới được tạo
- PR description luôn dùng template từ `.agents/workflows/create_pr.md`

## Design Style (Frontend ReactJS)
- Theme: Dark, cinema-inspired — navy `#0D1B2A`, accent gold `#F5C518`
- Không dùng MUI hoặc AntD — dùng TailwindCSS + Radix UI
- Animation: Framer Motion chỉ cho seat picker và page transition
- Mobile-first, responsive

## Multi-service Rule
Khi thay đổi ảnh hưởng nhiều service:
1. Backend API contract thay đổi TRƯỚC
2. Cập nhật docs/adr nếu là quyết định kiến trúc
3. Frontend / Mobile adapt SAU
4. Không commit thay đổi cả hai cùng lúc

## [TODO] Điền thông tin cá nhân bên dưới
# ─────────────────────────────────────────────────────────────────
# Bạn cần tự điền 3 phần sau, mình không thể biết thay bạn:

## Local Environment
# PORT backend đang chạy ở local:
BACKEND_PORT=8080
# PORT AI service:
AI_SERVICE_PORT=8000
# Tên database local:
LOCAL_DB_NAME=nova_db
# Username postgres local:
LOCAL_DB_USER=postgres

## VNPay Sandbox
# Điền để agent biết đang dùng sandbox hay production khi debug:
VNPAY_ENV=sandbox   # hoặc: production

## Personal Preferences
# Ngôn ngữ trả lời: (mặc định tiếng Việt)
RESPONSE_LANGUAGE=vi
# Khi không chắc về business logic: hỏi hay tự quyết?
UNCERTAINTY_BEHAVIOR=ask   # hoặc: decide
# ─────────────────────────────────────────────────────────────────
