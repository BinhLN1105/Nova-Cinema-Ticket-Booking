const fs = require('fs');
const axios = require('axios');
require('dotenv').config();

// ── BẢN ĐỒ PHÂN CHIA JIRA ACCOUNT ID CỦA 6 THÀNH VIÊN VỚI GITHUB ACTOR ─────────
const WORK_ALLOCATION = {
  "auth": { 
    name: "Tuấn Võ", 
    jiraId: "712020:dd9b41d9-cfaf-49e0-a2c9-acebb60480e2",
    githubActor: "TuanVo"
  },
  "movies": { 
    name: "trinm3962", 
    jiraId: "712020:1d6b82dd-c27c-40bd-9293-7343c211d3c5",
    githubActor: "trinm3962"
  },
  "cinemas": { 
    name: "Bình", 
    jiraId: "712020:3de7bc42-4e28-4428-bb84-dee097f18ddb",
    githubActor: "BinhLN1105"
  },
  "bookings": { 
    name: "ThangVN0987", 
    jiraId: "712020:1a6e786b-64d1-4a11-b27b-d6cad1b5fee1",
    githubActor: "ThangVN0987"
  },
  "payments": { 
    name: "Lưu Minh Triết", 
    jiraId: "712020:9cb1fb05-2475-4152-87f5-88814e347646",
    githubActor: "TrietLuu"
  },
  "utilities": { 
    name: "Nguyên Vũ", 
    jiraId: "712020:be91c1b0-0851-479d-8c13-1eb60b46c20b",
    githubActor: "VuNguyen"
  }
};

