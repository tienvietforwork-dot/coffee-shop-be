// Kịch bản test 36 usecase app-core (Hồ Tiến Việt) trên API thật. Ghi lại từng bước vào uc-results.json.
import fs from 'node:fs'

const BASE = 'http://localhost:8081/api'
const OUT = process.argv[2] ?? 'uc-results.json'
const TODAY = new Date(Date.now() + 7 * 3600e3).toISOString().slice(0, 10)
const plus = (d) => new Date(Date.now() + 7 * 3600e3 + d * 864e5).toISOString().slice(0, 10)
const results = []
let cur
const T = {}

function uc(no, name) { cur = { no, name, steps: [] }; results.push(cur) }
const short = (v, n = 220) => { const s = typeof v === 'string' ? v : JSON.stringify(v); return s && s.length > n ? s.slice(0, n) + '…' : s }

/** one API call = one test step. expect(res) returns a string describing the result, or throws. */
async function step(title, method, path, body, { token = T.admin, expect, expectStatus, form, binary } = {}) {
  const rec = { title, method, path, input: body === undefined ? undefined : short(body, 400) }
  try {
    const headers = {}
    if (token) headers.Authorization = 'Bearer ' + token
    let payload
    if (form) { payload = form } else if (body !== undefined) { headers['Content-Type'] = 'application/json'; payload = JSON.stringify(body) }
    const r = await fetch(BASE + path, { method, headers, body: payload, redirect: 'manual' })
    rec.status = r.status
    let data = null
    const ct = r.headers.get('content-type') ?? ''
    if (binary || ct.includes('spreadsheet') || ct.includes('octet')) { const b = Buffer.from(await r.arrayBuffer()); data = { bytes: b.length, contentType: ct, xlsx: b.slice(0, 2).toString() === 'PK' } }
    else { const t = await r.text(); try { data = t ? JSON.parse(t) : null } catch { data = t } }
    if (expectStatus ? r.status !== expectStatus : r.status >= 400) throw new Error(`HTTP ${r.status}: ${short(data, 300)}`)
    rec.actual = expect ? expect(data, r) : short(data)
    rec.pass = true
    cur.steps.push(rec)
    return data
  } catch (e) {
    rec.pass = false
    rec.actual = String(e.message ?? e)
    cur.steps.push(rec)
    return null
  }
}
function check(cond, msg) { if (!cond) throw new Error('Sai kỳ vọng: ' + msg); return msg }
const money = (n) => Number(n).toLocaleString('vi-VN') + 'đ'

// ---------------------------------------------------------------------------------------------
uc(23, 'Đăng ký / đăng nhập & hồ sơ cá nhân')
const login = await step('Quản lý đăng nhập admin / admin123', 'POST', '/auth/login', { username: 'admin', password: 'admin123' },
  { token: null, expect: (d) => check(d.token && d.user.roles.includes('ADMIN'), `nhận JWT, vai trò ${d.user.roles}, ${d.permissions.length} quyền`) })
T.admin = login?.token
await step('Đăng nhập sai mật khẩu bị từ chối', 'POST', '/auth/login', { username: 'admin', password: 'sai-mat-khau' },
  { token: null, expectStatus: 401, expect: () => '401 – không cấp token' })
const reg = await step('Khách đăng ký bằng SĐT 0938 456 712', 'POST', '/auth/register',
  { phone: '0938456712', password: 'Khach@2026', fullName: 'Nguyễn Thị Thu Hà', email: 'thuha.nguyen@gmail.com' },
  { token: null, expect: (d) => check(d.token && d.user.roles.includes('CUSTOMER'), `tạo tài khoản khách ${d.user.username}, nhận JWT`) })
T.customer = reg?.token
await step('Đăng ký trùng SĐT bị từ chối', 'POST', '/auth/register', { phone: '0938456712', password: 'Khach@2026' },
  { token: null, expectStatus: 400, expect: (d) => '400: ' + d?.error })
await step('Khách xem hồ sơ', 'GET', '/account', undefined, { token: T.customer, expect: (d) => `${d.fullName} · ${d.phone} · điểm ${d.points ?? 0}` })
await step('Khách cập nhật email', 'PUT', '/account', { fullName: 'Nguyễn Thị Thu Hà', email: 'thuha.ntt@outlook.com' },
  { token: T.customer, expect: (d) => check(d.email === 'thuha.ntt@outlook.com', 'email mới ' + d.email) })
await step('Khách đổi mật khẩu', 'PUT', '/auth/me/password', { currentPassword: 'Khach@2026', newPassword: 'ThuHa#0938' }, { token: T.customer, expect: () => 'đổi thành công' })
const relog = await step('Đăng nhập lại bằng mật khẩu mới', 'POST', '/auth/login', { username: '0938456712', password: 'ThuHa#0938' },
  { token: null, expect: (d) => check(!!d.token, 'đăng nhập được bằng mật khẩu mới') })
T.customer = relog?.token ?? T.customer

// ---------------------------------------------------------------------------------------------
uc(22, 'Phân quyền theo vai trò')
const roles = await step('Xem 3 vai trò cố định', 'GET', '/roles', undefined, { expect: (d) => d.map((r) => `${r.code}(${r.permissionCodes.length})`).join(', ') })
const perms = await step('Xem danh sách quyền màn hình', 'GET', '/permissions', undefined, { expect: (d) => `${d.length} quyền, ví dụ ${d.slice(0, 4).map((p) => p.code).join(', ')}` })
const staffRole = roles?.find((r) => r.code === 'STAFF')
const staffCodes = staffRole?.permissionCodes ?? []
await step('Cấp thêm quyền "suppliers" cho Nhân viên', 'PUT', `/roles/${staffRole?.id}/permissions`, { permissionCodes: [...staffCodes, 'suppliers'] },
  { expect: (d) => check(d.permissionCodes.includes('suppliers'), 'STAFF có quyền suppliers') })
await step('Thu hồi lại quyền "suppliers"', 'PUT', `/roles/${staffRole?.id}/permissions`, { permissionCodes: staffCodes },
  { expect: (d) => check(!d.permissionCodes.includes('suppliers'), `STAFF còn ${d.permissionCodes.length} quyền như cũ`) })

// ---------------------------------------------------------------------------------------------
uc(20, 'Quản lý nhân viên')
const st1 = await step('Thêm nhân viên pha chế', 'POST', '/staff',
  { fullName: 'Trần Minh Khoa', position: 'Pha chế', phone: '0907112233', email: 'khoa.tm@coffeeholic.vn', hireDate: '2026-10-01', workStatus: 'ACTIVE' },
  { expect: (d) => `id ${d.id} · ${d.fullName} · ${d.position}` })
const st2 = await step('Thêm nhân viên thu ngân', 'POST', '/staff',
  { fullName: 'Lê Hoàng Yến', position: 'Thu ngân', phone: '0912556677', hireDate: '2026-09-15', workStatus: 'ACTIVE' }, { expect: (d) => `id ${d.id} · ${d.fullName}` })
await step('Thêm trùng SĐT bị chặn', 'POST', '/staff', { fullName: 'Trùng', phone: '0907112233' }, { expectStatus: 400, expect: (d) => '400: ' + d?.error })
await step('Sửa: Khoa chuyển sang tạm nghỉ rồi đi làm lại', 'PUT', `/staff/${st1?.id}`,
  { fullName: 'Trần Minh Khoa', position: 'Pha chế chính', phone: '0907112233', email: 'khoa.tm@coffeeholic.vn', hireDate: '2026-10-01', workStatus: 'ACTIVE' },
  { expect: (d) => check(d.position === 'Pha chế chính', 'chức vụ ' + d.position) })
const st3 = await step('Thêm nhân viên thời vụ để xóa', 'POST', '/staff', { fullName: 'Phạm Văn Tèo', position: 'Phục vụ', phone: '0933001122' }, { expect: (d) => 'id ' + d.id })
await step('Xóa nhân viên thời vụ (chuyển trạng thái Nghỉ việc, giữ lịch sử)', 'DELETE', `/staff/${st3?.id}`, undefined, { expect: () => 'xóa thành công' })
await step('Danh sách + tìm "Khoa" (lọc ở FE)', 'GET', '/staff', undefined,
  { expect: (d) => check(d.some((s) => s.fullName.includes('Khoa')) && d.find((s) => s.id === st3?.id)?.workStatus === 'RESIGNED', `${d.length} NV, tìm thấy Khoa, Tèo ở trạng thái RESIGNED`) })

// ---------------------------------------------------------------------------------------------
uc(21, 'Quản lý tài khoản người dùng')
const u1 = await step('Tạo tài khoản cho thu ngân Yến (gắn hồ sơ NV)', 'POST', '/users',
  { username: 'yen.lh', password: 'Yen@2026', fullName: 'Lê Hoàng Yến', phone: '0912556677', active: true, roles: ['STAFF'], staffId: st2?.id },
  { expect: (d) => `id ${d.id} · ${d.username} · ${d.roles}` })
const yen = await step('Thu ngân Yến đăng nhập', 'POST', '/auth/login', { username: 'yen.lh', password: 'Yen@2026' }, { token: null, expect: (d) => `${d.permissions.length} quyền nhân viên` })
T.staff = yen?.token
await step('Nhân viên không vào được màn Tài khoản (403)', 'GET', '/users', undefined, { token: T.staff, expectStatus: 403, expect: () => '403 – đúng phân quyền' })
const u2 = await step('Tạo tài khoản pha chế Khoa', 'POST', '/users',
  { username: 'khoa.tm', password: 'Khoa@2026', fullName: 'Trần Minh Khoa', active: true, roles: ['STAFF'], staffId: st1?.id }, { expect: (d) => 'id ' + d.id })
await step('Khóa tài khoản Khoa', 'PUT', `/users/${u2?.id}`, { username: 'khoa.tm', fullName: 'Trần Minh Khoa', active: false, roles: ['STAFF'], staffId: st1?.id },
  { expect: (d) => check(d.active === false, 'active=false') })
await step('Tài khoản bị khóa không đăng nhập được', 'POST', '/auth/login', { username: 'khoa.tm', password: 'Khoa@2026' }, { token: null, expectStatus: 400, expect: (d) => '400: ' + d?.error })
await step('Mở lại tài khoản Khoa', 'PUT', `/users/${u2?.id}`, { username: 'khoa.tm', fullName: 'Trần Minh Khoa', active: true, roles: ['STAFF'], staffId: st1?.id },
  { expect: (d) => check(d.active === true, 'active=true') })
const u3 = await step('Tạo tài khoản thử để xóa', 'POST', '/users', { username: 'tam.thoi', password: 'Tam@2026', active: true, roles: ['STAFF'] }, { expect: (d) => 'id ' + d.id })
await step('Xóa tài khoản thử', 'DELETE', `/users/${u3?.id}`, undefined, { expect: () => 'xóa thành công' })
await step('Danh sách tài khoản + tìm "yen"', 'GET', '/users', undefined, { expect: (d) => check(d.some((u) => u.username === 'yen.lh') && !d.some((u) => u.id === u3?.id), `${d.length} tài khoản`) })

