// Dữ liệu demo 30 ngày cho app-core, tạo qua API thật (tồn kho, công nợ, hóa đơn, ca... tự khớp nhau).
// Mỗi "ngày giả" chạy trong một khung thời gian thật; demo-remap.sql sau đó dời mọi bản ghi của khung đó về đúng ngày giả.
// Chạy: node demo-data.mjs [BASE] [windows.json]   (BASE mặc định http://localhost:8081/api)
import fs from 'node:fs'

const BASE = process.argv[2] ?? 'http://localhost:8081/api'
const OUT = process.argv[3] ?? 'demo-windows.json'
const DAYS = Number(process.env.DEMO_DAYS ?? 30)
const SCALE = Number(process.env.DEMO_SCALE ?? 1) // nhân số đơn / ngày
const ADMIN_PASS = process.env.DEMO_ADMIN_PASS ?? 'admin123'

// ---- tiện ích ------------------------------------------------------------------------------------
let seed = 20261021
const rnd = () => { seed |= 0; seed = (seed + 0x6D2B79F5) | 0; let t = Math.imul(seed ^ (seed >>> 15), 1 | seed); t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t; return ((t ^ (t >>> 14)) >>> 0) / 4294967296 }
const pick = (a) => a[Math.floor(rnd() * a.length)]
const between = (a, b) => a + Math.floor(rnd() * (b - a + 1))
const chance = (p) => rnd() < p
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const localDate = (daysAgo) => new Date(Date.now() + 7 * 3600e3 - daysAgo * 864e5).toISOString().slice(0, 10)
const T = {}
const stats = { ok: 0, fail: 0, orders: 0, errors: {} }

async function call(method, path, body, token = T.admin, { form, raw } = {}) {
  const headers = {}
  if (token) headers.Authorization = 'Bearer ' + token
  let payload
  if (form) { headers['Content-Type'] = 'application/x-www-form-urlencoded'; payload = form } else if (body !== undefined) { headers['Content-Type'] = 'application/json'; payload = JSON.stringify(body) }
  const r = await fetch(BASE + path, { method, headers, body: payload, redirect: 'manual' })
  if (raw) return r
  const t = await r.text()
  let d = null
  try { d = t ? JSON.parse(t) : null } catch { d = t }
  if (r.status >= 400) throw new Error(`${method} ${path} → ${r.status} ${typeof d === 'object' ? d?.error : d}`)
  return d
}
/** best-effort: a refused step is logged and the run goes on */
async function tryCall(...a) {
  try { const d = await call(...a); stats.ok++; return d } catch (e) {
    stats.fail++
    const k = String(e.message).replace(/\d+/g, '#').slice(0, 120)
    stats.errors[k] = (stats.errors[k] ?? 0) + 1
    return null
  }
}
const login = async (u, p) => (await call('POST', '/auth/login', { username: u, password: p }, null)).token

