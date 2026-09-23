// ============================================================
// 共用的 UI 小工具：跳脫、標籤文字、時間格式、toast、modal、confirm
// ============================================================

/** 把使用者輸入的文字放進 innerHTML 前一定要先跳脫，避免 XSS */
export function esc(v) {
  if (v === null || v === undefined) return '';
  return String(v)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export const $ = (sel, root = document) => root.querySelector(sel);
export const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

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

/** 把 GET /tickets/assignees 的結果轉成 <option>，exclude 內的代號（自己／目前負責人）不列 */
export function assigneeOptionsHtml(agents, exclude = []) {
  const list = agents.filter((a) => !exclude.includes(a.agentId));
  if (!list.length) return '<option value="">（沒有可轉派的客服）</option>';
  return list.map((a) => `<option value="${esc(a.agentId)}">${esc(a.agentId)} ${esc(a.name)}</option>`).join('');
}

export function statusTag(status) {
  return `<span class="tag ${esc(status)}">${esc(TICKET_STATUS[status] || status)}</span>`;
}

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
  return v === null || v === undefined || v === '' ? '—' : esc(v);
}

// ---------------- toast ----------------
export function toast(message, type = 'ok', ms = 2800) {
  const root = $('#toast-root');
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.textContent = message;
  root.appendChild(el);
  setTimeout(() => el.remove(), ms);
}

/** 把 API 例外顯示成紅色 toast */
export function toastError(err) {
  toast(err?.message || '發生錯誤', 'err', 4000);
}

// ---------------- modal ----------------
/**
 * 開一個 modal。
 * @param {{ title: string, body: string, narrow?: boolean, onOpen?: (modalEl, close) => void }} opts
 * @returns {{ el: HTMLElement, close: () => void }}
 */
export function openModal({ title, body, narrow = false, onOpen }) {
  const root = $('#modal-root');
  const backdrop = document.createElement('div');
  backdrop.className = 'modal-backdrop';
  backdrop.innerHTML = `
    <div class="modal ${narrow ? 'narrow' : ''}" role="dialog" aria-modal="true">
      <div class="modal-head">
        <h3>${esc(title)}</h3>
        <button class="modal-close" type="button" aria-label="關閉">✕</button>
      </div>
      <div class="modal-body">${body}</div>
    </div>`;
  root.appendChild(backdrop);

  const close = () => {
    backdrop.remove();
    document.removeEventListener('keydown', onKey);
  };
  const onKey = (e) => { if (e.key === 'Escape') close(); };
  document.addEventListener('keydown', onKey);
  $('.modal-close', backdrop).addEventListener('click', close);
  backdrop.addEventListener('mousedown', (e) => { if (e.target === backdrop) close(); });

  const modalEl = $('.modal', backdrop);
  if (onOpen) onOpen(modalEl, close);
  const first = $('input, select, textarea, button.btn', modalEl);
  if (first) first.focus();
  return { el: modalEl, close };
}

/**
 * 自製的確認框（不用 window.confirm，風格一致也不會擋住頁面）。
 * @returns {Promise<boolean>}
 */
export function confirmDialog(message, { okText = '確定', danger = false } = {}) {
  return new Promise((resolve) => {
    const { close } = openModal({
      title: '請確認',
      narrow: true,
      body: `
        <p style="margin:0 0 6px; line-height:1.6">${esc(message)}</p>
        <div class="modal-foot">
          <button class="btn" type="button" data-act="cancel">取消</button>
          <button class="btn ${danger ? 'btn-danger' : 'btn-primary'}" type="button" data-act="ok">${esc(okText)}</button>
        </div>`,
      onOpen(el, closeFn) {
        $('[data-act="cancel"]', el).addEventListener('click', () => { closeFn(); resolve(false); });
        $('[data-act="ok"]', el).addEventListener('click', () => { closeFn(); resolve(true); });
        $('[data-act="ok"]', el).focus();
      },
    });
    void close;
  });
}

/** 讓按鈕在非同步動作期間變成 disabled，避免連點 */
export async function withBusy(btn, fn) {
  if (!btn) return fn();
  const oldText = btn.textContent;
  btn.disabled = true;
  try {
    return await fn();
  } finally {
    btn.disabled = false;
    btn.textContent = oldText;
  }
}

/** 簡單的 debounce，用在篩選輸入 */
export function debounce(fn, ms = 300) {
  let t;
  return (...args) => {
    clearTimeout(t);
    t = setTimeout(() => fn(...args), ms);
  };
}
