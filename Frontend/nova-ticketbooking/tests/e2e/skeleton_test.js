Feature('Skeleton E2E');

Scenario('Verify Homepage Load', ({ I }) => {
  I.amOnPage('/');
  I.see('NovaTicket');
});
