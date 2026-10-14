// ============================================================
// 路由 + 登入守衛。
//   #/login           登入
//   #/                首頁（工單列表）
//   #/call            通話工作台
//   #/tickets/:no     工單詳情
//   #/calendar        行事曆
// 用 hash 路由（HashRouter）的原因：Spring Boot 只把 / 對到 index.html，
// 換頁只改 # 後面的字串，不會真的向伺服器要別的路徑。
// ============================================================
import { useEffect } from 'react';
import { Navigate, Route, Routes, useLocation, useParams } from 'react-router-dom';
import { useSession } from './state.js';
import { ToastRoot, ConfirmRoot } from './components/Feedback.jsx';
import LoginPage from './pages/LoginPage.jsx';
import HomePage from './pages/HomePage.jsx';
import CallPage from './pages/CallPage.jsx';
import DetailPage from './pages/DetailPage.jsx';
import CalendarPage from './pages/CalendarPage.jsx';

/** 沒登入就導去登入頁 */
function RequireAuth({ children }) {
  const session = useSession();
  return session ? children : <Navigate to="/login" replace />;
}

/** 已登入就不要再看登入頁 */
function GuestOnly({ children }) {
  const session = useSession();
  return session ? <Navigate to="/" replace /> : children;
}

/** 換頁時捲回最上面 */
function ScrollToTop() {
  const { pathname } = useLocation();
  useEffect(() => { window.scrollTo(0, 0); }, [pathname]);
  return null;
}

/** key 綁單號：從一張工單直接連到另一張時，整頁重新載入，不會殘留上一張的狀態 */
function DetailRoute() {
  const { ticketNo } = useParams();
  return <DetailPage key={ticketNo} ticketNo={ticketNo} />;
}

export default function App() {
  return (
    <>
      <ScrollToTop />
      <div id="app">
        <Routes>
          <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
          <Route path="/" element={<RequireAuth><HomePage /></RequireAuth>} />
          <Route path="/call" element={<RequireAuth><CallPage /></RequireAuth>} />
          <Route path="/tickets/:ticketNo" element={<RequireAuth><DetailRoute /></RequireAuth>} />
          <Route path="/calendar" element={<RequireAuth><CalendarPage /></RequireAuth>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </div>
      <ConfirmRoot />
      <ToastRoot />
    </>
  );
}
