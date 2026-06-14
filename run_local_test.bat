@echo off
echo ======================================================================
echo  NOVA TICKET - BAT DAU CHAY KIEM THU API VA DONG BO GOOGLE DRIVE
echo ======================================================================
echo.

:: Tao thu muc baocaoLocal neu chua ton tai
if not exist baocaoLocal mkdir baocaoLocal

echo [1/3] Dang chay kiem thu API Happy Path E2E qua Newman...
call newman run qa-tests/postman/NOVATicket.postman_collection.json -e qa-tests/postman/environment/NovaTicket-Local.postman_environment.json --export-environment baocaoLocal/updated_env.json --reporters cli,json --reporter-json-export baocaoLocal/postman-report.json

echo.
echo [Trang thai: Check] Dang kiem tra loi API va tu dong dong bo/ACK len Jira...
if not exist node_modules\axios (
    echo [Local Setup] Dang tu dong cai dat thu vien Node.js de dong bo Jira...
    call npm install axios dotenv
)
node scripts/auto_log_jira_bug.js

echo.
echo [2/3] Dang chay kiem thu API Security & BVA qua Newman...
call newman run qa-tests/postman/NOVATicket_Security.postman_collection.json -e baocaoLocal/updated_env.json --reporters cli,json --reporter-json-export baocaoLocal/security-report.json
call newman run qa-tests/postman/NOVATicket_BVA.postman_collection.json -e baocaoLocal/updated_env.json --reporters cli,json --reporter-json-export baocaoLocal/bva-report.json

echo.
echo [3/3] Dang doc ket qua test, ve Excel va dong bo len Google Drive...
python scripts/generate_test_report.py
python scripts/generate_bva-sec_report.py

echo.
echo ======================================================================
echo  HOAN TAT!
echo  Bao cao Excel chi tiet da duoc luu tai thu muc: baocaoLocal
echo  File cung da duoc tai len Google Drive cua ban!
echo ======================================================================
pause
