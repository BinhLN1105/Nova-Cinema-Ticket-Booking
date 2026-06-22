import fs from 'node:fs';
import net from 'node:net';
import assert from 'node:assert/strict';

Feature('Customer Seat Picker');

const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080';
const REDIS_HOST = process.env.REDIS_HOST || '127.0.0.1';
const REDIS_PORT = Number(process.env.REDIS_PORT || 6379);

// Local Docker dùng mật khẩu mặc định này.
// Redis service trên GitHub Actions không có mật khẩu.
// Hàm bên dưới hỗ trợ được cả hai trường hợp.
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
    `Không lấy được seat map. HTTP status: ${response.status}`,
  );

  const body = await response.json();

  assert.equal(body.success, true, 'API seat map trả success=false');
  assert.ok(body.data?.seats?.length, 'Seat map không có dữ liệu ghế');

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
 * Gửi lệnh trực tiếp tới Redis bằng RESP.
 *
 * Lệnh AUTH luôn được gửi trước:
 * - Local: AUTH thành công vì Redis có mật khẩu.
 * - CI: AUTH có thể trả lỗi vì Redis không đặt mật khẩu,
 *   nhưng lệnh chính phía sau vẫn được thực thi.
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
       * AUTH và lệnh chính đều trả simple response:
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

  assert.equal(result, '+OK', 'Không tạo được Redis seat lock');
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
    'test-data.json không có showtime_id',
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

  // Thoát khỏi luồng đặt vé để tránh giữ trạng thái cho test sau.
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
  'LOCKED bị bôi xám và không tương tác',
  async ({ I }) => {
    const seatMapBeforeLock = await getSeatMap(
      testData.showtime_id,
    );

    const seatToLock = seatMapBeforeLock.seats.find(
      (seat) => seat.status === 'AVAILABLE',
    );

    assert.ok(
      seatToLock,
      'Không tìm thấy ghế AVAILABLE để tạo dữ liệu LOCKED',
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
      'API chưa trả ghế vừa giữ thành trạng thái LOCKED',
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
      'Ghế LOCKED chưa được làm mờ',
    );

    assert.match(
      lockedClass,
      /cursor-not-allowed/,
      'Ghế LOCKED chưa hiển thị trạng thái không được phép click',
    );

    I.seeElement(`${lockedSelector}:disabled`);

    assert.equal(
      await getSelectedSeatCount(I),
      0,
      'Ghế LOCKED vẫn có thể được chọn',
    );
  },
);

Scenario(
  'BVA: chọn được 6 ghế và chặn ghế thứ 7 bằng Toast',
  async ({ I }) => {
    const seatMap = await getSeatMap(testData.showtime_id);

    const availableSeats = seatMap.seats
      .filter((seat) => seat.status === 'AVAILABLE')
      .slice(0, 7);

    assert.equal(
      availableSeats.length,
      7,
      'Seed Data phải có ít nhất 7 ghế AVAILABLE',
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
      'UI không chọn được đúng 6 ghế hợp lệ',
    );

    const seventhSeatSelector = seatButtonSelector(
      availableSeats[6].seatLabel,
    );

    I.click(seventhSeatSelector);
    I.wait(1);

    // Boundary vượt ngưỡng: số ghế vẫn phải giữ nguyên là 6.
    assert.equal(
      await getSelectedSeatCount(I),
      6,
      'UI vẫn cho chọn ghế thứ 7, vượt giới hạn 6 ghế',
    );

    // React Hot Toast thường render thông báo trong role="status".
    I.waitForElement('[role="status"]', 5);

    const toastMessages = await I.grabTextFromAll(
      '[role="status"]',
    );

    const limitToast = toastMessages.find((message) =>
      /6|tối đa|giới hạn|hạn mức/i.test(message),
    );

    assert.ok(
      limitToast,
      `Không tìm thấy Toast cảnh báo giới hạn 6 ghế. Toast nhận được: ${toastMessages.join(' | ')}`,
    );
  },
);
