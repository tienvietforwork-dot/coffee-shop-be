// Design diagrams generated from ONE data model so the three ERD levels stay consistent:
//  - conceptual ERD (Chen notation, entities + relationships, not normalized)
//  - logical ERD (Vietnamese entity/attribute names, PK/FK, crow's foot, normalized to 3NF)
//  - physical database diagram (PostgreSQL table/column names, data types, constraints)
// plus the system architecture diagram.
const { Diagram, ST } = require('./lib');

const DOMAIN = {
  coffee: ['#d5e8d4', '#82b366', 'Cà phê & công thức'],
  stock: ['#e1d5e7', '#9673a6', 'Kho nguyên liệu'],
  sales: ['#dae8fc', '#6c8ebf', 'Bán hàng & vận hành'],
  customer: ['#fff2cc', '#d6b656', 'Khách hàng'],
  promo: ['#f8cecc', '#b85450', 'Khuyến mãi'],
  staff: ['#ffe6cc', '#d79b00', 'Nhân sự'],
};

// Physical model derived 1:1 from ERD mức Logical (entity → table, attribute → column, relationship → FK).
// Only additions outside the ERD: loyalty_point_history (ý 8) and staff.role (ý 12), as agreed.
// table: [domain, vnName, description, [ [key, column, vnColumn(ERD attribute), type], ... ]]
const T = {
  categories: ['coffee', 'Danh mục', '', [
    ['PK', 'id', 'Mã danh mục', 'BIGSERIAL'], ['UQ', 'name', 'Tên', 'VARCHAR(100)'], ['', 'description', 'Mô tả', 'TEXT'], ['', 'display_order', 'Thứ tự hiển thị', 'INT']]],
  coffees: ['coffee', 'Cà phê', '', [
    ['PK', 'id', 'Mã cà phê', 'BIGSERIAL'], ['FK', 'category_id', 'Mã danh mục', 'BIGINT'], ['', 'name', 'Tên', 'VARCHAR(150)'],
    ['', 'image_url', 'ảnh', 'VARCHAR(500)'], ['', 'price', 'giá bán', 'NUMERIC(12,2)'], ['', 'description', 'mô tả', 'TEXT'], ['', 'status', 'trạng thái', 'VARCHAR(20)']]],
  recipes: ['coffee', 'Công thức', '', [
    ['PK', 'id', 'Mã công thức', 'BIGSERIAL'], ['FK,UQ', 'coffee_id', 'Mã cà phê', 'BIGINT'], ['', 'brew_method', 'cách pha', 'VARCHAR(30)'],
    ['', 'version', 'phiên bản', 'INT'], ['', 'description', 'mô tả', 'TEXT'], ['', 'brew_time_min', 'thời gian pha', 'INT'], ['', 'is_active', 'đang áp dụng', 'BOOLEAN']]],
  recipe_steps: ['coffee', 'Các bước pha chế', '', [
    ['PK', 'id', 'Mã bước', 'BIGSERIAL'], ['FK', 'recipe_id', 'Mã công thức', 'BIGINT'], ['', 'step_no', 'bước số', 'INT'], ['', 'instruction', 'mô tả thao tác', 'TEXT']]],
  recipe_materials: ['coffee', 'Nguyên liệu công thức', '', [
    ['PK,FK', 'recipe_id', 'Mã công thức', 'BIGINT'], ['PK,FK', 'material_id', 'Mã nguyên liệu', 'BIGINT'], ['', 'quantity', 'định lượng', 'NUMERIC(10,2)'], ['', 'note', 'ghi chú', 'VARCHAR(255)']]],
  materials: ['stock', 'Nguyên liệu', '', [
    ['PK', 'id', 'Mã nguyên liệu', 'BIGSERIAL'], ['UQ', 'name', 'tên', 'VARCHAR(100)'], ['', 'unit', 'đơn vị tính', 'VARCHAR(20)'],
    ['', 'stock_quantity', 'số lượng tồn', 'NUMERIC(12,2)'], ['', 'min_stock', 'tồn tối thiểu', 'NUMERIC(12,2)'], ['', 'status', 'trạng thái', 'VARCHAR(20)']]],
  material_batches: ['stock', 'Lô nguyên liệu', '', [
    ['PK', 'id', 'Mã lô', 'BIGSERIAL'], ['FK', 'material_id', 'Mã nguyên liệu', 'BIGINT'], ['', 'import_quantity', 'số lượng nhập', 'NUMERIC(12,2)'],
    ['', 'remaining_quantity', 'số lượng còn lại', 'NUMERIC(12,2)'], ['', 'unit_cost', 'đơn giá nhập', 'NUMERIC(12,2)'], ['', 'expiry_date', 'hạn sử dụng', 'DATE'], ['', 'status', 'trạng thái', 'VARCHAR(20)']]],
  material_transactions: ['stock', 'Giao dịch nguyên liệu', '', [
    ['PK', 'id', 'Mã giao dịch', 'BIGSERIAL'], ['FK', 'batch_id', 'Mã lô', 'BIGINT'], ['FK', 'staff_id', 'Mã nhân viên', 'BIGINT'], ['FK', 'order_item_id', 'Mã chi tiết đơn', 'BIGINT'],
    ['', 'type', 'loại giao dịch', 'VARCHAR(20)'], ['', 'quantity', 'số lượng', 'NUMERIC(12,2)'], ['', 'note', 'ghi chú', 'VARCHAR(255)'], ['', 'created_at', 'thời gian', 'TIMESTAMP']]],
  staff: ['staff', 'Nhân viên', '', [
    ['PK', 'id', 'Mã nhân viên', 'BIGSERIAL'], ['', 'full_name', 'họ tên', 'VARCHAR(100)'], ['', 'position', 'chức vụ', 'VARCHAR(50)'], ['UQ', 'phone', 'số điện thoại', 'VARCHAR(15)'],
    ['UQ', 'email', 'email', 'VARCHAR(100)'], ['', 'hire_date', 'ngày vào làm', 'DATE'], ['', 'work_status', 'trạng thái làm việc', 'VARCHAR(20)'], ['', 'role', 'vai trò', 'VARCHAR(20)']]],
  promotions: ['promo', 'Đợt giảm giá', '', [
    ['PK', 'id', 'Mã đợt giảm giá', 'BIGSERIAL'], ['', 'name', 'tên chương trình', 'VARCHAR(150)'], ['', 'description', 'mô tả', 'TEXT'], ['', 'apply_scope', 'áp dụng cho', 'VARCHAR(10)'],
    ['', 'discount_percent', '% giảm', 'NUMERIC(5,2)'], ['', 'min_order_amount', 'đơn tối thiểu', 'NUMERIC(12,2)'], ['', 'start_date', 'ngày bắt đầu', 'TIMESTAMP'], ['', 'end_date', 'ngày kết thúc', 'TIMESTAMP'], ['', 'status', 'trạng thái', 'VARCHAR(20)']]],
  promotion_items: ['promo', 'Chi tiết đợt giảm giá', '', [
    ['PK', 'id', 'Mã chi tiết', 'BIGSERIAL'], ['FK', 'promotion_id', 'Mã đợt giảm giá', 'BIGINT'], ['FK', 'coffee_id', 'Mã cà phê', 'BIGINT'],
    ['', 'custom_discount', 'mức giảm riêng', 'NUMERIC(12,2)'], ['', 'sale_price', 'giá sau giảm', 'NUMERIC(12,2)'], ['', 'max_quantity', 'số lượng tối đa', 'INT'], ['', 'sold_quantity', 'số lượng đã bán', 'INT']]],
  dining_tables: ['sales', 'Bàn', '', [
    ['PK', 'id', 'Mã bàn', 'BIGSERIAL'], ['UQ', 'table_no', 'số bàn', 'VARCHAR(10)'], ['UQ', 'qr_code', 'mã QR', 'VARCHAR(255)'],
    ['', 'area', 'khu vực', 'VARCHAR(50)'], ['', 'capacity', 'sức chứa', 'INT'], ['', 'status', 'trạng thái', 'VARCHAR(20)']]],
  carts: ['sales', 'Giỏ hàng', '', [
    ['PK', 'id', 'Mã giỏ hàng', 'BIGSERIAL'], ['UQ', 'session_code', 'mã phiên', 'VARCHAR(64)'], ['FK', 'table_id', 'Mã bàn', 'BIGINT'],
    ['FK', 'customer_id', 'Mã khách hàng', 'BIGINT'], ['', 'status', 'trạng thái', 'VARCHAR(20)'], ['', 'created_at', 'ngày tạo', 'TIMESTAMP']]],
  cart_items: ['sales', 'Chi tiết giỏ hàng', '', [
    ['PK', 'id', 'Mã chi tiết', 'BIGSERIAL'], ['FK', 'cart_id', 'Mã giỏ hàng', 'BIGINT'], ['FK', 'coffee_id', 'Mã cà phê', 'BIGINT'],
    ['', 'quantity', 'số lượng', 'INT'], ['', 'note', 'ghi chú', 'VARCHAR(255)'], ['', 'subtotal', 'tạm tính', 'NUMERIC(12,2)']]],
  orders: ['sales', 'Đơn hàng', '', [
    ['PK', 'id', 'Mã đơn hàng', 'BIGSERIAL'], ['UQ', 'order_code', 'mã đơn', 'VARCHAR(30)'], ['FK', 'customer_id', 'Mã khách hàng', 'BIGINT'],
    ['FK', 'staff_id', 'Nhân viên tạo', 'BIGINT'], ['FK', 'table_id', 'Mã bàn', 'BIGINT'],
    ['FK,UQ', 'cart_id', 'Mã giỏ hàng', 'BIGINT'], ['FK', 'promotion_id', 'Mã đợt giảm giá', 'BIGINT'], ['', 'order_type', 'loại đơn', 'VARCHAR(20)'],
    ['', 'channel', 'kênh đặt hàng', 'VARCHAR(20)'], ['', 'status', 'trạng thái', 'VARCHAR(20)'], ['', 'subtotal', 'tạm tính', 'NUMERIC(12,2)'],
    ['', 'discount_amount', 'tiền giảm', 'NUMERIC(12,2)'], ['', 'total_amount', 'tổng tiền', 'NUMERIC(12,2)'], ['', 'note', 'ghi chú', 'TEXT'],
    ['', 'cancel_reason', 'lý do hủy', 'TEXT'], ['', 'pickup_time', 'thời gian nhận hẹn', 'TIMESTAMP'], ['', 'ordered_at', 'ngày giờ đặt', 'TIMESTAMP']]],
  order_items: ['sales', 'Chi tiết đơn hàng', '', [
    ['PK', 'id', 'Mã chi tiết', 'BIGSERIAL'], ['FK', 'order_id', 'Mã đơn hàng', 'BIGINT'], ['FK', 'coffee_id', 'Mã cà phê', 'BIGINT'],
    ['', 'quantity', 'số lượng', 'INT'], ['', 'unit_price', 'đơn giá', 'NUMERIC(12,2)'], ['', 'line_total', 'thành tiền', 'NUMERIC(12,2)'], ['', 'note', 'ghi chú', 'VARCHAR(255)']]],
  payments: ['sales', 'Thanh toán', '', [
    ['PK', 'id', 'Mã thanh toán', 'BIGSERIAL'], ['FK', 'order_id', 'Mã đơn hàng', 'BIGINT'], ['', 'method', 'hình thức', 'VARCHAR(20)'],
    ['', 'amount', 'số tiền', 'NUMERIC(12,2)'], ['', 'status', 'trạng thái', 'VARCHAR(20)'], ['UQ', 'transaction_code', 'mã giao dịch', 'VARCHAR(100)'],
    ['', 'paid_at', 'thời điểm trả', 'TIMESTAMP'], ['', 'refund_amount', 'số tiền hoàn', 'NUMERIC(12,2)']]],
  shipments: ['sales', 'Đơn giao hàng', '', [
    ['PK', 'id', 'Mã đơn giao', 'BIGSERIAL'], ['FK,UQ', 'order_id', 'Mã đơn hàng', 'BIGINT'], ['FK', 'address_id', 'Mã địa chỉ', 'BIGINT'],
    ['', 'carrier', 'đơn vị vận chuyển', 'VARCHAR(50)'], ['', 'tracking_code', 'mã vận đơn', 'VARCHAR(100)'], ['', 'shipping_fee', 'phí giao hàng', 'NUMERIC(12,2)'],
    ['', 'status', 'trạng thái', 'VARCHAR(20)'], ['', 'booked_at', 'thời điểm đặt xe', 'TIMESTAMP'], ['', 'driver_accepted_at', 'thời điểm tài xế nhận đơn', 'TIMESTAMP'],
    ['', 'delivered_at', 'thời điểm giao xong', 'TIMESTAMP'], ['', 'note', 'ghi chú', 'TEXT']]],
  incidents: ['sales', 'Sự cố', '', [
    ['PK', 'id', 'Mã sự cố', 'BIGSERIAL'], ['FK', 'order_id', 'Mã đơn hàng', 'BIGINT'], ['FK', 'handled_by', 'Nhân viên xử lý', 'BIGINT'],
    ['', 'type', 'loại sự cố', 'VARCHAR(30)'], ['', 'severity', 'mức độ nghiêm trọng', 'VARCHAR(20)'], ['', 'reason', 'lý do', 'VARCHAR(255)'],
    ['', 'description', 'mô tả', 'TEXT'], ['', 'status', 'trạng thái xử lý', 'VARCHAR(20)'], ['', 'reported_at', 'ngày ghi nhận', 'TIMESTAMP'], ['', 'resolved_at', 'ngày xử lý', 'TIMESTAMP']]],
  vouchers: ['promo', 'Voucher', '', [
    ['PK', 'id', 'Mã voucher (khóa)', 'BIGSERIAL'], ['UQ', 'code', 'mã voucher', 'VARCHAR(30)'], ['FK', 'customer_id', 'Khách hàng sở hữu', 'BIGINT'],
    ['FK', 'order_id', 'Đơn hàng áp dụng', 'BIGINT'], ['FK', 'incident_id', 'Sự cố đền bù', 'BIGINT'], ['', 'discount_value', 'giá trị giảm', 'NUMERIC(12,2)'],
    ['', 'conditions', 'điều kiện áp dụng', 'TEXT'], ['', 'status', 'trạng thái sử dụng', 'VARCHAR(20)'], ['', 'issued_at', 'ngày phát hành', 'TIMESTAMP'], ['', 'expires_at', 'ngày hết hạn', 'TIMESTAMP']]],
  customers: ['customer', 'Khách hàng', '', [
    ['PK', 'id', 'Mã khách hàng', 'BIGSERIAL'], ['', 'full_name', 'họ tên', 'VARCHAR(100)'], ['UQ', 'phone', 'số điện thoại', 'VARCHAR(15)'],
    ['', 'email', 'email', 'VARCHAR(100)'], ['', 'loyalty_points', 'điểm tích lũy', 'INT'], ['', 'registered_at', 'ngày đăng ký', 'TIMESTAMP']]],
  customer_addresses: ['customer', 'Địa chỉ khách hàng', '', [
    ['PK', 'id', 'Mã địa chỉ', 'BIGSERIAL'], ['FK', 'customer_id', 'Mã khách hàng', 'BIGINT'], ['', 'label', 'nhãn', 'VARCHAR(30)'],
    ['', 'recipient_name', 'người nhận', 'VARCHAR(100)'], ['', 'recipient_phone', 'sđt người nhận', 'VARCHAR(15)'], ['', 'address', 'địa chỉ', 'VARCHAR(255)'], ['', 'is_default', 'mặc định', 'BOOLEAN']]],
  customer_groups: ['customer', 'Nhóm khách hàng', '', [
    ['PK', 'id', 'Mã nhóm', 'BIGSERIAL'], ['UQ', 'name', 'tên nhóm', 'VARCHAR(100)'], ['', 'description', 'mô tả', 'TEXT'],
    ['', 'criteria_type', 'loại tiêu chí', 'VARCHAR(30)'], ['', 'criteria_rule', 'điều kiện phân nhóm', 'JSONB'], ['', 'recalculated_at', 'ngày tính lại', 'TIMESTAMP']]],
  customer_group_members: ['customer', 'Thành viên nhóm', '', [
    ['PK,FK', 'group_id', 'Mã nhóm', 'BIGINT'], ['PK,FK', 'customer_id', 'Mã khách hàng', 'BIGINT'], ['', 'is_manual', 'thêm thủ công', 'BOOLEAN'], ['', 'added_at', 'ngày thêm', 'TIMESTAMP']]],
  feedbacks: ['customer', 'Phản hồi', '', [
    ['PK', 'id', 'Mã phản hồi', 'BIGSERIAL'], ['FK', 'customer_id', 'Mã khách hàng', 'BIGINT'], ['', 'topic', 'chủ đề', 'VARCHAR(50)'],
    ['', 'category', 'phân loại', 'VARCHAR(30)'], ['', 'sentiment', 'cảm xúc', 'VARCHAR(20)'], ['', 'content', 'nội dung', 'TEXT'],
    ['', 'status', 'trạng thái', 'VARCHAR(20)'], ['', 'escalated', 'đã chuyển cấp', 'BOOLEAN'], ['', 'resolution', 'phương án xử lý', 'TEXT'],
    ['', 'satisfaction', 'mức hài lòng', 'VARCHAR(20)'], ['', 'created_at', 'ngày gửi', 'TIMESTAMP'], ['', 'closed_at', 'ngày đóng', 'TIMESTAMP']]],
  interactions: ['customer', 'Tương tác', '', [
    ['PK', 'id', 'Mã tương tác', 'BIGSERIAL'], ['FK', 'customer_id', 'Mã khách hàng', 'BIGINT'], ['', 'channel', 'kênh', 'VARCHAR(20)'],
    ['', 'content', 'nội dung', 'TEXT'], ['', 'note', 'ghi chú', 'TEXT'], ['', 'status', 'trạng thái', 'VARCHAR(20)'],
    ['', 'remind_at', 'ngày nhắc chăm sóc', 'TIMESTAMP'], ['', 'created_at', 'thời gian', 'TIMESTAMP'], ['', 'closed_at', 'thời điểm kết thúc', 'TIMESTAMP']]],
  loyalty_point_history: ['customer', 'Lịch sử điểm tích lũy (bổ sung ở DB – ý 8)', '', [
    ['PK', 'id', 'Mã lịch sử', 'BIGSERIAL'], ['FK', 'customer_id', 'Mã khách hàng', 'BIGINT'], ['FK', 'order_id', 'Mã đơn hàng', 'BIGINT'],
    ['', 'points_change', 'số điểm thay đổi', 'INT'], ['', 'reason', 'lý do', 'VARCHAR(100)'], ['', 'created_at', 'thời gian', 'TIMESTAMP']]],
};

