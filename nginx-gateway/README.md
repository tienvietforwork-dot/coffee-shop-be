# nginx-gateway

Cổng vào duy nhất cho frontend — gộp 4 backend độc lập (`app-core`, `app-crm`, `app-stats`, `app-promotions`) thành 1 địa chỉ. Frontend chỉ cần biết URL của gateway này, không cần biết 4 app kia nằm ở đâu.

## Quy tắc định tuyến

| Đường dẫn frontend gọi | Chuyển tới |
|---|---|
| `/api/crm/...` | `app-crm` (bỏ tiền tố `/crm`, ví dụ `/api/crm/ping` → `/api/ping` ở app-crm) |
| `/api/stats/...` | `app-stats` (tương tự) |
| `/api/promotions/...` | `app-promotions` (tương tự) |
| `/ws` | `app-core` (WebSocket, giữ nguyên) |
| Mọi thứ khác (`/api/auth`, `/api/products`, `/api/materials`, `/api/orders`...) | `app-core` (giữ nguyên, không đổi gì cả — đây là các API đã có sẵn từ trước) |

## Biến môi trường cần set trên Render

Khi tạo Web Service cho `nginx-gateway` (Root Directory = `nginx-gateway`, Runtime = Docker), thêm các Environment Variables sau — điền đúng URL thật của từng service **sau khi đã deploy xong cả 4 app kia** (không có dấu `/` ở cuối):

⚠️ Chỉ điền **tên miền trần** (không có `https://`, không có dấu `/` ở cuối):

```
CORE_UPSTREAM=coffee-shop-be-gn7a.onrender.com
CRM_UPSTREAM=<url-app-crm>.onrender.com
STATS_UPSTREAM=<url-app-stats>.onrender.com
PROMOTIONS_UPSTREAM=<url-app-promotions>.onrender.com
```

`PORT` không cần tự set — Render tự cấp, template đã đọc `${PORT}`.

## Frontend trỏ vào gateway thay vì trỏ thẳng app-core

Sau khi gateway deploy xong, sửa `.env` của `coffee-shop-fe`:

```
VITE_API_URL=https://<url-nginx-gateway>.onrender.com/api
VITE_WS_URL=https://<url-nginx-gateway>.onrender.com/ws
```

## Test nhanh sau khi deploy

```bash
curl https://<url-nginx-gateway>.onrender.com/api/auth/login -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'
# phải trả về y hệt kết quả gọi thẳng app-core

curl https://<url-nginx-gateway>.onrender.com/api/crm/ping -H "Authorization: Bearer <token>"
# phải trả về response từ app-crm, không phải app-core
```
