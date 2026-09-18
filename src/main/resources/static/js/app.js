// ============================================================
// 進入點：hash 路由 + 登入守衛。
//   #/login           登入
//   #/                首頁（工單列表）
//   #/call            通話工作台
//   #/tickets/:no     工單詳情
//   #/calendar        行事曆
// 用 hash 路由的原因：Spring Boot 只把 / 對到 index.html，
// 換頁只改 # 後面的字串，不會真的向伺服器要別的路徑。
// ============================================================
import { getSession } from './state.js';
import { $ } from './ui.js';
import * as login from './pages/login.js';
import * as home from './pages/home.js';
import * as call from './pages/call.js';
import * as detail from './pages/detail.js';
import * as calendar from './pages/calendar.js';

const routes = [
  { pattern: /^#\/login$/, page: login, auth: false },
  { pattern: /^#\/?$/, page: home, auth: true },
  { pattern: /^#\/call$/, page: call, auth: true },
  { pattern: /^#\/tickets\/([^/]+)$/, page: detail, auth: true, params: (m) => ({ ticketNo: decodeURIComponent(m[1]) }) },
  { pattern: /^#\/calendar$/, page: calendar, auth: true },
];

let cleanup = null;

export function navigate(hash) {
  if (location.hash === hash) route();
  else location.hash = hash;
}

function route() {
  const hash = location.hash || '#/';
  const match = routes.map((r) => ({ r, m: hash.match(r.pattern) })).find((x) => x.m);

  if (!match) { location.hash = '#/'; return; }

  const { r, m } = match;
  const loggedIn = !!getSession();
  if (r.auth && !loggedIn) { location.hash = '#/login'; return; }
  if (!r.auth && loggedIn && r.page === login) { location.hash = '#/'; return; }

  if (typeof cleanup === 'function') cleanup();
  $('#modal-root').innerHTML = '';
  const view = $('#view');
  window.scrollTo(0, 0);
  cleanup = r.page.render(view, r.params ? r.params(m) : {}) || null;
}

window.addEventListener('hashchange', route);
route();
