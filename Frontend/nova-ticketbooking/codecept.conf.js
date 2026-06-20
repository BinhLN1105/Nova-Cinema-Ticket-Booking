import bootstrap from './tests/e2e/bootstrap.cjs';

const fetchSession = async (I) => {
  const result = await I.executeScript(() => {
    const raw = localStorage.getItem('nova-auth');
    // Encode to base64 safely supporting Unicode/special characters
    const encoded = raw ? btoa(unescape(encodeURIComponent(raw))) : '';
    return {
      token: encoded,
      hostname: window.location.hostname
    };
  });
  return [
    {
      name: 'nova-auth-dummy-cookie',
      value: result.token,
      domain: result.hostname || 'localhost',
      path: '/'
    }
  ];
};

const restoreSession = async (I, cookies) => {
  const dummyCookie = cookies && cookies.find && cookies.find(c => c.name === 'nova-auth-dummy-cookie');
  let token = null;
  if (dummyCookie && dummyCookie.value) {
    try {
      token = decodeURIComponent(escape(atob(dummyCookie.value)));
    } catch (e) {
      console.error('[Session Restore] Base64 decode failed:', e.message);
    }
  }
  await I.amOnPage('/');
  await I.executeScript((val) => {
    if (val) {
      localStorage.setItem('nova-auth', val);
    } else {
      localStorage.clear();
    }
  }, token);
};

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
    },
    REST: {
      endpoint: 'http://localhost:8080',
      timeout: 10000
    }
  },
  bootstrap: bootstrap,
  include: {
    I: './tests/e2e/steps_file.js'
  },
  mocha: {
    reporter: 'mochawesome',
    reporterOptions: {
      reportDir: './tests/e2e/output',
      reportFilename: 'e2e-report',
      inlineAssets: true,
      overwrite: true,
      html: true,
      json: true
    }
  },
  name: 'nova-ticketbooking',
  plugins: {
    screenshotOnFail: {
      enabled: true
    },
    auth: {
      enabled: true,
      saveToFile: false,
      inject: 'loginAs',
      users: {
        admin: {
          login: (I) => {
            I.amOnPage('/');
            I.executeScript(() => {
              localStorage.clear();
              window.location.href = '/auth/login';
            });
            I.waitForElement('input[placeholder="Email của bạn"]', 15);
            I.fillField('input[placeholder="Email của bạn"]', 'admin_test@novaticket.com');
            I.fillField('input[placeholder="Mật khẩu"]', process.env.APP_TEST_ADMIN_PASSWORD || 'AdminPassword123!');
            I.click('form button[type="submit"]');
            I.waitForElement('aside', 15);
            I.wait(1);
          },
          check: (I) => {
            I.amOnPage('/admin/dashboard');
            I.wait(2);
            I.dontSeeInCurrentUrl('/auth/login');
          },
          fetch: fetchSession,
          restore: restoreSession
        },
        staff: {
          login: (I) => {
            I.amOnPage('/');
            I.executeScript(() => {
              localStorage.clear();
              window.location.href = '/auth/login';
            });
            I.waitForElement('input[placeholder="Email của bạn"]', 15);
            I.fillField('input[placeholder="Email của bạn"]', 'staff_test@novaticket.com');
            I.fillField('input[placeholder="Mật khẩu"]', process.env.APP_TEST_STAFF_PASSWORD || 'StaffPassword123!');
            I.click('form button[type="submit"]');
            I.waitForElement('aside', 15);
            I.wait(1);
          },
          check: (I) => {
            I.amOnPage('/staff/dashboard');
            I.wait(2);
            I.dontSeeInCurrentUrl('/auth/login');
          },
          fetch: fetchSession,
          restore: restoreSession
        },
        customer: {
          login: (I) => {
            I.amOnPage('/');
            I.executeScript(() => {
              localStorage.clear();
              window.location.href = '/auth/login';
            });
            I.waitForElement('input[placeholder="Email của bạn"]', 15);
            I.fillField('input[placeholder="Email của bạn"]', 'customer_test@novaticket.com');
            I.fillField('input[placeholder="Mật khẩu"]', process.env.APP_TEST_CUSTOMER_PASSWORD || 'CustomerPassword123!');
            I.click('form button[type="submit"]');
            I.waitForElement('header', 15);
            I.wait(1);
          },
          check: (I) => {
            I.amOnPage('/profile');
            I.wait(2);
            I.dontSeeInCurrentUrl('/auth/login');
          },
          fetch: fetchSession,
          restore: restoreSession
        }
      }
    }
  }
};
