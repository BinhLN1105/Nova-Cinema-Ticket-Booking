import fs from 'node:fs';
import assert from 'node:assert/strict';

Feature('Customer Combo And Voucher Validation');

const testData = JSON.parse(
  fs.readFileSync(
    new URL('../output/test-data.json', import.meta.url),
    'utf8',
  ),
);

const COMBOS = [
  {
    id: 'combo-a',
    name: 'Combo A',
    description: 'Combo mềm phục vụ kiểm thử',
    price: 50000,
    imageUrl: null,
  },
  {
    id: 'combo-b',
    name: 'Combo B',
    description: 'Combo mềm phục vụ kiểm thử',
    price: 70000,
    imageUrl: null,
  },
];

const ROUTES = {
  combos: '**/api/v1/combos',
  vouchers: '**/api/v1/users/me/vouchers',
  quote: '**/api/v1/bookings/quote',
  validate: '**/api/v1/vouchers/validate',
};

function apiBody(data, message = 'Success', status = 200) {
  return JSON.stringify({
    message,
    status,
    success: status < 400,
    data,
  });
}

async function seedBookingStorage(I, selectedCombos = {}) {
  await I.amOnPage('/');

  await I.executeScript(
    ({ showtimeId, combos }) => {
      const seatPrice = 100000;
      const comboTotal = Object.values(combos).reduce(
        (sum, item) => sum + item.price * item.quantity,
        0,
      );

      window.localStorage.setItem(
        'booking-storage',
        JSON.stringify({
          state: {
            selectedMovie: {
              id: 'movie-soft',
              title: 'Phim kiểm thử',
            },
            selectedShowtime: {
              id: showtimeId,
              cinemaName: 'Rạp kiểm thử',
              screenName: 'Phòng kiểm thử',
              screenType: '2D',
              startTime: new Date(Date.now() + 3600000).toISOString(),
            },
            selectedDate: new Date().toISOString().slice(0, 10),
            selectedSeats: [
              {
                showtimeSeatId: 'seat-soft-1',
                rowLabel: 'A',
                colNumber: 1,
                price: seatPrice,
                seatType: 'STANDARD',
                status: 'AVAILABLE',
              },
            ],
            selectedCombos: combos,
            appliedVoucher: null,
            subtotal: seatPrice + comboTotal,
            promotionDiscount: 0,
            rankDiscount: 0,
            appliedPromotionName: null,
            discount: 0,
            total: seatPrice + comboTotal,
            originalTotal: seatPrice + comboTotal,
            warningMessage: null,
            expiryTime: Date.now() + 600000,
          },
          version: 0,
        }),
      );
    },
    {
      showtimeId: testData.showtime_id,
      combos: selectedCombos,
    },
  );
}

function mockCombos(I) {
  I.mockRoute(ROUTES.combos, (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: apiBody(COMBOS),
    }),
  );
}

function mockVoucherFlow(I) {
  I.mockRoute(ROUTES.vouchers, (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: apiBody([]),
    }),
  );

  I.mockRoute(ROUTES.quote, (route) => {
    const requestBody = route.request().postDataJSON();
    const hasValidVoucher =
      requestBody?.voucherCode === testData.voucher_code_valid;

    const originalTotal = 100000;
    const discountAmount = hasValidVoucher ? 20000 : 0;

    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: apiBody({
        subtotal: originalTotal,
        promotionDiscountAmount: 0,
        rankDiscountAmount: 0,
        appliedPromotionName: null,
        discountAmount,
        totalAmount: originalTotal - discountAmount,
        totalOriginalAmount: originalTotal,
        warningMessage: null,
      }),
    });
  });

  I.mockRoute(ROUTES.validate, (route) => {
    const requestBody = route.request().postDataJSON();
    const code = requestBody?.code;

    if (code === testData.voucher_code_valid) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: apiBody({
          id: 'voucher-valid-soft',
          code,
          description: 'Giảm 20.000 đồng',
          status: 'AVAILABLE',
          discountType: 'FIXED_AMOUNT',
          discountValue: 20000,
          minOrder: 0,
          maxDiscount: 20000,
          endDate: '2099-12-31T23:59:59',
        }),
      });
    }

    return route.fulfill({
      status: 400,
      contentType: 'application/json',
      body: apiBody(
        null,
        'Mã voucher đã hết hạn',
        400,
      ),
    });
  });
}

async function getStoredComboTotal(I) {
  return I.executeScript(() => {
    const raw = window.localStorage.getItem('booking-storage');
    if (!raw) return 0;

    const parsed = JSON.parse(raw);
    const selectedCombos = parsed?.state?.selectedCombos || {};

    return Object.values(selectedCombos).reduce(
      (sum, item) => sum + (item?.quantity || 0),
      0,
    );
  });
}

