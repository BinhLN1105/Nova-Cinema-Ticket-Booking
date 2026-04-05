# ADR 002: Use of Hibernate (Spring Data JPA) over Raw SQL

## Status
Accepted

## Context
Dự án NovaTicket có cấu trúc dữ liệu phức tạp (Showtime, Cinema, Screen, Seat, Booking) với nhiều mối quan hệ One-to-Many, Many-to-Many. Việc quản lý các quan hệ này bằng Raw SQL thuần túy có thể dẫn đến:
- Codebase phình to (Boilerplate code cho SQL queries).
- Khó bảo trì khi schema thay đổi.
- Rủi ro về SQL Injection nếu không handle cẩn thận.
- Thiếu sự thống nhất trong cách ánh xạ dữ liệu (Data mapping).

## Decision
Sử dụng **Hibernate** (thông qua Spring Data JPA) làm công cụ ORM chính cho Backend.

## Rationale
1. **Productivity**: Hibernate tự động hóa việc tạo CRUD cơ bản. Các tính năng như `JpaRepository` cho phép viết query rất nhanh mà không cần SQL viết tay (ví dụ: `findByMovieIdAndStartTimeAfter`).
2. **Type Safety**: Tương tác với đối tượng Java thay vì String SQL, giúp compiler phát hiện lỗi sớm.
3. **Database Independence**: Tuy dự án hiện dùng PostgreSQL (`pgvector`), nhưng Hibernate giúp dễ dàng chuyển đổi sang DB khác nếu cần mà không phải sửa lại hàng nghìn dòng SQL.
4. **Caching & Lazy Loading**: Tối ưu hiệu năng thông qua cơ chế 1st/2nd level cache và chỉ tải dữ liệu liên quan khi thực sự cần thiết.
5. **Standardization**: Spring Boot + Data JPA là stack tiêu chuẩn, giúp các thành viên mới dễ dàng tiếp cận dự án.

## Consequences
- **Learning Curve**: Đòi hỏi lập trình viên hiểu sâu về Hibernate (N+1 query, Proxy objects, Session Management).
- **Performance Overhead**: Hibernate có overhead nhỏ so với JDBC thuần, nhưng có thể tối ưu bằng cách dùng `@Query` (JPQL) hoặc Native Query cho các tác vụ đặc biệt (như vector search).
- **Complexity**: Việc debug đôi khi khó khăn hơn Raw SQL khi Hibernate sinh ra các câu query phức tạp.
