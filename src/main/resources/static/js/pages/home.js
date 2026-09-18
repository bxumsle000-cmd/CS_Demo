// ============================================================
// 首頁：工單列表（對應 img/work1~3.jpg）
// GET /api/tickets?ticketNo&customerName&contactPhone&status&assigneeId&updatedFrom&updatedTo&page&size
// ============================================================
import { api } from '../api.js';
import { getSession, getCall, startCall, randomIncomingNumber } from '../state.js';
import { $, $$, esc, dash, statusTag, fmtDateTime, toast, toastError, openModal, debounce, TICKET_STATUS } from '../ui.js';
import { renderTopbar } from '../components/topbar.js';
import { ticketFormHtml, bindTicketForm } from '../components/ticketForm.js';
import { navigate } from '../app.js';

const TABS = [
  { key: '', label: '全部' },
  { key: 'RESOLVED', label: '已處理' },
  { key: 'IN_PROGRESS', label: '處理中' },
  { key: 'PENDING', label: '等待客戶回覆' },
];

// 篩選條件放模組層級，從詳情頁返回時還會記得剛才的查詢
const state = {
  ticketNo: '',
  customerName: '',
  contactPhone: '',
  status: '',
  assigneeId: null, // null = 尚未初始化，第一次進來預設帶入登入者
  updatedFrom: '',  // yyyy-MM-dd，依更新時間過濾（含當天）
  updatedTo: '',    // yyyy-MM-dd（含當天）
  page: 0,
  size: 10,
};

/** 讀 Spring 的分頁回應。設定 via_dto 時是 { content, page: {...} }；沒設定時欄位在最外層 */
function readPage(res) {
  const p = res.page || res;
  return {
    content: res.content || [],
    number: p.number ?? 0,
    size: p.size ?? 10,
    totalElements: p.totalElements ?? 0,
    totalPages: p.totalPages ?? 0,
  };
}

function baseQuery() {
  return {
    ticketNo: state.ticketNo,
    customerName: state.customerName,
    contactPhone: state.contactPhone,
    assigneeId: state.assigneeId,
    updatedFrom: state.updatedFrom,
    updatedTo: state.updatedTo,
  };
}

export function render(container) {
  renderTopbar({ title: '首頁' });
  const s = getSession();
  if (state.assigneeId === null) state.assigneeId = s?.agentId || '';

  container.innerHTML = `
    <div class="toolbar">
      <div class="toolbar-left">
        <button class="btn" type="button" id="btn-new">＋ 新增派件</button>
        <button class="btn btn-primary" type="button" id="btn-answer">📞 接聽電話</button>
      </div>
      <div class="toolbar-right">
        <a class="btn" href="#/calendar">📅 行事曆</a>
      </div>
    </div>

    <div class="tabs" id="tabs">
      ${TABS.map((t) => `<button class="tab ${state.status === t.key ? 'active' : ''}" type="button" data-tab="${t.key}">${t.label}</button>`).join('')}
    </div>

    <div class="card">
      <form class="filters" id="filters" autocomplete="off">
        <div class="field"><label>單號</label><input class="input" name="ticketNo" placeholder="全部" value="${esc(state.ticketNo)}"></div>
        <div class="field"><label>姓名</label><input class="input" name="customerName" placeholder="全部" value="${esc(state.customerName)}"></div>
        <div class="field"><label>電話</label><input class="input" name="contactPhone" placeholder="全部" value="${esc(state.contactPhone)}"></div>
        <div class="field"><label>狀態</label>
          <select class="select" name="status">
            <option value="">全部</option>
            ${Object.entries(TICKET_STATUS).map(([k, v]) => `<option value="${k}" ${state.status === k ? 'selected' : ''}>${v}</option>`).join('')}
          </select>
        </div>
        <div class="field"><label>客服</label><input class="input" name="assigneeId" placeholder="全部" value="${esc(state.assigneeId)}"></div>
        <div class="field"><label>更新時間（起）</label><input class="input" type="date" name="updatedFrom" value="${esc(state.updatedFrom)}"></div>
        <div class="field"><label>更新時間（迄）</label><input class="input" type="date" name="updatedTo" value="${esc(state.updatedTo)}"></div>
        <div class="field"><label>&nbsp;</label><button class="btn" type="button" id="btn-reset">重設</button></div>
      </form>
    </div>

    <div class="card" style="margin-top:12px">
      <div class="table-wrap">
        <table class="tbl">
          <thead><tr><th>單號</th><th>姓名</th><th>電話</th><th>狀態</th><th>客服</th><th>更新時間</th></tr></thead>
          <tbody id="tbody"><tr><td colspan="6" class="loading">載入中…</td></tr></tbody>
        </table>
      </div>
      <div class="pager" id="pager"></div>
    </div>`;

  // ---- 工具列 ----
  $('#btn-new').addEventListener('click', openNewTicketModal);
  const answerBtn = $('#btn-answer');
  const syncAnswerBtn = () => {
    const c = getCall();
    answerBtn.textContent = c ? '📞 回到通話工作台' : '📞 接聽電話';
  };
  syncAnswerBtn();
  answerBtn.addEventListener('click', () => {
    if (!getCall()) {
      startCall(randomIncomingNumber());
      toast('來電進線，已接聽');
    }
    navigate('#/call');
  });

  // ---- 頁籤 ----
  $$('[data-tab]').forEach((b) => b.addEventListener('click', () => {
    state.status = b.dataset.tab;
    state.page = 0;
    $('#filters').status.value = state.status;
    syncTabs();
    load();
  }));

  // ---- 篩選列 ----
  const form = $('#filters');
  const applyFilters = () => {
    state.ticketNo = form.ticketNo.value.trim();
    state.customerName = form.customerName.value.trim();
    state.contactPhone = form.contactPhone.value.trim();
    state.status = form.status.value;
    state.assigneeId = form.assigneeId.value.trim();
    state.updatedFrom = form.updatedFrom.value;
    state.updatedTo = form.updatedTo.value;
    if (state.updatedFrom && state.updatedTo && state.updatedFrom > state.updatedTo) {
      toast('起始日期不能晚於結束日期', 'err');
      return;
    }
    state.page = 0;
    syncTabs();
    load();
  };
  const debounced = debounce(applyFilters, 350);
  ['ticketNo', 'customerName', 'contactPhone', 'assigneeId'].forEach((n) => form[n].addEventListener('input', debounced));
  form.status.addEventListener('change', applyFilters);
  form.updatedFrom.addEventListener('change', applyFilters);
  form.updatedTo.addEventListener('change', applyFilters);
  form.addEventListener('submit', (e) => { e.preventDefault(); applyFilters(); });
  $('#btn-reset').addEventListener('click', () => {
    form.reset();
    form.assigneeId.value = s?.agentId || '';
    applyFilters();
  });

  load();
  return () => { /* 沒有需要清理的計時器 */ };
}