// ---------------------------------------------------------------------------------------------
uc(1, 'Quản lý danh mục cà phê')
const cat = await step('Thêm danh mục "Cà phê muối"', 'POST', '/categories', { name: 'Cà phê muối', description: 'Cà phê phin với lớp kem muối kiểu Huế', displayOrder: 5 },
  { expect: (d) => `id ${d.id} · ${d.name}` })
await step('Thêm trùng tên bị chặn', 'POST', '/categories', { name: 'Cà phê muối' }, { expectStatus: 400, expect: (d) => '400: ' + d?.error })
await step('Sửa mô tả, thứ tự', 'PUT', `/categories/${cat?.id}`, { name: 'Cà phê muối', description: 'Phin Robusta + kem muối béo mặn', displayOrder: 3 },
  { expect: (d) => check(d.displayOrder === 3, 'thứ tự 3') })
const cat2 = await step('Thêm danh mục thử', 'POST', '/categories', { name: 'Danh mục thử xóa' }, { expect: (d) => 'id ' + d.id })
await step('Xóa danh mục trống', 'DELETE', `/categories/${cat2?.id}`, undefined, { expect: () => 'xóa thành công' })
await step('Danh sách + tìm "muối"', 'GET', '/categories', undefined, { expect: (d) => check(d.some((c) => c.name === 'Cà phê muối'), `${d.length} danh mục, có "Cà phê muối"`) })

// ---------------------------------------------------------------------------------------------
uc(13, 'Quản lý nguyên liệu & quy cách đóng gói')
let mats = await step('Danh sách nguyên liệu', 'GET', '/materials', undefined, { expect: (d) => `${d.length} nguyên liệu` }) ?? []
const M = (name) => mats.find((m) => m.name === name)
const oat = await step('Thêm "Sữa yến mạch Oatside" + quy cách Hộp 1 L, Thùng 6 hộp', 'POST', '/materials',
  { name: 'Sữa yến mạch Oatside', unit: 'ml', minStock: 2000, expiryWarningDays: 5, kind: 'RAW', packages: [{ name: 'Hộp 1 L', size: 1000 }, { name: 'Thùng 6 hộp', size: 6000 }] },
  { expect: (d) => `id ${d.id} · ${d.packages?.map((p) => p.name + '=' + p.size).join(', ')}` })
const robusta = M('Cà phê Robusta rang xay'), cream = M('Whipping cream'), milk = M('Sữa đặc'), sugar = M('Đường'), ice = M('Đá viên'), salt = M('Muối'), freshMilk = M('Sữa tươi')
await step('Thêm quy cách "Bao 5 kg" cho Robusta', 'PUT', `/materials/${robusta?.id}`,
  { name: robusta?.name, unit: robusta?.unit, minStock: robusta?.minStock, expiryWarningDays: robusta?.expiryWarningDays, kind: 'RAW', packages: [{ name: 'Bao 5 kg', size: 5000 }, { name: 'Gói 1 kg', size: 1000 }] },
  { expect: (d) => d.packages?.map((p) => p.name).join(', ') })
await step('Thêm quy cách "Hộp 1 L" cho Whipping cream', 'PUT', `/materials/${cream?.id}`,
  { name: cream?.name, unit: cream?.unit, minStock: cream?.minStock, expiryWarningDays: cream?.expiryWarningDays, kind: 'RAW', packages: [{ name: 'Hộp 1 L', size: 1000 }] },
  { expect: (d) => d.packages?.map((p) => p.name).join(', ') })
await step('Sửa tồn tối thiểu Sữa yến mạch = 3000 ml', 'PUT', `/materials/${oat?.id}`,
  { name: 'Sữa yến mạch Oatside', unit: 'ml', minStock: 3000, expiryWarningDays: 5, kind: 'RAW', packages: [{ name: 'Hộp 1 L', size: 1000 }, { name: 'Thùng 6 hộp', size: 6000 }] },
  { expect: (d) => check(Number(d.minStock) === 3000, 'minStock 3000') })
const tmpMat = await step('Thêm nguyên liệu thử để xóa', 'POST', '/materials', { name: 'Bột cacao thử', unit: 'g', kind: 'RAW' }, { expect: (d) => 'id ' + d.id })
await step('Xóa nguyên liệu chưa dùng', 'DELETE', `/materials/${tmpMat?.id}`, undefined, { expect: () => 'xóa thành công' })
await step('Xóa nguyên liệu đang có lô = chuyển "Ngừng dùng" (giữ lô, lịch sử)', 'DELETE', `/materials/${salt?.id}`, undefined, { expect: () => '204' })
await step('Nguyên liệu ngừng dùng vẫn còn trong danh sách với trạng thái INACTIVE', 'GET', '/materials', undefined, { expect: (d) => check(d.find((m) => m.id === salt?.id)?.status === 'INACTIVE', 'Muối: INACTIVE') })
await step('Dùng lại nguyên liệu', 'PUT', `/materials/${salt?.id}`, { name: salt?.name, unit: salt?.unit, minStock: salt?.minStock, kind: 'RAW', status: 'ACTIVE' }, { expect: (d) => d.status })
await step('Xem các lô của Robusta', 'GET', `/materials/${robusta?.id}/batches`, undefined, { expect: (d) => d.map((b) => `lô #${b.id} còn ${b.remainingQuantity}g HSD ${b.expiryDate}`).join('; ') })
mats = await step('Danh sách sau khi sửa + tìm "yến mạch"', 'GET', '/materials', undefined, { expect: (d) => check(d.some((m) => m.name.includes('yến mạch')), `${d.length} nguyên liệu`) }) ?? mats

// ---------------------------------------------------------------------------------------------
uc(15, 'Quản lý nhà cung cấp')
const supCoffee = await step('Thêm NCC cà phê (công nợ 15 ngày)', 'POST', '/suppliers',
  { name: 'Công ty TNHH Cà phê Đất Bazan', contactName: 'Anh Nguyễn Văn Phúc', phone: '0905123789', email: 'sales@datbazan.vn', address: '123 Lê Duẩn, P. Tân Lợi, TP. Buôn Ma Thuột, Đắk Lắk', taxCode: '6001234567', paymentTermDays: 15,
    materials: [{ materialId: robusta?.id, unitPrice: 230 }] },
  { expect: (d) => `id ${d.id} · ${d.name} · nợ ${d.paymentTermDays} ngày · ${d.materials?.length} NL` })
const supDairy = await step('Thêm NCC sữa & kem (công nợ 7 ngày)', 'POST', '/suppliers',
  { name: 'NPP Sữa Hoàng Gia', contactName: 'Chị Trần Thị Mai', phone: '0283 8456 123', address: '45 Cộng Hòa, P.4, Q. Tân Bình, TP.HCM', taxCode: '0312987654', paymentTermDays: 7,
    materials: [{ materialId: cream?.id, unitPrice: 145 }, { materialId: milk?.id, unitPrice: 59 }, { materialId: oat?.id, unitPrice: 62 }] },
  { expect: (d) => `id ${d.id} · ${d.materials?.length} NL` })
const supTmp = await step('Thêm NCC thử', 'POST', '/suppliers', { name: 'NCC thử xóa', phone: '0900000000' }, { expect: (d) => 'id ' + d.id })
await step('Sửa SĐT, ghi chú NCC sữa', 'PUT', `/suppliers/${supDairy?.id}`,
  { name: 'NPP Sữa Hoàng Gia', contactName: 'Chị Trần Thị Mai', phone: '0908456123', address: '45 Cộng Hòa, P.4, Q. Tân Bình, TP.HCM', taxCode: '0312987654', paymentTermDays: 7, note: 'Giao 7h–9h sáng',
    materials: [{ materialId: cream?.id, unitPrice: 145 }, { materialId: milk?.id, unitPrice: 59 }, { materialId: oat?.id, unitPrice: 62 }] },
  { expect: (d) => check(d.phone === '0908456123', 'SĐT mới ' + d.phone) })
await step('Xóa NCC chưa có giao dịch', 'DELETE', `/suppliers/${supTmp?.id}`, undefined, { expect: () => 'xóa thành công' })
await step('Danh sách + tìm "Bazan"', 'GET', '/suppliers', undefined, { expect: (d) => check(d.some((s) => s.name.includes('Bazan')), `${d.length} NCC`) })

// ---------------------------------------------------------------------------------------------
uc(16, 'Quản lý phiếu đặt hàng nhà cung cấp')
const pkg = (m, name) => m?.packages?.find((p) => p.name === name)?.id
mats = (await (await fetch(BASE + '/materials', { headers: { Authorization: 'Bearer ' + T.admin } })).json())
const robusta2 = M('Cà phê Robusta rang xay'), cream2 = M('Whipping cream'), oat2 = M('Sữa yến mạch Oatside')
const po1 = await step('Lập phiếu đặt Robusta 2 bao 5 kg × 230đ/g, VAT 8%, hẹn giao 2 ngày', 'POST', '/purchase-orders',
  { supplierId: supCoffee?.id, expectedDate: plus(2), vatRate: 8, note: 'Rang vừa, xay pha phin', items: [{ materialId: robusta2?.id, packageId: pkg(robusta2, 'Bao 5 kg'), packageCount: 2, unitPrice: 230 }] },
  { expect: (d) => check(Number(d.totalAmount) === 2484000, `${d.code} · ${d.status} · ${money(d.totalAmount)} (10.000g × 230 = 2.300.000 + VAT 184.000)`) })
const po2 = await step('Lập phiếu đặt sữa: 12 hộp whipping + 6 hộp yến mạch, VAT 8%', 'POST', '/purchase-orders',
  { supplierId: supDairy?.id, expectedDate: plus(1), vatRate: 8, items: [{ materialId: cream2?.id, packageId: pkg(cream2, 'Hộp 1 L'), packageCount: 12, unitPrice: 145 }, { materialId: oat2?.id, packageId: pkg(oat2, 'Hộp 1 L'), packageCount: 6, unitPrice: 62 }] },
  { expect: (d) => check(Number(d.totalAmount) === 2280960, `${d.code} · ${money(d.totalAmount)} (1.740.000 + 372.000 = 2.112.000 + VAT 168.960)`) })
await step('Sửa phiếu nháp: thêm chiết khấu 50.000đ', 'PUT', `/purchase-orders/${po2?.id}`,
  { supplierId: supDairy?.id, expectedDate: plus(1), vatRate: 8, discountAmount: 50000, items: [{ materialId: cream2?.id, packageId: pkg(cream2, 'Hộp 1 L'), packageCount: 12, unitPrice: 145 }, { materialId: oat2?.id, packageId: pkg(oat2, 'Hộp 1 L'), packageCount: 6, unitPrice: 62 }] },
  { expect: (d) => `tổng mới ${money(d.totalAmount)}` })
