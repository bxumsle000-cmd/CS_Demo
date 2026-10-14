// ============================================================
// 首頁：工單列表（對應 img/work1~3.jpg）
// GET /api/tickets?ticketNo&customerName&contactPhone&status&assigneeId&updatedFrom&updatedTo&page&size
// ============================================================
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { getSession, useCall, startCall, randomIncomingNumber } from '../state.js';
import { dash, fmtDateTime, readPage, TICKET_STATUS } from '../ui.js';
import { Page } from '../components/Topbar.jsx';
import TicketForm from '../components/TicketForm.jsx';
import { Modal, StatusTag, toast, toastError } from '../components/Feedback.jsx';

const TABS = [
  { key: '', label: '全部' },
  { key: 'RESOLVED', label: '已處理' },
  { key: 'IN_PROGRESS', label: '處理中' },
  { key: 'PENDING', label: '等待客戶回覆' },
];

const emptyFilters = (agentId) => ({
  ticketNo: '',
  customerName: '',
  contactPhone: '',
  status: '',
  assigneeId: agentId || '', // 預設只看自己的工單
  updatedFrom: '',           // yyyy-MM-dd，依更新時間過濾（含當天）
  updatedTo: '',             // yyyy-MM-dd（含當天）
});

// 篩選條件存在元件外面：從詳情頁返回時還會記得剛才的查詢
let saved = null;

/** value 停止變動 ms 毫秒後才回傳新值（打字時不要每按一個鍵就查一次） */
function useDebounced(value, ms) {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return v;
}

