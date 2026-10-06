// Shared draw.io helpers for Coffeeholic analysis & design diagrams.
const fs = require('fs');
const path = require('path');

const SYSTEM = 'Hệ thống website bán cà phê Coffeeholic';

function esc(s) {
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/\n/g, '&#xa;');
}

class Diagram {
  constructor(file, name) {
    this.file = file; this.name = name; this.cells = []; this.n = 2; this.geo = {};
    this.b = { x0: Infinity, y0: Infinity, x1: -Infinity, y1: -Infinity };
  }
  grow(x, y, w, h) {
    this.b.x0 = Math.min(this.b.x0, x); this.b.y0 = Math.min(this.b.y0, y);
    this.b.x1 = Math.max(this.b.x1, x + w); this.b.y1 = Math.max(this.b.y1, y + h);
  }
  // x,y are relative to parent; abs is the absolute top-left (for bounds + routing)
  v(label, style, x, y, w, h, parent = '1', abs = null) {
    const id = 'c' + (this.n++);
    this.cells.push(`<mxCell id="${id}" value="${esc(label)}" style="${style}" vertex="1" parent="${parent}"><mxGeometry x="${x}" y="${y}" width="${w}" height="${h}" as="geometry"/></mxCell>`);
    const a = abs || [x, y];
    this.grow(a[0], a[1], w, h);
    this.geo[id] = { x: a[0], y: a[1], w, h };
    return id;
  }
  e(src, tgt, label, style, pts) {
    const id = 'c' + (this.n++);
    const p = pts && pts.length ? `<Array as="points">${pts.map(([x, y]) => `<mxPoint x="${x}" y="${y}"/>`).join('')}</Array>` : '';
    this.cells.push(`<mxCell id="${id}" value="${esc(label || '')}" style="${style}" edge="1" parent="1" source="${src}" target="${tgt}"><mxGeometry relative="1" as="geometry">${p}</mxGeometry></mxCell>`);
    return id;
  }
  xml() {
    const w = Math.ceil(this.b.x1 + 40), h = Math.ceil(this.b.y1 + 40);
    return `<mxfile host="drawio"><diagram name="${esc(this.name)}" id="${this.file}"><mxGraphModel dx="1400" dy="900" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${w}" pageHeight="${h}" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${this.cells.join('')}</root></mxGraphModel></diagram></mxfile>`;
  }
  save(outDir, meta = {}) {
    fs.mkdirSync(outDir, { recursive: true });
    fs.writeFileSync(path.join(outDir, this.file + '.drawio'), this.xml(), 'utf8');
    return { file: this.file, dir: outDir, b: this.b, ...meta };
  }
}

// ---------------- Use case ----------------
const ST = {
  title: 'text;html=1;align=left;verticalAlign=middle;fontStyle=1;fontSize=18;',
  actor: 'shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;fontStyle=1;fontSize=13;',
  uc: 'ellipse;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;fontSize=12;',
  boundary: 'rounded=0;whiteSpace=wrap;html=1;verticalAlign=top;align=center;fontStyle=1;fontSize=15;fillColor=none;strokeWidth=1.5;spacingTop=6;',
  assoc: 'endArrow=none;html=1;rounded=0;',
  dep: 'endArrow=open;endSize=10;dashed=1;html=1;rounded=0;fontSize=11;labelBackgroundColor=#ffffff;',
  gen: 'endArrow=block;endFill=0;endSize=14;html=1;rounded=0;',
  note: 'shape=note;whiteSpace=wrap;html=1;size=14;fillColor=#fff2cc;strokeColor=#d6b656;fontSize=11;align=left;spacingLeft=6;',
};

