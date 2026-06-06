#!/usr/bin/env python3
import os
import sys
import re
import json
import time
import base64
import urllib.request
import urllib.error
import urllib.parse
from pathlib import Path

# ── HẰNG SỐ CỦA HỆ THỐNG ──────────────────────────────────────────────────────
APPLICATION_JSON = "application/json"

# ── HÀM GỬI HTTP REQUEST TIÊU CHUẨN (KHÔNG DÙNG REQUESTS) ─────────────────────
def send_request(url, method="GET", headers=None, data=None):
    if headers is None:
        headers = {}
    
    req_data = None
    if data is not None:
        if isinstance(data, (dict, list)):
            req_data = json.dumps(data).encode("utf-8")
            headers["Content-Type"] = APPLICATION_JSON
        else:
            req_data = data.encode("utf-8")
            
    req = urllib.request.Request(url, data=req_data, headers=headers, method=method)
    
    try:
        with urllib.request.urlopen(req) as response:
            res_data = response.read().decode("utf-8")
            if response.headers.get("Content-Type", "").startswith(APPLICATION_JSON):
                return json.loads(res_data)
            return res_data
    except urllib.error.HTTPError as e:
        err_content = e.read().decode("utf-8")
        print(f"❌ HTTP Error {e.code}: {e.reason}")
        print(f"   Response Body: {err_content}")
        raise e
    except urllib.error.URLError as e:
        print(f"❌ URL Connection Error: {e.reason}")
        raise e

# ── HÀM QUÉT ĐỆ QUY TÌM FILE REPORT-TASK.TXT ──────────────────────────────────
def find_report_task_files(start_dir):
    report_files = []
    for path in Path(start_dir).rglob("report-task.txt"):
        # Bỏ qua các file nằm trong thư mục build/cache không mong muốn
        if "node_modules" not in path.parts and ".gradle" not in path.parts:
            report_files.append(path)
    return report_files

