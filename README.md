# coffee-shop-be (monorepo)

Backend cho DATN "Coffee Shop Management System". Đây là 1 repo chứa **nhiều app backend độc lập** (mỗi app tự build/deploy riêng), theo quyết định của nhóm: mỗi thành viên phụ trách 1 app, dùng chung 1 database Neon.

## Cấu trúc

```
coffee-shop-be/
├── app-core/            # Tiến Việt — Auth/Users, Products/Categories, Materials/kho, Orders/Shipments/Payments
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/...
├── app-crm/              # quản lý chăm sóc khách hàng (khung sườn, admin-only JWT)
├── app-stats/            # thống kê / điều chỉnh sản phẩm (khung sườn, admin-only JWT)
├── app-promotions/       # khuyến mại (khung sườn, admin-only JWT)
├── nginx-gateway/        # cổng vào duy nhất cho frontend, định tuyến sang đúng app
└── .github/workflows/    # 1 workflow CI riêng cho mỗi app (path-filtered)
```

Mỗi `app-*` là 1 project Spring Boot **hoàn toàn độc lập**: pom.xml riêng, Dockerfile riêng, deploy thành 1 Render Web Service riêng (dùng **Root Directory** = tên thư mục app khi tạo service trên Render). Tất cả cùng kết nối vào **1 Neon database chung** và dùng chung 1 giá trị `JWT_SECRET` để token đăng nhập ở app này dùng được ở app khác.

**Đăng nhập**: chỉ `app-core` có bảng `users` và endpoint `/api/auth/login` thật. 3 app còn lại (`app-crm`, `app-stats`, `app-promotions`) không tự đăng nhập — chỉ xác thực JWT do `app-core` cấp (`JWT_SECRET` phải giống hệt nhau ở cả 4 app), và chỉ chấp nhận role `ADMIN` (theo yêu cầu của thầy hướng dẫn — các module này chỉ cần dùng account quản trị, không cần phân quyền nhiều role).

**Frontend gọi vào đâu**: không gọi thẳng 4 app — gọi vào `nginx-gateway` (xem [nginx-gateway/README.md](nginx-gateway/README.md)), gateway tự định tuyến `/api/crm/*`, `/api/stats/*`, `/api/promotions/*` sang đúng app, còn lại (`/api/auth`, `/api/products`...) chuyển thẳng vào `app-core`.

## Thêm 1 app mới vào monorepo

1. Tạo thư mục `app-ten-module/` ở gốc repo.
2. Copy cấu trúc từ `app-core` làm khuôn mẫu (pom.xml, Dockerfile, SecurityConfig, JwtUtil, User entity — mỗi app tự có bản sao riêng để đăng nhập độc lập).
3. Thêm 1 file `.github/workflows/ci-ten-module.yml` (copy từ `ci-core.yml`, đổi `paths` và `working-directory` cho khớp thư mục mới).
4. Tạo 1 Render Web Service mới, Root Directory trỏ đúng vào `app-ten-module`.

## Xem chi tiết từng app

Mỗi app có README riêng trong thư mục của nó — xem [app-core/README.md](app-core/README.md) để biết chi tiết API, biến môi trường, cách chạy local.