await step('Gửi NCC phiếu cà phê', 'POST', `/purchase-orders/${po1?.id}/send`, undefined, { expect: (d) => check(d.status === 'ORDERED', d.status) })
await step('Gửi NCC phiếu sữa', 'POST', `/purchase-orders/${po2?.id}/send`, undefined, { expect: (d) => check(d.status === 'ORDERED', d.status) })
await step('Không sửa được phiếu đã gửi', 'PUT', `/purchase-orders/${po1?.id}`, { supplierId: supCoffee?.id, items: [{ materialId: robusta2?.id, quantity: 1000 }] }, { expectStatus: 400, expect: (d) => '400: ' + d?.error })
const po3 = await step('Lập phiếu nháp thử', 'POST', '/purchase-orders', { supplierId: supDairy?.id, items: [{ materialId: oat2?.id, quantity: 2000, unitPrice: 62 }] }, { expect: (d) => d.code })
await step('Xóa phiếu nháp', 'DELETE', `/purchase-orders/${po3?.id}`, undefined, { expect: () => 'xóa thành công' })
const po4 = await step('Lập + gửi phiếu sẽ hủy', 'POST', '/purchase-orders', { supplierId: supDairy?.id, items: [{ materialId: oat2?.id, quantity: 6000, unitPrice: 62 }] }, { expect: (d) => d.code })
await step('Gửi phiếu', 'POST', `/purchase-orders/${po4?.id}/send`, undefined, { expect: (d) => d.status })
await step('Hủy phiếu đã gửi (chưa nhận gì)', 'POST', `/purchase-orders/${po4?.id}/cancel`, { reason: 'NCC báo hết hàng yến mạch' }, { expect: (d) => check(d.status === 'CANCELLED', d.status) })
await step('Lọc phiếu theo trạng thái "Đã đặt"', 'GET', '/purchase-orders?status=ORDERED', undefined, { expect: (d) => check(d.length >= 2 && d.every((p) => p.status === 'ORDERED'), `${d.length} phiếu ORDERED`) })
await step('Lọc phiếu theo NCC Bazan', 'GET', `/purchase-orders?supplierId=${supCoffee?.id}`, undefined, { expect: (d) => `${d.length} phiếu của Bazan` })

// ---------------------------------------------------------------------------------------------
uc(17, 'Nhập kho (theo phiếu đặt / nhập lẻ)')
const po1d = await step('Mở phiếu đặt cà phê để nhận hàng', 'GET', `/purchase-orders/${po1?.id}`, undefined, { expect: (d) => d.items.map((i) => `${i.materialName}: ${i.packageCount} ${i.packageName}`).join('; ') })
const gr1 = await step('Nhận đủ 2 bao Robusta, HĐ 1C26TDB/0000456, HSD 6 tháng', 'POST', `/purchase-orders/${po1?.id}/receipts`,
  { delivererName: 'Anh Lâm (xe tải NCC)', invoiceSeries: '1C26TDB', invoiceNo: '0000456', invoiceDate: TODAY, vatRate: 8,
    lines: [{ itemId: po1d?.items[0].id, documentCount: 2, receivedCount: 2, expiryDate: plus(180) }] },
  { expect: (d) => check(Number(d.totalAmount) === 2484000, `${d.code} · ${money(d.totalAmount)} · hạn trả ${d.dueDate}`) })
const po2d = await step('Mở phiếu đặt sữa', 'GET', `/purchase-orders/${po2?.id}`, undefined, { expect: (d) => d.items.map((i) => `${i.materialName}: ${i.packageCount} ${i.packageName}`).join('; ') })
const gr2 = await step('Nhận hàng sữa: whipping chứng từ 12, nhận 11, trả 1 hộp móp; yến mạch nhận 4/6', 'POST', `/purchase-orders/${po2?.id}/receipts`,
  { delivererName: 'Chị Mai', invoiceSeries: '1C26THG', invoiceNo: '0012890', invoiceDate: TODAY, vatRate: 8, discountAmount: 50000,
    lines: [{ itemId: po2d?.items[0].id, documentCount: 12, receivedCount: 11, rejectedCount: 1, rejectReason: 'Hộp móp, rỉ', expiryDate: plus(60), temperature: 5 },
      { itemId: po2d?.items[1].id, documentCount: 4, receivedCount: 4, expiryDate: plus(90) }] },
  { expect: (d) => `${d.code} · tiền hàng ${money(d.goodsAmount)} · tổng ${money(d.totalAmount)}` })
await step('Phiếu đặt sữa chuyển "Nhận một phần"', 'GET', `/purchase-orders/${po2?.id}`, undefined, { expect: (d) => check(d.status === 'PARTIALLY_RECEIVED', d.status) })
const sup = await step('Nhập lẻ không có phiếu đặt: 5 kg đường Biên Hòa, trả tiền mặt ngay', 'POST', '/goods-receipts',
  { supplierId: supDairy?.id, delivererName: 'Mua tại chợ Tân Định', paidAmount: 125000, paymentMethod: 'CASH',
    lines: [{ materialId: sugar?.id, receivedCount: 5000, unitPrice: 25, expiryDate: plus(365) }] },
  { expect: (d) => check(Number(d.remainingAmount) === 0, `${d.code} · ${money(d.totalAmount)} · còn nợ ${money(d.remainingAmount)}`) })
await step('Nhập lẻ bắt buộc chọn NCC', 'POST', '/goods-receipts', { lines: [{ materialId: sugar?.id, receivedCount: 1000, unitPrice: 25 }] }, { expectStatus: 400, expect: (d) => '400: ' + d?.error })
const grX = await step('Nhập lẻ nhầm 1 kg muối', 'POST', '/goods-receipts', { supplierId: supDairy?.id, lines: [{ materialId: salt?.id, receivedCount: 1000, unitPrice: 10 }] }, { expect: (d) => d.code })
await step('Hủy phiếu nhập nhầm (lô chưa dùng → trả lại kho, công nợ)', 'POST', `/goods-receipts/${grX?.id}/cancel`, { reason: 'Nhập nhầm số lượng' }, { expect: (d) => check(d.status === 'CANCELLED', d.status) })
await step('Tìm phiếu nhập theo NCC + khoảng ngày', 'GET', `/goods-receipts?supplierId=${supDairy?.id}&from=${TODAY}&to=${TODAY}`, undefined, { expect: (d) => `${d.length} phiếu: ${d.map((g) => g.code + '/' + g.status).join(', ')}` })
await step('Tồn Robusta tăng 10.000 g', 'GET', `/materials/${robusta?.id}/batches`, undefined, { expect: (d) => d.map((b) => `${b.remainingQuantity}g @${b.unitCost}`).join(' + ') })

// ---------------------------------------------------------------------------------------------
uc(29, 'Phiếu trả hàng nhà cung cấp')
const gr2d = await step('Mở phiếu nhập sữa', 'GET', `/goods-receipts/${gr2?.id}`, undefined, { expect: (d) => d.lines.map((l) => `${l.materialName} nhận ${l.receivedQuantity}`).join('; ') })
const ret1 = await step('Lập phiếu trả tạm: 1 hộp whipping bị tách nước (trừ công nợ)', 'POST', '/supplier-returns',
  { goodsReceiptId: gr2?.id, reason: 'Kem tách nước khi mở hộp', settlement: 'DEBT', items: [{ receiptItemId: gr2d?.lines[0].id, count: 1 }] },
  { expect: (d) => `${d.code} · ${d.status} · ${money(d.totalAmount)}` })
await step('Sửa phiếu tạm: thêm số HĐ điều chỉnh giảm', 'PUT', `/supplier-returns/${ret1?.id}`,
  { goodsReceiptId: gr2?.id, reason: 'Kem tách nước khi mở hộp', settlement: 'DEBT', adjustmentInvoiceNo: '0012955', items: [{ receiptItemId: gr2d?.lines[0].id, count: 1 }] },
  { expect: (d) => check(d.adjustmentInvoiceNo === '0012955', 'HĐ điều chỉnh ' + d.adjustmentInvoiceNo) })
await step('Hoàn tất trả hàng (trừ kho 1.000 ml)', 'POST', `/supplier-returns/${ret1?.id}/complete`, undefined, { expect: (d) => check(d.status === 'COMPLETED', `${d.status} · ${money(d.totalAmount)}`) })
const ret2 = await step('Lập phiếu trả tạm khác', 'POST', '/supplier-returns', { goodsReceiptId: gr2?.id, reason: 'Thử', settlement: 'REFUND', refundMethod: 'CASH', items: [{ receiptItemId: gr2d?.lines[1].id, count: 1 }] }, { expect: (d) => d.code })
await step('Hủy phiếu trả tạm', 'POST', `/supplier-returns/${ret2?.id}/cancel`, { reason: 'Kiểm lại thấy hàng tốt' }, { expect: (d) => check(d.status === 'CANCELLED', d.status) })
await step('Không trả quá số đã nhận', 'POST', '/supplier-returns', { goodsReceiptId: gr2?.id, reason: 'Quá', settlement: 'DEBT', items: [{ receiptItemId: gr2d?.lines[0].id, count: 50 }] }, { expectStatus: 400, expect: (d) => '400: ' + d?.error })
await step('Lọc phiếu trả theo NCC', 'GET', `/supplier-returns?supplierId=${supDairy?.id}`, undefined, { expect: (d) => d.map((r) => r.code + '/' + r.status).join(', ') })

// ---------------------------------------------------------------------------------------------
uc(30, 'Công nợ & thanh toán nhà cung cấp')
await step('Xem công nợ các NCC', 'GET', '/supplier-debts', undefined, { expect: (d) => d.filter((x) => x.balance > 0).map((x) => `${x.supplierName}: ${money(x.balance)} (hạn ${x.nextDueDate})`).join('; ') })
const pay1 = await step('Chi tiền mặt trả Bazan 1.000.000đ cho phiếu nhập', 'POST', '/supplier-payments',
  { supplierId: supCoffee?.id, amount: 1000000, method: 'CASH', payeeName: 'Anh Lâm', goodsReceiptId: gr1?.id, note: 'Trả đợt 1' }, { expect: (d) => `${d.code} · ${money(d.amount)}` })
const pay2 = await step('Chuyển khoản trả Bazan phần còn lại', 'POST', '/supplier-payments',
  { supplierId: supCoffee?.id, amount: 1484000, method: 'BANK_TRANSFER', reference: 'FT26294718263', goodsReceiptId: gr1?.id }, { expect: (d) => `${d.code} · ${money(d.amount)}` })
await step('Không chi vượt công nợ', 'POST', '/supplier-payments', { supplierId: supCoffee?.id, amount: 99000000, method: 'CASH', goodsReceiptId: gr1?.id }, { expectStatus: 400, expect: (d) => '400: ' + d?.error })
await step('Hủy phiếu chi lập nhầm (chuyển khoản)', 'POST', `/supplier-payments/${pay2?.id}/cancel`, { reason: 'Sai số tham chiếu' }, { expect: (d) => check(d.status === 'CANCELLED', d.status) })
await step('Sổ chi tiết công nợ Bazan', 'GET', `/supplier-debts/${supCoffee?.id}/ledger`, undefined,
  { expect: (d) => check(Number(d.closingBalance) === 1484000, `đầu kỳ ${money(d.openingBalance)} + tăng ${money(d.increase)} − giảm ${money(d.decrease)} = cuối kỳ ${money(d.closingBalance)}`) })
