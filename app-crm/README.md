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

CRM đọc các bảng do Core quản lý: `orders`, `order_items`, `coffees`, `categories`, `promotions`, `vouchers` (loại voucher) và `voucher_details` (voucher đã phát cho khách). Chỉ số mua hàng và nhóm tự động chỉ tính đơn `COMPLETED`, `del_flag = false`.

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

API `/{feedbackId}/responses` ghi nhận nội dung nhân viên trả lời. Khách có thể tiếp tục trao đổi ngay trong cùng hồ sơ qua `POST /api/v1/customer-portal/feedbacks/{feedbackId}/responses`; lịch sử phân biệt người gửi `STAFF`/`CUSTOMER`. Khi khách nhắn vào hồ sơ mới hoặc đã đóng, hồ sơ chuyển sang `IN_PROGRESS` để nhân viên theo dõi. Cổng khách hàng gồm `GET /api/v1/customer-portal/feedbacks`, `GET /api/v1/customer-portal/interactions` và `POST /api/v1/customer-portal/feedbacks/{feedbackId}/responses/{responseId}/read`; yêu cầu JWT role `CUSTOMER` và tài khoản phải được liên kết với `customers`. Frontend gọi endpoint `/read` khi khách mở câu trả lời nhân viên; backend lưu thời điểm đọc đầu tiên và trả `readAt` cho phía nhân viên. Nhân viên chỉ cho hiển thị tương tác phù hợp cho khách bằng `visibleToCustomer: true`.

Với database đã cài migration CRM trước đó, chạy thêm `../database/migrate_crm_customer_portal.sql` và `../database/migrate_crm_feedback_conversation.sql` trong DBeaver. Tài khoản khách phải có `users.customer_id` liên kết tới đúng hồ sơ trong `customers`.

## Cấu trúc và vai trò các file

### Luồng xử lý

Thông thường, request đi theo luồng `Controller → Service → Repository → PostgreSQL`; controller nhận/kiểm tra HTTP, service áp dụng quy tắc CRM, repository đọc/ghi dữ liệu. Entity ánh xạ bảng; DTO định nghĩa dữ liệu nhận/trả API. `CustomerAnalyticsRepository` và `CustomerInsightRepository` dùng truy vấn SQL để đọc dữ liệu đơn hàng do Core quản lý trong database chung, không sở hữu hay sửa schema của các bảng đó.

### File ở thư mục gốc và cấu hình

| File | Vai trò |
|---|---|
| `pom.xml` | Khai báo Java 17, Spring Boot và các thư viện Maven. |
| `src/main/java/com/coffeeshop/crm/CrmApplication.java` | Điểm khởi chạy dịch vụ Spring Boot. |
| `src/main/resources/application.yml` | Cấu hình cổng, PostgreSQL, JWT, CORS, Hibernate và Actuator. Hibernate không tự tạo/sửa bảng (`ddl-auto: none`). |
| `config/SecurityConfig.java` | Cấu hình stateless JWT, quyền truy cập từng nhóm API và CORS. |
| `config/OpenApiConfig.java` | Cấu hình tài liệu Swagger/OpenAPI. |
| `security/JwtAuthenticationFilter.java` | Đọc, xác thực JWT và tạo thông tin đăng nhập cho request. |
| `security/JwtUtil.java` | Đọc claim và kiểm tra chữ ký/thời hạn token. |
| `security/CrmPrincipal.java` | Định dạng danh tính người dùng lưu trong Security Context. |
| `Dockerfile` | Đóng gói và chạy ứng dụng trong container. |
| `.env.example` | Tên các biến môi trường cần cấu hình; không chứa mật khẩu dùng thật. |

### Controller — các điểm vào HTTP

