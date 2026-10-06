// All BPMN process diagrams + step descriptions (used by the PDF builder).
const { BPMN, P } = require('./lib');

module.exports = function bpmn(OUT) {
  const R = [];
  const save = (d, steps) => R.push(d.save(OUT, { kind: 'bpmn', title: d.name, steps }));

  // ===== BPMN-01 Đặt hàng & thanh toán =====
  {
    const d = new BPMN('BPMN01_DatHang_ThanhToan', 'BPMN-01: Quy trình đặt hàng & thanh toán', 'BPMN-01: Quy trình đặt hàng & thanh toán (tại quầy / quét QR tại bàn / online)',
      'Coffeeholic – Đặt hàng & thanh toán', [['Khách hàng', 220], ['Hệ thống website', 200], ['Nhân viên', 180]], 3120);
    const KH = 170, HT = 380, NV = 560, NV_LOW = 628;
    const s = d.start('Khách có nhu cầu\nđặt món', 110, KH);
    const g0 = d.gw('Kênh đặt hàng?', 220, KH);
    const n0 = d.task('Nhận order trực tiếp tại quầy: nhập món, ghi chú, thu tiền', 220, NV, 170, 64);
    const t1 = d.task('Quét mã QR tại bàn / truy cập website', 392, KH, 130, 60);
    const t2 = d.task('Xem thực đơn, thêm cà phê vào giỏ (kèm ghi chú)', 560, KH);
    const t3 = d.task('Kiểm tra giỏ hàng & bấm "Đặt hàng"', 740, KH);
    const a1 = d.task('Kiểm tra khả năng bán & giá hiện hành', 740, HT);
    const g1 = d.gw('Món còn bán?', 910, HT);
    const t4 = d.task('Chọn hình thức nhận hàng (tại bàn / mang về / giao hàng)', 1060, KH, 160, 64);
    const g2 = d.gw('Giao hàng?', 1220, KH);
    const t5 = d.task('Chọn địa chỉ giao hàng', 1350, 235, 150, 56);
    const a2 = d.task('Tính phí giao hàng', 1350, HT);
    const g3 = d.gw('', 1520, KH);
    const t6 = d.task('Chọn PTTT, nhập voucher (nếu có), xác nhận đặt hàng', 1690, KH, 160, 64);
    const a3 = d.task('Áp dụng đợt giảm giá, kiểm tra voucher, tính tổng tiền', 1690, HT, 160, 64);
    const g4 = d.gw('Voucher hợp lệ?', 1870, HT);
    const g5 = d.gw('Phương thức\nthanh toán?', 2040, HT);
    const a4 = d.task('Xử lý thanh toán trực tuyến', 2210, HT);
    const g6 = d.gw('Thành công?', 2380, HT);
    const g7 = d.gw('', 2550, HT);
    const a5 = d.task('Gửi yêu cầu đặt hàng tới cửa hàng', 2720, HT);
    const b1 = d.task('Tiếp nhận yêu cầu đặt hàng', 2720, NV);
    const gm = d.gw('', 2880, NV);
    const b2 = d.task('Kiểm tra & tạo đơn hàng', 3030, NV);
    const a6 = d.task('Gửi xác nhận đơn hàng cho khách', 3030, HT);
    const e = d.end('Khách nhận\nxác nhận đơn hàng', 3030, KH);
    d.flow(s, g0);
    d.flow(g0, n0, 'Tại quầy', P(0.5, 1, 0.5, 0));
    d.flow(n0, gm, '', P(0.5, 1, 0.5, 1), [[220, NV_LOW], [2880, NV_LOW]]);
    d.flow(g0, t1, 'QR / online', P(1, 0.5, 0, 0.5));
    d.flow(t1, t2); d.flow(t2, t3); d.flow(t3, a1); d.flow(a1, g1);
    d.flow(g1, t2, 'Không – điều chỉnh giỏ', P(0.5, 1, 0.5, 1), [[910, 455], [560, 455]]);
    d.flow(g1, t4, 'Có', P(0.5, 0, 0, 0.5), [[910, KH]]);
    d.flow(t4, g2);
    d.flow(g2, t5, 'Có', P(0.5, 1, 0, 0.5), [[1220, 235]]);
    d.flow(g2, g3, 'Không (tại bàn / mang về)', P(1, 0.5, 0, 0.5));
    d.flow(t5, a2); d.flow(a2, g3, '', P(1, 0.5, 0.5, 1), [[1520, HT]]);
    d.flow(g3, t6); d.flow(t6, a3); d.flow(a3, g4);
    d.flow(g4, t6, 'Không hợp lệ', P(0.5, 0, 0.7, 0), [[1870, 92], [1722, 92]]);
    d.flow(g4, g5, 'Hợp lệ / không dùng', P(1, 0.5, 0, 0.5));
    d.flow(g5, a4, 'Online', P(1, 0.5, 0, 0.5));
    d.flow(g5, g7, 'Tiền mặt', P(0.5, 1, 0.5, 1), [[2040, 455], [2550, 455]]);
    d.flow(a4, g6);
    d.flow(g6, t6, 'Thất bại – chọn lại PTTT', P(0.5, 0, 0.3, 0), [[2380, 76], [1658, 76]]);
    d.flow(g6, g7, 'Thành công', P(1, 0.5, 0, 0.5));
    d.flow(g7, a5); d.flow(a5, b1); d.flow(b1, gm); d.flow(gm, b2); d.flow(b2, a6); d.flow(a6, e);
    save(d, [
      ['Khách hàng', 'Khách có nhu cầu đặt món, chọn kênh đặt: gọi trực tiếp tại quầy, quét mã QR tại bàn hoặc đặt online trên website.'],
      ['Nhân viên', 'Tại quầy: nhân viên nhận order, nhập cà phê và ghi chú vào hệ thống, thu tiền; chuyển sang bước 12.'],
      ['Khách hàng', 'QR / online: xem thực đơn, thêm cà phê vào giỏ (kèm ghi chú), kiểm tra giỏ và bấm "Đặt hàng".'],
      ['Hệ thống website', 'Kiểm tra khả năng bán và giá hiện hành. Có món hết → quay lại bước 3 để khách điều chỉnh giỏ.'],
      ['Khách hàng', 'Chọn hình thức nhận hàng: tại bàn (QR), mang về hoặc giao hàng.'],
      ['Khách hàng → Hệ thống', 'Nếu giao hàng: khách chọn địa chỉ giao hàng, hệ thống tính phí giao hàng.'],
      ['Khách hàng', 'Chọn phương thức thanh toán, nhập voucher (nếu có), xác nhận đặt hàng.'],
      ['Hệ thống website', 'Áp dụng đợt giảm giá đang chạy (cho hóa đơn hoặc cà phê cụ thể), kiểm tra voucher, tính tổng tiền. Voucher không hợp lệ → quay lại bước 7.'],
      ['Hệ thống website', 'Thanh toán online: xử lý qua cổng thanh toán, thất bại → quay lại bước 7. Tiền mặt: thanh toán khi nhận đồ.'],
      ['Hệ thống website', 'Gửi yêu cầu đặt hàng tới cửa hàng.'],
      ['Nhân viên', 'Tiếp nhận yêu cầu đặt hàng.'],
      ['Nhân viên', 'Kiểm tra và tạo đơn hàng (cả đơn tại quầy lẫn đơn QR / online).'],
      ['Hệ thống → Khách hàng', 'Gửi xác nhận đơn hàng; đơn chuyển sang quy trình BPMN-02.'],
    ]);
  }

  // ===== BPMN-02 Xử lý đơn hàng & vận hành =====
  {
    const d = new BPMN('BPMN02_XuLyDon_VanHanh', 'BPMN-02: Quy trình xử lý đơn hàng & vận hành', 'BPMN-02: Quy trình xử lý đơn hàng, pha chế & phục vụ / giao nhận',
      'Coffeeholic – Xử lý đơn hàng & vận hành', [['Khách hàng', 170], ['Hệ thống website', 170], ['Nhân viên', 340], ['Quản lý', 170]], 3120);
    const KH = 145, HT = 315, N1 = 455, N2 = 560, N3 = 665, QL = 825;
    const s = d.start('Có đơn\nhàng mới', 120, N1);
    const b1 = d.task('Kiểm tra đơn hàng (món, khả năng đáp ứng)', 260, N1);
    const g1 = d.gw('Chấp nhận đơn?', 430, N1);
    const b2 = d.task('Từ chối đơn & ghi nhận lý do', 430, N2);
    const c1 = d.task('Hoàn tiền (nếu khách đã thanh toán)', 430, QL);
    const e1 = d.end('Đơn bị từ chối\n(hệ thống thông báo khách)', 250, QL);
    const b3 = d.task('Xác nhận đơn hàng', 600, N1);
    const c2 = d.task('Phân công barista', 770, QL);
    const b4 = d.task('Pha chế theo công thức & ghi chú món', 940, N1);
    const a3 = d.task('Trừ kho nguyên liệu theo công thức', 940, HT);
    const g2 = d.gw('Có sự cố?', 1110, N1);
    const b5 = d.task('Ghi nhận sự cố đơn hàng', 1110, N2);
    const c3 = d.task('Xử lý sự cố (làm lại món nếu cần)', 1110, QL);
    const g3 = d.gw('Cần đền bù?', 1280, QL);
    const c4 = d.task('Phát voucher đền bù cho khách', 1450, QL);
    const g5 = d.gw('', 1620, QL);
    const g4 = d.gw('', 1620, N1);
    const b6 = d.task('Đánh dấu đơn "Sẵn sàng"', 1790, N1);
    const a4 = d.task('Cập nhật trạng thái & thông báo khách', 1790, HT);
    const g6 = d.gw('Hình thức nhận?', 1960, N1);
    const g6b = d.gw('', 1960, N2);
    const bT = d.task('Phục vụ đồ uống tại bàn (thu tiền nếu chưa thanh toán)', 2150, N1, 170, 60);
    const bM = d.task('Bàn giao tại quầy (thu tiền nếu chưa thanh toán)', 2340, N2, 170, 60);
    const b8 = d.task('Đặt xe, phân công tài xế', 2340, N3);
    const b9 = d.task('Bàn giao đơn cho tài xế', 2520, N3);
    const kT = d.task('Nhận đồ uống tại bàn', 2150, 100, 150, 44);
    const kM = d.task('Nhận đồ uống tại quầy', 2340, 145, 150, 44);
    const kG = d.task('Nhận hàng từ tài xế', 2520, 192, 150, 44);
    const gk = d.gw('', 2700, KH);
    const a5 = d.task('Hoàn tất đơn hàng & cộng điểm tích lũy', 2870, HT);
    const e2 = d.end('Đơn hàng\nhoàn tất', 3030, HT);
    d.flow(s, b1); d.flow(b1, g1);
    d.flow(g1, b2, 'Không', P(0.5, 1, 0.5, 0));
    d.flow(b2, c1); d.flow(c1, e1);
    d.flow(g1, b3, 'Có', P(1, 0.5, 0, 0.5));
    d.flow(b3, c2, '', P(1, 0.5, 0.5, 0), [[770, N1]]);
    d.flow(c2, b4, '', P(1, 0.5, 0.5, 1), [[940, QL]]);
    d.flow(b4, a3); d.flow(a3, g2, '', P(1, 0.5, 0.5, 0), [[1110, HT]]);
    d.flow(g2, b5, 'Có', P(0.5, 1, 0.5, 0));
    d.flow(b5, c3); d.flow(c3, g3);
    d.flow(g3, c4, 'Có', P(1, 0.5, 0, 0.5));
    d.flow(g3, g5, 'Không', P(0.5, 1, 0.5, 1), [[1280, 885], [1620, 885]]);
    d.flow(c4, g5);
    d.flow(g5, g4, '', P(0.5, 0, 0.5, 1));
    d.flow(g2, g4, 'Không', P(1, 0.5, 0, 0.5));
    d.flow(g4, b6); d.flow(b6, a4); d.flow(a4, g6, '', P(1, 0.5, 0.5, 0), [[1960, HT]]);
    d.flow(g6, bT, 'Tại bàn', P(1, 0.5, 0, 0.5));
    d.flow(g6, g6b, 'Khác', P(0.5, 1, 0.5, 0));
    d.flow(g6b, bM, 'Mang về', P(1, 0.5, 0, 0.5));
    d.flow(g6b, b8, 'Giao hàng', P(0.5, 1, 0, 0.5), [[1960, N3]]);
    d.flow(b8, b9);
    d.flow(bT, kT, '', P(0.5, 0, 0.5, 1));
    d.flow(bM, kM, '', P(0.5, 0, 0.5, 1));
    d.flow(b9, kG, '', P(0.5, 0, 0.5, 1));
    d.flow(kT, gk, '', P(1, 0.5, 0.5, 0), [[2700, 100]]);
    d.flow(kM, gk, '', P(1, 0.5, 0, 0.5));
    d.flow(kG, gk, '', P(1, 0.5, 0.5, 1), [[2700, 192]]);
    d.flow(gk, a5, '', P(1, 0.5, 0.5, 0), [[2870, KH]]);
    d.flow(a5, e2);
    save(d, [
      ['Nhân viên', 'Có đơn hàng mới (tại quầy, QR hoặc online), kiểm tra món và khả năng đáp ứng.'],
      ['Nhân viên → Quản lý', 'Nếu không chấp nhận: từ chối đơn, ghi nhận lý do; Quản lý hoàn tiền nếu khách đã thanh toán; hệ thống thông báo khách. Kết thúc.'],
      ['Nhân viên', 'Nếu chấp nhận: xác nhận đơn hàng.'],
      ['Quản lý', 'Phân công barista.'],
      ['Nhân viên', 'Pha chế theo công thức và ghi chú món.'],
      ['Hệ thống website', 'Trừ kho nguyên liệu theo công thức (giao dịch "bán" gắn với chi tiết đơn hàng, xuất theo lô gần hết hạn trước).'],
      ['Nhân viên → Quản lý', 'Nếu có sự cố: nhân viên ghi nhận; Quản lý xử lý (làm lại món nếu cần); nếu cần đền bù thì phát voucher cho khách (một sự cố có thể đền bù nhiều voucher).'],
      ['Nhân viên → Hệ thống', 'Đánh dấu đơn "Sẵn sàng"; hệ thống cập nhật trạng thái và thông báo khách.'],
      ['Nhân viên → Khách hàng', 'Tại bàn: phục vụ đồ uống tại bàn (thu tiền nếu chưa thanh toán); khách nhận đồ uống tại bàn.'],
      ['Nhân viên → Khách hàng', 'Mang về: bàn giao tại quầy (thu tiền nếu chưa thanh toán); khách nhận đồ uống tại quầy.'],
      ['Nhân viên → Khách hàng', 'Giao hàng: đặt xe, phân công tài xế, bàn giao đơn cho tài xế; khách nhận hàng từ tài xế.'],
      ['Hệ thống website', 'Hoàn tất đơn hàng và cộng điểm tích lũy cho khách (nếu có hồ sơ). Kết thúc.'],
    ]);
  }

  // ===== BPMN-03 Quản lý kho nguyên liệu =====
  {
    const d = new BPMN('BPMN03_QuanLyKho', 'BPMN-03: Quy trình nhập / xuất kho nguyên liệu', 'BPMN-03: Quy trình nhập / xuất kho nguyên liệu',
      'Coffeeholic – Quản lý kho', [['Nhân viên', 170], ['Hệ thống website', 230]], 1910);
    const NV = 145, H1 = 290, H2 = 390;
    const s = d.start('Có nhu cầu\nnhập / xuất kho', 120, NV);
    const n1 = d.task('Lập phiếu nhập / xuất nguyên liệu', 260, NV);
    const g1 = d.gw('Loại phiếu?', 430, NV);
    const n2 = d.task('Nhập thông tin lô (số lượng, đơn giá, hạn dùng)', 600, NV, 160, 60);
    const h1 = d.task('Tạo lô nguyên liệu mới', 600, H1);
    const h2 = d.task('Kiểm tra số lượng tồn', 430, H2);
    const g2 = d.gw('Đủ tồn kho?', 600, H2);
    const h3 = d.task('Chọn lô xuất theo hạn dùng gần nhất (FEFO)', 770, H2, 160, 60);
    const g3 = d.gw('', 940, H1);
    const h4 = d.task('Cập nhật tồn kho & ghi giao dịch kho', 1110, H1);
    const g4 = d.gw('Dưới mức tồn\ntối thiểu?', 1280, H1);
    const h5 = d.task('Gửi cảnh báo nguyên liệu sắp hết', 1280, H2);
    const q1 = d.task('Nhận cảnh báo & lập yêu cầu nhập hàng', 1620, NV);
    const e1 = d.end('Đã lập yêu cầu\nnhập hàng', 1790, NV);
    const e2 = d.end('Hoàn tất\ngiao dịch kho', 1450, H1);
    d.flow(s, n1); d.flow(n1, g1);
    d.flow(g1, n2, 'Nhập', P(1, 0.5, 0, 0.5));
    d.flow(n2, h1);
    d.flow(g1, h2, 'Xuất', P(0.5, 1, 0.5, 0));
    d.flow(h2, g2);
    d.flow(g2, n1, 'Không – báo không đủ tồn', P(0.5, 1, 0.5, 1), [[600, 445], [260, 445]]);
    d.flow(g2, h3, 'Có', P(1, 0.5, 0, 0.5));
    d.flow(h1, g3);
    d.flow(h3, g3, '', P(1, 0.5, 0.5, 1), [[940, H2]]);
    d.flow(g3, h4); d.flow(h4, g4);
    d.flow(g4, h5, 'Có', P(0.5, 1, 0.5, 0));
    d.flow(h5, q1, '', P(1, 0.5, 0.5, 1), [[1620, H2]]);
    d.flow(q1, e1);
    d.flow(g4, e2, 'Không', P(1, 0.5, 0, 0.5));
    save(d, [
      ['Nhân viên', 'Có nhu cầu nhập hoặc xuất kho, lập phiếu nhập / xuất nguyên liệu.'],
      ['Nhân viên → Hệ thống', 'Phiếu nhập: nhân viên nhập thông tin lô (số lượng, đơn giá, hạn sử dụng); hệ thống tạo lô nguyên liệu mới.'],
      ['Hệ thống website', 'Phiếu xuất: kiểm tra số lượng tồn. Không đủ → thông báo và quay lại bước 1.'],
      ['Hệ thống website', 'Đủ tồn: chọn lô xuất theo hạn dùng gần nhất (FEFO).'],
      ['Hệ thống website', 'Cập nhật số lượng tồn và ghi giao dịch kho (nhập / xuất / điều chỉnh).'],
      ['Hệ thống website', 'Kiểm tra mức tồn tối thiểu. Nếu không thấp hơn → hoàn tất giao dịch kho.'],
      ['Hệ thống → Nhân viên', 'Nếu thấp hơn: gửi cảnh báo nguyên liệu sắp hết; nhân viên nhận cảnh báo và lập yêu cầu nhập hàng. Kết thúc.'],
    ]);
  }

  // ===== BPMN-04 Xử lý phản hồi khách hàng =====
  {
    const d = new BPMN('BPMN04_PhanHoiKhachHang', 'BPMN-04: Quy trình xử lý phản hồi khách hàng', 'BPMN-04: Quy trình xử lý phản hồi / khiếu nại khách hàng',
      'Coffeeholic – Xử lý phản hồi', [['Khách hàng', 170], ['Hệ thống website', 170], ['Nhân viên', 170], ['Quản lý', 170]], 1560);
    const KH = 145, HT = 315, NV = 485, QL = 655;
    const s = d.start('Có phản hồi\n/ khiếu nại', 110, KH);
    const k1 = d.task('Gửi phản hồi qua biểu mẫu trên website', 260, KH);
    const h1 = d.task('Ghi nhận, đánh giá cảm xúc & tạo ticket', 260, HT);
    const n1 = d.task('Tiếp nhận, phân loại & gắn chủ đề', 260, NV);
    const g1 = d.gw('Vượt thẩm quyền\nnhân viên?', 430, NV);
    const n2 = d.task('Lập phương án xử lý (xin lỗi, đổi trả, tặng voucher...)', 600, NV, 160, 64);
    const q1 = d.task('Phê duyệt phương án giải quyết / đền bù', 600, QL, 160, 60);
    const g2 = d.gw('', 770, NV);
    const h2 = d.task('Gửi kết quả giải quyết cho khách hàng', 940, HT);
    const k2 = d.task('Xem phương án & đánh giá kết quả', 940, KH);
    const g3 = d.gw('Hài lòng?', 1110, KH);
    const h3 = d.task('Đóng ticket', 1280, HT);
    const e = d.end('Phản hồi\nđã đóng', 1450, HT);
    d.flow(s, k1); d.flow(k1, h1); d.flow(h1, n1); d.flow(n1, g1);
    d.flow(g1, n2, 'Không', P(1, 0.5, 0, 0.5));
    d.flow(g1, q1, 'Có', P(0.5, 1, 0, 0.5), [[430, QL]]);
    d.flow(n2, g2);
    d.flow(q1, g2, '', P(1, 0.5, 0.5, 1), [[770, QL]]);
    d.flow(g2, h2, '', P(1, 0.5, 0.5, 1), [[940, NV]]);
    d.flow(h2, k2); d.flow(k2, g3);
    d.flow(g3, h3, 'Có', P(1, 0.5, 0.5, 0), [[1280, KH]]);
    d.flow(g3, n1, 'Không – xử lý bổ sung', P(0.5, 0, 0, 0.5), [[1110, 76], [168, 76], [168, NV]]);
    d.flow(h3, e);
    save(d, [
      ['Khách hàng', 'Gửi phản hồi / khiếu nại qua biểu mẫu trên website.'],
      ['Hệ thống website', 'Tự động ghi nhận, đánh giá cảm xúc và tạo ticket.'],
      ['Nhân viên', 'Tiếp nhận ticket, kiểm tra nội dung, phân loại và gắn chủ đề, xác định mức độ xử lý.'],
      ['Nhân viên', 'Trong thẩm quyền: trực tiếp lập phương án xử lý (xin lỗi, đổi trả, tặng voucher...).'],
      ['Quản lý', 'Vượt thẩm quyền: chuyển cấp, Quản lý phê duyệt phương án giải quyết / đền bù.'],
      ['Hệ thống website', 'Gửi kết quả giải quyết cho khách hàng.'],
      ['Khách hàng', 'Xem phương án và đánh giá kết quả (hài lòng / không hài lòng).'],
      ['Hệ thống website', 'Hài lòng: đóng ticket, kết thúc. Không hài lòng: quay lại bước 3 để xử lý bổ sung.'],
    ]);
  }

  // ===== BPMN-05 Phân nhóm khách hàng =====
  {
    const d = new BPMN('BPMN05_PhanNhomKhachHang', 'BPMN-05: Quy trình phân nhóm khách hàng', 'BPMN-05: Quy trình phân nhóm khách hàng',
      'Coffeeholic – Phân nhóm khách hàng', [['Quản lý', 170], ['Hệ thống website', 230], ['Nhân viên', 150]], 1560);
    const QL = 145, H1 = 290, H2 = 390, NV = 535;
    const s = d.start('Cần phân nhóm\nkhách hàng', 110, QL);
    const q1 = d.task('Thiết lập tiêu chí & quy tắc phân nhóm (RFM)', 260, QL);
    const h1 = d.task('Thu thập & tổng hợp dữ liệu giao dịch', 260, H1);
    const h2 = d.task('Phân tích & tự động gắn nhãn nhóm (VIP, tiềm năng, nguy cơ rời bỏ...)', 430, H1, 160, 70);
    const n1 = d.task('Xem danh sách nhóm & lập kế hoạch chăm sóc (gửi voucher / email)', 600, NV, 170, 64);
    const g1 = d.gw('Cần điều chỉnh\ntiêu chí?', 770, QL);
    const q2 = d.task('Cập nhật lại bộ tiêu chí & quy tắc', 940, QL);
    const h3 = d.task('Lưu & tự động cập nhật phân nhóm định kỳ', 1110, H1);
    const h4 = d.task('Xuất báo cáo hiệu quả phân nhóm', 1280, H1);
    const e = d.end('Kết thúc', 1450, H1);
    d.flow(s, q1); d.flow(q1, h1); d.flow(h1, h2);
    d.flow(h2, n1, '', P(1, 0.5, 0.5, 0), [[600, H1]]);
    d.flow(n1, g1, '', P(1, 0.5, 0.5, 1), [[770, NV]]);
    d.flow(g1, q2, 'Có', P(1, 0.5, 0, 0.5));
    d.flow(q2, h2, '', P(0.5, 1, 0.5, 1), [[940, H2], [430, H2]]);
    d.flow(g1, h3, 'Không', P(0.5, 0, 0.5, 0), [[770, 82], [1110, 82]]);
    d.flow(h3, h4); d.flow(h4, e);
    save(d, [
      ['Quản lý', 'Thiết lập tiêu chí và quy tắc phân nhóm (ví dụ RFM: tần suất mua, tổng chi tiêu, thời gian mua gần nhất).'],
      ['Hệ thống website', 'Thu thập và tổng hợp dữ liệu giao dịch, lịch sử mua hàng của khách.'],
      ['Hệ thống website', 'Phân tích và tự động gắn nhãn nhóm (VIP, tiềm năng, nguy cơ rời bỏ...).'],
      ['Nhân viên', 'Xem danh sách khách theo nhóm, lập kế hoạch chăm sóc (gửi voucher / email).'],
      ['Quản lý', 'Đánh giá tiêu chí. Chưa phù hợp → cập nhật lại bộ tiêu chí và quay lại bước 3.'],
      ['Hệ thống website', 'Tiêu chí phù hợp → lưu và tự động cập nhật phân nhóm định kỳ.'],
      ['Hệ thống website', 'Xuất báo cáo hiệu quả phân nhóm (số lượng, doanh thu theo nhóm). Kết thúc.'],
    ]);
  }

  // ===== BPMN-06 Thiết lập & áp dụng đợt giảm giá =====
  {
    const d = new BPMN('BPMN06_DotGiamGia', 'BPMN-06: Quy trình thiết lập & áp dụng đợt giảm giá', 'BPMN-06: Quy trình thiết lập & áp dụng đợt giảm giá (cho hóa đơn hoặc cà phê cụ thể)',
      'Coffeeholic – Đợt giảm giá', [['Quản lý', 230], ['Hệ thống website', 190], ['Khách hàng', 170]], 2400);
    const Q1 = 130, Q2 = 225, HT = 385, KH = 565, TOP = 75, LOW = 625;
    const s = d.start('Có kế hoạch\ngiảm giá', 110, Q1);
    const q1 = d.task('Nhập tên, mô tả, thời gian bắt đầu & kết thúc', 260, Q1);
    const g1 = d.gw('Giảm giá cho?', 430, Q1);
    const q2 = d.task('Nhập % giảm & giá trị đơn tối thiểu', 600, Q1);
    const q3 = d.task('Chọn cà phê, mức giảm riêng / giá sau giảm, số lượng tối đa', 600, Q2, 170, 64);
    const g2 = d.gw('', 770, Q1);
    const h1 = d.task('Kiểm tra hợp lệ: thời gian, mức giảm, trùng đợt đang chạy', 940, HT, 170, 64);
    const g3 = d.gw('Hợp lệ?', 1110, HT);
    const h2 = d.task('Lưu & kích hoạt đợt giảm giá theo thời gian', 1280, HT);
    const k1 = d.task('Khách đặt hàng trong thời gian áp dụng', 1450, KH);
    const h3 = d.task('Tự áp dụng giảm giá cho hóa đơn / cà phê đủ điều kiện, cập nhật số lượng đã bán', 1630, HT, 180, 64);
    const g4 = d.gw('Hết thời gian /\nhết số lượng?', 1820, HT);
    const h4 = d.task('Kết thúc đợt giảm giá', 1990, HT);
    const q4 = d.task('Xem thống kê hiệu quả đợt giảm giá', 2160, Q1);
    const e = d.end('Kết thúc', 2320, Q1);
    d.flow(s, q1); d.flow(q1, g1);
    d.flow(g1, q2, 'Hóa đơn', P(1, 0.5, 0, 0.5));
    d.flow(g1, q3, 'Cà phê cụ thể', P(0.5, 1, 0, 0.5), [[430, Q2]]);
    d.flow(q2, g2);
    d.flow(q3, g2, '', P(1, 0.5, 0.5, 1), [[770, Q2]]);
    d.flow(g2, h1, '', P(1, 0.5, 0.5, 0), [[940, Q1]]);
    d.flow(h1, g3);
    d.flow(g3, q1, 'Không – nhập lại', P(0.5, 0, 0.5, 0), [[1110, TOP], [260, TOP]]);
    d.flow(g3, h2, 'Có', P(1, 0.5, 0, 0.5));
    d.flow(h2, k1, '', P(1, 0.5, 0.5, 0), [[1450, HT]]);
    d.flow(k1, h3, '', P(1, 0.5, 0.5, 1), [[1630, KH]]);
    d.flow(h3, g4);
    d.flow(g4, k1, 'Không – tiếp tục áp dụng', P(0.5, 1, 0.5, 1), [[1820, LOW], [1450, LOW]]);
    d.flow(g4, h4, 'Có', P(1, 0.5, 0, 0.5));
    d.flow(h4, q4, '', P(1, 0.5, 0.5, 1), [[2160, HT]]);
    d.flow(q4, e);
    save(d, [
      ['Quản lý', 'Nhập tên, mô tả, thời gian bắt đầu và kết thúc của đợt giảm giá.'],
      ['Quản lý', 'Chọn loại giảm giá. Cho hóa đơn: nhập % giảm và giá trị đơn tối thiểu.'],
      ['Quản lý', 'Cho cà phê cụ thể: chọn cà phê, mức giảm riêng / giá sau giảm và số lượng tối đa (chi tiết đợt giảm giá).'],
      ['Hệ thống website', 'Kiểm tra hợp lệ: thời gian, mức giảm, trùng với đợt giảm giá đang chạy. Không hợp lệ → quay lại bước 1.'],
      ['Hệ thống website', 'Lưu và kích hoạt đợt giảm giá theo thời gian đã đặt.'],
      ['Khách hàng', 'Khách đặt hàng trong thời gian áp dụng (xem BPMN-01).'],
      ['Hệ thống website', 'Tự áp dụng giảm giá cho hóa đơn đạt giá trị tối thiểu hoặc cho cà phê thuộc đợt; cập nhật số lượng đã bán.'],
      ['Hệ thống website', 'Chưa hết thời gian và còn số lượng → tiếp tục áp dụng. Ngược lại → kết thúc đợt giảm giá.'],
      ['Quản lý', 'Xem thống kê hiệu quả đợt giảm giá (doanh thu, số đơn, tổng tiền giảm). Kết thúc.'],
    ]);
  }

  // ===== BPMN-07 Phát hành & sử dụng voucher =====
  {
    const d = new BPMN('BPMN07_Voucher', 'BPMN-07: Quy trình phát hành & sử dụng voucher', 'BPMN-07: Quy trình phát hành & sử dụng voucher (cho hóa đơn, khách hàng sở hữu)',
      'Coffeeholic – Voucher', [['Quản lý', 170], ['Hệ thống website', 190], ['Khách hàng', 190]], 1840);
    const QL = 145, HT = 325, KH = 515, LOW = 585;
    const s = d.start('Cần phát hành\nvoucher', 110, QL);
    const q1 = d.task('Tạo voucher: giá trị giảm, điều kiện áp dụng, thời hạn', 270, QL, 170, 64);
    const h1 = d.task('Sinh mã voucher duy nhất', 450, HT);
    const q2 = d.task('Gán voucher cho khách hàng (chọn khách / nhóm khách hàng)', 630, QL, 170, 64);
    const h2 = d.task('Lưu voucher thuộc khách hàng & gửi thông báo', 810, HT);
    const k1 = d.task('Nhận voucher; khi đặt hàng nhập mã voucher', 990, KH);
    const h3 = d.task('Kiểm tra: đúng chủ sở hữu, chưa dùng, còn hạn, đủ điều kiện', 1170, HT, 170, 64);
    const g1 = d.gw('Hợp lệ?', 1350, HT);
    const h4 = d.task('Trừ giá trị voucher vào hóa đơn, đánh dấu đã sử dụng', 1530, HT, 170, 64);
    const e = d.end('Voucher\nđã sử dụng', 1710, HT);
    d.flow(s, q1);
    d.flow(q1, h1, '', P(1, 0.5, 0.5, 0), [[450, QL]]);
    d.flow(h1, q2, '', P(1, 0.5, 0.5, 1), [[630, HT]]);
    d.flow(q2, h2, '', P(1, 0.5, 0.5, 0), [[810, QL]]);
    d.flow(h2, k1, '', P(1, 0.5, 0.5, 0), [[990, HT]]);
    d.flow(k1, h3, '', P(1, 0.5, 0.5, 1), [[1170, KH]]);
    d.flow(h3, g1);
    d.flow(g1, k1, 'Không – từ chối, báo lý do', P(0.5, 1, 0.5, 1), [[1350, LOW], [990, LOW]]);
    d.flow(g1, h4, 'Có', P(1, 0.5, 0, 0.5));
    d.flow(h4, e);
    save(d, [
      ['Quản lý', 'Tạo voucher: giá trị giảm, điều kiện áp dụng (giá trị đơn tối thiểu), thời hạn sử dụng.'],
      ['Hệ thống website', 'Sinh mã voucher duy nhất.'],
      ['Quản lý', 'Gán voucher cho khách hàng (chọn từng khách hoặc theo nhóm khách hàng). Voucher đền bù sự cố được phát ở BPMN-02.'],
      ['Hệ thống website', 'Lưu voucher thuộc khách hàng và gửi thông báo.'],
      ['Khách hàng', 'Nhận voucher; khi đặt hàng nhập mã voucher (hoặc đưa mã cho nhân viên tại quầy).'],
      ['Hệ thống website', 'Kiểm tra: đúng khách hàng sở hữu, chưa sử dụng, còn hạn, hóa đơn đủ điều kiện. Không hợp lệ → từ chối, báo lý do, khách đặt hàng không dùng voucher hoặc nhập mã khác.'],
      ['Hệ thống website', 'Hợp lệ → trừ giá trị voucher vào hóa đơn, đánh dấu voucher đã sử dụng. Kết thúc.'],
    ]);
  }

  return R;
};
