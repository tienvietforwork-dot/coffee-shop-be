# Static audit: usecase matrix vs. code (app-core + FE)

Date 2026-10-21. Read-only audit. UI complexity was not audited (subjective). Method notes:
- Tables = entities read or written by the usecase service (incl. join and child tables, and tables read to build the response DTO).
- "Job gián tiếp" = no @Scheduled of its own, but the flow triggers `CoffeeStatusBackgroundService.requestRun()` (async single-thread worker "coffee-status-bg") or enqueues a row that a @Scheduled job later sends.
- All integrations run `mode=simulated` by default (application.yml: VNPAY_MODE, AHAMOVE_MODE, EINVOICE_MODE, ESMS_MODE).
- Delete = soft delete or status flip (INACTIVE / RESIGNED) when that is the "delete" action of the screen.

## Per-row verdict

| STT | Verdict | Evidence (<=40 words) | Suggested correct values |
|---|---|---|---|
| 1 | SAI: Search, Tables | CategoriesPage has no search/filter, GET /api/categories takes no params. CRUD ok, DELETE /api/categories/{id} = softDelete. CategoryService also reads CoffeeRepository (existsByCategoryId) -> 2 tables. | Search=FALSE; Tables="2-3 Bảng" |
| 2 | ĐÚNG | CoffeesPage: Input.Search "Tìm món" + category Select; /api/coffees CRUD (+PATCH status), /api/recipes*; ImageUpload (POST /api/images) + PrintRecipesModal (PDF); CoffeeStatusBackgroundService @Scheduled + async worker; coffee/category/recipe/step/material/image/promotion >5. | none |
| 3 | SAI: Tables | GET /api/public/menu (+ MenuPage "Tìm món…" + category filter). Reads coffee, category, recipe, recipe_material, material, promotion, promotion_item (stock snapshot + promo price) = 7. | Tables=">5 Bảng" |
| 4 | SAI: Tables (soft) | /api/public/carts: POST, GET, POST/PUT/DELETE items (DELETE = item.softDelete). Job = CartCleanupScheduler.abandonStaleCarts hourly (OK). Tables: cart, cart_item, coffee, dining_table, customer, promotion, promotion_item = 7. | Tables=">5 Bảng" (4-5 only if promo tables ignored) |
| 5 | SAI: Media | POST /api/public/checkout, POST /api/public/orders/{code}/pay -> VnpayGateway (redirect to payUrl); PaymentExpiryScheduler.expireUnpaid (1 min). Create only: ok. No QR image generated/rendered in FE/BE (QR is on VNPAY hosted page); no print/upload. | Media=FALSE |
| 6 | ĐÚNG (Job gián tiếp) | POST /api/orders (+/quote), PosPage: "Tìm món", customer lookup Input.Search (GET /api/customers/lookup), table picker. No own @Scheduled; consumeForOrder -> requestRun (async coffee-status worker) + notification queue. >5 tables ok. | Job: TRUE only if async/indirect counts, else FALSE |
| 7 | ĐÚNG (Job gián tiếp) | POST /api/orders/{id}/confirm,prepare,ready,dispatch,complete,reject; OrdersBoardPage filters (RangePicker, kênh, trạng thái). Job indirect: confirm -> consumeForOrder -> requestRun; complete -> invoiceService.queueFor (issued by @Scheduled InvoiceService.issueQueued). >5 tables. | Job: same note as row 6 |
| 8 | SAI: Tables | POST /api/orders/{id}/cancel, /reject, /api/payments/{id}/refund, POST /api/public/orders/{code}/cancel. close() touches order, payment, payment_transaction (REFUND row created), voucher, promotion_item, shipment, dining_table, order_status_history, invoice (adjust) -> >5. Job PaymentExpiryScheduler -> cancelUnpaidOnline; VNPAY refund via VnpayGateway.refund. | Tables=">5 Bảng" |
| 9 | SAI: Tables | GET /api/public/orders/{code}, GET /api/orders(+history); MyOrdersPage Input.Search "Nhập mã đơn", OrdersBoard filters. Response joins order, order_item, coffee, payment, shipment, customer, dining_table, staff, promotion (+ status_history) -> >5. No job. | Tables=">5 Bảng" (min "4-5") |
| 10 | SAI: 3rd, Tables | ShipmentsPage (Segmented "Đang xử lý/Tất cả"), PATCH /api/shipments/{id}, GET ahamove/estimate, POST ahamove/book, POST ahamove/cancel on THE SAME SCREEN -> AhamoveGateway (3rd party). Job ShipmentService.simulateTrips (15 s, simulated mode only). Tables: shipment, shipment_event, order, customer_address = 4. | 3rd=TRUE; Tables="4-5 Bảng" (overlaps row 26) |
| 11 | SAI: Search, Tables | GET /api/incidents (no params), GET/POST /api/orders/{id}/incidents, POST /api/incidents/{id}/resolve. IncidentsPage has no search/filter. Tables: incident, order, voucher (compensation), customer, staff. | Search=FALSE; Tables="4-5 Bảng" |
| 12 | SAI: Search | TablesPage: card grid, no search/filter. CRUD + POST /api/tables/{id}/regenerate-qr, DELETE softDelete. QR drawn in FE with qrcode.react (QRCodeSVG) + window.print(). 1 table (dining_table). | Search=FALSE |
| 13 | SAI: Media, Tables | MaterialsPage: Tabs RAW/PREPARED, search Input, Segmented "Đang cảnh báo"; /api/materials CRUD (DELETE = status INACTIVE). No upload, print or export on this screen. findAll reads material, material_package, material_component, material_batch, recipe, recipe_material, coffee (usedIn) = 7. | Media=FALSE; Tables=">5 Bảng" |
| 14 | ĐÚNG | GET /api/inventory/alerts (dashboard card + MaterialsPage "Đang cảnh báo" filter/search). InventoryScheduler.scanLowStock + expireBatches (both every 5 min). material, material_batch (+package) = 2-3. | none |
| 15 | SAI: Tables (soft) | SuppliersPage search "Tên, SĐT, nguyên liệu" + Segmented status; /api/suppliers CRUD, DELETE = status INACTIVE (ngừng hợp tác). Tables: supplier, supplier_material, material, material_package = 4. | Tables="4-5 Bảng" |
| 16 | ĐÚNG | /api/purchase-orders GET/POST/PUT/DELETE(soft) + send/cancel/close; PurchaseOrdersPage search + status + supplier filters; printPurchaseOrder (print). Tables: purchase_order, item, supplier, material, staff ~5. | none |
| 17 | SAI: Update/Delete (soft) | POST /api/goods-receipts, POST /api/purchase-orders/{id}/receipts, GET list/detail with filters, POST /api/goods-receipts/{id}/cancel (hủy phiếu = reverse stock; no PUT). Print 01-VT + ImageUpload (document_image_id). >5 tables. | Update or Delete=TRUE if "hủy phiếu" counts; else keep |
| 18 | SAI: Media | POST /api/inventory/dispose, /export, /produce (StockPage + ProduceModal), GET /api/inventory/transactions; StockHistoryPage material filter. No upload, print or export in these screens. Tables: material, material_batch, material_transaction, material_component. | Media=FALSE |
| 19 | SAI: Media, Tables | /api/stocktakes GET/GET{id}/POST/PUT/{id}/balance/{id}/cancel; StocktakesPage status filter. printStocktake (biên bản 05-VT) = print. Tables: stocktake, stocktake_item, material_batch, material, material_transaction, staff = 6. | Media=TRUE; Tables=">5 Bảng" |
| 20 | SAI: Search, Media | StaffPage: plain Table, no search/filter. /api/staff CRUD, DELETE = workStatus RESIGNED. No upload, print or export (no avatar). 1 table (staff). | Search=FALSE; Media=FALSE |
| 21 | ĐÚNG | UsersPage Segmented role filter; /api/users GET/POST/PUT/DELETE (softDelete user + user_role). Tables: user, user_role, role, staff, customer (+ role_permission via PermissionService). | none |
| 22 | SAI: Create, Delete (strict), Tables | Only GET /api/roles, GET /api/permissions, PUT /api/roles/{id}/permissions (RoleService: "3 fixed roles"). No role create/delete endpoint; role_permission rows are added/soft-deleted inside the PUT. Tables: role, permission, role_permission, user_role. | Create=FALSE, Delete=FALSE (keep TRUE only if role_permission rows count); Tables="4-5 Bảng" |
| 23 | SAI: 3rd, Tables | POST /api/auth/register (phone = username), POST /api/auth/login (JWT, local), GET /me, PUT /me/password, GET/PUT /api/account (+orders, vouchers, points). Customer registration/login DOES exist (guest AuthModal, AccountPage). NO third party: no OAuth, OTP, email verification or SMS on auth. Tables: user, user_role, role, role_permission, permission, customer (+order, voucher, loyalty_point_history for account) = >5. | 3rd=FALSE; Tables=">5 Bảng" |
| 24 | SAI: Search, Tables | GET /api/dashboard (+ /api/inventory/alerts, GET /api/orders today). DashboardPage: statistics and lists only, no search/filter input. Tables: order, order_item, coffee, material, material_batch = 5. | Search=FALSE; Tables="4-5 Bảng" |
| 25 | ĐÚNG | /api/payment-transactions (GET filters: from/to/status/mismatch; FE RangePicker + status + Segmented), POST .../requery, .../reconcile, public pay/ipn/return/sandbox. PaymentExpiryScheduler. VnpayGateway -> sandbox.vnpayment.vn. payment_transaction, payment, order = 3. | none |
| 26 | SAI: Tables (Media = FALSE) | Media answer: FALSE (only a tracking URL link; no image/print/export). AhamoveGateway estimate/create/cancel, POST /api/public/delivery/ahamove/webhook, ShipmentService.simulateTrips @Scheduled. Webhook applyPartnerStatus -> orderService.dispatch/completeDelivered touches order, order_status_history, payment, loyalty, invoice -> >5. | Media=FALSE; Tables=">5 Bảng" (min "4-5") |
| 27 | SAI: Job | InvoiceService.issueQueued @Scheduled(fixedDelay 20 s) issues queued invoices through EinvoiceProvider (api.meinvoice.vn); complete -> queueFor. Search filters, create (buyer, issue-now), update (replace/retry), print (printInvoice). Tables: invoice, invoice_line, order = 3. | Job=TRUE |
| 28 | SAI: Search, 3rd, Tables | DeliveryZonesPage: Table + modal, no search/filter. /api/delivery-zones CRUD (DELETE softDelete), public GET /api/public/delivery-zones. DeliveryZoneService is pure DB logic; no geocoding/maps/HTTP client. 1 table (delivery_zone). | Search=FALSE; 3rd=FALSE; Tables="1 Bảng" |
| 29 | ĐÚNG (tables borderline) | /api/supplier-returns GET/GET{id}/POST/PUT/complete/cancel; filters supplier + status; printSupplierReturn. Tables: supplier_return, item, supplier, goods_receipt, material_batch, material_transaction, material, staff (6-8). | consider Tables=">5 Bảng" |
| 30 | ĐÚNG (tables borderline) | /api/supplier-debts, ledger, ledger.xlsx (Excel), /api/supplier-payments GET/POST/cancel, printSupplierPayment, ImageUpload; RangePicker/Segmented. Tables: supplier, goods_receipt, supplier_return, supplier_payment, purchase_order, staff (+image). | maybe ">5 Bảng" |
| 31 | SAI: Tables (Create debatable) | GET /api/reports/inventory, /inventory.xlsx (Excel), /inventory/daily, POST /api/reports/inventory/close-day (creates inventory_snapshot); InventoryReportService.closeDay @Scheduled cron 23:55. Tables: material, material_batch, material_transaction, inventory_snapshot = 4. | Tables="4-5 Bảng"; Create=TRUE if snapshot creation counts |
| 32 | SAI: Tables (soft) | /api/cash-shifts (current, suggested-opening, list with RangePicker, open, movements, close); printCashShift. Tables: cash_shift, cash_movement, payment, staff = 4. | Tables="4-5 Bảng" |
| 33 | SAI: Media | /api/work-shifts CRUD, /api/shift-assignments (POST/DELETE/attendance), attendance check-in/out, GET /api/timesheets and /timesheets.xlsx (Excel export button "Xuất Excel"). Tables: work_shift, shift_assignment, staff = 3. | Media=TRUE |
| 34 | SAI: Tables | /api/coffee-costs, /{id}/history, POST /recalculate, PUT /{id}/price; CoffeeCostService.recalculate @Scheduled hourly; "Tìm món" search + Segmented. Tables: coffee, category, recipe, recipe_material, material, material_batch, coffee_cost_snapshot = 7. Create=F is debatable (snapshot rows are inserted). | Tables=">5 Bảng" |
| 35 | SAI: Search, Update, name | No OTP and no email anywhere in app-core (no mail sender, no OTP code). It is Zalo ZNS / SMS brandname via eSMS (EsmsGateway -> rest.esms.vn). NotificationsPage has RangePicker + status + phone filters; PUT /api/notifications/templates/{code} and POST .../resend = updates. Job CustomerNotificationService.sendQueued (10 s). Tables: notification_template, notification_message, order = 2-3. | Search=TRUE; Update=TRUE; rename to "Thông báo Zalo ZNS/SMS cho khách (không có email/OTP)" |
| 36 | SAI: Tables | GET /api/audit-logs (RangePicker, user, module, keyword, failed), /filters, /export.xlsx (Excel). Async queue AuditLogService.enqueue + @Scheduled flush (3 s) + cleanup cron. Only audit_log table (1). | Tables="1 Bảng" |

