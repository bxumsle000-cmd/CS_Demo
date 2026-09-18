// ============================================================
// 頂部列：標題列（含返回、使用者卡片與下拉選單）＋ 通話橫幅（有通話時才出現）
// ============================================================
import { api } from '../api.js';
import { getSession, patchSession, clearSession, getCall, endCall, toggleMute, onCallChange } from '../state.js';
import { $, $$, esc, AGENT_STATUS, MANUAL_AGENT_STATUSES, fmtDuration, toast, toastError, openModal, confirmDialog, withBusy } from '../ui.js';
import { navigate } from '../app.js';

let timerHandle = null;
let unsubscribeCall = null;

/**
 * 重新畫整個頂部列。
 * @param {{ title: string, back?: string }} opts  back = 返回連結的 hash（例如 '#/'）
 */
export function renderTopbar({ title, back }) {
  const root = $('#topbar');
  const s = getSession();
  root.innerHTML = `
    <div class="titlebar">
      <div class="titlebar-left">
        ${back ? `<a class="back" href="${esc(back)}">← ${esc(back === '#/' ? '返回首頁' : '返回')}</a>` : ''}
        <h1>${esc(title)}</h1>
      </div>
      <div class="titlebar-right">
        ${s ? userChipHtml(s) : ''}
      </div>
    </div>
    <div id="callbar-slot"></div>`;

  if (s) bindUserMenu(root);
  renderCallbar();

  // 通話狀態變化時只重畫橫幅與使用者卡片，不動整個頁面
  if (unsubscribeCall) unsubscribeCall();
  unsubscribeCall = onCallChange(() => {
    renderCallbar();
    refreshUserChip();
  });
}

// ---------------- 使用者卡片 ----------------
function displayStatus(s) {
  // 通話中由前端模擬：後端不允許手動設 ON_CALL，所以只在畫面上顯示
  return getCall() ? 'ON_CALL' : (s.status || 'ONLINE');
}

function userChipHtml(s) {
  const st = displayStatus(s);
  const initial = (s.name || s.agentId || '?').charAt(0);
  return `
    <div class="user-chip" id="user-chip">
      <button class="user-chip-btn" type="button" id="user-chip-btn">
        <div>
          <div class="uid">${esc(s.agentId)}</div>
          <div class="uname"><span class="dot ${esc(st)}"></span>${esc(s.name || '')} · ${esc(AGENT_STATUS[st] || st)}</div>
        </div>
        <div class="avatar">${esc(initial)}<span class="dot-badge dot ${esc(st)}"></span></div>
        <span class="muted">▾</span>
      </button>
    </div>`;
}

function refreshUserChip() {
  const s = getSession();
  const chip = $('#user-chip');
  if (!s || !chip) return;
  chip.outerHTML = userChipHtml(s);
  bindUserMenu($('#topbar'));
}

function bindUserMenu(root) {
  const btn = $('#user-chip-btn', root);
  if (!btn) return;
  btn.addEventListener('click', (e) => {
    e.stopPropagation();
    const existing = $('.user-menu', root);
    if (existing) { existing.remove(); return; }
    openUserMenu(root);
  });
}

function openUserMenu(root) {
  const s = getSession();
  const chip = $('#user-chip', root);
  const onCall = !!getCall();
  const current = displayStatus(s);
  const menu = document.createElement('div');
  menu.className = 'user-menu';
  menu.innerHTML = `
    ${onCall ? `<div class="note">通話中，狀態由系統自動控制<br>結束通話後會自動回到「線上」</div>` : ''}
    ${MANUAL_AGENT_STATUSES.map((st) => `
      <button class="item ${st === current ? 'active' : ''}" type="button" data-status="${st}" ${onCall ? 'disabled' : ''}>
        <span class="dot ${st}"></span>${esc(AGENT_STATUS[st])}
      </button>`).join('')}
    <div class="sep"></div>
    <button class="item" type="button" data-act="password">🔑 修改密碼</button>
    <button class="item" type="button" data-act="register">➕ 建立客服帳號</button>
    <div class="sep"></div>
    <button class="item" type="button" data-act="logout">🚪 登出</button>`;
  chip.appendChild(menu);

  const closeMenu = () => { menu.remove(); document.removeEventListener('click', onDoc); };
  const onDoc = (e) => { if (!menu.contains(e.target)) closeMenu(); };
  setTimeout(() => document.addEventListener('click', onDoc), 0);

  $$('[data-status]', menu).forEach((b) => b.addEventListener('click', async () => {
    const status = b.dataset.status;
    closeMenu();
    try {
      await api.updateMyStatus(status);
      patchSession({ status });
      refreshUserChip();
      toast(`狀態已改為「${AGENT_STATUS[status]}」`);
    } catch (err) { toastError(err); }
  }));

  $('[data-act="password"]', menu).addEventListener('click', () => { closeMenu(); openChangePasswordModal(); });
  $('[data-act="register"]', menu).addEventListener('click', () => { closeMenu(); openRegisterModal(); });
  $('[data-act="logout"]', menu).addEventListener('click', async () => {
    closeMenu();
    try { await api.logout(); } catch { /* 登出失敗也照樣清掉前端 session */ }
    endCall();
    clearSession();
    navigate('#/login');
  });
}

