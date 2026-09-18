// ============================================================
// 建立工單的表單，兩個入口共用：
//   - 通話工作台「建立工單並結束通話」（mode = 'call'，多一顆「帶入進線號碼」）
//   - 首頁「＋ 新增派件」modal（mode = 'agent'）
// 欄位對應 dto/ticket/CreateTicketRequest.java
// ============================================================
import { api } from '../api.js';
import { getSession } from '../state.js';
import { $, $$, esc, CATEGORIES, TICKET_STATUS, assigneeOptionsHtml, toastError } from '../ui.js';

const HONORIFICS = ['先生', '小姐', '經理', '總', '同學', '（無）'];

/** 回傳表單的 HTML；mode='call' 時 phone 為進線號碼 */
export function ticketFormHtml({ mode, phone = '' }) {
  const isCall = mode === 'call';
  return `
    <form class="ticket-form stack" data-mode="${mode}" novalidate>
      <div class="field">
        <label>主旨 <span class="req">*</span></label>
        <input class="input" name="title" maxlength="50" placeholder="簡述客戶問題" required>
      </div>

      <div class="field">
        <label>姓名${isCall ? '（通話中向客戶確認）' : ''}</label>
        <div class="row">
          <select class="select" name="honorific" style="width:110px">
            ${HONORIFICS.map((h) => `<option value="${esc(h)}">${esc(h)}</option>`).join('')}
          </select>
          <input class="input grow" name="customerName" maxlength="200" placeholder="${isCall ? '尚未識別' : '客戶姓氏或姓名'}">
        </div>
      </div>

      <div class="field">
        <label>聯絡電話</label>
        <div class="row">
          <input class="input grow" name="contactPhone" maxlength="50" placeholder="客戶提供的聯絡電話">
          ${isCall ? `<button class="btn" type="button" data-act="fill-phone">✓ 帶入進線號碼</button>` : ''}
        </div>
      </div>

      <div class="field">
        <label>分類 <span class="req">*</span></label>
        <select class="select" name="category">
          ${CATEGORIES.map((c) => `<option value="${esc(c)}">${esc(c)}</option>`).join('')}
        </select>
      </div>

      <div class="field">
        <label class="check"><input type="checkbox" name="reassign"> 轉派給其他客服</label>
        <div class="row hidden" data-reassign-box>
          <select class="select grow" name="assigneeId"><option value="">載入中…</option></select>
        </div>
      </div>

      <div class="field">
        <label>${isCall ? '通話摘要（邊講邊記）' : '問題描述'}</label>
        <textarea class="textarea" name="description" placeholder="輸入處理內容..."></textarea>
      </div>

      <div class="field">
        <label>${isCall ? '通話結果' : '狀態'}</label>
        <div class="chips" data-status-chips>
          <button class="chip-btn ${isCall ? 'active' : ''}" type="button" data-status="RESOLVED">已解決</button>
          <button class="chip-btn ${isCall ? '' : 'active'}" type="button" data-status="IN_PROGRESS">${isCall ? '需再追蹤' : TICKET_STATUS.IN_PROGRESS}</button>
          <button class="chip-btn" type="button" data-status="PENDING">${TICKET_STATUS.PENDING}</button>
        </div>
      </div>

      <div class="error-text hidden" data-form-error></div>

      <button class="btn btn-primary btn-block" type="submit">
        ${isCall ? '✓ 建立工單並結束通話' : '建立工單'}
      </button>
    </form>`;
}

/**
 * 綁定表單行為。onSubmit(body) 回 Promise；成功與否由呼叫端處理。
 * @param {HTMLElement} formEl
 * @param {{ phone?: string, onSubmit: (body: object) => Promise<void> }} opts
 */
export function bindTicketForm(formEl, { phone = '', onSubmit }) {
  const fillBtn = $('[data-act="fill-phone"]', formEl);
  if (fillBtn) fillBtn.addEventListener('click', () => { formEl.contactPhone.value = phone; });

  const reassignBox = $('[data-reassign-box]', formEl);
  let agentsLoaded = false;
  formEl.reassign.addEventListener('change', async () => {
    reassignBox.classList.toggle('hidden', !formEl.reassign.checked);
    if (!formEl.reassign.checked) return;
    // 第一次勾選才去撈客服清單；自己不列（沒勾就是自己負責）
    if (!agentsLoaded) {
      try {
        const agents = await api.assignees();
        formEl.assigneeId.innerHTML = assigneeOptionsHtml(agents, [getSession()?.agentId]);
        agentsLoaded = true;
      } catch (err) {
        formEl.assigneeId.innerHTML = '<option value="">（客服清單載入失敗）</option>';
        toastError(err);
      }
    }
    formEl.assigneeId.focus();
  });

  $$('[data-status]', formEl).forEach((b) => b.addEventListener('click', () => {
    $$('[data-status]', formEl).forEach((x) => x.classList.remove('active'));
    b.classList.add('active');
  }));

  formEl.addEventListener('submit', async (e) => {
    e.preventDefault();
    const errEl = $('[data-form-error]', formEl);
    errEl.classList.add('hidden');

    const title = formEl.title.value.trim();
    if (!title) { showErr(errEl, '主旨為必填'); formEl.title.focus(); return; }

    const name = formEl.customerName.value.trim();
    const honorific = formEl.honorific.value;
    const customerName = name ? name + (honorific === '（無）' ? '' : honorific) : null;

    const phoneVal = formEl.contactPhone.value.trim();
    const assigneeId = formEl.reassign.checked ? formEl.assigneeId.value.trim() : '';
    if (formEl.reassign.checked && !assigneeId) { showErr(errEl, '請選擇要轉派的客服'); formEl.assigneeId.focus(); return; }

    const body = {
      title,
      customerName,
      contactPhone: phoneVal || null,
      category: formEl.category.value,
      assigneeId: assigneeId || null,
      description: formEl.description.value.trim() || null,
      status: $('[data-status].active', formEl).dataset.status,
    };

    const submitBtn = $('button[type="submit"]', formEl);
    submitBtn.disabled = true;
    try {
      await onSubmit(body);
    } catch (ex) {
      showErr(errEl, ex.message || '建立失敗');
    } finally {
      submitBtn.disabled = false;
    }
  });
}

function showErr(el, msg) {
  el.textContent = msg;
  el.classList.remove('hidden');
}
