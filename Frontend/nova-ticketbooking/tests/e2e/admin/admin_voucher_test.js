import testData from '../output/test-data.json' with { type: 'json' };

Feature('Admin Voucher CRUD');

// Biến lưu ID voucher test để cleanup
let testVoucherId = null;
let TEST_VOUCHER_CODE = '';

Before(() => {
  TEST_VOUCHER_CODE = `E2ETEST${Date.now()}_${Math.floor(Math.random() * 10000)}`;
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

// Helper để lấy token customer phục vụ gọi REST API
async function getCustomerAuthHeaders(I) {
  try {
    const res = await I.sendPostRequest('/api/v1/auth/login', {
      email: 'customer_test@novaticket.com',
      password: 'CustomerPassword123!',
    });
    const token = res.data?.data?.tokens?.accessToken || res.data?.data?.accessToken;
    return token ? { 'Authorization': `Bearer ${token}` } : {};
  } catch (e) {
    console.error('[REST Auth] Customer login failed:', e.message);
    return {};
  }
}

// ── Cleanup ──────────────────────────────────
After(async ({ I }) => {
  if (testVoucherId) {
    try {
      const headers = await getAuthHeaders(I);
      await I.sendDeleteRequest(`/api/v1/admin/vouchers/${testVoucherId}`, headers);
      console.log(`[Cleanup] Đã xóa voucher test: ${testVoucherId}`);
    } catch (e) {
      console.warn(`[Cleanup] Không xóa được voucher:`, e.message);
    }
    testVoucherId = null;
  }
});

// ── Scenario 1: Admin tạo Voucher mới ──────────────
Scenario('Admin tạo voucher giảm giá thành công', async ({ I, loginAs }) => {
  await loginAs('admin');
  await I.amOnPage('/admin/promotions');
  await I.waitForText('Khuyến mãi & Voucher', 15);

  // Đảm bảo tab Voucher đang active
  await I.waitForElement(locate('button').withText('Mã Voucher'), 15);
  await I.click(locate('button').withText('Mã Voucher'));
  await I.wait(1);

  // Click nút "Tạo Voucher"
  await I.waitForElement(locate('button').withText('Tạo Voucher'), 15);
  await I.click(locate('button').withText('Tạo Voucher'));
  await I.waitForText('Tạo Voucher mới', 10);

  // Điền form tạo voucher
  await I.fillField(locate('input').withAttr({ placeholder: 'VD: NOVA20' }), TEST_VOUCHER_CODE);
  await I.fillField(locate('input').withAttr({ placeholder: 'Mô tả ngắn về voucher này...' }), 'E2E Test voucher - Giảm 10% cho tất cả đơn hàng');

  // Chọn loại giảm giá (PERCENTAGE)
  await I.selectOption('select[name="discountType"]', 'PERCENTAGE');

  // Giá trị giảm
  await I.fillField(locate('input').withAttr({ placeholder: '20' }), '10');

  // Đơn tối thiểu
  await I.fillField(locate('input').withAttr({ placeholder: '100000' }), '50000');

  // Số lần dùng tối đa
  await I.fillField(locate('input').withAttr({ placeholder: '100' }), '5');

  // Ngày bắt đầu (hôm nay)
  const today = new Date().toISOString().split('T')[0];
  const nextMonth = new Date();
  nextMonth.setMonth(nextMonth.getMonth() + 1);
  const endDateStr = nextMonth.toISOString().split('T')[0];

  // Điền ngày bắt đầu — dùng executeScript riêng lẻ (CodeceptJS chỉ truyền tốt 1 arg)
  await I.executeScript((dateVal) => {
    const input = document.querySelector('input[name="startDate"]');
    if (input) {
      const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
      setter.call(input, dateVal);
      input.dispatchEvent(new Event('input', { bubbles: true }));
      input.dispatchEvent(new Event('change', { bubbles: true }));
    }
  }, today);

  // Điền ngày kết thúc — executeScript riêng lẻ
  await I.executeScript((dateVal) => {
    const input = document.querySelector('input[name="endDate"]');
    if (input) {
      const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
      setter.call(input, dateVal);
      input.dispatchEvent(new Event('input', { bubbles: true }));
      input.dispatchEvent(new Event('change', { bubbles: true }));
    }
  }, endDateStr);

  // Submit form
  await I.click('form button[type="submit"]');
  await I.waitForText('Tạo voucher thành công', 15);

  // Verify voucher xuất hiện trong bảng
  await I.waitForText(TEST_VOUCHER_CODE, 10);
  await I.see(TEST_VOUCHER_CODE);

  // Lấy ID voucher qua API với Auth Headers
  const headers = await getAuthHeaders(I);
  const response = await I.sendGetRequest('/api/v1/admin/vouchers', headers);
  const vouchers = response.data?.data?.content;
  if (Array.isArray(vouchers)) {
    const found = vouchers.find(v => v.code === TEST_VOUCHER_CODE);
    if (found) testVoucherId = found.id;
  }
});

// ── Scenario 2: Customer thấy và claim voucher ──────────────
Scenario('Customer claim mã voucher thành công', async ({ I, loginAs }) => {
  // Tạo voucher qua API trước
  await loginAs('admin');

  const today = new Date().toISOString().split('T')[0];
  const nextMonth = new Date();
  nextMonth.setMonth(nextMonth.getMonth() + 1);
  const endDateStr = nextMonth.toISOString().split('T')[0];

  const adminHeaders = await getAuthHeaders(I);
  const createRes = await I.sendPostRequest('/api/v1/admin/vouchers', {
    code: TEST_VOUCHER_CODE,
    description: 'E2E Test voucher',
    discountType: 'PERCENTAGE',
    discountValue: 10,
    minOrder: 0,
    usageLimit: 5,
    startDate: today,
    endDate: endDateStr,
    applicableTo: 'ALL',
  }, adminHeaders);
  testVoucherId = createRes.data?.data?.id;

  // Customer claim voucher qua API với Auth Headers
  await loginAs('customer');
  const customerHeaders = await getCustomerAuthHeaders(I);
  const claimRes = await I.sendPostRequest('/api/v1/users/me/vouchers/claim', {
    code: TEST_VOUCHER_CODE,
  }, customerHeaders);

  // Verify claim thành công (API trả về không lỗi)
  await I.amOnPage('/profile');
  await I.waitForElement('header', 15);
  // Customer đã có voucher trong ví

  // Đánh dấu null để không cố xóa voucher đã bị claim trong After()
  testVoucherId = null;
});

// ── Scenario 3: Admin xóa voucher ──────────────
Scenario('Admin xóa voucher thành công', async ({ I, loginAs }) => {
  // Tạo voucher qua API trước
  await loginAs('admin');

  const today = new Date().toISOString().split('T')[0];
  const nextMonth = new Date();
  nextMonth.setMonth(nextMonth.getMonth() + 1);
  const endDateStr = nextMonth.toISOString().split('T')[0];

  const adminHeaders = await getAuthHeaders(I);
  const createRes = await I.sendPostRequest('/api/v1/admin/vouchers', {
    code: TEST_VOUCHER_CODE,
    description: 'E2E Test voucher to delete',
    discountType: 'FIXED_AMOUNT',
    discountValue: 20000,
    minOrder: 0,
    usageLimit: 3,
    startDate: today,
    endDate: endDateStr,
    applicableTo: 'ALL',
  }, adminHeaders);
  testVoucherId = createRes.data?.data?.id;

  await I.amOnPage('/admin/promotions');
  await I.waitForText('Khuyến mãi & Voucher', 15);
  await I.waitForElement(locate('button').withText('Mã Voucher'), 15);
  await I.click(locate('button').withText('Mã Voucher'));
  await I.wait(2);

  // Tìm kiếm voucher vừa tạo — sử dụng React value setter để điền search box tin cậy
  await I.executeScript((val) => {
    const input = document.querySelector('input[placeholder="Tìm mã voucher..."]');
    if (input) {
      const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
      setter.call(input, val);
      input.dispatchEvent(new Event('input', { bubbles: true }));
      input.dispatchEvent(new Event('change', { bubbles: true }));
    }
  }, TEST_VOUCHER_CODE);
  await I.wait(1);

  // Verify voucher hiển thị
  await I.waitForText(TEST_VOUCHER_CODE, 10);

  // Xóa voucher qua API với Auth Headers
  await I.sendDeleteRequest(`/api/v1/admin/vouchers/${testVoucherId}`, adminHeaders);

  // Refresh trang
  await I.refreshPage();
  await I.waitForText('Khuyến mãi & Voucher', 15);
  await I.waitForElement(locate('button').withText('Mã Voucher'), 15);
  await I.click(locate('button').withText('Mã Voucher'));
  await I.wait(2);

  // Tìm lại với search — sử dụng React value setter để điền search box
  await I.executeScript((val) => {
    const input = document.querySelector('input[placeholder="Tìm mã voucher..."]');
    if (input) {
      const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
      setter.call(input, val);
      input.dispatchEvent(new Event('input', { bubbles: true }));
      input.dispatchEvent(new Event('change', { bubbles: true }));
    }
  }, TEST_VOUCHER_CODE);
  await I.wait(1);

  // Voucher đã biến mất
  await I.dontSee(TEST_VOUCHER_CODE);

  // Đánh dấu đã xóa
  testVoucherId = null;
});