// ---- dữ liệu nền -----------------------------------------------------------------------------------
const NEW_STAFF = [
  { fullName: 'Nguyễn Minh Thư', position: 'Thu ngân', phone: '0938112401', email: 'thu.nm@coffeeholic.vn', hireDate: '2026-08-01', user: 'thu.nm', pass: 'Thu@2026' },
  { fullName: 'Đặng Quốc Khánh', position: 'Pha chế', phone: '0938112402', email: 'khanh.dq@coffeeholic.vn', hireDate: '2026-07-15', user: 'khanh.dq', pass: 'Khanh@2026' },
  { fullName: 'Bùi Ngọc Ánh', position: 'Phục vụ', phone: '0938112403', hireDate: '2026-09-01', user: 'anh.bn', pass: 'Anh@2026' },
  { fullName: 'Lâm Tuấn Kiệt', position: 'Thu ngân', phone: '0938112404', hireDate: '2026-09-10', user: 'kiet.lt', pass: 'Kiet@2026' },
]
const CUSTOMERS = [
  ['Nguyễn Thị Thu Hà', '0938456712'], ['Trần Văn Nam', '0903221144'], ['Lê Hoàng Phúc', '0912334455'], ['Phạm Ngọc Mai', '0987123321'],
  ['Võ Minh Tuấn', '0909556677'], ['Đỗ Thị Lan Anh', '0977889900'], ['Huỳnh Gia Bảo', '0933445566'], ['Ngô Thanh Trúc', '0918223344'],
  ['Bùi Đức Thắng', '0966554433'], ['Đinh Khánh Linh', '0945667788'], ['Trương Mỹ Duyên', '0901998877'], ['Lý Quốc Huy', '0935112233'],
  ['Phan Thị Hồng', '0989001122'], ['Mai Anh Khoa', '0911002233'], ['Cao Bảo Ngọc', '0938776655'], ['Tạ Minh Châu', '0907665544'],
  ['Vương Thùy Dung', '0968334455'], ['Châu Kim Ngân', '0932556688'], ['Hà Gia Hân', '0979112244'], ['Kiều Văn Lộc', '0908447799'],
]
const ADDRESSES = {
  'Quận 1': ['12 Lê Thánh Tôn, P. Bến Nghé, Quận 1', '45 Nguyễn Huệ, P. Bến Nghé, Quận 1', '88 Pasteur, P. Bến Nghé, Quận 1'],
  'Quận 3': ['58 Võ Văn Tần, P. Võ Thị Sáu, Quận 3', '210 Nam Kỳ Khởi Nghĩa, P. Võ Thị Sáu, Quận 3', '9 Trần Quốc Thảo, P. Võ Thị Sáu, Quận 3'],
  'Phú Nhuận': ['120 Phan Xích Long, P.2, Phú Nhuận', '33 Hoa Lan, P.2, Phú Nhuận'],
  'Bình Thạnh': ['208 Nguyễn Hữu Cảnh, P.22, Bình Thạnh', '15 Đinh Bộ Lĩnh, P.24, Bình Thạnh'],
  'Tân Bình': ['364 Cộng Hòa, P.13, Tân Bình', '18 Trường Sơn, P.2, Tân Bình'],
  'Quận 10': ['285 Cách Mạng Tháng Tám, P.12, Quận 10'],
  'Gò Vấp': ['102 Phan Văn Trị, P.10, Gò Vấp'],
}
const SUPPLIERS = [
  { name: 'Công ty TNHH Cà phê Đất Bazan', contactName: 'Anh Nguyễn Văn Phúc', phone: '0905123789', email: 'sales@datbazan.vn', address: '123 Lê Duẩn, P. Tân Lợi, TP. Buôn Ma Thuột, Đắk Lắk', taxCode: '6001234567', paymentTermDays: 15,
    items: [['Cà phê Robusta rang xay', 230, 'Bao 5 kg', 5000], ['Hạt Arabica', 450, 'Gói 1 kg', 1000]] },
  { name: 'NPP Sữa Hoàng Gia', contactName: 'Chị Trần Thị Mai', phone: '0908456123', address: '45 Cộng Hòa, P.4, Q. Tân Bình, TP.HCM', taxCode: '0312987654', paymentTermDays: 7,
    items: [['Sữa đặc', 59, 'Hộp 1 L', 1000], ['Sữa tươi', 35, 'Hộp 1 L', 1000], ['Whipping cream', 145, 'Hộp 1 L', 1000], ['Cốt dừa', 90, 'Hộp 1 L', 1000]] },
  { name: 'Chợ đầu mối Bình Điền – vựa Cô Sáu', contactName: 'Cô Sáu', phone: '0918334455', address: 'Lô C, chợ Bình Điền, Q.8, TP.HCM', paymentTermDays: 0,
    items: [['Trứng gà', 3500, null, null], ['Cam vàng', 9000, null, null], ['Sả cây', 40, null, null], ['Quả cherry', 180, null, null]] },
  { name: 'Nước đá tinh khiết Phú Mỹ', contactName: 'Anh Tư', phone: '0907223344', address: '56 Lũy Bán Bích, Tân Phú, TP.HCM', paymentTermDays: 0,
    items: [['Đá viên', 2, 'Bao 20 kg', 20000]] },
  { name: 'Đại lý đường Biên Hòa – Tân Định', contactName: 'Chị Hạnh', phone: '0283 8201 456', address: '78 Hai Bà Trưng, P. Tân Định, Q.1, TP.HCM', taxCode: '0301122334', paymentTermDays: 7,
    items: [['Đường', 25, 'Bao 5 kg', 5000], ['Muối', 10, null, null]] },
]
const ZONES = [
  { name: 'Trung tâm', districts: ['Quận 1', 'Quận 3', 'Phú Nhuận'], fee: 15000, freeShipThreshold: 200000, minOrderAmount: 50000, estimatedMinutes: 25, sortOrder: 1 },
  { name: 'Vùng ven', districts: ['Bình Thạnh', 'Tân Bình', 'Quận 10'], fee: 22000, minOrderAmount: 80000, estimatedMinutes: 40, sortOrder: 2 },
  { name: 'Xa', districts: ['Gò Vấp', 'Quận 7', 'Thủ Đức'], fee: 35000, minOrderAmount: 150000, estimatedMinutes: 55, sortOrder: 3 },
]

