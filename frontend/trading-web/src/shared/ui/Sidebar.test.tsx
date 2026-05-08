import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { Sidebar } from "./Sidebar";

describe("Sidebar", () => {
  it("shows tax summary menu", () => {
    render(
      <MemoryRouter>
        <Sidebar isOpen={true} setOpen={() => {}} />
      </MemoryRouter>
    );

    expect(screen.getByRole("link", { name: "세금 요약" })).toBeInTheDocument();
  });

  it("shows market bars chart menu", () => {
    render(
      <MemoryRouter>
        <Sidebar isOpen={true} setOpen={() => {}} />
      </MemoryRouter>
    );

    expect(screen.getByRole("link", { name: "분봉 히스토리 차트" })).toBeInTheDocument();
  });
});
