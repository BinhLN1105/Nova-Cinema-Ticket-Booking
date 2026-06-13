# Assignment: Kiểm thử chức năng đăng ký học phần

**Thời lượng:** 90 phút  
**Chủ đề:** Phân hoạch lớp tương đương, phân tích giá trị biên, thiết kế test case và kiểm thử tự động  
**Mức độ:** Cơ bản đến trung bình  
**Hình thức:** Cá nhân  
**Tổng điểm:** 10 điểm

---

## 1. Mục tiêu bài tập

1. Xác định được **điều kiện kiểm thử** từ một đặc tả yêu cầu đơn giản.
2. Áp dụng được kỹ thuật **phân hoạch lớp tương đương** để chia miền dữ liệu đầu vào thành các lớp hợp lệ và không hợp lệ.
3. Áp dụng được kỹ thuật **phân tích giá trị biên** để chọn các dữ liệu kiểm thử nằm gần ranh giới giữa vùng hợp lệ và không hợp lệ.
4. Thiết kế được bảng **test case** có đầy đủ input, expected result và tag bao phủ.
5. Viết được hàm kiểm tra logic và một số **unit test** cho các trường hợp biên.

---

## 2. Nội dung tham khảo

Bài tập này bám sát các nội dung chính trong phần kỹ thuật kiểm thử hộp đen:

- **Equivalence Partitioning**: phân chia miền dữ liệu đầu vào thành các lớp tương đương.
- **Boundary Value Analysis**: chọn giá trị tại biên và gần biên để kiểm thử.
- **Test case design**: thiết kế test case có input, expected outcome và tag bao phủ.
- **Test script / Unit test**: triển khai kiểm thử tự động bằng code.

Trong bài này, sinh viên cần đặc biệt chú ý đến cách trình bày theo mẫu:

| Conditions | Valid Partitions | Tag | Invalid Partitions | Tag | Valid Boundaries | Tag |
|---|---|---|---|---|---|---|

và bảng test case theo mẫu:

| Test Case | Input | Expected Outcome | New Tags Covered |
|---|---|---|---|

---

## 3. Mô tả bài toán

Hệ thống đăng ký học phần của Trường Đại học UTH cho phép sinh viên gửi yêu cầu đăng ký học phần.

Một yêu cầu đăng ký được xem là **hợp lệ** khi tất cả các điều kiện sau đồng thời thỏa mãn:

| Biến đầu vào | Ý nghĩa | Kiểu dữ liệu | Miền giá trị hợp lệ |
|---|---|---|---|
| `tinChi` | Số tín chỉ sinh viên muốn đăng ký | Số nguyên | Từ 10 đến 25 |
| `gpa` | Điểm trung bình tích lũy hiện tại | Số thực | Từ 2.0 đến 4.0 |
| `monNo` | Số môn sinh viên đang nợ | Số nguyên | Từ 0 đến 3 |
| `hocKy` | Học kỳ hiện tại của sinh viên | Số nguyên | Từ 1 đến 10 |

Hệ thống trả về:

- `True` hoặc thông báo **Hợp lệ** nếu tất cả điều kiện đều đúng.
- `False` hoặc thông báo **Không hợp lệ** nếu có ít nhất một điều kiện sai.

---

## 4. Giả định của bài toán

Để tránh hiểu nhầm, bài tập sử dụng các giả định sau:

1. Chỉ xét dữ liệu đầu vào là dữ liệu số.
2. Không xét dữ liệu `null`, rỗng, chuỗi ký tự hoặc định dạng sai.
3. `tinChi`, `monNo`, `hocKy` là số nguyên.
4. `gpa` là số thực, có thể có phần thập phân.
5. Một yêu cầu đăng ký hợp lệ khi và chỉ khi **tất cả** biến đầu vào nằm trong miền hợp lệ.

Công thức logic tổng quát:

$$
Valid =
(10 \leq tinChi \leq 25)
\land
(2.0 \leq gpa \leq 4.0)
\land
(0 \leq monNo \leq 3)
\land
(1 \leq hocKy \leq 10)
$$

---

# PHẦN A. ĐỀ BÀI GIAO CHO SINH VIÊN

---

## Câu 1. Xác định lớp tương đương

**Điểm:** 2 điểm

Hãy xác định các lớp tương đương hợp lệ và không hợp lệ cho từng biến đầu vào.

Sinh viên cần điền vào bảng sau:

| Biến đầu vào | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
|---|---|---|---|---|
| Số tín chỉ | `10 <= tinChi <= 25` | V1 | `tinChi < 10`<br>`tinChi > 25` | X1<br>X2 |
| GPA | `2.0 <= gpa <= 4.0` | V2 | `gpa < 2.0`<br>`gpa > 4.0` | X3<br>X4 |
| Số môn nợ | `0 <= monNo <= 3` | V3 | `monNo < 0`<br>`monNo > 3` | X5<br>X6 |
| Học kỳ | `1 <= hocKy <= 10` | V4 | `hocKy < 1`<br>`hocKy > 10` | X7<br>X8 |

### Yêu cầu

- Mỗi biến cần có ít nhất 1 lớp hợp lệ.
- Mỗi biến cần có ít nhất 2 lớp không hợp lệ:
  - Nhỏ hơn giá trị nhỏ nhất.
  - Lớn hơn giá trị lớn nhất.
- Mỗi lớp cần được đặt tag để phục vụ theo dõi độ bao phủ.
- Có thể đặt tag theo mẫu:
  - `V1`, `V2`, `V3`, ... cho lớp hợp lệ.
  - `X1`, `X2`, `X3`, ... cho lớp không hợp lệ.

---

## Câu 2. Phân tích giá trị biên

**Điểm:** 2 điểm

Áp dụng kỹ thuật **Standard Boundary Value Analysis** để xác định các giá trị cần kiểm thử cho từng biến.

Với mỗi biến có miền giá trị:

$$
[min, max]
$$

cần xác định 5 giá trị:

| Ký hiệu | Ý nghĩa |
|---|---|
| `min` | Giá trị nhỏ nhất hợp lệ |
| `min+` | Giá trị ngay trên giá trị nhỏ nhất |
| `nominal` | Giá trị đại diện nằm giữa miền hợp lệ |
| `max-` | Giá trị ngay dưới giá trị lớn nhất |
| `max` | Giá trị lớn nhất hợp lệ |

Sinh viên cần điền vào bảng sau:

| Biến đầu vào | min | min+ | nominal | max- | max | Tag biên |
|---|---:|---:|---:|---:|---:|---|
| Số tín chỉ | 10 | 11 | 18 | 24 | 25 | B1 (min), B2 (min+), B3 (nominal), B4 (max-), B5 (max) |
| GPA | 2.0 | 2.1 | 3.0 | 3.9 | 4.0 | B6 (min), B7 (min+), B8 (nominal), B9 (max-), B10 (max) |
| Số môn nợ | 0 | 1 | 2 | 2 | 3 | B11 (min), B12 (min+), B13 (nominal), B14 (max-), B15 (max) |
| Học kỳ | 1 | 2 | 5 | 9 | 10 | B16 (min), B17 (min+), B18 (nominal), B19 (max-), B20 (max) |

### Gợi ý chọn nominal

| Biến | Miền hợp lệ | Có thể chọn nominal |
|---|---:|---:|
| Số tín chỉ | 10 đến 25 | 18 |
| GPA | 2.0 đến 4.0 | 3.0 |
| Số môn nợ | 0 đến 3 | 2 |
| Học kỳ | 1 đến 10 | 5 |

### Lưu ý

Với biến GPA là số thực, sinh viên có thể chọn `min+` và `max-` theo độ chính xác giả định.

Ví dụ nếu giả định GPA lấy 1 chữ số thập phân:

| Biên | Giá trị |
|---|---:|
| min | 2.0 |
| min+ | 2.1 |
| nominal | 3.0 |
| max- | 3.9 |
| max | 4.0 |

Nếu muốn kiểm thử mạnh hơn, có thể bổ sung các giá trị ngoài biên như `1.9` và `4.1`, nhưng phần này thuộc hướng **Robustness BVA**.

---

## Câu 3. Thiết kế test case

**Điểm:** 3 điểm

Dựa trên kết quả Câu 1 và Câu 2, hãy thiết kế bảng test case để kiểm thử chức năng đăng ký học phần.

### Yêu cầu

- Thiết kế tối thiểu 8 test case.
- Phải có cả test case hợp lệ và không hợp lệ.
- Phải có test case kiểm tra tại giá trị biên.
- Mỗi test case cần ghi rõ tag được bao phủ.
- Kết quả mong đợi phải ghi rõ:
  - **Hợp lệ**, hoặc
  - **Không hợp lệ**, kèm lý do.

Sinh viên điền vào bảng sau:

