// ============================================================
// 登入頁（對應 img/index.jpg）
// POST /api/auth/login → 存 token → GET /api/auth/me 補姓名與狀態 → 進首頁
// ============================================================
import { useState } from 'react';
import { api } from '../api.js';
import { setSession, patchSession } from '../state.js';

export default function LoginPage() {
  const [agentId, setAgentId] = useState('CSC00001');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const id = agentId.trim();
    if (!id || !password) {
      setError('請輸入客服代號與密碼');
      return;
    }
    setBusy(true);
    try {
      const res = await api.login(id, password);
      setSession({ agentId: res.agentId, token: res.token });
      // /me 會用剛拿到的 token 取回姓名與狀態
      try {
        const me = await api.me();
        patchSession({ agentId: me.agentId, name: me.name, status: me.status });
      } catch { /* /me 失敗不影響登入 */ }
      // 不用手動換頁：session 有值之後，GuestOnly 會自動導到首頁
    } catch (ex) {
      setError(ex.message);
      setBusy(false);
    }
  };

  return (
    <>
      <header id="topbar"></header>
      <main id="view">
        <div className="login-wrap">
          <div className="login-card">
            <div className="login-brand"><div className="logo">☎</div>電話客服工單系統</div>
            <form className="stack" noValidate onSubmit={onSubmit}>
              <div className="field">
                <label>客服代號</label>
                <input className="input" autoComplete="username" value={agentId} onChange={(e) => setAgentId(e.target.value)} />
              </div>
              <div className="field">
                <label>密碼</label>
                <input className="input" type="password" autoComplete="current-password" autoFocus
                       value={password} onChange={(e) => setPassword(e.target.value)} />
              </div>
              {error && <div className="error-text">{error}</div>}
              <button className="btn btn-primary btn-block" type="submit" disabled={busy}>{busy ? '登入中…' : '登入'}</button>
            </form>
            <div className="login-hint">開發用帳號：CSC00001 ～ CSC00003<br />密碼皆為 pass1234</div>
          </div>
        </div>
      </main>
    </>
  );
}
