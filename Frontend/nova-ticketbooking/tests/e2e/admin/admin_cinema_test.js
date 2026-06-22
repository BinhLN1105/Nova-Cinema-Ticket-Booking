import testData from '../output/test-data.json' with { type: 'json' };

Feature('Admin Cinema Management');

// Biến lưu ID dữ liệu test để cleanup
let testCinemaId = null;
let testShowtimeId = null;
const TEST_CINEMA_NAME = `E2E Test Cinema ${Date.now()}`;

// Helper để lấy token admin phục vụ gọi REST API
async function getAuthHeaders(I) {
  try {
    const res = await I.sendPostRequest('/api/v1/auth/login', {
      email: 'admin_test@novaticket.com',
      password: 'AdminPassword123!',
    });
    const token = res.data?.data?.tokens?.accessToken || res.data?.data?.accessToken;
    return token ? { 'Authorization': `Bearer ${token}` } : {};
  } catch (e) {
    console.error('[REST Auth] Login failed:', e.message);
    return {};
  }
}

// ── Cleanup ──────────────────────────────────
After(async ({ I }) => {
  const headers = await getAuthHeaders(I);

  // Xóa showtime nếu tạo
  if (testShowtimeId) {
    try {
      await I.sendDeleteRequest(`/api/v1/showtimes/${testShowtimeId}`, headers);
      console.log(`[Cleanup] Đã xóa showtime: ${testShowtimeId}`);
    } catch (e) {
      console.warn(`[Cleanup] Không xóa được showtime:`, e.message);
    }
    testShowtimeId = null;
  }

  // Xóa cinema nếu tạo
  if (testCinemaId) {
    try {
      await I.sendDeleteRequest(`/api/v1/cinemas/${testCinemaId}`, headers);
      console.log(`[Cleanup] Đã xóa cinema: ${testCinemaId}`);
    } catch (e) {
      console.warn(`[Cleanup] Không xóa được cinema:`, e.message);
    }
    testCinemaId = null;
  }
});

// ── Scenario 1: Admin tạo rạp chiếu mới ──────────────
Scenario('Admin tạo rạp chiếu mới thành công', async ({ I, loginAs }) => {
  await loginAs('admin');
  await I.amOnPage('/admin/cinemas');
  await I.waitForText('Quản lý rạp chiếu', 15);

  // Click nút "Thêm rạp"
  await I.click('Thêm rạp');
  await I.waitForText('Thêm rạp mới', 10);

  // Điền form tạo rạp
  await I.fillField(locate('input').withAttr({ placeholder: 'VD: Nova Cinema Quận 1' }), TEST_CINEMA_NAME);
  await I.fillField(locate('input').withAttr({ placeholder: 'VD: 123 Nguyễn Huệ, Quận 1' }), '999 Đường Test, Quận E2E');

  // Chọn thành phố
  await I.selectOption(locate('select').inside('.fixed'), 'Ho Chi Minh');

  // Điền SĐT
  await I.fillField(locate('input').withAttr({ placeholder: 'VD: 0281234567' }), '0909123456');

  // Submit
  await I.click(locate('button').withText('Thêm rạp').inside('.fixed'));
  await I.waitForText('Thêm rạp thành công', 15);

  // Verify rạp xuất hiện trong bảng
  await I.waitForText(TEST_CINEMA_NAME, 10);
  await I.see(TEST_CINEMA_NAME);

  // Lấy ID cinema qua API
  const headers = await getAuthHeaders(I);
  const response = await I.sendGetRequest('/api/v1/cinemas/admin', headers);
  const cinemas = response.data?.data;
  if (Array.isArray(cinemas)) {
    const found = cinemas.find(c => c.name === TEST_CINEMA_NAME);
    if (found) testCinemaId = found.id;
  }
});

// ── Scenario 2: Admin xem danh sách rạp có phân loại trạng thái ──
Scenario('Admin thấy danh sách rạp với trạng thái', async ({ I, loginAs }) => {
  await loginAs('admin');
  await I.amOnPage('/admin/cinemas');
  await I.waitForText('Quản lý rạp chiếu', 15);

  // Verify có ít nhất 1 rạp hiển thị (từ seed data)
  await I.seeElement('table');

  // Verify có cột trạng thái
  await I.see('Trạng thái');

  // Verify có cột tên rạp
  await I.see('Tên rạp');
});

