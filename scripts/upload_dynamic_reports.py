import os
import sys
import io
import zipfile
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload
from dotenv import load_dotenv

# Fix encoding cho Windows console
if sys.stdout.encoding != 'utf-8':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

load_dotenv()

def zip_directory(folder_path, zip_path):
    """Nén toàn bộ thư mục thành file zip để upload lên Drive gọn gàng"""
    print(f"📦 Đang nén thư mục {folder_path} thành {zip_path}...")
    if not os.path.exists(folder_path):
        print(f"❌ Không tìm thấy thư mục {folder_path} để nén!")
        return False
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk(folder_path):
            for file in files:
                file_path = os.path.join(root, file)
                # Lưu đường dẫn tương đối trong zip
                arcname = os.path.relpath(file_path, folder_path)
                zipf.write(file_path, arcname)
    print("✅ Nén thư mục thành công!")
    return True

def upload_file_to_gdrive(file_path, mimetype):
    """Tải file lên Google Drive chung của team, hỗ trợ cả OAuth2 và Service Account"""
    from google.oauth2.credentials import Credentials
    
    if not os.path.exists(file_path):
        print(f"❌ Không tìm thấy file {file_path} để upload!")
        return

    folder_id = os.getenv("GDRIVE_FOLDER_ID")
    if not folder_id:
        print("⚠️ Thiếu cấu hình thư mục Google Drive (GDRIVE_FOLDER_ID). Không thể upload!")
        return

    file_name = os.path.basename(file_path)
    file_metadata = {
        'name': file_name,
        'parents': [folder_id]
    }
    
    media = MediaFileUpload(file_path, mimetype=mimetype, resumable=True)

    # ── PHƯƠNG ÁN A: DÙNG OAUTH2 NHÂN DANH TÀI KHOẢN CÁ NHÂN (Có Refresh Token) ──
    refresh_token = os.getenv("GDRIVE_REFRESH_TOKEN")
    client_id = os.getenv("GDRIVE_CLIENT_ID")
    client_secret = os.getenv("GDRIVE_CLIENT_SECRET")

    if refresh_token and client_id and client_secret:
        print(f"🔄 [OAuth2] Đang thử upload file: {file_name}...")
        try:
            creds = Credentials(
                token=None,
                refresh_token=refresh_token,
                token_uri="https://oauth2.googleapis.com/token",
                client_id=client_id,
                client_secret=client_secret
            )
            service = build('drive', 'v3', credentials=creds)
            service.about().get(fields="user").execute() # test network/token
            
            new_file = service.files().create(
                body=file_metadata,
                media_body=media,
                fields='id'
            ).execute()
            print(f"✅ Đã tải thành công file {file_name} lên Google Drive bằng OAuth2!")
            print(f" ID File: {new_file.get('id')}")
            return True
        except Exception as oauth_error:
            print(f"⚠️ Phương thức OAuth2 gặp lỗi: {oauth_error}")
            print("🔄 Đang tự động chuyển hướng sang phương thức Service Account...")

    # ── PHƯƠNG ÁN B: DÙNG SERVICE ACCOUNT (Dự phòng) ──
    key_path = ".gdrive_key.json"
    raw_key = os.getenv("GDRIVE_SERVICE_ACCOUNT_KEY_RAW")
    if raw_key:
        with open(".gdrive_key.json", "w") as temp_file:
            temp_file.write(raw_key)
        key_path = ".gdrive_key.json"

    if os.path.exists(key_path):
        try:
            print(f"🔄 [Service Account] Đang thử upload file: {file_name}...")
            scopes = ['https://www.googleapis.com/auth/drive']
            creds = service_account.Credentials.from_service_account_file(key_path, scopes=scopes)
            service = build('drive', 'v3', credentials=creds)
            
            new_file = service.files().create(
                body=file_metadata,
                media_body=media,
                fields='id'
            ).execute()
            print(f"✅ Đã tải thành công file {file_name} lên Google Drive bằng Service Account!")
            print(f" ID File: {new_file.get('id')}")
            
            if os.getenv("GDRIVE_SERVICE_ACCOUNT_KEY_RAW") and os.path.exists(".gdrive_key.json"):
                try:
                    os.remove(".gdrive_key.json")
                except Exception:
                    pass
            return True
        except Exception as sa_error:
            print(f"❌ Lỗi khi upload bằng Service Account: {sa_error}")
    else:
        print("❌ Không tìm thấy thông tin xác thực Google Drive hợp lệ!")
    return False

if __name__ == "__main__":
    # 1. Định vị các file báo cáo cần upload
    e2e_report = "baocaoLocal/e2e-report.html"
    jacoco_folder = "baocaoLocal/jacoco-report"
    jacoco_zip = "baocaoLocal/jacoco-coverage-report.zip"

    # 2. Upload E2E Playwright HTML Report
    if os.path.exists(e2e_report):
        upload_file_to_gdrive(e2e_report, "text/html")
    else:
        print("⚠️ Không tìm thấy file báo cáo E2E tại baocaoLocal/e2e-report.html")

    # 3. Nén và Upload JaCoCo Coverage Report
    if os.path.exists(jacoco_folder):
        if zip_directory(jacoco_folder, jacoco_zip):
            upload_file_to_gdrive(jacoco_zip, "application/zip")
            # Dọn dẹp file zip tạm sau khi upload
            try:
                os.remove(jacoco_zip)
            except Exception:
                pass
    else:
        print("⚠️ Không tìm thấy thư mục báo cáo JaCoCo tại baocaoLocal/jacoco-report")
