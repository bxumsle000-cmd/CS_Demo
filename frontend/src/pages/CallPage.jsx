// ============================================================
// 通話工作台（對應 img/order.jpg）
// 左：本次通話工單表單 → POST /api/tickets/from-call
// 右：進線號碼 + 此號碼歷史紀錄 → GET /api/tickets?contactPhone=...
// ============================================================
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { useCall, startCall, hangUp, clearCall, saveCallDraft, randomIncomingNumber } from '../state.js';
import { fmtDateTime, readPage } from '../ui.js';
import { Page } from '../components/Topbar.jsx';
import TicketForm from '../components/TicketForm.jsx';
import { StatusTag, toast, toastError } from '../components/Feedback.jsx';

export default function CallPage() {
  const call = useCall();
  const navigate = useNavigate();

  if (!call) {
    return (
      <Page title="工作台" back="/">
        <div className="card" style={{ maxWidth: 520, margin: '40px auto', textAlign: 'center' }}>
          <div className="card-title">目前沒有進行中的通話</div>
          <p className="muted" style={{ margin: '0 0 16px' }}>接聽電話後，這裡會出現「本次通話工單」表單與進線號碼的歷史紀錄。</p>
          <button className="btn btn-primary" type="button"
                  onClick={() => { startCall(randomIncomingNumber()); toast('來電進線，已接聽'); }}>
            📞 接聽電話（模擬來電）
          </button>
        </div>
      </Page>
    );
  }

  const ended = !!call.endedAt;

  return (
    <Page title="工作台" back="/">
      <div className="layout-split">
        <div className="card">
          <div className="card-title">本次通話工單{ended && <span className="muted small">（通話已結束，請完成工單）</span>}</div>
          {/* key 綁開始時間：換了一通電話，表單就重新開始。
              表單內容存進 call.draft，離開工作台再回來、或重新整理，填到一半的內容都還在 */}
          <TicketForm
            key={call.startedAt}
            mode="call"
            phone={call.phone}
            callEnded={ended}
            initialDraft={call.draft}
            onDraftChange={saveCallDraft}
            onSubmit={async (body) => {
              await api.createTicketFromCall(body);
              hangUp();
              clearCall();
              toast(ended ? '工單已建立' : '工單已建立，通話已結束');
              navigate('/');
            }}
          />
        </div>
        <PhoneHistory key={call.phone} phone={call.phone} />
      </div>
    </Page>
  );
}

function PhoneHistory({ phone }) {
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let ignore = false; // 已離開頁面或換頁的話，丟掉這次結果
    api.searchTickets({ contactPhone: phone, page, size: 10 })
      .then((res) => { if (!ignore) { setResult(readPage(res)); setError(''); } })
      .catch((err) => { if (!ignore) { setError(err.message); toastError(err); } });
    return () => { ignore = true; };
  }, [phone, page]);

  const rows = result?.content ?? [];
  const known = rows.find((t) => t.customerName);
  const totalPages = Math.max(result?.totalPages ?? 1, 1);
  const cur = result?.number ?? 0;

  return (
    <div className="card">
      <div className="muted small">進線號碼</div>
      <div className="big-number">{phone}</div>
      <div className="muted small">
        {known ? `曾以「${known.customerName}」進線 · 通話中再次確認` : '號碼未識別 · 通話中確認身分'}
      </div>
      <div style={{ borderTop: '1px solid var(--border)', margin: '14px 0' }}></div>
      <div className="card-title">此號碼歷史紀錄{result ? `（${result.totalElements}）` : ''}</div>

      {error ? (
        <div className="error-text">{error}</div>
      ) : !result ? (
        <div className="loading">載入中…</div>
      ) : !rows.length ? (
        <div className="muted">此號碼沒有歷史工單</div>
      ) : (
        <>
          {rows.map((t) => (
            <div className="history-item" key={t.ticketNo}>
              <div className="t">
                <StatusTag status={t.status} />
                <Link to={`/tickets/${encodeURIComponent(t.ticketNo)}`}>{t.customerName || '未識別客戶'}</Link>
              </div>
              <div className="m">{t.ticketNo} · {fmtDateTime(t.updatedAt)} · {t.assigneeId}</div>
            </div>
          ))}
          {totalPages > 1 && (
            <div className="row" style={{ marginTop: 10 }}>
              <button className="btn btn-sm" type="button" disabled={cur <= 0} onClick={() => setPage(cur - 1)}>‹</button>
              <span className="muted small">第 {cur + 1} / {totalPages} 頁</span>
              <button className="btn btn-sm" type="button" disabled={cur + 1 >= totalPages} onClick={() => setPage(cur + 1)}>›</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
