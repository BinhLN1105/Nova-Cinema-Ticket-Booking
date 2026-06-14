Feature('Skeleton E2E');

Scenario('Verify Homepage Load', ({ I }) => {
  I.amOnPage('/');
  I.waitForText('Trang chủ', 15); // Chờ text "Trang chủ" trong body xuất hiện (đảm bảo React đã mount xong)
  I.see('NovaTicket');
});
