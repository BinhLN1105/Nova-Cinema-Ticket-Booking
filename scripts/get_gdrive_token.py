import os
from google_auth_oauthlib.flow import InstalledAppFlow
from dotenv import load_dotenv

def get_refresh_token():
    load_dotenv()
    client_id = os.getenv("GDRIVE_CLIENT_ID")
    client_secret = os.getenv("GDRIVE_CLIENT_SECRET")

    if not client_id or not client_secret or "your_google" in client_id:
        print("❌ Vui lòng điền GDRIVE_CLIENT_ID và GDRIVE_CLIENT_SECRET vào file .env trước khi chạy script này!")
        return

    # Khởi tạo luồng xác thực OAuth2 Desktop
    client_config = {
        "installed": {
            "client_id": client_id,
            "client_secret": client_secret,
            "auth_uri": "https://accounts.google.com/o/oauth2/auth",
            "token_uri": "https://oauth2.googleapis.com/token"
        }
    }

    scopes = ['https://www.googleapis.com/auth/drive.file']
    flow = InstalledAppFlow.from_client_config(client_config, scopes=scopes)
    
    # Mở trình duyệt để xác thực
    print("🌐 Trình duyệt đang mở để bạn đăng nhập Google. Hãy nhấn Allow để cấp quyền...")
    creds = flow.run_local_server(port=0)

    # Lấy Refresh Token
    refresh_token = creds.refresh_token
    if refresh_token:
        # Tự động cập nhật file .env
        env_lines = []
        with open(".env", "r", encoding="utf-8") as f:
            env_lines = f.readlines()

        new_env_lines = []
        for line in env_lines:
            if line.startswith("GDRIVE_REFRESH_TOKEN="):
                new_env_lines.append(f"GDRIVE_REFRESH_TOKEN={refresh_token}\n")
            else:
                new_env_lines.append(line)

        with open(".env", "w", encoding="utf-8") as f:
            f.writelines(new_env_lines)

        print("\n✅ THÀNH CÔNG RỰC RỠ!")
        print(f"🔑 Refresh Token đã được tự động lưu vào file .env!")
        print("Bây giờ bạn có thể chạy xuất báo cáo Excel lên Google Drive cá nhân bình thường!")
    else:
        print("❌ Không lấy được Refresh Token. Bạn hãy chắc chắn đã chọn tài khoản Test và cấp quyền đầy đủ.")

if __name__ == "__main__":
    get_refresh_token()
