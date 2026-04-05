# ADR 001: Separation of AI Service (FastAPI)

## Status
Accepted

## Context
Hệ thống NovaTicket yêu cầu tính năng hỗ trợ khách hàng thông qua chatbot AI (RAG - Retrieval-Augmented Generation). Tính năng này đòi hỏi:
- Xử lý ngôn ngữ tự nhiên (NLP) mạnh mẽ.
- Tương tác với Vector Database (ChromaDB) để tìm kiếm ngữ cảnh.
- Sử dụng các thư viện AI/ML mới nhất (LangChain, OpenAI/Gemini SDKs).
- Tiêu tốn tài nguyên tính toán (CPU/GPU) khác biệt so với logic nghiệp vụ thông thường.

## Decision
Chúng tôi quyết định tách tính năng AI thành một dịch vụ riêng chạy bằng **FastAPI (Python)**, thay vì tích hợp trực tiếp vào Spring Boot (Java).

## Rationale
1. **Ecosystem**: Python là ngôn ngữ số 1 cho AI/ML. Các thư viện như LangChain, PyTorch, và các SDK của LLM luôn được cập nhật sớm nhất cho Python.
2. **Resource Scaling**: Dịch vụ AI có thể được scale độc lập. Ví dụ: Chạy AI service trên cluster có GPU mà không cần nâng cấp toàn bộ backend Spring Boot nặng nề.
3. **Decoupling**: Các thay đổi về model AI, thuật toán embedding hay cấu trúc Vector DB không làm ảnh hưởng đến logic đặt vé (Booking process) cốt lõi.
4. **FastAPI Efficiency**: FastAPI cung cấp hiệu năng cực cao khi xử lý bất đồng bộ (Asyncio), phù hợp cho việc gọi các API LLM có độ trễ lớn.

## Consequences
- **Kỹ thuật**: Cần quản lý giao tiếp giữa 2 dịch vụ (HTTP/JSON).
- **Security**: Phải thiết lập cơ chế xác thực nội bộ (Internal API Key) để đảm bảo chỉ Backend mới có quyền gọi AI Service.
- **Complexity**: Tăng thêm một thành phần cần monitor và deploy (Docker container riêng).
- **Latency**: Thêm một hop mạng giữa Java và Python (~5-20ms), nhưng không đáng kể so với thời gian LLM xử lý (~1000-3000ms).
