// ============================================================
// 共用的小工具：enum 中文、時間格式
// （React 會自動跳脫 {} 裡的文字，所以不再需要舊版的 esc()）
// ============================================================

// ---------------- enum 對應的中文 ----------------
export const TICKET_STATUS = {
  IN_PROGRESS: '處理中',
  PENDING: '待客戶回覆',
  RESOLVED: '已解決',
};

export const AGENT_STATUS = {
  ONLINE: '線上',
  ON_CALL: '通話中',
  BREAK: '休息',
  RESTROOM: '廁所',
  LUNCH: '午休',
  MEETING: '簡報',
  OFFLINE: '離線',
};

/** 客服可以手動切換的狀態（ON_CALL 由通話事件、OFFLINE 由登出設定，後端都會擋） */
export const MANUAL_AGENT_STATUSES = ['ONLINE', 'BREAK', 'RESTROOM', 'LUNCH', 'MEETING'];

export const CHANNEL = {
  PHONE: '電話',
  AGENT: '客服手動建立',
};

export const CATEGORIES = ['帳號問題', '付款、發票', '課程內容', '技術問題', '其他'];

// ---------------- 時間格式 ----------------
const pad2 = (n) => String(n).padStart(2, '0');

/** 後端 LocalDateTime 序列化成 "2026-09-18T10:30:00"，直接交給 Date 解析為本地時間 */
export function parseDateTime(iso) {
  if (!iso) return null;
  const d = new Date(iso);
  return isNaN(d.getTime()) ? null : d;
}

function isSameDay(a, b) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

/** 今天 → "今天 09:48"；同年 → "9/13 22:45"；其他 → "2025/9/13 22:45"；空值 → "—" */
export function fmtDateTime(iso) {
  const d = parseDateTime(iso);
  if (!d) return '—';
  const now = new Date();
  const hm = `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
  if (isSameDay(d, now)) return `今天 ${hm}`;
  if (d.getFullYear() === now.getFullYear()) return `${d.getMonth() + 1}/${d.getDate()} ${hm}`;
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()} ${hm}`;
}

export function fmtTime(iso) {
  const d = parseDateTime(iso);
  return d ? `${pad2(d.getHours())}:${pad2(d.getMinutes())}` : '—';
}

/** Date → "yyyy-MM-dd"（用本地時區，不用 toISOString 以免跨日） */
export function toDateKey(d) {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

/** 秒數 → "mm:ss"，通話計時用 */
export function fmtDuration(seconds) {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${pad2(m)}:${pad2(s)}`;
}

/** null / 空字串 → "—" */
export function dash(v) {
  return v === null || v === undefined || v === '' ? '—' : v;
}

/** 讀 Spring 的分頁回應。設定 via_dto 時是 { content, page: {...} }；沒設定時欄位在最外層 */
export function readPage(res) {
  const p = res.page || res;
  return {
    content: res.content || [],
    number: p.number ?? 0,
    size: p.size ?? 10,
    totalElements: p.totalElements ?? 0,
    totalPages: p.totalPages ?? 0,
  };
}