# ── HÀM PARSE FILE REPORT-TASK.TXT ───────────────────────────────────────────
def parse_report_task(file_path):
    properties = {}
    with open(file_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and "=" in line:
                key, val = line.split("=", 1)
                properties[key.strip()] = val.strip()
    return properties

# ── HÀM TRÍCH XUẤT JIRA ISSUE KEY TỪ GIT BRANCH HOẶC COMMIT MESSAGE ─────────
def extract_jira_key():
    # 1. Tìm trong tên nhánh Git của PR hoặc Push
    branch_name = os.getenv("GITHUB_HEAD_REF") or os.getenv("GITHUB_REF_NAME") or ""
    print(f"ℹ️ Tên nhánh Git hiện tại: '{branch_name}'")
    match = re.search(r"([A-Z]+-\d+)", branch_name)
    if match:
        return match.group(1).upper()

    # 2. Nếu không thấy, tìm trong commit message gần nhất
    try:
        import subprocess
        commit_msg = subprocess.check_output(
            ["git", "log", "-1", "--pretty=%B"], 
            stderr=subprocess.DEVNULL
        ).decode("utf-8").strip()
        print(f"ℹ️ Commit message gần nhất: '{commit_msg}'")
        match = re.search(r"([A-Z]+-\d+)", commit_msg)
        if match:
            return match.group(1).upper()
    except Exception:
        pass

    return None

# ── HÀM TRÍCH XUẤT CHỮ TỪ JIRA ADF (ATLASSIAN DOCUMENT FORMAT) ────────────────
def extract_adf_text(node):
    if not node or not isinstance(node, dict):
        return ""
    text = ""
    if node.get("type") == "text":
        text += node.get("text", "")
    if "content" in node and isinstance(node["content"], list):
        for child in node["content"]:
            text += extract_adf_text(child) + " "
    return text

# ── HÀM TRÍCH XUẤT RULE VÀ FILE TỪ JIRA TICKET ────────────────────────────────
def extract_rule_and_file_from_jira(jira_domain, auth_header, issue_key):
    print(f"ℹ️ Đang đọc thông tin chi tiết của task {issue_key} từ Jira...")
    url = f"https://{jira_domain}/rest/api/3/issue/{issue_key}"
    headers = {
        "Authorization": auth_header,
        "Accept": APPLICATION_JSON
    }
    
    issue_data = send_request(url, headers=headers)
    fields = issue_data.get("fields", {})
    summary = fields.get("summary", "")
    
    description_node = fields.get("description")
    description_text = ""
    if description_node:
        description_text = extract_adf_text(description_node)
        
    combined_text = f"{summary}\n{description_text}"
    
    # 1. Trích xuất Rule ID (ví dụ: java:S112, python:S117, S1186)
    rule_ids = set()
    # Tìm rule đầy đủ: java:S112
    full_rules = re.findall(r"\b([a-zA-Z0-9]+:S\d+)\b", combined_text)
    for r in full_rules:
        rule_ids.add(r)
    # Tìm rule viết tắt: S112
    short_rules = re.findall(r"\b(S\d{3,4})\b", combined_text)
    for r in short_rules:
        rule_ids.add(r)
        
    # 2. Trích xuất tên File
    files = set()
    found_files = re.findall(r"\b([a-zA-Z0-9_\-/\\]+\.(?:java|py|js|jsx|ts|tsx|xml))\b", combined_text)
    for f in found_files:
        files.add(os.path.basename(f))
        
    print(f"   👉 Rule IDs tìm thấy trên Jira: {list(rule_ids)}")
    print(f"   👉 Files tìm thấy trên Jira: {list(files)}")
    
    return list(rule_ids), list(files)

# ── HÀM POLLING CHỜ SONARCLOUD XỬ LÝ XONG BÁO CÁO (COMPUTE ENGINE) ────────────
def poll_sonar_task(ce_task_url, sonar_token):
    auth_bytes = f"{sonar_token}:".encode("utf-8")
    sonar_auth = f"Basic {base64.b64encode(auth_bytes).decode('utf-8')}"
    headers = {"Authorization": sonar_auth}
    
    print("⏳ Đang bắt đầu vòng lặp polling chờ SonarCloud phân tích báo cáo...")
    timeout = 180  # 3 phút
    start_time = time.time()
    
    while time.time() - start_time < timeout:
        task_info = send_request(ce_task_url, headers=headers)
        task = task_info.get("task", {})
        status = task.get("status")
        print(f"   👉 Trạng thái tác vụ Sonar (ceTask): {status}")
        
        if status == "SUCCESS":
            print("✅ SonarCloud đã phân tích xong báo cáo thành công.")
            return True
        elif status in ["FAILED", "CANCELED"]:
            print(f"❌ Tác vụ SonarCloud thất bại hoặc bị hủy. Trạng thái: {status}")
            return False
            
        time.sleep(10)
        
    print("❌ Quá thời gian chờ (Timeout 3 phút) cho tác vụ phân tích của SonarCloud.")
    return False

# ── HÀM TRUY VẤN LỖI TRÊN SONARCLOUD CHO RULE/FILE CỤ THỂ ─────────────────────
def check_sonar_issues(project_key, rule_ids, file_names, sonar_token):
    auth_bytes = f"{sonar_token}:".encode("utf-8")
    sonar_auth = f"Basic {base64.b64encode(auth_bytes).decode('utf-8')}"
    headers = {"Authorization": sonar_auth}
    
    all_issues = []
    
    # 1. Nếu không tìm thấy rule/file cụ thể nào từ Jira, chúng ta sẽ quét toàn bộ lỗi của project
    if not rule_ids:
        url = f"https://sonarcloud.io/api/issues/search?componentKeys={project_key}&resolved=false"
        print(f"ℹ️ Đang quét toàn bộ lỗi mở trên project Sonar '{project_key}'...")
        res = send_request(url, headers=headers)
        all_issues.extend(res.get("issues", []))
    else:
        # 2. Quét theo các Rule ID cụ thể để tối ưu
        for rule_id in rule_ids:
            url = f"https://sonarcloud.io/api/issues/search?componentKeys={project_key}&resolved=false&rules={rule_id}"
            print(f"ℹ️ Truy vấn lỗi Sonar cho rule: {rule_id}...")
            try:
                res = send_request(url, headers=headers)
                all_issues.extend(res.get("issues", []))
            except Exception:
                pass
                
    # 3. Lọc lại theo file_names nếu có chỉ định file trên Jira
    filtered_issues = []
    if file_names:
        for issue in all_issues:
            component_path = issue.get("component", "")
            is_matching_file = any(f_name.lower() in component_path.lower() for f_name in file_names)
            if is_matching_file:
                filtered_issues.append(issue)
    else:
        filtered_issues = all_issues
        
    return filtered_issues

# ── CÁC HÀM TRỢ GIÚP CHO CHỨC NĂNG CHÍNH ĐỂ GIẢM COGNITIVE COMPLEXITY ────────
def get_jira_config():
    jira_email = os.getenv("JIRA_EMAIL")
    jira_api_token = os.getenv("JIRA_API_TOKEN")
    jira_domain = os.getenv("JIRA_DOMAIN")
    
    if jira_domain:
        jira_domain = jira_domain.replace("https://", "").replace("http://", "").split("/")[0]

    if not all([jira_email, jira_api_token, jira_domain]):
        return None
        
    auth_str = f"{jira_email}:{jira_api_token}".encode("utf-8")
    jira_auth = f"Basic {base64.b64encode(auth_str).decode('utf-8')}"
    
    return {
        "email": jira_email,
        "token": jira_api_token,
        "domain": jira_domain,
        "auth": jira_auth
    }

def find_and_parse_sonar_report():
    report_files = find_report_task_files(".")
    if not report_files:
        print("⚠️ Không tìm thấy file 'report-task.txt' nào trong workspace. Không thể polling trạng thái SonarCloud.")
        return None
        
    report_files.sort(key=os.path.getmtime, reverse=True)
    report_path = report_files[0]
    print(f"ℹ️ Đang đọc file cấu hình scan: {report_path}")
    
    sonar_props = parse_report_task(report_path)
    project_key = sonar_props.get("projectKey")
    ce_task_url = sonar_props.get("ceTaskUrl")
    
    if not project_key or not ce_task_url:
        print("❌ File report-task.txt bị thiếu thuộc tính 'projectKey' hoặc 'ceTaskUrl'.")
        return None
        
    return project_key, ce_task_url

def handle_pass_scenario(jira_domain, jira_auth, issue_key, jira_rules, jira_files, run_url):
    print("🎉 Chúc mừng! Không còn lỗi nào chưa sửa khớp với rule/file được chỉ định.")
    comment_payload = {
        "body": {
            "type": "doc",
            "version": 1,
            "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {"type": "text", "text": "🎉 "},
                        {"type": "text", "text": "[SonarCloud Bot] Check Passed!", "marks": [{"type": "strong"}]},
                        {"type": "text", "text": "\n\nKhông còn lỗi nào chưa sửa liên quan đến:\n"},
                        {"type": "text", "text": f"- Rules: {jira_rules if jira_rules else 'Tất cả'}\n"},
                        {"type": "text", "text": f"- Files: {jira_files if jira_files else 'Tất cả'}\n\n"},
                        {"type": "text", "text": "Mã nguồn sạch lỗi! Sẵn sàng để review và merge.\n"},
                        {"type": "text", "text": "Phiên chạy CI: "},
                        {"type": "text", "text": "Link GitHub Actions Run", "marks": [{"type": "link", "attrs": {"href": run_url}}]}
                    ]
                }
            ]
        }
    }
    
    comment_url = f"https://{jira_domain}/rest/api/3/issue/{issue_key}/comment"
    send_request(comment_url, method="POST", headers={"Authorization": jira_auth}, data=comment_payload)
    print("✅ Đã comment thành công lên Jira.")

