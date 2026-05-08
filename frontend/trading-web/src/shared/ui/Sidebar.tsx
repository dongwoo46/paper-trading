import { NavLink } from "react-router-dom";
import { BookOpen, CandlestickChart, ChartLine, ClipboardList, Globe, History, Home, ReceiptText, TrendingUp, Wallet, X, Zap } from "lucide-react";

const NAV_LINK_BASE = "flex items-center gap-3 px-4 py-3 rounded-xl font-medium text-[14.5px] transition-all duration-250 border";
const NAV_LINK_ACTIVE = "bg-blue-400/25 text-brand-primary border-blue-400/50 shadow";
const NAV_LINK_INACTIVE = "text-text-secondary border-transparent hover:bg-white/[0.04] hover:text-text-primary";

export function Sidebar({ isOpen, setOpen }: { isOpen: boolean; setOpen: (v: boolean) => void }) {
  const closeOnMobile = () => {
    if (window.innerWidth <= 1024) setOpen(false);
  };

  return (
    <>
      {/* Backdrop */}
      <div
        className={`fixed inset-0 bg-black/60 backdrop-blur-sm z-[90] transition-opacity duration-300 lg:hidden ${isOpen ? "opacity-100 pointer-events-auto" : "opacity-0 pointer-events-none"}`}
        onClick={() => setOpen(false)}
      />

      {/* Sidebar */}
      <aside
        className={`w-[280px] bg-bg-sidebar border-r border-white/12 flex flex-col shrink-0 z-[100] transition-transform duration-400 shadow-[10px_0_30px_rgba(0,0,0,0.2)] max-lg:fixed max-lg:h-full ${isOpen ? "translate-x-0" : "max-lg:-translate-x-full"}`}
      >
        <div className="h-[72px] px-6 flex items-center gap-3 border-b border-white/12">
          <div className="w-9 h-9 flex items-center justify-center rounded-[10px] -rotate-3 shadow-lg bg-gradient-to-br from-blue-500 to-emerald-500">
            <TrendingUp size={20} color="white" />
          </div>
          <h1 className="text-lg font-bold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
            트레이딩 콘솔
          </h1>
          <button
            className="lg:hidden ml-auto p-1 text-text-secondary hover:text-text-primary transition-colors"
            onClick={() => setOpen(false)}
          >
            <X size={20} />
          </button>
        </div>

        <nav className="flex-1 py-6 px-4 flex flex-col gap-1.5">
          <NavLink
            to="/"
            end
            className={({ isActive }) => `${NAV_LINK_BASE} ${isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE}`}
            onClick={closeOnMobile}
          >
            <Home size={18} />
            <span>대시보드</span>
          </NavLink>
          <NavLink
            to="/account"
            className={({ isActive }) => `${NAV_LINK_BASE} ${isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE}`}
            onClick={closeOnMobile}
          >
            <Wallet size={18} />
            <span>계좌·포지션</span>
          </NavLink>
          <NavLink
            to="/realtime"
            className={({ isActive }) => `${NAV_LINK_BASE} ${isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE}`}
            onClick={closeOnMobile}
          >
            <Zap size={18} />
            <span>실시간 데이터</span>
          </NavLink>
          <NavLink
            to="/historical"
            className={({ isActive }) => `${NAV_LINK_BASE} ${isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE}`}
            onClick={closeOnMobile}
          >
            <History size={18} />
            <span>과거 시세 수집</span>
          </NavLink>
          <NavLink
            to="/macro"
            className={({ isActive }) => `${NAV_LINK_BASE} ${isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE}`}
            onClick={closeOnMobile}
          >
            <Globe size={18} />
            <span>거시경제 지표</span>
          </NavLink>
          <NavLink
            to="/market-unified"
            className={({ isActive }) => `${NAV_LINK_BASE} ${isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE}`}
            onClick={closeOnMobile}
          >
            <CandlestickChart size={18} />
            <span>통합 시세 차트</span>
          </NavLink>
          <NavLink
            to="/orders"
            className={({ isActive }) => `${NAV_LINK_BASE} ${isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE}`}
            onClick={closeOnMobile}
          >
            <ClipboardList size={18} />
            <span>주문 관리</span>
          </NavLink>
          <NavLink
            to="/portfolio"
            className={({ isActive }) => `${NAV_LINK_BASE} ${isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE}`}
            onClick={closeOnMobile}
          >
            <ChartLine size={18} />
            <span>포트폴리오 차트</span>
          </NavLink>
          <NavLink
            to="/tax-summary"
            className={({ isActive }) => `${NAV_LINK_BASE} ${isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE}`}
            onClick={closeOnMobile}
          >
            <ReceiptText size={18} />
            <span>세금 요약</span>
          </NavLink>
          <NavLink
            to="/trading-journals"
            className={({ isActive }) => `${NAV_LINK_BASE} ${isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE}`}
            onClick={closeOnMobile}
          >
            <BookOpen size={18} />
            <span>거래 일지</span>
          </NavLink>
        </nav>

        <div className="p-6 border-t border-white/12 text-xs text-text-muted flex flex-col gap-1">
          <p>Trading Hub v1.2.0</p>
          <p>© 2026 Paper Trading</p>
        </div>
      </aside>
    </>
  );
}
