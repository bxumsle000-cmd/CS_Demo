// ============================================================
// 建立工單的表單，兩個入口共用：
//   - 通話工作台「建立工單並結束通話」（mode = 'call'，多一顆「帶入進線號碼」）
//   - 首頁「＋ 新增派件」modal（mode = 'agent'）
// 欄位對應 dto/ticket/CreateTicketRequest.java
// ============================================================
import { useEffect, useState } from 'react';
import { useSession } from '../state.js';
import { CATEGORIES, TICKET_STATUS } from '../ui.js';
import { AssigneeOptions, useAssignees } from './Feedback.jsx';

const HONORIFICS = ['先生', '小姐', '經理', '總', '同學', '（無）'];

/**
 * @param {{ mode: 'call'|'agent', phone?: string, callEnded?: boolean,
 *           initialDraft?: object, onDraftChange?: (form: object) => void,
 *           onSubmit: (body: object) => Promise<void> }} props
 *   callEnded     通話已掛斷，只剩表單要寫（按鈕不再寫「並結束通話」）
 *   initialDraft  上次填到一半的內容；onDraftChange 每次修改都會被呼叫，讓外面存起來
 *   onSubmit 丟出例外時，錯誤訊息會顯示在表單上
 */
export default function TicketForm({ mode, phone = '', callEnded = false, initialDraft, onDraftChange, onSubmit }) {
  const isCall = mode === 'call';
  const session = useSession();

  // 受控元件（controlled component）：輸入框的值存在 state，畫面跟著 state 走
  const [form, setForm] = useState(() => ({
    title: '',
    honorific: HONORIFICS[0],
    customerName: '',
    contactPhone: '',
    category: CATEGORIES[0],
    reassign: false,
    assigneeId: '',
    description: '',
    status: isCall ? 'RESOLVED' : 'IN_PROGRESS',
    ...initialDraft,
  }));
  const [error, setError] = useState('');

  // 表單一有變動就通知外面存草稿
  useEffect(() => { onDraftChange?.(form); }, [form, onDraftChange]);
  const [busy, setBusy] = useState(false);

  // 第一次勾「轉派」才去撈客服清單
  const agents = useAssignees(form.reassign);
  const set = (name) => (e) => setForm((f) => ({ ...f, [name]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));

  // 清單載入後，下拉選單預設選第一個（自己不列）
  const selectableAgents = Array.isArray(agents) ? agents.filter((a) => a.agentId !== session?.agentId) : [];
  const assigneeId = form.assigneeId || selectableAgents[0]?.agentId || '';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const title = form.title.trim();
    if (!title) { setError('主旨為必填'); return; }

    const name = form.customerName.trim();
    const customerName = name ? name + (form.honorific === '（無）' ? '' : form.honorific) : null;

    if (form.reassign && !assigneeId) { setError('請選擇要轉派的客服'); return; }

    const body = {
      title,
      customerName,
      contactPhone: form.contactPhone.trim() || null,
      category: form.category,
      assigneeId: form.reassign ? assigneeId : null,
      description: form.description.trim() || null,
      status: form.status,
    };

    setBusy(true);
    try {
      await onSubmit(body);
    } catch (ex) {
      setError(ex.message || '建立失敗');
    } finally {
      setBusy(false);
    }
  };

  const statusChips = [
    { key: 'RESOLVED', label: '已解決' },
    { key: 'IN_PROGRESS', label: isCall ? '需再追蹤' : TICKET_STATUS.IN_PROGRESS },
    { key: 'PENDING', label: TICKET_STATUS.PENDING },
  ];

  return (
    <form className="ticket-form stack" noValidate onSubmit={handleSubmit}>
      <div className="field">
        <label>主旨 <span className="req">*</span></label>
        <input className="input" maxLength={50} placeholder="簡述客戶問題" value={form.title} onChange={set('title')} />
      </div>

      <div className="field">
        <label>姓名{isCall ? '（通話中向客戶確認）' : ''}</label>
        <div className="row">
          <select className="select" style={{ width: 110 }} value={form.honorific} onChange={set('honorific')}>
            {HONORIFICS.map((h) => <option key={h} value={h}>{h}</option>)}
          </select>
          <input className="input grow" maxLength={200} placeholder={isCall ? '尚未識別' : '客戶姓氏或姓名'}
                 value={form.customerName} onChange={set('customerName')} />
        </div>
      </div>

      <div className="field">
        <label>聯絡電話</label>
        <div className="row">
          <input className="input grow" maxLength={50} placeholder="客戶提供的聯絡電話"
                 value={form.contactPhone} onChange={set('contactPhone')} />
          {isCall && (
            <button className="btn" type="button" onClick={() => setForm((f) => ({ ...f, contactPhone: phone }))}>
              ✓ 帶入進線號碼
            </button>
          )}
        </div>
      </div>

      <div className="field">
        <label>分類 <span className="req">*</span></label>
        <select className="select" value={form.category} onChange={set('category')}>
          {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
      </div>

      <div className="field">
        <label className="check"><input type="checkbox" checked={form.reassign} onChange={set('reassign')} /> 轉派給其他客服</label>
        {form.reassign && (
          <div className="row">
            <select className="select grow" autoFocus value={assigneeId} onChange={set('assigneeId')}>
              <AssigneeOptions agents={agents} exclude={[session?.agentId]} />
            </select>
          </div>
        )}
      </div>

      <div className="field">
        <label>{isCall ? '通話摘要（邊講邊記）' : '問題描述'}</label>
        <textarea className="textarea" placeholder="輸入處理內容..." value={form.description} onChange={set('description')} />
      </div>

      <div className="field">
        <label>{isCall ? '通話結果' : '狀態'}</label>
        <div className="chips">
          {statusChips.map((c) => (
            <button key={c.key} className={`chip-btn ${form.status === c.key ? 'active' : ''}`} type="button"
                    onClick={() => setForm((f) => ({ ...f, status: c.key }))}>
              {c.label}
            </button>
          ))}
        </div>
      </div>

      {error && <div className="error-text">{error}</div>}

      <button className="btn btn-primary btn-block" type="submit" disabled={busy}>
        {!isCall ? '建立工單' : callEnded ? '✓ 建立工單' : '✓ 建立工單並結束通話'}
      </button>
    </form>
  );
}