def handle_fail_scenario(jira_domain, jira_auth, issue_key, sonar_issues, run_url):
    print("⚠️ Vẫn còn lỗi chưa được sửa. Bắt đầu trả thẻ về cột 'In Progress'...")
    
    # Lấy transitions hợp lệ
    transitions_url = f"https://{jira_domain}/rest/api/3/issue/{issue_key}/transitions"
    res_transitions = send_request(transitions_url, headers={"Authorization": jira_auth})
    transitions = res_transitions.get("transitions", [])
    
    in_progress_transition_id = None
    for t in transitions:
        t_name = t.get("name", "").lower()
        if "progress" in t_name or "reopen" in t_name or "to do" in t_name:
            in_progress_transition_id = t.get("id")
            print(f"   👉 Tìm thấy transition hợp lệ: ID {in_progress_transition_id} ('{t.get('name')}')")
            break
            
    if in_progress_transition_id:
        transition_payload = {"transition": {"id": in_progress_transition_id}}
        send_request(transitions_url, method="POST", headers={"Authorization": jira_auth}, data=transition_payload)
        print(f"🔄 Đã tự động chuyển thẻ {issue_key} về cột 'In Progress'.")
    else:
        print("⚠️ Không tìm thấy transition chuyển về 'In Progress' trong danh sách trạng thái của thẻ.")
        
    # Tạo danh sách lỗi
    issue_details = []
    for i, issue in enumerate(sonar_issues[:5]):
        issue_details.append(f"{i+1}. Rule: {issue.get('rule')} | File: {issue.get('component')} | Line: {issue.get('line')}\n   Msg: {issue.get('message')}")
        
    issues_list_text = "\n".join(issue_details)
    if len(sonar_issues) > 5:
        issues_list_text += f"\n... và còn {len(sonar_issues) - 5} lỗi khác."

    comment_payload = {
        "body": {
            "type": "doc",
            "version": 1,
            "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {"type": "text", "text": "🚨 "},
                        {"type": "text", "text": "[SonarCloud Bot] Check Failed!", "marks": [{"type": "strong"}, {"type": "textColor", "attrs": {"color": "#de350b"}}]},
                        {"type": "text", "text": f"\n\nVẫn phát hiện còn {len(sonar_issues)} lỗi chưa được giải quyết trên file/rule được chỉ định:\n\n"},
                        {"type": "text", "text": issues_list_text, "marks": [{"type": "code"}]},
                        {"type": "text", "text": "\n\nThẻ tự động bị lôi về cột "},
                        {"type": "text", "text": "In Progress", "marks": [{"type": "strong"}]},
                        {"type": "text", "text": ". Vui lòng sửa hết lỗi và push lại.\n"},
                        {"type": "text", "text": "Phiên chạy CI: "},
                        {"type": "text", "text": "Link GitHub Actions Run", "marks": [{"type": "link", "attrs": {"href": run_url}}]}
                    ]
                }
            ]
        }
    }
    
    comment_url = f"https://{jira_domain}/rest/api/3/issue/{issue_key}/comment"
    send_request(comment_url, method="POST", headers={"Authorization": jira_auth}, data=comment_payload)
    print("✅ Đã viết comment báo lỗi chi tiết lên Jira.")