// ---- khung thời gian -------------------------------------------------------------------------------
const windows = []
let cur = null
const openWindow = (daysAgo) => { cur = { daysAgo, date: localDate(daysAgo), realStart: new Date().toISOString() } }
const closeWindow = async () => { await sleep(1500); cur.realEnd = new Date().toISOString(); windows.push(cur); fs.writeFileSync(OUT, JSON.stringify(windows, null, 1)); await sleep(1500) }

// ---- chạy ----------------------------------------------------------------------------------------------
T.admin = await login('admin', ADMIN_PASS)
const day0 = DAYS - 1
openWindow(day0)
console.log('Thiết lập dữ liệu nền…')

// nhân viên + tài khoản
const staffList = await call('GET', '/staff')
const cashiers = []
for (const s of NEW_STAFF) {
  let st = staffList.find((x) => x.phone === s.phone)
  st ??= await tryCall('POST', '/staff', { fullName: s.fullName, position: s.position, phone: s.phone, email: s.email, hireDate: s.hireDate, workStatus: 'ACTIVE' })
  if (!st) continue
  s.id = st.id
  await tryCall('POST', '/users', { username: s.user, password: s.pass, fullName: s.fullName, phone: s.phone, active: true, roles: ['STAFF'], staffId: st.id })
  const tok = await login(s.user, s.pass).catch(() => null)
  if (tok && s.position === 'Thu ngân') cashiers.push({ ...s, token: tok })
}
if (!cashiers.length) cashiers.push({ fullName: 'Quản lý', token: T.admin })
const allStaff = (await call('GET', '/staff')).filter((s) => s.workStatus !== 'RESIGNED')

// ca mẫu + lịch ca từ hôm nay tới tuần sau (ngày đã qua không xếp được: chấm công các ngày trước do demo-remap.sql sinh)
let shifts = await call('GET', '/work-shifts')
for (const [name, a, b, color] of [['Ca sáng', '06:30', '12:30', '#f59e0b'], ['Ca chiều', '12:30', '18:30', '#3b82f6'], ['Ca tối', '18:00', '22:30', '#8b5cf6']]) {
  if (!shifts.some((s) => s.name === name)) await tryCall('POST', '/work-shifts', { name, startTime: a, endTime: b, graceMinutes: 5, color })
}
shifts = await call('GET', '/work-shifts')
const SH = Object.fromEntries(shifts.map((s) => [s.name, s]))
const dates = Array.from({ length: 7 }, (_, i) => localDate(-i))
for (const [i, st] of allStaff.entries()) {
  const shiftName = ['Ca sáng', 'Ca chiều', 'Ca tối'][i % 3]
  const off = (i * 2) % 7 // mỗi người nghỉ 1 ngày / tuần
  const myDates = dates.filter((d) => new Date(d).getDay() !== off)
  if (SH[shiftName]) await tryCall('POST', '/shift-assignments', { staffIds: [st.id], dates: myDates, shiftId: SH[shiftName].id, position: st.position })
}

// bàn, khu vực giao hàng
const tables = await call('GET', '/tables')
for (const [no, area, cap] of [['B07', 'Lầu 1', 4], ['B08', 'Lầu 1', 6], ['B09', 'Sân vườn', 2], ['B10', 'Sân vườn', 4]]) {
  if (!tables.some((t) => t.tableNo === no)) await tryCall('POST', '/tables', { tableNo: no, area, capacity: cap, status: 'AVAILABLE' })
}
const zones = await call('GET', '/delivery-zones')
for (const z of ZONES) if (!zones.some((x) => x.name === z.name)) await tryCall('POST', '/delivery-zones', { ...z, active: true })

// quy cách đóng gói + nhà cung cấp
let mats = await call('GET', '/materials')
const M = (n) => mats.find((m) => m.name === n)
for (const s of SUPPLIERS) for (const [mn, , pk, size] of s.items) {
  const m = M(mn)
  if (!m || !pk || m.kind !== 'RAW' || m.packages?.some((p) => p.name === pk)) continue
  await tryCall('PUT', `/materials/${m.id}`, { name: m.name, unit: m.unit, minStock: m.minStock, expiryWarningDays: m.expiryWarningDays, status: m.status, kind: m.kind,
    packages: [...(m.packages ?? []).map((p) => ({ name: p.name, size: p.size })), { name: pk, size }] })
}
mats = await call('GET', '/materials')
const suppliers = await call('GET', '/suppliers')
for (const s of SUPPLIERS) {
  let sup = suppliers.find((x) => x.name === s.name)
  sup ??= await tryCall('POST', '/suppliers', { name: s.name, contactName: s.contactName, phone: s.phone, email: s.email, address: s.address, taxCode: s.taxCode, paymentTermDays: s.paymentTermDays,
    materials: s.items.filter(([mn]) => M(mn)).map(([mn, price]) => ({ materialId: M(mn).id, unitPrice: price })) })
  s.id = sup?.id
}
const pkgId = (m, name) => m?.packages?.find((p) => p.name === name)?.id

