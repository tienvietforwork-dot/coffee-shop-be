# app-crm -- Quan ly cham soc khach hang (CRM)

## Gioi thieu

`app-crm` la mot module admin-tool doc lap trong he thong coffee-shop-be. App nay
**khong co dang nhap / bang users cua rieng no** -- no tin tuong hoan toan vao JWT
da duoc phat hanh boi `app-core` (module chinh, so huu Auth/Users). Vi vay:

- Bien moi truong `JWT_SECRET` cua app nay **phai giong het** `JWT_SECRET` cua
  `app-core`, vi ca hai deu dung chung mot secret key de ky/verify token (HMAC-SHA).
- App nay chi **verify** token (khong co `generateToken`), khong tra cuu database
  nguoi dung -- toan bo thong tin (username, role) duoc lay truc tiep tu claims
  cua JWT.
- Tat ca cac endpoint (tru `/actuator/health`) yeu cau role `ADMIN`. Day la quyet
  dinh cua team: ca 3 module ve tinh (`app-crm`, `app-stats`, `app-promotions`)
  deu la cong cu quan tri noi bo, chi danh cho ADMIN, khong can phan quyen phuc
  tap nhieu role nhu app-core.
- Database: dung chung mot Neon Postgres voi `app-core`, nhung moi module tu
  quan ly bang cua minh. `ddl-auto: none` -- schema duoc tao thu cong qua DBeaver,
  khong dung Hibernate auto-generate hay migration tool nao.

## Chay local

1. Copy file env mau:
   ```bash
   cp .env.example .env
   ```
2. Mo `.env` va dien:
   - `JWT_SECRET` -- copy chinh xac tu `.env` cua `app-core`.
   - `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD` -- connection string Neon
     (co the dung chung connection string voi `app-core`).
3. Chay app:
   ```bash
   ./mvnw spring-boot:run
   ```
   Tren Windows:
   ```bash
   mvnw.cmd spring-boot:run
   ```

## Vi du test bang curl

Buoc 1 -- lay token tu app-core (dang nhap voi tai khoan co role ADMIN):

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "your-password"}'
```

Buoc 2 -- goi thu endpoint ping cua app-crm bang token vua lay duoc:

```bash
curl http://localhost:8081/api/ping \
  -H "Authorization: Bearer <access_token_tu_buoc_1>"
```

Response mau:

```json
{
  "module": "app-crm",
  "authenticatedAs": "admin",
  "authorities": ["ROLE_ADMIN"]
}
```

## Can lam tiep (what to build next)

1. Thiet ke bang du lieu can cho CRM (vi du: bang `customers` luu thong tin
   khach hang than thiet, lich su cham soc, ghi chu...). Tao bang nay **thu cong**
   tren Neon qua DBeaver, theo dung quy trinh trong `SETUP_GUIDE.docx` cua team
   (khong dung Hibernate ddl-auto, khong dung migration tool).
2. Viet `@Entity` tuong ung (vi du `Customer.java`) trong package
   `com.coffeeshop.crm.entity`, anh xa dung ten cot da tao tren Neon.
3. Viet `Repository` (Spring Data JPA), `Service`, `Controller` theo dung pattern
   da co san trong `app-core` (xem `app-core/src/main/java/com/coffeeshop/...`
   de tham khao cach to chuc code, DTO request/response, exception handling...).
4. Xoa `PingController` (chi la placeholder de xac nhan JWT hoat dong dung) khi
   da co controller thuc te.
5. Deploy len Render nhu mot Web Service rieng, voi Root Directory = `app-crm`.