class UC extends Diagram {
  constructor(file, name, title) {
    super(file, name);
    this.v(title, ST.title, 40, 6, 1000, 36);
    this.counts = { actors: 0, ucs: 0 };
  }
  boundary(x, y, w, h) { return this.v(SYSTEM, ST.boundary, x, y, w, h); }
  actor(name, x, y) { this.counts.actors++; return this.v(name, ST.actor, x, y, 40, 80); }
  uc(label, x, y, w = 220, h = 60) { this.counts.ucs++; return this.v(label, ST.uc, x, y, w, h); }
  // Association pinned to the near tip of the ellipse, so it never cuts through other use cases.
  assoc(a, u) {
    const A = this.geo[a], U = this.geo[u];
    const left = A.x + A.w / 2 < U.x + U.w / 2;
    const pin = left ? 'exitX=1;exitY=0.35;entryX=0;entryY=0.5;' : 'exitX=0;exitY=0.35;entryX=1;entryY=0.5;';
    return this.e(a, u, '', ST.assoc + pin + 'exitDx=0;exitDy=0;entryDx=0;entryDy=0;');
  }
  // Between two horizontally separated use cases, pin the right-hand ellipse at its left tip:
  // a line leaving a column of equal-x tips can never cut through the other ellipses of that column.
  pin(src, tgt, extra) {
    if (/exit|entry/.test(extra)) return extra;
    const S = this.geo[src], T = this.geo[tgt];
    if (S.x + S.w <= T.x) return extra + 'exitX=1;exitY=0.5;exitDx=0;exitDy=0;entryX=0;entryY=0.5;entryDx=0;entryDy=0;';
    if (T.x + T.w <= S.x) return extra + 'exitX=0;exitY=0.5;exitDx=0;exitDy=0;entryX=1;entryY=0.5;entryDx=0;entryDy=0;';
    return extra;
  }
  include(base, inc, extra = '', pts) { return this.e(base, inc, '«include»', ST.dep + this.pin(base, inc, extra), pts); }
  extend(ext, base, extra = '', pts) { return this.e(ext, base, '«extend»', ST.dep + this.pin(ext, base, extra), pts); }
  gen(child, parent, extra = '', pts) { return this.e(child, parent, '', ST.gen + this.pin(child, parent, extra), pts); }
  // Actor generalization: child sits offset outward from the parent; arrow enters the parent's outer side.
  actorGen(child, parent) {
    const C = this.geo[child], P = this.geo[parent];
    const right = C.x > P.x;
    const up = C.y > P.y;
    const exit = up ? 'exitX=0.5;exitY=0;' : `exitX=${right ? 1 : 0};exitY=0.5;`;
    return this.e(child, parent, '', ST.gen + `edgeStyle=orthogonalEdgeStyle;${exit}entryX=${right ? 1 : 0};entryY=0.5;exitDx=0;exitDy=0;entryDx=0;entryDy=0;`);
  }
  save(outDir, meta = {}) {
    const total = this.counts.actors + this.counts.ucs;
    if (total > 15) throw new Error(`${this.file}: ${total} phần tử (> 15 gồm cả actor)`);
    return super.save(outDir, { ...meta, ucCount: this.counts.ucs, actorCount: this.counts.actors });
  }
}

