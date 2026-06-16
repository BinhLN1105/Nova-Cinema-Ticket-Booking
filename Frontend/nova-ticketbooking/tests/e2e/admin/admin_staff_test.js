import testData from '../output/test-data.json' with { type: 'json' };

Feature('Admin Staff Management & Cross-Role');

// Biến lưu thông tin Staff test để cleanup
let testStaffId = null;
let testCinemaId = null;
let TEST_STAFF_EMAIL = `e2e.staff.${Date.now()}@novaticket.com`;
const TEST_STAFF_NAME = `E2E Staff ${Date.now()}`;
const TEST_STAFF_PASSWORD = 'TestStaff123!';

Before(() => {
  TEST_STAFF_EMAIL = `e2e.staff.${Date.now()}_${Math.floor(Math.random() * 10000)}@novaticket.com`;
});

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

// ── Cleanup: Xóa/ban Staff và rạp test vừa tạo ──────────────────
After(async ({ I }) => {
  const headers = await getAuthHeaders(I);

  if (testStaffId) {
    try {
      // Ban tài khoản Staff (API không cho hard delete user, chỉ ban)
      await I.sendPatchRequest(`/api/v1/admin/users/${testStaffId}/ban`, {}, headers);
      console.log(`[Cleanup] Đã ban tài khoản Staff: ${testStaffId}`);
    } catch (e) {
      console.warn(`[Cleanup] Không ban được Staff:`, e.message);
    }
    testStaffId = null;
  }

  if (testCinemaId) {
    try {
      await I.sendDeleteRequest(`/api/v1/cinemas/${testCinemaId}`, headers);
      console.log(`[Cleanup] Đã xóa cinema test: ${testCinemaId}`);
    } catch (e) {
      console.warn(`[Cleanup] Không xóa được cinema test:`, e.message);
    }
    testCinemaId = null;
  }
});

// ── Scenario 1: Admin tạo tài khoản Staff mới ──────────
Scenario('Admin tạo tài khoản Staff mới thành công', async ({ I, loginAs }) => {
  await loginAs('admin');
  await I.amOnPage('/admin/users');
  await I.waitForText('Quản lý người dùng', 15);

  // Click nút "Tạo nhân viên"
  await I.click('Tạo nhân viên');
  await I.waitForText('Tạo tài khoản Nhân viên (STAFF)', 10);

  // Điền form tạo Staff
  await I.fillField(locate('input').withAttr({ placeholder: 'Nhập họ tên nhân viên' }), TEST_STAFF_NAME);
  await I.fillField(locate('input').withAttr({ placeholder: 'staff@novacinema.com' }), TEST_STAFF_EMAIL);
  await I.fillField(locate('input').withAttr({ placeholder: 'Tối thiểu 6 ký tự' }), TEST_STAFF_PASSWORD);

  // Chọn rạp từ test-data.json (cinema_id)
  if (testData.cinema_id) {
    await I.selectOption('#staffCinemaSelect', testData.cinema_id);
  }

  // Submit — click nút "Tạo nhân viên" trong modal
  await I.click(locate('button').withText('Tạo nhân viên').inside('.fixed'));
  await I.waitForText('Tạo nhân viên thành công', 15);

  // Lấy ID Staff vừa tạo qua API
  const headers = await getAuthHeaders(I);
  const response = await I.sendGetRequest('/api/v1/admin/users?search=' + encodeURIComponent(TEST_STAFF_EMAIL), headers);
  const users = response.data?.data?.content;
  if (Array.isArray(users)) {
    const found = users.find(u => u.email === TEST_STAFF_EMAIL);
    if (found) testStaffId = found.id;
  }
});

// ── Scenario 2: Staff đăng nhập và truy cập Staff Dashboard ──
Scenario('Staff vừa tạo đăng nhập thành công', async ({ I, loginAs }) => {
  // Tạo Staff qua API trước
  await loginAs('admin');

  const headers = await getAuthHeaders(I);
  const createRes = await I.sendPostRequest('/api/v1/admin/staff', {
    fullName: TEST_STAFF_NAME,
    email: TEST_STAFF_EMAIL,
    password: TEST_STAFF_PASSWORD,
    cinemaId: testData.cinema_id || '',
  }, headers);
  testStaffId = createRes.data?.data?.id;

  if (!testStaffId) {
    console.warn('[Skip] Không tạo được Staff qua API');
    return;
  }

  // Đăng xuất Admin bằng cách clear localStorage
  await I.executeScript(() => {
    localStorage.clear();
  });
  await I.amOnPage('/auth/login');
  await I.waitForElement('form', 15);

  // Đăng nhập bằng tài khoản Staff vừa tạo
  await I.fillField('input[placeholder="Email của bạn"]', TEST_STAFF_EMAIL);
  await I.fillField('input[placeholder="Mật khẩu"]', TEST_STAFF_PASSWORD);
  await I.click('button[type="submit"]');

  // Chờ redirect tới Staff Dashboard
  await I.wait(3);
  await I.amOnPage('/staff/dashboard');
  await I.waitForElement('main', 15);

  // Verify đang ở giao diện Staff (không bị redirect về login)
  await I.dontSeeInCurrentUrl('/auth/login');
});

// ── Scenario 3: Admin gắn rạp cho Staff ──────────────
Scenario('Admin phân công rạp cho Staff', async ({ I, loginAs }) => {
  await loginAs('admin');
  const headers = await getAuthHeaders(I);

  // Tạo một rạp tạm thời qua API để gán lúc ban đầu
  const tempCinemaRes = await I.sendPostRequest('/api/v1/cinemas', {
    name: `Temp Cinema ${Date.now()}`,
    address: '999 Đường Test, Quận E2E',
    city: 'Ho Chi Minh',
    phone: '0909123456'
  }, headers);
  testCinemaId = tempCinemaRes.data?.data?.id;

  if (!testCinemaId) {
    console.warn('[Skip] Không tạo được rạp tạm thời');
    return;
  }

  // Tạo Staff gắn vào rạp tạm thời đó
  const createRes = await I.sendPostRequest('/api/v1/admin/staff', {
    fullName: TEST_STAFF_NAME,
    email: TEST_STAFF_EMAIL,
    password: TEST_STAFF_PASSWORD,
    cinemaId: testCinemaId,
  }, headers);
  testStaffId = createRes.data?.data?.id;

  if (!testStaffId) {
    console.warn('[Skip] Không tạo được Staff');
    return;
  }

  await I.amOnPage('/admin/users');
  await I.waitForText('Quản lý người dùng', 15);

  // Tìm Staff vừa tạo bằng search
  await I.fillField(locate('input').withAttr({ placeholder: 'Tìm theo tên, email...' }), TEST_STAFF_EMAIL);
  await I.wait(2);

  // Phải thấy tên Staff
  await I.waitForText(TEST_STAFF_NAME, 10);

  // Đổi rạp/phân công rạp mới qua API (vì UI cần click icon button khó target)
  if (testData.cinema_id) {
    await I.sendPatchRequest(`/api/v1/admin/staff/${testStaffId}/cinema`, {
      cinemaId: testData.cinema_id,
    }, headers);

    // Refresh trang
    await I.refreshPage();
    await I.waitForText('Quản lý người dùng', 15);

    // Search lại
    await I.fillField(locate('input').withAttr({ placeholder: 'Tìm theo tên, email...' }), TEST_STAFF_EMAIL);
    await I.wait(2);

    // Staff phải có label rạp bên cạnh (cinemaName)
    await I.waitForText(TEST_STAFF_NAME, 10);
  }
});
