import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { RouteErrorBoundary, RouteErrorPage } from "./RouteErrorPage";

function BrokenPage() {
  throw new Error("route render failed");
}

describe("RouteErrorPage", () => {
  it("shows an actionable error when a child route fails to render", () => {
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});

    render(
      <RouteErrorBoundary>
        <BrokenPage />
      </RouteErrorBoundary>,
    );

    expect(screen.getByRole("alert")).toHaveTextContent("페이지를 불러오지 못했습니다");
    expect(screen.getByRole("button", { name: "다시 시도" })).toBeInTheDocument();
    consoleError.mockRestore();
  });

  it("runs the provided retry action", async () => {
    const onRetry = vi.fn();
    render(<RouteErrorPage onRetry={onRetry} />);

    await userEvent.click(screen.getByRole("button", { name: "다시 시도" }));

    expect(onRetry).toHaveBeenCalledOnce();
  });

  it("recovers when navigation changes the boundary reset key", async () => {
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    const { rerender } = render(
      <RouteErrorBoundary resetKey="failed-route">
        <BrokenPage />
      </RouteErrorBoundary>,
    );

    expect(screen.getByRole("alert")).toBeInTheDocument();

    rerender(
      <RouteErrorBoundary resetKey="healthy-route">
        <h1>정상 화면</h1>
      </RouteErrorBoundary>,
    );

    expect(await screen.findByRole("heading", { name: "정상 화면" })).toBeInTheDocument();
    consoleError.mockRestore();
  });
});
