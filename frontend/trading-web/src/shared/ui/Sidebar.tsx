import { useEffect, useRef, useState, type KeyboardEvent } from "react";
import { NavLink } from "react-router-dom";
import { TrendingUp, X } from "lucide-react";
import { navigationGroups } from "../model/navigation";
import { cn } from "../lib/utils";
import { Button, buttonVariants } from "./shadcn/button";

const NAV_LINK_BASE =
  "group h-auto min-h-10 w-full justify-start gap-3 rounded-lg px-3 py-2.5 text-sm";
const NAV_LINK_ACTIVE =
  "bg-accent font-semibold text-accent-foreground shadow-sm";
const NAV_LINK_INACTIVE =
  "text-muted-foreground hover:bg-muted hover:text-foreground";
const DESKTOP_BREAKPOINT_PX = 1024;

const isMobileViewport = () => window.innerWidth < DESKTOP_BREAKPOINT_PX;

type SidebarProps = {
  isOpen: boolean;
  setOpen: (value: boolean) => void;
};

export function Sidebar({ isOpen, setOpen }: SidebarProps) {
  const sidebarRef = useRef<HTMLElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const returnFocusRef = useRef<HTMLElement | null>(null);
  const [isMobile, setIsMobile] = useState(isMobileViewport);

  useEffect(() => {
    const main = document.querySelector("main");
    if (isOpen) {
      returnFocusRef.current = document.activeElement as HTMLElement | null;
    }

    const syncViewportIsolation = () => {
      const mobile = isMobileViewport();
      setIsMobile(mobile);

      if (!isOpen || !mobile) {
        main?.removeAttribute("inert");
        if (document.activeElement === closeButtonRef.current) {
          sidebarRef.current?.querySelector<HTMLElement>('a[href]')?.focus();
        }
        returnFocusRef.current = null;
        return;
      }

      main?.setAttribute("inert", "");
    };

    syncViewportIsolation();
    if (isOpen && isMobileViewport()) closeButtonRef.current?.focus();
    window.addEventListener("resize", syncViewportIsolation);

    return () => {
      window.removeEventListener("resize", syncViewportIsolation);
      main?.removeAttribute("inert");
      returnFocusRef.current?.focus();
      returnFocusRef.current = null;
    };
  }, [isOpen]);

  const handleDrawerKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    if (!isMobileViewport()) return;

    if (event.key === "Escape") {
      event.preventDefault();
      setOpen(false);
      return;
    }

    if (event.key !== "Tab") return;

    const focusableElements = Array.from(
      sidebarRef.current?.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])',
      ) ?? [],
    );
    const first = focusableElements[0];
    const last = focusableElements.at(-1);

    if (!first || !last) return;
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  };

  const closeOnMobile = () => {
    if (isMobileViewport()) setOpen(false);
  };

  return (
    <>
      <Button
        type="button"
        variant="ghost"
        aria-label="사이드바 닫기"
        aria-hidden="true"
        tabIndex={-1}
        className={cn(
          "fixed inset-0 z-40 h-auto w-auto rounded-none bg-foreground/30 p-0 backdrop-blur-sm transition-opacity duration-200 hover:bg-foreground/30 lg:hidden",
          isOpen ? "pointer-events-auto opacity-100" : "pointer-events-none opacity-0",
        )}
        onClick={() => setOpen(false)}
      />

      <aside
        ref={sidebarRef}
        aria-label="주요 내비게이션"
        aria-hidden={isMobile && !isOpen ? true : undefined}
        inert={isMobile && !isOpen ? true : undefined}
        onKeyDown={handleDrawerKeyDown}
        className={cn(
          "z-50 flex w-sidebar shrink-0 flex-col border-r bg-card transition-transform duration-200 max-lg:fixed max-lg:inset-y-0 max-lg:left-0",
          isOpen ? "translate-x-0" : "max-lg:-translate-x-full",
        )}
      >
        <div className="flex h-topbar items-center gap-3 border-b px-5">
          <div className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-sm">
            <TrendingUp aria-hidden="true" size={19} />
          </div>
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold tracking-tight text-foreground">
              Paper Trading
            </p>
            <p className="truncate text-xs text-muted-foreground">운영 워크스테이션</p>
          </div>
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            ref={closeButtonRef}
            className="ml-auto lg:hidden"
            onClick={() => setOpen(false)}
            aria-label="사이드바 닫기"
          >
            <X aria-hidden="true" size={18} />
          </Button>
        </div>

        <nav aria-label="업무 영역" className="flex-1 space-y-6 overflow-y-auto px-3 py-5">
          {navigationGroups.map((group) => (
            <section key={group.id} aria-labelledby={`nav-${group.id}`}>
              <h2
                id={`nav-${group.id}`}
                className="mb-2 px-3 text-xs font-semibold uppercase tracking-widest text-muted-foreground"
              >
                {group.label}
              </h2>
              <div className="space-y-1">
                {group.items.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.to}
                      to={item.to}
                      end={item.end}
                      className={({ isActive }) =>
                        cn(
                          buttonVariants({ variant: isActive ? "secondary" : "ghost" }),
                          NAV_LINK_BASE,
                          isActive ? NAV_LINK_ACTIVE : NAV_LINK_INACTIVE,
                        )
                      }
                      onClick={closeOnMobile}
                    >
                      <Icon aria-hidden="true" size={17} />
                      <span className="min-w-0 whitespace-normal break-words text-left leading-5">
                        {item.label}
                      </span>
                    </NavLink>
                  );
                })}
              </div>
            </section>
          ))}
        </nav>

        <div className="border-t px-5 py-4 text-xs text-muted-foreground">
          <p>Trading Hub v1.2.0</p>
        </div>
      </aside>
    </>
  );
}
