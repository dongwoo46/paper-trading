import { NavLink } from "react-router-dom";
import { Globe, History, Home, TrendingUp, X, Zap } from "lucide-react";

export function Sidebar({ isOpen, setOpen }: { isOpen: boolean; setOpen: (v: boolean) => void }) {
  const closeOnMobile = () => {
    if (window.innerWidth <= 1024) setOpen(false);
  };

  return (
    <>
      <div className={`sidebar-backdrop ${isOpen ? "open" : ""}`} onClick={() => setOpen(false)} />
      <aside className={`sidebar ${isOpen ? "open" : ""}`}>
        <div className="sidebar-header">
          <div className="sidebar-brand-icon">
            <TrendingUp size={20} color="white" />
          </div>
          <h1>트레이딩 콘솔</h1>
          <button className="mobile-only" onClick={() => setOpen(false)} style={{ marginLeft: "auto" }}>
            <X size={20} color="var(--text-secondary)" />
          </button>
        </div>

        <nav className="sidebar-nav">
          <NavLink to="/" end className="nav-link" onClick={closeOnMobile}>
            <Home size={18} />
            <span>대시보드</span>
          </NavLink>
          <NavLink to="/realtime" className="nav-link" onClick={closeOnMobile}>
            <Zap size={18} />
            <span>실시간 데이터</span>
          </NavLink>
          <NavLink to="/historical" className="nav-link" onClick={closeOnMobile}>
            <History size={18} />
            <span>과거 시세 수집</span>
          </NavLink>
          <NavLink to="/macro" className="nav-link" onClick={closeOnMobile}>
            <Globe size={18} />
            <span>거시경제 지표</span>
          </NavLink>
        </nav>

        <div className="sidebar-footer">
          <p>Trading Hub v1.2.0</p>
          <p>© 2026 Paper Trading</p>
        </div>
      </aside>
    </>
  );
}