await step('Xuất Excel sổ chi tiết', 'GET', `/supplier-debts/${supCoffee?.id}/ledger.xlsx`, undefined, { binary: true, expect: (d) => check(d.xlsx, `file xlsx ${d.bytes} byte`) })
await step('Tìm phiếu chi theo NCC + ngày', 'GET', `/supplier-payments?supplierId=${supCoffee?.id}&from=${TODAY}&to=${TODAY}`, undefined, { expect: (d) => d.map((p) => `${p.code}/${p.status}`).join(', ') })

// ---------------------------------------------------------------------------------------------
uc(18, 'Hủy nguyên liệu / sơ chế nguyên liệu')
const creamLots = await step('Lô whipping vừa nhập', 'GET', `/materials/${cream?.id}/batches`, undefined, { expect: (d) => d.map((b) => `#${b.id}: ${b.remainingQuantity}`).join(', ') })
const lot = creamLots?.find((b) => b.remainingQuantity > 500)
await step('Hủy 250 ml whipping bị đổ khi đánh kem', 'POST', '/inventory/dispose', { batchId: lot?.id, quantity: 250, reason: 'SPILLED', note: 'Đổ khi đánh kem muối' }, { expect: (d) => `${d.type} ${d.quantity} ml · ${d.reason}` })
await step('Xuất dùng nội bộ 100 ml (thử món mới)', 'POST', '/inventory/export', { batchId: lot?.id, quantity: 100, reason: 'INTERNAL_USE', note: 'R&D món kem muối dừa' }, { expect: (d) => `${d.type} ${d.quantity} ml` })
await step('Không hủy quá tồn của lô', 'POST', '/inventory/dispose', { batchId: lot?.id, quantity: 999999, reason: 'SPOILED' }, { expectStatus: 400, expect: (d) => '400: ' + d?.error })
const kemMuoi = M('Kem muối')
await step('Sơ chế 1 mẻ "Kem muối" (trừ whipping, sữa, muối theo định mức)', 'POST', '/inventory/produce', { materialId: kemMuoi?.id, batches: 1, note: 'Mẻ sáng' },
  { expect: (d) => short(d, 200) })
await step('Lịch sử kho: lọc giao dịch whipping', 'GET', `/inventory/transactions?materialId=${cream?.id}`, undefined, { expect: (d) => d.slice(0, 5).map((t) => `${t.type}${t.reason ? '/' + t.reason : ''} ${t.quantity}`).join('; ') })

// ---------------------------------------------------------------------------------------------
uc(19, 'Kiểm kê & điều chỉnh tồn kho')
const stk = await step('Lập phiếu kiểm kê cuối ngày: Robusta, Đường, Đá viên', 'POST', '/stocktakes', { members: 'Lê Hoàng Yến, Trần Minh Khoa', materialIds: [robusta?.id, sugar?.id, ice?.id] },
  { expect: (d) => `${d.code} · ${d.items.length} lô · sổ: ${d.items.map((i) => i.bookQuantity).join('/')}` })
const counts = (stk?.items ?? []).map((i, k) => ({ itemId: i.id, countedQuantity: Number(i.bookQuantity) - (k === 0 ? 150 : 0), quality: 'GOOD', reason: k === 0 ? 'Hao hụt khi xay, rơi vãi' : undefined }))
await step('Nhập số đếm thực tế (lô đầu thiếu 150)', 'PUT', `/stocktakes/${stk?.id}`, { members: 'Lê Hoàng Yến, Trần Minh Khoa', counts }, { expect: (d) => d.items.map((i) => `${i.bookQuantity}→${i.countedQuantity}`).join(', ') })
await step('Quản lý duyệt cân bằng kho', 'POST', `/stocktakes/${stk?.id}/balance`, { resolution: 'Ghi nhận hao hụt định mức' }, { expect: (d) => check(d.status === 'BALANCED', `${d.status}`) })
const stk2 = await step('Lập phiếu kiểm kê thử', 'POST', '/stocktakes', { materialIds: [salt?.id] }, { expect: (d) => d.code })
await step('Hủy phiếu kiểm kê', 'POST', `/stocktakes/${stk2?.id}/cancel`, undefined, { expect: (d) => check(d.status === 'CANCELLED', d.status) })
await step('Lọc phiếu đã cân bằng', 'GET', '/stocktakes?status=BALANCED', undefined, { expect: (d) => d.map((s) => s.code).join(', ') })

// ---------------------------------------------------------------------------------------------
uc(2, 'Quản lý cà phê & công thức pha chế')
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==', 'base64')
const fd = new FormData(); fd.append('file', new Blob([png], { type: 'image/png' }), 'ca-phe-muoi.png')
const img = await step('Tải ảnh món (PNG)', 'POST', '/images', undefined, { form: fd, expect: (d) => `ảnh id ${d.id} · ${d.url}` })
await step('Thêm trùng tên món đã có "Cà phê muối" bị chặn', 'POST', '/coffees', { categoryId: cat?.id, name: 'cà phê muối', price: 35000 }, { expectStatus: 400, expect: (d) => '400: ' + d?.error })
const cf = await step('Thêm món "Cà phê muối Huế" 35.000đ', 'POST', '/coffees', { categoryId: cat?.id, name: 'Cà phê muối Huế', imageId: img?.id, price: 35000, description: 'Phin Robusta, sữa đặc, phủ kem muối', status: 'AVAILABLE' },
  { expect: (d) => `id ${d.id} · ${money(d.price)} · ảnh ${d.imageUrl ? 'có' : 'không'}` })
const rc = await step('Lập công thức: 20g Robusta, 25ml sữa đặc, 30ml kem muối, 120g đá', 'POST', `/coffees/${cf?.id}/recipes`,
  { brewMethod: 'PHIN', brewTimeMin: 5, activate: true, description: 'Ly 360ml',
    materials: [{ materialId: robusta?.id, quantity: 20 }, { materialId: milk?.id, quantity: 25 }, { materialId: kemMuoi?.id, quantity: 30 }, { materialId: ice?.id, quantity: 120 }],
    steps: ['Pha phin 20g với 60ml nước 92°C', 'Cho 25ml sữa đặc vào ly, đổ cà phê, khuấy đều', 'Thêm đá', 'Phủ 30ml kem muối'] },
  { expect: (d) => `công thức #${d.id} · ${d.materials.length} NL · ${d.steps.length} bước · active=${d.active}` })
const rc2 = await step('Lập công thức nháp v2 (ít ngọt)', 'POST', `/coffees/${cf?.id}/recipes`,
  { brewMethod: 'PHIN', activate: false, materials: [{ materialId: robusta?.id, quantity: 20 }, { materialId: milk?.id, quantity: 15 }, { materialId: kemMuoi?.id, quantity: 30 }, { materialId: ice?.id, quantity: 120 }], steps: ['Như v1, giảm sữa'] },
  { expect: (d) => `#${d.id} active=${d.active}` })
await step('Sửa công thức v2: 18ml sữa', 'PUT', `/recipes/${rc2?.id}`,
  { brewMethod: 'PHIN', materials: [{ materialId: robusta?.id, quantity: 20 }, { materialId: milk?.id, quantity: 18 }, { materialId: kemMuoi?.id, quantity: 30 }, { materialId: ice?.id, quantity: 120 }], steps: ['Như v1, 18ml sữa'] },
  { expect: (d) => d.materials.map((m) => `${m.materialName} ${m.quantity}`).join(', ') })
await step('Xóa công thức nháp v2', 'DELETE', `/recipes/${rc2?.id}`, undefined, { expect: () => 'xóa thành công' })
await step('Sửa giá món 35.000 → 39.000', 'PUT', `/coffees/${cf?.id}`, { categoryId: cat?.id, name: 'Cà phê muối Huế', imageId: img?.id, price: 39000, description: 'Phin Robusta, sữa đặc, phủ kem muối', status: 'AVAILABLE' },
  { expect: (d) => check(Number(d.price) === 39000, money(d.price)) })
await step('Tạm hết món (SOLD_OUT) rồi mở bán lại', 'PATCH', `/coffees/${cf?.id}/status`, { status: 'SOLD_OUT' }, { expect: (d) => d.status })
await step('Mở bán lại', 'PATCH', `/coffees/${cf?.id}/status`, { status: 'AVAILABLE' }, { expect: (d) => d.status })
await step('Danh sách món + tìm "muối"', 'GET', '/coffees', undefined, { expect: (d) => check(d.some((c) => c.name === 'Cà phê muối Huế'), `${d.length} món`) })
await step('Công thức đang dùng của các món', 'GET', '/recipes', undefined, { expect: (d) => `${d.length} công thức đang áp dụng` })

// ---------------------------------------------------------------------------------------------
uc(12, 'Quản lý bàn & mã QR')
const tb = await step('Thêm bàn T05 tầng 2, 4 chỗ', 'POST', '/tables', { tableNo: 'T05', area: 'Tầng 2', capacity: 4, status: 'AVAILABLE' }, { expect: (d) => `id ${d.id} · QR ${d.qrCode}` })
await step('Sửa sức chứa 6 chỗ', 'PUT', `/tables/${tb?.id}`, { tableNo: 'T05', area: 'Tầng 2 – ban công', capacity: 6, status: 'AVAILABLE' }, { expect: (d) => `${d.area} · ${d.capacity} chỗ` })
const tbq = await step('Đổi mã QR (QR cũ bị chụp lại)', 'POST', `/tables/${tb?.id}/regenerate-qr`, undefined, { expect: (d) => check(d.qrCode !== tb?.qrCode, `QR mới ${d.qrCode}`) })
await step('QR cũ không còn mở được thực đơn', 'GET', `/public/tables/${encodeURIComponent(tb?.qrCode ?? 'x')}`, undefined, { token: null, expectStatus: 404, expect: () => '404' })
const tbx = await step('Thêm bàn thử', 'POST', '/tables', { tableNo: 'TMP', capacity: 2 }, { expect: (d) => 'id ' + d.id })
await step('Xóa bàn thử', 'DELETE', `/tables/${tbx?.id}`, undefined, { expect: () => 'xóa thành công' })
await step('Danh sách bàn + tìm "T05"', 'GET', '/tables', undefined, { expect: (d) => check(d.some((t) => t.tableNo === 'T05'), `${d.length} bàn`) })

