import { Menu, Activity } from "lucide-react";

export function TopBar({ title, toggleSidebar }: { title: string; toggleSidebar: () => void }) {
  return (
    <header className="h-[72px] flex items-center justify-between px-8 border-b border-border-primary bg-bg-card z-40">
      <div className="flex items-center gap-4">
        <button
          className="flex lg:hidden items-center justify-center px-3 py-2 rounded-xl border border-border-primary text-text-secondary hover:bg-bg-input transition-all"
          onClick={toggleSidebar}
        >
          <Menu size={20} />
        </button>
        <h2 className="text-xl font-semibold text-text-primary tracking-tight">{title}</h2>
      </div>
      <div>
        <button className="p-2 rounded-xl border border-border-primary text-text-secondary hover:bg-bg-input transition-all">
          <Activity size={18} />
        </button>
      </div>
    </header>
  );
}