async function clearBookingStorage(I) {
  await I.executeScript(() => {
    window.localStorage.removeItem('booking-storage');
  });
}

Before(async ({ I, loginAs }) => {
  assert.ok(
    testData.showtime_id,
    'test-data.json không có showtime_id',
  );
  assert.ok(
    testData.voucher_code_valid,
    'test-data.json không có voucher_code_valid',
  );
  assert.ok(
    testData.voucher_code_expired,
    'test-data.json không có voucher_code_expired',
  );

  await loginAs('customer');
  await seedBookingStorage(I);
});

After(async ({ I }) => {
  await clearBookingStorage(I);
  await I.amOnPage('/');
});

Scenario(
  'Tăng giảm combo và khóa toàn bộ nút cộng khi tổng bằng 10',
  async ({ I }) => {
    mockCombos(I);

    I.amOnPage('/booking/combo');

    const plusA = '[data-testid="combo-plus-combo-a"]';
    const minusA = '[data-testid="combo-minus-combo-a"]';
    const qtyA = '[data-testid="combo-quantity-combo-a"]';
    const plusB = '[data-testid="combo-plus-combo-b"]';
    const qtyB = '[data-testid="combo-quantity-combo-b"]';

    I.waitForElement(plusA, 15);
    I.waitForElement(plusB, 15);

    I.click(plusA);
    I.click(plusA);
    I.see('2', qtyA);

    I.click(minusA);
    I.see('1', qtyA);

    for (let index = 0; index < 5; index += 1) {
      I.click(plusA);
    }

    for (let index = 0; index < 4; index += 1) {
      I.click(plusB);
    }

    I.see('6', qtyA);
    I.see('4', qtyB);

    assert.equal(
      await getStoredComboTotal(I),
      10,
      'Tổng số lượng combo không bằng 10',
    );

    I.seeElement(`${plusA}:disabled`);
    I.seeElement(`${plusB}:disabled`);

    I.stopMockingRoute(ROUTES.combos);
  },
);

Scenario(
  'Voucher hợp lệ làm giảm tổng tiền hiển thị',
  async ({ I }) => {
    mockCombos(I);
    mockVoucherFlow(I);
    await seedBookingStorage(I);

    I.amOnPage('/booking/confirm');

    const totalSelector =
      '.text-gold-400.text-2xl.font-display.font-bold';

    I.waitForElement(totalSelector, 15);
    I.wait(1);

    const totalBefore = await I.grabTextFrom(totalSelector);

    I.click('button.text-sm.text-brand-400.font-bold');
    I.waitForElement('div.fixed input', 10);
    I.fillField('div.fixed input', testData.voucher_code_valid);
    I.click('div.fixed button.bg-brand-500');

    I.wait(2);
    I.see('Giảm giá Voucher');

    const totalAfter = await I.grabTextFrom(totalSelector);

    assert.notEqual(
      totalAfter,
      totalBefore,
      'Voucher hợp lệ không làm thay đổi tổng tiền',
    );

    I.stopMockingRoute(ROUTES.combos);
    I.stopMockingRoute(ROUTES.vouchers);
    I.stopMockingRoute(ROUTES.quote);
    I.stopMockingRoute(ROUTES.validate);
  },
);

Scenario(
  'Voucher hết hạn hiện Toast đỏ và tổng tiền không đổi',
  async ({ I }) => {
    mockCombos(I);
    mockVoucherFlow(I);
    await seedBookingStorage(I);

    I.amOnPage('/booking/confirm');

    const totalSelector =
      '.text-gold-400.text-2xl.font-display.font-bold';

    I.waitForElement(totalSelector, 15);
    I.wait(1);

    const totalBefore = await I.grabTextFrom(totalSelector);

    I.click('button.text-sm.text-brand-400.font-bold');
    I.waitForElement('div.fixed input', 10);
    I.fillField('div.fixed input', testData.voucher_code_expired);
    I.click('div.fixed button.bg-brand-500');

    I.waitForElement('[role="status"]', 10);

    const toastMessages = await I.grabTextFromAll('[role="status"]');

    assert.ok(
      toastMessages.some((message) =>
        /hết hạn|không hợp lệ|voucher/i.test(message),
      ),
      `Không tìm thấy Toast voucher hết hạn: ${toastMessages.join(' | ')}`,
    );

    const totalAfter = await I.grabTextFrom(totalSelector);

    assert.equal(
      totalAfter,
      totalBefore,
      'Voucher hết hạn làm thay đổi tổng tiền',
    );

    I.stopMockingRoute(ROUTES.combos);
    I.stopMockingRoute(ROUTES.vouchers);
    I.stopMockingRoute(ROUTES.quote);
    I.stopMockingRoute(ROUTES.validate);
  },
);