| STT | Tên test case | Số tín chỉ | GPA | Số môn nợ | Học kỳ | Kết quả mong đợi | Tag được bao phủ |
|---:|---|---:|---:|---:|---:|---|---|
| 1 | TC_VAL_NOM (Tất cả giá trị nominal hợp lệ) | 18 | 3.0 | 2 | 5 | Hợp lệ | V1, V2, V3, V4, B3, B8, B13, B18 |
| 2 | TC_VAL_MIN (Tất cả giá trị min biên hợp lệ) | 10 | 2.0 | 0 | 1 | Hợp lệ | V1, V2, V3, V4, B1, B6, B11, B16 |
| 3 | TC_VAL_MAX (Tất cả giá trị max biên hợp lệ) | 25 | 4.0 | 3 | 10 | Hợp lệ | V1, V2, V3, V4, B5, B10, B15, B20 |
| 4 | TC_INV_TC_LOW (Tín chỉ dưới biên hợp lệ) | 9 | 3.0 | 2 | 5 | Không hợp lệ (Số tín chỉ = 9 < 10) | X1 |
| 5 | TC_INV_TC_HIGH (Tín chỉ vượt quá biên hợp lệ) | 26 | 3.0 | 2 | 5 | Không hợp lệ (Số tín chỉ = 26 > 25) | X2 |
| 6 | TC_INV_GPA_LOW (GPA dưới biên hợp lệ) | 18 | 1.9 | 2 | 5 | Không hợp lệ (GPA = 1.9 < 2.0) | X3 |
| 7 | TC_INV_GPA_HIGH (GPA vượt quá biên hợp lệ) | 18 | 4.1 | 2 | 5 | Không hợp lệ (GPA = 4.1 > 4.0) | X4 |
| 8 | TC_INV_MN_LOW (Số môn nợ dưới biên hợp lệ) | 18 | 3.0 | -1 | 5 | Không hợp lệ (Số môn nợ = -1 < 0) | X5 |
| 9 | TC_INV_MN_HIGH (Số môn nợ vượt quá biên hợp lệ) | 18 | 3.0 | 4 | 5 | Không hợp lệ (Số môn nợ = 4 > 3) | X6 |
| 10 | TC_INV_HK_LOW (Học kỳ dưới biên hợp lệ) | 18 | 3.0 | 2 | 0 | Không hợp lệ (Học kỳ = 0 < 1) | X7 |
| 11 | TC_INV_HK_HIGH (Học kỳ vượt quá biên hợp lệ) | 18 | 3.0 | 2 | 11 | Không hợp lệ (Học kỳ = 11 > 10) | X8 |

---

## Câu 4. Triển khai kiểm thử tự động

**Điểm:** 3 điểm

Hãy viết chương trình kiểm tra logic của hàm:

```python
ValidateDangKy(tinChi, gpa, monNo, hocKy)
```

Hàm trả về:

- `True` nếu tất cả đầu vào hợp lệ.
- `False` nếu có ít nhất một đầu vào không hợp lệ.

Sinh viên có thể chọn một trong các ngôn ngữ sau:

| Ngôn ngữ | Framework gợi ý |
|---|---|
| Python | `unittest` hoặc `pytest` |
| Java | `JUnit` |
| C# | `NUnit` hoặc `xUnit` |

### Yêu cầu bắt buộc

1. Viết hàm `ValidateDangKy`.
2. Viết ít nhất 2 unit test cho trường hợp biên.
3. Các unit test phải dựa trên giá trị biên đã xác định ở Câu 2.
4. Có ít nhất:
   - 1 test case hợp lệ tại biên.
   - 1 test case không hợp lệ ngoài biên.

### Bài làm Câu 4: Mã nguồn và kết quả kiểm thử tự động

#### 1. Hàm `ValidateDangKy` (Lưu trong file `validate.py`)

```python
def ValidateDangKy(tinChi: int, gpa: float, monNo: int, hocKy: int) -> bool:
    """
    Hàm kiểm tra tính hợp lệ của yêu cầu đăng ký học phần.
    
    Các điều kiện hợp lệ:
    - Số tín chỉ (tinChi): Từ 10 đến 25 (bao gồm cả 10 và 25)
    - Điểm trung bình tích lũy (gpa): Từ 2.0 đến 4.0 (bao gồm cả 2.0 và 4.0)
    - Số môn nợ (monNo): Từ 0 đến 3 (bao gồm cả 0 và 3)
    - Học kỳ hiện tại (hocKy): Từ 1 đến 10 (bao gồm cả 1 và 10)
    
    Trả về:
    - True nếu tất cả điều kiện đều đúng.
    - False nếu có ít nhất một điều kiện sai.
    """
    if not (10 <= tinChi <= 25):
        return False
    if not (2.0 <= gpa <= 4.0):
        return False
    if not (0 <= monNo <= 3):
        return False
    if not (1 <= hocKy <= 10):
        return False
    return True
```

