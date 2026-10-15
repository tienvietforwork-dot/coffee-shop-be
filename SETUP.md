# Chạy local cho cả nhóm

Mục tiêu: điền `.env`, chạy là được. Không cần cài Maven, nginx hay plugin `.env` cho IDE.

## Cổng cố định

| App | Cổng | Ghi chú |
|---|---|---|
| app-core | 8080 | luôn cần (đăng nhập, menu, đơn… đều ở đây) |
| app-crm | 8081 | |
| app-stats | 8082 | |
| app-promotions | 8083 | |
| coffee-shop-fe | 5173 | |

Ở local, **Vite dev server đóng vai nginx-gateway**: FE gọi `/api/...` ở cổng 5173 rồi chuyển tiếp sang đúng app
(`/api/crm/*` → 8081, `/api/stats/*` → 8082, `/api/promotions/*` → 8083, còn lại → 8080). App nào không chạy thì
trang của nó hiện "Dịch vụ … chưa sẵn sàng", các trang khác vẫn dùng bình thường.

## 1. Cài đặt

- Git, **JDK 17** (Temurin), **Node.js 20+**
- IntelliJ IDEA hoặc VS Code. Maven **không** cần cài: mỗi app có sẵn `mvnw` / `mvnw.cmd`.

## 2. Clone

```bash
git clone https://github.com/tienvietforwork-dot/coffee-shop-be.git
git clone https://github.com/tienvietforwork-dot/coffee-shop-fe.git
```

## 3. Điền **một** file `.env` dùng chung (không push lên Git)

Ở gốc `coffee-shop-be`:

```bash
cp .env.example .env
```

Điền `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` (Neon chung) và `JWT_SECRET` theo tài liệu nội bộ của nhóm.
Cả 4 app đọc chung file này. Cổng **không** đặt ở đây: mỗi app có cổng mặc định riêng như bảng trên.

Muốn đổi riêng cho 1 app (ví dụ cổng), tạo `app-<tên>/.env` chỉ chứa dòng cần đổi; nó ghi đè file chung.
Trên Render mỗi app vẫn có biến môi trường riêng, file `.env` chỉ dùng khi chạy local.

## 4. Chạy backend

Chạy cả 4 app, mỗi app một cửa sổ / một JVM riêng (một app chết hay bị tắt không ảnh hưởng các app còn lại):

```bash
run-all.cmd                  # Windows (double-click cũng được)
./run-all.sh                 # Mac/Linux, log ở logs/<app>.log, Ctrl+C tắt hết
run-all.cmd core,crm         # chỉ chạy vài app  (Mac/Linux: ./run-all.sh core crm)
```

Hoặc chạy lẻ: `cd app-crm` rồi `mvnw.cmd spring-boot:run` (Mac/Linux `./mvnw spring-boot:run`),
hoặc trong IntelliJ mở thư mục `coffee-shop-be` và Run class `*Application` của app đó.
Log phải có `Tomcat started on port 8080` (hoặc 8081/8082/8083).

## 5. Chạy frontend

```bash
cd coffee-shop-fe
cp .env.example .env.local
npm install
npm run dev
```

Mở http://localhost:5173/admin, đăng nhập `admin` / `admin123`.

Kiểm tra app của mình đã nối được: http://localhost:5173/api/crm/ping (hoặc `stats` / `promotions`) phải trả về
tên module khi đã đăng nhập. Swagger từng app: `http://localhost:<cổng>/swagger-ui/index.html`.

## 6. Việc của từng module: chỉ viết CRUD

- **Bảng đã có sẵn** trên Neon (xem `database/schema.sql`), **không** tạo lại bảng trùng tên:
  - CRM: `customers`, `customer_addresses`, `customer_groups`, `customer_group_members`, `interactions`, `feedbacks`, `loyalty_point_history`
  - Khuyến mãi: `promotions`, `promotion_items`, `vouchers`
  - Thống kê: đọc từ `orders`, `order_items`, `payments`, `coffees`…
- **Giao diện đã có sẵn.** API mà FE cần nằm ở `coffee-shop-fe/src/api/crm.ts`, `promotions.ts`, `stats.ts`. Viết
  Entity → Repository → Service → Controller trong `app-<module>/src/main/java/com/coffeeshop/<module>/`.
- **Đường dẫn:** FE gọi `/api/crm/customers` thì app-crm nhận `/api/customers` (tiền tố module bị bỏ, giống gateway trên Render).
- **Quyền:** mọi API (trừ health/swagger) yêu cầu role `ADMIN` trong JWT do app-core cấp.
- Xóa `PingController` khi đã có controller thật.

## Lỗi hay gặp

| Triệu chứng | Nguyên nhân |
|---|---|
| Trang module báo "chưa sẵn sàng" | App đó chưa chạy, sai cổng, hoặc chưa có endpoint |
| 403 khi gọi API module | Chưa đăng nhập, tài khoản không phải ADMIN, hoặc `JWT_SECRET` khác app-core |
| Login lỗi 500 `JDBC exception` | Chưa có `coffee-shop-be/.env` nên app-core nối vào `localhost:5432` |
