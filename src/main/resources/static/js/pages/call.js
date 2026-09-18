// ============================================================
// 通話工作台（對應 img/order.jpg）
// 左：本次通話工單表單 → POST /api/tickets/from-call
// 右：進線號碼 + 此號碼歷史紀錄 → GET /api/tickets?contactPhone=...
// ============================================================
import { api } from '../api.js';
import { getCall, startCall, endCall, randomIncomingNumber } from '../state.js';
import { $, esc, statusTag, fmtDateTime, toast, toastError } from '../ui.js';
import { renderTopbar } from '../components/topbar.js';
import { ticketFormHtml, bindTicketForm } from '../components/ticketForm.js';
import { navigate } from '../app.js';

export function render(container) {
  renderTopbar({ title: '工作台', back: '#/' });
  const call = getCall();

  if (!call) {
    container.innerHTML = `
      <div class="card" style="max-width:520px; margin: 40px auto; text-align:center">
        <div class="card-title">目前沒有進行中的通話</div>
        <p class="muted" style="margin:0 0 16px">接聽電話後，這裡會出現「本次通話工單」表單與進線號碼的歷史紀錄。</p>
        <button class="btn btn-primary" type="button" id="btn-answer">📞 接聽電話（模擬來電）</button>
      </div>`;
    $('#btn-answer').addEventListener('click', () => {
      startCall(randomIncomingNumber());
      toast('來電進線，已接聽');
      render(container);
    });
    return;
  }

  container.innerHTML = `
    <div class="layout-split">
      <div class="card">
        <div class="card-title">本次通話工單</div>
        ${ticketFormHtml({ mode: 'call', phone: call.phone })}
      </div>
      <div class="card">
        <div class="muted small">進線號碼</div>
        <div class="big-number">${esc(call.phone)}</div>
        <div class="muted small" id="phone-sub">號碼未識別 · 通話中確認身分</div>
        <div style="border-top:1px solid var(--border); margin:14px 0"></div>
        <div class="card-title" id="history-title">此號碼歷史紀錄</div>
        <div id="history"><div class="loading">載入中…</div></div>
      </div>
    </div>`;

  bindTicketForm($('form.ticket-form'), {
    phone: call.phone,
    async onSubmit(body) {
      await api.createTicketFromCall(body);
      endCall();
      toast('工單已建立，通話已結束');
      navigate('#/');
    },
  });

  loadHistory(call.phone, 0);
}

async function loadHistory(phone, page) {
  const box = $('#history');
  try {
    const res = await api.searchTickets({ contactPhone: phone, page, size: 10 });
    if (!$('#history')) return; // 已離開頁面
    const p = res.page || res;
    const rows = res.content || [];
    const total = p.totalElements ?? rows.length;
    const totalPages = Math.max(p.totalPages ?? 1, 1);
    const cur = p.number ?? 0;

    $('#history-title').textContent = `此號碼歷史紀錄（${total}）`;
    const known = rows.find((t) => t.customerName);
    if (known) $('#phone-sub').textContent = `曾以「${known.customerName}」進線 · 通話中再次確認`;

    if (!rows.length) {
      box.innerHTML = `<div class="muted">此號碼沒有歷史工單</div>`;
      return;
    }
    box.innerHTML = `
      ${rows.map((t) => `
        <div class="history-item">
          <div class="t">${statusTag(t.status)}<a href="#/tickets/${esc(t.ticketNo)}">${esc(t.customerName || '未識別客戶')}</a></div>
          <div class="m">${esc(t.ticketNo)} · ${fmtDateTime(t.updatedAt)} · ${esc(t.assigneeId)}</div>
        </div>`).join('')}
      ${totalPages > 1 ? `
        <div class="row" style="margin-top:10px">
          <button class="btn btn-sm" type="button" id="h-prev" ${cur <= 0 ? 'disabled' : ''}>‹</button>
          <span class="muted small">第 ${cur + 1} / ${totalPages} 頁</span>
          <button class="btn btn-sm" type="button" id="h-next" ${cur + 1 >= totalPages ? 'disabled' : ''}>›</button>
        </div>` : ''}`;
    $('#h-prev')?.addEventListener('click', () => loadHistory(phone, cur - 1));
    $('#h-next')?.addEventListener('click', () => loadHistory(phone, cur + 1));
  } catch (err) {
    box.innerHTML = `<div class="error-text">${esc(err.message)}</div>`;
    toastError(err);
  }
}
