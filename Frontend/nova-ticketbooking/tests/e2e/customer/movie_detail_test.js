import testData from '../output/test-data.json' with { type: 'json' };
import assert from 'node:assert';

Feature('Movie Detail');

// Helper: đăng nhập customer trước khi vào trang booking
async function loginAsCustomer(I) {
    I.amOnPage('/auth/login');
    I.waitForElement('input[type="email"]', 10);
    I.fillField('input[type="email"]', 'voduytuan2802@gmail.com');
    I.fillField('input[type="password"]', 'duytuan2802@');
    I.click('button[type="submit"]');

    // Chờ redirect về trang chủ — dùng waitUrlEquals để match CHÍNH XÁC '/'
    // Không dùng waitInUrl('/') vì '/auth/login' cũng chứa '/' → match sai ngay lập tức
    I.waitUrlEquals('https://localhost:5173/', 15);

    // Buffer nhỏ để token/cookie ghi vào storage hoàn toàn
    I.wait(2);
}

// ─────────────────────────────────────────────
// NHÓM 1: Chi tiết phim
// ─────────────────────────────────────────────

Scenario('Xem chi tiết phim - hiển thị thông tin đầy đủ', async ({ I }) => {

    I.amOnPage('/movies');

    I.waitForText(testData.movie_name, 15);

    // Click vào card phim đầu tiên khớp tên
    I.click(locate('h3').withText(testData.movie_name).first());

    I.wait(3);

    // Xác minh tên phim hiển thị trên trang chi tiết
    I.see(testData.movie_name);

    // Xác minh các khối thông tin cố định của trang chi tiết phim đều hiển thị
    // (label không phụ thuộc vào từng phim cụ thể, nên không cần lấy từ test-data.json)
    I.see('phút'); // Thời lượng phim
    I.see('Nội dung phim');
    I.see('Đội ngũ sản xuất');
    I.see('Đạo diễn');
    I.see('Diễn viên');

    // Poster phải hiển thị
    I.seeElement('img');

    // Ghi chú: theo xác nhận của trưởng nhóm, mục Trailer KHÔNG nằm trong phạm vi
    // Sub-task 4 vì tính năng chưa được FE bổ sung link trailer trên UI. Không
    // viết test cho phần này cho đến khi tính năng được implement.

});

// ─────────────────────────────────────────────
// NHÓM 2: Luồng đặt vé & chọn suất chiếu
// (Tất cả scenario dưới đây đều cần đăng nhập)
// ─────────────────────────────────────────────

Scenario('Đặt vé - điều hướng đúng trang booking khi đã đăng nhập', async ({ I }) => {

    await loginAsCustomer(I);

    I.amOnPage('/movies');

    I.waitForText(testData.movie_name, 15);

    I.click(locate('h3').withText(testData.movie_name).first());

    I.wait(3);

    I.scrollPageToBottom();

    I.click('Đặt vé');

    I.wait(3);

    // Khi đã đăng nhập, phải vào trang booking — không được redirect về login
    I.dontSeeInCurrentUrl('/auth/login');

});

Scenario('Chọn ngày xem phim - bộ lọc ngày hiển thị đúng', async ({ I }) => {

    await loginAsCustomer(I);

    // Truy cập thẳng trang chọn suất chiếu bằng showtime_id từ test-data
    I.amOnPage(`/booking/showtime/${testData.showtime_id}`);

    I.wait(5);

    // Xác minh section "CHỌN NGÀY" xuất hiện
    I.see('CHỌN NGÀY');

    // Xác minh có ít nhất một nút ngày hiển thị
    I.seeElement('button.rounded-2xl');

});

Scenario('Chọn ngày khác - bộ lọc cập nhật theo ngày được chọn', async ({ I }) => {

    await loginAsCustomer(I);

    I.amOnPage(`/booking/showtime/${testData.showtime_id}`);

    I.wait(5);

    // Chờ các nút ngày xuất hiện
    I.waitForElement('button.rounded-2xl', 10);

    // Click vào nút "Thứ 3" (ngày thứ 2 trong danh sách, index bắt đầu từ 1)
    // Dùng XPath để tìm button chứa span text "Thứ 3"
    I.click('//button[contains(@class,"rounded-2xl")][.//span[contains(text(),"Thứ 3")]]');

    I.wait(2);

    // Xác minh nút "Thứ 3" đã được active (có class bg-brand-500)
    I.seeElement('//button[contains(@class,"bg-brand-500")][.//span[contains(text(),"Thứ 3")]]');

});

Scenario('Lọc theo cụm rạp - hiển thị đúng suất chiếu của rạp được chọn', async ({ I }) => {

    await loginAsCustomer(I);

    I.amOnPage(`/booking/showtime/${testData.showtime_id}`);

    I.wait(5);

    // Chờ nút lọc rạp "CINEMA Q12" xuất hiện
    I.waitForElement('//button[contains(text(),"CINEMA Q12")]', 15);

    I.click('//button[contains(text(),"CINEMA Q12")]');

    I.wait(3);

    // Xác minh rạp CINEMA Q12 được highlight (active)
    I.seeElement('//button[contains(text(),"CINEMA Q12") and contains(@class,"bg-brand")]');

});