// ---------------------------------------------------------------------------------------------
uc(28, 'Quản lý khu vực & phí giao hàng')
const z1 = await step('Thêm khu vực "Trung tâm": Q.1, Q.3, Phú Nhuận – 15.000đ, free ship từ 200.000đ', 'POST', '/delivery-zones',
  { name: 'Trung tâm', districts: ['Quận 1', 'Quận 3', 'Phú Nhuận'], fee: 15000, freeShipThreshold: 200000, minOrderAmount: 50000, estimatedMinutes: 25, active: true, sortOrder: 1 }, { expect: (d) => `id ${d.id} · ${d.districts}` })
const z2 = await step('Thêm khu vực "Vùng ven": Bình Thạnh, Tân Bình, Q.10 – 25.000đ', 'POST', '/delivery-zones',
  { name: 'Vùng ven', districts: ['Bình Thạnh', 'Tân Bình', 'Quận 10'], fee: 25000, minOrderAmount: 80000, estimatedMinutes: 40, active: true, sortOrder: 2 }, { expect: (d) => 'id ' + d.id })
await step('Sửa phí vùng ven 25.000 → 22.000đ', 'PUT', `/delivery-zones/${z2?.id}`,
  { name: 'Vùng ven', districts: ['Bình Thạnh', 'Tân Bình', 'Quận 10'], fee: 22000, minOrderAmount: 80000, estimatedMinutes: 40, active: true, sortOrder: 2 }, { expect: (d) => money(d.fee) })
const z3 = await step('Thêm khu vực thử', 'POST', '/delivery-zones', { name: 'Thử', districts: ['Quận 12'], fee: 40000 }, { expect: (d) => 'id ' + d.id })
await step('Xóa khu vực thử', 'DELETE', `/delivery-zones/${z3?.id}`, undefined, { expect: () => 'xóa thành công' })
await step('Khách xem quận giao được', 'GET', '/public/delivery-zones', undefined, { token: null, expect: (d) => d.map((z) => `${z.name}: ${money(z.fee)}`).join('; ') })

// ---------------------------------------------------------------------------------------------
uc(3, 'Xem & lọc thực đơn')
const menu = await step('Khách mở thực đơn (không cần đăng nhập)', 'GET', '/public/menu', undefined, { token: null,
  expect: (d) => { const cats = d.categories ?? []; const all = cats.flatMap((c) => c.coffees ?? c.items ?? []); return check(all.some((c) => c.name === 'Cà phê muối Huế'), `${cats.length} danh mục, ${all.length} món, có "Cà phê muối Huế" 39.000đ`) } })
const coffeesAll = (menu?.categories ?? []).flatMap((c) => c.coffees ?? c.items ?? [])
const cfBacXiu = coffeesAll.find((c) => c.name === 'Bạc xỉu'), cfMuoi = coffeesAll.find((c) => c.name === 'Cà phê muối Huế'), cfAme = coffeesAll.find((c) => c.name === 'Americano')
await step('Quét QR bàn T05 → mở thực đơn tại bàn', 'GET', `/public/tables/${encodeURIComponent(tbq?.qrCode ?? '')}`, undefined, { token: null, expect: (d) => `bàn ${d.tableNo} · ${d.area}` })

// ---------------------------------------------------------------------------------------------
uc(4, 'Quản lý giỏ hàng')
const cart = await step('Tạo giỏ tại bàn T05', 'POST', '/public/carts', { tableQr: tbq?.qrCode }, { token: null, expect: (d) => `giỏ ${d.sessionCode} · bàn ${d.tableNo}` })
const c1 = await step('Thêm 2 Cà phê muối (ít đá)', 'POST', `/public/carts/${cart?.sessionCode}/items`, { coffeeId: cfMuoi?.id, quantity: 2, note: 'Ít đá' }, { token: null, expect: (d) => `${d.items.length} dòng · ${money(d.subtotal)}` })
await step('Thêm 1 Bạc xỉu', 'POST', `/public/carts/${cart?.sessionCode}/items`, { coffeeId: cfBacXiu?.id, quantity: 1 }, { token: null, expect: (d) => `${d.items.length} dòng · ${money(d.subtotal)}` })
const c3 = await step('Thêm 1 Americano', 'POST', `/public/carts/${cart?.sessionCode}/items`, { coffeeId: cfAme?.id, quantity: 1 }, { token: null, expect: (d) => `${d.items.length} dòng · ${money(d.subtotal)}` })
await step('Sửa Bạc xỉu thành 2 ly', 'PUT', `/public/carts/${cart?.sessionCode}/items/${c3?.items.find((i) => i.coffeeId === cfBacXiu?.id)?.id}`, { quantity: 2 }, { token: null, expect: (d) => money(d.subtotal) })
await step('Bỏ Americano khỏi giỏ', 'DELETE', `/public/carts/${cart?.sessionCode}/items/${c3?.items.find((i) => i.coffeeId === cfAme?.id)?.id}`, undefined, { token: null,
  expect: (d) => check(d.items.length === 2 && Number(d.subtotal) === 2 * 39000 + 2 * 39000, `${d.items.length} dòng · ${money(d.subtotal)} (2×39.000 + 2×39.000)`) })
await step('Xem lại giỏ', 'GET', `/public/carts/${cart?.sessionCode}`, undefined, { token: null, expect: (d) => d.items.map((i) => `${i.quantity}× ${i.coffeeName}`).join(', ') })

// ---------------------------------------------------------------------------------------------
uc(5, 'Đặt hàng & thanh toán (QR / online)')
const q1 = await step('Báo giá đơn tại bàn', 'POST', `/public/carts/${cart?.sessionCode}/quote`, { orderType: 'DINE_IN' }, { token: null, expect: (d) => `tạm tính ${money(d.subtotal)} · tổng ${money(d.total)}` })
const oQR = await step('Đặt đơn tại bàn, trả tiền mặt tại quầy', 'POST', '/public/checkout', { sessionCode: cart?.sessionCode, orderType: 'DINE_IN', customerName: 'Khách bàn T05', paymentMethod: 'CASH', note: 'Mang ra cùng lúc' },
  { token: null, expect: (d) => `${d.orderCode} · ${d.status} · ${money(d.totalAmount)} · ${d.channel}` })
// online, giao hàng Q.3
const cartB = await step('Khách online tạo giỏ', 'POST', '/public/carts', {}, { token: null, expect: (d) => d.sessionCode })
await step('Thêm 3 Cà phê muối + 2 Bạc xỉu', 'POST', `/public/carts/${cartB?.sessionCode}/items`, { coffeeId: cfMuoi?.id, quantity: 3 }, { token: null, expect: (d) => money(d.subtotal) })
await step('(tiếp)', 'POST', `/public/carts/${cartB?.sessionCode}/items`, { coffeeId: cfBacXiu?.id, quantity: 2 }, { token: null, expect: (d) => money(d.subtotal) })
await step('Báo giá giao tới Quận 3 (dưới 200k → phí 15.000đ)', 'POST', `/public/carts/${cartB?.sessionCode}/quote`, { orderType: 'DELIVERY', deliveryDistrict: 'Quận 3' }, { token: null,
  expect: (d) => `tạm tính ${money(d.subtotal)} · phí ship ${money(d.shippingFee)} · tổng ${money(d.total)}` })
await step('Quận ngoài vùng giao bị từ chối', 'POST', `/public/carts/${cartB?.sessionCode}/quote`, { orderType: 'DELIVERY', deliveryDistrict: 'Củ Chi' }, { token: null, expectStatus: 400, expect: (d) => '400 ' + short(d?.error) })
const oOnline = await step('Đặt giao hàng, thanh toán VNPAY (ATM nội địa)', 'POST', '/public/checkout',
  { sessionCode: cartB?.sessionCode, orderType: 'DELIVERY', customerName: 'Nguyễn Thị Thu Hà', customerPhone: '0938456712', deliveryDistrict: 'Quận 3',
    delivery: { recipientName: 'Nguyễn Thị Thu Hà', recipientPhone: '0938456712', address: '58 Võ Văn Tần, Phường Võ Thị Sáu, Quận 3, TP.HCM' }, paymentMethod: 'BANK_TRANSFER' },
  { token: null, expect: (d) => `${d.orderCode} · ${money(d.totalAmount)} · thanh toán ${d.payments.map((p) => p.status).join(',')}` })
const link = await step('Tạo link VNPAY (ký HMAC-SHA512, hết hạn 15 phút)', 'POST', `/public/orders/${oOnline?.orderCode}/pay`, undefined, { token: null,
  expect: (d) => check(d.paymentUrl.includes('vnp_SecureHash='), `${d.txnRef} · hết hạn ${d.expireAt}`) })
const vnp = link ? Object.fromEntries(new URL(link.paymentUrl).searchParams) : {}
const sb = new URLSearchParams(vnp)
const done = await fetch(`${BASE}/public/payments/sandbox/complete?result=00`, { method: 'POST', body: sb, headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, redirect: 'manual' })
cur.steps.push({ title: 'Khách bấm "Thanh toán thành công" trên cổng VNPAY (giả lập) → IPN về server', method: 'POST', path: '/public/payments/sandbox/complete?result=00', status: done.status, pass: done.status === 302,
  actual: 'redirect ' + (done.headers.get('location') ?? '').slice(0, 90) })
const back = done.headers.get('location') ? new URL(done.headers.get('location')).search : ''
await step('Trang kết quả kiểm chữ ký return URL', 'GET', `/public/payments/vnpay/return${back}`, undefined, { token: null, expect: (d) => check(d.validSignature && d.status === 'SUCCESS', `chữ ký hợp lệ · ${d.status} · ${money(d.amount)}`) })
await step('Đơn online đã được ghi nhận thanh toán', 'GET', `/public/orders/${oOnline?.orderCode}`, undefined, { token: null, expect: (d) => check(d.payments.some((p) => p.status === 'PAID'), d.payments.map((p) => p.method + '/' + p.status).join(',')) })
await step('IPN giả mạo (sai chữ ký) bị từ chối', 'GET', `/public/payments/vnpay/ipn?vnp_TxnRef=${vnp.vnp_TxnRef}&vnp_Amount=100&vnp_ResponseCode=00&vnp_SecureHash=abc`, undefined, { token: null,
  expect: (d) => check(d.RspCode === '97', `RspCode ${d.RspCode} – ${d.Message}`) })

// ---------------------------------------------------------------------------------------------
uc(32, 'Mở / chốt ca thu ngân')
await step('Gợi ý tiền đầu ca', 'GET', '/cash-shifts/suggested-opening', undefined, { token: T.staff, expect: (d) => money(d.openingCash) })
const sh = await step('Thu ngân Yến mở ca với 1.500.000đ', 'POST', '/cash-shifts/open', { openingCash: 1500000, note: 'Ca sáng' }, { token: T.staff, expect: (d) => `${d.code} · đầu ca ${money(d.openingCash)}` })
await step('Không mở ca thứ 2 khi đang có ca', 'POST', '/cash-shifts/open', { openingCash: 0 }, { token: T.staff, expectStatus: 400, expect: (d) => '400 ' + short(d?.error) })

