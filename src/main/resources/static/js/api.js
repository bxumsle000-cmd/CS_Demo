// ============================================================
// 後端 API 封裝。所有 fetch 都從這裡出去，頁面不直接碰 fetch。
// 路徑對應 controller/*.java；錯誤統一轉成 ApiError（code + message）。
// ============================================================
import { getToken } from './state.js';

const BASE = '/api';

/** 後端回的 ErrorResponse { code, message } 會轉成這個例外 */
export class ApiError extends Error {
  constructor(code, message) {
    super(message);
    this.code = code;
  }
}

/**
 * 共用的請求函式。
 * @param {string} method  GET / POST / PUT / DELETE
 * @param {string} path    /api 之後的路徑
 * @param {{ body?: object, query?: object }} opts  body 會轉成 JSON；query 中空值會自動略過
 */
async function request(method, path, { body, query } = {}) {
  let url = BASE + path;
  if (query) {
    const qs = new URLSearchParams();
    for (const [k, v] of Object.entries(query)) {
      if (v !== undefined && v !== null && v !== '') qs.set(k, v);
    }
    const s = qs.toString();
    if (s) url += '?' + s;
  }

  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  // 後端目前用寫死的 CurrentAgent，還沒驗 token；先照規格帶上，之後接 JWT 就直接可用
  const token = getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;

  let res;
  try {
    res = await fetch(url, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, '無法連線到伺服器，請確認後端是否啟動');
  }

  // void 方法回 200 且沒有內容，所以先讀 text 再決定要不要 parse
  const text = await res.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch { data = text; }
  }

  if (!res.ok) {
    const msg = (data && typeof data === 'object' && data.message) || `請求失敗（HTTP ${res.status}）`;
    throw new ApiError(res.status, msg);
  }
  return data;
}

const enc = encodeURIComponent;

export const api = {
  // ---- AuthController ----
  login: (agentId, password) => request('POST', '/auth/login', { body: { agentId, password } }),
  logout: () => request('POST', '/auth/logout'),
  me: () => request('GET', '/auth/me'),

  // ---- AgentController ----
  updateMyStatus: (status) => request('PUT', '/agents/me/status', { body: { status } }),
  register: (agentId, name, password) => request('POST', '/agents/register', { body: { agentId, name, password } }),
  changePassword: (oldPassword, newPassword) => request('PUT', '/agents/me/password', { body: { oldPassword, newPassword } }),

  // ---- TicketController ----
  /** query: ticketNo, customerName, contactPhone, status, assigneeId, updatedFrom, updatedTo, page, size */
  searchTickets: (query) => request('GET', '/tickets', { query }),
  /** 「轉派給其他客服」下拉選單：[{ agentId, name }] */
  assignees: () => request('GET', '/tickets/assignees'),
  createTicketFromCall: (body) => request('POST', '/tickets/from-call', { body }),
  createTicketFromAgent: (body) => request('POST', '/tickets/from-agent', { body }),

  // ---- TicketDetailController ----
  ticketDetail: (ticketNo) => request('GET', `/tickets/${enc(ticketNo)}`),
  updateTicketStatus: (ticketNo, status) => request('PUT', `/tickets/${enc(ticketNo)}/status`, { body: { status } }),
  reassignTicket: (ticketNo, assignId) => request('PUT', `/tickets/${enc(ticketNo)}/assignee`, { body: { assignId } }),
  addComment: (ticketNo, content) => request('POST', `/tickets/${enc(ticketNo)}/comments`, { body: { content } }),

  // ---- FollowUpController ----
  /** from / to 為 yyyy-MM-dd */
  followUps: (from, to) => request('GET', '/follow-ups', { query: { from, to } }),
  assignableTickets: () => request('GET', '/follow-ups/tickets'),
  /** body: { ticketNo, followUpAt: 'yyyy-MM-ddTHH:mm:ss', note } */
  createFollowUp: (body) => request('POST', '/follow-ups', { body }),
  deleteFollowUp: (followUpId) => request('DELETE', `/follow-ups/${enc(followUpId)}`),
};