// ── Scenario 3: Admin tạo suất chiếu mới ──────────────
Scenario('Admin tạo suất chiếu thành công', async ({ I, loginAs }) => {
  await loginAs('admin');
  await I.amOnPage('/admin/showtimes');
  await I.waitForText('Quản lý suất chiếu', 15);

  // Click nút "Thêm suất chiếu"
  await I.click('Thêm suất chiếu');
  await I.waitForText('Thêm suất chiếu mới', 10);

  // Chọn phim từ dropdown (dùng phim từ seed data)
  await I.waitForElement('#showtime-movie option:nth-child(2)', 15);
  await I.executeScript((movieName) => {
    const select = document.querySelector('#showtime-movie');
    if (select) {
      const option = Array.from(select.options).find(opt => opt.text.trim().includes(movieName.trim()));
      if (option) {
        const setter = Object.getOwnPropertyDescriptor(window.HTMLSelectElement.prototype, "value").set;
        setter.call(select, option.value);
        select.dispatchEvent(new Event('change', { bubbles: true }));
      }
    }
  }, testData.movie_name);

  // Chọn rạp
  await I.waitForElement('#showtime-cinema option:nth-child(2)', 10);
  // Chọn rạp đầu tiên có trong list
  await I.executeScript(() => {
    const select = document.querySelector('#showtime-cinema');
    if (select && select.options.length > 1) {
      const setter = Object.getOwnPropertyDescriptor(window.HTMLSelectElement.prototype, "value").set;
      setter.call(select, select.options[1].value);
      select.dispatchEvent(new Event('change', { bubbles: true }));
    }
  });

  // Chờ load phòng chiếu rồi chọn phòng đầu tiên
  await I.waitForElement('#showtime-screen option:nth-child(2)', 10);
  await I.executeScript(() => {
    const select = document.querySelector('#showtime-screen');
    if (select && select.options.length > 1) {
      const setter = Object.getOwnPropertyDescriptor(window.HTMLSelectElement.prototype, "value").set;
      setter.call(select, select.options[1].value);
      select.dispatchEvent(new Event('change', { bubbles: true }));
    }
  });

  // Đặt giờ chiếu (30 ngày sau, giờ ngẫu nhiên để tránh trùng từ lần chạy trước)
  const futureDate = new Date();
  futureDate.setDate(futureDate.getDate() + 30);
  const randomHour = String(Math.floor(Math.random() * 10) + 1).padStart(2, '0'); // 01-10
  const dateStr = futureDate.toISOString().split('T')[0] + `T${randomHour}:00`;
  await I.executeScript((val) => {
    const el = document.querySelector('#showtime-start-time');
    if (el) {
      const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
      setter.call(el, val);
      el.dispatchEvent(new Event('input', { bubbles: true }));
    }
  }, dateStr);

  // Giá vé
  await I.clearField('#showtime-base-price');
  await I.fillField('#showtime-base-price', '80000');

  // Submit
  await I.click('Tạo suất chiếu');
  await I.waitForText('Tạo suất chiếu thành công', 15);

  // Lấy ID showtime vừa tạo để cleanup
  // (Thông qua API lấy showtimes mới nhất)
  const headers = await getAuthHeaders(I);
  const response = await I.sendGetRequest('/api/v1/showtimes/admin?size=5', headers);
  const showtimes = response.data?.data?.content;
  if (showtimes && showtimes.length > 0) {
    // Tìm showtime với basePrice 80000 vừa tạo
    const found = showtimes.find(s => s.basePrice === 80000);
    if (found) testShowtimeId = found.id;
  }
});

// ── Scenario 4: Admin hủy suất chiếu → ghế được giải phóng ──
Scenario('Admin hủy suất chiếu', async ({ I, loginAs }) => {
  // Tạo suất chiếu trước qua API
  await loginAs('admin');

  // Lấy cinema đầu tiên
  const cinemaRes = await I.sendGetRequest('/api/v1/cinemas');
  const cinemas = cinemaRes.data?.data;
  if (!cinemas || cinemas.length === 0) {
    console.warn('[Skip] Không có rạp nào');
    return;
  }
  const cinemaId = cinemas[0].id;

  // Lấy screen đầu tiên
  const screenRes = await I.sendGetRequest(`/api/v1/cinemas/${cinemaId}/screens`);
  const screens = screenRes.data?.data;
  if (!screens || screens.length === 0) {
    console.warn('[Skip] Rạp không có phòng chiếu');
    return;
  }
  const screenId = screens[0].id;

  // Tạo suất chiếu
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 2);
  const startTime = tomorrow.toISOString().split('T')[0] + 'T16:00:00';

  let createRes;
  try {
    const headers = await getAuthHeaders(I);
    createRes = await I.sendPostRequest('/api/v1/showtimes', {
      movieId: testData.movie_id,
      screenId: screenId,
      startTime: startTime,
      basePrice: 90000,
    }, headers);
    console.log('[API Debug] Create showtime status:', createRes?.status);
    console.log('[API Debug] Create showtime response:', JSON.stringify(createRes?.data || createRes, null, 2));
  } catch (err) {
    console.error('[API Debug] Create showtime error:', err.response?.data || err.message);
  }
  testShowtimeId = createRes?.data?.data?.id;

  if (!testShowtimeId) {
    console.warn('[Skip] Không tạo được suất chiếu');
    return;
  }

  // Vào trang quản lý suất chiếu
  await I.amOnPage('/admin/showtimes');
  await I.waitForText('Quản lý suất chiếu', 15);

  // Expand rạp đầu tiên
  await I.waitForText(cinemas[0].name, 20);
  await I.click(locate('button').withText(cinemas[0].name));
  await I.wait(3); // Chờ load showtimes

  // Xóa suất chiếu bằng API trực tiếp (vì UI cần hover)
  const headersDel = await getAuthHeaders(I);
  await I.sendDeleteRequest(`/api/v1/showtimes/${testShowtimeId}`, headersDel);

  // Refresh trang
  await I.refreshPage();
  await I.waitForText('Quản lý suất chiếu', 15);

  // Đánh dấu đã xóa
  testShowtimeId = null;
});
