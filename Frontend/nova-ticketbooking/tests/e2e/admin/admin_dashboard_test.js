Feature('Admin Dashboard');

// Dashboard chỉ đọc dữ liệu, không tạo gì → không cần cleanup
// After() trống

// ── Scenario 1: Dashboard load không bị trắng ──────────
Scenario('Dashboard load thành công, không blank UI', async ({ I, loginAs }) => {
  await loginAs('admin');
  await I.amOnPage('/admin/dashboard');

  // Chờ heading chính
  await I.waitForText('Dashboard', 15);
  await I.see('Tổng quan hoạt động hệ thống');

  // Verify không phải trang trắng — phải thấy bộ lọc
  await I.seeElement(locate('button').withText('Hôm nay'));
  await I.seeElement(locate('button').withText('7 ngày'));
  await I.seeElement(locate('button').withText('30 ngày'));
});

// ── Scenario 2: Stat cards hiển thị đúng ──────────────
Scenario('Dashboard hiển thị 4 stat cards với dữ liệu', async ({ I, loginAs }) => {
  await loginAs('admin');
  await I.amOnPage('/admin/dashboard');
  await I.waitForText('Dashboard', 15);

  // Chờ stat cards render (không còn skeleton)
  await I.waitForText('Vé đã bán', 20);

  // Verify 4 stat cards
  await I.see('Doanh thu');
  await I.see('Vé đã bán');
  await I.see('Phim đang chiếu');
  await I.see('Người dùng');

  // Verify các card có giá trị (class stat-card phải tồn tại)
  await I.seeNumberOfVisibleElements('.stat-card', 4);
});

// ── Scenario 3: Bộ lọc theo ngày hoạt động ──────────
Scenario('Bộ lọc period hoạt động không lỗi', async ({ I, loginAs }) => {
  await loginAs('admin');
  await I.amOnPage('/admin/dashboard');
  await I.waitForText('Dashboard', 15);
  await I.waitForText('Doanh thu', 15);

  // Click "Hôm nay"
  await I.click(locate('button').withText('Hôm nay'));
  await I.wait(2);
  // Biểu đồ phải reload — heading thay đổi
  await I.see('Doanh thu hôm nay');

  // Click "30 ngày"
  await I.click(locate('button').withText('30 ngày'));
  await I.wait(2);
  await I.see('Doanh thu 30 ngày qua');

  // Click "7 ngày" (trở về mặc định)
  await I.click(locate('button').withText('7 ngày'));
  await I.wait(2);
  await I.see('Doanh thu 7 ngày qua');

  // Click "Tùy chỉnh" → hiện bộ lọc datetime
  await I.click(locate('button').withText('Tùy chỉnh'));
  await I.wait(1);
  await I.seeElement('#custom-start-date');
  await I.seeElement('#custom-end-date');
});

// ── Scenario 4: Biểu đồ doanh thu render ──────────────
Scenario('Biểu đồ doanh thu render không blank', async ({ I, loginAs }) => {
  await loginAs('admin');
  await I.amOnPage('/admin/dashboard');
  await I.waitForText('Dashboard', 15);

  // Chờ chart render
  await I.waitForText('Biểu đồ doanh thu theo ngày', 15);
  await I.waitForElement('svg.recharts-surface', 20);

  // Verify biểu đồ SVG đã render (recharts render SVG)
  await I.seeElement('svg.recharts-surface');

  // Verify section "Phim ăn khách nhất" tồn tại
  await I.see('Phim ăn khách nhất');

  // Verify bảng "Đặt vé gần đây" tồn tại
  await I.see('Đặt vé gần đây');
  await I.seeElement(locate('table').inside(locate('div').withText('Đặt vé gần đây')));
});

// ── Scenario 5: Bộ lọc Cinema selector ──────────────
Scenario('Bộ lọc rạp trên Dashboard hoạt động', async ({ I, loginAs }) => {
  await loginAs('admin');
  await I.amOnPage('/admin/dashboard');
  await I.waitForText('Dashboard', 15);
  await I.waitForText('Vé đã bán', 20);

  // Verify có dropdown "Tất cả rạp" và đợi load xong rạp
  await I.seeElement('select');
  await I.waitForElement('select option:nth-child(2)', 10);

  // Chọn option đầu tiên (rạp cụ thể)
  const optionValue = await I.executeScript(() => {
    const select = document.querySelector('select');
    return select.options[1] ? select.options[1].value : '';
  });
  await I.selectOption('select', optionValue);
  await I.waitForText('Vé đã bán', 20);

  // Dashboard vẫn hiển thị, không bị blank
  await I.see('Doanh thu');
  await I.see('Vé đã bán');
});