# ── MAIN EXECUTION ────────────────────────────────────────────────────────────
def main():
    print("🚀 ĐANG KHỞI CHẠY BOT KIỂM TRA SONARCLOUD & ĐỒNG BỘ JIRA...")
    
    sonar_token = os.getenv("SONAR_TOKEN")
    if not sonar_token:
        print("❌ Thiếu biến môi trường SONAR_TOKEN")
        sys.exit(1)
        
    jira_config = get_jira_config()
    if not jira_config:
        print("❌ Thiếu các biến môi trường cấu hình Jira (JIRA_EMAIL, JIRA_API_TOKEN, JIRA_DOMAIN)")
        sys.exit(1)
        
    issue_key = extract_jira_key()
    if not issue_key:
        print("⚠️ Không tìm thấy Jira Issue Key trong tên nhánh hoặc commit message. Bot kết thúc tiến trình.")
        sys.exit(0)
        
    print(f"🎯 Đã xác định Jira Task cần đồng bộ: {issue_key}")

    sonar_report = find_and_parse_sonar_report()
    if not sonar_report:
        sys.exit(1)
        
    project_key, ce_task_url = sonar_report

    # Polling chờ SonarCloud phân tích xong
    if not poll_sonar_task(ce_task_url, sonar_token):
        sys.exit(1)

    # Trích xuất File + Rule ID từ Jira Issue
    jira_rules, jira_files = extract_rule_and_file_from_jira(jira_config["domain"], jira_config["auth"], issue_key)

    # Cho phép ghi đè qua biến môi trường để test local
    env_file = os.getenv("TARGET_FILE")
    env_rule = os.getenv("TARGET_RULE")
    if env_file:
        jira_files = [env_file]
    if env_rule:
        jira_rules = [env_rule]

    # NẾU KHÔNG CÓ RULE VÀ FILE NÀO ĐƯỢC CHỈ ĐỊNH -> BỎ QUA GÁC CỔNG AN TOÀN
    if not jira_rules and not jira_files:
        print("ℹ️ Không tìm thấy Rule ID hoặc File nào được chỉ định trong Jira Task. Đây là task thông thường, bỏ qua kiểm tra gác cổng SonarCloud.")
        sys.exit(0)

    # Truy vấn kiểm tra lỗi trên SonarCloud
    sonar_issues = check_sonar_issues(project_key, jira_rules, jira_files, sonar_token)
    print(f"📊 Kết quả: Tìm thấy {len(sonar_issues)} lỗi chưa được giải quyết khớp với mô tả của task {issue_key}.")

    # Lấy thông tin chạy workflow để chèn vào comment
    run_id = os.getenv("GITHUB_RUN_ID")
    repo = os.getenv("GITHUB_REPOSITORY")
    run_url = f"https://github.com/{repo}/runs/{run_id}" if run_id and repo else "Local Run"

    # Xử lý trạng thái Jira dựa trên kết quả kiểm tra
    if len(sonar_issues) == 0:
        handle_pass_scenario(jira_config["domain"], jira_config["auth"], issue_key, jira_rules, jira_files, run_url)
    else:
        handle_fail_scenario(jira_config["domain"], jira_config["auth"], issue_key, sonar_issues, run_url)

if __name__ == "__main__":
    main()
