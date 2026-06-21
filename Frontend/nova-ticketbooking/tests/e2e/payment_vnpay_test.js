import { readFileSync } from 'node:fs';

const testDataUrl = new URL('./output/test-data.json', import.meta.url);

function loadTestData() {
  const data = JSON.parse(readFileSync(testDataUrl, 'utf8'));
  if (!data.movie_id || !data.showtime_id) {
    throw new Error('test-data.json chưa có movie_id/showtime_id. Hãy chạy Newman seed trước.');
  }
  return data;
}