#### 2. Unit Test (Lưu trong file `test_validate.py`)

```python
import unittest
from validate import ValidateDangKy

class TestValidateDangKy(unittest.TestCase):
    
    def run_validation_test(self, test_name, tinChi, gpa, monNo, hocKy, expected):
        actual = ValidateDangKy(tinChi, gpa, monNo, hocKy)
        print(f"[{test_name}] Input: tinChi={tinChi:2d}, gpa={gpa:.1f}, monNo={monNo:2d}, hocKy={hocKy:2d} -> Expected: {expected}, Actual: {actual}")
        self.assertEqual(actual, expected)
        
    # 1. Các trường hợp hợp lệ (Valid Cases / Nominal & Boundary)
    def test_valid_nominal(self):
        """Kiểm thử giá trị đại diện (nominal values) - Mong đợi: True"""
        self.run_validation_test("TC_VAL_NOM ", 18, 3.0, 2, 5, True)
        
    def test_valid_min_boundaries(self):
        """Kiểm thử tất cả các giá trị tại biên dưới (min) - Mong đợi: True"""
        self.run_validation_test("TC_VAL_MIN ", 10, 2.0, 0, 1, True)
        
    def test_valid_max_boundaries(self):
        """Kiểm thử tất cả các giá trị tại biên trên (max) - Mong đợi: True"""
        self.run_validation_test("TC_VAL_MAX ", 25, 4.0, 3, 10, True)

    def test_valid_near_boundaries(self):
        """Kiểm thử các giá trị sát biên (min+ và max-) - Mong đợi: True"""
        self.run_validation_test("TC_VAL_MIN+", 11, 2.1, 1, 2, True)
        self.run_validation_test("TC_VAL_MAX-", 24, 3.9, 2, 9, True)

    # 2. Các trường hợp không hợp lệ (Invalid Cases / Out of Boundary)
    def test_invalid_tinChi_below_min(self):
        """Số tín chỉ nhỏ hơn biên dưới (9 < 10) - Mong đợi: False"""
        self.run_validation_test("TC_INV_TC_L", 9, 3.0, 2, 5, False)
        
    def test_invalid_tinChi_above_max(self):
        """Số tín chỉ lớn hơn biên trên (26 > 25) - Mong đợi: False"""
        self.run_validation_test("TC_INV_TC_H", 26, 3.0, 2, 5, False)

    def test_invalid_gpa_below_min(self):
        """Điểm GPA nhỏ hơn biên dưới (1.9 < 2.0) - Mong đợi: False"""
        self.run_validation_test("TC_INV_GPA_L", 18, 1.9, 2, 5, False)
        
    def test_invalid_gpa_above_max(self):
        """Điểm GPA lớn hơn biên trên (4.1 > 4.0) - Mong đợi: False"""
        self.run_validation_test("TC_INV_GPA_H", 18, 4.1, 2, 5, False)

    def test_invalid_monNo_below_min(self):
        """Số môn nợ nhỏ hơn biên dưới (-1 < 0) - Mong đợi: False"""
        self.run_validation_test("TC_INV_MN_L ", 18, 3.0, -1, 5, False)
        
    def test_invalid_monNo_above_max(self):
        """Số môn nợ lớn hơn biên trên (4 > 3) - Mong đợi: False"""
        self.run_validation_test("TC_INV_MN_H ", 18, 3.0, 4, 5, False)

    def test_invalid_hocKy_below_min(self):
        """Học kỳ nhỏ hơn biên dưới (0 < 1) - Mong đợi: False"""
        self.run_validation_test("TC_INV_HK_L ", 18, 3.0, 2, 0, False)
        
    def test_invalid_hocKy_above_max(self):
        """Học kỳ lớn hơn biên trên (11 > 10) - Mong đợi: False"""
        self.run_validation_test("TC_INV_HK_H ", 18, 3.0, 2, 11, False)

if __name__ == '__main__':
    unittest.main()
```

#### 3. Kết quả chạy thử nghiệm (Test Execution Result)

