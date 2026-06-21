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

