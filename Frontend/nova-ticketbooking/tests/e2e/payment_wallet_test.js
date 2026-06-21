import { readFileSync } from 'node:fs';

const testDataUrl = new URL('./output/test-data.json', import.meta.url);
const CP_RATE = 1_000;
const MIN_GATEWAY_AMOUNT = 10_000;

function loadTestData() {
  const data = JSON.parse(readFileSync(testDataUrl, 'utf8'));
  if (!data.movie_id || !data.showtime_id || !Number.isFinite(Number(data.customer_reward_points))) {
    throw new Error('test-data.json chưa có dữ liệu payment hợp lệ. Hãy chạy Newman seed trước.');
  }
  return data;
}

function calculateCinePointPayment(totalAmount, balance) {
  const pointsToPayAll = Math.ceil(totalAmount / CP_RATE);
  if (balance >= pointsToPayAll && pointsToPayAll > 0) {
    return { pointsUsed: pointsToPayAll, remainingAmount: 0 };
  }

  let pointsUsed = Math.min(Math.floor(totalAmount / CP_RATE), balance);
  let remainingAmount = totalAmount - (pointsUsed * CP_RATE);

  if (remainingAmount > 0 && remainingAmount < MIN_GATEWAY_AMOUNT) {
    pointsUsed = Math.max(0, Math.min(Math.floor((totalAmount - MIN_GATEWAY_AMOUNT) / CP_RATE), balance));
    remainingAmount = totalAmount - (pointsUsed * CP_RATE);
  }
  return { pointsUsed, remainingAmount };
}

async function startCheckout(I, testData) {
  await I.executeScript(() => localStorage.removeItem('booking-storage'));

  await I.usePlaywrightTo('choose the seeded showtime through the UI', async ({ page }) => {
    await page.goto(`/booking/showtime/${testData.movie_id}`);
    const detailResponse = await page.request.get(`/api/v1/showtimes/${testData.showtime_id}`);
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

    const time = seededShowtime.startTime.slice(11, 16);
    await page.getByRole('button', { name: new RegExp(`${time}.*Đặt vé`) }).first().click();
    await page.waitForURL(`**/booking/seats/${testData.showtime_id}`, { timeout: 20_000 });
  });

  I.waitForText('Chọn ghế ngồi', 20);
  await I.usePlaywrightTo('select an available seat', async ({ page }) => {
    await page.locator('button[title*="—"]:not([disabled])').first().click();
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

async function interceptRemainingVnpayPayment(I) {
  await I.usePlaywrightTo('keep a hybrid wallet payment inside the test browser', async ({ page }) => {
    await page.route('**/api/v1/payments', async (route) => {
      if (route.request().method() !== 'POST') return route.continue();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: { paymentUrl: '/api/v1/payments/vnpay/callback?vnp_ResponseCode=00' } }),
      });
    });
    await page.route('**/api/v1/payments/vnpay/callback**', async (route) => {
      await route.fulfill({
        status: 302,
        headers: { location: '/booking/result?status=success&vnp_ResponseCode=00' },
      });
    });
  });
}

Feature('Payment - CinePoint wallet and loyalty reduction');

Scenario('Customer sees the seeded CinePoint balance, its correct reduction, and a safe result page', async ({ I, loginAs }) => {
  const testData = loadTestData();
  const initialPoints = Number(testData.customer_reward_points);
  loginAs('customer');

  I.amOnPage('/profile');
  I.waitForText(`${initialPoints.toLocaleString('vi-VN')} CP`, 15);
  await startCheckout(I, testData);

  const expected = await I.usePlaywrightTo('verify the UI loyalty conversion before paying', async ({ page }) => {
    const totalText = await page.locator('div').filter({ hasText: /^Tổng tiền/ }).last().locator('span').last().innerText();
    const totalAmount = Number(totalText.replace(/[^0-9]/g, ''));
    const payment = calculateCinePointPayment(totalAmount, initialPoints);
    if (payment.pointsUsed <= 0) throw new Error('Seed customer không có đủ CinePoint để thực hiện luồng ví.');

    await page.getByRole('button', { name: 'Ví CinePoint' }).click();
    await page.getByText(new RegExp(`Dùng ${payment.pointsUsed.toLocaleString('vi-VN')} CP`)).waitFor();
    return payment;
  });

  if (expected.remainingAmount > 0) await interceptRemainingVnpayPayment(I);

  await I.usePlaywrightTo('pay with CinePoint', async ({ page }) => {
    await page.getByRole('button', { name: /Thanh toán|Trừ/ }).last().click();
  });
  I.waitForText('Đặt vé thành công!', 20);
  // BookingResult refreshes the auth store after the callback result is rendered.
  // Keep this page mounted long enough for a hybrid payment's CP deduction to appear.
  if (expected.remainingAmount > 0) I.wait(1);

  I.amOnPage('/profile');
  I.waitForText(`${(initialPoints - expected.pointsUsed).toLocaleString('vi-VN')} CP`, 15);
});
