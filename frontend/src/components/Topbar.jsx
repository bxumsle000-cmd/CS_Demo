// ============================================================
// 頂部列：標題列（含返回、使用者卡片與下拉選單）＋ 通話橫幅（有通話時才出現）
// <Page> 是每一頁的外框：上面頂部列、下面頁面內容。
// ============================================================
import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { useSession, patchSession, clearSession, getCall, useCall, hangUp, isOnCall, toggleMute } from '../state.js';
import { AGENT_STATUS, MANUAL_AGENT_STATUSES, fmtDuration } from '../ui.js';
import { toast, toastError, Modal } from './Feedback.jsx';

/**
 * 每一頁的外框。
 * @param {{ title: string, back?: string, children }} props  back = 返回連結的路徑（例如 '/'）
 */
export function Page({ title, back, children }) {
  return (
    <>
      <header id="topbar">
        <Topbar title={title} back={back} />
      </header>
      <main id="view">{children}</main>
    </>
  );
}

function Topbar({ title, back }) {
  const session = useSession();
  return (
    <>
      <div className="titlebar">
        <div className="titlebar-left">
          {back && <Link className="back" to={back}>← {back === '/' ? '返回首頁' : '返回'}</Link>}
          <h1>{title}</h1>
        </div>
        <div className="titlebar-right">
          {session && <UserChip session={session} />}
        </div>
      </div>
      <Callbar />
    </>
  );
}

// ---------------- 使用者卡片 ----------------
function UserChip({ session }) {
  const call = useCall();
  const [menuOpen, setMenuOpen] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const closeMenu = useCallback(() => setMenuOpen(false), []);

  // 通話中由前端模擬：後端不允許手動設 ON_CALL，所以只在畫面上顯示
  const onCall = isOnCall(call);
  const st = onCall ? 'ON_CALL' : (session.status || 'ONLINE');
  const initial = (session.name || session.agentId || '?').charAt(0);

  return (
    <div className="user-chip">
      <button className="user-chip-btn" type="button" onClick={() => setMenuOpen((v) => !v)}>
        <div>
          <div className="uid">{session.agentId}</div>
          <div className="uname"><span className={`dot ${st}`}></span>{session.name || ''} · {AGENT_STATUS[st] || st}</div>
        </div>
        <div className="avatar">{initial}<span className={`dot-badge dot ${st}`}></span></div>
        <span className="muted">▾</span>
      </button>

      {menuOpen && (
        <UserMenu
          current={st}
          onCall={onCall}
          onClose={closeMenu}
          onOpenPassword={() => setShowPassword(true)}
        />
      )}
      {showPassword && <ChangePasswordModal onClose={() => setShowPassword(false)} />}
    </div>
  );
}

function UserMenu({ current, onCall, onClose, onOpenPassword }) {
  const menuRef = useRef(null);

  // 點選單以外的地方就關閉。用 setTimeout 延後註冊，避免「打開選單的那一下點擊」馬上又把它關掉
  useEffect(() => {
    const onDoc = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) onClose();
    };
    const t = setTimeout(() => document.addEventListener('click', onDoc), 0);
    return () => { clearTimeout(t); document.removeEventListener('click', onDoc); };
  }, [onClose]);

  const navigate = useNavigate();

  const changeStatus = async (status) => {
    onClose();
    try {
      await api.updateMyStatus(status);
      patchSession({ status });
      toast(`狀態已改為「${AGENT_STATUS[status]}」`);
    } catch (err) { toastError(err); }
  };

  const logout = async () => {
    onClose();
    // 每通電話都一定要建立工單：還有電話（通話中或已掛斷未送出）就不能登出
    if (getCall()) {
      toast('請先完成這通電話的工單再登出', 'err');
      navigate('/call');
      return;
    }
    try { await api.logout(); } catch { /* 登出失敗也照樣清掉前端 session */ }
    clearSession(); // session 變成 null，RequireAuth 會自動導回登入頁
  };

  return (
    <div className="user-menu" ref={menuRef}>
      {onCall && <div className="note">通話中，狀態由系統自動控制<br />結束通話後會自動回到「線上」</div>}
      {MANUAL_AGENT_STATUSES.map((st) => (
        <button key={st} className={`item ${st === current ? 'active' : ''}`} type="button" disabled={onCall}
                onClick={() => changeStatus(st)}>
          <span className={`dot ${st}`}></span>{AGENT_STATUS[st]}
        </button>
      ))}
      <div className="sep"></div>
      <button className="item" type="button" onClick={() => { onClose(); onOpenPassword(); }}>🔑 修改密碼</button>
      <div className="sep"></div>
      <button className="item" type="button" onClick={logout}>🚪 登出</button>
    </div>
  );
}