// [child.fk, parent, optional?, oneToOne?] – one entry per ERD relationship
const FKS = [
  ['coffees.category_id', 'categories'],                    // Danh mục bao gồm Cà phê
  ['recipes.coffee_id', 'coffees', false, true],            // Công thức pha chế thành Cà phê (1–1)
  ['recipe_steps.recipe_id', 'recipes'],                    // Công thức gồm Các bước
  ['recipe_materials.recipe_id', 'recipes'],                // Nguyên liệu công thức tiêu hao
  ['recipe_materials.material_id', 'materials'],            // … thuộc Nguyên liệu
  ['material_batches.material_id', 'materials'],            // Nguyên liệu gồm Lô
  ['material_transactions.batch_id', 'material_batches'],   // Lô tác động Giao dịch
  ['material_transactions.staff_id', 'staff', true],        // Nhân viên thực hiện (0..1)
  ['material_transactions.order_item_id', 'order_items', true], // Chi tiết đơn phát sinh (ý 6)
  ['promotion_items.promotion_id', 'promotions'],           // Đợt giảm giá bao gồm
  ['promotion_items.coffee_id', 'coffees'],                 // Cà phê áp dụng cho
  ['carts.table_id', 'dining_tables', true],                // Bàn có Giỏ hàng
  ['carts.customer_id', 'customers', true],                 // Khách hàng mở Giỏ hàng
  ['cart_items.cart_id', 'carts'], ['cart_items.coffee_id', 'coffees'],
  ['orders.cart_id', 'carts', true, true],                  // Giỏ hàng chuyển thành Đơn hàng (ý 3)
  ['orders.table_id', 'dining_tables', true],               // Bàn thuộc về
  ['orders.promotion_id', 'promotions', true],              // Đợt giảm giá áp dụng cho (ý 2)
  ['orders.staff_id', 'staff'],                             // Nhân viên tạo (ý 4)
  ['orders.customer_id', 'customers', true],                // Khách hàng mua
  ['order_items.order_id', 'orders'], ['order_items.coffee_id', 'coffees'],
  ['payments.order_id', 'orders'],                          // thanh toán bằng
  ['shipments.order_id', 'orders', false, true],            // tạo ra (0..1)
  ['shipments.address_id', 'customer_addresses'],           // giao đến
  ['customer_addresses.customer_id', 'customers'],          // có
  ['incidents.order_id', 'orders'],                         // Xảy ra
  ['incidents.handled_by', 'staff', true],                  // Xử lý
  ['vouchers.customer_id', 'customers'],                    // Sở hữu
  ['vouchers.order_id', 'orders', true],                    // áp dụng cho
  ['vouchers.incident_id', 'incidents', true],              // Đền bù (ý 11)
  ['customer_group_members.group_id', 'customer_groups'], ['customer_group_members.customer_id', 'customers'], // thuộc / gồm
  ['feedbacks.customer_id', 'customers'],                   // gửi
  ['interactions.customer_id', 'customers'],                // có
  ['loyalty_point_history.customer_id', 'customers'], ['loyalty_point_history.order_id', 'orders', true],
];