async function processNewmanFailures() {
  // Linh hoạt đường dẫn: kiểm tra thư mục cục bộ và thư mục gốc của GitHub Actions
  let reportPath = 'baocaoLocal/postman-report.json';
  if (!fs.existsSync(reportPath)) {
    reportPath = 'postman-report.json';
  }
  
  if (!fs.existsSync(reportPath)) {
    console.log("❌ Không tìm thấy file báo cáo ở cả baocaoLocal/postman-report.json và postman-report.json");
    return;
  }

  const report = JSON.parse(fs.readFileSync(reportPath, 'utf8'));
  const failures = report.run.failures;

  if (!failures || failures.length === 0) {
    console.log("🎉 Tuyệt vời! Tất cả các testcase API đều ĐẠT (PASS)!");
    return;
  }

  console.log(`🔍 Phát hiện ${failures.length} testcase bị FAIL. Tiến hành log bug lên Jira...`);

  // LẤY THÔNG TIN TỪ GITHUB ACTIONS ĐỂ MINH CHỨNG TÁC GIẢ LỖI
  const commitHash = process.env.GITHUB_SHA ? process.env.GITHUB_SHA.substring(0, 7) : "local";
  const githubActor = process.env.GITHUB_ACTOR || "local-user";
  const runId = process.env.GITHUB_RUN_ID || "";
  const repo = process.env.GITHUB_REPOSITORY || "";
  const runUrl = runId ? `https://github.com/${repo}/actions/runs/${runId}` : "";

  // TÌM JIRA ACCOUNT ID CỦA NGƯỜI VỪA PUSH CODE GÂY LỖI
  let pusherJiraId = null;
  let pusherName = githubActor;
  for (const module in WORK_ALLOCATION) {
    if (WORK_ALLOCATION[module].githubActor && WORK_ALLOCATION[module].githubActor.toLowerCase() === githubActor.toLowerCase()) {
      pusherJiraId = WORK_ALLOCATION[module].jiraId;
      pusherName = WORK_ALLOCATION[module].name;
      break;
    }
  }

  // Tránh log trùng một API lỗi nhiều lần trong cùng một đợt chạy
  const loggedBugs = new Set();

  for (const fail of failures) {
    const apiName = fail.source.name; // Tên kịch bản kiểm thử (vd: "[400] Đăng nhập sai mật khẩu")
    const method = fail.source.request.method; // GET, POST...
    const rawUrl = fail.source.request.url.path.join('/'); // Path URL (vd: api/v1/auth/login)
    const errorMessage = fail.error.message; // Mô tả lỗi kiểm thử từ Postman
    const testScriptCode = fail.error.test; // Đoạn script test bị lỗi

    // CHỐNG LOG BUG JIRA KHI SERVER CHƯA BẬT (BỎ QUA CÁC LỖI KẾT NỐI HỆ THỐNG / LỖI VẬN HÀNH CỦA TESTER)
    const isConnectionError = 
      errorMessage.includes('ECONNREFUSED') || 
      errorMessage.includes('ENOTFOUND') || 
      errorMessage.includes('ECONNRESET') || 
      errorMessage.includes('ETIMEDOUT') ||
      errorMessage.includes('Invalid URI') ||
      errorMessage.includes('connection refused') ||
      errorMessage.includes('Invalid URL');

    if (isConnectionError) {
      console.log(`⚠️ [Jira Bypass] Bỏ qua log bug API /${rawUrl} vì đây là lỗi kết nối hệ thống (Chưa bật server hoặc lỗi mạng), không phải lỗi logic Backend!`);
      continue;
    }

    const bugKey = `${method}_${rawUrl}`;
    if (loggedBugs.has(bugKey)) continue;
    loggedBugs.add(bugKey);

    // PHÂN TÍCH ĐƯỜNG DẪN ĐỂ XÁC ĐỊNH PHÂN HỆ VÀ ASSIGNEE MẶC ĐỊNH
    let moduleKey = "utilities"; // Phân hệ mặc định
    if (rawUrl.includes('auth')) {
      moduleKey = "auth";
    } else if (rawUrl.includes('movies') || rawUrl.includes('genres')) {
      moduleKey = "movies";
    } else if (rawUrl.includes('cinemas') || rawUrl.includes('screens')) {
      moduleKey = "cinemas";
    } else if (rawUrl.includes('bookings') || rawUrl.includes('check-in')) {
      moduleKey = "bookings";
    } else if (rawUrl.includes('payments')) {
      moduleKey = "payments";
    }

    const assignee = WORK_ALLOCATION[moduleKey];

    // CẤU TRÚC PAYLOAD ĐỂ GỬI LÊN JIRA REST API (FORMAT ATOMIC DOCUMENT v1)
    const payload = {
      fields: {
        project: {
          key: process.env.JIRA_PROJECT_KEY || "NOVA"
        },
        summary: `🚨 [Auto-Bug] API Fail: [${method}] /${rawUrl}`,
        description: {
          type: "doc",
          version: 1,
          content: [
            {
              type: "paragraph",
              content: [
                {
                  type: "text",
                  text: `Hệ thống kiểm thử tự động Newman vừa phát hiện lỗi nghiêm trọng trên API:\n\n`
                },
                {
                  type: "text",
                  text: `• Request API: [${method}] /${rawUrl}\n`
                },
                {
                  type: "text",
                  text: `• Kịch bản lỗi: ${apiName}\n`
                },
                {
                  type: "text",
                  text: `• Chi tiết lỗi: ${errorMessage}\n`
                },
                {
                  type: "text",
                  text: `• Script lỗi: ${testScriptCode}\n\n`
                },
                {
                  type: "text",
                  text: `• Người commit gây lỗi: `
                },
                {
                  type: "text",
                  text: `@${githubActor} (${pusherName})\n`,
                  marks: [{ type: "strong" }]
                },
                {
                  type: "text",
                  text: `• Commit Hash: \`${commitHash}\`\n`
                },
                {
                  type: "text",
                  text: `• Phiên chạy CI/CD: `
                },
                {
                  type: "text",
                  text: runUrl ? "Link GitHub Actions Run\n\n" : "Local Run\n\n",
                  marks: runUrl ? [{ type: "link", attrs: { href: runUrl } }] : []
                },
                {
                  type: "text",
                  text: `Vui lòng kiểm tra lại logic Backend, fix lỗi và push code lên để tự động đóng ticket này!`
                }
              ]
            }
          ]
        },
        issuetype: {
          name: "Bug"
        },
        priority: {
          name: "High"
        },
        labels: [
          "bug",
          "ci-failed"
        ],
        assignee: {
          id: pusherJiraId || assignee.jiraId // Assign trực tiếp cho người vừa push gây lỗi, nếu không map được thì gán cho chủ phân hệ
        }
      }
    };

    // GỌI JIRA REST API
    try {
      const email = process.env.JIRA_EMAIL;
      const apiToken = process.env.JIRA_API_TOKEN;
      let domain = process.env.JIRA_DOMAIN || "";
      const projectKey = process.env.JIRA_PROJECT_KEY || "NOVA";

      if (!email || !apiToken || !domain) {
        console.error("❌ Thiếu biến môi trường cấu hình Jira (JIRA_EMAIL, JIRA_API_TOKEN, JIRA_DOMAIN)");
        return;
      }

      // Làm sạch domain: Loại bỏ tiền tố http/https và các ký tự dư thừa phía sau
      domain = domain.replace(/^https?:\/\//i, '').split('/')[0];

      const authHeader = Buffer.from(`${email}:${apiToken}`).toString('base64');

      // ── BƯỚC THÔNG MINH CHỐNG TRÙNG LẶP BUG TRÊN JIRA ───────────────────────
      // Tìm kiếm xem trên dự án có ticket Bug nào tương tự đang MỞ hay không
      const jql = `project = "${projectKey}" AND issuetype = "Bug" AND summary ~ "\\"${rawUrl}\\"" AND statusCategory != Done`;
      
      const searchResponse = await axios.get(
        `https://${domain}/rest/api/3/search/jql`,
        {
          params: { jql: jql },
          headers: {
            'Authorization': `Basic ${authHeader}`,
            'Accept': 'application/json'
          }
        }
      );

      const existingIssues = searchResponse.data.issues;
      
      if (existingIssues && existingIssues.length > 0) {
        const existingBug = existingIssues[0];
        console.log(`ℹ️ [Jira Check] API /${rawUrl} đã có Bug đang mở (Key: ${existingBug.key}, Trạng thái: ${existingBug.fields.status.name}).`);
        console.log(`➡️ [ACK Mechanism] Tiến hành comment minh chứng lỗi và gán (re-assign) Bug cho người gây lỗi mới nhất...`);

        // 1. Thêm comment vào Bug cũ để làm minh chứng lịch sử (Who failed the build?)
        const commentPayload = {
          body: {
            type: "doc",
            version: 1,
            content: [
              {
                type: "paragraph",
                content: [
                  {
                    type: "text",
                    text: `🚨 [CI/CD Re-occurrence] Lỗi này lại tiếp tục xuất hiện tại commit `
                  },
                  {
                    type: "text",
                    text: `${commitHash}`,
                    marks: [{ type: "code" }]
                  },
                  {
                    type: "text",
                    text: ` push bởi `
                  },
                  {
                    type: "text",
                    text: `@${githubActor} (${pusherName})`,
                    marks: [{ type: "strong" }]
                  },
                  {
                    type: "text",
                    text: `.\n`
                  },
                  {
                    type: "text",
                    text: `• Chi tiết lỗi: ${errorMessage}\n`
                  },
                  {
                    type: "text",
                    text: `• Phiên chạy CI/CD: `
                  },
                  {
                    type: "text",
                    text: runUrl ? "Link GitHub Actions Run" : "Local Run",
                    marks: runUrl ? [{ type: "link", attrs: { href: runUrl } }] : []
                  }
                ]
              }
            ]
          }
        };

        await axios.post(
          `https://${domain}/rest/api/3/issue/${existingBug.key}/comment`,
          commentPayload,
          {
            headers: {
              'Authorization': `Basic ${authHeader}`,
              'Content-Type': 'application/json',
              'Accept': 'application/json'
            }
          }
        );
        console.log(`   📝 Đã ghi nhận comment minh chứng trên ticket ${existingBug.key}`);

        // 2. Giao lại Bug (Re-assign) cho người push mới nhất để bắt buộc fix
        const newAssigneeId = pusherJiraId || assignee.jiraId;
        await axios.put(
          `https://${domain}/rest/api/3/issue/${existingBug.key}/assignee`,
          { accountId: newAssigneeId },
          {
            headers: {
              'Authorization': `Basic ${authHeader}`,
              'Content-Type': 'application/json',
              'Accept': 'application/json'
            }
          }
        );
        console.log(`   👤 Đã re-assign ticket ${existingBug.key} sang cho ${pusherName} để xử lý!`);
        continue; // Bỏ qua không tạo bug mới nữa
      }
      // ────────────────────────────────────────────────────────────────────────

      // Nếu chưa có, tiến hành tạo mới hoàn toàn
      const response = await axios.post(
        `https://${domain}/rest/api/3/issue`,
        payload,
        {
          headers: {
            'Authorization': `Basic ${authHeader}`,
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          }
        }
      );

      console.log(`✅ Đã tạo tự động Bug ticket ${response.data.key} trên Jira, gắn nhãn 'bug' và assign cho ${pusherName}!`);
    } catch (error) {
      console.error(`❌ Lỗi khi gửi API lên Jira cho endpoint /${rawUrl}:`, 
        error.response ? JSON.stringify(error.response.data) : error.message
      );
    }
  }
}

processNewmanFailures();
