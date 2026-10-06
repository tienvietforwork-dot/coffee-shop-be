# app-core — Sales & Operation (Coffeeholic)

Spring Boot 3.3 · Java 17. Phụ trách: cà phê & thực đơn, giỏ hàng, đặt hàng & thanh toán,
điều phối đơn (pha chế, phục vụ, giao nhận, sự cố), kho nguyên liệu theo lô (FEFO), bàn & QR,
nhân viên, tài khoản đăng nhập. Thông báo realtime qua STOMP/SockJS (`/ws`).

## Database

Schema theo sơ đồ DB01 ở `docs/phan-tich/giai-doan-1.2_thiet-ke` — **quản lý bằng tay** trên Neon,
Hibernate để `ddl-auto: none`. Các file SQL ở [`../database`](../database):

| File | Dùng khi |
|---|---|
| `schema.sql` | Tạo 32 bảng trên DB trống (27 bảng nghiệp vụ + `users`, `roles`, `permissions`, `user_roles`, `role_permissions`) |
| `seed.sql` | Phần 1 (bắt buộc): 3 role, 29 quyền, quyền mặc định. Phần 2: dữ liệu demo + tài khoản `admin`, `staff1`, `staff2`, `0912345678` (mật khẩu `admin123`) |
| `migrate_legacy_to_backup.sql` | Lịch sử: đã chuyển bảng của schema v1 sang schema `legacy_backup` |

- **Mọi bảng** có 6 cột `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`, `del_user`
  (`BaseEntity` tự điền; `created_by` = tên đăng nhập, `guest` cho khách vãng lai, `system` cho job).
- **Xóa mềm**: không xóa dòng thật; entity có `@SQLRestriction("del_flag = false")`, gọi `softDelete()`.
  UNIQUE là unique index `WHERE NOT del_flag` nên tạo lại được bản ghi đã xóa.
- Kiểm tra entity khớp schema thật: chạy với `-Dspring.jpa.hibernate.ddl-auto=validate`.

## Phân quyền (user – role – permission, như MES/SMT)

- Chỉ 3 role: `ADMIN` (Quản lý), `STAFF` (Nhân viên), `CUSTOMER` (Khách hàng). Một tài khoản có thể có nhiều role.
- Mỗi màn hình là một quyền (`permissions.code` + `path`), quyền thao tác đặc biệt có `path` NULL
  (`orders.refund`, `incidents.resolve`, `menu.edit`, `materials.edit`, `tables.edit`). Danh sách mã: `security/Perm.java`.
- API kiểm tra bằng `@PreAuthorize(Perm.CAN_…)`; API dùng chung giữa nhiều màn nhận một trong các quyền (`hasAnyAuthority`).
- Quyền của user = hợp các quyền của các role, cache trong `PermissionService` (xóa cache khi đổi phân quyền / tài khoản).
- JWT: `sub`, `userId`, `role` (role cao nhất – 3 app CRM/Khuyến mãi/Thống kê đang kiểm tra claim này),
  `roles`, `perms` (để các app kia phân quyền theo màn hình).
- Tài khoản nhân viên gắn `users.staff_id`: **không có phân công** – nhân viên bấm thao tác được ghi vào
  `orders.staff_id`, `incidents.handled_by`, `material_transactions.staff_id`.
- Khách đăng nhập là **tùy chọn** (`users.customer_id`): vẫn đặt món không cần tài khoản; có tài khoản thì
  đơn tự gắn hồ sơ, xem được đơn / điểm / voucher ở `/api/account`.

## Chạy local

```bash
cp .env.example .env   # điền SPRING_DATASOURCE_* của Neon, JWT_SECRET
set -a && . ./.env && set +a && mvn spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui.html`. Test: `mvn test` (H2 in-memory).

