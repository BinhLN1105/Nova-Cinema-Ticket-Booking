import { readFileSync } from 'node:fs';

const testDataUrl = new URL('./output/test-data.json', import.meta.url);

function loadTestData() {
  const data = JSON.parse(readFileSync(testDataUrl, 'utf8'));
  if (!data.movie_id || !data.showtime_id || !Number.isFinite(Number(data.customer_reward_points))) {
    throw new Error('test-data.json chưa có dữ liệu payment hợp lệ. Hãy chạy Newman seed trước.');
  }
  return data;
}

function toVnd(text) {
  return Number(String(text).replace(/[^0-9]/g, ''));
}

async function startCheckout(I, testData) {
  await I.executeScript(() => localStorage.removeItem('booking-storage'));

  I.amOnPage(`/booking/showtime/${testData.movie_id}`);

  await I.usePlaywrightTo('choose the seeded showtime through the UI', async ({ page }) => {
    const detailResponse = await page.request.get(new URL(`/api/v1/showtimes/${testData.showtime_id}`, page.url()).href);
    if (!detailResponse.ok()) throw new Error(`Không đọc được showtime seed ${testData.showtime_id}.`);
    const detailPayload = await detailResponse.json();
    const seededShowtime = detailPayload.data ?? detailPayload;
    const [year, month, day] = seededShowtime.startTime.slice(0, 10).split('-');
    const dateButton = page.getByRole('button', { name: new RegExp(`${Number(day)}\\s*${month}/${year}`) });
    const responsePromise = page.waitForResponse((res) =>
      res.url().includes(`/api/v1/showtimes?movieId=${testData.movie_id}`) && res.request().method() === 'GET' && res.ok(),
    );
    await dateButton.click();
    await responsePromise;
    await page.getByRole('button', { name: new RegExp(`${seededShowtime.startTime.slice(11, 16)}.*Đặt vé`) }).first().click();
    await page.waitForURL(`**/booking/seats/${testData.showtime_id}`, { timeout: 20_000 });
  });

  I.waitForText('Chọn ghế ngồi', 20);
  await I.usePlaywrightTo('select one available seat', async ({ page }) => {
    const seat = page.locator('button[title*="—"]:not([disabled])').first();
    await seat.click();
  });
  I.click('Tiếp theo');
  I.waitForText('Chọn Bắp nước', 15);
  I.click('Tiếp tục');
  I.waitForText('Xác nhận đặt vé', 20);
  await I.usePlaywrightTo('create the booking from the confirmation UI', async ({ page }) => {
    await page.getByRole('button', { name: /Thanh toán/ }).click();
  });
  I.waitForText('Chọn phương thức thanh toán', 20);
}

Feature('Payment - CinePoint wallet and loyalty reduction');

Scenario('Customer pays a new booking with CinePoint and sees the correct point reduction', async ({ I, loginAs }) => {
  const testData = loadTestData();
  await loginAs('customer');

  const initialPoints = await I.usePlaywrightTo('get initial reward points', async ({ page }) => {
    const element = page.locator('span').filter({ hasText: /CP$/ }).first();
    const text = await element.innerText();
    return Number(text.replace(/[^0-9]/g, ''));
  });

  await startCheckout(I, testData);

  I.see('Ví CinePoint');
  const { totalVnd, pointsUsed } = await I.usePlaywrightTo('verify the point conversion shown on checkout', async ({ page }) => {
    await page.getByRole('button', { name: 'Ví CinePoint' }).click();
    const totalText = await page.locator('div').filter({ hasText: /^Tổng tiền/ }).last().locator('span').last().innerText();
    const totalVnd = toVnd(totalText);
    const pointsUsed = Math.ceil(totalVnd / 1000);
    return { totalVnd, pointsUsed };
  });

  if (initialPoints < pointsUsed) {
    throw new Error(`Tài khoản chỉ có ${initialPoints} CP, không đủ thanh toán ${totalVnd.toLocaleString('vi-VN')}đ.`);
  }

  I.see(`Dùng ${pointsUsed.toLocaleString('vi-VN')} CP`);
  I.see('miễn phí');
  await I.usePlaywrightTo('submit CinePoint payment', async ({ page }) => {
    await page.getByRole('button', { name: /Thanh toán .*CP/ }).click();
  });
  I.waitForText('Đặt vé thành công!', 20);

  I.amOnPage('/profile');
  const expectedPoints = initialPoints - pointsUsed;
  I.waitForText(`${expectedPoints.toLocaleString('vi-VN')} CP`, 15);
});
