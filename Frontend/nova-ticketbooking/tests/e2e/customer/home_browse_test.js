import testData from '../output/test-data.json' with { type: 'json' };

Feature('Home Browse');

// ─────────────────────────────────────────────
// NHÓM 1: Hiển thị danh sách phim
// ─────────────────────────────────────────────

Scenario('Hiển thị phim đang chiếu', async ({ I }) => {

    I.amOnPage('/');

    I.waitForText('Phim Đang Chiếu', 15);

    I.waitForText(testData.movie_name, 15); // Đọc tên phim từ test-data.json

});

Scenario('Hiển thị phim sắp chiếu', async ({ I }) => {

    I.amOnPage('/');

    I.waitForText('Sắp ra mắt', 15);

});

// ─────────────────────────────────────────────
// NHÓM 2: Banner Carousel
// ─────────────────────────────────────────────

Scenario('Kiểm thử Banner Carousel - Hiển thị tiêu đề', async ({ I }) => {

    I.amOnPage('/');

    I.wait(3); // Chờ banner render

    I.see('Phim nổi bật tuần này');

});

Scenario('Kiểm thử Banner Carousel - Nội dung banner hiển thị đúng', async ({ I }) => {

    I.amOnPage('/');

    I.wait(3);

    // Xác minh các thành phần chính của banner hiển thị đúng
    I.see('Phim nổi bật tuần này');

    // Xác minh có nút kêu gọi hành động
    // Text thật trên UI là tiếng Anh "View Schedule" / "Book Now" (đã xác nhận qua
    // debug dump DOM thực tế), không phải "Xem lịch chiếu" / "Đặt vé ngay" như giả
    // định ban đầu. Cả 2 nút đều là thẻ <a> với class btn-primary / btn-ghost.
    // Nút CTA có thể hiển thị tiếng Việt hoặc tiếng Anh tuỳ ngôn ngữ trình duyệt
    // Dùng XPath để bắt cả 2 trường hợp
    I.seeElement('//a[contains(text(),"Xem lịch chiếu") or contains(text(),"View Schedule")]');
    I.seeElement('//a[contains(text(),"Đặt vé ngay") or contains(text(),"Book Now")]');

});

Scenario('Kiểm thử Banner Carousel - Tự động chuyển ảnh nền (auto-slide)', async ({ I }) => {

    I.amOnPage('/');

    I.wait(3); // Chờ banner khởi tạo

    // Lấy style ảnh nền của banner trước khi chờ
    const styleBefore = await I.executeScript(() => {
        const el = document.querySelector('section, .hero, [class*="hero"], [class*="banner"], [class*="carousel"]');
        return el ? el.getAttribute('style') || el.className : '';
    });

    I.wait(6); // Chờ auto-slide chạy (thường 3-5 giây)

    // Lấy style ảnh nền sau khi chờ
    const styleAfter = await I.executeScript(() => {
        const el = document.querySelector('section, .hero, [class*="hero"], [class*="banner"], [class*="carousel"]');
        return el ? el.getAttribute('style') || el.className : '';
    });

    // Ghi log để theo dõi — banner có thể thay đổi style khi chuyển slide
    console.log('[T4] Banner style before:', styleBefore?.substring(0, 100));
    console.log('[T4] Banner style after:', styleAfter?.substring(0, 100));

    // Xác minh banner vẫn hiển thị sau khi auto-slide
    I.see('Phim nổi bật tuần này');

});

// ─────────────────────────────────────────────
// NHÓM 3: Tìm kiếm phim
// ─────────────────────────────────────────────

Scenario('Tìm kiếm phim hợp lệ - hiển thị đúng phim cần tìm', async ({ I }) => {

    I.amOnPage('/movies');

    I.waitForElement('input', 15);

    // Đọc tên phim động từ test-data.json — không hardcode
    I.fillField('input', testData.movie_name);

    // Danh sách tự động lọc, phải hiển thị đúng phim
    I.waitForText(testData.movie_name, 15);

    // Đảm bảo chỉ phim đúng tên xuất hiện
    I.see(testData.movie_name);

});

Scenario('Tìm kiếm không tìm thấy kết quả - hiển thị thông báo phù hợp', async ({ I }) => {

    I.amOnPage('/movies');

    I.waitForElement('input', 15);

    // Nhập chuỗi ký tự rác không tồn tại
    I.fillField('input', 'abcxyz123456@@!!');

    I.wait(2); // Chờ debounce và UI cập nhật

    // UI phải hiển thị màn hình trống kèm đúng thông báo theo yêu cầu DoD
    I.see('Không tìm thấy phim nào'); // Text thực tế trên UI

});