// ---------------------------------------------------------------------------------------------
uc(6, 'Tạo đơn tại quầy')
await step('Tra khách theo SĐT 0938456712', 'GET', '/customers/lookup?phone=0938456712', undefined, { token: T.staff, expect: (d) => d ? `${d.fullName ?? d.name} · điểm ${d.points ?? 0}` : 'không có' })
await step('Báo giá tại quầy', 'POST', '/orders/quote', { items: [{ coffeeId: cfAme?.id, quantity: 2 }, { coffeeId: cfMuoi?.id, quantity: 1 }], orderType: 'TAKE_AWAY' }, { token: T.staff, expect: (d) => money(d.total) })
const oCounter = await step('Tạo đơn mang đi: 2 Americano + 1 Cà phê muối, tiền mặt', 'POST', '/orders',
  { items: [{ coffeeId: cfAme?.id, quantity: 2 }, { coffeeId: cfMuoi?.id, quantity: 1, note: 'Nhiều kem' }], orderType: 'TAKE_AWAY', customerPhone: '0938456712', paymentMethod: 'CASH' },
  { token: T.staff, expect: (d) => `${d.orderCode} · ${d.status} · ${money(d.totalAmount)} · ${d.payments.map((p) => p.method + '/' + p.status)}` })
const oCounter2 = await step('Tạo đơn tại bàn, quẹt thẻ', 'POST', '/orders', { items: [{ coffeeId: cfBacXiu?.id, quantity: 1 }], orderType: 'DINE_IN', tableId: tb?.id, paymentMethod: 'CARD' },
  { token: T.staff, expect: (d) => `${d.orderCode} · ${money(d.totalAmount)}` })
await step('Tìm đơn hôm nay kênh tại quầy', 'GET', `/orders?channel=COUNTER&from=${TODAY}&to=${TODAY}`, undefined, { token: T.staff, expect: (d) => `${d.length} đơn: ${d.map((o) => o.orderCode).join(', ')}` })

// ---------------------------------------------------------------------------------------------
uc(7, 'Xử lý đơn hàng (xác nhận → hoàn tất)')
const oq = await step('Lọc đơn chờ xác nhận', 'GET', `/orders?status=PENDING&from=${TODAY}&to=${TODAY}`, undefined, { token: T.staff, expect: (d) => d.map((o) => `${o.orderCode}/${o.channel}`).join(', ') })
const qrId = oq?.find((o) => o.orderCode === oQR?.orderCode)?.id
await step('Xác nhận đơn QR bàn T05 (→ đang pha chế, trừ kho theo công thức)', 'POST', `/orders/${qrId}/confirm`, undefined, { token: T.staff, expect: (d) => d.status })
await step('Pha xong → sẵn sàng', 'POST', `/orders/${qrId}/ready`, undefined, { token: T.staff, expect: (d) => d.status })
await step('Thu tiền mặt tại quầy', 'POST', `/orders/${qrId}/collect-payment`, undefined, { token: T.staff, expect: (d) => d.payments.map((p) => p.method + '/' + p.status).join(',') })
await step('Hoàn tất đơn', 'POST', `/orders/${qrId}/complete`, undefined, { token: T.staff, expect: (d) => check(d.status === 'COMPLETED', d.status) })
await step('Lịch sử trạng thái đơn', 'GET', `/orders/${qrId}/history`, undefined, { token: T.staff, expect: (d) => d.map((h) => h.toStatus ?? h.status).join(' → ') })
for (const o of [oCounter, oCounter2]) if (o) {
  await step(`Đơn tại quầy ${o.orderCode} vào thẳng bếp (không cần xác nhận)`, 'GET', `/orders/${o.id}`, undefined, { token: T.staff, expect: (d) => check(d.status === 'PREPARING', d.status) })
  for (const a of ['ready', 'complete']) await step(`${a} đơn ${o.orderCode}`, 'POST', `/orders/${o.id}/${a}`, undefined, { token: T.staff, expect: (d) => d.status })
}
await step('Tồn Robusta giảm theo công thức đã bán', 'GET', `/materials/${robusta?.id}/batches`, undefined, { expect: (d) => d.map((b) => `${b.remainingQuantity}g`).join(' + ') })

// ---------------------------------------------------------------------------------------------
uc(10, 'Điều phối giao hàng')
const onl = (await (await fetch(`${BASE}/orders?from=${TODAY}&to=${TODAY}`, { headers: { Authorization: 'Bearer ' + T.admin } })).json()).find((o) => o.orderCode === oOnline?.orderCode)
await step('Xác nhận đơn giao hàng (đã trả online)', 'POST', `/orders/${onl?.id}/confirm`, undefined, { token: T.staff, expect: (d) => d.status })
await step('Pha xong', 'POST', `/orders/${onl?.id}/ready`, undefined, { token: T.staff, expect: (d) => d.status })
const ships = await step('Danh sách chuyến giao chờ đặt xe', 'GET', '/shipments', undefined, { token: T.staff, expect: (d) => d.map((s) => `${s.orderCode}/${s.status}`).join(', ') })
const shp = ships?.find((s) => s.orderCode === oOnline?.orderCode)

// ---------------------------------------------------------------------------------------------
uc(26, 'Tích hợp đối tác giao hàng (báo giá, đặt xe, webhook)')
await step('Báo giá Ahamove (SGN-BIKE)', 'GET', `/shipments/${shp?.id}/ahamove/estimate`, undefined, { token: T.staff, expect: (d) => `${money(d.fee)} · ${d.distanceKm} km · ~${d.durationMinutes} phút` })
const bk = await step('Đặt xe Ahamove', 'POST', `/shipments/${shp?.id}/ahamove/book`, undefined, { token: T.staff, expect: (d) => `${d.status} · mã ${d.trackingCode ?? d.partnerOrderId}` })
const pid = bk?.partnerOrderId ?? bk?.trackingCode
await step('Webhook sai token bị chặn', 'POST', `/public/delivery/ahamove/webhook?token=sai`, { _id: pid, status: 'ACCEPTED' }, { token: null, expectStatus: 401, expect: () => '401' })
await step('Webhook: tài xế nhận đơn', 'POST', `/public/delivery/ahamove/webhook?token=coffeeholic-webhook`, { _id: pid, status: 'ACCEPTED', supplier_name: 'Trần Văn Tài', supplier_id: '84909888777' }, { token: null, expect: (d) => d.message })
await step('Webhook: đang giao', 'POST', `/public/delivery/ahamove/webhook?token=coffeeholic-webhook`, { _id: pid, status: 'IN PROCESS' }, { token: null, expect: (d) => d.message })
await step('Lịch sử chuyến', 'GET', `/shipments/${shp?.id}/events`, undefined, { token: T.staff, expect: (d) => d.map((e) => `${e.source}:${e.status ?? e.partnerStatus}`).join(' → ') })
await step('Webhook: giao thành công', 'POST', `/public/delivery/ahamove/webhook?token=coffeeholic-webhook`, { _id: pid, status: 'COMPLETED', path: [{ status: 'COMPLETED' }] }, { token: null, expect: (d) => d.message })
await step('Đơn giao xong', 'GET', `/orders/${onl?.id}`, undefined, { token: T.staff, expect: (d) => `${d.status} · chuyến ${d.shipment?.status} · tài xế ${d.shipment?.driverName ?? '-'}` })
// gọi xe ngoài cho đơn khác
const cartC = await (await fetch(BASE + '/public/carts', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}' })).json()
await fetch(`${BASE}/public/carts/${cartC.sessionCode}/items`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ coffeeId: cfMuoi?.id, quantity: 6 }) })
const oShip2 = await step('Đơn giao Bình Thạnh 6 ly, trả tiền mặt khi nhận (COD)', 'POST', '/public/checkout',
  { sessionCode: cartC.sessionCode, orderType: 'DELIVERY', customerName: 'Văn phòng Lotus', customerPhone: '0918777666', deliveryDistrict: 'Bình Thạnh',
    delivery: { recipientName: 'Chị Ngọc – lễ tân', recipientPhone: '0918777666', address: '208 Nguyễn Hữu Cảnh, P.22, Bình Thạnh' }, paymentMethod: 'CASH' },
  { token: null, expect: (d) => `${d.orderCode} · ${money(d.totalAmount)} (6×39.000 + ship ${money(d.shippingFee)})` })
const o2 = (await (await fetch(`${BASE}/orders?from=${TODAY}&to=${TODAY}`, { headers: { Authorization: 'Bearer ' + T.admin } })).json()).find((o) => o.orderCode === oShip2?.orderCode)
for (const a of ['confirm', 'ready']) await step(`${a}`, 'POST', `/orders/${o2?.id}/${a}`, undefined, { token: T.staff, expect: (d) => d.status })
const s2 = (await (await fetch(`${BASE}/shipments`, { headers: { Authorization: 'Bearer ' + T.admin } })).json()).find((s) => s.orderCode === oShip2?.orderCode)
await step('Gọi xe ngoài (Grab) – nhập tay mã chuyến, tài xế lấy hàng', 'PATCH', `/shipments/${s2?.id}`, { status: 'DELIVERING', carrier: 'GrabExpress', trackingCode: 'GRAB-26-88123', note: 'Tài xế Minh 0909xxx' }, { token: T.staff, expect: (d) => `${d.status} · ${d.carrier}` })
await step('Đơn chuyển "Đang giao"', 'GET', `/orders/${o2?.id}`, undefined, { token: T.staff, expect: (d) => check(d.status === 'DELIVERING', d.status) })
await step('Cập nhật đã giao', 'PATCH', `/shipments/${s2?.id}`, { status: 'DELIVERED', carrier: 'GrabExpress', trackingCode: 'GRAB-26-88123' }, { token: T.staff, expect: (d) => d.status })
await step('Đơn tự hoàn tất, tiền COD ghi nhận đã thu', 'GET', `/orders/${o2?.id}`, undefined, { token: T.staff, expect: (d) => check(d.status === 'COMPLETED' && d.payments.every((p) => p.status === 'PAID'), `${d.status} · ${d.payments.map((p) => p.method + '/' + p.status)}`) })
cur = results.find((r) => r.no === 10)
await step('Lọc chuyến giao (FE lọc theo trạng thái)', 'GET', '/shipments', undefined, { token: T.staff, expect: (d) => d.map((s) => `${s.orderCode}/${s.status}`).join(', ') })

// ---------------------------------------------------------------------------------------------
uc(9, 'Theo dõi trạng thái đơn hàng')
await step('Khách tra cứu đơn online bằng mã', 'GET', `/public/orders/${oOnline?.orderCode}`, undefined, { token: null, expect: (d) => `${d.status} · ${d.shipment?.status} · ${d.items.length} món` })
await step('Mã đơn sai → 404', 'GET', '/public/orders/KHONGCO123', undefined, { token: null, expectStatus: 404, expect: () => '404' })
await step('Khách đăng nhập xem lịch sử đơn của mình', 'GET', '/account/orders', undefined, { token: T.customer, expect: (d) => `${d.length} đơn: ${d.map((o) => o.orderCode + '/' + o.status).join(', ')}` })

