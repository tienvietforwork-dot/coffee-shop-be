# Phân tích & thiết kế – Website bán cà phê Coffeeholic

| Giai đoạn | File nộp | Nội dung |
|---|---|---|
| 1.1 – Phân tích | `giai-doan-1.1_usecase-bpmn/Coffeeholic_GiaiDoan1.1_UseCase_BPMN.pdf` (23 trang) | UC-00 tổng quát, 15 use case chi tiết; 7 BPMN, mỗi trang BPMN kèm mô tả các bước |
| 1.2 – Thiết kế | `giai-doan-1.2_thiet-ke/Coffeeholic_GiaiDoan1.2_ThietKe.pdf` (8 trang) | ERD Conceptual, ERD Logical (3 trang), database diagram (3 trang), sơ đồ kiến trúc |

Mỗi sơ đồ có file `.drawio` (mở bằng draw.io để sửa) và ảnh PNG trong thư mục `png/` bên cạnh.

## Mô hình nghiệp vụ

- Quán chỉ bán **cà phê pha chế theo một thực đơn cố định**. Món không có tùy chọn (size, topping, combo...); khách chỉ ghi chú món.
- **Kênh đặt hàng:**
  - gọi trực tiếp tại quầy (nhân viên nhập đơn và thu tiền);
  - quét mã QR tại bàn;
  - đặt online trên website.
- **Nhận hàng:**
  - phục vụ tại bàn;
  - bàn giao tại quầy (mang về);
  - giao cho tài xế (giao hàng).
- **Khuyến mãi** chỉ gồm 2 loại, theo ERD:
  - **Đợt giảm giá:** giảm % cho hóa đơn (kèm giá trị đơn tối thiểu) hoặc giảm cho cà phê cụ thể (chi tiết đợt giảm giá: mức giảm riêng, giá sau giảm, số lượng tối đa).
  - **Voucher:** giảm cho hóa đơn, thuộc một khách hàng; có thể phát để đền bù sự cố.

## Nguyên tắc

- Use case dựa trên ULNL (sheet FA26_ORT_Coffeeholic). **Mỗi module ULNL trong phạm vi là một sơ đồ chi tiết:**
  - Sales & Operation: 5 module.
  - CRM: 6 module.
  - Promotion: 3 module (Promotion Management → đợt giảm giá, Voucher, Promotion Analytics).
  - Thêm UC-06 Kho nguyên liệu, lấy từ ERD.
- **Ngoài phạm vi:**
  - toàn bộ Product Merchandising (trưng bày, combo, bán kèm, nâng cấp, đề xuất, lịch và phân tích trưng bày);
  - Promotion Rule, Promotion Targeting, Campaign.
- Mỗi sơ đồ có tối đa 15 phần tử, tính cả actor. Không có use case đăng nhập.
- Chỉ dùng 3 actor:
  - **Khách hàng:** chỉ có hành động của khách (xem thực đơn, đặt hàng, theo dõi / hủy đơn, gửi phản hồi);
  - **Nhân viên;**
  - **Quản lý:** kế thừa Nhân viên; quan hệ generalization thể hiện ở UC-00.
- Ký hiệu UML:
  - association là đường liền không mũi tên;
  - «include» / «extend» là nét đứt, mũi tên mở, có nhãn «»;
  - generalization là tam giác rỗng.
- Use case, BPMN và ERD được đối chiếu chéo:
  - Sự cố: nhân viên ghi nhận, quản lý xử lý và đền bù voucher.
  - Cảnh báo tồn kho được gửi cho nhân viên.
  - Đợt giảm giá được áp dụng khi tính tổng tiền.
- Trong toàn bộ tài liệu, "sản phẩm" đổi thành "cà phê" (bảng `coffees`).
- Cả 3 mức ERD sinh từ **cùng một mô hình dữ liệu** (`_tools/design.js`):
  - thuộc tính bám ERD gốc;
  - phần bổ sung theo use case: kênh bán của đơn, phạm vi áp dụng và số lượng đã bán của đợt giảm giá, lịch sử điểm tích lũy, các bảng CRM, doanh thu theo ngày.

## Sinh lại sơ đồ & PDF

```bash
node docs/phan-tich/_tools/build.js
```

Lệnh trên cần Microsoft Edge (chạy headless) và mạng để tải draw.io viewer. Lệnh `build.js png UC07` chỉ render lại ảnh xem trước của các sơ đồ có tên bắt đầu bằng `UC07`; `build.js nopng` chỉ sinh file và PDF.

Bản ERD gốc của nhóm được lưu ở `giai-doan-1.2_thiet-ke/erd/ban-goc/`.
