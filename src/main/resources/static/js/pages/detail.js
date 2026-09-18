// ============================================================
// 工單詳情頁（對應 img/detail.png）
// GET /api/tickets/{no}；PUT .../status；PUT .../assignee；POST .../comments
// 每支寫入 API 都回完整的 TicketDetailResponse，直接拿回傳值重畫即可。
// ============================================================
import { api } from '../api.js';
import { $, $$, esc, dash, statusTag, fmtDateTime, toast, toastError, withBusy, TICKET_STATUS, CHANNEL, assigneeOptionsHtml } from '../ui.js';
import { renderTopbar } from '../components/topbar.js';

export function render(container, { ticketNo }) {
  renderTopbar({ title: ticketNo, back: '#/' });
  container.innerHTML = `<div class="loading">載入中…</div>`;
  api.ticketDetail(ticketNo)
    .then((t) => paint(container, t))
    .catch((err) => {
      container.innerHTML = `<div class="card"><div class="error-text">${esc(err.message)}</div><p><a href="#/">← 返回工單列表</a></p></div>`;
    });
}

function paint(container, t) {
  const statusButtons = Object.keys(TICKET_STATUS)
    .filter((s) => s !== t.status)
    .map((s) => `<button class="btn btn-sm" type="button" data-set-status="${s}">→ 標記為「${TICKET_STATUS[s]}」</button>`)
    .join('');

  container.innerHTML = `
    <div class="layout-split">
      <div>
        <div class="card">
          <div class="detail-title">${esc(t.title)}</div>
          <div class="detail-desc">${t.description ? esc(t.description) : '<span class="muted">（沒有問題描述）</span>'}</div>
          <div class="detail-actions">
            <div class="row" style="flex-wrap:wrap">${statusButtons}</div>
            <label class="check"><input type="checkbox" id="reassign-toggle"> 轉派給其他客服</label>
            <form class="row hidden" id="reassign-form">
              <select class="select grow" name="assignId"><option value="">載入中…</option></select>
              <button class="btn btn-primary btn-sm" type="submit">轉派</button>
            </form>
          </div>
        </div>

        <div class="card">
          <div class="card-title">處理記錄</div>
          <div class="comments" id="comments">${commentsHtml(t.comments)}</div>
          <form class="row" id="comment-form">
            <input class="input grow" name="content" placeholder="新增處理記錄..." maxlength="2000" autocomplete="off">
            <button class="btn" type="submit">送出</button>
          </form>
        </div>
      </div>

      <div class="card">
        <div class="info-list">
          <div><div class="k">狀態</div><div class="v">${statusTag(t.status)}</div></div>
          <div><div class="k">客服</div><div class="v">${esc(t.assigneeId)}</div></div>
          <div><div class="k">分類</div><div class="v">${dash(t.category)}</div></div>
          <div><div class="k">進線管道</div><div class="v">${t.channel === 'PHONE' ? '📞 ' : '👤 '}${esc(CHANNEL[t.channel] || t.channel)}</div></div>
          <div class="sep"></div>
          <div><div class="k">客戶</div><div class="v">${dash(t.customerName)}</div></div>
          <div><div class="k">聯絡電話</div><div class="v">${dash(t.contactPhone)}</div></div>
          <div><div class="k">建立時間</div><div class="v">${fmtDateTime(t.createdAt)}</div></div>
          <div><div class="k">單號</div><div class="v muted">${esc(t.ticketNo)}</div></div>
        </div>
      </div>
    </div>`;

  // ---- 變更狀態 ----
  $$('[data-set-status]').forEach((b) => b.addEventListener('click', () => withBusy(b, async () => {
    try {
      const next = await api.updateTicketStatus(t.ticketNo, b.dataset.setStatus);
      toast(`狀態已改為「${TICKET_STATUS[next.status]}」`);
      paint(container, next);
    } catch (err) { toastError(err); }
  })));

  // ---- 轉派 ----
  const toggle = $('#reassign-toggle');
  const rform = $('#reassign-form');
  let agentsLoaded = false;
  toggle.addEventListener('change', async () => {
    rform.classList.toggle('hidden', !toggle.checked);
    if (!toggle.checked) return;
    // 第一次勾選才去撈客服清單；目前負責人不列
    if (!agentsLoaded) {
      try {
        const agents = await api.assignees();
        rform.assignId.innerHTML = assigneeOptionsHtml(agents, [t.assigneeId]);
        agentsLoaded = true;
      } catch (err) {
        rform.assignId.innerHTML = '<option value="">（客服清單載入失敗）</option>';
        toastError(err);
      }
    }
    rform.assignId.focus();
  });
  rform.addEventListener('submit', async (e) => {
    e.preventDefault();
    const assignId = rform.assignId.value.trim();
    if (!assignId) { rform.assignId.focus(); return; }
    await withBusy($('button', rform), async () => {
      try {
        const next = await api.reassignTicket(t.ticketNo, assignId);
        toast(`已轉派給 ${assignId}`);
        paint(container, next);
      } catch (err) { toastError(err); }
    });
  });

  // ---- 新增處理記錄 ----
  const cform = $('#comment-form');
  cform.addEventListener('submit', async (e) => {
    e.preventDefault();
    const content = cform.content.value.trim();
    if (!content) { cform.content.focus(); return; }
    await withBusy($('button', cform), async () => {
      try {
        const next = await api.addComment(t.ticketNo, content);
        // 只換留言區，輸入框清空後保留焦點方便連續輸入
        $('#comments').innerHTML = commentsHtml(next.comments);
        cform.content.value = '';
        cform.content.focus();
        t.comments = next.comments;
      } catch (err) { toastError(err); }
    });
  });
}

function commentsHtml(comments) {
  if (!comments || !comments.length) return `<div class="muted">尚無處理記錄</div>`;
  // 畫面由新到舊，後端給的是舊到新，反過來排
  return [...comments].reverse().map((c) => {
    const isSys = !c.agentId;
    const who = isSys ? '系統' : `${c.agentName || ''} ${c.agentId}`.trim();
    return `
      <div class="comment">
        <div class="avatar ${isSys ? 'sys' : ''}">${isSys ? '系' : esc((c.agentName || c.agentId).charAt(0))}</div>
        <div>
          <div class="head"><b>${esc(who)}</b><span class="muted">· ${fmtDateTime(c.createdAt)}</span></div>
          <div class="body">${esc(c.content)}</div>
        </div>
      </div>`;
  }).join('');
}
