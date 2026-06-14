Feature('Skeleton E2E');

Scenario('Verify Homepage Load', ({ I }) => {
  I.amOnPage('/');
  I.waitForText('NovaTicket', 15); // Chờ tối đa 15s cho React render xong logo trên CI
  I.see('NovaTicket');
});
