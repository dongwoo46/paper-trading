import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, useLocation } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import App from "./App";

vi.mock("./shared/api/chartAnalysisApi", async () => {
  const actual = await vi.importActual<typeof import("./shared/api/chartAnalysisApi")>(
    "./shared/api/chartAnalysisApi",
  );
  return {
    ...actual,
    subscribeAnalysisNotifications: vi.fn(() => () => {}),
  };
});

vi.mock("./features/execution-toast/ui/ExecutionToastProvider", () => ({
  ExecutionToastProvider: () => null,
}));

vi.mock("./pages/order/ui/OrderPage", () => {
  throw new Error("simulated route chunk failure");
});

function LocationProbe() {
  const location = useLocation();
  return <output aria-label="현재 경로">{location.pathname}</output>;
}

describe("App routes", () => {
  it("replaces an unknown URL with the home route", async () => {
    render(
      <MemoryRouter initialEntries={["/not-registered"]}>
        <App />
        <LocationProbe />
      </MemoryRouter>,
    );

    expect(
      await screen.findByRole("heading", { level: 1, name: "오늘의 작업" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("status", { name: "현재 경로" })).toHaveTextContent("/");
  });

  it("recovers from a failed lazy route when the user navigates home", async () => {
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={["/orders"]}>
        <App />
        <LocationProbe />
      </MemoryRouter>,
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "페이지를 불러오지 못했습니다",
    );

    await user.click(screen.getByRole("button", { name: "사이드바 열기" }));
    await user.click(screen.getByRole("link", { name: "홈" }));

    expect(
      await screen.findByRole("heading", { level: 1, name: "오늘의 작업" }),
    ).toBeInTheDocument();
    consoleError.mockRestore();
  });

  it("removes mobile drawer isolation when the viewport grows to desktop", async () => {
    Object.defineProperty(window, "innerWidth", { value: 390, configurable: true });
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>,
    );

    await user.click(screen.getByRole("button", { name: "사이드바 열기" }));
    expect(document.querySelector("main")).toHaveAttribute("inert");

    Object.defineProperty(window, "innerWidth", { value: 1280, configurable: true });
    act(() => {
      window.dispatchEvent(new Event("resize"));
    });

    expect(document.querySelector("main")).not.toHaveAttribute("inert");
    expect(screen.getByLabelText("주요 내비게이션")).not.toHaveAttribute("inert");
  });
});
