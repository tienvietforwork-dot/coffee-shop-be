// Use case diagrams. One detailed diagram per ULNL module (each <= 15 elements incl. actors), no login.
// ulnl: the module's 10 functions from the ULNL sheet → use case key, list of keys, or 'NA:<lý do>'.
const { UC, autoUC } = require('./lib');

const NA_SIZE = 'NA:Quán chỉ bán cà phê một size, không có topping / biến thể';

// UC-00: each overall use case belongs to exactly one actor; Khách hàng only gets customer actions.
const GROUPS = [
  { key: 'k1', label: 'Xem thực đơn', actor: 'KH', details: ['UC-01'] },
  { key: 'k2', label: 'Đặt hàng (quét QR tại bàn / online)', actor: 'KH', details: ['UC-02', 'UC-03', 'UC-14'] },
  { key: 'k3', label: 'Theo dõi & hủy đơn hàng', actor: 'KH', details: ['UC-04'] },
  { key: 'k4', label: 'Gửi phản hồi & đánh giá', actor: 'KH', details: ['UC-09'] },
  { key: 'n1', label: 'Quản lý cà phê & thực đơn', actor: 'NV', details: ['UC-01'] },
  { key: 'n2', label: 'Xử lý đơn hàng (tại quầy / QR / online)', actor: 'NV', details: ['UC-04', 'UC-14'] },
  { key: 'n3', label: 'Pha chế & phục vụ, giao nhận', actor: 'NV', details: ['UC-05'] },
  { key: 'n4', label: 'Quản lý kho nguyên liệu', actor: 'NV', details: ['UC-06'] },
  { key: 'n5', label: 'Chăm sóc khách hàng', actor: 'NV', details: ['UC-07', 'UC-08', 'UC-09'] },
  { key: 'q1', label: 'Quản lý đợt giảm giá & voucher', actor: 'QL', details: ['UC-13', 'UC-14'] },
  { key: 'q2', label: 'Phân tích khách hàng & hiệu quả giảm giá', actor: 'QL', details: ['UC-10', 'UC-11', 'UC-12', 'UC-15'] },
];

// ULNL modules that are out of scope for this shop
const OUT_OF_SCOPE = [
  { tab: 'Product Merchandising', modules: ['Product Placement', 'Product Bundle', 'Cross-sell', 'Upsell', 'Recommendation', 'Merchandising Schedule', 'Merchandising Analytics'],
    reason: 'Quán chỉ bán cà phê pha chế theo một thực đơn cố định, không có tùy chọn món (chỉ ghi chú): không trưng bày, không combo, không bán kèm / nâng cấp / đề xuất.' },
  { tab: 'Promotion', modules: ['Promotion Rule', 'Promotion Targeting', 'Campaign'],
    reason: 'Khuyến mãi chỉ gồm đợt giảm giá (cho hóa đơn hoặc cà phê cụ thể) và voucher (cho hóa đơn, khách hàng sở hữu) theo ERD: không có bộ quy tắc phức tạp, nhắm mục tiêu hay chiến dịch.' },
];

