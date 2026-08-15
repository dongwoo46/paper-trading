import { NavLink } from "react-router-dom";
import { BarChart2, BookOpen, CandlestickChart, ChartLine, ClipboardList, Globe, History, Home, ReceiptText, TrendingUp, Wallet, X, Zap } from "lucide-react";
import { Button, buttonVariants } from "./shadcn/button";
import { cn } from "../lib/utils";

const NAV_LINK_BASE = "h-auto w-full justify-start gap-3 px-4 py-3 text-sm";
const NAV_LINK_ACTIVE = "bg-accent font-bold text-accent-foreground";
const NAV_LINK_INACTIVE = "text-muted-foreground";

export function Sidebar({ isOpen, setOpen }: { isOpen: boolean; setOpen: (v: boolean) => void }) {
  const closeOnMobile = () => {
    if (window.innerWidth <= 1024) setOpen(false);
  };

  return (
    <>
      {/* Backdrop */}
      <Button
        type="button"
        variant="ghost"
        aria-label="사이드바 닫기"
        className={`fixed inset-0 z-40 h-auto w-auto rounded-none bg-foreground/40 p-0 backdrop-blur-sm transition-opacity duration-300 hover:bg-foreground/40 lg:hidden ${isOpen ? "pointer-events-auto opacity-100" : "pointer-events-none opacity-0"}`}
        onClick={() => setOpen(false)}
      />

      {/* Sidebar */}
      <aside
        className={`z-50 flex w-sidebar shrink-0 flex-col border-r bg-card shadow-sm transition-transform duration-300 max-lg:fixed max-lg:h-full ${isOpen ? "translate-x-0" : "max-lg:-translate-x-full"}`}
      >
        <div className="flex h-topbar items-center gap-3 border-b px-6">
          <div className="flex size-9 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <TrendingUp size={20} />
          </div>
          <h1 className="text-xl font-bold tracking-tight text-foreground">
            트레이딩 콘솔
          </h1>
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            className="ml-auto lg:hidden"
            onClick={() => setOpen(false)}
            aria-label="사이드바 닫기"
          >
            <X size={20} />
          </Button>
        </div>

        <nav className="flex-1 py-6 px-4 flex flex-col gap-1.5 overflow-y-auto">
          <NavLink
            to="/"
            end
            className={({ isActive }) => cn(buttonVariants({ variant: isActive ? "secondary" : "ghost" }), NAV_LINK_BASE, isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE)}
            onClick={closeOnMobile}
          >
            <Home size={18} />
            <span>대시보드</span>
          </NavLink>
          <NavLink
            to="/account"
            className={({ isActive }) => cn(buttonVariants({ variant: isActive ? "secondary" : "ghost" }), NAV_LINK_BASE, isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE)}
            onClick={closeOnMobile}
          >
            <Wallet size={18} />
            <span>계좌·포지션</span>
          </NavLink>
          <NavLink
            to="/realtime"
            className={({ isActive }) => cn(buttonVariants({ variant: isActive ? "secondary" : "ghost" }), NAV_LINK_BASE, isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE)}
            onClick={closeOnMobile}
          >
            <Zap size={18} />
            <span>실시간 데이터</span>
          </NavLink>
          <NavLink
            to="/historical"
            className={({ isActive }) => cn(buttonVariants({ variant: isActive ? "secondary" : "ghost" }), NAV_LINK_BASE, isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE)}
            onClick={closeOnMobile}
          >
            <History size={18} />
            <span>과거 시세 수집</span>
          </NavLink>
          <NavLink
            to="/macro"
            className={({ isActive }) => cn(buttonVariants({ variant: isActive ? "secondary" : "ghost" }), NAV_LINK_BASE, isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE)}
            onClick={closeOnMobile}
          >
            <Globe size={18} />
            <span>거시경제 지표</span>
          </NavLink>
          <NavLink
            to="/market-unified"
            className={({ isActive }) => cn(buttonVariants({ variant: isActive ? "secondary" : "ghost" }), NAV_LINK_BASE, isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE)}
            onClick={closeOnMobile}
          >
            <CandlestickChart size={18} />
            <span>통합 시세 차트</span>
          </NavLink>
          <NavLink
            to="/orders"
            className={({ isActive }) => cn(buttonVariants({ variant: isActive ? "secondary" : "ghost" }), NAV_LINK_BASE, isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE)}
            onClick={closeOnMobile}
          >
            <ClipboardList size={18} />
            <span>주문 관리</span>
          </NavLink>
          <NavLink
            to="/portfolio"
            className={({ isActive }) => cn(buttonVariants({ variant: isActive ? "secondary" : "ghost" }), NAV_LINK_BASE, isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE)}
            onClick={closeOnMobile}
          >
            <ChartLine size={18} />
            <span>포트폴리오 차트</span>
          </NavLink>
          <NavLink
            to="/tax-summary"
            className={({ isActive }) => cn(buttonVariants({ variant: isActive ? "secondary" : "ghost" }), NAV_LINK_BASE, isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE)}
            onClick={closeOnMobile}
          >
            <ReceiptText size={18} />
            <span>세금 요약</span>
          </NavLink>
          <NavLink
            to="/trading-journals"
            className={({ isActive }) => cn(buttonVariants({ variant: isActive ? "secondary" : "ghost" }), NAV_LINK_BASE, isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE)}
            onClick={closeOnMobile}
          >
            <BookOpen size={18} />
            <span>거래 일지</span>
          </NavLink>
          <NavLink
            to="/chart-analysis"
            className={({ isActive }) => cn(buttonVariants({ variant: isActive ? "secondary" : "ghost" }), NAV_LINK_BASE, isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE)}
            onClick={closeOnMobile}
          >
            <BarChart2 size={18} />
            <span>차트 분석</span>
          </NavLink>
        </nav>

        <div className="flex flex-col gap-1 border-t p-6 text-xs text-muted-foreground">
          <p>Trading Hub v1.2.0</p>
          <p>© 2026 Paper Trading</p>
        </div>
      </aside>
    </>
  );
}