Biến môi trường: `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `JWT_SECRET` (giống hệt 3 app còn lại),
`JWT_EXPIRATION_MS`, `CORS_ALLOWED_ORIGIN`, `PORT`, `SHIPPING_FEE` (mặc định 15000).

## Nghiệp vụ chính

- **Trạng thái đơn**: `PENDING → CONFIRMED → PREPARING → READY → (DELIVERING) → COMPLETED`,
  nhánh `REJECTED` (từ PENDING) và `CANCELLED` (từ PENDING/CONFIRMED). Đơn tại quầy tạo xong là `CONFIRMED`.
- **Trừ kho** khi đơn chuyển sang `PREPARING`, theo công thức đang áp dụng của từng món; xuất theo lô
  hạn dùng gần nhất trước (FEFO); mỗi lô ghi 1 giao dịch `SALE` gắn `order_item_id`.
- **Giá**: giá đợt giảm giá theo cà phê (rẻ nhất, còn suất) → giảm theo hóa đơn (đợt có mức giảm lớn nhất
  đạt đơn tối thiểu) → voucher (chỉ có `discount_value` + `min_order_value`; đúng chủ sở hữu, còn hạn) → + phí giao.
  `order_items.unit_price` là giá thực thu; `orders.discount_amount` = giảm hóa đơn + voucher.
- **Thanh toán**: tiền mặt `PENDING` tới khi thu (hoặc tự thu khi hoàn tất); phương thức online đang
  **giả lập** thành công (`transaction_code = SIM-…`), chưa tích hợp cổng thanh toán.
- **Hủy / từ chối**: hoàn tiền khoản đã thu, trả voucher về `ISSUED`, trả lại suất khuyến mãi, giải phóng bàn.
- **Hoàn tất**: cộng điểm 1 điểm / 10.000đ, ghi `loyalty_point_history`.
- **Sự cố**: xử lý xong có thể phát N voucher đền bù cho khách của đơn (`vouchers.incident_id`).
- **Scheduler**: cảnh báo tồn thấp (5 phút), hủy lô hết hạn (00:05), đánh dấu giỏ bỏ dở sau 24h (mỗi giờ).

## API

Khách (không đăng nhập) — `/api/public/**`:

| Method | Path | |
|---|---|---|
| GET | `/menu` | Thực đơn hiển thị + giá sau giảm + đợt giảm theo hóa đơn |
| GET | `/tables/{qrCode}` | Thông tin bàn từ mã QR |
| POST | `/carts` `{tableQr?}` | Tạo giỏ (trả `sessionCode`) |
| GET/POST/PUT/DELETE | `/carts/{code}`, `/carts/{code}/items[/{id}]` | Xem / thêm / sửa / xóa món |
| POST | `/carts/{code}/quote` | Báo giá (khuyến mãi, voucher, phí giao) |
| POST | `/checkout` | Đặt hàng từ giỏ |
| GET | `/orders/{orderCode}` | Theo dõi đơn |
| POST | `/orders/{orderCode}/cancel` | Khách hủy (chỉ khi còn `PENDING`) |

Xác thực: `POST /api/auth/login`, `POST /api/auth/register` (khách), `GET /api/auth/me` (tài khoản + quyền kèm path
để dựng menu), `PUT /api/auth/me/password`. Khách đã đăng nhập: `GET/PUT /api/account`, `/api/account/{orders,vouchers,points}`.
Tài khoản & phân quyền: `/api/users`, `GET /api/roles`, `GET /api/permissions`, `PUT /api/roles/{id}/permissions`.

Quản trị (JWT, mỗi endpoint yêu cầu quyền của màn hình tương ứng):

| Nhóm | Endpoints |
|---|---|
| Thực đơn | `/api/categories`, `/api/coffees`, `PATCH /api/coffees/{id}/status`, `/api/coffees/{id}/recipes`, `/api/recipes/{id}[/activate]` |
| Đơn hàng | `GET /api/orders?status&channel&from&to`, `POST /api/orders` (tại quầy), `POST /api/orders/quote`, `POST /api/orders/{id}/{confirm,reject,cancel,prepare,ready,dispatch,complete,collect-payment}`, `POST /api/payments/{id}/refund` |
| Giao hàng | `GET /api/shipments`, `PATCH /api/shipments/{id}` (BOOKED → DRIVER_ACCEPTED → DELIVERING → DELIVERED) |
| Sự cố | `GET/POST /api/orders/{id}/incidents`, `GET /api/incidents`, `POST /api/incidents/{id}/resolve` |
| Kho | `/api/materials`, `/api/materials/{id}/batches`, `POST /api/inventory/{import,export,adjust}`, `GET /api/inventory/{transactions,alerts}` |
| Khác | `/api/tables` (+ `regenerate-qr`), `/api/staff`, `/api/users`, `GET /api/customers/lookup?phone`, `GET /api/dashboard`, `POST /api/auth/login` |

WebSocket: `/topic/orders` (đơn tạo/đổi trạng thái), `/topic/inventory-alerts` (nguyên liệu dưới mức tối thiểu).