function syncTabs() {
  $$('[data-tab]').forEach((b) => b.classList.toggle('active', b.dataset.tab === state.status));
}

let loadSeq = 0;
async function load() {
  const seq = ++loadSeq;
  const tbody = $('#tbody');
  const q = { ...baseQuery(), status: state.status, page: state.page, size: state.size };

  try {
    const listRes = await api.searchTickets(q);
    if (seq !== loadSeq) return; // 已經有更新的查詢，丟掉這次結果

    const page = readPage(listRes);
    renderRows(page);
    renderPager(page);
  } catch (err) {
    if (seq !== loadSeq) return;
    tbody.innerHTML = `<tr><td colspan="6" class="empty">${esc(err.message)}</td></tr>`;
    toastError(err);
  }
}

function renderRows(page) {
  const tbody = $('#tbody');
  if (!page.content.length) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty">沒有符合條件的工單</td></tr>`;
    return;
  }
  tbody.innerHTML = page.content.map((t) => `
    <tr>
      <td><a href="#/tickets/${esc(t.ticketNo)}">${esc(t.ticketNo)}</a></td>
      <td><b>${dash(t.customerName)}</b></td>
      <td>${dash(t.contactPhone)}</td>
      <td>${statusTag(t.status)}</td>
      <td>${esc(t.assigneeId)}</td>
      <td class="muted">${fmtDateTime(t.updatedAt)}</td>
    </tr>`).join('');
}

function renderPager(page) {
  const pager = $('#pager');
  const totalPages = Math.max(page.totalPages, 1);
  const cur = page.number;
  pager.innerHTML = `
    <div class="pager-left">
      <span class="muted">每頁</span>
      <select class="select" id="page-size">
        ${[10, 20, 50].map((n) => `<option value="${n}" ${state.size === n ? 'selected' : ''}>${n}</option>`).join('')}
      </select>
      <span class="muted">筆 · 共 ${page.totalElements} 筆</span>
    </div>
    <div class="pager-right">
      <span class="muted">第 ${cur + 1} / ${totalPages} 頁</span>
      <button class="btn btn-sm" type="button" id="page-prev" ${cur <= 0 ? 'disabled' : ''}>‹</button>
      <button class="btn btn-sm" type="button" id="page-next" ${cur + 1 >= totalPages ? 'disabled' : ''}>›</button>
    </div>`;
  $('#page-size').addEventListener('change', (e) => { state.size = Number(e.target.value); state.page = 0; load(); });
  $('#page-prev').addEventListener('click', () => { state.page = Math.max(0, cur - 1); load(); });
  $('#page-next').addEventListener('click', () => { state.page = cur + 1; load(); });
}

// ---------------- ＋ 新增派件 ----------------
function openNewTicketModal() {
  openModal({
    title: '新增派件',
    body: ticketFormHtml({ mode: 'agent' }),
    onOpen(el, close) {
      bindTicketForm($('form', el), {
        async onSubmit(body) {
          await api.createTicketFromAgent(body);
          toast('工單已建立');
          close();
          state.page = 0;
          load();
        },
      });
    },
  });
}