## @Scheduled jobs in app-core

| Class.method | Schedule | What it does |
|---|---|---|
| CartCleanupScheduler.abandonStaleCarts | cron `0 0 * * * *` (hourly) | OPEN carts idle > 1 day -> ABANDONED |
| InventoryScheduler.scanLowStock | fixedRate 5 min, delay 60 s | WebSocket alert for materials newly below min_stock |
| InventoryScheduler.expireBatches | fixedRate 5 min, delay 90 s | Write off expired lots (EXPIRED + DISPOSAL transaction) |
| PaymentExpiryScheduler.expireUnpaid | fixedDelay 60 s (`app.payment.expiry-scan-ms`) | Close expired VNPAY links and cancel unpaid QR/online orders |
| AuditLogService.flush | fixedDelay 3 s (`app.audit.flush-ms`) | Drain in-memory queue to audit_log (async queue) |
| AuditLogService.cleanup | cron `0 30 3 * * *` | Delete audit logs older than 180 days |
| CoffeeCostService.recalculate | cron `0 5 * * * *` (hourly) | Recompute coffee cost snapshots |
| CoffeeStatusBackgroundService.runOnSchedule | fixedDelay 5 min (`app.inventory.status-scan-ms`) | Sync AVAILABLE/SOLD_OUT with stock; also an async worker triggered by requestRun() from stock/recipe/coffee changes; logs to background_service_logs |
| CustomerNotificationService.sendQueued | fixedDelay 10 s | Send queued ZNS/SMS messages via eSMS (retry up to MAX_ATTEMPTS) |
| InventoryReportService.closeDay | cron `0 55 23 * * *` | Daily closing stock snapshot |
| InvoiceService.issueQueued | fixedDelay 20 s | Issue queued e-invoices via EinvoiceProvider |
| ShipmentService.simulateTrips | fixedDelay 15 s (only when Ahamove simulated) | Simulated driver accept / deliver |

