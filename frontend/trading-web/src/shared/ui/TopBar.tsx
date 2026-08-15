import { useEffect, useRef, useState } from "react";
import { Bell, Menu, X } from "lucide-react";
import { useNotificationStore } from "../model/useNotificationStore";
import { Button } from "./shadcn/button";
import { Card, CardContent, CardHeader, CardTitle } from "./shadcn/card";

function NotificationDropdown({ onClose }: { onClose: () => void }) {
  const notifications = useNotificationStore((s) => s.notifications);
  const markAllRead = useNotificationStore((s) => s.markAllRead);
  const clear = useNotificationStore((s) => s.clear);

  return (
    <Card className="absolute right-0 top-full z-50 mt-2 w-80 gap-0 py-0 shadow-xl">
      <CardHeader className="flex flex-row items-center justify-between border-b py-3">
        <CardTitle>알림</CardTitle>
        <div className="flex items-center gap-2">
          {notifications.length > 0 && (
            <>
              <Button
                type="button"
                variant="ghost"
                size="xs"
                onClick={markAllRead}
              >
                모두 읽음
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="xs"
                onClick={clear}
              >
                지우기
              </Button>
            </>
          )}
          <Button
            type="button"
            variant="ghost"
            size="icon-xs"
            onClick={onClose}
            aria-label="알림 닫기"
          >
            <X size={14} />
          </Button>
        </div>
      </CardHeader>

      <CardContent className="max-h-80 overflow-y-auto px-0">
        {notifications.length === 0 ? (
          <p className="px-4 py-6 text-center text-sm text-muted-foreground">알림이 없습니다.</p>
        ) : (
          notifications.map((n) => (
            <div
              key={n.id}
              className={`border-b px-4 py-3 last:border-0 ${n.read ? "" : "bg-accent"}`}
            >
              <div className="flex items-start gap-2">
                {!n.read && (
                  <span className="mt-1.5 size-2 flex-shrink-0 rounded-full bg-primary" />
                )}
                <div className={n.read ? "ml-4" : ""}>
                  <p className="text-sm font-semibold text-foreground">
                    LLM 분석 완료
                  </p>
                  <p className="mt-0.5 text-xs text-muted-foreground">
                    <span className="font-mono">{n.symbol}</span> {n.name}
                  </p>
                  <p className="mt-0.5 text-xs text-muted-foreground">
                    {n.window} / {n.interval === "D" ? "일봉" : "주봉"} · {n.source}
                  </p>
                  <p className="mt-0.5 text-xs text-muted-foreground">
                    {n.createdAt.toLocaleTimeString("ko-KR")}
                  </p>
                </div>
              </div>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );
}

export function TopBar({ toggleSidebar }: { toggleSidebar: () => void }) {
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
    if (!open) markAllRead();
    setOpen((previousOpen) => !previousOpen);
  }

  return (
    <header className="z-30 flex h-topbar items-center justify-between border-b bg-card/95 px-4 backdrop-blur sm:px-6 lg:px-8">
      <div className="flex min-w-0 items-center gap-3">
        <Button
          type="button"
          variant="outline"
          size="icon"
          className="lg:hidden"
          onClick={toggleSidebar}
          aria-label="사이드바 열기"
        >
          <Menu size={20} />
        </Button>
        <div className="hidden min-w-0 items-center gap-2 sm:flex">
          <span className="text-xs font-semibold uppercase tracking-widest text-primary">
            Paper Trading
          </span>
          <span aria-hidden="true" className="size-1 rounded-full bg-border" />
          <span className="truncate text-sm text-muted-foreground">운영 워크스테이션</span>
        </div>
      </div>

      <div className="relative" ref={dropdownRef}>
        <Button
          type="button"
          variant="outline"
          size="icon"
          onClick={handleBellClick}
          className="relative"
          aria-label="알림"
        >
          <Bell size={18} />
          {unreadCount > 0 && (
            <span className="absolute -right-1 -top-1 flex size-5 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-primary-foreground">
              {unreadCount > 9 ? "9+" : unreadCount}
            </span>
          )}
        </Button>

        {open && <NotificationDropdown onClose={() => setOpen(false)} />}
      </div>
    </header>
  );
}