| File | Vai trò |
|---|---|
| `controller/CustomerController.java` | Tìm kiếm, tạo/sửa hồ sơ, Customer 360, gộp hồ sơ và CRUD địa chỉ. |
| `controller/CustomerGroupController.java` | CRUD nhóm khách, xem trước/tính lại nhóm tự động và quản lý thành viên. |
| `controller/InteractionController.java` | Ghi/list/sửa/xóa tương tác, truy vấn lịch nhắc và đóng tương tác. |
| `controller/FeedbackController.java` | Nhận góp ý công khai/nội bộ, phân công, chuyển cấp, đóng và quản lý câu trả lời. |
| `controller/CustomerPortalController.java` | API phía khách đã đăng nhập để xem phản hồi/tương tác và ghi nhận đã đọc. |
| `controller/CustomerInsightController.java` | API thống kê hành vi, nhóm, phản hồi, xu hướng và xuất CSV. |
| `controller/PingController.java` | Endpoint chẩn đoán danh tính/quyền của request; không phải nghiệp vụ CRM. |

### Service — quy tắc nghiệp vụ

| File | Vai trò |
|---|---|
| `service/Customer360Service.java` | Hồ sơ khách, lịch sử 360 và gộp dữ liệu khách trùng. |
| `service/CustomerAddressService.java` | Thêm/sửa/xóa và truy vấn địa chỉ thuộc khách hàng. |
| `service/CustomerGroupService.java` | Kiểm tra tiêu chí, xem trước, tính lại nhóm tự động và quản lý thành viên thủ công. |
| `service/InteractionService.java` | Vòng đời tương tác, trạng thái, kết quả, người phụ trách và nhắc chăm sóc. |
| `service/FeedbackService.java` | Vòng đời góp ý/khiếu nại, phân công, chuyển cấp, đóng, lưu câu trả lời và xóa mềm. |
| `service/FeedbackSentimentAnalyzer.java` | Đoán cảm xúc theo nội dung/điểm đánh giá khi request chưa cung cấp cảm xúc. Đây là phân loại từ khóa đơn giản, không phải mô hình AI. |
| `service/CustomerPortalService.java` | Giới hạn dữ liệu theo hồ sơ gắn với tài khoản khách và ghi thời điểm đọc câu trả lời. |
| `service/CustomerInsightService.java` | Kiểm tra tham số, phối hợp truy vấn cho các báo cáo và so sánh giai đoạn. |
| `scheduler/CustomerGroupScheduler.java` | Lập lịch tính lại các nhóm tự động theo cấu hình của scheduler. |

### Repository, Entity và DTO

| Thư mục/file | Vai trò |
|---|---|
| `repository/CustomerRepository.java` | CRUD và tìm kiếm entity khách hàng. |
| `repository/CustomerAddressRepository.java` | Truy vấn địa chỉ khách hàng. |
| `repository/CustomerGroupRepository.java`, `CustomerGroupMemberRepository.java` | Truy vấn nhóm và thành viên nhóm. |
| `repository/InteractionRepository.java`, `FeedbackRepository.java`, `FeedbackResponseRepository.java` | Truy vấn tương tác, phản hồi và nội dung trả lời. |
| `repository/CustomerPortalRepository.java` | Truy vấn dữ liệu portal theo quan hệ tài khoản-khách. |
| `repository/CustomerAnalyticsRepository.java` | SQL tổng hợp đơn hàng, sản phẩm yêu thích và lọc khách theo tiêu chí dùng cho Customer 360/nhóm. |
| `repository/CustomerInsightRepository.java` | SQL cho xếp hạng, khách lâu chưa mua, xu hướng, tăng trưởng, giá trị, phản hồi và mức độ yêu thích. |
| `entity/BaseEntity.java` | Trường audit và xóa mềm dùng chung. |
| `entity/Customer.java`, `CustomerAddress.java` | Hồ sơ và địa chỉ khách CRM. |
| `entity/CustomerGroup.java`, `CustomerGroupMember.java` | Nhóm và liên kết thành viên nhóm. |
| `entity/Interaction.java` | Lịch sử chăm sóc/tương tác. |
| `entity/Feedback.java`, `FeedbackResponse.java` | Góp ý/khiếu nại và từng câu trả lời có dấu thời gian đã đọc. |
| `dto/request/*` | Các lớp `Customer*Request`, `CriteriaRuleDTO`, `Interaction*Request`, `Feedback*Request` mô tả đầu vào API và ràng buộc kiểm tra. `PublicFeedbackRequest` là đầu vào góp ý công khai. |
| `dto/response/Customer360Response.java`, `CustomerInsightResponses.java` | Cấu trúc trả về Customer 360 và các báo cáo. |
| `dto/response/CustomerResponse.java`, `CustomerAddressResponse.java`, `CustomerGroupResponse.java`, `CustomerGroupMemberResponse.java`, `InteractionResponse.java` | Dạng trả về hồ sơ, địa chỉ, nhóm, thành viên và tương tác. |
| `dto/response/FeedbackResponse.java` | Dạng trả về **một hồ sơ góp ý**. |
| `dto/response/FeedbackResponseDto.java` | Dạng trả về **một câu trả lời cho góp ý**. Hai tên gần nhau nhưng đại diện hai loại dữ liệu khác nhau; không phải hai API trùng nhau. |
| `dto/response/CustomerPortal*Response.java` | Dạng dữ liệu đã giới hạn trường cho giao diện khách; `CustomerPortalReadReceiptResponse` là xác nhận đã đọc. |
| `dto/response/CustomerMergeResponse.java`, `GroupEvaluationResponse.java`, `ApiErrorResponse.java`, `PingResponse.java` | Kết quả gộp khách, tính lại nhóm, lỗi API và endpoint chẩn đoán. |
| `exception/CrmNotFoundException.java`, `CrmConflictException.java`, `GlobalExceptionHandler.java` | Lỗi nghiệp vụ và chuyển lỗi thành HTTP response thống nhất. |