// ---------------- 修改密碼 / 建立帳號 ----------------
function openChangePasswordModal() {
  openModal({
    title: '修改密碼',
    narrow: true,
    body: `
      <form id="pw-form" class="stack">
        <div class="field"><label>舊密碼</label><input class="input" type="password" name="oldPassword" required autocomplete="current-password"></div>
        <div class="field"><label>新密碼</label><input class="input" type="password" name="newPassword" required autocomplete="new-password"></div>
        <div class="field"><label>再輸入一次新密碼</label><input class="input" type="password" name="confirm" required autocomplete="new-password"></div>
        <div class="error-text hidden" id="pw-err"></div>
        <div class="modal-foot">
          <button class="btn" type="button" data-act="cancel">取消</button>
          <button class="btn btn-primary" type="submit">儲存</button>
        </div>
      </form>`,
    onOpen(el, close) {
      $('[data-act="cancel"]', el).addEventListener('click', close);
      $('#pw-form', el).addEventListener('submit', async (e) => {
        e.preventDefault();
        const f = e.target;
        const err = $('#pw-err', el);
        err.classList.add('hidden');
        if (f.newPassword.value !== f.confirm.value) {
          err.textContent = '兩次輸入的新密碼不一致';
          err.classList.remove('hidden');
          return;
        }
        await withBusy($('button[type="submit"]', f), async () => {
          try {
            await api.changePassword(f.oldPassword.value, f.newPassword.value);
            toast('密碼已更新');
            close();
          } catch (ex) {
            err.textContent = ex.message;
            err.classList.remove('hidden');
          }
        });
      });
    },
  });
}

function openRegisterModal() {
  openModal({
    title: '建立客服帳號',
    narrow: true,
    body: `
      <form id="reg-form" class="stack">
        <div class="field"><label>客服代號</label><input class="input" name="agentId" placeholder="例如 CSC00004" required maxlength="10"></div>
        <div class="field"><label>姓名</label><input class="input" name="name" required maxlength="50"></div>
        <div class="field"><label>密碼</label><input class="input" type="password" name="password" required autocomplete="new-password"></div>
        <div class="error-text hidden" id="reg-err"></div>
        <div class="modal-foot">
          <button class="btn" type="button" data-act="cancel">取消</button>
          <button class="btn btn-primary" type="submit">建立</button>
        </div>
      </form>`,
    onOpen(el, close) {
      $('[data-act="cancel"]', el).addEventListener('click', close);
      $('#reg-form', el).addEventListener('submit', async (e) => {
        e.preventDefault();
        const f = e.target;
        const err = $('#reg-err', el);
        err.classList.add('hidden');
        await withBusy($('button[type="submit"]', f), async () => {
          try {
            await api.register(f.agentId.value.trim(), f.name.value.trim(), f.password.value);
            toast(`已建立帳號 ${f.agentId.value.trim()}`);
            close();
          } catch (ex) {
            err.textContent = ex.message;
            err.classList.remove('hidden');
          }
        });
      });
    },
  });
}

// ---------------- 通話橫幅 ----------------
function renderCallbar() {
  const slot = $('#callbar-slot');
  if (!slot) return;
  const call = getCall();
  clearInterval(timerHandle);
  if (!call) { slot.innerHTML = ''; return; }

  slot.innerHTML = `
    <div class="callbar">
      <div class="callbar-left" id="callbar-go" title="前往通話工作台">
        <div class="phone-icon">☎</div>
        <div>
          <div class="number">${esc(call.phone)}</div>
          <div class="sub">來電進線 · 號碼未識別</div>
        </div>
      </div>
      <div class="callbar-right">
        <span class="timer" id="call-timer">00:00</span>
        <button class="icon-btn ${call.muted ? 'mute-on' : ''}" type="button" id="call-mute" title="${call.muted ? '取消靜音' : '靜音'}">${call.muted ? '🔇' : '🔈'}</button>
        <button class="icon-btn hangup" type="button" id="call-hangup" title="結束通話">☎</button>
      </div>
    </div>`;

  const tick = () => {
    const el = $('#call-timer');
    if (el) el.textContent = fmtDuration(Math.floor((Date.now() - call.startedAt) / 1000));
  };
  tick();
  timerHandle = setInterval(tick, 1000);

  $('#callbar-go').addEventListener('click', () => navigate('#/call'));
  $('#call-mute').addEventListener('click', () => toggleMute());
  $('#call-hangup').addEventListener('click', async () => {
    const ok = await confirmDialog('要直接結束通話嗎？這通電話將不會建立工單。', { okText: '結束通話', danger: true });
    if (!ok) return;
    endCall();
    toast('通話已結束');
    if (location.hash.startsWith('#/call')) navigate('#/');
  });
}
