// ============================================================
// 工單詳情頁（對應 img/detail.png）
// GET /api/tickets/{no}；PUT .../status；PUT .../assignee；POST .../comments
// 每支寫入 API 都回完整的 TicketDetailResponse，直接拿回傳值更新 state，畫面就會重畫。
// ============================================================
import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { dash, fmtDateTime, TICKET_STATUS, CHANNEL } from '../ui.js';
import { Page } from '../components/Topbar.jsx';
import { AssigneeOptions, StatusTag, toast, toastError, useAssignees } from '../components/Feedback.jsx';

export default function DetailPage({ ticketNo }) {
  const [ticket, setTicket] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let ignore = false;
    api.ticketDetail(ticketNo)
      .then((t) => { if (!ignore) setTicket(t); })
      .catch((err) => { if (!ignore) setError(err.message); });
    return () => { ignore = true; };
  }, [ticketNo]);

  return (
    <Page title={ticketNo} back="/">
      {error ? (
        <div className="card"><div className="error-text">{error}</div><p><Link to="/">← 返回工單列表</Link></p></div>
      ) : !ticket ? (
        <div className="loading">載入中…</div>
      ) : (
        <TicketDetail t={ticket} onChange={setTicket} />
      )}
    </Page>
  );
}

function TicketDetail({ t, onChange }) {
  const [busyStatus, setBusyStatus] = useState(null);

  const setStatus = async (status) => {
    setBusyStatus(status);
    try {
      const next = await api.updateTicketStatus(t.ticketNo, status);
      toast(`狀態已改為「${TICKET_STATUS[next.status]}」`);
      onChange(next);
    } catch (err) { toastError(err); }
    setBusyStatus(null);
  };

  return (
    <div className="layout-split">
      <div>
        <div className="card">
          <div className="detail-title">{t.title}</div>
          <div className="detail-desc">{t.description || <span className="muted">（沒有問題描述）</span>}</div>
          <div className="detail-actions">
            <div className="row" style={{ flexWrap: 'wrap' }}>
              {Object.keys(TICKET_STATUS).filter((s) => s !== t.status).map((s) => (
                <button key={s} className="btn btn-sm" type="button" disabled={busyStatus !== null} onClick={() => setStatus(s)}>
                  → 標記為「{TICKET_STATUS[s]}」
                </button>
              ))}
            </div>
            {/* key 綁負責人：轉派成功後收起轉派區塊 */}
            <ReassignBox key={t.assigneeId} t={t} onChange={onChange} />
          </div>
        </div>

        <Comments t={t} onChange={onChange} />
      </div>

      <div className="card">
        <div className="info-list">
          <div><div className="k">狀態</div><div className="v"><StatusTag status={t.status} /></div></div>
          <div><div className="k">客服</div><div className="v">{t.assigneeId}</div></div>
          <div><div className="k">分類</div><div className="v">{dash(t.category)}</div></div>
          <div><div className="k">進線管道</div><div className="v">{t.channel === 'PHONE' ? '📞 ' : '👤 '}{CHANNEL[t.channel] || t.channel}</div></div>
          <div className="sep"></div>
          <div><div className="k">客戶</div><div className="v">{dash(t.customerName)}</div></div>
          <div><div className="k">聯絡電話</div><div className="v">{dash(t.contactPhone)}</div></div>
          <div><div className="k">建立時間</div><div className="v">{fmtDateTime(t.createdAt)}</div></div>
          <div><div className="k">單號</div><div className="v muted">{t.ticketNo}</div></div>
        </div>
      </div>
    </div>
  );
}

// ---------------- 轉派 ----------------
function ReassignBox({ t, onChange }) {
  const [open, setOpen] = useState(false);
  const [picked, setPicked] = useState('');
  const [busy, setBusy] = useState(false);
  // 第一次勾選才去撈客服清單；目前負責人不列
  const agents = useAssignees(open);
  const choices = Array.isArray(agents) ? agents.filter((a) => a.agentId !== t.assigneeId) : [];
  const assignId = choices.some((a) => a.agentId === picked) ? picked : (choices[0]?.agentId || '');

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!assignId) return;
    setBusy(true);
    try {
      const next = await api.reassignTicket(t.ticketNo, assignId);
      toast(`已轉派給 ${assignId}`);
      onChange(next);
    } catch (err) { toastError(err); }
    setBusy(false);
  };

  return (
    <>
      <label className="check"><input type="checkbox" checked={open} onChange={(e) => setOpen(e.target.checked)} /> 轉派給其他客服</label>
      {open && (
        <form className="row" onSubmit={onSubmit}>
          <select className="select grow" autoFocus value={assignId} onChange={(e) => setPicked(e.target.value)}>
            <AssigneeOptions agents={agents} exclude={[t.assigneeId]} />
          </select>
          <button className="btn btn-primary btn-sm" type="submit" disabled={busy}>轉派</button>
        </form>
      )}
    </>
  );
}

// ---------------- 處理記錄 ----------------
function Comments({ t, onChange }) {
  const [content, setContent] = useState('');
  const [busy, setBusy] = useState(false);
  const inputRef = useRef(null);

  const onSubmit = async (e) => {
    e.preventDefault();
    const text = content.trim();
    if (!text) { inputRef.current?.focus(); return; }
    setBusy(true);
    try {
      const next = await api.addComment(t.ticketNo, text);
      onChange(next);
      setContent(''); // 清空後保留焦點，方便連續輸入
    } catch (err) { toastError(err); }
    setBusy(false);
    inputRef.current?.focus();
  };

  // 畫面由新到舊，後端給的是舊到新，反過來排
  const comments = [...(t.comments || [])].reverse();

  return (
    <div className="card">
      <div className="card-title">處理記錄</div>
      <div className="comments">
        {!comments.length ? <div className="muted">尚無處理記錄</div> : comments.map((c, i) => {
          const isSys = !c.agentId;
          const who = isSys ? '系統' : `${c.agentName || ''} ${c.agentId}`.trim();
          return (
            <div className="comment" key={`${c.createdAt}-${i}`}>
              <div className={`avatar ${isSys ? 'sys' : ''}`}>{isSys ? '系' : (c.agentName || c.agentId).charAt(0)}</div>
              <div>
                <div className="head"><b>{who}</b><span className="muted">· {fmtDateTime(c.createdAt)}</span></div>
                <div className="body">{c.content}</div>
              </div>
            </div>
          );
        })}
      </div>
      <form className="row" onSubmit={onSubmit}>
        <input className="input grow" ref={inputRef} placeholder="新增處理記錄..." maxLength={2000} autoComplete="off"
               value={content} onChange={(e) => setContent(e.target.value)} />
        <button className="btn" type="submit" disabled={busy}>送出</button>
      </form>
    </div>
  );
}