// Pages: full tables {name: [x, y]} and reference stubs {name: [x, y]} for parents that live on another page.
const PAGES = [
  { key: 'P1', title: 'Bán hàng, vận hành & kho nguyên liệu',
    full: { categories: [40, 40], coffees: [40, 210], recipes: [40, 480], recipe_steps: [40, 690],
      recipe_materials: [380, 40], materials: [380, 190], material_batches: [380, 380], material_transactions: [380, 610],
      carts: [720, 40], cart_items: [720, 250], orders: [720, 460],
      dining_tables: [1060, 40], order_items: [1060, 230], payments: [1060, 460], shipments: [1060, 690],
      staff: [1400, 40], incidents: [1400, 270] },
    stub: { customers: [1400, 570], customer_addresses: [1400, 650], promotions: [1400, 730] } },
  { key: 'P2', title: 'Khách hàng & CRM',
    full: { customers: [380, 40], customer_addresses: [380, 320], loyalty_point_history: [380, 530],
      customer_groups: [720, 40], customer_group_members: [720, 250], interactions: [720, 400], care_reminders: [720, 650],
      feedbacks: [1060, 40] },
    stub: { orders: [40, 540], staff: [1060, 420] } },
  { key: 'P3', title: 'Đợt giảm giá, voucher & thống kê',
    full: { promotions: [380, 40], promotion_items: [380, 300], vouchers: [720, 40], daily_revenue: [1060, 40] },
    stub: { coffees: [40, 330], customers: [40, 60], orders: [40, 160], incidents: [40, 240] } },
];

