@echo off
echo ======================================================================
echo  NOVA TICKET - BAT DAU CHAY KIEM THU API VA DONG BO GOOGLE DRIVE
echo ======================================================================
echo.

:: Tao thu muc baocaoLocal neu chua ton tai
if not exist baocaoLocal mkdir baocaoLocal

echo [1/2] Dang chay kiem thu API thuc te qua Newman...
call newman run qa-tests/postman/NOVATicket.postman_collection.json -e qa-tests/postman/NovaTicket-Local.postman_environment.json --reporters cli,json --reporter-json-export baocaoLocal/postman-report.json

echo.
echo [2/2] Dang doc ket qua test, ve Excel va dong bo len Google Drive...
python scripts/generate_test_report.py

echo.
echo ======================================================================
echo  HOAN TAT!
echo  Bao cao Excel chi tiet da duoc luu tai thu muc: baocaoLocal
echo  File cung da duoc tai len Google Drive cua ban!
echo ======================================================================
pause
