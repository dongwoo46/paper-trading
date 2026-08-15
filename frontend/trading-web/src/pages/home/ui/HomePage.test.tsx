import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { HomePage } from "./HomePage";

describe("HomePage", () => {
  it("provides a task-focused entry point to every real destination", () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("heading", { level: 1, name: "오늘의 작업" }),
    ).toBeInTheDocument();
    expect(screen.getAllByRole("link")).toHaveLength(10);
    expect(screen.getByRole("link", { name: /주문 관리/ })).toHaveAttribute("href", "/orders");
    expect(screen.getByRole("link", { name: /차트 분석/ })).toHaveAttribute(
      "href",
      "/chart-analysis",
    );
  });

  it("does not invent operational metrics or readiness states", () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    );

    expect(screen.queryByText(/시스템 운영 준비 완료/)).not.toBeInTheDocument();
    expect(screen.queryByText(/통합 모니터링/)).not.toBeInTheDocument();
  });
});