function erdPage(OUT, page, level) {
  const physical = level === 'physical';
  const file = `${physical ? 'DB' : 'ERDL'}_${page.key}`;
  const name = `${physical ? 'Database diagram' : 'ERD mức logic'} – ${page.title}`;
  const d = new Diagram(file, name);
  d.v(name, ST.title, 40, 0, 1400, 30);
  const W = 280, HEAD = 28, ROW = 20, OY = 50;
  const rowId = {};
  const table = (tname, x, y, stub) => {
    const [dom, vn, , cols] = T[tname];
    const [fill, stroke] = DOMAIN[dom];
    const shown = stub ? cols.filter(c => c[0].includes('PK')) : cols;
    const h = HEAD + ROW * shown.length;
    const title = physical ? tname : vn;
    const style = stub
      ? `swimlane;startSize=${HEAD};html=1;collapsible=0;fontStyle=3;fontSize=12;fillColor=#f5f5f5;strokeColor=#999999;dashed=1;swimlaneFillColor=#ffffff;`
      : `swimlane;startSize=${HEAD};html=1;collapsible=0;fontStyle=1;fontSize=13;fillColor=${fill};strokeColor=${stroke};swimlaneFillColor=#ffffff;`;
    const t = d.v(stub ? `${title} (xem trang khác)` : title, style, x, y + OY, W, h);
    shown.forEach(([key, col, vnCol, type], i) => {
      const ry = HEAD + i * ROW;
      const label = physical ? col : vnCol;
      const keyHtml = key ? `<b>${key}</b>&nbsp;` : '';
      const colHtml = key.includes('PK') ? `<u>${label}</u>` : label;
      const id = d.v(`${keyHtml}${colHtml}`, 'text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;spacingLeft=6;fontSize=11;overflow=hidden;', 0, ry, physical ? 175 : W, ROW, t, [x, y + OY + ry]);
      if (physical) d.v(type, 'text;html=1;strokeColor=none;fillColor=none;align=right;verticalAlign=middle;spacingRight=6;fontSize=10;fontColor=#555555;', 160, ry, 120, ROW, t, [x + 160, y + OY + ry]);
      rowId[`${tname}.${col}`] = id;
    });
    rowId[tname] = rowId[`${tname}.${shown[0][1]}`];
  };
  for (const [n, [x, y]] of Object.entries(page.full)) table(n, x, y, false);
  for (const [n, [x, y]] of Object.entries(page.stub)) table(n, x, y, true);
  for (const [child, parent, optional, oneToOne] of FKS) {
    const ct = child.split('.')[0];
    if (!page.full[ct]) continue;
    const src = rowId[child], tgt = rowId[parent];
    if (!src) throw new Error(`${file}: thiếu cột ${child}`);
    if (!tgt) throw new Error(`${file}: thiếu bảng cha ${parent} cho ${child} (cần stub)`);
    const start = oneToOne ? 'ERzeroToOne' : 'ERzeroToMany';
    const end = optional ? 'ERzeroToOne' : 'ERmandOne';
    d.e(src, tgt, '', `edgeStyle=entityRelationEdgeStyle;html=1;rounded=0;startArrow=${start};endArrow=${end};startFill=0;endFill=0;startSize=10;endSize=10;strokeColor=#555555;`);
  }
  d.cells.sort((a, b) => (b.includes('edge="1"') ? 1 : 0) - (a.includes('edge="1"') ? 1 : 0));
  const ly = d.b.y1 + 30;
  d.v('Chú thích', 'text;html=1;fontStyle=1;fontSize=13;align=left;', 40, ly, 200, 24);
  Object.values(DOMAIN).forEach(([fill, stroke, label], i) => d.v(label, `rounded=0;html=1;fillColor=${fill};strokeColor=${stroke};fontSize=11;`, 40 + i * 175, ly + 30, 165, 26));
  d.v(`PK: khóa chính (gạch chân) · FK: khóa ngoại · UQ: duy nhất · Crow's foot: | = một, &lt; = nhiều, o = không bắt buộc · Bảng nét đứt: tham chiếu tới bảng ở trang khác`, 'text;html=1;fontSize=11;align=left;', 40, ly + 64, 1300, 22);
  return d.save(OUT, { kind: 'design', title: name, level, page: page.key });
}

// ---------------- Conceptual ERD (Chen) ----------------
const ENT = {
  danhMuc: ['Danh mục', 'Category', 'coffee', 100, 100], caPhe: ['Cà phê', 'Coffee', 'coffee', 700, 100],
  nguyenLieu: ['Nguyên liệu', 'Material', 'stock', 100, 340], gioHang: ['Giỏ hàng', 'Cart', 'sales', 700, 340],
  dotGiamGia: ['Đợt giảm giá', 'Promotion', 'promo', 1300, 340],
  ban: ['Bàn', 'Table', 'sales', 100, 580], donHang: ['Đơn hàng', 'Order', 'sales', 700, 580], voucher: ['Voucher', 'Voucher', 'promo', 1300, 580],
  thanhToan: ['Thanh toán', 'Payment', 'sales', 100, 820], nhanVien: ['Nhân viên', 'Staff', 'staff', 700, 820],
  khachHang: ['Khách hàng', 'Customer', 'customer', 1300, 820], nhomKH: ['Nhóm khách hàng', 'Customer group', 'customer', 1900, 820],
  giaoHang: ['Đơn giao hàng', 'Shipment', 'sales', 400, 1060], suCo: ['Sự cố', 'Incident', 'sales', 1000, 1060], phanHoi: ['Phản hồi', 'Feedback', 'customer', 1300, 1060],
};
// [a, cardA, label, cardB, b, diamondX, diamondY]
const REL = [
  ['danhMuc', '1', 'bao gồm', 'N', 'caPhe', 400, 100],
  ['nguyenLieu', 'N', 'pha chế thành', 'N', 'caPhe', 400, 220], ['caPhe', 'N', 'được thêm vào', 'N', 'gioHang', 700, 220],
  ['caPhe', 'N', 'áp dụng cho', 'N', 'dotGiamGia', 1000, 220], ['caPhe', 'N', 'có trong', 'N', 'donHang', 850, 340],
  ['ban', '1', 'gắn với', 'N', 'gioHang', 400, 460], ['ban', '1', 'thuộc về', 'N', 'donHang', 400, 580], ['gioHang', '1', 'chuyển thành', '1', 'donHang', 700, 460],
  ['dotGiamGia', '1', 'áp dụng cho', 'N', 'donHang', 1000, 460], ['donHang', '1', 'áp dụng', 'N', 'voucher', 1000, 580],
  ['donHang', '1', 'thanh toán bằng', 'N', 'thanhToan', 400, 700], ['nhanVien', '1', 'phục vụ', 'N', 'donHang', 700, 700],
  ['khachHang', '1', 'đặt', 'N', 'donHang', 1000, 700], ['khachHang', '1', 'sở hữu', 'N', 'voucher', 1300, 700],
  ['khachHang', 'N', 'thuộc', 'N', 'nhomKH', 1600, 820],
  ['donHang', '1', 'giao bằng', '1', 'giaoHang', 550, 820], ['donHang', '1', 'phát sinh', 'N', 'suCo', 850, 820],
  ['nhanVien', '1', 'xử lý', 'N', 'suCo', 850, 940], ['suCo', '1', 'đền bù', 'N', 'voucher', 1150, 820],
  ['khachHang', '1', 'gửi', 'N', 'phanHoi', 1300, 940],
];