// ---------------- 修改密碼 ----------------
function ChangePasswordModal({ onClose }) {
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    const f = e.target;
    setError('');
    if (f.newPassword.value !== f.confirm.value) {
      setError('兩次輸入的新密碼不一致');
      return;
    }
    setBusy(true);
    try {
      await api.changePassword(f.oldPassword.value, f.newPassword.value);
      toast('密碼已更新');
      onClose();
    } catch (ex) {
      setError(ex.message);
      setBusy(false);
    }
  };

  return (
    <Modal title="修改密碼" narrow onClose={onClose}>
      <form className="stack" onSubmit={onSubmit}>
        <div className="field"><label>舊密碼</label><input className="input" type="password" name="oldPassword" required autoComplete="current-password" /></div>
        <div className="field"><label>新密碼</label><input className="input" type="password" name="newPassword" required autoComplete="new-password" /></div>
        <div className="field"><label>再輸入一次新密碼</label><input className="input" type="password" name="confirm" required autoComplete="new-password" /></div>
        {error && <div className="error-text">{error}</div>}
        <div className="modal-foot">
          <button className="btn" type="button" onClick={onClose}>取消</button>
          <button className="btn btn-primary" type="submit" disabled={busy}>儲存</button>
        </div>
      </form>
    </Modal>
  );
}

// ---------------- 通話橫幅 ----------------
function Callbar() {
  const call = useCall();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [now, setNow] = useState(Date.now());

  const ended = !!call?.endedAt;

  // 通話中每秒更新一次，讓計時器跳動；掛斷後計時停住，不用再更新
  useEffect(() => {
    if (!call || ended) return;
    const h = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(h);
  }, [call, ended]);

  if (!call) return null;

  // 掛斷只是結束通話，工單表單保留，帶到工作台繼續寫
  const hangup = () => {
    hangUp();
    toast('通話已結束，請繼續完成工單');
    if (!pathname.startsWith('/call')) navigate('/call');
  };

  const seconds = Math.max(0, Math.floor(((call.endedAt || now) - call.startedAt) / 1000));

  return (
    <div className={`callbar ${ended ? 'ended' : ''}`}>
      <div className="callbar-left" title="前往通話工作台" onClick={() => navigate('/call')}>
        <div className="phone-icon">☎</div>
        <div>
          <div className="number">{call.phone}</div>
          <div className="sub">{ended ? '通話已結束 · 工單尚未送出' : '來電進線 · 號碼未識別'}</div>
        </div>
      </div>
      <div className="callbar-right">
        <span className="timer">{fmtDuration(seconds)}</span>
        {/* 掛斷後沒有任何按鈕：一定要把工單送出，這通電話才會結束 */}
        {!ended && (
          <>
            <button className={`icon-btn ${call.muted ? 'mute-on' : ''}`} type="button" title={call.muted ? '取消靜音' : '靜音'} onClick={toggleMute}>
              {call.muted ? '🔇' : '🔈'}
            </button>
            <button className="icon-btn hangup" type="button" title="結束通話（掛斷後仍須完成工單）" onClick={hangup}>☎</button>
          </>
        )}
      </div>
    </div>
  );
}