// mẫu tin ZNS
await tryCall('PUT', '/notifications/templates/ORDER_CONFIRMED', { znsTemplateId: '298765', smsFallback: true })
await tryCall('PUT', '/notifications/templates/ORDER_COMPLETED', { znsTemplateId: '298771', smsFallback: true })

// ---- nhập hàng ---------------------------------------------------------------------------------------
const receipts = [] // { id, supplierId, total }
/** one purchase: phiếu đặt → gửi → nhận (đủ hoặc thiếu) */
async function purchase(sup, scale, { partial = false, reject = false } = {}) {
  if (!sup.id) return
  const lines = sup.items.filter(([mn]) => M(mn)).map(([mn, price, pk, size]) => {
    const m = M(mn)
    const pId = pkgId(m, pk)
    const base = { 'Cà phê Robusta rang xay': 1, 'Hạt Arabica': 2, 'Sữa đặc': 4, 'Sữa tươi': 10, 'Whipping cream': 2, 'Cốt dừa': 2, 'Đá viên': 1, 'Đường': 1 }[mn]
    return pId ? { materialId: m.id, packageId: pId, packageCount: Math.max(1, Math.round((base ?? 2) * scale)), unitPrice: price }
      : { materialId: m.id, quantity: Math.round(({ 'Trứng gà': 30, 'Cam vàng': 20, 'Sả cây': 1500, 'Quả cherry': 1000, 'Muối': 1000 }[mn] ?? 1000) * scale), unitPrice: price }
  })
  const po = await tryCall('POST', '/purchase-orders', { supplierId: sup.id, expectedDate: localDate(0), vatRate: sup.taxCode ? 8 : undefined, note: chance(0.3) ? 'Giao trước 9h sáng' : undefined, items: lines })
  if (!po) return
  await tryCall('POST', `/purchase-orders/${po.id}/send`)
  const full = await tryCall('GET', `/purchase-orders/${po.id}`)
  if (!full) return
  const shelf = { 'Sữa tươi': 10, 'Whipping cream': 45, 'Sữa đặc': 180, 'Cốt dừa': 60, 'Trứng gà': 14, 'Cam vàng': 10, 'Sả cây': 10, 'Quả cherry': 7, 'Đá viên': 3 }
  const rlines = full.items.map((it, k) => {
    const doc = it.packageCount ?? it.quantity
    let got = doc
    let rej = 0
    if (partial && k === full.items.length - 1) got = Math.max(1, Math.floor(doc / 2))
    if (reject && k === 0 && doc > 2) { rej = 1; got = doc - 1 }
    return { itemId: it.id, documentCount: doc, receivedCount: got, rejectedCount: rej || undefined, rejectReason: rej ? 'Hộp móp, rỉ khi kiểm nhận' : undefined,
      expiryDate: localDate(-(shelf[it.materialName] ?? 365)), temperature: it.materialName.includes('Sữa') || it.materialName.includes('Whipping') ? 5 : undefined }
  })
  const gr = await tryCall('POST', `/purchase-orders/${po.id}/receipts`, { delivererName: sup.contactName, invoiceSeries: sup.taxCode ? '1C26T' + sup.name.slice(-3).toUpperCase().replace(/[^A-Z]/g, 'X') : undefined,
    invoiceNo: sup.taxCode ? String(between(1000, 99999)).padStart(7, '0') : undefined, invoiceDate: sup.taxCode ? localDate(0) : undefined, vatRate: sup.taxCode ? 8 : undefined,
    lines: rlines })
  if (gr) receipts.push({ id: gr.id, supplierId: sup.id, total: gr.totalAmount, remaining: gr.remainingAmount, lines: gr.lines })
  if (partial) await tryCall('POST', `/purchase-orders/${po.id}/close`, { reason: 'NCC báo thiếu hàng, giao bù đợt sau' })
}
/** NCC trả ngay (chợ, đá): nhập rồi lập phiếu chi tiền mặt đúng số tiền phiếu nhập */
async function purchaseSafe(sup, scale, opt) {
  if (sup.paymentTermDays) return purchase(sup, scale, opt)
  const before = receipts.length
  await purchase(sup, scale, opt)
  const r = receipts[receipts.length - 1]
  if (receipts.length > before && r) await tryCall('POST', '/supplier-payments', { supplierId: sup.id, amount: r.total, method: 'CASH', payeeName: sup.contactName, goodsReceiptId: r.id, note: 'Trả tiền mặt khi nhận hàng' })
}