function conceptual(OUT) {
  const d = new Diagram('ERDC_Conceptual', 'ERD mức khái niệm (Conceptual)');
  d.v('ERD mức khái niệm (Conceptual) – Website bán cà phê Coffeeholic', ST.title, 20, 0, 1400, 30);
  const OY = 50, EW = 150, EH = 50;
  const id = {};
  for (const [k, [vn, en, dom, cx, cy]] of Object.entries(ENT)) {
    const [fill, stroke] = DOMAIN[dom];
    id[k] = d.v(`<b>${vn}</b><br><font style="font-size:9px;color:#666666">${en}</font>`, `rounded=0;whiteSpace=wrap;html=1;fillColor=${fill};strokeColor=${stroke};fontSize=13;`, cx - EW / 2, cy - EH / 2 + OY, EW, EH);
  }
  const card = (ent, dx, dy, text) => {
    const [, , , cx, cy] = ENT[ent];
    const vx = dx - cx, vy = dy - cy, len = Math.hypot(vx, vy), ux = vx / len, uy = vy / len;
    const tExit = Math.min(ux ? EW / 2 / Math.abs(ux) : Infinity, uy ? EH / 2 / Math.abs(uy) : Infinity);
    const px = cx + ux * (tExit + 14) - uy * 11, py = cy + uy * (tExit + 14) + ux * 11;
    d.v(text, 'text;html=1;align=center;verticalAlign=middle;fontStyle=1;fontSize=12;fontColor=#b20000;', px - 10, py - 9 + OY, 20, 18);
  };
  for (const [a, ca, label, cb, b, dx, dy] of REL) {
    const r = d.v(`<i>${label}</i>`, 'rhombus;whiteSpace=wrap;html=1;fontSize=11;fillColor=#ffffff;', dx - 55, dy - 24 + OY, 110, 48);
    d.e(id[a], r, '', 'endArrow=none;html=1;rounded=0;');
    d.e(r, id[b], '', 'endArrow=none;html=1;rounded=0;');
    card(a, dx, dy, ca); card(b, dx, dy, cb);
  }
  d.cells.sort((x, y) => (y.includes('edge="1"') ? 1 : 0) - (x.includes('edge="1"') ? 1 : 0));
  return d.save(OUT, { kind: 'design', title: d.name });
}

