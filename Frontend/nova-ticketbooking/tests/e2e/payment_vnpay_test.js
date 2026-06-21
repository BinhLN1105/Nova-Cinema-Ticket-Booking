import { readFileSync } from 'node:fs';

const testDataUrl = new URL('./output/test-data.json', import.meta.url);

function loadTestData() {
  const data = JSON.parse(readFileSync(testDataUrl, 'utf8'));
  if (!data.movie_id || !data.showtime_id) {
    throw new Error('test-data.json chưa có movie_id/showtime_id. Hãy chạy Newman seed trước.');
  }
  return data;
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
    await page.getByRole('button', { name: new RegExp(`${seededShowtime.startTime.slice(11, 16)}.*Đặt vé`) }).first().click();
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

async function interceptVnpay(I, responseCode) {
  await I.usePlaywrightTo(`intercept the VNPay ${responseCode} callback`, async ({ page }) => {
    await page.route('**/api/v1/payments', async (route) => {
      if (route.request().method() !== 'POST') return route.continue();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { paymentUrl: `/api/v1/payments/vnpay/callback?vnp_ResponseCode=${responseCode}&vnp_TxnRef=e2e-test` },
        }),
      });
    });
    // The real backend callback is GET and redirects to /booking/result.
    await page.route('**/api/v1/payments/vnpay/callback**', async (route) => {
      await route.fulfill({
        status: 302,
        headers: { location: `/booking/result?status=${responseCode === '00' ? 'success' : 'failed'}&vnp_ResponseCode=${responseCode}` },
      });
    });
  });
}

async function payWithVnpay(I) {
  await I.usePlaywrightTo('choose VNPay and proceed', async ({ page }) => {
    await page.getByRole('button', { name: 'VNPay' }).click();
    await page.getByRole('button', { name: 'Tiến hành thanh toán' }).click();
  });
}

Feature('Payment - VNPay callback interception');

Scenario('VNPay success callback redirects to booking confirmation', async ({ I, loginAs }) => {
  loginAs('customer');
  await startCheckout(I, loadTestData());
  await interceptVnpay(I, '00');
  await payWithVnpay(I);
  I.waitForText('Đặt vé thành công!', 20);
  I.seeInCurrentUrl('status=success');
  I.seeInCurrentUrl('vnp_ResponseCode=00');
});

Scenario('VNPay cancellation callback displays payment failure', async ({ I, loginAs }) => {
  loginAs('customer');
  await startCheckout(I, loadTestData());
  await interceptVnpay(I, '24');
  await payWithVnpay(I);
  I.waitForText('Thanh toán thất bại', 20);
  I.seeInCurrentUrl('status=failed');
  I.seeInCurrentUrl('vnp_ResponseCode=24');
});