Scenario('Chọn suất chiếu 19:00 - hiển thị đúng khung giờ', async ({ I }) => {

    await loginAsCustomer(I);

    I.amOnPage('/movies');
    I.waitForText(testData.movie_name, 15);
    I.click(locate('h3').withText(testData.movie_name).first());
    I.wait(3);
    I.scrollPageToBottom();
    I.click('Đặt vé');
    I.wait(5);

    // Chờ trang booking load xong
    I.waitForElement('button.rounded-2xl', 10);

    // Showtime có thể ở bất kỳ ngày nào trong tuần (tùy Newman seed)
    // Duyệt qua từng nút ngày cho đến khi tìm thấy 19:00
    const maxDays = 7;
    for (let i = 1; i <= maxDays; i++) {
        I.click(`(//button[contains(@class,"rounded-2xl")])[${i}]`);
        I.wait(2);
        const hasShowtime = await I.grabNumberOfVisibleElements(
            '//*[contains(text(),"19:00")]'
        );
        if (hasShowtime > 0) break;
    }

    // Xác minh đã tìm thấy 19:00 sau khi duyệt qua các ngày
    I.waitForText('19:00', 10);

    // Click vào suất chiếu 19:00
    I.click(locate('*').withText('19:00').first());

    I.wait(2);
    I.see('19:00');

});

Scenario('Đi tới trang chọn ghế - điều hướng đúng rồi back ngay', async ({ I }) => {

    await loginAsCustomer(I);

    I.amOnPage('/movies');
    I.waitForText(testData.movie_name, 15);
    I.click(locate('h3').withText(testData.movie_name).first());
    I.wait(3);
    I.scrollPageToBottom();
    I.click('Đặt vé');
    I.wait(5);

    // Chờ trang booking load xong
    I.waitForElement('button.rounded-2xl', 10);

    // Duyệt qua từng ngày cho đến khi tìm thấy 19:00
    const maxDays = 7;
    for (let i = 1; i <= maxDays; i++) {
        I.click(`(//button[contains(@class,"rounded-2xl")])[${i}]`);
        I.wait(2);
        const hasShowtime = await I.grabNumberOfVisibleElements(
            '//*[contains(text(),"19:00")]'
        );
        if (hasShowtime > 0) break;
    }

    // Xác minh tìm thấy 19:00
    I.waitForText('19:00', 10);

    // Click vào button cha bọc "19:00" và "ĐẶT VÉ" — đây mới là button navigate
    // Cấu trúc DOM: <button><span class="font-display">19:00</span><span>ĐẶT VÉ</span></button>
    I.click('//span[contains(@class,"font-display") and contains(text(),"19:00")]/ancestor::button[1]');

    I.wait(5);

    const currentUrl = await I.grabCurrentUrl();
    console.log('[T4] URL sau khi click button 19:00:', currentUrl);

    // Xác minh đã điều hướng KHỎI trang booking showtime
    I.dontSeeInCurrentUrl('/booking/showtime');

    // ✅ DoD: Back ngay — tuyệt đối không giữ ghế ở trạng thái LOCKED
    I.executeScript('window.history.back()');

    I.wait(3);

    // Xác nhận đã quay về trang booking
    I.seeInCurrentUrl('/booking');

});

// ─────────────────────────────────────────────
// NHÓM 3: Kiểm thử bộ lọc ngày - ngày quá khứ
// ─────────────────────────────────────────────

Scenario('Ngày quá khứ bị vô hiệu hóa - không thể click chọn', async ({ I }) => {

    await loginAsCustomer(I);

    I.amOnPage(`/booking/showtime/${testData.showtime_id}`);

    I.wait(5);

    // Chờ bộ lọc ngày render xong
    I.waitForElement('button.rounded-2xl', 10);

    // Đếm các nút ngày bị disabled
    const disabledCount = await I.grabNumberOfVisibleElements(
        '//button[contains(@class,"rounded-2xl") and @disabled]'
    );

    // Nếu có ngày disabled thì xác minh không click được
    if (disabledCount > 0) {

        // Thử click vào nút disabled đầu tiên — không được thay đổi UI
        const urlBefore = await I.grabCurrentUrl();

        I.tryTo(() => {
            I.click('//button[contains(@class,"rounded-2xl") and @disabled][1]');
        });

        I.wait(1);

        const urlAfter = await I.grabCurrentUrl();

        // URL không được thay đổi sau khi click nút disabled
        assert.strictEqual(urlBefore, urlAfter, 'URL thay đổi sau khi click ngày disabled — UI bị lỗi!');

        console.log(`[T4] ✅ Tìm thấy ${disabledCount} ngày quá khứ bị disabled đúng cách`);

    } else {

        // Hôm nay là ngày đầu tiên của showtime nên không có ngày quá khứ — vẫn pass
        console.log('[T4] ℹ️ Không có ngày quá khứ trong danh sách — showtime mới tạo hôm nay, bỏ qua assertion disabled');

    }

    // Dù sao cũng xác minh trang booking vẫn hiển thị đúng
    I.see('CHỌN NGÀY');

});