// ---------------------------------------------------------------------------------------------
uc(8, 'Hủy đơn & hoàn tiền')
const cartD = await (await fetch(BASE + '/public/carts', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}' })).json()
await fetch(`${BASE}/public/carts/${cartD.sessionCode}/items`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ coffeeId: cfBacXiu?.id, quantity: 2 }) })
const oCancel = await step('Khách đặt mang đi, tiền mặt', 'POST', '/public/checkout', { sessionCode: cartD.sessionCode, orderType: 'TAKE_AWAY', customerName: 'Anh Bảo', customerPhone: '0977123456', paymentMethod: 'CASH' }, { token: null, expect: (d) => d.orderCode })
await step('Khách tự hủy khi đơn còn chờ xác nhận', 'POST', `/public/orders/${oCancel?.orderCode}/cancel`, { reason: 'Đặt nhầm món' }, { token: null, expect: (d) => check(d.status === 'CANCELLED', d.status) })
// đơn online đã trả rồi hủy → hoàn tiền qua cổng
const cartE = await (await fetch(BASE + '/public/carts', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}' })).json()
await fetch(`${BASE}/public/carts/${cartE.sessionCode}/items`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ coffeeId: cfMuoi?.id, quantity: 2 }) })
const oPaid = await step('Đơn mang đi trả VNPAY', 'POST', '/public/checkout', { sessionCode: cartE.sessionCode, orderType: 'TAKE_AWAY', customerName: 'Chị Linh', customerPhone: '0966332211', paymentMethod: 'E_WALLET' }, { token: null, expect: (d) => d.orderCode })
const l2 = await (await fetch(`${BASE}/public/orders/${oPaid?.orderCode}/pay`, { method: 'POST' })).json()
await fetch(`${BASE}/public/payments/sandbox/complete?result=00`, { method: 'POST', body: new URLSearchParams(Object.fromEntries(new URL(l2.paymentUrl).searchParams)), headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, redirect: 'manual' })
const op = (await (await fetch(`${BASE}/orders?from=${TODAY}&to=${TODAY}`, { headers: { Authorization: 'Bearer ' + T.admin } })).json()).find((o) => o.orderCode === oPaid?.orderCode)
await step('Quán từ chối đơn đã thanh toán (hết kem muối) → tự hoàn tiền qua VNPAY', 'POST', `/orders/${op?.id}/reject`, { reason: 'Hết kem muối' }, { token: T.staff,
  expect: (d) => `${d.status} · ${d.payments.map((p) => p.method + '/' + p.status).join(',')}` })
// hoàn 1 phần cho đơn đã hoàn tất (tại quầy)
const oc = await (await fetch(`${BASE}/orders/${oCounter?.id}`, { headers: { Authorization: 'Bearer ' + T.admin } })).json()
await step('Hoàn 39.000đ cho đơn tại quầy đã hoàn tất (1 ly làm sai)', 'POST', `/payments/${oc.payments?.[0]?.id}/refund`, { amount: 39000 }, { expect: (d) => `${d.status} · hoàn ${money(d.refundedAmount ?? 39000)}` })
await step('Không hoàn quá số đã trả', 'POST', `/payments/${oc.payments?.[0]?.id}/refund`, { amount: 9999999 }, { expectStatus: 400, expect: (d) => '400: ' + d?.error })
await step('Không hủy được đơn đã hoàn tất', 'POST', `/orders/${oCounter2?.id}/cancel`, { reason: 'Thử' }, { expectStatus: 400, expect: (d) => '400 (đơn đã hoàn tất không hủy được) ' + short(d?.error) })

// ---------------------------------------------------------------------------------------------
uc(11, 'Ghi nhận & xử lý sự cố đơn hàng')
const inc = await step('Ghi nhận sự cố: giao trễ 25 phút đơn online', 'POST', `/orders/${onl?.id}/incidents`, { type: 'LATE', severity: 'MEDIUM', reason: 'Tài xế lạc đường', description: 'Khách gọi phàn nàn lúc 10:40' },
  { token: T.staff, expect: (d) => `#${d.id} · ${d.type} · ${d.status}` })
await step('Sự cố của đơn', 'GET', `/orders/${onl?.id}/incidents`, undefined, { token: T.staff, expect: (d) => `${d.length} sự cố` })
await step('Xử lý: xin lỗi + tặng voucher 20.000đ hạn 30 ngày', 'POST', `/incidents/${inc?.id}/resolve`, { resolution: 'Gọi xin lỗi khách, tặng voucher', voucherValue: 20000, voucherMinOrderValue: 50000, voucherCount: 1, voucherValidDays: 30 },
  { expect: (d) => `${d.status} · voucher ${d.voucherCode ?? short(d.vouchers ?? '')}` })
await step('Danh sách sự cố (lọc FE theo trạng thái)', 'GET', '/incidents', undefined, { expect: (d) => d.map((i) => `${i.orderCode}/${i.type}/${i.status}`).join(', ') })

// ---------------------------------------------------------------------------------------------
uc(27, 'Hóa đơn điện tử')
await step('Khách online yêu cầu xuất hóa đơn công ty', 'POST', `/public/orders/${oOnline?.orderCode}/invoice-request`,
  { buyerTaxCode: '0316789012', buyerCompany: 'Công ty TNHH Giải pháp Số Thu Hà', buyerAddress: '58 Võ Văn Tần, P. Võ Thị Sáu, Q.3, TP.HCM', buyerName: 'Nguyễn Thị Thu Hà', buyerEmail: 'ketoan@thuha-digital.vn' },
  { token: null, expect: (d) => `${d.status} · ${d.buyerCompany}` })
await step('Phát hành ngay (job phát hành chạy tay)', 'POST', '/invoices/issue-now', undefined, { expect: (d) => `${d.issued} hóa đơn` })
const invs = await step('Tìm hóa đơn hôm nay', 'GET', `/invoices?from=${TODAY}&to=${TODAY}`, undefined, { expect: (d) => d.map((i) => `${i.series}-${i.invoiceNo}/${i.kind}/${i.status}/${money(i.totalAmount)}`).join('; ') })
const invOnline = invs?.find((i) => i.orderCode === oOnline?.orderCode && i.kind === 'ORIGINAL')
await step('Tìm theo MST', 'GET', `/invoices?from=${TODAY}&to=${TODAY}&keyword=0316789012`, undefined, { expect: (d) => `${d.length} kết quả` })
await step('Xem chi tiết hóa đơn', 'GET', `/invoices/${invOnline?.id}`, undefined, { expect: (d) => `mã CQT ${d.taxAuthorityCode} · tra cứu ${d.lookupCode} · ${d.lines.length} dòng` })
await step('Sai tên công ty → lập hóa đơn thay thế', 'POST', `/invoices/${invOnline?.id}/replace`,
  { buyer: { buyerTaxCode: '0316789012', buyerCompany: 'Công ty TNHH Giải pháp Số Thu Hà Việt Nam', buyerAddress: '58 Võ Văn Tần, P. Võ Thị Sáu, Q.3, TP.HCM' }, reason: 'Sai tên đơn vị' },
  { expect: (d) => `${d.kind} · ${d.status}` })
await step('Phát hành hóa đơn thay thế', 'POST', '/invoices/issue-now', undefined, { expect: (d) => `${d.issued}` })
await step('Hóa đơn gốc chuyển "Đã bị thay thế", hóa đơn hoàn tiền là "Điều chỉnh"', 'GET', `/invoices?from=${TODAY}&to=${TODAY}`, undefined,
  { expect: (d) => d.map((i) => `${i.orderCode}:${i.kind}/${i.status}`).join('; ') })
await step('Khách tra cứu hóa đơn của đơn', 'GET', `/public/orders/${oOnline?.orderCode}/invoices`, undefined, { token: null, expect: (d) => d.map((i) => `${i.kind}/${i.status}/${i.invoiceNo}`).join(', ') })

// ---------------------------------------------------------------------------------------------
uc(25, 'Tích hợp & đối soát thanh toán online (VNPAY)')
const txs = await step('Danh sách giao dịch VNPAY hôm nay', 'GET', `/payment-transactions?from=${TODAY}&to=${TODAY}`, undefined, { expect: (d) => d.map((t) => `${t.txnRef}/${t.type}/${t.status}/${money(t.amount)}`).join('; ') })
await step('Lọc giao dịch lệch trạng thái', 'GET', `/payment-transactions?from=${TODAY}&to=${TODAY}&mismatch=true`, undefined, { expect: (d) => `${d.length} giao dịch lệch` })
const tx = txs?.find((t) => t.status === 'SUCCESS' && t.type === 'PAY')
await step('Truy vấn lại trạng thái (querydr)', 'POST', `/payment-transactions/${tx?.id}/requery`, undefined, { expect: (d) => d.status })
await step('Đánh dấu đã đối soát với file VNPAY', 'POST', `/payment-transactions/${tx?.id}/reconcile`, { note: 'Khớp file đối soát ngày ' + TODAY }, { expect: (d) => check(d.reconciled, `đã đối soát bởi ${d.reconciledBy}`) })

// ---------------------------------------------------------------------------------------------
uc(35, 'Gửi email/SMS xác nhận đơn & OTP')
await step('Mẫu tin', 'GET', '/notifications/templates', undefined, { expect: (d) => d.map((t) => `${t.code}${t.enabled ? '' : '(tắt)'}`).join(', ') })
await step('Gắn ID mẫu ZNS đã duyệt cho tin xác nhận', 'PUT', '/notifications/templates/ORDER_CONFIRMED', { znsTemplateId: '298765', smsFallback: true }, { expect: (d) => `ZNS ${d.znsTemplateId}` })
await new Promise((r) => setTimeout(r, 15000))
const msgs = await step('Nhật ký gửi tin hôm nay', 'GET', `/notifications/messages?from=${TODAY}&to=${TODAY}`, undefined, { expect: (d) => d.map((m) => `${m.templateCode}/${m.channel ?? '-'}/${m.status}`).join('; ') })
await step('Lọc theo SĐT khách', 'GET', `/notifications/messages?from=${TODAY}&to=${TODAY}&phone=0938456712`, undefined, { expect: (d) => `${d.length} tin: ${d.map((m) => m.templateCode + '/' + m.channel + '/' + m.status).join(', ')}` })
const failed = msgs?.find((m) => m.status === 'FAILED')
if (failed) await step('Gửi lại tin lỗi', 'POST', `/notifications/messages/${failed.id}/resend`, undefined, { expect: (d) => d.status })
await step('Không gửi lại tin đã gửi thành công', 'POST', `/notifications/messages/${msgs?.find((m) => m.status === 'SENT' || m.status === 'DELIVERED')?.id}/resend`, undefined, { expectStatus: 400, expect: (d) => '400: ' + d?.error })

