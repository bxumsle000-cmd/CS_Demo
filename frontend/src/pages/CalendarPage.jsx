// ============================================================
// 行事曆（對應 img/calendar.jpg）
// 左：月曆（週日起，6 列 42 格）→ GET /api/follow-ups?from&to（from/to = 第一格與最後一格）
// 右：選定日期的回電安排 + 加入案件 → GET /api/follow-ups/tickets、POST /api/follow-ups、DELETE /api/follow-ups/{id}
// ============================================================
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { dash, fmtTime, toDateKey, TICKET_STATUS } from '../ui.js';
import { Page } from '../components/Topbar.jsx';
import { StatusTag, toast, toastError, confirmDialog } from '../components/Feedback.jsx';

const DOW = ['日', '一', '二', '三', '四', '五', '六'];

/** 這個月月曆實際畫出來的 42 個 Date */
function gridDates(year, month) {
  const first = new Date(year, month, 1);
  const start = new Date(first);
  start.setDate(1 - first.getDay());
  return Array.from({ length: 42 }, (_, i) => {
    const d = new Date(start);
    d.setDate(start.getDate() + i);
    return d;
  });
}

export default function CalendarPage() {
  const today = new Date();
  const [ym, setYm] = useState({ year: today.getFullYear(), month: today.getMonth() }); // month: 0-11
  const [selected, setSelected] = useState(toDateKey(today));                             // yyyy-MM-dd
  const [byDate, setByDate] = useState(new Map());   // yyyy-MM-dd → FollowUpGetListResponse[]
  const [assignable, setAssignable] = useState(null); // 可加入的工單；null = 載入中
  const [reloadKey, setReloadKey] = useState(0);
  const reload = () => setReloadKey((k) => k + 1);

  const dates = useMemo(() => gridDates(ym.year, ym.month), [ym]);

  // ---- 撈這個月（42 格範圍）的回電安排 ----
  useEffect(() => {
    let ignore = false;
    api.followUps(toDateKey(dates[0]), toDateKey(dates[dates.length - 1]))
      .then((list) => {
        if (ignore) return;
        const map = new Map();
        for (const f of list) {
          const key = String(f.followUpAt).slice(0, 10); // "2026-09-14T10:30:00" → "2026-09-14"
          if (!map.has(key)) map.set(key, []);
          map.get(key).push(f);
        }
        for (const arr of map.values()) arr.sort((a, b) => String(a.followUpAt).localeCompare(String(b.followUpAt)));
        setByDate(map);
      })
      .catch((err) => { if (!ignore) { toastError(err); setByDate(new Map()); } });
    return () => { ignore = true; };
  }, [dates, reloadKey]);

  // ---- 撈可加入的工單（只需要一次） ----
  useEffect(() => {
    api.assignableTickets()
      .then(setAssignable)
      .catch((err) => { setAssignable([]); toastError(err); });
  }, []);

  const shiftMonth = (delta) => {
    const d = new Date(ym.year, ym.month + delta, 1);
    setYm({ year: d.getFullYear(), month: d.getMonth() });
  };

  const goToday = () => {
    const t = new Date();
    setYm({ year: t.getFullYear(), month: t.getMonth() });
    setSelected(toDateKey(t));
  };

  const todayKey = toDateKey(today);

  return (
    <Page title="行事曆" back="/">
      <div className="layout-split">
        <div className="card">
          <div className="cal-head">
            <div className="nav">
              <button className="btn btn-sm" type="button" onClick={() => shiftMonth(-1)}>‹</button>
              <span className="month">{ym.year} 年 {ym.month + 1} 月</span>
              <button className="btn btn-sm" type="button" onClick={() => shiftMonth(1)}>›</button>
            </div>
            <button className="btn btn-sm" type="button" onClick={goToday}>今天</button>
          </div>
          <div className="cal-grid">
            {DOW.map((d) => <div key={d} className="cal-dow">{d}</div>)}
            {dates.map((d) => {
              const key = toDateKey(d);
              const cnt = byDate.get(key)?.length || 0;
              const cls = [
                'cal-cell',
                d.getMonth() !== ym.month ? 'other' : '',
                key === todayKey ? 'today' : '',
                key === selected ? 'selected' : '',
              ].join(' ');
              return (
                <button key={key} className={cls} type="button" onClick={() => setSelected(key)}>
                  <span className="d">{d.getDate()}</span>
                  {cnt > 0 && <span className="cnt">{cnt} 件</span>}
                </button>
              );
            })}
          </div>
        </div>

        <DayPanel
          selected={selected}
          items={byDate.get(selected) || []}
          assignable={assignable}
          onChanged={reload}
        />
      </div>
    </Page>
  );
}