// ---------------- Auto-laid-out use case diagram ----------------
// spec: { file, code, title, items: { key: [label, [actorCodes]] }, rels: [['inc'|'ext'|'gen', a, b]] }
//  inc a→b: a includes b · ext a→b: a extends b · gen a→b: a is a child of b
// Dependent use cases (included / extending / child) go in columns to the right of their partner;
// actors attached only to dependent use cases are placed on the right side.
const ACTOR_NAME = { KH: 'Khách hàng', NV: 'Nhân viên', QL: 'Quản lý' };
const ACTOR_ORDER = ['KH', 'NV', 'QL'];
function autoUC(spec) {
  const d = new UC(spec.file, `${spec.code}: ${spec.title}`, `${spec.code}: ${spec.title}`);
  const keys = Object.keys(spec.items);
  const dependentOf = {};
  for (const [t, a, b] of spec.rels) {
    const dep = t === 'inc' ? b : a, partner = t === 'inc' ? a : b;
    (dependentOf[dep] = dependentOf[dep] || []).push(partner);
  }
  const isPrimary = k => !dependentOf[k];
  const actorsOf = k => spec.items[k][1] || [];
  const groupOf = k => { const a = actorsOf(k); const idx = a.length ? Math.min(...a.map(x => ACTOR_ORDER.indexOf(x))) : 9; return idx; };
  const primaries = keys.filter(isPrimary).sort((a, b) => groupOf(a) - groupOf(b) || keys.indexOf(a) - keys.indexOf(b));
  // parent in the layout tree: prefer a primary partner
  const parent = {};
  for (const k of keys) if (!isPrimary(k)) parent[k] = dependentOf[k].find(isPrimary) || dependentOf[k][0];
  const children = k => keys.filter(c => parent[c] === k);
  const col = {};
  const colOf = k => col[k] !== undefined ? col[k] : (col[k] = isPrimary(k) ? 0 : colOf(parent[k]) + 1);
  keys.forEach(colOf);
  const S = 86, X = [240, 600, 930, 1250], W = [220, 230, 230, 220];
  const blockH = k => Math.max(S, children(k).reduce((s, c) => s + blockH(c), 0));
  const pos = {};
  const place = (k, top) => {
    const bh = blockH(k);
    pos[k] = top + (bh - S) / 2;
    let t = top + (bh - children(k).reduce((s, c) => s + blockH(c), 0)) / 2;
    for (const c of children(k)) { place(c, t); t += blockH(c); }
  };
  let cursor = 100, prevGroup = null;
  for (const k of primaries) {
    const g = groupOf(k);
    if (prevGroup !== null && g !== prevGroup) cursor += 30;
    place(k, cursor); cursor += blockH(k); prevGroup = g;
  }
  const maxCol = Math.max(...keys.map(k => col[k]));
  const right = X[maxCol] + W[maxCol] + 40;
  const bottom = Math.max(...keys.map(k => pos[k])) + S;
  d.boundary(200, 50, right - 200, bottom - 50 + 10);
  const id = {};
  for (const k of keys) {
    const label = spec.items[k][0];
    const h = label.length > 34 ? 64 : 58;
    id[k] = d.uc(label, X[col[k]], pos[k] + (58 - h) / 2, W[col[k]], h);
  }
  // actors
  const used = ACTOR_ORDER.filter(a => keys.some(k => actorsOf(k).includes(a)));
  const leftActors = [], rightActors = [];
  for (const a of used) {
    const mine = keys.filter(k => actorsOf(k).includes(a));
    (mine.some(isPrimary) ? leftActors : rightActors).push(a);
  }
  const actorY = (a, onlyPrimary) => {
    const mine = keys.filter(k => actorsOf(k).includes(a) && (!onlyPrimary || isPrimary(k)));
    const own = onlyPrimary ? mine.filter(k => groupOf(k) === ACTOR_ORDER.indexOf(a)) : mine;
    const list = own.length ? own : mine;
    return list.reduce((s, k) => s + pos[k] + 29, 0) / list.length - 30;
  };
  const aid = {};
  let lastY = -Infinity;
  for (const a of leftActors) { const y = Math.max(actorY(a, true), lastY + 150); aid[a] = d.actor(ACTOR_NAME[a], 80, y); lastY = y; }
  lastY = -Infinity;
  for (const a of rightActors) { const y = Math.max(actorY(a, false), lastY + 150); aid[a] = d.actor(ACTOR_NAME[a], right + 50, y); lastY = y; }
  for (const k of keys) for (const a of actorsOf(k)) d.assoc(aid[a], id[k]);
  // relations; same-column relations are routed around the right side of that column
  let lane = 0;
  for (const [t, a, b] of spec.rels) {
    const fn = t === 'inc' ? 'include' : t === 'ext' ? 'extend' : 'gen';
    if (col[a] === col[b]) {
      const xr = X[col[a]] + W[col[a]] + 22 + 12 * (lane++);
      const ya = pos[a] + 29, yb = pos[b] + 29;
      d[fn](id[a], id[b], 'edgeStyle=orthogonalEdgeStyle;exitX=1;exitY=0.5;exitDx=0;exitDy=0;entryX=1;entryY=0.5;entryDx=0;entryDy=0;', [[xr, ya], [xr, yb]]);
    } else d[fn](id[a], id[b]);
  }
  return d;
}