// ---------------------------------------------------------------------------------------------
uc(14, 'Cảnh báo tồn kho thấp & lô hết hạn')
await step('Danh sách cảnh báo', 'GET', '/inventory/alerts', undefined, { expect: (d) => short(d, 300) })

// ---------------------------------------------------------------------------------------------
uc(31, 'Báo cáo xuất – nhập – tồn kho')
await step('Chốt tồn hôm nay (job 23:55 chạy tay)', 'POST', '/reports/inventory/close-day', undefined, { expect: (d) => `${d.materials} nguyên liệu` })
await step('Báo cáo từ đầu tháng', 'GET', `/reports/inventory?from=${TODAY.slice(0, 8)}01&to=${TODAY}`, undefined,
  { expect: (d) => `đầu kỳ ${money(d.openingValue)} + nhập ${money(d.inValue)} − xuất ${money(d.outValue)} = cuối kỳ ${money(d.closingValue)} · ${d.rows.length} dòng` })
await step('Tồn theo ngày của Robusta', 'GET', `/reports/inventory/daily?from=${TODAY.slice(0, 8)}01&to=${TODAY}&materialId=${robusta?.id}`, undefined, { expect: (d) => d.map((x) => `${x.date}: ${x.quantity}`).join('; ') })
await step('Xuất Excel', 'GET', `/reports/inventory.xlsx?from=${TODAY.slice(0, 8)}01&to=${TODAY}`, undefined, { binary: true, expect: (d) => check(d.xlsx, `xlsx ${d.bytes} byte`) })

// ---------------------------------------------------------------------------------------------
uc(34, 'Giá vốn món theo công thức')
await step('Tính lại giá vốn (job mỗi giờ chạy tay)', 'POST', '/coffee-costs/recalculate', undefined, { expect: (d) => `${d.changed} món đổi giá vốn` })
const costs = await step('Bảng giá vốn', 'GET', '/coffee-costs', undefined, { expect: (d) => d.filter((c) => ['Cà phê muối Huế', 'Bạc xỉu', 'Americano'].includes(c.coffeeName)).map((c) => `${c.coffeeName}: vốn ${money(c.unitCost)}/ly, bán ${money(c.price)}, food cost ${c.foodCostPercent}%`).join('; ') })
await step('Điều chỉnh giá bán Cà phê muối 39.000 → 42.000đ', 'PUT', `/coffee-costs/${cf?.id}/price`, { price: 42000 }, { expect: (d) => `food cost còn ${d.foodCostPercent}%` })
await step('Lịch sử giá vốn', 'GET', `/coffee-costs/${cf?.id}/history`, undefined, { expect: (d) => d.map((h) => `${money(h.unitCost)}/${money(h.price)}`).join(' → ') })

// ---------------------------------------------------------------------------------------------
cur = results.find((r) => r.no === 32)
await step('Chi trong ca: mua đá 2 bao', 'POST', '/cash-shifts/current/movements', { type: 'OUT', amount: 50000, reason: 'Mua 2 bao đá 20kg' }, { token: T.staff, expect: (d) => `chi ${money(d.cashOut)}` })
await step('Thu khác: khách gửi trả tiền thiếu hôm qua', 'POST', '/cash-shifts/current/movements', { type: 'IN', amount: 20000, reason: 'Khách bàn 3 trả bù' }, { token: T.staff, expect: (d) => `thu ${money(d.cashIn)}` })
const curSh = await step('Xem ca hiện tại (tiền mặt lý thuyết)', 'GET', '/cash-shifts/current', undefined, { token: T.staff, expect: (d) => `${d.code} · đơn ${d.orderCount} · tiền mặt bán ${money(d.cashSales)} · lý thuyết ${money(d.expectedCash)}` })
// đếm tiền đúng theo mệnh giá
let rest = Number(curSh?.expectedCash ?? 0); const den = {}
for (const v of [500000, 200000, 100000, 50000, 20000, 10000, 5000, 2000, 1000]) { const n = Math.floor(rest / v); if (n) den[v] = n; rest -= n * v }
await step('Chốt ca: đếm theo mệnh giá, để lại 1.000.000đ ca sau, nộp phần còn lại', 'POST', '/cash-shifts/current/close', { denominations: den, nextOpeningCash: 1000000, receivedBy: 'Nguyễn Minh Anh (quản lý)', note: 'Ca sáng 21/10' },
  { token: T.staff, expect: (d) => `thực đếm ${money(d.countedCash)} · lệch ${money(Number(d.countedCash) - Number(d.expectedCash))} · nộp ${money(d.handoverCash)}` })
await step('Lịch sử ca', 'GET', `/cash-shifts?from=${TODAY}&to=${TODAY}`, undefined, { expect: (d) => d.map((s) => `${s.code}/${s.closedAt ? 'đã chốt' : 'mở'}`).join(', ') })

// ---------------------------------------------------------------------------------------------
uc(33, 'Ca làm việc & chấm công nhân viên')
const ws = await step('Thêm ca mẫu "Ca gãy trưa" 11:00–14:00', 'POST', '/work-shifts', { name: 'Ca gãy trưa', startTime: '11:00', endTime: '14:00', graceMinutes: 5, color: '#f59e0b' }, { expect: (d) => `id ${d.id} · ${d.minutes} phút` })
await step('Sửa ca: cho trễ 10 phút', 'PUT', `/work-shifts/${ws?.id}`, { id: ws?.id, name: 'Ca gãy trưa', startTime: '11:00', endTime: '14:00', graceMinutes: 10, color: '#f59e0b' }, { expect: (d) => `${d.graceMinutes} phút` })
const allWs = await (await fetch(BASE + '/work-shifts', { headers: { Authorization: 'Bearer ' + T.admin } })).json()
const nowH = new Date(Date.now() + 7 * 3600e3).getUTCHours()
const todayShift = await step('Thêm ca phủ giờ hiện tại để chấm công', 'POST', '/work-shifts', { name: 'Ca test', startTime: `${String(Math.max(0, nowH - 1)).padStart(2, '0')}:00`, endTime: `${String(Math.min(23, nowH + 2)).padStart(2, '0')}:00`, graceMinutes: 5 }, { expect: (d) => `${d.startTime}–${d.endTime}` })
const asg = await step('Xếp ca cho Yến + Khoa hôm nay và ngày mai', 'POST', '/shift-assignments', { staffIds: [st2?.id, st1?.id], dates: [TODAY, plus(1)], shiftId: todayShift?.id, position: 'Thu ngân' },
  { expect: (d) => `${d.length} phân ca` })
await step('Lịch tuần', 'GET', `/shift-assignments?from=${TODAY}&to=${plus(6)}`, undefined, { expect: (d) => `${d.length} ca trong tuần` })
const mine = await step('Yến xem ca của mình hôm nay', 'GET', '/attendance/today', undefined, { token: T.staff, expect: (d) => d.map((a) => `${a.shiftName} ${a.state}`).join(', ') })
const myA = mine?.[0]
await step('Yến chấm vào', 'POST', `/attendance/${myA?.id}/check-in`, undefined, { token: T.staff, expect: (d) => `${d.state} lúc ${d.checkInAt}` })
await step('Yến chấm ra', 'POST', `/attendance/${myA?.id}/check-out`, undefined, { token: T.staff, expect: (d) => `${d.state} lúc ${d.checkOutAt}` })
const khoaA = asg?.find((a) => a.staffId === st1?.id && a.workDate === TODAY)
await step('Quản lý sửa công: Khoa quên chấm, ghi nghỉ có phép', 'PUT', `/shift-assignments/${khoaA?.id}/attendance`, { leaveType: 'P', note: 'Xin nghỉ ốm, có giấy' }, { expect: (d) => d.state })
const tomorrow = asg?.find((a) => a.staffId === st1?.id && a.workDate === plus(1))
await step('Bỏ phân ca ngày mai của Khoa', 'DELETE', `/shift-assignments/${tomorrow?.id}`, undefined, { expect: () => 'xóa thành công' })
await step('Bảng chấm công tháng', 'GET', `/timesheets?month=${TODAY.slice(0, 7)}`, undefined, { expect: (d) => d.rows.map((r) => `${r.staffName}: xếp ${r.scheduled} ca, làm ${r.worked} ca (${r.workedHours} giờ), trễ ${r.lateTimes}, nghỉ P ${r.leavePaid}, KL ${r.leaveUnpaid}, vắng ${r.absent}`).join(" | ") })
await step('Xuất Excel bảng chấm công (mẫu 01a-LĐTL)', 'GET', `/timesheets.xlsx?month=${TODAY.slice(0, 7)}`, undefined, { binary: true, expect: (d) => check(d.xlsx, `xlsx ${d.bytes} byte`) })
await step('Xóa ca mẫu "Ca gãy trưa" (chưa dùng)', 'DELETE', `/work-shifts/${ws?.id}`, undefined, { expect: () => 'xóa thành công' })

// ---------------------------------------------------------------------------------------------
uc(24, 'Theo dõi tổng quan vận hành')
await step('Tổng quan hôm nay', 'GET', '/dashboard', undefined, { expect: (d) => short(d, 300) })

// ---------------------------------------------------------------------------------------------
uc(36, 'Nhật ký thao tác hệ thống')
await new Promise((r) => setTimeout(r, 6000))
await step('Bộ lọc: người dùng, module', 'GET', '/audit-logs/filters', undefined, { expect: (d) => `${d.usernames.join(', ')} · ${d.modules.length} module` })
await step('Tìm thao tác của yen.lh hôm nay', 'GET', `/audit-logs?from=${TODAY}&to=${TODAY}&username=yen.lh&size=50`, undefined, { expect: (d) => `${d.total} thao tác, vd ${d.items.slice(0, 3).map((a) => a.action + ' ' + a.path).join('; ')}` })
await step('Lọc thao tác lỗi', 'GET', `/audit-logs?from=${TODAY}&to=${TODAY}&failed=true&size=50`, undefined, { expect: (d) => `${d.total} thao tác lỗi (các ca test chặn sai)` })
await step('Tìm theo từ khóa "purchase-orders"', 'GET', `/audit-logs?from=${TODAY}&to=${TODAY}&keyword=purchase-orders&size=50`, undefined, { expect: (d) => `${d.total} kết quả` })
await step('Xuất Excel nhật ký', 'GET', `/audit-logs/export.xlsx?from=${TODAY}&to=${TODAY}`, undefined, { binary: true, expect: (d) => check(d.xlsx, `xlsx ${d.bytes} byte`) })

results.sort((a, b) => a.no - b.no)
fs.writeFileSync(OUT, JSON.stringify({ runAt: new Date().toISOString(), base: BASE, results }, null, 1))
for (const r of results) {
  const f = r.steps.filter((s) => !s.pass)
  console.log(`UC${String(r.no).padStart(2)} ${f.length ? 'FAIL' : 'PASS'} ${r.steps.length - f.length}/${r.steps.length} ${r.name}`)
  for (const s of f) console.log(`      ✗ ${s.title} [${s.method} ${s.path}] → ${s.actual}`)
}
