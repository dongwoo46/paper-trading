import { Menu, Activity } from "lucide-react";

export function TopBar({ title, toggleSidebar }: { title: string; toggleSidebar: () => void }) {
  return (
    <header className="topbar">
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <button className="btn btn-outline mobile-only" onClick={toggleSidebar}>
          <Menu size={20} />
        </button>
        <div className="page-title-group">
          <h2>{title}</h2>
        </div>
      </div>
      <div className="topbar-actions">
        <button className="btn btn-outline" style={{ padding: '8px' }}>
          <Activity size={18} />
        </button>
      </div>
    </header>
  );
}
