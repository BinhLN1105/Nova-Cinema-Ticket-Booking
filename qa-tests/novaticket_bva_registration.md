### Phụ lục A: Báo cáo chi tiết BVA - Đăng ký tài khoản

#### 1. Mô tả bài toán
Hệ thống NovaTicket cho phép người dùng đăng ký tài khoản mới. Một yêu cầu đăng ký được xem là hợp lệ khi các trường dữ liệu đầu vào thỏa mãn điều kiện:

| Biến đầu vào | Ý nghĩa | Kiểu dữ liệu | Miền giá trị hợp lệ |
| :--- | :--- | :--- | :--- |
| `password` | Mật khẩu tài khoản (yêu cầu chứa cả chữ cái và chữ số) | Chuỗi (String) | Từ 6 đến 32 ký tự |
| `fullName` | Họ và tên người dùng | Chuỗi (String) | Từ 2 đến 100 ký tự |

#### 2. Xác định lớp tương đương
| Biến đầu vào | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
| :--- | :--- | :--- | :--- | :--- |
| Độ dài Mật khẩu | 6 <= password <= 32 | V1 | password < 6<br>password > 32<br>Chỉ có chữ / Chỉ có số | X1 |
| Độ dài Họ tên | 2 <= fullName <= 100 | V2 | fullName < 2<br>fullName > 100 | X2 |

#### 3. Bảng phân tích giá trị biên
| Biến đầu vào | min | min+ | nominal | max- | max | Tag biên |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Mật khẩu (ký tự) | 6 | 7 | 12 | 31 | 32 | B1(min), B2(min+), B3(nominal), B4(max-), B5(max) |
| Họ tên (ký tự) | 2 | 3 | 15 | 99 | 100 | B6(min), B7(min+), B8(nominal), B9(max-), B10(max) |

#### 4. Bảng Test Case chi tiết và Kết quả thực thi
| STT | Tên Test Case | Dữ liệu đầu vào (Input mô phỏng) | Kết quả mong đợi (Expected) | Kết quả thực tế (Actual) | Trạng thái | Tag bao phủ |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | BVA_PWD_01: Biên dưới mật khẩu không hợp lệ [5 ký tự] | `password: "abc12"` | Bị chặn lỗi 400 Bad Request. | ✅ Mật khẩu 5 ký tự bị chặn - Trả về 400 | PASS | X1 |
| 2 | BVA_PWD_02: Biên dưới mật khẩu hợp lệ nhỏ nhất [6 ký tự] | `password: "abc123"` | API phản hồi hợp lệ. | ✅ Mật khẩu 6 ký tự được chấp nhận - Đăng ký thành công | PASS | V1, B1 |
| 3 | BVA_PWD_03: Nominal - Vùng hợp lệ mật khẩu [12 ký tự] | `password: "novaTicket2026"` | API phản hồi hợp lệ. | ✅ Mật khẩu 12 ký tự hợp lệ - Đăng ký thành công | PASS | V1, B3 |
| 4 | BVA_PWD_04: Biên trên mật khẩu hợp lệ lớn nhất [32 ký tự] | `password: "{{bva_pwd_32}}"` | API phản hồi hợp lệ. | ✅ Mật khẩu 32 ký tự hợp lệ - Đăng ký thành công | PASS | V1, B5 |
| 5 | BVA_PWD_05: Biên trên mật khẩu không hợp lệ [33 ký tự] | `password: "{{bva_pwd_33}}"` | Bị chặn lỗi 400 Bad Request. | ✅ Mật khẩu 33 ký tự bị chặn - Trả về 400 | PASS | X1 |
| 6 | BVA_PWD_06: Biên mật khẩu thiếu Số [chỉ chứa chữ] | `password: "abcdefgh"` | Bị chặn lỗi 400 Bad Request. | ✅ Mật khẩu chỉ chứa chữ bị chặn - Trả về 400 | PASS | X1 |
| 7 | BVA_PWD_07: Biên mật khẩu thiếu Chữ [chỉ chứa số] | `password: "12345678"` | Bị chặn lỗi 400 Bad Request. | ✅ Mật khẩu chỉ chứa số bị chặn - Trả về 400 | PASS | X1 |
| 8 | BVA_NAM_01: Biên dưới fullName không hợp lệ [1 ký tự] | `fullName: "A"` | Bị chặn lỗi 400 Bad Request. | ✅ fullName 1 ký tự bị chặn - Trả về 400 | PASS | X2 |
| 9 | BVA_NAM_02: Biên dưới fullName hợp lệ nhỏ nhất [2 ký tự] | `fullName: "An"` | API phản hồi hợp lệ. | ✅ fullName 2 ký tự hợp lệ - Đăng ký thành công | PASS | V2, B6 |
| 10 | BVA_NAM_03: Biên trên fullName hợp lệ lớn nhất [100 ký tự] | `fullName: "{{bva_long_name_100}}"`| API phản hồi hợp lệ. | ✅ fullName 100 ký tự hợp lệ - Đăng ký thành công | PASS | V2, B10 |
| 11 | BVA_NAM_04: Biên trên fullName không hợp lệ [101 ký tự] | `fullName: "{{bva_long_name_101}}"`| Bị chặn lỗi 400 Bad Request. | ✅ fullName 101 ký tự bị chặn - Trả về 400 | PASS | X2 |