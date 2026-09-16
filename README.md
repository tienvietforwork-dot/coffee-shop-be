# coffee-shop-be (monorepo)

Backend cho DATN "Coffee Shop Management System". Đây là 1 repo chứa **nhiều app backend độc lập** (mỗi app tự build/deploy riêng), theo quyết định của nhóm: mỗi thành viên phụ trách 1 app, dùng chung 1 database Neon.

## Cấu trúc

```
coffee-shop-be/
├── app-core/            # Tiến Việt — Auth/Users, Products/Categories, Materials/kho, Orders/Shipments/Payments
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/...
├── app-crm/              # (sẽ thêm) quản lý chăm sóc khách hàng
├── app-stats/            # (sẽ thêm) thống kê / điều chỉnh sản phẩm
├── app-promotions/       # (sẽ thêm) khuyến mại
└── .github/workflows/    # 1 workflow CI riêng cho mỗi app (path-filtered)
```

Mỗi `app-*` là 1 project Spring Boot **hoàn toàn độc lập**: pom.xml riêng, Dockerfile riêng, deploy thành 1 Render Web Service riêng (dùng **Root Directory** = tên thư mục app khi tạo service trên Render). Tất cả cùng kết nối vào **1 Neon database chung** và dùng chung 1 giá trị `JWT_SECRET` để token đăng nhập ở app này dùng được ở app khác.

## Thêm 1 app mới vào monorepo

1. Tạo thư mục `app-ten-module/` ở gốc repo.
2. Copy cấu trúc từ `app-core` làm khuôn mẫu (pom.xml, Dockerfile, SecurityConfig, JwtUtil, User entity — mỗi app tự có bản sao riêng để đăng nhập độc lập).
3. Thêm 1 file `.github/workflows/ci-ten-module.yml` (copy từ `ci-core.yml`, đổi `paths` và `working-directory` cho khớp thư mục mới).
4. Tạo 1 Render Web Service mới, Root Directory trỏ đúng vào `app-ten-module`.

## Xem chi tiết từng app

Mỗi app có README riêng trong thư mục của nó — xem [app-core/README.md](app-core/README.md) để biết chi tiết API, biến môi trường, cách chạy local.
