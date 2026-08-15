import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Button } from "./shadcn/button";
import { PageHeader } from "./PageHeader";

describe("PageHeader", () => {
  it("renders the page title as the single primary heading with its description", () => {
    render(
      <PageHeader
        title="주문 관리"
        description="주문을 생성하고 내역을 관리합니다."
      />,
    );

    expect(screen.getByRole("heading", { level: 1, name: "주문 관리" })).toBeInTheDocument();
    expect(screen.getAllByRole("heading", { level: 1 })).toHaveLength(1);
    expect(screen.getByText("주문을 생성하고 내역을 관리합니다.")).toBeInTheDocument();
  });

  it("keeps a page-level action beside the heading content", () => {
    render(
      <PageHeader
        title="차트 분석"
        description="수집된 종목을 분석합니다."
        actions={<Button type="button">수집 종목 새로고침</Button>}
      />,
    );

    expect(screen.getByRole("button", { name: "수집 종목 새로고침" })).toBeInTheDocument();
  });
});