// ---- sơ chế ------------------------------------------------------------------------------------------
async function produceAll() {
  mats = await call('GET', '/materials')
  for (const m of mats.filter((x) => x.kind === 'PREPARED' && x.status === 'ACTIVE')) {
    const target = Math.max(Number(m.minStock ?? 0) * 2, 1500)
    if (Number(m.stockQuantity) >= target) continue
    const batches = Math.min(4, Math.max(1, Math.ceil((target - Number(m.stockQuantity)) / Number(m.yieldQuantity || 1000))))
    await tryCall('POST', '/inventory/produce', { materialId: m.id, batches, note: 'Sơ chế đầu ca' })
  }
}

// ---- bán hàng ----------------------------------------------------------------------------------------
const tablesNow = await call('GET', '/tables')
const pubCart = async (tableQr) => call('POST', '/public/carts', { tableQr }, null)
async function menuAvail() {
  const menu = await call('GET', '/public/menu', undefined, null)
  return (menu.categories ?? []).flatMap((c) => c.coffees ?? c.items ?? []).filter((c) => c.available !== false && (c.status ?? 'AVAILABLE') === 'AVAILABLE')
}
const lines = (menu) => {
  const n = between(1, 3)
  const used = new Set()
  return Array.from({ length: n }, () => pick(menu)).filter((c) => !used.has(c.id) && used.add(c.id))
    .map((c) => ({ coffeeId: c.id, quantity: chance(0.7) ? 1 : between(2, 3), note: chance(0.2) ? pick(['Ít đá', 'Ít ngọt', 'Không đá', 'Đá riêng', 'Ít đá, ít ngọt']) : undefined }))
}
async function vnpayPay(code) {
  const link = await tryCall('POST', `/public/orders/${code}/pay`, undefined, null)
  if (!link) return false
  const q = new URL(link.paymentUrl).searchParams
  const r = await call('POST', `/public/payments/sandbox/complete?result=${chance(0.05) ? '24' : '00'}`, undefined, null, { form: q, raw: true }).catch(() => null)
  return r?.status === 302
}
async function finish(o, tok, { delivery, collect }) {
  if (!o) return
  if (o.status === 'PENDING' && !(await tryCall('POST', `/orders/${o.id}/confirm`, undefined, tok))) return
  if (!(await tryCall('POST', `/orders/${o.id}/ready`, undefined, tok))) return
  if (delivery) {
    const ord = await tryCall('GET', `/orders/${o.id}`, undefined, tok)
    const sh = ord?.shipment
    if (!sh) return
    if (chance(0.6)) {
      await tryCall('GET', `/shipments/${sh.id}/ahamove/estimate`, undefined, tok)
      const b = await tryCall('POST', `/shipments/${sh.id}/ahamove/book`, undefined, tok)
      const pid = b?.partnerOrderId
      if (!pid) return
      const driver = pick(['Trần Văn Tài', 'Nguyễn Hữu Lộc', 'Phạm Minh Đức', 'Lê Quang Vinh', 'Huỳnh Tấn Phát'])
      for (const st of ['ACCEPTED', 'IN PROCESS', 'COMPLETED']) {
        await tryCall('POST', '/public/delivery/ahamove/webhook?token=coffeeholic-webhook', { _id: pid, status: st, supplier_name: driver, supplier_id: '849' + between(10000000, 99999999), path: [{ status: st === 'COMPLETED' ? 'COMPLETED' : 'ASSIGNING' }] }, null)
      }
    } else {
      const carrier = pick(['Grab', 'Be', 'Shopee Food', 'Xe quán'])
      await tryCall('PATCH', `/shipments/${sh.id}`, { status: 'DELIVERING', carrier, trackingCode: carrier === 'Xe quán' ? undefined : carrier.slice(0, 3).toUpperCase() + '-' + between(10000, 99999) }, tok)
      await tryCall('PATCH', `/shipments/${sh.id}`, { status: 'DELIVERED', carrier }, tok)
    }
    return
  }
  if (collect) await tryCall('POST', `/orders/${o.id}/collect-payment`, undefined, tok)
  await tryCall('POST', `/orders/${o.id}/complete`, undefined, tok)
}
const completedOnline = []

