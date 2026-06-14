Feature('Skeleton E2E');

Scenario('Verify Homepage Load', ({ I }) => {
  I.amOnPage('/');
  I.waitForElement('header', 15); // Chờ thẻ <header> được React render ra body
  I.see('NovaTicket', 'header'); // Chỉ tìm chữ "NovaTicket" trong thẻ header để tránh khớp head title
});