// ---------------- Logical ERD (Chen, attributes) – bản của nhóm + các ý bổ sung đã thống nhất ----------------
// entity: [label, cx, cy, w, h, [[attrLabel, angleDeg, radiusScale?], ...]]
const LE = {
  danhMuc: ['Danh mục', 1500, 170, 170, 55, [['Mô tả', 150], ['Tên', 90], ['Thứ tự hiển thị', 30]]],
  caPhe: ['Cà phê', 1500, 520, 170, 55, [['Tên', 135], ['ảnh', 160], ['giá bán', 215], ['mô tả', 245], ['trạng thái', 320]]],
  congThuc: ['Công thức', 1000, 520, 170, 55, [['cách pha (phin / máy / cold brew)', 140], ['phiên bản', 90], ['mô tả', 40], ['thời gian pha', 225], ['đang áp dụng', 315]]],
  nlct: ['Nguyên liệu công thức', 500, 520, 170, 55, [['định lượng', 125], ['ghi chú', 55]]],
  buoc: ['Các bước pha chế', 1000, 870, 170, 55, [['bước số', 215], ['mô tả thao tác', 325]]],
  nguyenLieu: ['Nguyên liệu', 500, 870, 170, 55, [['tên', 150], ['đơn vị tính', 30], ['số lượng tồn', 185], ['tồn tối thiểu', 355], ['trạng thái', 220]]],
  lo: ['Lô nguyên liệu', 500, 1220, 170, 55, [['số lượng nhập', 145], ['số lượng còn lại', 185], ['trạng thái', 225], ['đơn giá nhập', 270], ['hạn sử dụng', 320]]],
  giaoDich: ['Giao dịch nguyên liệu', 1000, 1220, 170, 55, [['loại giao dịch (nhập, xuất, bán, điều chỉnh)', 100, 1.15], ['ghi chú', 145], ['số lượng', 215], ['thời gian', 245]]],
  nhanVien: ['Nhân viên', 1000, 1570, 170, 55, [['họ tên', 125], ['trạng thái làm việc', 155], ['chức vụ', 185], ['số điện thoại', 215], ['email', 245], ['ngày vào làm', 330]]],
  ctdgg: ['Chi tiết đợt giảm giá', 2000, 170, 170, 55, [['mức giảm riêng', 120], ['giá sau giảm', 70], ['số lượng tối đa', 280], ['số lượng đã bán', 325]]],
  dgg: ['Đợt giảm giá', 2500, 170, 170, 55, [['tên chương trình', 150, 1.1], ['trạng thái', 110], ['đơn tối thiểu', 70], ['ngày bắt đầu', 30], ['ngày kết thúc', 0], ['% giảm', 340], ['mô tả', 315], ['áp dụng cho (hóa đơn / cà phê)', 275, 1.5]]],
  ctgh: ['Chi tiết giỏ hàng', 2000, 520, 170, 55, [['Số lượng', 125], ['ghi chú', 90], ['tạm tính', 55]]],
  gioHang: ['Giỏ hàng', 2500, 520, 170, 55, [['mã phiên', 125], ['trạng thái (đang mở / đã đặt / bỏ dở)', 90, 1.1], ['ngày tạo', 50]]],
  ban: ['Bàn', 2500, 870, 170, 55, [['số bàn', 145], ['trạng thái', 115], ['mã QR', 250], ['khu vực', 280], ['sức chứa', 310]]],
  ctdh: ['Chi tiết đơn hàng', 1500, 870, 170, 55, [['ghi chú', 150], ['số lượng', 30], ['đơn giá', 355], ['thành tiền', 322]]],
  donHang: ['Đơn hàng', 1500, 1220, 190, 80, [['mã đơn', 58, 1.0], ['ngày giờ đặt', 72, 1.35], ['ghi chú', 108, 1.15], ['trạng thái', 132, 1.2], ['loại đơn (tại bàn / mang về / giao)', 158, 1.25],
    ['kênh đặt hàng (tại quầy / QR / online)', 182, 1.25], ['tổng tiền', 205, 1.25], ['tạm tính', 300, 1.05], ['tiền giảm', 312, 1.55], ['lý do hủy', 120, 1.6], ['thời gian nhận hẹn', 145, 1.75]]],
  thanhVien: ['Thành viên nhóm', 3480, 950, 170, 55, [['thêm thủ công', 90], ['ngày thêm', 270]]],
  nhomKH: ['Nhóm khách hàng', 3980, 950, 170, 55, [['tên nhóm', 110], ['mô tả', 60], ['loại tiêu chí', 15], ['điều kiện phân nhóm', 330], ['ngày tính lại', 285]]],
  phanHoi: ['Phản hồi', 3720, 1520, 170, 55, [['chủ đề', 125], ['phân loại', 95], ['cảm xúc', 65], ['nội dung', 35], ['trạng thái', 5], ['đã chuyển cấp', 335], ['phương án xử lý', 305], ['mức hài lòng', 275], ['ngày gửi', 245], ['ngày đóng', 215]]],
  tuongTac: ['Tương tác', 3780, 1880, 170, 55, [['kênh', 125], ['nội dung', 90], ['ghi chú', 55], ['trạng thái', 20], ['ngày nhắc chăm sóc', 345], ['thời gian', 315], ['thời điểm kết thúc', 230]]],
  khachHang: ['Khách hàng', 2950, 1220, 170, 55, [['họ tên', 105], ['số điện thoại', 80], ['email', 55], ['điểm tích lũy', 30], ['ngày đăng ký', 215]]],
  diaChi: ['Địa chỉ khách hàng', 2950, 1570, 170, 55, [['nhãn (nhà / công ty)', 40], ['người nhận', 10], ['sđt người nhận', 340], ['địa chỉ', 240], ['mặc định', 290]]],
  dgh: ['Đơn giao hàng', 2450, 1570, 170, 55, [['phí giao hàng', 100], ['thời điểm đặt xe', 65], ['đơn vị vận chuyển', 30], ['thời điểm tài xế nhận đơn', 205], ['mã vận đơn', 235], ['trạng thái', 265], ['thời điểm giao xong', 300], ['ghi chú', 335]]],
  thanhToan: ['Thanh toán', 1500, 1670, 170, 55, [['hình thức', 200, 0.95], ['số tiền', 228, 0.95], ['trạng thái', 252, 0.95], ['mã giao dịch', 288, 0.95], ['thời điểm trả', 312, 0.95], ['số tiền hoàn', 340, 0.95]]],
  suCo: ['Sự cố', 1000, 1920, 170, 55, [['loại sự cố', 150], ['mức độ nghiêm trọng', 180], ['lý do', 210], ['mô tả', 240], ['ngày ghi nhận', 270], ['ngày xử lý', 300], ['trạng thái xử lý', 330]]],
  voucher: ['Voucher', 1750, 1920, 170, 55, [['mã voucher', 70], ['giá trị giảm', 40], ['trạng thái sử dụng', 10], ['điều kiện áp dụng', 340], ['ngày phát hành', 310], ['ngày hết hạn', 230]]],
};
// [a, cardA, label, cardB, b, diamondX, diamondY, pointsA?, pointsB?]
const LR = [
  ['danhMuc', '1', 'bao gồm', 'N', 'caPhe', 1500, 345], ['congThuc', '1', 'pha chế thành', '1', 'caPhe', 1250, 520],
  ['nlct', 'N', 'tiêu hao', '1', 'congThuc', 750, 520], ['nlct', 'N', 'thuộc', '1', 'nguyenLieu', 500, 695],
  ['congThuc', '1', 'gồm', 'N', 'buoc', 1000, 695], ['nguyenLieu', '1', 'gồm', 'N', 'lo', 500, 1045],
  ['lo', '1', 'tác động', 'N', 'giaoDich', 750, 1220], ['giaoDich', 'N', 'thực hiện', '0..1', 'nhanVien', 1000, 1395],
  ['ctdh', '1', 'phát sinh', 'N', 'giaoDich', 1250, 1045],
  ['caPhe', '1', 'áp dụng cho', 'N', 'ctdgg', 1750, 345], ['dgg', '1', 'bao gồm', 'N', 'ctdgg', 2250, 170],
  ['caPhe', '1', 'nằm trong', 'N', 'ctdh', 1500, 695], ['ctdh', 'N', 'có trong', '1', 'donHang', 1500, 1045],
  ['caPhe', '1', 'được thêm vào', 'N', 'ctgh', 1750, 520], ['ctgh', 'N', 'có trong', '1', 'gioHang', 2250, 520],
  ['ban', '1', 'có', 'N', 'gioHang', 2500, 695], ['khachHang', '1', 'mở', 'N', 'gioHang', 2725, 870],
  ['gioHang', '1', 'chuyển thành', '0..1', 'donHang', 2000, 870], ['ban', '1', 'thuộc về', 'N', 'donHang', 2000, 1045],
  ['dgg', '1', 'áp dụng cho', 'N', 'donHang', 2030, 680],
  ['nhanVien', '1', 'Tạo', 'N', 'donHang', 1250, 1395], ['khachHang', '1', 'mua', 'N', 'donHang', 2225, 1220],
  ['donHang', '1', 'thanh toán bằng', 'N', 'thanhToan', 1500, 1445], ['donHang', '1', 'tạo ra', '0..1', 'dgh', 1975, 1395],
  ['dgh', 'N', 'giao đến', '1', 'diaChi', 2700, 1570], ['khachHang', '1', 'có', 'N', 'diaChi', 2950, 1395],
  ['donHang', '1', 'Xảy ra', 'N', 'suCo', 1250, 1570], ['nhanVien', '1', 'Xử lý', 'N', 'suCo', 1000, 1745],
  ['donHang', '1', 'áp dụng cho', 'N', 'voucher', 1625, 1570], ['suCo', '1', 'Đền bù', 'N', 'voucher', 1375, 1920],
  ['khachHang', '1', 'Sở hữu', 'N', 'voucher', 2600, 2080, [[4380, 1220], [4380, 2080]], [[1750, 2080]]],
  ['khachHang', 'N', 'thuộc', '1', 'thanhVien', 3230, 1060], ['nhomKH', '1', 'gồm', 'N', 'thanhVien', 3720, 950],
  ['khachHang', '1', 'gửi', 'N', 'phanHoi', 3330, 1480], ['khachHang', '1', 'có', 'N', 'tuongTac', 3400, 1790],
];