## Integrations (all default to `simulated`)

| Class | External host (default) |
|---|---|
| integration/vnpay/VnpayGateway (+VnpayProperties) | sandbox.vnpayment.vn (paymentv2/vpcpay.html, merchant_webapi) |
| integration/ahamove/AhamoveGateway (+AhamoveProperties) | partner-apistg.ahamove.com/v3 (tracking cloudjetstg.ahamove.com) |
| integration/einvoice/EinvoiceProvider (+EinvoiceProperties) | api.meinvoice.vn (lookup www.meinvoice.vn/tra-cuu) |
| integration/esms/EsmsGateway | rest.esms.vn (Zalo ZNS + SMS brandname) |

No email, OTP, OAuth, maps/geocoding or other HTTP clients found.

## Export / print / upload features

- Image upload: POST /api/images (ImageController, ImageService, stored in DB, 3 MB), GET /api/public/images/{id}. FE `admin/components/ImageUpload.tsx` used in CoffeesPage (coffee image), purchase.tsx ReceiptModal (goods receipt document image), SupplierDebtsPage (payment voucher image).
- Excel (ExcelSheet): GET /api/audit-logs/export.xlsx; GET /api/reports/inventory.xlsx; GET /api/supplier-debts/{id}/ledger.xlsx; GET /api/timesheets.xlsx (FE `download()` in api/core.ts).
- Print (FE window.print via lib/purchase.ts printHtml): printPurchaseOrder, printGoodsReceipt (01-VT), printSupplierReturn, printSupplierPayment (02-TT), printInvoice, printCashShift, printStocktake (05-VT); PrintRecipesModal (PDF download of recipes, CoffeesPage); TablesPage QR print (qrcode.react QRCodeSVG + window.print).
- Not present: POS receipt/order print, material label print, staff avatar, QR image in checkout (VNPAY page shows it).
