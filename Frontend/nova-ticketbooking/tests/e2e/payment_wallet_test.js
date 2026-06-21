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

  await I.usePlaywrightTo('choose the seeded showtime through the UI', async ({ page }) => {
    const showtimesResponse = page.waitForResponse((response) =>
      response.url().includes('/api/v1/showtimes') && response.request().method() === 'GET',
    );
    await page.goto(`/booking/showtime/${testData.movie_id}`);
    const payload = await (await showtimesResponse).json();
    const showtimes = payload.data ?? payload;
    const seededShowtime = showtimes.find((showtime) => showtime.id === testData.showtime_id);
    if (!seededShowtime) throw new Error(`Không tìm thấy showtime seed ${testData.showtime_id}`);

    // UI does not expose the UUID, so the start time is the stable visual identifier.
    const time = seededShowtime.startTime.slice(11, 16);
    const candidates = page.getByRole('button', { name: new RegExp(`${time}\\s*Đặt vé`) });
    await candidates.first().click();
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
  I.click(locate('button').withText(/Thanh toán/));
  I.waitForText('Chọn phương thức thanh toán', 20);
}

Feature('Payment - CinePoint wallet and loyalty reduction');

Scenario('Customer pays a new booking with CinePoint and sees the correct point reduction', async ({ I, loginAs }) => {
  const testData = loadTestData();
  loginAs('customer');
  await startCheckout(I, testData);

  I.see('Ví CinePoint');
  const { totalVnd, pointsUsed } = await I.usePlaywrightTo('verify the point conversion shown on checkout', async ({ page }) => {
    await page.getByRole('button', { name: 'Ví CinePoint' }).click();
    const totalText = await page.locator('div').filter({ hasText: /^Tổng tiền/ }).last().locator('span').last().innerText();
    const totalVnd = toVnd(totalText);
    const pointsUsed = Math.ceil(totalVnd / 1000);
    return { totalVnd, pointsUsed };
  });

  if (Number(testData.customer_reward_points) < pointsUsed) {
    throw new Error(`Seed chỉ có ${testData.customer_reward_points} CP, không đủ thanh toán ${totalVnd.toLocaleString('vi-VN')}đ.`);
  }

  I.see(`Dùng ${pointsUsed.toLocaleString('vi-VN')} CP`);
  I.see('miễn phí');
  await I.usePlaywrightTo('submit CinePoint payment', async ({ page }) => {
    await page.getByRole('button', { name: /Thanh toán .*CP/ }).click();
  });
  I.waitForText('Đặt vé thành công!', 20);

  I.amOnPage('/profile');
  I.waitForText(`${(Number(testData.customer_reward_points) - pointsUsed).toLocaleString('vi-VN')} CP`, 15);
});
