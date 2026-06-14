import bootstrap from './tests/e2e/bootstrap.cjs';

/** @type {CodeceptJS.MainConfig} */
export const config = {
  tests: './tests/e2e/**/*_test.js',
  output: './tests/e2e/output',
  helpers: {
    Playwright: {
      url: process.env.BASE_URL || 'https://localhost:5173',
      show: process.env.HEADLESS !== 'true',
      browser: 'chromium',
      waitForNavigation: 'networkidle',
      waitForTimeout: 15000,
      windowSize: '1600x1200',
      ignoreHTTPSErrors: true
    }
  },
  bootstrap: bootstrap,
  include: {
    I: './tests/e2e/steps_file.js'
  },
  name: 'nova-ticketbooking',
  plugins: {
    screenshotOnFail: {
      enabled: true
    },
    auth: {
      enabled: true,
      saveToFile: true,
      inject: 'loginAs',
      users: {
        admin: {
          login: (I) => {
            I.amOnPage('/auth/login');
            I.fillField('input[placeholder="Email của bạn"]', 'admin_test@novaticket.com');
            I.fillField('input[placeholder="Mật khẩu"]', process.env.APP_TEST_ADMIN_PASSWORD);
            I.click('form button[type="submit"]');
            I.wait(2);
          },
          check: (I) => {
            I.amOnPage('/admin/dashboard');
            I.dontSeeInCurrentUrl('/auth/login');
          }
        },
        staff: {
          login: (I) => {
            I.amOnPage('/auth/login');
            I.fillField('input[placeholder="Email của bạn"]', 'staff_test@novaticket.com');
            I.fillField('input[placeholder="Mật khẩu"]', process.env.APP_TEST_STAFF_PASSWORD);
            I.click('form button[type="submit"]');
            I.wait(2);
          },
          check: (I) => {
            I.amOnPage('/staff/dashboard');
            I.dontSeeInCurrentUrl('/auth/login');
          }
        },
        customer: {
          login: (I) => {
            I.amOnPage('/auth/login');
            I.fillField('input[placeholder="Email của bạn"]', 'customer_test@novaticket.com');
            I.fillField('input[placeholder="Mật khẩu"]', process.env.APP_TEST_CUSTOMER_PASSWORD);
            I.click('form button[type="submit"]');
            I.wait(2);
          },
          check: (I) => {
            I.amOnPage('/profile');
            I.dontSeeInCurrentUrl('/auth/login');
          }
        }
      }
    }
  }
};
