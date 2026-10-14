// ============================================================
// 前端的全域狀態：
//   1. session — 登入者資料（存 localStorage，重新整理不會掉）
//   2. call    — 模擬的進線通話（後端沒有電話系統 API，這部分純前端模擬；
//                存 sessionStorage，同一個分頁重新整理仍保留）
//
// 這兩個狀態放在 React 外面（普通的 JS 變數），因為 api.js 這種非元件的程式也要讀。
// 元件用 useSession() / useCall() 讀取，值一改，有用到的元件會自動重畫。
// ============================================================
import { useSyncExternalStore } from 'react';

/** 建一個最小的「可訂閱的值」：get 取值、set 改值並通知所有訂閱者 */
function createStore(initial, persist) {
  let value = initial;
  const listeners = new Set();
  return {
    get: () => value,
    set(next) {
      value = next;
      persist(next);
      listeners.forEach((fn) => fn());
    },
    subscribe(fn) {
      listeners.add(fn);
      return () => listeners.delete(fn);
    },
  };
}

// ---------------- session ----------------
const SESSION_KEY = 'cs_demo_session';

function loadJson(storage, key) {
  try { return JSON.parse(storage.getItem(key)) || null; } catch { return null; }
}

function saveJson(storage, key, value) {
  try {
    if (value) storage.setItem(key, JSON.stringify(value));
    else storage.removeItem(key);
  } catch { /* 無痕模式等情況忽略 */ }
}

const sessionStore = createStore(loadJson(localStorage, SESSION_KEY), (v) => saveJson(localStorage, SESSION_KEY, v));

/** @returns {{agentId:string, token:string, name?:string, status?:string}|null} */
export const getSession = sessionStore.get;
export const setSession = sessionStore.set;
/** 只更新部分欄位（例如 /me 回來後補 name、status） */
export const patchSession = (partial) => sessionStore.set({ ...(sessionStore.get() || {}), ...partial });
export const clearSession = () => sessionStore.set(null);
export const getToken = () => sessionStore.get()?.token || null;

export function useSession() {
  return useSyncExternalStore(sessionStore.subscribe, sessionStore.get);
}

// ---------------- 模擬通話 ----------------
const CALL_KEY = 'cs_demo_call';
const callStore = createStore(loadJson(sessionStorage, CALL_KEY), (v) => saveJson(sessionStorage, CALL_KEY, v));

/**
 * 一通電話有兩個階段：
 *   通話中   endedAt = null   → 計時中、狀態顯示「通話中」
 *   已掛斷   endedAt = 時間   → 電話斷了，但工單還沒送出，客服繼續填表單
 * 工單送出（或放棄）後才用 clearCall() 整個清掉。
 * @returns {{phone:string, startedAt:number, endedAt:number|null, muted:boolean, draft?:object}|null}
 */
export const getCall = callStore.get;
export const startCall = (phone) => callStore.set({ phone, startedAt: Date.now(), endedAt: null, muted: false });
/** 掛斷電話：停止計時，但保留通話資料讓客服繼續寫工單 */
export function hangUp() {
  const call = callStore.get();
  if (call && !call.endedAt) callStore.set({ ...call, endedAt: Date.now(), muted: false });
}
/** 存下填到一半的工單表單（掛斷後、換頁再回來都能接著寫） */
export function saveCallDraft(draft) {
  const call = callStore.get();
  if (call) callStore.set({ ...call, draft });
}
/** 工單送出或放棄後，清掉這通電話 */
export const clearCall = () => callStore.set(null);
/** 是否還在講電話（已掛斷、只剩表單沒寫完的不算） */
export const isOnCall = (call) => !!call && !call.endedAt;
export function toggleMute() {
  const call = callStore.get();
  if (call) callStore.set({ ...call, muted: !call.muted });
}

export function useCall() {
  return useSyncExternalStore(callStore.subscribe, callStore.get);
}

/** 模擬來電號碼池（demo 用），接聽時隨機挑一個 */
const DEMO_NUMBERS = ['04-2233-1188', '02-2345-6789', '03-567-8899', '0912-345-678', '02-8765-4321'];
export function randomIncomingNumber() {
  return DEMO_NUMBERS[Math.floor(Math.random() * DEMO_NUMBERS.length)];
}