function erdLogical(OUT) {
  const d = new Diagram('ERDL_Logical', 'ERD mức Logical');
  d.v('ERD mức Logical – Quán cà phê', ST.title, 40, 0, 1400, 36);
  const OY = 40;
  const id = {};
  const ellipseW = t => Math.min(190, Math.max(84, t.length * 6.4 + 26));
  for (const [k, [label, cx, cy, w, h, attrs]] of Object.entries(LE)) {
    id[k] = d.v(label, 'rounded=0;whiteSpace=wrap;html=1;fontSize=13;', cx - w / 2, cy - h / 2 + OY, w, h);
    for (const [a, deg, rs = 1] of attrs) {
      const rad = deg * Math.PI / 180, rx = (w / 2 + 95) * rs, ry = (h / 2 + 62) * rs;
      const ax = cx + Math.cos(rad) * rx, ay = cy - Math.sin(rad) * ry;
      const aw = ellipseW(a), ah = a.length > 26 ? 46 : 36;
      const aid = d.v(a, 'ellipse;whiteSpace=wrap;html=1;fontSize=11;', ax - aw / 2, ay - ah / 2 + OY, aw, ah);
      d.e(id[k], aid, '', 'endArrow=none;html=1;rounded=0;');
    }
  }
  const card = (ent, tx, ty, text) => {
    const [, cx, cy, w, h] = LE[ent];
    const vx = tx - cx, vy = ty - cy, len = Math.hypot(vx, vy), ux = vx / len, uy = vy / len;
    const tExit = Math.min(ux ? w / 2 / Math.abs(ux) : Infinity, uy ? h / 2 / Math.abs(uy) : Infinity);
    const px = cx + ux * (tExit + 16) - uy * 12, py = cy + uy * (tExit + 16) + ux * 12;
    d.v(text, 'text;html=1;align=center;verticalAlign=middle;fontStyle=1;fontSize=13;fontColor=#b20000;', px - 16, py - 9 + OY, 32, 18);
  };
  for (const [a, ca, label, cb, b, dx, dy, ptsA, ptsB] of LR) {
    const r = d.v(label, 'rhombus;whiteSpace=wrap;html=1;fontSize=12;', dx - 62, dy - 26 + OY, 124, 52);
    const shift = p => p && p.map(([x, y]) => [x, y + OY]);
    d.e(id[a], r, '', `endArrow=none;html=1;rounded=0;${ptsA ? 'edgeStyle=orthogonalEdgeStyle;exitX=1;exitY=0.5;exitDx=0;exitDy=0;entryX=1;entryY=0.5;entryDx=0;entryDy=0;' : ''}`, shift(ptsA));
    d.e(r, id[b], '', `endArrow=none;html=1;rounded=0;${ptsB ? 'edgeStyle=orthogonalEdgeStyle;exitX=0;exitY=0.5;exitDx=0;exitDy=0;entryX=0.5;entryY=1;entryDx=0;entryDy=0;' : ''}`, shift(ptsB));
    const firstA = ptsA ? ptsA[0] : [dx, dy], lastB = ptsB ? ptsB[ptsB.length - 1] : [dx, dy];
    card(a, firstA[0], firstA[1], ca); card(b, lastB[0], lastB[1], cb);
  }
  d.cells.sort((x, y) => (y.includes('edge="1"') ? 1 : 0) - (x.includes('edge="1"') ? 1 : 0));
  return d.save(OUT, { kind: 'design', title: d.name });
}

function architecture(OUT) {
  const d = new Diagram('ARCH01_KienTrucHeThong', 'Sơ đồ kiến trúc hệ thống');
  d.v('Sơ đồ kiến trúc hệ thống – Website bán cà phê Coffeeholic', ST.title, 20, 0, 1200, 30);
  const layer = (label, x, y, w, h, bottom) => d.v(label, `rounded=1;arcSize=3;html=1;dashed=1;fillColor=#fafafa;strokeColor=#999999;verticalAlign=${bottom ? 'bottom' : 'top'};fontStyle=1;fontSize=13;spacingTop=4;spacingBottom=4;`, x, y, w, h);
  const box = (label, x, y, w, h, fill, stroke, extra = '') => d.v(label, `rounded=1;arcSize=8;whiteSpace=wrap;html=1;fillColor=${fill};strokeColor=${stroke};fontSize=12;align=left;spacingLeft=10;spacingRight=6;verticalAlign=middle;${extra}`, x, y, w, h);
  const link = (a, b, label, extra = '', pts) => d.e(a, b, label, `edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;endArrow=block;endFill=1;fontSize=11;labelBackgroundColor=#ffffff;${extra}`, pts);

  layer('Người dùng', 20, 50, 220, 560);
  const kh = d.v('Khách hàng\n(quét QR / website)', ST.actor + 'whiteSpace=nowrap;', 110, 100, 40, 70);
  const nv = d.v('Nhân viên', ST.actor, 110, 280, 40, 70);
  const ql = d.v('Quản lý', ST.actor, 110, 450, 40, 70);

  layer('Tầng giao diện (Frontend)', 290, 50, 300, 560);
  const fe = box('<b>coffee-shop-fe</b><br>React 18 + TypeScript + Vite<br>Ant Design · React Query · Zustand<br>Axios (REST) · STOMP/SockJS (thời gian thực)<br><br><i>Trang khách hàng</i> (quét QR tại bàn / online): thực đơn, giỏ hàng, đặt hàng, voucher, theo dõi đơn, phản hồi<br><i>Trang quản trị:</i> order tại quầy, đơn hàng, pha chế &amp; phục vụ, kho, CRM, đợt giảm giá &amp; voucher, phân tích<br><br>Đóng gói Docker + Nginx', 310, 140, 260, 380, '#dae8fc', '#6c8ebf');

  layer('API Gateway', 640, 50, 280, 560);
  const gw = box('<b>nginx-gateway</b> (Render)<br>Cổng vào duy nhất của frontend<br><br>/api/crm/* → app-crm<br>/api/promotions/* → app-promotions<br>/api/stats/* → app-stats<br>/ws (WebSocket) → app-core<br>/api/* còn lại → app-core', 660, 190, 240, 280, '#fff2cc', '#d6b656');

  layer('Tầng dịch vụ (Backend) – Spring Boot 3.3 · Java 17 · Docker trên Render', 970, 50, 470, 560);
  const crm = box('<b>app-crm</b> – Quản lý quan hệ khách hàng<br>Hồ sơ khách hàng, tương tác &amp; chăm sóc, phản hồi, phân nhóm, hành vi, phân tích khách hàng', 990, 90, 430, 90, '#d5e8d4', '#82b366');
  const stats = box('<b>app-stats</b> – Thống kê<br>Báo cáo doanh thu, phân tích khách hàng và hiệu quả giảm giá (truy vấn trực tiếp đơn hàng, thanh toán)', 990, 195, 430, 90, '#d5e8d4', '#82b366');
  const promo = box('<b>app-promotions</b> – Khuyến mãi<br>Đợt giảm giá (cho hóa đơn / cà phê cụ thể), voucher (khách hàng sở hữu), phân tích hiệu quả giảm giá', 990, 300, 430, 90, '#d5e8d4', '#82b366');
  const core = box('<b>app-core</b> – Sales &amp; Operation<br>Cà phê &amp; thực đơn, giỏ hàng, đặt hàng &amp; thanh toán, đơn hàng, vận hành pha chế &amp; giao nhận, kho nguyên liệu<br>WebSocket STOMP (thông báo đơn hàng, tồn kho) · Scheduler (cảnh báo tồn kho)', 990, 405, 430, 160, '#d5e8d4', '#82b366');

  layer('Tầng dữ liệu', 1490, 50, 250, 560);
  const db = d.v('<b>PostgreSQL (Neon)</b><br>CSDL dùng chung<br>cho 4 dịch vụ', 'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=15;fillColor=#e1d5e7;strokeColor=#9673a6;fontSize=12;', 1530, 240, 170, 160);
  d.v('JDBC / JPA', 'text;html=1;fontSize=11;align=center;', 1440, 300, 60, 20);

  layer('Dịch vụ bên ngoài (dự kiến tích hợp)', 970, 640, 470, 110, true);
  const pay = box('Cổng thanh toán trực tuyến', 990, 660, 200, 50, '#f8cecc', '#b85450', 'align=center;spacingLeft=0;');
  const ship = box('Đối tác giao hàng (đặt xe / tài xế)', 1220, 660, 200, 50, '#f8cecc', '#b85450', 'align=center;spacingLeft=0;');

  layer('Triển khai &amp; vận hành (DevOps)', 20, 640, 900, 110);
  const gh = box('<b>GitHub</b> – monorepo coffee-shop-be + coffee-shop-fe', 40, 680, 260, 50, '#f5f5f5', '#666666');
  const ci = box('<b>GitHub Actions</b> – CI riêng cho từng app (lọc theo thư mục)', 330, 680, 270, 50, '#f5f5f5', '#666666');
  box('<b>Sao lưu CSDL</b> – pg_dump PostgreSQL (Neon) hằng tuần bằng GitHub Actions', 630, 680, 270, 50, '#f5f5f5', '#666666');

  [kh, nv, ql].forEach(a => link(a, fe, 'HTTPS', 'exitX=1;exitY=0.5;exitDx=0;exitDy=0;entryX=0;entryY=0.5;entryDx=0;entryDy=0;'));
  link(fe, gw, 'REST /api\nWebSocket /ws');
  [crm, stats, promo, core].forEach(s => link(gw, s, '', 'exitX=1;exitY=0.5;exitDx=0;exitDy=0;entryX=0;entryY=0.5;entryDx=0;entryDy=0;'));
  [crm, stats, promo, core].forEach(s => link(s, db, '', 'exitX=1;exitY=0.5;exitDx=0;exitDy=0;entryX=0;entryY=0.5;entryDx=0;entryDy=0;'));
  link(core, pay, '', 'dashed=1;exitX=0.233;exitY=1;exitDx=0;exitDy=0;entryX=0.5;entryY=0;entryDx=0;entryDy=0;');
  link(core, ship, '', 'dashed=1;exitX=0.767;exitY=1;exitDx=0;exitDy=0;entryX=0.5;entryY=0;entryDx=0;entryDy=0;');
  link(gh, ci, '');
  return d.save(OUT, { kind: 'design', title: d.name });
}

