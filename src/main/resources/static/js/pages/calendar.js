// ============================================================
// 行事曆（對應 img/calendar.jpg）
// 左：月曆（週日起，6 列 42 格）→ GET /api/follow-ups?from&to（from/to = 第一格與最後一格）
// 右：選定日期的回電安排 + 加入案件 → GET /api/follow-ups/tickets、POST /api/follow-ups、DELETE /api/follow-ups/{id}
// ============================================================
import { api } from '../api.js';
import { $, $$, esc, dash, statusTag, fmtTime, toDateKey, toast, toastError, confirmDialog, withBusy, TICKET_STATUS } from '../ui.js';
import { renderTopbar } from '../components/topbar.js';

const DOW = ['日', '一', '二', '三', '四', '五', '六'];

const state = {
  year: 0,
  month: 0,         // 0-11
  selected: '',     // yyyy-MM-dd
  byDate: new Map(),// yyyy-MM-dd → FollowUpGetListResponse[]
  assignable: [],   // 可加入的工單
};

export function render(container) {
  renderTopbar({ title: '行事曆', back: '#/' });
  const today = new Date();
  state.year = today.getFullYear();
  state.month = today.getMonth();
  state.selected = toDateKey(today);

  container.innerHTML = `
    <div class="layout-split">
      <div class="card" id="cal-card"></div>
      <div class="card" id="day-card"></div>
    </div>`;

  paintCalendarShell();
  loadAssignable();
  loadMonth();
}

/** 這個月月曆實際畫出來的 42 個 Date */
function gridDates() {
  const first = new Date(state.year, state.month, 1);
  const start = new Date(first);
  start.setDate(1 - first.getDay());
  return Array.from({ length: 42 }, (_, i) => {
    const d = new Date(start);
    d.setDate(start.getDate() + i);
    return d;
  });
}

function paintCalendarShell() {
  $('#cal-card').innerHTML = `
    <div class="cal-head">
      <div class="nav">
        <button class="btn btn-sm" type="button" id="cal-prev">‹</button>
        <span class="month" id="cal-month"></span>
        <button class="btn btn-sm" type="button" id="cal-next">›</button>
      </div>
      <button class="btn btn-sm" type="button" id="cal-today">今天</button>
    </div>
    <div class="cal-grid" id="cal-grid"></div>`;
  $('#cal-prev').addEventListener('click', () => shiftMonth(-1));
  $('#cal-next').addEventListener('click', () => shiftMonth(1));
  $('#cal-today').addEventListener('click', () => {
    const t = new Date();
    state.year = t.getFullYear();
    state.month = t.getMonth();
    state.selected = toDateKey(t);
    loadMonth();
  });
}

function shiftMonth(delta) {
  const d = new Date(state.year, state.month + delta, 1);
  state.year = d.getFullYear();
  state.month = d.getMonth();
  loadMonth();
}

async function loadMonth() {
  const dates = gridDates();
  const from = toDateKey(dates[0]);
  const to = toDateKey(dates[dates.length - 1]);
  $('#cal-month').textContent = `${state.year} 年 ${state.month + 1} 月`;
  try {
    const list = await api.followUps(from, to);
    state.byDate = new Map();
    for (const f of list) {
      const key = String(f.followUpAt).slice(0, 10); // "2026-09-14T10:30:00" → "2026-09-14"
      if (!state.byDate.has(key)) state.byDate.set(key, []);
      state.byDate.get(key).push(f);
    }
    for (const arr of state.byDate.values()) arr.sort((a, b) => String(a.followUpAt).localeCompare(String(b.followUpAt)));
  } catch (err) {
    toastError(err);
    state.byDate = new Map();
  }
  paintGrid(dates);
  paintDay();
}

