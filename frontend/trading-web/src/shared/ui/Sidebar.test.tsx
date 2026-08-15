import { useState } from "react";
import { act, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { Sidebar } from "./Sidebar";

function MobileSidebarHarness() {
  const [isOpen, setOpen] = useState(false);

  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>
        메뉴 열기 테스트
      </button>
      <main>
        <a href="#content">본문 작업</a>
      </main>
      <Sidebar isOpen={isOpen} setOpen={setOpen} />
    </>
  );
}

function DesktopKeyboardHarness({ setOpen }: { setOpen: (value: boolean) => void }) {
  return (
    <>
      <Sidebar isOpen={true} setOpen={setOpen} />
      <button type="button">사이드바 뒤 작업</button>
    </>
  );
}

describe("Sidebar", () => {
  it("renders every route under the four workstation groups", () => {
    render(
      <MemoryRouter>
        <Sidebar isOpen={true} setOpen={() => {}} />
      </MemoryRouter>,
    );

    expect(
      screen.getAllByRole("heading", { level: 2 }).map((heading) => heading.textContent),
    ).toEqual(["개요", "트레이딩", "시장 데이터", "분석"]);
    expect(screen.getAllByRole("link")).toHaveLength(11);
    expect(screen.getByRole("link", { name: "세금 요약" })).toBeInTheDocument();
  });

  it("closes the mobile navigation after selecting a destination", async () => {
    const setOpen = vi.fn();
    Object.defineProperty(window, "innerWidth", { value: 390, configurable: true });

    render(
      <MemoryRouter>
        <Sidebar isOpen={true} setOpen={setOpen} />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByRole("link", { name: "주문 관리" }));

    expect(setOpen).toHaveBeenCalledWith(false);
  });

  it("keeps focus inside an open mobile drawer and restores it when closed", async () => {
    Object.defineProperty(window, "innerWidth", { value: 390, configurable: true });
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <MobileSidebarHarness />
      </MemoryRouter>,
    );

    const openButton = screen.getByRole("button", { name: "메뉴 열기 테스트" });
    const sidebar = screen.getByLabelText("주요 내비게이션");
    expect(sidebar).toHaveAttribute("inert");
    expect(sidebar).toHaveAttribute("aria-hidden", "true");

    await user.click(openButton);

    expect(sidebar).not.toHaveAttribute("inert");
    expect(document.querySelector("main")).toHaveAttribute("inert");
    expect(
      within(sidebar).getByRole("button", { name: "사이드바 닫기" }),
    ).toHaveFocus();

    await user.keyboard("{Escape}");

    expect(sidebar).toHaveAttribute("inert");
    expect(document.querySelector("main")).not.toHaveAttribute("inert");
    expect(openButton).toHaveFocus();
  });

  it("does not apply drawer-only Escape handling on desktop", async () => {
    Object.defineProperty(window, "innerWidth", { value: 1280, configurable: true });
    const setOpen = vi.fn();
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <Sidebar isOpen={true} setOpen={setOpen} />
      </MemoryRouter>,
    );

    screen.getByRole("link", { name: "홈" }).focus();
    await user.keyboard("{Escape}");

    expect(setOpen).not.toHaveBeenCalled();
  });

  it("keeps the sidebar exposed at the exact desktop breakpoint", () => {
    Object.defineProperty(window, "innerWidth", { value: 1024, configurable: true });

    render(
      <MemoryRouter>
        <Sidebar isOpen={false} setOpen={() => {}} />
      </MemoryRouter>,
    );

    const sidebar = screen.getByLabelText("주요 내비게이션");
    expect(sidebar).not.toHaveAttribute("aria-hidden");
    expect(sidebar).not.toHaveAttribute("inert");
  });

  it("does not apply drawer-only keyboard handling at the exact desktop breakpoint", async () => {
    Object.defineProperty(window, "innerWidth", { value: 1024, configurable: true });
    const setOpen = vi.fn();
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <DesktopKeyboardHarness setOpen={setOpen} />
      </MemoryRouter>,
    );

    const sidebar = screen.getByLabelText("주요 내비게이션");
    within(sidebar).getByRole("link", { name: "차트 분석" }).focus();
    await user.tab();

    expect(screen.getByRole("button", { name: "사이드바 뒤 작업" })).toHaveFocus();

    within(sidebar).getByRole("link", { name: "홈" }).focus();
    await user.keyboard("{Escape}");

    expect(setOpen).not.toHaveBeenCalled();
  });

  it("removes mobile drawer isolation when an open drawer reaches the desktop breakpoint", async () => {
    Object.defineProperty(window, "innerWidth", { value: 390, configurable: true });
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <MobileSidebarHarness />
      </MemoryRouter>,
    );

    await user.click(screen.getByRole("button", { name: "메뉴 열기 테스트" }));
    expect(document.querySelector("main")).toHaveAttribute("inert");

    Object.defineProperty(window, "innerWidth", { value: 1024, configurable: true });
    act(() => {
      window.dispatchEvent(new Event("resize"));
    });

    expect(document.querySelector("main")).not.toHaveAttribute("inert");
  });
});