## Rà soát phần có thể thừa hoặc dễ nhầm

- Đã bỏ bốn overload trung gian không có nơi gọi trong module: `CustomerInsightService.topCustomers(from,to,limit)`, `CustomerInsightService.monthlySales(from,to)`, `CustomerInsightRepository.topCustomers(from,to,limit)` và `CustomerInsightRepository.monthlySales(from,to)`. Các API hiện dùng overload có `sortBy`/`period`; chức năng HTTP không đổi.
- Không nên gộp `POST /feedbacks/public` với `POST /feedbacks`: hai endpoint phục vụ khách chưa đăng nhập và nhân viên CRM, có quyền và DTO đầu vào khác nhau.
- `/inactive-customers` và `/inactive-customers/count` có phần lọc giống nhau nhưng trả danh sách giới hạn và tổng số toàn bộ tương ứng; count phục vụ KPI nên không phải API trùng hoàn toàn.
- `GET /feedbacks/{id}/responses` là API nội bộ cho nhân viên, còn phản hồi khách xem qua `/customer-portal/feedbacks`; chúng có phạm vi dữ liệu và quyền khác nhau.
- `GET /top-customers/export` chỉ xuất bảng xếp hạng phía backend; nút xuất toàn bộ báo cáo của frontend tạo một file CSV tổng hợp khác. Nếu không có client nào dùng export top khách riêng thì có thể cân nhắc bỏ ở lần dọn API sau.
- `PingController` chỉ cần nếu nhóm còn dùng để kiểm tra JWT/quyền; nếu không còn dùng, đây là endpoint chẩn đoán có thể bỏ. Nó không thay thế `/actuator/health`.
- Bộ nhận diện sentiment hiện dựa vào từ khóa, có thể gán sai câu có phủ định/ngữ cảnh. Nhân viên vẫn có thể hiệu chỉnh thủ công; nếu cần độ chính xác cao hơn nên cải thiện bằng bộ quy tắc có kiểm thử hoặc dịch vụ phân tích riêng.
- `application.yml` có giá trị mặc định cho mật khẩu DB và JWT để chạy local; khi deploy phải đặt biến môi trường bí mật riêng, không dùng giá trị mặc định.

Không xóa các endpoint đang công khai hoặc các lớp DTO có khả năng ảnh hưởng contract frontend. Các mục “có thể bỏ” phía trên là điểm rà soát, không phải lỗi khiến CRM không chạy.
