Feature('Authentication Register');

Scenario('Test Register with dynamic valid data and 100 char fullName', ({ I }) => {
  I.amOnPage('/auth/register');
  I.waitForElement('form', 10);
  
  // BVA: Full name exactly 100 characters
  const fullName100 = 'A'.repeat(100);
  const emailDynamic = `bva100_${Date.now()}@test.com`;
  
  I.fillField('input[placeholder="Họ và tên"]', fullName100);
  I.fillField('input[placeholder="Email"]', emailDynamic);
  I.fillField('input[placeholder="Mật khẩu"]', 'UserPassword123!');
  I.fillField('input[placeholder="Xác nhận mật khẩu"]', 'UserPassword123!');
  
  I.click('button[type="submit"]');
  
  // Successful registration auto-logins and redirects to home page '/'
  I.waitInUrl('/', 15);
  I.seeCurrentUrlEquals('/');
});

Scenario('Test Register Validation with dirty emails', ({ I }) => {
  I.amOnPage('/auth/register');
  I.waitForElement('form', 10);
  
  // 1. Missing @ in email
  I.fillField('input[placeholder="Họ và tên"]', 'Validator Test');
  I.fillField('input[placeholder="Email"]', 'invalid-email-format');
  I.fillField('input[placeholder="Mật khẩu"]', 'UserPassword123!');
  I.fillField('input[placeholder="Xác nhận mật khẩu"]', 'UserPassword123!');
  I.click('button[type="submit"]');
  I.see('Email không hợp lệ');

  // 2. Email with @ but no domain
  I.fillField('input[placeholder="Email"]', 'test@');
  I.click('button[type="submit"]');
  I.see('Email không hợp lệ');

  // 3. Password too short
  I.fillField('input[placeholder="Email"]', 'valid@test.com');
  I.fillField('input[placeholder="Mật khẩu"]', '123');
  I.click('button[type="submit"]');
  I.see('Mật khẩu ít nhất 6 ký tự');

  // 4. Confirm password mismatch
  I.fillField('input[placeholder="Mật khẩu"]', 'UserPassword123!');
  I.fillField('input[placeholder="Xác nhận mật khẩu"]', 'MismatchPassword123!');
  I.click('button[type="submit"]');
  I.see('Mật khẩu không khớp');
});
