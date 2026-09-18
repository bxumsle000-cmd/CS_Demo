// ============================================================
// 前端的全域狀態：
//   1. session — 登入者資料（存 localStorage，重新整理不會掉）
//   2. call    — 模擬的進線通話（後端沒有電話系統 API，這部分純前端模擬；
//                存 sessionStorage，同一個分頁重新整理仍保留）
// ============================================================

// ---------------- session ----------------
const SESSION_KEY = 'cs_demo_session';

function loadSession() {
  try { return JSON.parse(localStorage.getItem(SESSION_KEY)) || null; } catch { return null; }
}

let session = loadSession();

/** @returns {{agentId:string, token:string, name?:string, status?:string}|null} */
export function getSession() { return session; }

export function setSession(next) {
  session = next;
  try { localStorage.setItem(SESSION_KEY, JSON.stringify(next)); } catch { /* 無痕模式等情況忽略 */ }
}

/** 只更新部分欄位（例如 /me 回來後補 name、status） */
export function patchSession(partial) {
  setSession({ ...(session || {}), ...partial });
}

export function clearSession() {
  session = null;
  try { localStorage.removeItem(SESSION_KEY); } catch { /* ignore */ }
}

export function getToken() { return session?.token || null; }

// ---------------- 模擬通話 ----------------
const CALL_KEY = 'cs_demo_call';
const listeners = new Set();

function loadCall() {
  try { return JSON.parse(sessionStorage.getItem(CALL_KEY)) || null; } catch { return null; }
}

let call = loadCall();

function persistCall() {
  try {
    if (call) sessionStorage.setItem(CALL_KEY, JSON.stringify(call));
    else sessionStorage.removeItem(CALL_KEY);
  } catch { /* ignore */ }
  listeners.forEach((fn) => fn(call));
}

/** @returns {{phone:string, startedAt:number, muted:boolean}|null} */
export function getCall() { return call; }

export function startCall(phone) {
  call = { phone, startedAt: Date.now(), muted: false };
  persistCall();
}

export function endCall() {
  call = null;
  persistCall();
}

export function toggleMute() {
  if (!call) return;
  call = { ...call, muted: !call.muted };
  persistCall();
}

/** 訂閱通話狀態變化；回傳取消訂閱的函式 */
export function onCallChange(fn) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

/** 模擬來電號碼池（demo 用），接聽時隨機挑一個 */
const DEMO_NUMBERS = ['04-2233-1188', '02-2345-6789', '03-567-8899', '0912-345-678', '02-8765-4321'];
export function randomIncomingNumber() {
  return DEMO_NUMBERS[Math.floor(Math.random() * DEMO_NUMBERS.length)];
}
