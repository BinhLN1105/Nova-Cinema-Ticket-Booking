# AGENTS.md - Development Standards

Hướng dẫn này quy định Tech Stack, Quy tắc đặt tên (Naming Convention) và Kiến trúc phân lớp (Layer Architecture) thực tế đang được áp dụng trong project NovaTicket.

## 1. Tech Stack Summary

### 1.1 Backend (ticket-booking)
- **Language**: Java 21 LTS
- **Framework**: Spring Boot 4.0.3+ (Spring Data JPA, Spring Security, Spring WebFlux)
- **Database**: PostgreSQL (với extension `pgvector`)
- **ORM**: Hibernate / Spring Data JPA
- **Migration**: Flyway (Dependency sẵn có, hiện đang là Technical Debt)
- **Caching**: Redis
- **Security**: JWT (jjwt 0.12.5), Password Hashing (BCrypt)
- **Tools**: Lombok, MapStruct (Mapping DTO), Cloudinary (Image storage), Firebase Admin (FCM).

### 1.2 AI Service (ai-booking)
- **Language**: Python 3.10+
- **Framework**: FastAPI
- **Vector DB**: ChromaDB
- **LLM Context**: LangChain / RAG Pattern
- **Server**: Uvicorn

### 1.3 Android App
- **Language**: Java 17
- **Architecture**: MVVM
- **Frameworks**: Hilt (DI), Retrofit 2 (Network), Room (Local DB), Navigation Component.
- **UI**: ViewBinding, ViewModel, LiveData.

### 1.4 Web Frontend (React)
- **Language**: Javascript / Typescript
- **Framework**: React 18 (Vite based)
- **Styling**: Tailwind CSS
- **State Management**: Zustand, TanStack Query.

## 2. Naming Conventions

### 2.1 Backend (Java)
- **Class Naming**: PascalCase (ví dụ: `BookingService`, `UserController`).
- **Endpoint naming**: kebab-case (ví dụ: `/api/v1/movie-showtimes`).
- **Standard Suffixes**:
  - Controller: `*Controller`
  - Service: `*Service` (interface) & `*ServiceImpl` (nếu tách biệt)
  - Repository: `*Repository`
  - DTO: `*Request` (Input), `*Response` (Output)
  - Mapper: `*Mapper`
  - Model/Entity: `*Entity` (hoặc nằm trong package `model`)

### 2.2 Android (Java & XML)
- **Class Naming**:
  - Fragment: `*Fragment`
  - ViewModel: `*ViewModel`
  - Repository: `*Repository`
  - Adapter: `*Adapter`
- **Resource Naming**:
  - Layout: `fragment_*.xml`, `activity_*.xml`, `item_*.xml`, `dialog_*.xml`
  - Drawable: `ic_*.xml`, `bg_*.xml`
  - Values: `strings.xml`, `colors.xml`, `dimens.xml`.

### 2.3 Database (PostgreSQL)
- **Table Name**: snake_case, plural (ví dụ: `bookings`, `users`).
- **Column Name**: snake_case (ví dụ: `created_at`, `movie_id`).
- **Primary Key**: `id` (UUID hoặc BigSerial).

## 3. Layer Architecture

### 3.1 Backend Spring Boot
1. **Controller Layer**: Tiếp nhận request, validation, gọi service và trả về DTO.
2. **Service Layer**: Xử lý logic nghiệp vụ chính, transaction management.
3. **Repository Layer (Data Access)**: Tương tác trực tiếp với Database qua Spring Data JPA.
4. **DTO Layer**: Chuyển đổi dữ liệu giữa Controller và Service để bảo mật cấu trúc Database.

### 3.2 AI Service FastAPI
1. **API Layer (`main.py`)**: Định nghĩa endpoints.
2. **Agent Layer**: Quyết định luồng xử lý (Chatbot logic).
3. **Ingestion Layer**: Xử lý nạp dữ liệu (VectorStore, Embedder).

### 3.3 Android MVVM
1. **View (Fragment/Activity)**: Chỉ hiển thị UI và quan sát LiveData.
2. **ViewModel**: Giữ trạng thái UI, giao tiếp với Repository.
3. **Repository**: Trung gian quản lý dữ liệu từ Remote (Retrofit) và Local (Room).

## 4. [TURBO_PERMISSIONS]
(Để trống theo yêu cầu)

## 5. [BUSINESS_DECISIONS]
(Để trống theo yêu cầu)
