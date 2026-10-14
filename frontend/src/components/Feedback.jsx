// ============================================================
// 全站共用的回饋元件：toast（右下角提示）、Modal、確認框、狀態標籤
//
// toast() 和 confirmDialog() 是普通函式，任何地方都能呼叫（不必是元件）；
// 畫面由 App 裡的 <ToastRoot /> 和 <ConfirmRoot /> 負責顯示。
// ============================================================
import { useEffect, useRef, useState, useSyncExternalStore } from 'react';
import { createPortal } from 'react-dom';
import { api } from '../api.js';
import { TICKET_STATUS } from '../ui.js';

// ---------------- toast ----------------
let toasts = [];
let toastSeq = 0;
const toastListeners = new Set();
const emitToasts = () => toastListeners.forEach((fn) => fn());

export function toast(message, type = 'ok', ms = 2800) {
  const id = ++toastSeq;
  toasts = [...toasts, { id, message, type }];
  emitToasts();
  setTimeout(() => {
    toasts = toasts.filter((t) => t.id !== id);
    emitToasts();
  }, ms);
}

/** 把 API 例外顯示成紅色 toast */
export function toastError(err) {
  toast(err?.message || '發生錯誤', 'err', 4000);
}

export function ToastRoot() {
  const list = useSyncExternalStore(
    (fn) => { toastListeners.add(fn); return () => toastListeners.delete(fn); },
    () => toasts,
  );
  return (
    <div id="toast-root">
      {list.map((t) => <div key={t.id} className={`toast ${t.type}`}>{t.message}</div>)}
    </div>
  );
}

// ---------------- Modal ----------------
/**
 * 彈出視窗。按 Esc、點背景、按 ✕ 都會呼叫 onClose。
 * 打開時自動把焦點放到第一個輸入框（或按鈕）。
 */
export function Modal({ title, narrow = false, onClose, children }) {
  const modalRef = useRef(null);

  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);

  useEffect(() => {
    // autoFocus 的元素優先；沒有的話找第一個輸入框或按鈕
    const el = modalRef.current;
    if (el && !el.contains(document.activeElement)) {
      el.querySelector('input, select, textarea, button.btn')?.focus();
    }
  }, []);

  return createPortal(
    <div className="modal-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className={`modal ${narrow ? 'narrow' : ''}`} role="dialog" aria-modal="true" ref={modalRef}>
        <div className="modal-head">
          <h3>{title}</h3>
          <button className="modal-close" type="button" aria-label="關閉" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>,
    document.body,
  );
}

// ---------------- 確認框 ----------------
let pendingConfirm = null;
const confirmListeners = new Set();
const emitConfirm = () => confirmListeners.forEach((fn) => fn());

/**
 * 自製的確認框（不用 window.confirm，風格一致也不會擋住頁面）。
 * @returns {Promise<boolean>}
 */
export function confirmDialog(message, { okText = '確定', danger = false } = {}) {
  return new Promise((resolve) => {
    pendingConfirm = { message, okText, danger, resolve };
    emitConfirm();
  });
}

export function ConfirmRoot() {
  const c = useSyncExternalStore(
    (fn) => { confirmListeners.add(fn); return () => confirmListeners.delete(fn); },
    () => pendingConfirm,
  );
  if (!c) return null;
  const finish = (ok) => {
    pendingConfirm = null;
    emitConfirm();
    c.resolve(ok);
  };
  return (
    <Modal title="請確認" narrow onClose={() => finish(false)}>
      <p style={{ margin: '0 0 6px', lineHeight: 1.6 }}>{c.message}</p>
      <div className="modal-foot">
        <button className="btn" type="button" onClick={() => finish(false)}>取消</button>
        <button className={`btn ${c.danger ? 'btn-danger' : 'btn-primary'}`} type="button" autoFocus onClick={() => finish(true)}>
          {c.okText}
        </button>
      </div>
    </Modal>
  );
}

// ---------------- 小元件 ----------------
export function StatusTag({ status }) {
  return <span className={`tag ${status}`}>{TICKET_STATUS[status] || status}</span>;
}

/** 「轉派給其他客服」下拉選單的選項；exclude 內的代號（自己／目前負責人）不列 */
export function AssigneeOptions({ agents, exclude = [] }) {
  if (agents === null) return <option value="">載入中…</option>;
  if (agents === 'error') return <option value="">（客服清單載入失敗）</option>;
  const list = agents.filter((a) => !exclude.includes(a.agentId));
  if (!list.length) return <option value="">（沒有可轉派的客服）</option>;
  return list.map((a) => <option key={a.agentId} value={a.agentId}>{a.agentId} {a.name}</option>);
}

/** 第一次 enabled 為 true 時才去撈客服清單；回傳 null（載入中）、'error' 或陣列 */
export function useAssignees(enabled) {
  const [agents, setAgents] = useState(null);
  const started = useRef(false);
  useEffect(() => {
    if (!enabled || started.current) return;
    started.current = true;
    api.assignees()
      .then(setAgents)
      .catch((err) => { setAgents('error'); toastError(err); });
  }, [enabled]);
  return agents;
}