export default function HomePage() {
  const navigate = useNavigate();
  const call = useCall();
  const [filters, setFilters] = useState(() => saved?.filters ?? emptyFilters(getSession()?.agentId));
  const [page, setPage] = useState(saved?.page ?? 0);
  const [size, setSize] = useState(saved?.size ?? 10);
  const [result, setResult] = useState(null);   // readPage() 的結果；null = 載入中
  const [loadError, setLoadError] = useState('');
  const [reloadKey, setReloadKey] = useState(0); // +1 就會重新查詢
  const [showNew, setShowNew] = useState(false);

  useEffect(() => { saved = { filters, page, size }; }, [filters, page, size]);

  // 文字欄位延遲 350ms 才查詢；下拉選單和日期立即生效
  const textKey = useDebounced(
    JSON.stringify([filters.ticketNo, filters.customerName, filters.contactPhone, filters.assigneeId]), 350);
  const { status, updatedFrom, updatedTo } = filters;

  useEffect(() => {
    if (updatedFrom && updatedTo && updatedFrom > updatedTo) {
      toast('起始日期不能晚於結束日期', 'err');
      return;
    }
    const [ticketNo, customerName, contactPhone, assigneeId] = JSON.parse(textKey);
    let ignore = false; // 條件又變了的話，丟掉這次（較舊）的結果
    api.searchTickets({
      ticketNo: ticketNo.trim(), customerName: customerName.trim(), contactPhone: contactPhone.trim(),
      assigneeId: assigneeId.trim(), status, updatedFrom, updatedTo, page, size,
    })
      .then((res) => { if (!ignore) { setResult(readPage(res)); setLoadError(''); } })
      .catch((err) => { if (!ignore) { setLoadError(err.message); toastError(err); } });
    return () => { ignore = true; };
  }, [textKey, status, updatedFrom, updatedTo, page, size, reloadKey]);

  /** 改任何篩選條件都回到第 1 頁 */
  const setFilter = (name, value) => {
    setFilters((f) => ({ ...f, [name]: value }));
    setPage(0);
  };
  const bind = (name) => ({ name, value: filters[name], onChange: (e) => setFilter(name, e.target.value) });

  const answer = () => {
    if (!call) {
      startCall(randomIncomingNumber());
      toast('來電進線，已接聽');
    }
    navigate('/call');
  };

  const totalPages = Math.max(result?.totalPages ?? 0, 1);
  const cur = result?.number ?? page;

  return (
    <Page title="首頁">
      <div className="toolbar">
        <div className="toolbar-left">
          <button className="btn" type="button" onClick={() => setShowNew(true)}>＋ 新增派件</button>
          <button className="btn btn-primary" type="button" onClick={answer}>
            {/* 上一通掛斷了但工單還沒送出：先寫完才能接下一通 */}
            {!call ? '📞 接聽電話' : call.endedAt ? '📝 完成上一通工單' : '📞 回到通話工作台'}
          </button>
        </div>
        <div className="toolbar-right">
          <Link className="btn" to="/calendar">📅 行事曆</Link>
        </div>
      </div>

      <div className="tabs">
        {TABS.map((t) => (
          <button key={t.key} className={`tab ${status === t.key ? 'active' : ''}`} type="button"
                  onClick={() => setFilter('status', t.key)}>
            {t.label}
          </button>
        ))}
      </div>

      <div className="card">
        <form className="filters" autoComplete="off" onSubmit={(e) => e.preventDefault()}>
          <div className="field"><label>單號</label><input className="input" placeholder="全部" {...bind('ticketNo')} /></div>
          <div className="field"><label>姓名</label><input className="input" placeholder="全部" {...bind('customerName')} /></div>
          <div className="field"><label>電話</label><input className="input" placeholder="全部" {...bind('contactPhone')} /></div>
          <div className="field"><label>狀態</label>
            <select className="select" {...bind('status')}>
              <option value="">全部</option>
              {Object.entries(TICKET_STATUS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </div>
          <div className="field"><label>客服</label><input className="input" placeholder="全部" {...bind('assigneeId')} /></div>
          <div className="field"><label>更新時間（起）</label><input className="input" type="date" {...bind('updatedFrom')} /></div>
          <div className="field"><label>更新時間（迄）</label><input className="input" type="date" {...bind('updatedTo')} /></div>
          <div className="field"><label>&nbsp;</label>
            <button className="btn" type="button" onClick={() => { setFilters(emptyFilters(getSession()?.agentId)); setPage(0); }}>重設</button>
          </div>
        </form>
      </div>

      <div className="card" style={{ marginTop: 12 }}>
        <div className="table-wrap">
          <table className="tbl">
            <thead><tr><th>單號</th><th>姓名</th><th>電話</th><th>狀態</th><th>客服</th><th>更新時間</th></tr></thead>
            <tbody>
              {loadError ? (
                <tr><td colSpan={6} className="empty">{loadError}</td></tr>
              ) : !result ? (
                <tr><td colSpan={6} className="loading">載入中…</td></tr>
              ) : !result.content.length ? (
                <tr><td colSpan={6} className="empty">沒有符合條件的工單</td></tr>
              ) : result.content.map((t) => (
                <tr key={t.ticketNo}>
                  <td><Link to={`/tickets/${encodeURIComponent(t.ticketNo)}`}>{t.ticketNo}</Link></td>
                  <td><b>{dash(t.customerName)}</b></td>
                  <td>{dash(t.contactPhone)}</td>
                  <td><StatusTag status={t.status} /></td>
                  <td>{t.assigneeId}</td>
                  <td className="muted">{fmtDateTime(t.updatedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {result && (
          <div className="pager">
            <div className="pager-left">
              <span className="muted">每頁</span>
              <select className="select" value={size} onChange={(e) => { setSize(Number(e.target.value)); setPage(0); }}>
                {[10, 20, 50].map((n) => <option key={n} value={n}>{n}</option>)}
              </select>
              <span className="muted">筆 · 共 {result.totalElements} 筆</span>
            </div>
            <div className="pager-right">
              <span className="muted">第 {cur + 1} / {totalPages} 頁</span>
              <button className="btn btn-sm" type="button" disabled={cur <= 0} onClick={() => setPage(Math.max(0, cur - 1))}>‹</button>
              <button className="btn btn-sm" type="button" disabled={cur + 1 >= totalPages} onClick={() => setPage(cur + 1)}>›</button>
            </div>
          </div>
        )}
      </div>

      {showNew && (
        <Modal title="新增派件" onClose={() => setShowNew(false)}>
          <TicketForm
            mode="agent"
            onSubmit={async (body) => {
              await api.createTicketFromAgent(body);
              toast('工單已建立');
              setShowNew(false);
              setPage(0);
              setReloadKey((k) => k + 1);
            }}
          />
        </Modal>
      )}
    </Page>
  );
}
