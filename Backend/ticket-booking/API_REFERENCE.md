# Cinema API — Endpoint Reference
Base URL: `/api/v1`

## 🔓 Auth
| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| POST | `/auth/register` | Public | Đăng ký tài khoản |
| POST | `/auth/login` | Public | Đăng nhập LOCAL |
| POST | `/auth/social-login` | Public | Đăng nhập Google/Facebook |
| POST | `/auth/refresh` | Public | Lấy access token mới |
| POST | `/auth/logout` | 🔑 User | Đăng xuất |

## 👤 User
| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/users/me` | 🔑 User | Xem profile |
| PATCH | `/users/me` | 🔑 User | Cập nhật profile |
| PATCH | `/users/me/fcm-token` | 🔑 User | Cập nhật FCM token |

## 🎬 Movie
| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/movies?status=NOW_SHOWING&page=0&size=10` | Public | Danh sách phim theo trạng thái |
| GET | `/movies/search?q=avengers` | Public | Tìm kiếm phim |
| GET | `/movies/{id}` | Public | Chi tiết phim |
| GET | `/movies/cinema/{cinemaId}` | Public | Phim đang chiếu tại rạp |
| GET | `/movies/genres` | Public | Danh sách thể loại |
| POST | `/movies` | 🛡️ ADMIN | Tạo phim mới |
| PUT | `/movies/{id}` | 🛡️ ADMIN | Cập nhật phim |
| DELETE | `/movies/{id}` | 🛡️ ADMIN | Xoá phim (soft) |

## 🏢 Cinema & Screen
| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/cinemas?city=HoChiMinh` | Public | Danh sách rạp theo thành phố |
| GET | `/cinemas/{id}` | Public | Chi tiết rạp |
| POST | `/cinemas` | 🛡️ ADMIN | Tạo rạp mới |
| PUT | `/cinemas/{id}` | 🛡️ ADMIN | Cập nhật rạp |
| DELETE | `/cinemas/{id}` | 🛡️ ADMIN | Vô hiệu hoá rạp |
| GET | `/cinemas/{cinemaId}/screens` | Public | Danh sách phòng chiếu |
| POST | `/cinemas/{cinemaId}/screens` | 🛡️ ADMIN | Tạo phòng chiếu |
| PUT | `/cinemas/{cinemaId}/screens/{screenId}` | 🛡️ ADMIN | Cập nhật phòng chiếu |

## 🕐 Showtime & Seat Map
| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/showtimes?movieId=...&date=2024-12-01` | Public | Suất chiếu theo phim + ngày |
| GET | `/showtimes?movieId=...&cinemaId=...&date=...` | Public | Lọc thêm theo rạp |
| GET | `/showtimes/{id}` | Public | Chi tiết suất chiếu |
| GET | `/showtimes/{id}/seats` | Public | Sơ đồ ghế (real-time status) |
| POST | `/showtimes` | 🛡️ ADMIN | Tạo suất chiếu |

## 🎟️ Booking
| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| POST | `/bookings` | 🔑 User | Đặt vé (lock ghế + tạo đơn) |
| GET | `/bookings/me?page=0&size=10` | 🔑 User | Lịch sử đặt vé của tôi |
| GET | `/bookings/{id}` | 🔑 User | Chi tiết đơn đặt vé |
| DELETE | `/bookings/{id}` | 🔑 User | Huỷ đơn (chỉ khi PENDING) |
| POST | `/bookings/check-in?qrCode=...` | 🛡️ STAFF/ADMIN | Quét QR check-in tại rạp |

## 💳 Payment
| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| POST | `/payments` | 🔑 User | Tạo URL thanh toán VNPay |
| GET | `/payments/vnpay/callback` | Public | VNPay redirect callback |
| GET | `/payments/booking/{bookingId}` | 🔑 User | Trạng thái thanh toán |

## 🎁 Voucher
| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/vouchers/validate?code=SUMMER30` | Public | Kiểm tra voucher |
| POST | `/vouchers` | 🛡️ ADMIN | Tạo voucher |
| PUT | `/vouchers/{id}` | 🛡️ ADMIN | Cập nhật voucher |
| PATCH | `/vouchers/{id}/toggle` | 🛡️ ADMIN | Bật/tắt voucher |

## ⭐ Review
| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/reviews?movieId=...&page=0&size=10` | Public | Đánh giá của phim |
| POST | `/reviews` | 🔑 User | Gửi đánh giá (cần booking PAID) |
| DELETE | `/reviews/{id}` | 🔑 User | Xoá đánh giá |

## 🔔 Notification
| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/notifications?page=0&size=20` | 🔑 User | Danh sách thông báo |
| GET | `/notifications/unread-count` | 🔑 User | Số thông báo chưa đọc (badge) |
| PATCH | `/notifications/read-all` | 🔑 User | Đánh dấu đọc tất cả |

## 🍿 Combo
| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/combos` | Public | Danh sách combo đang bán |

---
## Auth Legend
- **Public** — Không cần token
- **🔑 User** — Cần Bearer token (mọi role đã đăng nhập)
- **🛡️ ADMIN** — Cần role ADMIN
- **🛡️ STAFF/ADMIN** — Cần role STAFF hoặc ADMIN