// ---------------- BPMN ----------------
const BS = {
  pool: 'swimlane;horizontal=0;html=1;startSize=30;fontStyle=1;fontSize=14;fillColor=#f5f5f5;',
  lane: 'swimlane;horizontal=0;html=1;startSize=30;fontStyle=1;fontSize=13;swimlaneFillColor=#ffffff;',
  start: 'ellipse;html=1;aspect=fixed;strokeWidth=1.5;fillColor=#d5e8d4;strokeColor=#82b366;verticalLabelPosition=bottom;verticalAlign=top;fontSize=11;whiteSpace=nowrap;',
  end: 'ellipse;html=1;aspect=fixed;strokeWidth=4;fillColor=#f8cecc;strokeColor=#b85450;verticalLabelPosition=bottom;verticalAlign=top;fontSize=11;whiteSpace=nowrap;',
  task: 'rounded=1;whiteSpace=wrap;html=1;arcSize=14;fillColor=#dae8fc;strokeColor=#6c8ebf;fontSize=12;',
  gw: 'shape=mxgraph.bpmn.gateway2;html=1;perimeter=rhombusPerimeter;outlineConnect=0;outline=none;symbol=none;gwType=exclusive;fillColor=#fff2cc;strokeColor=#d6b656;labelPosition=left;verticalLabelPosition=top;align=right;verticalAlign=bottom;fontSize=11;whiteSpace=nowrap;spacing=0;labelBackgroundColor=#ffffff;',
  flow: 'edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;endArrow=block;endFill=1;endSize=8;fontSize=11;labelBackgroundColor=#ffffff;',
};
const LANE_COLOR = { 'Khách hàng': '#e1d5e7', 'Hệ thống website': '#dae8fc', 'Nhân viên': '#d5e8d4', 'Quản lý': '#ffe6cc' };

class BPMN extends Diagram {
  // lanes: [[name, height]]
  constructor(file, name, title, poolLabel, lanes, width, origin = [20, 60]) {
    super(file, name);
    this.v(title, ST.title, 20, 8, 1600, 36);
    const [px, py] = origin;
    const total = lanes.reduce((s, l) => s + l[1], 0);
    this.pool = this.v(poolLabel, BS.pool, px, py, width, total);
    this.lanes = {};
    let y = py;
    for (const [lname, h] of lanes) {
      const id = this.v(lname, BS.lane + `fillColor=${LANE_COLOR[lname]};`, 30, y - py, width - 30, h, this.pool, [px + 30, y]);
      this.lanes[lname] = { id, x: px + 30, y, h };
      y += h;
    }
  }
  laneAt(cy) { return Object.values(this.lanes).find(l => cy >= l.y && cy < l.y + l.h); }
  node(label, style, cx, cy, w, h) {
    const l = this.laneAt(cy);
    if (!l) throw new Error(`${this.file}: no lane at y=${cy} for "${label}"`);
    return this.v(label, style, cx - w / 2 - l.x, cy - h / 2 - l.y, w, h, l.id, [cx - w / 2, cy - h / 2]);
  }
  start(l, cx, cy) { return this.node(l, BS.start, cx, cy, 40, 40); }
  end(l, cx, cy) { return this.node(l, BS.end, cx, cy, 40, 40); }
  task(l, cx, cy, w = 150, h = 60) { return this.node(l, BS.task, cx, cy, w, h); }
  gw(l, cx, cy) { return this.node(l, BS.gw, cx, cy, 50, 50); }
  flow(a, b, label = '', extra = '', pts) { return this.e(a, b, label, BS.flow + extra, pts); }
}
const P = (ex, ey, nx, ny) => `exitX=${ex};exitY=${ey};exitDx=0;exitDy=0;entryX=${nx};entryY=${ny};entryDx=0;entryDy=0;`;

module.exports = { SYSTEM, esc, Diagram, UC, BPMN, ST, BS, P, autoUC, ACTOR_NAME };