function paintGrid(dates) {
  const todayKey = toDateKey(new Date());
  $('#cal-grid').innerHTML = `
    ${DOW.map((d) => `<div class="cal-dow">${d}</div>`).join('')}
    ${dates.map((d) => {
      const key = toDateKey(d);
      const cnt = state.byDate.get(key)?.length || 0;
      const cls = [
        'cal-cell',
        d.getMonth() !== state.month ? 'other' : '',
        key === todayKey ? 'today' : '',
        key === state.selected ? 'selected' : '',
      ].join(' ');
      return `<button class="${cls}" type="button" data-date="${key}">
        <span class="d">${d.getDate()}</span>
        ${cnt ? `<span class="cnt">${cnt} 件</span>` : ''}
      </button>`;
    }).join('')}`;
  $$('[data-date]').forEach((b) => b.addEventListener('click', () => {
    state.selected = b.dataset.date;
    $$('.cal-cell').forEach((c) => c.classList.toggle('selected', c.dataset.date === state.selected));
    paintDay();
  }));
}

// ---------------- 右側：當天安排 + 加入案件 ----------------
function paintDay() {
  const d = new Date(state.selected + 'T00:00:00');
  const items = state.byDate.get(state.selected) || [];
  $('#day-card').innerHTML = `
    <div class="card-title" style="margin-bottom:2px">${d.getMonth() + 1} 月 ${d.getDate()} 日（週${DOW[d.getDay()]}）</div>
    <div class="muted small" style="margin-bottom:12px">當日跟進 / 回電安排</div>
    <div id="fu-list">
      ${items.length ? items.map(followUpHtml).join('') : `<div class="muted">這天沒有安排</div>`}
    </div>
    <div style="border-top:1px solid var(--border); margin:14px 0"></div>
    <div class="card-title">加入案件到這天</div>
    <form id="fu-form" class="stack">
      <select class="select" name="ticketNo" id="fu-ticket"></select>
      <div class="row">
        <input class="input" type="time" name="time" value="09:00" required style="width:130px">
        <input class="input grow" name="note" placeholder="備註（選填，只有自己看得到）" maxlength="200">
        <button class="btn btn-primary" type="submit">加入</button>
      </div>
      <div class="muted small">只列出你「處理中 / 等待客戶回覆」的案件；同一張案件可以排多筆時間。</div>
    </form>`;

  paintAssignableOptions();

  $$('[data-del]').forEach((b) => b.addEventListener('click', async () => {
    const id = b.dataset.del;
    const ok = await confirmDialog('要刪除這筆回電安排嗎？', { okText: '刪除', danger: true });
    if (!ok) return;
    try {
      await api.deleteFollowUp(id);
      toast('已刪除');
      loadMonth();
    } catch (err) { toastError(err); }
  }));

  const form = $('#fu-form');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const ticketNo = form.ticketNo.value;
    if (!ticketNo) { toast('沒有可加入的案件', 'err'); return; }
    const time = form.time.value || '09:00';
    await withBusy($('button[type="submit"]', form), async () => {
      try {
        await api.createFollowUp({
          ticketNo,
          followUpAt: `${state.selected}T${time}:00`,
          note: form.note.value.trim() || null,
        });
        toast('已加入行事曆');
        loadMonth();
      } catch (err) { toastError(err); }
    });
  });
}

function followUpHtml(f) {
  return `
    <div class="fu-item">
      <div class="time">${fmtTime(f.followUpAt)}</div>
      <div class="main">
        <div class="t"><a href="#/tickets/${esc(f.ticketNo)}" style="color:inherit">${esc(f.title)}</a></div>
        <div class="m">${esc(f.ticketNo)} · ${dash(f.customerName)} · ${dash(f.contactPhone)}</div>
        ${f.note ? `<div class="n">📝 ${esc(f.note)}</div>` : ''}
      </div>
      ${statusTag(f.status)}
      <button class="x" type="button" data-del="${f.followUpId}" title="刪除">✕</button>
    </div>`;
}

async function loadAssignable() {
  try {
    state.assignable = await api.assignableTickets();
  } catch (err) {
    state.assignable = [];
    toastError(err);
  }
  paintAssignableOptions();
}

function paintAssignableOptions() {
  const sel = $('#fu-ticket');
  if (!sel) return;
  if (!state.assignable.length) {
    sel.innerHTML = `<option value="">（目前沒有處理中 / 待客戶回覆的案件）</option>`;
    return;
  }
  sel.innerHTML = state.assignable
    .map((t) => `<option value="${esc(t.ticketNo)}">${esc(t.ticketNo)}｜${esc(t.title)}（${esc(TICKET_STATUS[t.status] || t.status)}）</option>`)
    .join('');
}
