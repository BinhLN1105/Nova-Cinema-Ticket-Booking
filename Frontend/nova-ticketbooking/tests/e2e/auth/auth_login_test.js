Feature('Authentication Login');

const customerEmail = 'customer_test@novaticket.com';
const customerPassword = process.env.APP_TEST_CUSTOMER_PASSWORD || 'CustomerPassword123!';
const bannedEmail = 'banned_test@novaticket.com';
const bannedPassword = 'CustomerPassword123!';

Scenario('Test Customer Login Success', ({ I }) => {
  I.amOnPage('/auth/login');
  I.waitForElement('form', 10);
  
  I.fillField('input[placeholder="Email của bạn"]', customerEmail);
  I.fillField('input[placeholder="Mật khẩu"]', customerPassword);
  
  I.click('button[type="submit"]');
  
  I.waitInUrl('/', 15);
  I.seeCurrentUrlEquals('/');
});

Scenario('Test Login Failure 401 with wrong password', ({ I }) => {
  I.amOnPage('/auth/login');
  I.waitForElement('form', 10);
  
  I.fillField('input[placeholder="Email của bạn"]', customerEmail);
  I.fillField('input[placeholder="Mật khẩu"]', 'WrongPassword123!');
  I.click('button[type="submit"]');
  
  // Checks for the API error toast message
  I.waitForText('Tài khoản hoặc mật khẩu không chính xác', 10);
});

Scenario('Test Login Blocked Account', ({ I }) => {
  I.amOnPage('/auth/login');
  I.waitForElement('form', 10);
  
  I.fillField('input[placeholder="Email của bạn"]', bannedEmail);
  I.fillField('input[placeholder="Mật khẩu"]', bannedPassword);
  I.click('button[type="submit"]');
  
  // Checks for the banned account API error toast message
  I.waitForText('Tài khoản đã bị khoá', 10);
});

Scenario('Test Forgot Password and OTP Reset Flow', ({ I }) => {
  I.amOnPage('/auth/login');
  I.waitForElement('form', 10);
  
  // Click forgot password link
  I.click('Quên mật khẩu?');
  
  I.waitForElement('#email', 10);
  I.fillField('#email', customerEmail);
  I.click('button[type="submit"]'); // Submits email to request OTP
  
  // System redirects to Verify OTP page
  I.waitForElement('#otp', 15);
  I.seeInCurrentUrl('/auth/verify-otp');
  
  // Fills the fixed OTP code for tests
  I.fillField('#otp', '123456');
  I.click('button[type="submit"]'); // Submits OTP code
  
  // System redirects to Reset Password page
  I.waitForElement('#password', 15);
  I.seeInCurrentUrl('/auth/reset-password');
  
  // Resets password back to standard password to ensure next test runs are clean
  I.fillField('#password', customerPassword);
  I.fillField('#confirmPassword', customerPassword);
  I.click('button[type="submit"]');
  
  // Checks for success screen
  I.waitForText('Đặt lại mật khẩu thành công!', 10);
  I.click('Đăng nhập ngay');
  
  // Verify redirected back to login page
  I.waitForElement('form', 10);
  I.seeInCurrentUrl('/auth/login');
  
  // Try logging in with the reset password to confirm it works
  I.fillField('input[placeholder="Email của bạn"]', customerEmail);
  I.fillField('input[placeholder="Mật khẩu"]', customerPassword);
  I.click('button[type="submit"]');
  
  I.waitInUrl('/', 15);
  I.seeCurrentUrlEquals('/');
});
