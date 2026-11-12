# Limitations & Security Boundaries

| Rủi ro | Mô tả | Giảm thiểu |
|---|---|---|
| **Bất đồng giữa parser và PostgreSQL** | Parser tách câu ở chỗ khác DB. Chiều nguy hiểm là parser thấy **một** câu còn PostgreSQL thấy nhiều câu | Dialect PostgreSQL, không bật backslash escape, tắt dòng trống làm dấu phân tách, chế độ nghiêm ngặt cấm `;` bên trong, và quyền DB tối thiểu |
| **Tên hàm phụ thuộc `search_path`** | Hàm `upper` không kèm schema được PostgreSQL tìm theo `search_path`. Nếu kẻ tấn công tạo được hàm cùng tên trong schema nằm trước, allowlist bị đánh lừa | Vai trò chỉ-đọc không có quyền tạo hàm; đặt `search_path` cố định cho vai trò |
| **View hoặc hàm ẩn tác dụng phụ** | `SELECT * FROM v` có thể gọi một hàm ghi bên trong định nghĩa view | Chỉ cấp `SELECT` trên các view đã duyệt, vai trò không có `EXECUTE` trên hàm ghi |
| **Hàm trong allowlist nhưng tốn tài nguyên** | Ví dụ `generate_series(1, 1000000000)` | `statement_timeout` ở vai trò, `query-timeout` của thư viện |
| **Đọc dữ liệu nhạy cảm bằng câu `SELECT` hợp lệ** | Không phải chuyện chỉ-đọc | Rule allowlist bảng và cột |
| **Bản JSqlParser khác phân loại khác** | Hành vi của `getFeatures()` đổi theo phiên bản | Bộ test `GuardRulesTest` chạy trong CI mỗi lần nâng phiên bản |
| **Chặn nhầm câu hợp lệ** | Hàm chưa trong allowlist, hoặc cú pháp parser chưa hỗ trợ | Chạy `OBSERVE` trước, bổ sung allowlist qua `withPureFunctions(...)` |

> **Quy tắc vàng:** Guard này là lớp bổ sung. Kết nối chỉ-đọc phải dùng vai trò DB **không có quyền ghi**. Lớp quyền đó chặn được cả những gì Guard không thấy.
