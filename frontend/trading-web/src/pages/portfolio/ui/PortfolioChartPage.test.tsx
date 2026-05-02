import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PortfolioChartPage } from "./PortfolioChartPage";

vi.mock("../../../entities/account/api/accountApi", () => ({
  fetchAccounts: vi.fn(),
}));

vi.mock("../../../shared/api/portfolioApi", () => ({
  fetchDailyBalances: vi.fn(),
  fetchKospiBenchmark: vi.fn(),
}));

import { fetchAccounts } from "../../../entities/account/api/accountApi";
import { fetchDailyBalances, fetchKospiBenchmark } from "../../../shared/api/portfolioApi";

const mockFetchAccounts = vi.mocked(fetchAccounts);
const mockFetchDailyBalances = vi.mocked(fetchDailyBalances);
const mockFetchKospiBenchmark = vi.mocked(fetchKospiBenchmark);

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe("PortfolioChartPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockFetchAccounts.mockResolvedValue([
      { id: 1, accountName: "A", isActive: true },
      { id: 2, accountName: "B", isActive: true },
    ] as never);
    mockFetchDailyBalances.mockResolvedValue([
      { date: "2026-04-01", evaluationAmount: 1000, deposit: 900 },
      { date: "2026-04-02", evaluationAmount: 1100, deposit: 900 },
    ]);
    mockFetchKospiBenchmark.mockResolvedValue([
      { date: "2026-04-01", close: 2000 },
      { date: "2026-04-02", close: 2100 },
    ]);
  });

  it("refetches chart data when account changes", async () => {
    render(<PortfolioChartPage />, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(mockFetchDailyBalances).toHaveBeenCalled();
    });

    const select = await screen.findByLabelText("계좌 선택");
    await userEvent.selectOptions(select, "2");

    await waitFor(() => {
      expect(mockFetchDailyBalances).toHaveBeenLastCalledWith(
        2,
        expect.any(String),
        expect.any(String)
      );
    });
  });

  it("renders portfolio-only rows when benchmark API returns 404", async () => {
    mockFetchKospiBenchmark.mockRejectedValue(new Error("404 Not Found"));

    render(<PortfolioChartPage />, { wrapper: createWrapper() });

    expect(await screen.findByText("2026-04-01")).toBeInTheDocument();
    expect(screen.getByText("일부 날짜의 벤치마크 데이터가 없어 해당 포인트를 제외했습니다.")).toBeInTheDocument();
  });
});