// ---------------- 右側：當天安排 + 加入案件 ----------------
function DayPanel({ selected, items, assignable, onChanged }) {
  const [ticketNo, setTicketNo] = useState('');
  const [time, setTime] = useState('09:00');
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);

  const d = new Date(selected + 'T00:00:00');
  const list = assignable || [];
  // 還沒選過（或選的已不在清單裡）就用第一個
  const pickedTicket = list.some((t) => t.ticketNo === ticketNo) ? ticketNo : (list[0]?.ticketNo || '');

  const remove = async (id) => {
    const ok = await confirmDialog('要刪除這筆回電安排嗎？', { okText: '刪除', danger: true });
    if (!ok) return;
    try {
      await api.deleteFollowUp(id);
      toast('已刪除');
      onChanged();
    } catch (err) { toastError(err); }
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!pickedTicket) { toast('沒有可加入的案件', 'err'); return; }
    setBusy(true);
    try {
      await api.createFollowUp({
        ticketNo: pickedTicket,
        followUpAt: `${selected}T${time || '09:00'}:00`,
        note: note.trim() || null,
      });
      toast('已加入行事曆');
      setNote('');
      onChanged();
    } catch (err) { toastError(err); }
    setBusy(false);
  };

  return (
    <div className="card">
      <div className="card-title" style={{ marginBottom: 2 }}>{d.getMonth() + 1} 月 {d.getDate()} 日（週{DOW[d.getDay()]}）</div>
      <div className="muted small" style={{ marginBottom: 12 }}>當日跟進 / 回電安排</div>
      <div>
        {!items.length ? <div className="muted">這天沒有安排</div> : items.map((f) => (
          <div className="fu-item" key={f.followUpId}>
            <div className="time">{fmtTime(f.followUpAt)}</div>
            <div className="main">
              <div className="t"><Link to={`/tickets/${encodeURIComponent(f.ticketNo)}`} style={{ color: 'inherit' }}>{f.title}</Link></div>
              <div className="m">{f.ticketNo} · {dash(f.customerName)} · {dash(f.contactPhone)}</div>
              {f.note && <div className="n">📝 {f.note}</div>}
            </div>
            <StatusTag status={f.status} />
            <button className="x" type="button" title="刪除" onClick={() => remove(f.followUpId)}>✕</button>
          </div>
        ))}
      </div>
      <div style={{ borderTop: '1px solid var(--border)', margin: '14px 0' }}></div>
      <div className="card-title">加入案件到這天</div>
      <form className="stack" onSubmit={onSubmit}>
        <select className="select" value={pickedTicket} onChange={(e) => setTicketNo(e.target.value)}>
          {assignable === null ? (
            <option value="">載入中…</option>
          ) : !list.length ? (
            <option value="">（目前沒有處理中 / 待客戶回覆的案件）</option>
          ) : list.map((t) => (
            <option key={t.ticketNo} value={t.ticketNo}>{t.ticketNo}｜{t.title}（{TICKET_STATUS[t.status] || t.status}）</option>
          ))}
        </select>
        <div className="row">
          <input className="input" type="time" required style={{ width: 130 }} value={time} onChange={(e) => setTime(e.target.value)} />
          <input className="input grow" placeholder="備註（選填，只有自己看得到）" maxLength={200} value={note} onChange={(e) => setNote(e.target.value)} />
          <button className="btn btn-primary" type="submit" disabled={busy}>加入</button>
        </div>
        <div className="muted small">只列出你「處理中 / 等待客戶回覆」的案件；同一張案件可以排多筆時間。</div>
      </form>
    </div>
  );
}
