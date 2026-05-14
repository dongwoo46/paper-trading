import { useEffect, useRef, useState } from "react";
import { Bell, Menu, X } from "lucide-react";
import { useNotificationStore } from "../model/useNotificationStore";

function NotificationDropdown({ onClose }: { onClose: () => void }) {
  const notifications = useNotificationStore((s) => s.notifications);
  const markAllRead = useNotificationStore((s) => s.markAllRead);
  const clear = useNotificationStore((s) => s.clear);

  return (
    <div className="absolute right-0 top-full mt-2 z-50 w-80 rounded-xl border border-border-primary bg-bg-card shadow-xl">
      <div className="flex items-center justify-between border-b border-border-primary px-4 py-3">
        <span className="text-sm font-semibold text-text-primary">알림</span>
        <div className="flex items-center gap-2">
          {notifications.length > 0 && (
            <>
              <button
                type="button"
                onClick={markAllRead}
                className="text-xs text-text-muted hover:text-text-primary"
              >
                모두 읽음
              </button>
              <button
                type="button"
                onClick={clear}
                className="text-xs text-text-muted hover:text-text-primary"
              >
                지우기
              </button>
            </>
          )}
          <button
            type="button"
            onClick={onClose}
            className="rounded-full p-1 text-text-muted hover:bg-bg-input hover:text-text-primary"
          >
            <X size={14} />
          </button>
        </div>
      </div>

      <div className="max-h-80 overflow-y-auto">
        {notifications.length === 0 ? (
          <p className="px-4 py-6 text-center text-sm text-text-muted">알림이 없습니다.</p>
        ) : (
          notifications.map((n) => (
            <div
              key={n.id}
              className={`border-b border-border-primary px-4 py-3 last:border-0 ${n.read ? "" : "bg-brand-secondary"}`}
            >
              <div className="flex items-start gap-2">
                {!n.read && (
                  <span className="mt-1.5 h-2 w-2 flex-shrink-0 rounded-full bg-brand-primary" />
                )}
                <div className={n.read ? "ml-4" : ""}>
                  <p className="text-sm font-semibold text-text-primary">
                    LLM 분석 완료
                  </p>
                  <p className="mt-0.5 text-xs text-text-secondary">
                    <span className="font-mono">{n.symbol}</span> {n.name}
                  </p>
                  <p className="mt-0.5 text-xs text-text-muted">
                    {n.window} / {n.interval === "D" ? "일봉" : "주봉"} · {n.source}
                  </p>
                  <p className="mt-0.5 text-xs text-text-muted">
                    {n.createdAt.toLocaleTimeString("ko-KR")}
                  </p>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

export function TopBar({ title, toggleSidebar }: { title: string; toggleSidebar: () => void }) {
  const [open, setOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const unreadCount = useNotificationStore((s) => s.unreadCount);
  const markAllRead = useNotificationStore((s) => s.markAllRead);

  // 드롭다운 외부 클릭 시 닫기
  useEffect(() => {
    if (!open) return;
    function handleClick(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [open]);

  function handleBellClick() {
    setOpen((prev) => {
      if (!prev) markAllRead();
      return !prev;
    });
  }

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

      <div className="relative" ref={dropdownRef}>
        <button
          type="button"
          onClick={handleBellClick}
          className="relative p-2 rounded-xl border border-border-primary text-text-secondary hover:bg-bg-input transition-all"
          aria-label="알림"
        >
          <Bell size={18} />
          {unreadCount > 0 && (
            <span className="absolute -right-1 -top-1 flex h-5 w-5 items-center justify-center rounded-full bg-brand-primary text-[10px] font-bold text-white">
              {unreadCount > 9 ? "9+" : unreadCount}
            </span>
          )}
        </button>

        {open && <NotificationDropdown onClose={() => setOpen(false)} />}
      </div>
    </header>
  );
}