const SPECS = [
  // ===================== Sales & Operation =====================
  { file: 'UC01_CaPhe_ThucDon', code: 'UC-01', title: 'Cà phê & thực đơn', group: 'g1', tab: 'Sales & Operation', module: 'Product & Menu',
    items: {
      xem: ['Xem thực đơn', ['KH']], loc: ['Lọc thực đơn theo danh mục'],
      batTat: ['Bật/tắt hiển thị cà phê', ['NV']], avail: ['Thiết lập khả năng bán (còn món / hết món)', ['NV']],
      tao: ['Tạo cà phê mới', ['QL']], phanLoai: ['Phân loại cà phê theo danh mục'], congThuc: ['Thiết lập công thức pha chế'],
      gia: ['Cập nhật giá bán', ['QL']], ngung: ['Ngừng bán cà phê', ['QL']], publish: ['Công bố thực đơn', ['QL']],
    },
    rels: [['ext', 'loc', 'xem'], ['inc', 'tao', 'phanLoai'], ['inc', 'tao', 'congThuc']],
    ulnl: [['Tạo sản phẩm', 'tao'], ['Cập nhật giá', 'gia'], ['Tạo size', NA_SIZE], ['Tạo topping', NA_SIZE], ['Thiết lập product variant', NA_SIZE],
      ['Bật/tắt sản phẩm', 'batTat'], ['Thiết lập availability', 'avail'], ['Phân loại sản phẩm', 'phanLoai'], ['Publish menu', 'publish'], ['Ngừng bán sản phẩm', 'ngung']] },

  { file: 'UC02_GioHang', code: 'UC-02', title: 'Giỏ hàng', group: 'g2', tab: 'Sales & Operation', module: 'Cart',
    items: {
      qr: ['Mở giỏ hàng qua mã QR tại bàn', ['KH']], them: ['Thêm cà phê vào giỏ', ['KH']], ghiChu: ['Ghi chú món (ít đá, ít đường...)'],
      xem: ['Xem giỏ hàng', ['KH']], sl: ['Thay đổi số lượng'], xoa: ['Xóa cà phê khỏi giỏ'], tamTinh: ['Tính tạm tính'], gia: ['Kiểm tra giá hiện hành'],
      luu: ['Lưu giỏ hàng', ['KH']], khoiPhuc: ['Khôi phục giỏ hàng', ['KH']],
    },
    rels: [['ext', 'ghiChu', 'them'], ['ext', 'sl', 'xem'], ['ext', 'xoa', 'xem'], ['inc', 'xem', 'tamTinh'], ['inc', 'tamTinh', 'gia'], ['inc', 'khoiPhuc', 'tamTinh']],
    ulnl: [['Thêm sản phẩm', 'them'], ['Xóa sản phẩm', 'xoa'], ['Đổi quantity', 'sl'], ['Chọn size', NA_SIZE], ['Chọn topping', NA_SIZE],
      ['Ghi chú món', 'ghiChu'], ['Kiểm tra giá', 'gia'], ['Tính subtotal', 'tamTinh'], ['Lưu cart', 'luu'], ['Khôi phục cart', 'khoiPhuc']] },

  { file: 'UC03_DatHang_ThanhToan', code: 'UC-03', title: 'Đặt hàng & thanh toán (Checkout)', group: 'g2', tab: 'Sales & Operation', module: 'Checkout',
    items: {
      base: ['Đặt hàng (Checkout)', ['KH']],
      hinhThuc: ['Chọn hình thức nhận hàng (tại bàn / mang về / giao hàng)'], diaChi: ['Chọn địa chỉ giao hàng'], phiShip: ['Tính phí giao hàng'],
      thoiGian: ['Chọn thời gian nhận hàng'], pttt: ['Chọn phương thức thanh toán'], voucher: ['Nhập voucher'],
      avail: ['Kiểm tra khả năng bán'], tong: ['Tính tổng tiền thanh toán'], dotGiam: ['Áp dụng đợt giảm giá đang chạy'], xacNhan: ['Xác nhận đặt hàng'], fail: ['Xử lý đặt hàng thất bại'],
    },
    rels: [['inc', 'base', 'hinhThuc'], ['ext', 'diaChi', 'hinhThuc'], ['inc', 'diaChi', 'phiShip'], ['ext', 'thoiGian', 'base'], ['inc', 'base', 'pttt'],
      ['ext', 'voucher', 'base'], ['inc', 'base', 'avail'], ['inc', 'base', 'tong'], ['inc', 'tong', 'dotGiam'], ['inc', 'base', 'xacNhan'], ['ext', 'fail', 'base']],
    ulnl: [['Chọn địa chỉ', 'diaChi'], ['Chọn pickup/delivery', 'hinhThuc'], ['Chọn thời gian nhận', 'thoiGian'], ['Chọn payment method', 'pttt'], ['Nhập voucher', 'voucher'],
      ['Kiểm tra availability', 'avail'], ['Tính shipping fee', 'phiShip'], ['Tính final amount', ['tong', 'dotGiam']], ['Xác nhận order', 'xacNhan'], ['Xử lý checkout failure', 'fail']] },

  { file: 'UC04_QuanLyDonHang', code: 'UC-04', title: 'Xử lý & theo dõi đơn hàng', group: 'g3', tab: 'Sales & Operation', module: 'Order Management',
    items: {
      theoDoi: ['Theo dõi trạng thái đơn hàng', ['KH']], huy: ['Hủy đơn hàng', ['KH', 'NV']], lyDo: ['Ghi nhận lý do'], hoanTien: ['Hoàn tiền', ['QL']],
      tao: ['Tạo đơn hàng (order tại quầy / từ QR, online)', ['NV']], xacNhan: ['Xác nhận đơn hàng', ['NV']], tuChoi: ['Từ chối đơn hàng', ['NV']],
      chuanBi: ['Cập nhật trạng thái "Đang chuẩn bị"', ['NV']], sanSang: ['Cập nhật trạng thái "Sẵn sàng"', ['NV']],
      phanCong: ['Phân công nhận / giao hàng', ['NV']], dieuPhoi: ['Điều phối giao hàng', ['NV']], hoanTat: ['Hoàn tất đơn hàng', ['NV']],
    },
    rels: [['inc', 'huy', 'lyDo'], ['ext', 'hoanTien', 'huy'], ['inc', 'tuChoi', 'lyDo']],
    ulnl: [['Create order', 'tao'], ['Confirm order', 'xacNhan'], ['Reject order', 'tuChoi'], ['Prepare order', 'chuanBi'], ['Ready', 'sanSang'],
      ['Assign pickup', 'phanCong'], ['Dispatch', 'dieuPhoi'], ['Complete', 'hoanTat'], ['Cancel', 'huy'], ['Refund', 'hoanTien']] },

  { file: 'UC05_VanHanh', code: 'UC-05', title: 'Vận hành pha chế & giao nhận', group: 'g4', tab: 'Sales & Operation', module: 'Operation',
    items: {
      tiepNhan: ['Tiếp nhận đơn hàng', ['NV']], batDau: ['Pha chế theo công thức', ['NV']], tuyChinh: ['Tùy chỉnh đồ uống theo ghi chú'],
      xong: ['Đánh dấu món đã pha xong', ['NV']], banGiao: ['Bàn giao đơn hàng', ['NV']], taiBan: ['Phục vụ đồ uống tại bàn'], quay: ['Bàn giao tại quầy (mang về)'], taiXe: ['Bàn giao cho tài xế'],
      tre: ['Xử lý đơn hàng trễ', ['NV']], suCo: ['Ghi nhận sự cố đơn hàng', ['NV']], xuLySuCo: ['Xử lý sự cố (làm lại món nếu cần)', ['QL']], denBu: ['Đền bù bằng voucher'],
      barista: ['Phân công barista', ['QL']],
    },
    rels: [['ext', 'tuyChinh', 'batDau'], ['gen', 'taiBan', 'banGiao'], ['gen', 'quay', 'banGiao'], ['gen', 'taiXe', 'banGiao'], ['ext', 'denBu', 'xuLySuCo']],
    ulnl: [['Receive order', 'tiepNhan'], ['Assign barista', 'barista'], ['Start preparation', 'batDau'], ['Customize drink', 'tuyChinh'], ['Mark ready', 'xong'],
      ['Handover', ['banGiao', 'taiBan']], ['Pickup', 'quay'], ['Delivery handoff', 'taiXe'], ['Delay handling', 'tre'], ['Order incident', ['suCo', 'xuLySuCo', 'denBu']]] },

  { file: 'UC06_KhoNguyenLieu', code: 'UC-06', title: 'Quản lý kho nguyên liệu', group: 'g5', tab: 'Bổ sung từ ERD', module: 'Kho nguyên liệu',
    items: {
      xem: ['Xem tồn kho nguyên liệu', ['NV']], nhap: ['Nhập kho nguyên liệu', ['NV']], lo: ['Ghi nhận lô nguyên liệu (số lượng, đơn giá, hạn dùng)'],
      gd: ['Ghi nhận giao dịch kho'], xuat: ['Xuất kho nguyên liệu', ['NV']], fefo: ['Chọn lô theo hạn dùng gần nhất (FEFO)'],
      canhBao: ['Nhận cảnh báo nguyên liệu sắp hết / sắp hết hạn', ['NV']], yc: ['Lập yêu cầu nhập hàng'],
      danhMuc: ['Quản lý danh mục nguyên liệu', ['QL']], minStock: ['Thiết lập mức tồn tối thiểu', ['QL']], dieuChinh: ['Điều chỉnh tồn kho (kiểm kê)', ['QL']],
    },
    rels: [['inc', 'nhap', 'lo'], ['inc', 'nhap', 'gd'], ['inc', 'xuat', 'fefo'], ['inc', 'xuat', 'gd'], ['inc', 'dieuChinh', 'gd'], ['ext', 'yc', 'canhBao']],
    ulnl: null },

  // ===================== CRM =====================
  { file: 'UC07_HoSoKhachHang', code: 'UC-07', title: 'Quản lý hồ sơ khách hàng (Customer 360)', group: 'g6', tab: 'CRM', module: 'Customer 360',
    items: {
      tao: ['Tạo hồ sơ khách hàng', ['NV']], capNhat: ['Cập nhật hồ sơ khách hàng', ['NV']], xem: ['Xem hồ sơ khách hàng', ['NV']],
      ls: ['Xem lịch sử mua hàng'], chiTieu: ['Xem tổng chi tiêu'], tanSuat: ['Xem tần suất mua hàng'], yeuThich: ['Xem cà phê yêu thích'],
      tuongTac: ['Xem lịch sử tương tác'], phanHoi: ['Xem phản hồi của khách hàng'], timeline: ['Xem dòng thời gian hoạt động'],
      gop: ['Gộp hồ sơ khách hàng bị trùng', ['QL']],
    },
    rels: ['ls', 'chiTieu', 'tanSuat', 'yeuThich', 'tuongTac', 'phanHoi', 'timeline'].map(k => ['ext', k, 'xem']),
    ulnl: [['Tạo hồ sơ khách hàng', 'tao'], ['Cập nhật hồ sơ khách hàng', 'capNhat'], ['Xem lịch sử mua hàng', 'ls'], ['Xem tổng chi tiêu', 'chiTieu'], ['Xem tần suất mua hàng', 'tanSuat'],
      ['Xem sản phẩm yêu thích', 'yeuThich'], ['Xem lịch sử tương tác', 'tuongTac'], ['Xem phản hồi của khách hàng', 'phanHoi'], ['Xem dòng thời gian hoạt động của khách hàng', 'timeline'], ['Gộp các hồ sơ khách hàng bị trùng', 'gop']] },

  { file: 'UC08_TuongTacKhachHang', code: 'UC-08', title: 'Quản lý tương tác & chăm sóc khách hàng', group: 'g7', tab: 'CRM', module: 'Customer Interaction',
    items: {
      ghiNhan: ['Tạo lịch sử tương tác', ['NV']], goi: ['Ghi nhận cuộc gọi'], chat: ['Ghi nhận cuộc trò chuyện'], email: ['Ghi nhận email'], ghiChu: ['Thêm ghi chú'],
      xemLs: ['Xem lịch sử tương tác', ['NV']], lich: ['Đặt lịch nhắc chăm sóc', ['NV']], chamSoc: ['Thực hiện chăm sóc lại khách hàng', ['NV']],
      ketThuc: ['Kết thúc tương tác', ['NV']], phanCong: ['Phân công nhân viên phụ trách', ['QL']],
    },
    rels: [['gen', 'goi', 'ghiNhan'], ['gen', 'chat', 'ghiNhan'], ['gen', 'email', 'ghiNhan'], ['ext', 'ghiChu', 'ghiNhan']],
    ulnl: [['Tạo lịch sử tương tác', 'ghiNhan'], ['Ghi nhận cuộc gọi', 'goi'], ['Ghi nhận cuộc trò chuyện', 'chat'], ['Ghi nhận email', 'email'], ['Thêm ghi chú', 'ghiChu'],
      ['Phân công nhân viên phụ trách', 'phanCong'], ['Thực hiện chăm sóc lại khách hàng', 'chamSoc'], ['Xem lịch sử tương tác', 'xemLs'], ['Đặt lịch nhắc chăm sóc', 'lich'], ['Kết thúc tương tác', 'ketThuc']] },

  { file: 'UC09_PhanHoiKhachHang', code: 'UC-09', title: 'Phản hồi & khiếu nại của khách hàng', group: 'g7', tab: 'CRM', module: 'Customer Feedback',
    items: {
      gui: ['Gửi phản hồi / khiếu nại', ['KH']], danhGia: ['Đánh giá kết quả xử lý', ['KH']],
      tiepNhan: ['Tiếp nhận & phân loại phản hồi', ['NV']], chuDe: ['Gắn chủ đề cho phản hồi'], camXuc: ['Đánh giá cảm xúc khách hàng'],
      phanHoi: ['Phản hồi khách hàng', ['NV']], chuyenCap: ['Chuyển cấp xử lý vấn đề'], dong: ['Đóng phản hồi', ['NV']],
      phanCong: ['Phân công người phụ trách', ['QL']], pheDuyet: ['Phê duyệt phương án xử lý', ['QL']],
      xuHuong: ['Phân tích xu hướng phản hồi', ['QL']], lapLai: ['Xác định các khiếu nại lặp lại'],
    },
    rels: [['inc', 'tiepNhan', 'chuDe'], ['inc', 'tiepNhan', 'camXuc'], ['ext', 'chuyenCap', 'phanHoi'], ['ext', 'lapLai', 'xuHuong']],
    ulnl: [['Tiếp nhận phản hồi', 'tiepNhan'], ['Phân loại phản hồi', 'tiepNhan'], ['Đánh giá cảm xúc của khách hàng', 'camXuc'], ['Gắn chủ đề cho phản hồi', 'chuDe'], ['Phân công người phụ trách', 'phanCong'],
      ['Phản hồi khách hàng', 'phanHoi'], ['Chuyển cấp xử lý vấn đề', ['chuyenCap', 'pheDuyet']], ['Đóng phản hồi', 'dong'], ['Phân tích xu hướng phản hồi', 'xuHuong'], ['Xác định các khiếu nại lặp lại', 'lapLai']] },

  { file: 'UC10_PhanNhomKhachHang', code: 'UC-10', title: 'Phân nhóm khách hàng', group: 'g8', tab: 'CRM', module: 'Customer Segmentation',
    items: {
      them: ['Thêm khách hàng vào nhóm', ['NV']], xoa: ['Xóa khách hàng khỏi nhóm', ['NV']],
      tao: ['Tạo nhóm khách hàng', ['QL']], xemTruoc: ['Xem trước danh sách khách hàng thuộc nhóm'], tieuChi: ['Xác định tiêu chí phân nhóm'],
      chiTieu: ['Phân nhóm theo mức chi tiêu'], tanSuat: ['Phân nhóm theo tần suất mua hàng'], ganNhat: ['Phân nhóm theo thời gian mua gần nhất'], soThich: ['Phân nhóm theo sở thích cà phê'],
      tinhLai: ['Tính toán lại nhóm khách hàng', ['QL']],
    },
    rels: [['ext', 'xemTruoc', 'tao'], ['inc', 'tao', 'tieuChi'], ['gen', 'chiTieu', 'tieuChi'], ['gen', 'tanSuat', 'tieuChi'], ['gen', 'ganNhat', 'tieuChi'], ['gen', 'soThich', 'tieuChi'], ['inc', 'tinhLai', 'tieuChi']],
    ulnl: [['Tạo nhóm khách hàng', 'tao'], ['Xác định tiêu chí phân nhóm', 'tieuChi'], ['Phân nhóm theo mức chi tiêu', 'chiTieu'], ['Phân nhóm theo tần suất mua hàng', 'tanSuat'], ['Phân nhóm theo thời gian mua hàng gần nhất', 'ganNhat'],
      ['Phân nhóm theo sở thích sản phẩm', 'soThich'], ['Xem trước danh sách khách hàng thuộc nhóm', 'xemTruoc'], ['Thêm khách hàng vào nhóm', 'them'], ['Tính toán lại nhóm khách hàng', 'tinhLai'], ['Xóa khách hàng khỏi nhóm', 'xoa']] },

  { file: 'UC11_HanhViKhachHang', code: 'UC-11', title: 'Theo dõi hành vi khách hàng', group: 'g8', tab: 'CRM', module: 'Customer Behavior',
    items: {
      base: ['Theo dõi hành vi mua hàng', ['QL']], ls: ['Theo dõi lịch sử mua hàng'], soThich: ['Theo dõi sở thích cà phê'], danhMuc: ['Xác định danh mục cà phê yêu thích'],
      tanSuat: ['Theo dõi tần suất mua hàng'], ganNhat: ['Theo dõi thời gian mua gần nhất'], giaTri: ['Theo dõi giá trị đơn hàng trung bình'],
      khongHD: ['Phát hiện khách hàng không hoạt động'], giaTriCao: ['Phát hiện khách hàng có giá trị cao'],
      soSanh: ['So sánh hành vi mua hàng giữa các giai đoạn', ['QL']], baoCao: ['Lập báo cáo hành vi khách hàng', ['QL']],
    },
    rels: [['inc', 'base', 'ls'], ['inc', 'base', 'soThich'], ['inc', 'soThich', 'danhMuc'], ['inc', 'base', 'tanSuat'], ['inc', 'base', 'ganNhat'], ['inc', 'base', 'giaTri'],
      ['ext', 'khongHD', 'base'], ['ext', 'giaTriCao', 'base']],
    ulnl: [['Theo dõi lịch sử mua hàng', 'ls'], ['Theo dõi sở thích sản phẩm', 'soThich'], ['Theo dõi tần suất mua hàng', 'tanSuat'], ['Theo dõi thời gian mua hàng gần nhất', 'ganNhat'], ['Theo dõi giá trị đơn hàng trung bình', 'giaTri'],
      ['Xác định danh mục sản phẩm yêu thích', 'danhMuc'], ['Phát hiện khách hàng không hoạt động', 'khongHD'], ['Phát hiện khách hàng có giá trị cao', 'giaTriCao'], ['So sánh hành vi mua hàng giữa các giai đoạn', 'soSanh'], ['Lập báo cáo hành vi khách hàng', 'baoCao']] },

  { file: 'UC12_PhanTichKhachHang', code: 'UC-12', title: 'Phân tích khách hàng (Customer Insight)', group: 'g8', tab: 'CRM', module: 'Customer Insight',
    items: {
      base: ['Phân tích thói quen mua hàng', ['QL']], xuHuong: ['Phân tích xu hướng mua hàng'], quanTam: ['Phân tích mối quan tâm giữa khách hàng và cà phê'],
      nhieuNhat: ['Xác định khách hàng mua nhiều nhất'], roiBo: ['Xác định khách hàng có nguy cơ rời bỏ'], thuongXuyen: ['Xác định khách hàng mua thường xuyên'],
      chiTieuCao: ['Xác định khách hàng có mức chi tiêu cao'], soSanh: ['So sánh giữa các nhóm khách hàng'],
      giaTri: ['Phân tích giá trị khách hàng', ['QL']], tangTruong: ['Lập báo cáo tăng trưởng khách hàng', ['QL']], xuat: ['Xuất dữ liệu phân tích khách hàng'],
    },
    rels: [['inc', 'base', 'xuHuong'], ['inc', 'base', 'quanTam'], ...['nhieuNhat', 'roiBo', 'thuongXuyen', 'chiTieuCao', 'soSanh'].map(k => ['ext', k, 'base']), ['ext', 'xuat', 'tangTruong']],
    ulnl: [['Xác định khách hàng mua hàng nhiều nhất', 'nhieuNhat'], ['Xác định khách hàng có nguy cơ rời bỏ', 'roiBo'], ['Xác định khách hàng mua hàng thường xuyên', 'thuongXuyen'], ['Xác định khách hàng có mức chi tiêu cao', 'chiTieuCao'],
      ['Phân tích mối quan tâm giữa khách hàng và sản phẩm', 'quanTam'], ['Phân tích xu hướng mua hàng', 'xuHuong'], ['Phân tích giá trị khách hàng', 'giaTri'], ['So sánh giữa các nhóm khách hàng', 'soSanh'], ['Lập báo cáo tăng trưởng khách hàng', 'tangTruong'], ['Xuất dữ liệu phân tích khách hàng', 'xuat']] },

  // ===================== Promotion =====================
  { file: 'UC13_DotGiamGia', code: 'UC-13', title: 'Quản lý đợt giảm giá', group: 'q1', tab: 'Promotion', module: 'Promotion Management',
    items: {
      tao: ['Tạo đợt giảm giá', ['QL']], hoaDon: ['Giảm giá cho hóa đơn (% giảm, giá trị đơn tối thiểu)'], caPhe: ['Giảm giá cho cà phê cụ thể'],
      chonCaPhe: ['Chọn cà phê, mức giảm riêng, số lượng tối đa'], thoiGian: ['Thiết lập thời gian áp dụng'],
      kichHoat: ['Kích hoạt đợt giảm giá', ['QL']], tamDung: ['Tạm dừng đợt giảm giá', ['QL']], ketThuc: ['Kết thúc đợt giảm giá', ['QL']], nhanBan: ['Nhân bản đợt giảm giá', ['QL']],
    },
    rels: [['gen', 'hoaDon', 'tao'], ['gen', 'caPhe', 'tao'], ['inc', 'caPhe', 'chonCaPhe'], ['inc', 'tao', 'thoiGian']],
    ulnl: [['Tạo chương trình khuyến mãi', 'tao'], ['Xác định mức giảm giá', ['hoaDon', 'chonCaPhe']], ['Thiết lập điều kiện khuyến mãi', 'hoaDon'], ['Thiết lập phạm vi sản phẩm', ['caPhe', 'chonCaPhe']],
      ['Thiết lập phạm vi khách hàng', 'NA:Đợt giảm giá áp dụng cho mọi khách hàng; ưu đãi riêng cho từng khách hàng dùng voucher'], ['Thiết lập thời gian áp dụng', 'thoiGian'],
      ['Kích hoạt chương trình', 'kichHoat'], ['Tạm dừng chương trình', 'tamDung'], ['Kết thúc chương trình', 'ketThuc'], ['Nhân bản chương trình', 'nhanBan']] },

  { file: 'UC14_Voucher', code: 'UC-14', title: 'Voucher', group: 'q1', tab: 'Promotion', module: 'Voucher',
    items: {
      suDung: ['Sử dụng voucher khi đặt hàng', ['KH']], apDung: ['Áp dụng voucher tại quầy', ['NV']], kiemTra: ['Kiểm tra voucher (chủ sở hữu, chưa dùng, còn hạn, đủ điều kiện)'], tuChoi: ['Từ chối voucher không hợp lệ'],
      tao: ['Phát hành voucher', ['QL']], ma: ['Tạo mã voucher'], giaTri: ['Thiết lập giá trị giảm'], dieuKien: ['Thiết lập điều kiện áp dụng (giá trị đơn tối thiểu)'], thoiHan: ['Thiết lập thời hạn voucher'],
      gan: ['Gán voucher cho khách hàng', ['QL']], theoDoi: ['Theo dõi tình trạng sử dụng voucher', ['QL']],
    },
    rels: [['inc', 'suDung', 'kiemTra'], ['inc', 'apDung', 'kiemTra'], ['ext', 'tuChoi', 'kiemTra'], ...['ma', 'giaTri', 'dieuKien', 'thoiHan'].map(k => ['inc', 'tao', k])],
    ulnl: [['Tạo voucher', 'tao'], ['Tạo mã voucher', 'ma'], ['Thiết lập giới hạn sử dụng', 'NA:Mỗi voucher dùng một lần (trạng thái sử dụng), được kiểm tra khi áp dụng'],
      ['Thiết lập giới hạn theo khách hàng', 'gan'], ['Thiết lập giá trị đơn hàng tối thiểu', 'dieuKien'], ['Thiết lập thời hạn voucher', 'thoiHan'], ['Gán voucher cho đối tượng khách hàng', 'gan'],
      ['Sử dụng voucher', ['suDung', 'apDung']], ['Từ chối voucher không hợp lệ', 'tuChoi'], ['Theo dõi tình trạng sử dụng voucher', 'theoDoi']] },

  { file: 'UC15_PhanTichKhuyenMai', code: 'UC-15', title: 'Phân tích hiệu quả đợt giảm giá & voucher', group: 'q2', tab: 'Promotion', module: 'Promotion Analytics',
    items: {
      base: ['Phân tích hiệu quả đợt giảm giá & voucher', ['QL']], luot: ['Theo dõi lượt sử dụng giảm giá / voucher'], doanhThu: ['Phân tích doanh thu'], soDon: ['Phân tích mức tăng số lượng đơn hàng'],
      giaTri: ['So sánh giá trị đơn hàng trung bình'], phanHoi: ['Phân tích phản hồi khách hàng'], chuyenDoi: ['Phân tích tỷ lệ sử dụng voucher'],
      chiPhi: ['Phân tích tổng tiền giảm giá'], roi: ['Phân tích ROI'], soSanh: ['So sánh các đợt giảm giá', ['QL']], baoCao: ['Lập báo cáo'],
    },
    rels: [...['luot', 'doanhThu', 'soDon', 'giaTri', 'phanHoi', 'chuyenDoi', 'chiPhi', 'roi'].map(k => ['gen', k, 'base']), ['ext', 'baoCao', 'soSanh']],
    ulnl: [['Theo dõi lượt sử dụng khuyến mãi', 'luot'], ['Phân tích doanh thu', 'doanhThu'], ['Phân tích mức tăng số lượng đơn hàng', 'soDon'], ['So sánh giá trị đơn hàng trung bình', 'giaTri'], ['Phân tích phản hồi khách hàng', 'phanHoi'],
      ['Phân tích tỷ lệ chuyển đổi', 'chuyenDoi'], ['So sánh các chiến dịch', 'soSanh'], ['Phân tích chi phí khuyến mãi', 'chiPhi'], ['Phân tích ROI', 'roi'], ['Lập báo cáo', 'baoCao']] },
];