// ---------------- Physical database diagram: one image, ELK layout + orthogonal routing, crow's foot (IE) ----------------
async function dbDiagram(OUT) {
  const ELK = require('elkjs/lib/elk.bundled.js');
  const elk = new ELK();
  const W = 270, HEAD = 28, ROW = 20;
  const names = Object.keys(T);
  const graph = {
    id: 'root',
    layoutOptions: {
      'elk.algorithm': 'layered', 'elk.direction': 'RIGHT', 'elk.edgeRouting': 'ORTHOGONAL',
      'elk.layered.spacing.nodeNodeBetweenLayers': '110', 'elk.spacing.nodeNode': '45',
      'elk.spacing.edgeEdge': '14', 'elk.spacing.edgeNode': '24', 'elk.layered.spacing.edgeNodeBetweenLayers': '24',
      'elk.layered.crossingMinimization.strategy': 'LAYER_SWEEP', 'elk.layered.nodePlacement.strategy': 'NETWORK_SIMPLEX',
      'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES', 'elk.layered.mergeEdges': 'false',
    },
    children: names.map(n => ({ id: n, width: W, height: HEAD + ROW * T[n][3].length })),
    // parent → child so referenced tables sit to the left
    edges: FKS.map(([c, p], i) => ({ id: 'e' + i, sources: [p], targets: [c.split('.')[0]] })),
  };
  const res = await elk.layout(graph);
  const d = new Diagram('DB01_DatabaseDiagram', 'Database diagram');
  const OX = 40, OY = 70;
  d.v('Database diagram (mức vật lý) – PostgreSQL – Website bán cà phê Coffeeholic', ST.title, 40, 10, 1600, 36);
  const box = {};
  for (const n of res.children) {
    const [dom, , , cols] = T[n.id];
    const [fill, stroke] = DOMAIN[dom];
    const x = n.x + OX, y = n.y + OY;
    const t = d.v(n.id, `swimlane;startSize=${HEAD};html=1;collapsible=0;fontStyle=1;fontSize=13;fillColor=${fill};strokeColor=${stroke};swimlaneFillColor=#ffffff;`, x, y, W, n.height);
    cols.forEach(([key, col, , type], i) => {
      const ry = HEAD + i * ROW;
      const keyHtml = key ? `<b>${key}</b>&nbsp;` : '';
      const colHtml = key.includes('PK') ? `<u>${col}</u>` : col;
      d.v(`${keyHtml}${colHtml}`, 'text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;spacingLeft=6;fontSize=11;overflow=hidden;', 0, ry, 175, ROW, t, [x, y + ry]);
      d.v(type, 'text;html=1;strokeColor=none;fillColor=none;align=right;verticalAlign=middle;spacingRight=6;fontSize=10;fontColor=#555555;', 160, ry, 110, ROW, t, [x + 160, y + ry]);
    });
    box[n.id] = { id: t, x, y, w: W, h: n.height };
  }
  const frac = (b, px, py) => [(px - b.x) / b.w, (py - b.y) / b.h].map(v => Math.min(1, Math.max(0, v)).toFixed(4));
  for (const e of res.edges) {
    const [c, p, optional, oneToOne] = FKS[+e.id.slice(1)];
    const child = c.split('.')[0];
    const sec = e.sections[0];
    const sp = [sec.startPoint.x + OX, sec.startPoint.y + OY], ep = [sec.endPoint.x + OX, sec.endPoint.y + OY];
    const [ex, ey] = frac(box[p], ...sp), [nx, ny] = frac(box[child], ...ep);
    const bends = (sec.bendPoints || []).map(b => [b.x + OX, b.y + OY]);
    const parentEnd = optional ? 'ERzeroToOne' : 'ERmandOne';
    const childEnd = oneToOne ? 'ERzeroToOne' : 'ERzeroToMany';
    d.e(box[p].id, box[child].id, '', `edgeStyle=none;html=1;rounded=0;startArrow=${parentEnd};endArrow=${childEnd};startFill=0;endFill=0;startSize=10;endSize=10;strokeColor=#555555;exitX=${ex};exitY=${ey};exitDx=0;exitDy=0;entryX=${nx};entryY=${ny};entryDx=0;entryDy=0;`, bends);
  }
  d.cells.sort((a, b) => (b.includes('edge="1"') ? 1 : 0) - (a.includes('edge="1"') ? 1 : 0));
  // legend
  const ly = d.b.y1 + 30;
  Object.values(DOMAIN).forEach(([fill, stroke, label], i) => d.v(label, `rounded=0;html=1;fillColor=${fill};strokeColor=${stroke};fontSize=11;`, 40 + i * 175, ly, 165, 26));
  d.v("Ký hiệu crow's foot (IE): ||  bắt buộc đúng một · o|  không hoặc một · o&lt;  không hoặc nhiều. PK: khóa chính (gạch chân) · FK: khóa ngoại · UQ: duy nhất.", 'text;html=1;fontSize=12;align=left;', 40, ly + 36, 1400, 22);
  return d.save(OUT, { kind: 'design', title: d.name });
}

module.exports = { erdPage, dbDiagram, conceptual, erdLogical, architecture, PAGES, T, FKS, DOMAIN };
