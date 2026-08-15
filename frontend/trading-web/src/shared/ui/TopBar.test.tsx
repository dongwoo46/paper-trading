import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { useNotificationStore, type AppNotification } from "../model/useNotificationStore";
import { TopBar } from "./TopBar";

function notification(index: number): AppNotification {
  return {
    id: String(index),
    symbol: `TEST${index}`,
    name: `테스트 ${index}`,
    window: "3M",
    interval: "D",
    source: "test",
    read: false,
    createdAt: new Date("2026-08-16T00:00:00+09:00"),
  };
}

afterEach(() => {
  act(() => {
    useNotificationStore.setState({ notifications: [], unreadCount: 0 });
  });
});

describe("TopBar", () => {
  it("keeps the utility bar free of a competing page heading", async () => {
    const toggleSidebar = vi.fn();
    render(<TopBar toggleSidebar={toggleSidebar} />);

    expect(screen.queryByRole("heading")).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "사이드바 열기" }));
    expect(toggleSidebar).toHaveBeenCalledOnce();
  });

  it("keeps the 9+ notification badge and opens the notification panel", async () => {
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    const notifications = Array.from({ length: 10 }, (_, index) => notification(index));
    act(() => {
      useNotificationStore.setState({ notifications, unreadCount: notifications.length });
    });

    render(<TopBar toggleSidebar={() => {}} />);

    expect(screen.getByText("9+")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "알림" }));

    expect(screen.getByText("알림")).toBeInTheDocument();
    expect(useNotificationStore.getState().unreadCount).toBe(0);
    expect(consoleError).not.toHaveBeenCalled();
    consoleError.mockRestore();
  });
});
