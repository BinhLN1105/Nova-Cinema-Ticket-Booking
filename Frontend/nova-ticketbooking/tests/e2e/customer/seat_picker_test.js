import fs from 'node:fs';
import net from 'node:net';
import assert from 'node:assert/strict';

Feature('Customer Seat Picker');

const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080';
const REDIS_HOST = process.env.REDIS_HOST || '127.0.0.1';
const REDIS_PORT = Number(process.env.REDIS_PORT || 6379);

// Local Docker dÃ¹ng máº­t kháº©u máº·c Ä‘á»‹nh nÃ y.
// Redis service trÃªn GitHub Actions khÃ´ng cÃ³ máº­t kháº©u.
// HÃ m bÃªn dÆ°á»›i há»— trá»£ Ä‘Æ°á»£c cáº£ hai trÆ°á»ng há»£p.
const REDIS_PASSWORD = process.env.REDIS_PASSWORD || 'novaRedis2026';

let testData;
let temporaryLockedSeatId = null;

function loadTestData() {
  return JSON.parse(
    fs.readFileSync(
      new URL('../output/test-data.json', import.meta.url),
      'utf8',
    ),
  );
}

async function getSeatMap(showtimeId) {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/showtimes/${showtimeId}/seats`,
  );

  assert.equal(
    response.ok,
    true,
    `KhÃ´ng láº¥y Ä‘Æ°á»£c seat map. HTTP status: ${response.status}`,
  );

  const body = await response.json();

  assert.equal(body.success, true, 'API seat map tráº£ success=false');
  assert.ok(body.data?.seats?.length, 'Seat map khÃ´ng cÃ³ dá»¯ liá»‡u gháº¿');

  return body.data;
}

function encodeRedisCommand(parts) {
  return (
    `*${parts.length}\r\n` +
    parts
      .map((part) => {
        const value = String(part);
        return `$${Buffer.byteLength(value)}\r\n${value}\r\n`;
      })
      .join('')
  );
}

/**
 * Gá»­i lá»‡nh trá»±c tiáº¿p tá»›i Redis báº±ng RESP.
 *
 * Lá»‡nh AUTH luÃ´n Ä‘Æ°á»£c gá»­i trÆ°á»›c:
 * - Local: AUTH thÃ nh cÃ´ng vÃ¬ Redis cÃ³ máº­t kháº©u.
 * - CI: AUTH cÃ³ thá»ƒ tráº£ lá»—i vÃ¬ Redis khÃ´ng Ä‘áº·t máº­t kháº©u,
 *   nhÆ°ng lá»‡nh chÃ­nh phÃ­a sau váº«n Ä‘Æ°á»£c thá»±c thi.
 */
function executeRedisCommand(parts) {
  return new Promise((resolve, reject) => {
    const socket = net.createConnection({
      host: REDIS_HOST,
      port: REDIS_PORT,
    });

    let responseBuffer = '';
    let completed = false;

    const finish = (error, result) => {
      if (completed) return;
      completed = true;
      socket.end();

      if (error) {
        reject(error);
      } else {
        resolve(result);
      }
    };

    socket.setTimeout(10000);

    socket.on('connect', () => {
      socket.write(
        encodeRedisCommand(['AUTH', REDIS_PASSWORD]),
      );

      socket.write(encodeRedisCommand(parts));
    });

    socket.on('data', (chunk) => {
      responseBuffer += chunk.toString('utf8');

      /*
       * AUTH vÃ  lá»‡nh chÃ­nh Ä‘á»u tráº£ simple response:
       * +OK
       * -ERR ...
       * :1
       */
      const responses =
        responseBuffer.match(/[+\-:][^\r\n]*\r\n/g) || [];

      if (responses.length < 2) return;

      const commandResponse = responses[1].trim();

      if (commandResponse.startsWith('-')) {
        finish(
          new Error(`Redis command failed: ${commandResponse}`),
        );
        return;
      }

      finish(null, commandResponse);
    });

    socket.on('timeout', () => {
      finish(new Error('Redis command timed out'));
    });

    socket.on('error', (error) => {
      finish(error);
    });
  });
}

async function lockSeatInRedis(showtimeSeatId) {
  const result = await executeRedisCommand([
    'SETEX',
    `seat_lock:${showtimeSeatId}`,
    '600',
    'KAN-63-SEAT-PICKER-TEST',
  ]);

  assert.equal(result, '+OK', 'KhÃ´ng táº¡o Ä‘Æ°á»£c Redis seat lock');
}

async function releaseSeatInRedis(showtimeSeatId) {
  if (!showtimeSeatId) return;

  await executeRedisCommand([
    'DEL',
    `seat_lock:${showtimeSeatId}`,
  ]);
}

function seatButtonSelector(seatLabel) {
  return `button[title^="${seatLabel}"]`;
}

async function getSelectedSeatCount(I) {
  return I.executeScript(() => {
    const raw = window.localStorage.getItem('booking-storage');

    if (!raw) return 0;

    try {
      const parsed = JSON.parse(raw);
      return parsed?.state?.selectedSeats?.length || 0;
    } catch {
      return 0;
    }
  });
}

async function clearBookingStore(I) {
  await I.executeScript(() => {
    window.localStorage.removeItem('booking-storage');
  });
}

Before(async ({ I, loginAs }) => {
  testData = loadTestData();

  assert.ok(
    testData.showtime_id,
    'test-data.json khÃ´ng cÃ³ showtime_id',
  );

  await loginAs('customer');
  await clearBookingStore(I);
});

After(async ({ I }) => {
  if (temporaryLockedSeatId) {
    await releaseSeatInRedis(temporaryLockedSeatId);
    temporaryLockedSeatId = null;
  }

  await clearBookingStore(I);

  // ThoÃ¡t khá»i luá»“ng Ä‘áº·t vÃ© Ä‘á»ƒ trÃ¡nh giá»¯ tráº¡ng thÃ¡i cho test sau.
  await I.amOnPage('/');
});

Scenario(
  'AVAILABLE chọn được, BOOKED bị bôi xám và không tương tác',
  async ({ I }) => {
    const seatMap = await getSeatMap(testData.showtime_id);

    const availableSeat = seatMap.seats.find(
      (seat) => seat.status === 'AVAILABLE',
    );

    assert.ok(
      availableSeat,
      'Seed Data không có ghế AVAILABLE',
    );

    let bookedSeat = seatMap.seats.find(
      (seat) => seat.status === 'BOOKED',
    );

    let mockedRoutePattern = null;

    if (!bookedSeat) {
      const seatForMock = seatMap.seats.find(
        (seat) =>
          seat.status === 'AVAILABLE' &&
          seat.showtimeSeatId !== availableSeat.showtimeSeatId,
      );

      assert.ok(
        seatForMock,
        'Không có ghế AVAILABLE dự phòng để mock BOOKED',
      );

      bookedSeat = {
        ...seatForMock,
        status: 'BOOKED',
      };

      const mockedSeatMap = {
        ...seatMap,
        seats: seatMap.seats.map((seat) =>
          seat.showtimeSeatId === bookedSeat.showtimeSeatId
            ? bookedSeat
            : seat,
        ),
      };

      mockedRoutePattern =
        `**/api/v1/showtimes/${testData.showtime_id}/seats`;

      I.mockRoute(mockedRoutePattern, (route) =>
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            message: 'Seat map fetched successfully',
            status: 200,
            success: true,
            data: mockedSeatMap,
          }),
        }),
      );
    }

    I.amOnPage(
      `/booking/seats/${testData.showtime_id}`,
    );

    const availableSelector = seatButtonSelector(
      availableSeat.seatLabel,
    );

    const bookedSelector = seatButtonSelector(
      bookedSeat.seatLabel,
    );

    I.waitForElement(availableSelector, 15);
    I.waitForElement(bookedSelector, 15);

    I.click(availableSelector);
    I.wait(1);

    assert.equal(
      await getSelectedSeatCount(I),
      1,
      'Ghế AVAILABLE không được thêm vào danh sách đã chọn',
    );

    I.click(availableSelector);
    I.wait(1);

    assert.equal(
      await getSelectedSeatCount(I),
      0,
      'Ghế AVAILABLE không bỏ chọn được',
    );

    const bookedClass = await I.grabAttributeFrom(
      bookedSelector,
      'class',
    );

    assert.match(
      bookedClass,
      /opacity-40/,
      'Ghế BOOKED chưa được làm mờ',
    );

    assert.match(
      bookedClass,
      /cursor-not-allowed/,
      'Ghế BOOKED chưa hiển thị trạng thái không được phép',
    );

    I.seeElement(`${bookedSelector}:disabled`);

    assert.equal(
      await getSelectedSeatCount(I),
      0,
      'Ghế BOOKED làm thay đổi danh sách ghế đã chọn',
    );

    if (mockedRoutePattern) {
      I.stopMockingRoute(mockedRoutePattern);
    }
  },
);

Scenario(
  'LOCKED bá»‹ bÃ´i xÃ¡m vÃ  khÃ´ng tÆ°Æ¡ng tÃ¡c',
  async ({ I }) => {
    const seatMapBeforeLock = await getSeatMap(
      testData.showtime_id,
    );

    const seatToLock = seatMapBeforeLock.seats.find(
      (seat) => seat.status === 'AVAILABLE',
    );

    assert.ok(
      seatToLock,
      'KhÃ´ng tÃ¬m tháº¥y gháº¿ AVAILABLE Ä‘á»ƒ táº¡o dá»¯ liá»‡u LOCKED',
    );

    temporaryLockedSeatId = seatToLock.showtimeSeatId;

    await lockSeatInRedis(temporaryLockedSeatId);

    const seatMapAfterLock = await getSeatMap(
      testData.showtime_id,
    );

    const lockedSeat = seatMapAfterLock.seats.find(
      (seat) =>
        seat.showtimeSeatId === temporaryLockedSeatId &&
        seat.status === 'LOCKED',
    );

    assert.ok(
      lockedSeat,
      'API chÆ°a tráº£ gháº¿ vá»«a giá»¯ thÃ nh tráº¡ng thÃ¡i LOCKED',
    );

    I.amOnPage(
      `/booking/seats/${testData.showtime_id}`,
    );

    const lockedSelector = seatButtonSelector(
      lockedSeat.seatLabel,
    );

    I.waitForElement(lockedSelector, 15);

    const lockedClass = await I.grabAttributeFrom(
      lockedSelector,
      'class',
    );

    assert.match(
      lockedClass,
      /opacity-40/,
      'Gháº¿ LOCKED chÆ°a Ä‘Æ°á»£c lÃ m má»',
    );

    assert.match(
      lockedClass,
      /cursor-not-allowed/,
      'Gháº¿ LOCKED chÆ°a hiá»ƒn thá»‹ tráº¡ng thÃ¡i khÃ´ng Ä‘Æ°á»£c phÃ©p click',
    );

    I.seeElement(`${lockedSelector}:disabled`);

    assert.equal(
      await getSelectedSeatCount(I),
      0,
      'Gháº¿ LOCKED váº«n cÃ³ thá»ƒ Ä‘Æ°á»£c chá»n',
    );
  },
);

Scenario(
  'BVA: chá»n Ä‘Æ°á»£c 6 gháº¿ vÃ  cháº·n gháº¿ thá»© 7 báº±ng Toast',
  async ({ I }) => {
    const seatMap = await getSeatMap(testData.showtime_id);

    const availableSeats = seatMap.seats
      .filter((seat) => seat.status === 'AVAILABLE')
      .slice(0, 7);

    assert.equal(
      availableSeats.length,
      7,
      'Seed Data pháº£i cÃ³ Ã­t nháº¥t 7 gháº¿ AVAILABLE',
    );

    I.amOnPage(
      `/booking/seats/${testData.showtime_id}`,
    );

    for (let index = 0; index < 6; index += 1) {
      const selector = seatButtonSelector(
        availableSeats[index].seatLabel,
      );

      I.waitForElement(selector, 15);
      I.click(selector);
    }

    I.wait(1);

    assert.equal(
      await getSelectedSeatCount(I),
      6,
      'UI khÃ´ng chá»n Ä‘Æ°á»£c Ä‘Ãºng 6 gháº¿ há»£p lá»‡',
    );

    const seventhSeatSelector = seatButtonSelector(
      availableSeats[6].seatLabel,
    );

    I.click(seventhSeatSelector);
    I.wait(1);

    // Boundary vÆ°á»£t ngÆ°á»¡ng: sá»‘ gháº¿ váº«n pháº£i giá»¯ nguyÃªn lÃ  6.
    assert.equal(
      await getSelectedSeatCount(I),
      6,
      'UI váº«n cho chá»n gháº¿ thá»© 7, vÆ°á»£t giá»›i háº¡n 6 gháº¿',
    );

    // React Hot Toast thÆ°á»ng render thÃ´ng bÃ¡o trong role="status".
    I.waitForElement('[role="status"]', 5);

    const toastMessages = await I.grabTextFromAll(
      '[role="status"]',
    );

    const limitToast = toastMessages.find((message) =>
      /6|tá»‘i Ä‘a|giá»›i háº¡n|háº¡n má»©c/i.test(message),
    );

    assert.ok(
      limitToast,
      `KhÃ´ng tÃ¬m tháº¥y Toast cáº£nh bÃ¡o giá»›i háº¡n 6 gháº¿. Toast nháº­n Ä‘Æ°á»£c: ${toastMessages.join(' | ')}`,
    );
  },
);