function overview() {
  const d = new UC('UC00_TongQuat', 'UC-00: Use case tổng quát', 'UC-00: Biểu đồ use case tổng quát – Website bán cà phê Coffeeholic');
  const W = 290, L = 300, R = 700, S = 86, TOP = 110;
  const id = {};
  const kh = GROUPS.filter(g => g.actor === 'KH'), staff = GROUPS.filter(g => g.actor !== 'KH');
  staff.forEach((g, i) => { id[g.key] = d.uc(g.label, R, TOP + i * S + (g.actor === 'QL' ? 30 : 0), W, 58); });
  const right = staff.length * S + TOP + 30;
  const khTop = TOP + (right - TOP - kh.length * S) / 2;
  kh.forEach((g, i) => { id[g.key] = d.uc(g.label, L, khTop + i * S, W, 58); });
  d.boundary(260, 50, R + W + 40 - 260, right - 50 + 10);
  const kha = d.actor('Khách hàng', 100, khTop + 1.5 * S - 10);
  const nv = d.actor('Nhân viên', R + W + 120, TOP + 2 * S - 10);
  const ql = d.actor('Quản lý', R + W + 180, TOP + 5.5 * S + 30);
  d.actorGen(ql, nv);
  const A = { KH: kha, NV: nv, QL: ql };
  for (const g of GROUPS) d.assoc(A[g.actor], id[g.key]);
  return d;
}

module.exports = function usecases(OUT) {
  const R = [];
  const o = overview();
  R.push(o.save(OUT, { kind: 'uc', title: o.name, code: 'UC-00' }));
  for (const s of SPECS) {
    const d = autoUC(s);
    R.push(d.save(OUT, { kind: 'uc', title: d.name, code: s.code, group: GROUPS.filter(g => g.details.includes(s.code)).map(g => g.label).join('; '), tab: s.tab, module: s.module,
      labels: Object.fromEntries(Object.entries(s.items).map(([k, v]) => [k, v[0]])), ulnl: s.ulnl }));
  }
  return R;
};
module.exports.GROUPS = GROUPS;
module.exports.OUT_OF_SCOPE = OUT_OF_SCOPE;
