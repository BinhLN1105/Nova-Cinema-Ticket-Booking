import testData from '../output/test-data.json' with { type: 'json' };

Feature('Admin Movie CRUD');

// Biến lưu ID phim tạo trong test để cleanup
let testMovieId = null;
const TEST_MOVIE_TITLE = `E2E Test Movie ${Date.now()}`;
const TEST_MOVIE_TITLE_S1 = `${TEST_MOVIE_TITLE}-S1`;
const TEST_MOVIE_TITLE_S2 = `${TEST_MOVIE_TITLE}-S2`;
const TEST_MOVIE_TITLE_S2_UPDATED = `${TEST_MOVIE_TITLE_S2}-Updated`;
const TEST_MOVIE_TITLE_S3 = `${TEST_MOVIE_TITLE}-S3`;
const TEST_MOVIE_TITLE_S4 = `${TEST_MOVIE_TITLE}-S4`;

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

// ── Cleanup: Xóa phim do test tạo ra ──────────────────
After(async ({ I }) => {
  if (testMovieId) {
    try {
      const headers = await getAuthHeaders(I);
      await I.sendDeleteRequest(`/api/v1/movies/${testMovieId}`, headers);
      console.log(`[Cleanup] Đã xóa phim test: ${testMovieId}`);
    } catch (e) {
      console.warn(`[Cleanup] Không xóa được phim ${testMovieId}:`, e.message);
    }
    testMovieId = null;
  }
});

// ── Scenario 1: Admin thêm phim mới ──────────────────
Scenario('Admin thêm phim mới thành công', async ({ I, loginAs }) => {
  await loginAs('admin');
  I.amOnPage('/admin/movies');
  I.waitForText('Quản lý phim', 15);

  // Click nút "Thêm phim"
  I.click('Thêm phim');
  I.waitForText('Thêm Phim Mới', 10);

  // Điền form tạo phim
  I.fillField('#movie-title', TEST_MOVIE_TITLE_S1);
  I.clearField('#movie-duration');
  I.fillField('#movie-duration', '120');
  I.selectOption('#movie-rated', 'C13');
  I.selectOption('#movie-status', 'NOW_SHOWING');

  // Điền ngày phát hành (hôm nay) bằng cách set value thông qua React's internal value tracker và trigger change event
  const today = new Date().toISOString().split('T')[0];
  I.executeScript((dateVal) => {
    const el = document.querySelector('#movie-release-date');
    if (el) {
      const nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
      nativeInputValueSetter.call(el, dateVal);
      el.dispatchEvent(new Event('input', { bubbles: true }));
    }
  }, today);

  // Submit form
  I.click(locate('button').withText('Thêm mới'));
  I.waitForText('Thêm phim thành công', 15);

  // Verify phim xuất hiện trong bảng
  I.waitForText(TEST_MOVIE_TITLE_S1, 10);
  I.see(TEST_MOVIE_TITLE_S1);

  // Lấy ID phim vừa tạo qua API để dùng cho cleanup
  const headers = await getAuthHeaders(I);
  const response = await I.sendGetRequest('/api/v1/movies/admin?search=' + encodeURIComponent(TEST_MOVIE_TITLE_S1), headers);
  const movies = response.data?.data?.content;
  if (movies && movies.length > 0) {
    testMovieId = movies[0].id;
  }
});

// ── Scenario 2: Admin sửa tên phim ──────────────────
Scenario('Admin sửa tên phim thành công', async ({ I, loginAs }) => {
  // Tạo phim trước để có phim sửa
  await loginAs('admin');

  // Tạo phim qua API
  const today = new Date().toISOString().split('T')[0];
  const headers = await getAuthHeaders(I);
  const createRes = await I.sendPostRequest('/api/v1/movies', {
    title: TEST_MOVIE_TITLE_S2,
    duration: 120,
    rated: 'C13',
    status: 'NOW_SHOWING',
    releaseDate: today,
    genreIds: [],
  }, headers);
  testMovieId = createRes.data?.data?.id;

  I.amOnPage('/admin/movies');
  I.waitForText('Quản lý phim', 15);
  I.waitForText(TEST_MOVIE_TITLE_S2, 10);

  // Click nút Edit trên row phim
  // Tìm row chứa tên phim rồi click nút Edit (icon Edit2)
  I.click(locate('tr').withText(TEST_MOVIE_TITLE_S2).find('button').at(2));

  I.waitForText('Cập nhật Phim', 10);
  I.waitForElement('#movie-title', 15);

  // Xóa title cũ và điền title mới
  I.clearField('#movie-title');
  I.fillField('#movie-title', TEST_MOVIE_TITLE_S2_UPDATED);

  // Submit
  I.click(locate('button').withText('Lưu thay đổi'));
  I.waitForText('Cập nhật thành công', 15);

  // Verify title mới hiển thị
  I.waitForText(TEST_MOVIE_TITLE_S2_UPDATED, 10);
});

// ── Scenario 3: Customer thấy phim trên giao diện ──────
Scenario('Customer thấy phim mới trên trang Movies', async ({ I, loginAs }) => {
  // Tạo phim trước
  await loginAs('admin');
  const today = new Date().toISOString().split('T')[0];
  const headers = await getAuthHeaders(I);
  const createRes = await I.sendPostRequest('/api/v1/movies', {
    title: TEST_MOVIE_TITLE_S3,
    duration: 120,
    rated: 'C13',
    status: 'NOW_SHOWING',
    releaseDate: today,
    genreIds: [],
  }, headers);
  testMovieId = createRes.data?.data?.id;

  // Chuyển sang Customer
  await loginAs('customer');
  await I.amOnPage('/movies');
  await I.waitForElement('header', 15);

  // Customer phải thấy tên phim
  await I.waitForText(TEST_MOVIE_TITLE_S3, 15);
});

// ── Scenario 4: Admin xóa phim → Customer không thấy ──
Scenario('Admin xóa phim thì Customer không thấy nữa', async ({ I, loginAs }) => {
  // Tạo phim trước
  await loginAs('admin');
  const today = new Date().toISOString().split('T')[0];
  const headers = await getAuthHeaders(I);
  const createRes = await I.sendPostRequest('/api/v1/movies', {
    title: TEST_MOVIE_TITLE_S4,
    duration: 120,
    rated: 'C13',
    status: 'NOW_SHOWING',
    releaseDate: today,
    genreIds: [],
  }, headers);
  testMovieId = createRes.data?.data?.id;

  await I.amOnPage('/admin/movies');
  await I.waitForText('Quản lý phim', 15);
  await I.waitForText(TEST_MOVIE_TITLE_S4, 10);

  // Click nút Delete trên row phim
  await I.click(locate('tr').withText(TEST_MOVIE_TITLE_S4).find('button').at(3));

  // Xác nhận xóa trong modal
  await I.waitForText('Xác nhận xóa phim', 10);
  await I.click(locate('button').withText('Xóa').inside('.fixed'));
  await I.waitForText('Đã xóa phim', 15);

  // Phim biến mất khỏi bảng admin
  await I.waitForDetached(locate('tr').withText(TEST_MOVIE_TITLE_S4), 15);

  // Đánh dấu đã xóa để After() không xóa lại
  testMovieId = null;

  // Chuyển sang Customer → không thấy phim
  await loginAs('customer');
  await I.amOnPage('/movies');
  await I.waitForElement('header', 15);
  await I.wait(2); // Đợi danh sách phim của Customer render xong
  await I.dontSee(TEST_MOVIE_TITLE_S4);
});
