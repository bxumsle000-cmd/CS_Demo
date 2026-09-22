// ============================================================
// 登入頁（對應 img/index.jpg）
// POST /api/auth/login → 存 token → GET /api/auth/me 補姓名與狀態 → 進首頁
// ============================================================
import { api } from '../api.js';
import { setSession, patchSession } from '../state.js';
import { $, esc } from '../ui.js';
import { navigate } from '../app.js';

export function render(container) {
  $('#topbar').innerHTML = '';
  container.innerHTML = `
    <div class="login-wrap">
      <div class="login-card">
        <div class="login-brand"><div class="logo">☎</div>電話客服工單系統</div>
        <form id="login-form" class="stack" novalidate>
          <div class="field">
            <label>客服代號</label>
            <input class="input" name="agentId" value="CSC00001" autocomplete="username" required>
          </div>
          <div class="field">
            <label>密碼</label>
            <input class="input" type="password" name="password" autocomplete="current-password" required>
          </div>
          <div class="error-text hidden" id="login-err"></div>
          <button class="btn btn-primary btn-block" type="submit">登入</button>
        </form>
        <div class="login-hint">開發用帳號：CSC00001 ～ CSC00003<br>密碼皆為 pass1234</div>
      </div>
    </div>`;

  const form = $('#login-form');
  form.password.focus();
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const err = $('#login-err');
    err.classList.add('hidden');
    const agentId = form.agentId.value.trim();
    const password = form.password.value;
    if (!agentId || !password) {
      err.textContent = '請輸入客服代號與密碼';
      err.classList.remove('hidden');
      return;
    }
    const btn = $('button[type="submit"]', form);
    btn.disabled = true;
    btn.textContent = '登入中…';
    try {
      const res = await api.login(agentId, password);
      setSession({ agentId: res.agentId, token: res.token });
      // /me 會用剛拿到的 token 取回姓名與狀態
      try {
        const me = await api.me();
        patchSession({ agentId: me.agentId, name: me.name, status: me.status });
      } catch { /* /me 失敗不影響登入 */ }
      navigate('#/');
    } catch (ex) {
      err.textContent = esc(ex.message);
      err.classList.remove('hidden');
      btn.disabled = false;
      btn.textContent = '登入';
    }
  });
}
