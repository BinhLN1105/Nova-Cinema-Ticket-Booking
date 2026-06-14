const newman = require('newman');
const fs = require('fs');
const path = require('path');

module.exports = async function() {
  console.log('\x1b[36m%s\x1b[0m', '[E2E Bootstrap] Khởi chạy Newman Seed Data cho môi trường UI...');
  
  const collectionPath = path.resolve(__dirname, '../../../qa-tests/postman/NOVATicket_UI_Seed_Data.postman_collection.json');
  const envPath = path.resolve(__dirname, '../../../qa-tests/postman/environment/NovaTicket-Local.postman_environment.json');
  const outputPath = path.resolve(__dirname, 'output');
  const envOutputPath = path.resolve(outputPath, 'seed-env.json');
  const dataOutputPath = path.resolve(outputPath, 'test-data.json');
  
  // Tạo thư mục output nếu chưa tồn tại
  if (!fs.existsSync(outputPath)) {
    fs.mkdirSync(outputPath, { recursive: true });
  }
  
  // Nếu chưa có file Postman collection, tự động tạo dữ liệu mock để không block setup
  if (!fs.existsSync(collectionPath)) {
    console.warn('\x1b[33m%s\x1b[0m', `[E2E Bootstrap] Không tìm thấy file collection tại: ${collectionPath}`);
    console.warn('\x1b[33m%s\x1b[0m', '[E2E Bootstrap] Tự tạo dữ liệu mock mẫu để không block skeleton...');
    
    const mockData = {
      movie_id: "",
      movie_name: "",
      showtime_id: "",
      cinema_id: "",
      customer_wallet_balance: 0,
      customer_reward_points: 0,
      staff_checkin_code: "",
      cancel_test_booking_code: "",
      voucher_code_valid: "",
      voucher_code_expired: ""
    };
    
    fs.writeFileSync(dataOutputPath, JSON.stringify(mockData, null, 2));
    console.log('[E2E Bootstrap] Đã tạo xong file test-data.json mock.');
    return;
  }

  // Chạy Newman và xuất dữ liệu ra file JSON
  return new Promise((resolve, reject) => {
    newman.run({
      collection: collectionPath,
      environment: envPath,
      reporters: 'cli',
      exportEnvironment: envOutputPath,
      envVar: [
        { key: 'vnpay_hash_secret', value: process.env.VNPAY_HASH_SECRET || '' }
      ]
    }, function (err) {
      if (err) {
        console.error('[E2E Bootstrap] Lỗi chạy Newman:', err);
        return reject(err);
      }
      
      console.log('[E2E Bootstrap] Newman hoàn thành, đang phân tách dữ liệu...');
      try {
        if (!fs.existsSync(envOutputPath)) {
          return reject(new Error(`Không tìm thấy file environment kết quả tại ${envOutputPath}`));
        }
        
        const envJson = JSON.parse(fs.readFileSync(envOutputPath, 'utf-8'));
        const variables = {};
        
        if (envJson.values) {
          envJson.values.forEach(v => {
            variables[v.key] = v.value;
          });
        }
        
        // Map sang format test-data.json
        const finalData = {
          movie_id: variables.movie_id,
          movie_name: variables.movie_name,
          showtime_id: variables.showtime_id,
          cinema_id: variables.cinema_id,
          customer_wallet_balance: Number(variables.customer_wallet_balance),
          customer_reward_points: Number(variables.customer_reward_points),
          staff_checkin_code: variables.staff_checkin_code,
          cancel_test_booking_code: variables.cancel_test_booking_code,
          voucher_code_valid: variables.voucher_code_valid,
          voucher_code_expired: variables.voucher_code_expired
        };
        
        fs.writeFileSync(dataOutputPath, JSON.stringify(finalData, null, 2));
        console.log('[E2E Bootstrap] Gieo dữ liệu thành công! test-data.json sẵn sàng.');
        resolve();
      } catch (parseErr) {
        console.error('[E2E Bootstrap] Lỗi khi xử lý file seed-env.json:', parseErr);
        reject(parseErr);
      }
    });
  });
};