async function sellOne(daysAgo, tok, menu, leaveOpen) {
  const roll = rnd()
  const cust = chance(0.55) ? pick(CUSTOMERS) : null
  const items = lines(menu)
  if (!items.length) return
  stats.orders++
  if (roll < 0.45) {
    // tại quầy
    const type = chance(0.55) ? 'TAKE_AWAY' : 'DINE_IN'
    const method = pick(['CASH', 'CASH', 'CASH', 'BANK_TRANSFER', 'CARD', 'E_WALLET'])
    const o = await tryCall('POST', '/orders', { items, orderType: type, tableId: type === 'DINE_IN' ? pick(tablesNow).id : undefined, customerName: cust?.[0], customerPhone: cust?.[1], paymentMethod: method, paidNow: method !== 'CASH' || chance(0.8) }, tok)
    if (!o || leaveOpen) return
    if (chance(0.03)) { await tryCall('POST', `/orders/${o.id}/cancel`, { reason: pick(['Khách đổi ý', 'Pha nhầm món, làm lại đơn mới']) }, tok); return }
    await finish(o, tok, { collect: o.payments.some((p) => p.status === 'PENDING') })
    if (chance(0.03)) {
      const ord = await tryCall('GET', `/orders/${o.id}`, undefined, tok)
      const pay = ord?.payments?.find((p) => p.status === 'PAID')
      if (pay) await tryCall('POST', `/payments/${pay.id}/refund`, { amount: ord.items[0].unitPrice }, T.admin)
    }
    return
  }
  if (roll < 0.70) {
    // QR tại bàn
    const t = pick(tablesNow)
    const cart = await tryCall('POST', '/public/carts', { tableQr: t.qrCode }, null)
    if (!cart) return
    for (const it of items) await tryCall('POST', `/public/carts/${cart.sessionCode}/items`, it, null)
    const online = chance(0.35)
    const o = await tryCall('POST', '/public/checkout', { sessionCode: cart.sessionCode, orderType: 'DINE_IN', customerName: cust?.[0] ?? `Khách bàn ${t.tableNo}`, customerPhone: cust?.[1], paymentMethod: online ? 'E_WALLET' : 'CASH' }, null)
    if (!o) return
    if (online) await vnpayPay(o.orderCode)
    if (leaveOpen) return
    if (chance(0.04)) { await tryCall('POST', `/public/orders/${o.orderCode}/cancel`, { reason: 'Khách đổi ý' }, null); return }
    await finish(o, tok, { collect: !online })
    return
  }
  // online: mang đi hoặc giao hàng
  const delivery = chance(0.6)
  const c = cust ?? pick(CUSTOMERS)
  const cart = await tryCall('POST', '/public/carts', {}, null)
  if (!cart) return
  for (const it of items) await tryCall('POST', `/public/carts/${cart.sessionCode}/items`, { ...it, quantity: it.quantity + (delivery ? 1 : 0) }, null)
  const district = pick(Object.keys(ADDRESSES))
  const method = pick(['BANK_TRANSFER', 'E_WALLET', 'CARD', 'CASH'])
  const o = await tryCall('POST', '/public/checkout', delivery
    ? { sessionCode: cart.sessionCode, orderType: 'DELIVERY', customerName: c[0], customerPhone: c[1], deliveryDistrict: district,
      delivery: { recipientName: c[0], recipientPhone: c[1], address: pick(ADDRESSES[district]) }, paymentMethod: method }
    : { sessionCode: cart.sessionCode, orderType: 'TAKE_AWAY', customerName: c[0], customerPhone: c[1], paymentMethod: method }, null)
  if (!o) return
  const paidOnline = method !== 'CASH' && (await vnpayPay(o.orderCode))
  if (method !== 'CASH' && !paidOnline) return // giao dịch lỗi / khách hủy: đơn chờ thanh toán, tự hủy sau 15 phút
  if (leaveOpen) return
  if (chance(0.04)) {
    await tryCall('POST', `/orders/${o.id}/reject`, { reason: pick(['Hết nguyên liệu cho món này', 'Quán quá tải, không kịp giao']) }, tok)
    return
  }
  await finish(o, tok, { delivery, collect: method === 'CASH' && !delivery })
  completedOnline.push({ id: o.id, code: o.orderCode, phone: c[1], name: c[0] })
}

