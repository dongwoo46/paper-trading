import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { fetchMarketBars } from "../../../shared/api/marketBarsApi";
import { MarketBarsChartPage } from "./MarketBarsChartPage";

vi.mock("../../../shared/api/marketBarsApi", () => ({
  fetchMarketBars: vi.fn(),
}));

const mockFetchMarketBars = vi.mocked(fetchMarketBars);

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

const barFixture = {
  startedAt: "2026-05-08T01:00:00Z",
  open: 100,
  high: 110,
  low: 90,
  close: 105,
  volume: 1000,
  tradeValue: 100000,
  vwap: 102,
  tickCount: 30,
};

describe("MarketBarsChartPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("requests with default interval and changed limit", async () => {
    mockFetchMarketBars.mockResolvedValue([barFixture]);

    render(<MarketBarsChartPage />, { wrapper: createWrapper() });

    await userEvent.clear(screen.getByLabelText("심볼 입력"));
    await userEvent.type(screen.getByLabelText("심볼 입력"), "005930");
    await userEvent.click(screen.getByRole("button", { name: "조회" }));

    await waitFor(() => {
      expect(mockFetchMarketBars).toHaveBeenCalledWith({ symbol: "005930", interval: "1m", limit: 100 });
    });

    await userEvent.selectOptions(screen.getByLabelText("분봉 개수"), "200");
    await waitFor(() => {
      expect(mockFetchMarketBars).toHaveBeenLastCalledWith({ symbol: "005930", interval: "1m", limit: 200 });
    });
  });

  it("switches interval tab and refetches", async () => {
    mockFetchMarketBars.mockResolvedValue([]);

    render(<MarketBarsChartPage />, { wrapper: createWrapper() });

    await userEvent.type(screen.getByLabelText("심볼 입력"), "AAPL");
    await userEvent.click(screen.getByRole("button", { name: "조회" }));

    await waitFor(() => {
      expect(mockFetchMarketBars).toHaveBeenCalledWith({ symbol: "AAPL", interval: "1m", limit: 100 });
    });

    await userEvent.click(screen.getByRole("button", { name: "5m" }));
    await waitFor(() => {
      expect(mockFetchMarketBars).toHaveBeenLastCalledWith({ symbol: "AAPL", interval: "5m", limit: 100 });
    });
  });

  it("shows 400 error message", async () => {
    mockFetchMarketBars.mockRejectedValue(new Error("400 Bad Request"));

    render(<MarketBarsChartPage />, { wrapper: createWrapper() });

    await userEvent.type(screen.getByLabelText("심볼 입력"), "AAPL");
    await userEvent.click(screen.getByRole("button", { name: "조회" }));

    expect(await screen.findByText("조회 조건을 확인해 주세요.")).toBeInTheDocument();
    expect(screen.queryByText("해당 조건의 분봉 데이터가 없습니다.")).not.toBeInTheDocument();
  });

  it("treats 404 as empty state", async () => {
    mockFetchMarketBars.mockRejectedValue(new Error("404 Not Found"));

    render(<MarketBarsChartPage />, { wrapper: createWrapper() });

    await userEvent.type(screen.getByLabelText("심볼 입력"), "AAPL");
    await userEvent.click(screen.getByRole("button", { name: "조회" }));

    expect(await screen.findByText("해당 조건의 분봉 데이터가 없습니다.")).toBeInTheDocument();
  });

  it("shows loading message while fetching", async () => {
    mockFetchMarketBars.mockImplementation(() => new Promise(() => {}));

    render(<MarketBarsChartPage />, { wrapper: createWrapper() });
    await userEvent.type(screen.getByLabelText("심볼 입력"), "AAPL");
    await userEvent.click(screen.getByRole("button", { name: "조회" }));

    expect(await screen.findByText("분봉 데이터를 불러오는 중...")).toBeInTheDocument();
  });

  it("renders data area on success", async () => {
    mockFetchMarketBars.mockResolvedValue([barFixture]);

    render(<MarketBarsChartPage />, { wrapper: createWrapper() });
    await userEvent.type(screen.getByLabelText("심볼 입력"), "AAPL");
    await userEvent.click(screen.getByRole("button", { name: "조회" }));

    expect(await screen.findByText(/마지막 갱신:/)).toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "시각" })).toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "종가" })).toBeInTheDocument();
  });

  it("shows empty state for empty array response", async () => {
    mockFetchMarketBars.mockResolvedValue([]);

    render(<MarketBarsChartPage />, { wrapper: createWrapper() });
    await userEvent.type(screen.getByLabelText("심볼 입력"), "AAPL");
    await userEvent.click(screen.getByRole("button", { name: "조회" }));

    expect(await screen.findByText("해당 조건의 분봉 데이터가 없습니다.")).toBeInTheDocument();
  });

  it("shows server error message and retry button for 5xx", async () => {
    mockFetchMarketBars.mockRejectedValue(new Error("500 Internal Server Error"));

    render(<MarketBarsChartPage />, { wrapper: createWrapper() });
    await userEvent.type(screen.getByLabelText("심볼 입력"), "AAPL");
    await userEvent.click(screen.getByRole("button", { name: "조회" }));

    expect(await screen.findByText("일시적인 오류가 발생했습니다. 다시 시도해 주세요.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "재시도" })).toBeInTheDocument();
    expect(screen.queryByText("해당 조건의 분봉 데이터가 없습니다.")).not.toBeInTheDocument();
  });

  it("shows server error message and retry button for network errors", async () => {
    mockFetchMarketBars.mockRejectedValue(new Error("Network Error"));

    render(<MarketBarsChartPage />, { wrapper: createWrapper() });
    await userEvent.type(screen.getByLabelText("심볼 입력"), "AAPL");
    await userEvent.click(screen.getByRole("button", { name: "조회" }));

    expect(await screen.findByText("일시적인 오류가 발생했습니다. 다시 시도해 주세요.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "재시도" })).toBeInTheDocument();
  });
});
