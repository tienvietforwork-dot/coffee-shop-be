# app-crm — Quản lý quan hệ khách hàng

`app-crm` là dịch vụ Spring Boot chạy độc lập. Dịch vụ dùng chung PostgreSQL với các app backend khác, không phụ thuộc mã Java hoặc gọi API nội bộ của các app đó. CRM đọc đơn hàng, chi tiết đơn, cà phê và danh mục từ database chung để tính chỉ số và phân nhóm.

## Chức năng chính

- Tìm kiếm khách hàng, cập nhật hồ sơ, quản lý địa chỉ và hồ sơ 360 độ.
- Chỉ số mua hàng từ đơn hoàn tất, chưa xóa: chi tiêu, số đơn, giá trị trung bình, tần suất gần đây, sản phẩm/danh mục yêu thích và lịch sử mua.
- Nhóm khách thủ công/tự động, xem trước, tính toán lại và quản lý thành viên.
- Lịch sử tương tác, lịch nhắc chăm sóc, kết quả tương tác và quy trình feedback nhiều chủ đề.
- Phân loại cảm xúc theo từ khóa khi không truyền sentiment; nhân viên có thể ghi đè kết quả.
- Ghi lịch sử nội dung phản hồi gửi khách, báo cáo giá trị khách, mức quan tâm sản phẩm, so sánh nhóm/kỳ và xuất CSV.

Các API nghiệp vụ yêu cầu JWT quyền ADMIN do `app-core` cấp. Hai dịch vụ cần dùng cùng `JWT_SECRET`. Health, OpenAPI và `POST /api/v1/feedbacks/public` (tiếp nhận góp ý từ khách) là endpoint công khai.

## Database setup

Schema khởi tạo nằm ở [`../database/schema.sql`](../database/schema.sql). Với database hiện có, chạy [`../database/migrate_crm_backend.sql`](../database/migrate_crm_backend.sql) thủ công trước khi khởi động phiên bản backend mới. Migration thêm thời điểm/kết quả tương tác, bảng chủ đề feedback và bảng lịch sử phản hồi; không sửa dữ liệu đơn hàng của Core. Hibernate dùng `ddl-auto: none`, không tự thay đổi schema.

CRM đọc các bảng do Core quản lý: `orders`, `order_items`, `coffees`, `categories`, `promotions` và `vouchers`. Chỉ số mua hàng và nhóm tự động chỉ tính đơn `COMPLETED`, `del_flag = false`.

## Run locally

Copy `.env.example` to `.env`, set the shared database credentials and the same `JWT_SECRET` as `app-core`, then run:

```bash
mvn spring-boot:run
```

The application reads `PORT` (default `8080`), `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`, and `CORS_ALLOWED_ORIGIN`.

## API routes

Direct service paths use `/api/v1/...`. Through `nginx-gateway`, call the same paths under `/api/crm`, for example `/api/crm/v1/customers`; the gateway removes `/crm` before forwarding.

| Area | Routes |
|---|---|
| Customers | `GET/POST /api/v1/customers`, `PUT /{id}`, `GET /{id}/360`, `POST /{id}/merge` |
| Addresses | `/api/v1/customers/{customerId}/addresses` |
| Customer groups | `/api/v1/customer-groups` and `/{id}/members`, `/preview`, `/evaluate` |
| Tương tác | `/api/v1/interactions`, `/reminders`, `/{id}`, `/{id}/close` |
| Phản hồi | `POST /api/v1/feedbacks/public` (khách gửi góp ý không cần đăng nhập); API CRM yêu cầu JWT có quyền tương ứng như `crm.feedbacks`, `crm.customers`, `crm.interactions`, `crm.groups` hoặc `crm.insights`. Quyền ADMIN vẫn được cấp đầy đủ qua danh sách quyền trong JWT. |
| Phân tích | `/api/v1/insights/top-customers` (`sortBy=spend|orders|average`), `/inactive-customers`, `/customer-value`, `/product-affinity`, `/sales-trends` (`period=week|month|quarter`), `/customer-growth`, `/feedback-summary`, `/repeated-complaints`, `/groups/{groupId}`, `/groups/compare`, `/customers/{id}/period-comparison` |
| Export | `GET /api/v1/insights/top-customers/export` (CSV) |

Customer 360 và API phân tích nhận `fromDate`/`toDate` tùy chọn theo định dạng `YYYY-MM-DD`, tính cả hai ngày. Chỉ số số đơn 30 ngày là cửa sổ lăn. Các tiêu đề nhóm chức năng và mô tả API trong Swagger được trình bày bằng tiếng Việt; URL, tên thuộc tính JSON, bảng và entity giữ nguyên để không phá tương thích với app khác.

API `/{feedbackId}/responses` ghi nhận nội dung phản hồi, nhân viên và thời điểm để lưu lịch sử; việc gửi email/chat thực tế cần tích hợp nhà cung cấp truyền tin riêng.
