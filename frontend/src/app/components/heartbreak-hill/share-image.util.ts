import { ActivityType } from '../../models/heartbreak-hill.model';
import { buildElevationPoints } from './heartbreak-hill.util';

export type ShareTemplate = 'A' | 'B' | 'C';

export const SHARE_W = 1080;
export const SHARE_H = 1920;

export interface ShareImageData {
  segmentName: string;
  activityType: ActivityType;
  elevation: [number, number, number][] | null;
  tempo: string;     // pre-formatted ("23,4 km/h" | "4:35 /km" | "—")
  time: string;      // elapsedFormatted ("4:02")
  rank: number;
  totalCount: number;
}

export interface ShareLabels {
  tempo: string;     // "TEMPO" / "SPEED" / "PACE"
  time: string;      // "ZEIT" / "TIME"
  rank: string;      // "RANG" / "RANK"
  of: string;        // "von" / "of"
}

/** Ride → "23,4 km/h" (locale-aware), Run → "4:35 /km". Missing value → em dash. */
export function formatTempo(
  type: ActivityType,
  avgSpeedKmh: number | null,
  avgPaceSecondsPerKm: number | null,
  locale: string
): string {
  if (type === 'RIDE') {
    if (avgSpeedKmh == null) {
      return '—';
    }
    const n = new Intl.NumberFormat(locale, {
      minimumFractionDigits: 1, maximumFractionDigits: 1
    }).format(avgSpeedKmh);
    return `${n} km/h`;
  }
  if (avgPaceSecondsPerKm == null) {
    return '—';
  }
  const m = Math.floor(avgPaceSecondsPerKm / 60);
  const s = avgPaceSecondsPerKm % 60;
  return `${m}:${s < 10 ? '0' + s : s} /km`;
}

/** e.g. "heartbreak-hill-rang6-rad.png". */
export function shareFileName(rank: number, type: ActivityType): string {
  return `heartbreak-hill-rang${rank}-${type === 'RIDE' ? 'rad' : 'lauf'}.png`;
}

const GREEN = '#8ffc2e';
const SYNTHETIC_ELE = [
  100, 103, 109, 112, 110, 118, 126, 130, 128, 138,
  150, 158, 166, 170, 176, 184, 190, 198, 206, 214
];

/** Loads an <img> from a same-origin asset URL (no canvas tainting). */
export function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = reject;
    img.src = src;
  });
}

function fontFamily(): string {
  const v = getComputedStyle(document.documentElement)
    .getPropertyValue('--font-family').trim();
  return v || 'system-ui, sans-serif';
}

function syntheticPoints(): [number, number, number][] {
  return SYNTHETIC_ELE.map((e, i) => [0, i, e]);
}

function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number): void {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
}

function shadowText(ctx: CanvasRenderingContext2D, text: string, x: number, y: number): void {
  ctx.save();
  ctx.shadowColor = 'rgba(0,0,0,0.55)';
  ctx.shadowBlur = 10;
  ctx.shadowOffsetY = 2;
  ctx.fillText(text, x, y);
  ctx.restore();
}

function textWidth(ctx: CanvasRenderingContext2D, text: string, font: string): number {
  ctx.font = font;
  return ctx.measureText(text).width;
}

/** Centered PACR logo lockup on a dark, green-bordered chip — prominent branding. */
function drawLogoLockup(ctx: CanvasRenderingContext2D, logo: HTMLImageElement | null, centerX: number, bottomY: number, logoH: number): void {
  const fam = fontFamily();
  const padX = 28, padY = 15;
  const logoW = logo ? (logo.width / logo.height) * logoH : textWidth(ctx, 'PACR', `800 ${logoH}px ${fam}`);
  const pillW = logoW + padX * 2;
  const pillH = logoH + padY * 2;
  const px = centerX - pillW / 2;
  const py = bottomY - pillH;
  ctx.fillStyle = 'rgba(8,12,9,0.62)';
  roundRect(ctx, px, py, pillW, pillH, 18);
  ctx.fill();
  ctx.strokeStyle = 'rgba(143,252,46,0.30)';
  ctx.lineWidth = 2;
  ctx.stroke();
  if (logo) {
    ctx.drawImage(logo, px + padX, py + padY, logoW, logoH);
  } else {
    ctx.fillStyle = GREEN;
    ctx.font = `800 ${logoH}px ${fam}`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('PACR', centerX, py + pillH / 2 + 1);
    ctx.textBaseline = 'alphabetic';
    ctx.textAlign = 'left';
  }
}