// ---- vòng lặp 30 ngày ----------------------------------------------------------------------------------
for (let d = day0; d >= 0; d--) {
  if (d !== day0) openWindow(d)
  const date = localDate(d)
  const weekday = new Date(date).getDay()
  const weekend = weekday === 0 || weekday === 6
  console.log(`Ngày ${date} (${d} ngày trước)…`)
  const cashier = cashiers[d % cashiers.length]
  const tok = cashier.token

  // nhập hàng: ngày đầu nhập đủ cho tuần, sau đó mỗi tuần; đá mỗi 2 ngày, chợ mỗi 3 ngày
  if (d === day0 || d % 7 === 2) {
    await purchaseSafe(SUPPLIERS[0], d === day0 ? 1.2 : 1)
    await purchaseSafe(SUPPLIERS[1], d === day0 ? 1 : 0.8, { partial: d % 14 === 2, reject: d % 21 === 2 })
    await purchaseSafe(SUPPLIERS[4], 1)
  }
  if (d % 2 === day0 % 2) await purchaseSafe(SUPPLIERS[3], 1)
  if (d % 3 === 0 || d === day0) await purchaseSafe(SUPPLIERS[2], 1)
  if (d % 3 === 1 && d !== day0) await purchaseSafe(SUPPLIERS[1], 0.5) // sữa tươi bổ sung

  await produceAll()
  if (d === day0) {
    // mở bán lại các món đang "Hết món" sau khi đã có hàng
    await sleep(2000)
    for (const c of await call('GET', '/coffees')) if (c.status === 'SOLD_OUT') await tryCall('PATCH', `/coffees/${c.id}/status`, { status: 'AVAILABLE' })
    await sleep(3000)
  }

  // mở ca thu ngân
  const opening = await tryCall('GET', '/cash-shifts/suggested-opening', undefined, tok)
  await tryCall('POST', '/cash-shifts/open', { openingCash: opening?.openingCash || 1500000, note: weekend ? 'Ca cuối tuần' : undefined }, tok)

  // bán hàng
  let menu = await menuAvail()
  const n = Math.round((weekend ? between(13, 17) : between(8, 12)) * SCALE)
  for (let i = 0; i < n; i++) {
    if (i % 5 === 4) menu = await menuAvail()
    if (!menu.length) { await produceAll(); menu = await menuAvail(); if (!menu.length) break }
    await sellOne(d, tok, menu, d === 0 && i >= n - 3)
    await sleep(between(1500, 4500)) // giãn đơn ra để khi dời về ngày giả, đơn rải từ sáng tới tối
    if (i === Math.floor(n / 2) && chance(0.6)) {
      await tryCall('POST', '/cash-shifts/current/movements', pick([
        { type: 'OUT', amount: 40000, reason: 'Mua 2 bao đá 20kg' }, { type: 'OUT', amount: 25000, reason: 'Trả phí gửi xe giao hàng' },
        { type: 'IN', amount: 200000, reason: 'Đổi tiền lẻ từ quản lý' }, { type: 'OUT', amount: 60000, reason: 'Mua chanh, sả ngoài chợ' }]), tok)
    }
  }

  // sự cố + đền bù
  if (d % 4 === 1 && completedOnline.length) {
    const o = completedOnline[completedOnline.length - 1]
    const inc = await tryCall('POST', `/orders/${o.id}/incidents`, pick([
      { type: 'LATE', severity: 'MEDIUM', reason: 'Giao trễ hơn 20 phút', description: 'Khách gọi phàn nàn' },
      { type: 'SPILLED', severity: 'HIGH', reason: 'Ly bị đổ khi giao', description: 'Tài xế báo nắp ly bung' },
      { type: 'WRONG_ITEM', severity: 'MEDIUM', reason: 'Giao nhầm Bạc xỉu thành Cà phê sữa' },
      { type: 'QUALITY', severity: 'LOW', reason: 'Khách chê hơi ngọt' }]), tok)
    if (inc && d > 0) await tryCall('POST', `/incidents/${inc.id}/resolve`, { resolution: 'Gọi xin lỗi khách, tặng voucher lần sau', voucherValue: pick([15000, 20000, 30000]), voucherMinOrderValue: 50000, voucherCount: 1, voucherValidDays: 30 })
  }

  // hóa đơn công ty
  if (d % 3 === 0 && completedOnline.length) {
    const o = pick(completedOnline.slice(-6))
    await tryCall('POST', `/public/orders/${o.code}/invoice-request`, pick([
      { buyerTaxCode: '0316789012', buyerCompany: 'Công ty TNHH Giải pháp Số Thu Hà', buyerAddress: '58 Võ Văn Tần, P. Võ Thị Sáu, Q.3, TP.HCM', buyerName: o.name, buyerEmail: 'ketoan@thuha-digital.vn' },
      { buyerTaxCode: '0312345678', buyerCompany: 'Công ty CP Truyền thông Sao Việt', buyerAddress: '12 Lê Thánh Tôn, P. Bến Nghé, Q.1, TP.HCM', buyerName: o.name, buyerEmail: 'invoice@saoviet.vn' },
      { buyerName: o.name, buyerIdNumber: '079' + between(100000000, 999999999), buyerEmail: 'khachle' + between(10, 99) + '@gmail.com' }]), null)
  }
  await tryCall('POST', '/invoices/issue-now')

  // kho: hủy hàng, kiểm kê
  if (d % 4 === 2) {
    mats = await call('GET', '/materials')
    const milk = M('Sữa tươi')
    const lots = milk ? await tryCall('GET', `/materials/${milk.id}/batches`) : null
    const lot = lots?.find((b) => b.status === 'AVAILABLE' && Number(b.remainingQuantity) > 600)
    if (lot) await tryCall('POST', '/inventory/dispose', { batchId: lot.id, quantity: between(2, 5) * 100, reason: pick(['SPILLED', 'SPOILED']), note: pick(['Đổ khi đánh sữa', 'Sữa có mùi chua, hủy']) })
  }
  if (d % 7 === 5) {
    mats = await call('GET', '/materials')
    const ids = ['Cà phê Robusta rang xay', 'Sữa đặc', 'Đường', 'Hạt Arabica'].map((n) => M(n)?.id).filter(Boolean)
    const st = await tryCall('POST', '/stocktakes', { members: 'Hồ Tiến Việt, Nguyễn Minh Thư', materialIds: ids, note: 'Kiểm kê định kỳ cuối tuần' })
    if (st) {
      const counts = st.items.map((it, k) => ({ itemId: it.id, countedQuantity: Math.max(0, Number(it.bookQuantity) - (k === 0 ? between(50, 200) : 0)), quality: 'GOOD', reason: k === 0 ? 'Hao hụt khi xay, rơi vãi' : undefined }))
      await tryCall('PUT', `/stocktakes/${st.id}`, { members: 'Hồ Tiến Việt, Nguyễn Minh Thư', counts })
      await tryCall('POST', `/stocktakes/${st.id}/balance`, { resolution: 'Ghi nhận hao hụt trong định mức' })
    }
  }

  // trả hàng NCC + trả nợ NCC
  if (d % 10 === 3) {
    const r = receipts.filter((x) => x.supplierId === SUPPLIERS[1].id).at(-1)
    if (r?.lines?.length) await tryCall('POST', '/supplier-returns', { goodsReceiptId: r.id, reason: 'Hàng cận date / móp méo', settlement: 'DEBT', complete: true, adjustmentInvoiceNo: String(between(10000, 99999)), items: [{ receiptItemId: r.lines[0].id, count: 1 }] })
  }
  if (d % 7 === 0 && d !== day0) {
    const debts = await tryCall('GET', '/supplier-debts')
    for (const dbt of (debts ?? []).filter((x) => x.balance > 0 && SUPPLIERS.some((s) => s.id === x.supplierId))) {
      await tryCall('POST', '/supplier-payments', { supplierId: dbt.supplierId, amount: Math.round(dbt.balance * pick([0.5, 1])), method: 'BANK_TRANSFER', reference: 'FT26' + between(10000000, 99999999), payeeName: 'Kế toán NCC', note: 'Thanh toán công nợ định kỳ' })
    }
  }

  // giá vốn, đối soát
  if (d % 7 === 4) await tryCall('POST', '/coffee-costs/recalculate')
  if (d > 0) {
    const tx = await tryCall('GET', `/payment-transactions?from=${localDate(0)}&to=${localDate(0)}`)
    for (const t of (tx ?? []).filter((x) => x.status === 'SUCCESS' && !x.reconciled).slice(0, 8)) await tryCall('POST', `/payment-transactions/${t.id}/reconcile`, { note: 'Khớp file đối soát VNPAY' })
  }

  // chốt ca (hôm nay để ca mở)
  if (d > 0) {
    const cs = await tryCall('GET', '/cash-shifts/current', undefined, tok)
    if (cs?.expectedCash != null) {
      let rest = Number(cs.expectedCash) - (chance(0.15) ? pick([10000, 20000, 5000]) : 0)
      const den = {}
      for (const v of [500000, 200000, 100000, 50000, 20000, 10000, 5000, 2000, 1000]) { const k = Math.floor(rest / v); if (k) den[v] = k; rest -= k * v }
      const counted = Object.entries(den).reduce((s, [v, k]) => s + Number(v) * k, 0)
      await tryCall('POST', '/cash-shifts/current/close', { denominations: den, nextOpeningCash: 1500000, receivedBy: 'Hồ Tiến Việt (quản lý)', differenceReason: counted !== Number(cs.expectedCash) ? 'Thối nhầm tiền lẻ cho khách' : undefined, note: `Chốt ca ${date}` }, tok)
    }
  }
  await sleep(11000) // để job gửi tin / phát hành hóa đơn xử lý trong khung của ngày
  await closeWindow()
}

console.log(JSON.stringify({ windows: windows.length, ...stats, errors: Object.entries(stats.errors).sort((a, b) => b[1] - a[1]).slice(0, 15) }, null, 1))