```shell
$ python -m unittest test_validate.py
............
----------------------------------------------------------------------
Ran 12 tests in 0.003s

OK
[TC_INV_GPA_H] Input: tinChi=18, gpa=4.1, monNo= 2, hocKy= 5 -> Expected: False, Actual: False
[TC_INV_GPA_L] Input: tinChi=18, gpa=1.9, monNo= 2, hocKy= 5 -> Expected: False, Actual: False
[TC_INV_HK_H ] Input: tinChi=18, gpa=3.0, monNo= 2, hocKy=11 -> Expected: False, Actual: False
[TC_INV_HK_L ] Input: tinChi=18, gpa=3.0, monNo= 2, hocKy= 0 -> Expected: False, Actual: False
[TC_INV_MN_H ] Input: tinChi=18, gpa=3.0, monNo= 4, hocKy= 5 -> Expected: False, Actual: False
[TC_INV_MN_L ] Input: tinChi=18, gpa=3.0, monNo=-1, hocKy= 5 -> Expected: False, Actual: False
[TC_INV_TC_H] Input: tinChi=26, gpa=3.0, monNo= 2, hocKy= 5 -> Expected: False, Actual: False
[TC_INV_TC_L] Input: tinChi= 9, gpa=3.0, monNo= 2, hocKy= 5 -> Expected: False, Actual: False
[TC_VAL_MAX ] Input: tinChi=25, gpa=4.0, monNo= 3, hocKy=10 -> Expected: True, Actual: True
[TC_VAL_MIN ] Input: tinChi=10, gpa=2.0, monNo= 0, hocKy= 1 -> Expected: True, Actual: True
[TC_VAL_MIN+] Input: tinChi=11, gpa=2.1, monNo= 1, hocKy= 2 -> Expected: True, Actual: True
[TC_VAL_MAX-] Input: tinChi=24, gpa=3.9, monNo= 2, hocKy= 9 -> Expected: True, Actual: True
[TC_VAL_NOM ] Input: tinChi=18, gpa=3.0, monNo= 2, hocKy= 5 -> Expected: True, Actual: True
```

---



# PHẦN B. BẢNG CHẤM ĐIỂM CHI TIẾT

---

## Câu 1. Lớp tương đương: 2 điểm

| Tiêu chí | Điểm |
|---|---:|
| Xác định đúng lớp hợp lệ cho 4 biến | 0.8 |
| Xác định đúng lớp không hợp lệ nhỏ hơn min | 0.4 |
| Xác định đúng lớp không hợp lệ lớn hơn max | 0.4 |
| Có đặt tag rõ ràng cho các lớp | 0.4 |
| **Tổng** | **2.0** |

---

## Câu 2. Giá trị biên: 2 điểm

| Tiêu chí | Điểm |
|---|---:|
| Xác định đúng biên cho số tín chỉ | 0.5 |
| Xác định đúng biên cho GPA | 0.5 |
| Xác định đúng biên cho số môn nợ | 0.5 |
| Xác định đúng biên cho học kỳ | 0.5 |
| **Tổng** | **2.0** |

---

## Câu 3. Test case: 3 điểm

| Tiêu chí | Điểm |
|---|---:|
| Có tối thiểu 8 test case | 0.5 |
| Có test case hợp lệ | 0.5 |
| Có test case không hợp lệ | 0.5 |
| Có test case tại biên hoặc gần biên | 0.5 |
| Expected result rõ ràng, có lý do khi không hợp lệ | 0.5 |
| Có tag được bao phủ | 0.5 |
| **Tổng** | **3.0** |

---

## Câu 4. Unit test: 3 điểm

| Tiêu chí | Điểm |
|---|---:|
| Viết đúng hàm `ValidateDangKy` | 1.0 |
| Có sử dụng framework unit test | 0.5 |
| Có ít nhất 2 test case biên | 0.5 |
| Có ít nhất 1 case hợp lệ tại biên | 0.5 |
| Có ít nhất 1 case không hợp lệ ngoài biên | 0.5 |
| **Tổng** | **3.0** |

---

# PHẦN C. NHẬN XÉT

## 1. Vì sao cần tag?

Tag giúp theo dõi test case nào đã bao phủ lớp nào hoặc biên nào.

Ví dụ:

| Tag | Ý nghĩa |
|---|---|
| V1 | Số tín chỉ hợp lệ |
| X1 | Số tín chỉ nhỏ hơn min |
| X2 | Số tín chỉ lớn hơn max |
| B1 | Số tín chỉ tại min |
| B5 | Số tín chỉ tại max |

Khi thiết kế test case, sinh viên có thể ghi:

| Test case | Tag bao phủ |
|---|---|
| TC01 | V1, V2, V3, V4 |
| TC02 | B1, B6, B11, B16 |
| TC04 | X1 |

---