/** Small right-anchored PACR logo for an already-dark surface (no chip). */
function drawLogoInline(ctx: CanvasRenderingContext2D, logo: HTMLImageElement | null, rightX: number, baseline: number, logoH: number): void {
  const fam = fontFamily();
  if (logo) {
    const logoW = (logo.width / logo.height) * logoH;
    ctx.drawImage(logo, rightX - logoW, baseline - logoH + Math.round(logoH * 0.16), logoW, logoH);
  } else {
    ctx.fillStyle = GREEN;
    ctx.font = `800 ${Math.round(logoH * 0.8)}px ${fam}`;
    ctx.textAlign = 'right';
    ctx.textBaseline = 'alphabetic';
    ctx.fillText('PACR', rightX, baseline);
    ctx.textAlign = 'left';
  }
}

interface Col { label: string; value: string; sub?: string; green: boolean; }

function columns(data: ShareImageData, labels: ShareLabels): Col[] {
  return [
    { label: labels.tempo, value: data.tempo, green: false },
    { label: labels.time, value: data.time, green: false },
    { label: labels.rank, value: `#${data.rank}`, sub: `${labels.of} ${data.totalCount}`, green: true }
  ];
}

function strokeRidge(ctx: CanvasRenderingContext2D, pts: { x: number; y: number }[], offsetX: number, offsetY: number): void {
  ctx.beginPath();
  pts.forEach((p, i) => (i ? ctx.lineTo(offsetX + p.x, offsetY + p.y) : ctx.moveTo(offsetX + p.x, offsetY + p.y)));
  ctx.stroke();
}

/** Renders the chosen template onto a transparent 1080×1920 context. */
export function drawShareImage(
  ctx: CanvasRenderingContext2D,
  data: ShareImageData,
  template: ShareTemplate,
  labels: ShareLabels,
  logo: HTMLImageElement | null
): void {
  ctx.clearRect(0, 0, SHARE_W, SHARE_H);
  ctx.lineJoin = 'round';
  ctx.lineCap = 'round';
  const ele = data.elevation && data.elevation.length >= 2 ? data.elevation : syntheticPoints();
  if (template === 'A') {
    drawTemplateA(ctx, data, labels, logo, ele);
  } else if (template === 'B') {
    drawTemplateB(ctx, data, labels, logo, ele);
  } else {
    drawTemplateC(ctx, data, labels, logo, ele);
  }
}

function drawTemplateA(ctx: CanvasRenderingContext2D, data: ShareImageData, labels: ShareLabels, logo: HTMLImageElement | null, ele: [number, number, number][]): void {
  const fam = fontFamily();
  const rh = Math.round(SHARE_H * 0.40);
  const ry = SHARE_H - rh;
  const pts = buildElevationPoints(ele, SHARE_W, rh);

  ctx.beginPath();
  ctx.moveTo(0, ry + pts[0].y);
  pts.forEach(p => ctx.lineTo(p.x, ry + p.y));
  ctx.lineTo(SHARE_W, SHARE_H);
  ctx.lineTo(0, SHARE_H);
  ctx.closePath();
  ctx.fillStyle = 'rgba(143,252,46,0.82)';
  ctx.fill();

  ctx.strokeStyle = '#d7ff9e';
  ctx.lineWidth = 7;
  strokeRidge(ctx, pts, 0, ry);

  ctx.textBaseline = 'alphabetic';
  ctx.textAlign = 'left';
  ctx.fillStyle = 'rgba(255,255,255,0.92)';
  ctx.font = `500 32px ${fam}`;
  shadowText(ctx, data.segmentName.toUpperCase(), 60, 132);

  const cols = columns(data, labels);
  const colW = SHARE_W / 3;
  const baseY = ry - 80;
  ctx.textAlign = 'center';
  cols.forEach((c, i) => {
    const cx = colW * i + colW / 2;
    ctx.font = `500 28px ${fam}`;
    ctx.fillStyle = '#bdefae';
    ctx.fillText(c.label.toUpperCase(), cx, baseY);
    ctx.font = `800 66px ${fam}`;
    ctx.fillStyle = c.green ? GREEN : '#ffffff';
    shadowText(ctx, c.value, cx, baseY + 72);
    if (c.sub) {
      ctx.font = `500 30px ${fam}`;
      ctx.fillStyle = 'rgba(255,255,255,0.8)';
      ctx.fillText(c.sub, cx, baseY + 116);
    }
  });

  drawLogoLockup(ctx, logo, SHARE_W / 2, SHARE_H - 44, 64);
}

