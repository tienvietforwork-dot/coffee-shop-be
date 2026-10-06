// Build everything: draw.io files, PNG previews, and the two PDF deliverables.
//   node docs/phan-tich/_tools/build.js            -> all
//   node docs/phan-tich/_tools/build.js png UC07   -> only re-render PNG previews whose file name starts with UC07
//   node docs/phan-tich/_tools/build.js nopng      -> files + PDFs, no PNG previews
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const usecases = require('./usecases');
const bpmn = require('./bpmn');
const { dbDiagram, erdLogical, architecture } = require('./design');
const { esc } = require('./lib');

const ROOT = path.resolve(__dirname, '..');
const D11 = path.join(ROOT, 'giai-doan-1.1_usecase-bpmn');
const D12 = path.join(ROOT, 'giai-doan-1.2_thiet-ke');
const TMP = path.join(__dirname, '.tmp');
const EDGE = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const VIEWER = 'https://viewer.diagrams.net/js/viewer-static.min.js';
fs.mkdirSync(TMP, { recursive: true });

const [mode, only] = process.argv.slice(2);

(async () => {
const uc = usecases(path.join(D11, 'usecase'));
const bp = bpmn(path.join(D11, 'bpmn'));
const erdL = erdLogical(path.join(D12, 'erd'));
const db = [await dbDiagram(path.join(D12, 'database'))];
const arch = architecture(path.join(D12, 'kien-truc'));
const all = [...uc, ...bp, erdL, ...db, arch];
console.log(`generated ${all.length} diagrams`);
for (const u of uc) if (u.ucCount + u.actorCount > 15) throw new Error(u.file + ' > 15');

const xmlOf = r => fs.readFileSync(path.join(r.dir, r.file + '.drawio'), 'utf8');
const viewerDiv = (r, zoom, w, h) => {
  const cfg = { xml: xmlOf(r), lightbox: false, nav: false, resize: false, toolbar: '', border: 8, zoom };
  return `<div class="mxgraph" style="width:${Math.ceil(w)}px;height:${Math.ceil(h)}px" data-mxgraph="${esc(JSON.stringify(cfg))}"></div>`;
};
const edge = args => execFileSync(EDGE, ['--headless=new', '--disable-gpu', '--hide-scrollbars', '--allow-file-access-from-files', '--virtual-time-budget=40000', ...args], { stdio: 'ignore', timeout: 300000 });
const fileUrl = p => 'file:///' + p.split(path.sep).join('/');
const caPhe = s => s.replace(/sản phẩm/g, 'cà phê').replace(/Sản phẩm/g, 'Cà phê');

// ---------- PNG previews ----------
function renderPng(r) {
  const w = Math.ceil(r.b.x1 + 40), h = Math.ceil(r.b.y1 + 40);
  const html = `<!doctype html><meta charset="utf-8"><style>html,body{margin:0;background:#fff}</style>${viewerDiv(r, 1, w, h)}<script src="${VIEWER}"></script>`;
  const hp = path.join(TMP, r.file + '.html');
  fs.writeFileSync(hp, html, 'utf8');
  const out = path.join(r.dir, 'png', r.file + '.png');
  fs.mkdirSync(path.dirname(out), { recursive: true });
  edge([`--window-size=${w},${h}`, '--force-device-scale-factor=' + (w * h > 5e6 ? 1.3 : 2), `--screenshot=${out}`, fileUrl(hp)]);
  return out;
}
if (mode === 'png') {
  all.filter(r => !only || r.file.startsWith(only)).forEach(r => console.log('png', renderPng(r)));
  process.exit(0);
}

// ---------- PDF ----------
// A3 landscape, 12mm margins -> usable ~1497 x 1032 CSS px
const PAGE_W = 1490, PAGE_H = 1020;
const CSS = `
@page { size: A3 landscape; margin: 12mm; }
* { box-sizing: border-box; }
body { margin: 0; font-family: "Segoe UI", Arial, sans-serif; color: #1a1a1a; }
.page { width: ${PAGE_W}px; height: ${PAGE_H}px; page-break-after: always; display: flex; flex-direction: column; overflow: hidden; }
.page:last-child { page-break-after: auto; }
h1 { font-size: 40px; margin: 0 0 16px; }
h2 { font-size: 24px; margin: 0 0 6px; }
h3 { font-size: 18px; margin: 0 0 6px; color: #444; font-weight: 600; }
.sub { color: #555; font-size: 15px; margin: 0 0 10px; }
.row { flex: 1; display: flex; gap: 14px; min-height: 0; }
.dia { flex: 1; display: flex; align-items: center; justify-content: center; min-height: 0; }
.side { width: 440px; overflow: hidden; }
table { border-collapse: collapse; width: 100%; font-size: 14px; }
th, td { border: 1px solid #bbb; padding: 6px 8px; text-align: left; vertical-align: top; }
th { background: #eef2f7; }
td.n { width: 48px; text-align: center; }
td.l { width: 230px; white-space: nowrap; }
td.na { color: #8a5a00; font-style: italic; }
.cover { justify-content: center; padding: 0 80px; }
.cover .meta { font-size: 20px; line-height: 1.7; color: #333; }
.toc { columns: 3; font-size: 14px; line-height: 1.55; }
.small td, .small th { font-size: 12px; padding: 3px 6px; }
.tiny td, .tiny th { font-size: 11px; padding: 2px 5px; }
`;
function fitZoom(r, maxW, maxH) { const w = r.b.x1 + 40, h = r.b.y1 + 40; return { w, h, z: Math.min(maxW / w, maxH / h, 1.3) }; }
function diagramPage(r, heading, sub = '', side = '') {
  const headH = (heading ? 44 : 0) + (sub ? 30 : 0);
  const maxW = side ? PAGE_W - 460 : PAGE_W - 10;
  const { w, h, z } = fitZoom(r, maxW, PAGE_H - headH - 10);
  return `<section class="page">${heading ? `<h2>${heading}</h2>` : ''}${sub ? `<div class="sub">${sub}</div>` : ''}<div class="row"><div class="dia">${viewerDiv(r, z, w * z, h * z)}</div>${side ? `<div class="side">${side}</div>` : ''}</div></section>`;
}
function stepsPage(r) {
  const rows = r.steps.map((s, i) => `<tr><td class="n">${i + 1}</td><td class="l">${s[0]}</td><td>${s[1]}</td></tr>`).join('');
  return `<section class="page"><h2>${r.title} – Mô tả các bước</h2><table><tr><th>Bước</th><th>Lane (vai trò)</th><th>Mô tả</th></tr>${rows}</table></section>`;
}
function printPdf(name, body, outFile) {
  const hp = path.join(TMP, name + '.html');
  fs.writeFileSync(hp, `<!doctype html><html lang="vi"><head><meta charset="utf-8"><title>${name}</title><style>${CSS}</style></head><body>${body}<script src="${VIEWER}"></script></body></html>`, 'utf8');
  edge(['--no-pdf-header-footer', `--print-to-pdf=${outFile}`, fileUrl(hp)]);
  console.log('pdf', outFile);
}
const COVER = (title, subtitle, items) => `<section class="page cover"><h1>${title}</h1><div class="meta">
<b>Dự án:</b> Xây dựng website bán cà phê Coffeeholic<br><b>Nhóm:</b> FA26_ORT_Coffeeholic<br><b>Nội dung:</b> ${subtitle}</div>
<h3 style="margin-top:36px">Mục lục</h3><div class="toc">${items.map(i => `• ${i}`).join('<br>')}</div></section>`;

// ----- File 1: Use case + BPMN (chỉ sơ đồ; BPMN kèm mô tả các bước) -----
const diagramOnly = r => diagramPage(r, '', '');
function bpmnPage(r) {
  const rows = r.steps.map((st, i) => `<tr><td class="n">${i + 1}</td><td class="l">${st[0]}</td><td>${st[1]}</td></tr>`).join('');
  const table = `<table class="small"><tr><th>Bước</th><th>Vai trò</th><th>Mô tả các bước thực hiện</th></tr>${rows}</table>`;
  const tableH = 30 + r.steps.length * 25;
  const { w, h, z } = fitZoom(r, PAGE_W - 10, PAGE_H - tableH - 20);
  return `<section class="page"><div class="dia">${viewerDiv(r, z, w * z, h * z)}</div>${table}</section>`;
}
// File 1 đã nộp: chỉ build lại khi chạy 'build.js file1'
if (mode === 'file1') printPdf('GiaiDoan1.1', [...uc.map(diagramOnly), ...bp.map(bpmnPage)].join('\n'), path.join(D11, 'Coffeeholic_GiaiDoan1.1_UseCase_BPMN.pdf'));

// ----- File 2: ERD Conceptual (bản gốc của nhóm) + ERD Logical (bản của nhóm, bổ sung các ý đã thống nhất) + database diagram + kiến trúc -----
const imgPage = f => `<section class="page"><div class="dia"><img src="${fileUrl(path.join(D12, 'erd', f))}"></div></section>`;
printPdf('GiaiDoan1.2', [imgPage('ERD_Conceptual.png'), ...[erdL, ...db, arch].map(diagramOnly)].join('\n'), path.join(D12, 'Coffeeholic_GiaiDoan1.2_ThietKe.pdf'));

if (mode !== 'nopng') all.forEach(r => renderPng(r));
console.log('done');
})().catch(e => { console.error(e); process.exit(1); });