function drawTemplateB(ctx: CanvasRenderingContext2D, data: ShareImageData, labels: ShareLabels, logo: HTMLImageElement | null, ele: [number, number, number][]): void {
  const fam = fontFamily();
  const cardX = 48, cardW = SHARE_W - 96, cardH = 440;
  const cardY = SHARE_H - 60 - cardH;
  const padX = 44;

  ctx.fillStyle = 'rgba(11,15,20,0.58)';
  roundRect(ctx, cardX, cardY, cardW, cardH, 28);
  ctx.fill();

  ctx.textBaseline = 'alphabetic';
  ctx.textAlign = 'left';
  ctx.fillStyle = '#cfe9c2';
  ctx.font = `500 28px ${fam}`;
  ctx.fillText(data.segmentName.toUpperCase(), cardX + padX, cardY + 74);
  drawLogoInline(ctx, logo, cardX + cardW - padX, cardY + 78, 40);

  const rh = 150;
  const pts = buildElevationPoints(ele, cardW - padX * 2, rh);
  ctx.strokeStyle = GREEN;
  ctx.lineWidth = 6;
  strokeRidge(ctx, pts, cardX + padX, cardY + 110);

  const cols = columns(data, labels);
  const colW = cardW / 3;
  const rowY = cardY + cardH - 120;
  ctx.textAlign = 'center';
  cols.forEach((c, i) => {
    const cx = cardX + colW * i + colW / 2;
    ctx.font = `500 26px ${fam}`;
    ctx.fillStyle = '#bdefae';
    ctx.fillText(c.label.toUpperCase(), cx, rowY);
    ctx.font = `800 60px ${fam}`;
    ctx.fillStyle = c.green ? GREEN : '#ffffff';
    ctx.fillText(c.value, cx, rowY + 66);
    if (c.sub) {
      ctx.font = `500 28px ${fam}`;
      ctx.fillStyle = 'rgba(255,255,255,0.75)';
      ctx.fillText(c.sub, cx, rowY + 106);
    }
  });
}

function drawTemplateC(ctx: CanvasRenderingContext2D, data: ShareImageData, labels: ShareLabels, logo: HTMLImageElement | null, ele: [number, number, number][]): void {
  const fam = fontFamily();
  const rh = Math.round(SHARE_H * 0.34);
  const ry = Math.round(SHARE_H * 0.46);
  const pts = buildElevationPoints(ele, SHARE_W, rh);

  ctx.save();
  ctx.shadowColor = 'rgba(0,0,0,0.6)';
  ctx.shadowBlur = 8;
  ctx.shadowOffsetY = 2;
  ctx.strokeStyle = GREEN;
  ctx.lineWidth = 6;
  strokeRidge(ctx, pts, 0, ry);
  ctx.restore();

  ctx.textBaseline = 'alphabetic';
  ctx.textAlign = 'left';
  ctx.fillStyle = 'rgba(255,255,255,0.92)';
  ctx.font = `500 32px ${fam}`;
  shadowText(ctx, data.segmentName.toUpperCase(), 60, 132);

  const rows = [
    { value: data.tempo, label: labels.tempo, green: false },
    { value: data.time, label: labels.time, green: false },
    { value: `#${data.rank}`, label: `${labels.of} ${data.totalCount}`, green: true }
  ];
  let y = SHARE_H - 330;
  rows.forEach(r => {
    const valueFont = `800 76px ${fam}`;
    ctx.font = valueFont;
    ctx.fillStyle = r.green ? GREEN : '#ffffff';
    shadowText(ctx, r.value, 60, y);
    ctx.font = `500 28px ${fam}`;
    ctx.fillStyle = '#bdefae';
    shadowText(ctx, r.label.toUpperCase(), 60 + textWidth(ctx, r.value, valueFont) + 20, y);
    y += 88;
  });

  drawLogoLockup(ctx, logo, SHARE_W / 2, SHARE_H - 44, 64);
}
