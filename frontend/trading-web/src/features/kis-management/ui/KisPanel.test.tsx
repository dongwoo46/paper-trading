import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { KisPanel } from "./KisPanel";

vi.mock("../../../shared/api", () => ({
  fetchJson: vi.fn(),
  normalizeByModes: (data: Record<string, string[]> | null | undefined) => ({
    paper: data?.paper ?? [],
    live: data?.live ?? []
  })
}));

import { fetchJson } from "../../../shared/api";

const mockFetchJson = vi.mocked(fetchJson);

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false }
    }
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

function setupDefaultFetchMock() {
  mockFetchJson.mockImplementation((path: string, init?: RequestInit) => {
    if (path.startsWith("/api/kis/symbols/catalog")) {
      return Promise.resolve({
        items: [],
        returnedCount: 0,
        totalCatalogCount: 0,
        totalSubscribedCount: 0
      });
    }

    if (path.includes("/api/kis/symbols/subscriptions") && !init?.method) {
      return Promise.resolve({ items: [], returnedCount: 0 });
    }

    if (path.includes("/api/kis/ws/subscriptions")) {
      return Promise.resolve({ paper: [], live: [] });
    }

    if (path.includes("/api/kis/rest/watchlist/price")) {
      return Promise.resolve({});
    }

    if (path === "/api/kis/symbols/subscriptions" && (init?.method === "POST" || init?.method === "DELETE")) {
      return Promise.resolve({ status: "ok", totalSelected: 1 });
    }
    if (path.startsWith("/api/subscriptions/favorites")) {
      if (!init?.method) return Promise.resolve({ mode: "paper", channel: "ws", items: ["005930"], returnedCount: 1, status: "ok" });
      return Promise.resolve({ status: "added", mode: "paper", channel: "ws", symbol: "005930", totalSelected: 1 });
    }
    if (path.startsWith("/api/subscriptions/strategy-symbols")) {
      if (!init?.method) return Promise.resolve({ mode: "paper", items: ["000660"], returnedCount: 1, status: "ok" });
      return Promise.resolve({ status: "added", mode: "paper", symbol: "000660", totalSelected: 1 });
    }
    if (path.startsWith("/api/subscriptions/routing-status")) {
      return Promise.resolve({
        generatedAt: "2026-05-05T10:15:30Z",
        mode: "paper",
        ws: { slotUsed: 1, slotMax: 40, symbols: ["005930"] },
        rest: { symbols: ["000660"] },
        sources: { manual: ["005930"], favorites: ["000660"], strategyPriority: [] },
        status: "ok"
      });
    }

    return Promise.resolve({});
  });
}

describe("KisPanel manual routing actions", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupDefaultFetchMock();
  });

  it("shows validation error and does not call write API when symbol is empty", async () => {
    const user = userEvent.setup();
    render(<KisPanel />, { wrapper: createWrapper() });

    await screen.findByText("한국투자증권(KIS) 종목 카탈로그");
    const routingSection = screen.getByRole("heading", { name: "KIS 구독 라우팅 제어 (Manual)" }).closest("section");
    if (!routingSection) throw new Error("routing section not found");

    await user.click(within(routingSection).getByRole("button", { name: "구독 추가" }));

    expect(await screen.findByText("종목코드를 입력해 주세요.")).toBeInTheDocument();
    expect(
      mockFetchJson.mock.calls.some(([path, init]) => path === "/api/kis/symbols/subscriptions" && init?.method === "POST")
    ).toBe(false);
  });

  it("submits manual subscribe successfully", async () => {
    const user = userEvent.setup();
    render(<KisPanel />, { wrapper: createWrapper() });

    await screen.findByText("한국투자증권(KIS) 종목 카탈로그");
    const routingSection = screen.getByRole("heading", { name: "KIS 구독 라우팅 제어 (Manual)" }).closest("section");
    if (!routingSection) throw new Error("routing section not found");

    await user.type(within(routingSection).getByPlaceholderText("종목코드"), "005930");
    await user.click(within(routingSection).getByRole("button", { name: "구독 추가" }));

    await waitFor(() => {
      expect(
        mockFetchJson.mock.calls.some(([path, init]) => path === "/api/kis/symbols/subscriptions" && init?.method === "POST")
      ).toBe(true);
    });

    expect(await screen.findByText("구독 상태가 변경되었습니다.")).toBeInTheDocument();
  });

  it("prevents duplicate in-flight writes for same action", async () => {
    const user = userEvent.setup();

    let resolveMutation: ((value: unknown) => void) | null = null;
    mockFetchJson.mockImplementation((path: string, init?: RequestInit) => {
      if (path.startsWith("/api/kis/symbols/catalog")) {
        return Promise.resolve({ items: [], returnedCount: 0, totalCatalogCount: 0, totalSubscribedCount: 0 });
      }
      if (path.includes("/api/kis/symbols/subscriptions") && !init?.method) {
        return Promise.resolve({ items: [], returnedCount: 0 });
      }
      if (path.includes("/api/kis/ws/subscriptions")) {
        return Promise.resolve({ paper: [], live: [] });
      }
      if (path === "/api/kis/symbols/subscriptions" && init?.method === "POST") {
        return new Promise((resolve) => {
          resolveMutation = resolve;
        });
      }
      if (path.startsWith("/api/subscriptions/favorites")) {
        if (!init?.method) return Promise.resolve({ mode: "paper", channel: "ws", items: [], returnedCount: 0, status: "ok" });
        return Promise.resolve({ status: "added", mode: "paper", channel: "ws", symbol: "005930", totalSelected: 1 });
      }
      if (path.startsWith("/api/subscriptions/strategy-symbols")) {
        if (!init?.method) return Promise.resolve({ mode: "paper", items: [], returnedCount: 0, status: "ok" });
        return Promise.resolve({ status: "added", mode: "paper", symbol: "005930", totalSelected: 1 });
      }
      if (path.startsWith("/api/subscriptions/routing-status")) {
        return Promise.resolve({
          generatedAt: "2026-05-05T10:15:30Z",
          mode: "paper",
          ws: { slotUsed: 0, slotMax: 40, symbols: [] },
          rest: { symbols: [] },
          sources: { manual: [], favorites: [], strategyPriority: [] },
          status: "ok"
        });
      }
      return Promise.resolve({});
    });

    render(<KisPanel />, { wrapper: createWrapper() });

    await screen.findByText("한국투자증권(KIS) 종목 카탈로그");
    const routingSection = screen.getByRole("heading", { name: "KIS 구독 라우팅 제어 (Manual)" }).closest("section");
    if (!routingSection) throw new Error("routing section not found");

    await user.type(within(routingSection).getByPlaceholderText("종목코드"), "005930");

    const addButton = within(routingSection).getByRole("button", { name: "구독 추가" });
    await user.click(addButton);

    await waitFor(() => {
      expect(addButton).toBeDisabled();
    });

    await user.click(addButton);

    const writeCalls = mockFetchJson.mock.calls.filter(
      ([path, init]) => path === "/api/kis/symbols/subscriptions" && init?.method === "POST"
    );
    expect(writeCalls).toHaveLength(1);

    resolveMutation?.({ status: "ok", totalSelected: 1 });
  });

  it("submits favorites add and strategy add after validation", async () => {
    const user = userEvent.setup();
    render(<KisPanel />, { wrapper: createWrapper() });

    await screen.findByText("한국투자증권(KIS) 종목 카탈로그");

    const favoritesSection = screen.getByRole("heading", { name: "즐겨찾기 라우팅 (Favorites)" }).closest("section");
    const strategySection = screen.getByRole("heading", { name: "전략 우선 라우팅 (Strategy Priority)" }).closest("section");
    if (!favoritesSection || !strategySection) throw new Error("routing sections not found");

    await user.clear(within(favoritesSection).getByPlaceholderText("종목코드"));
    await user.click(within(favoritesSection).getByRole("button", { name: "즐겨찾기 추가" }));
    expect(await screen.findByText("종목코드를 입력해 주세요.")).toBeInTheDocument();

    await user.type(within(favoritesSection).getByPlaceholderText("종목코드"), "005930");
    await user.click(within(favoritesSection).getByRole("button", { name: "즐겨찾기 추가" }));

    await waitFor(() => {
      expect(
        mockFetchJson.mock.calls.some(([path, init]) => path === "/api/subscriptions/favorites" && init?.method === "POST")
      ).toBe(true);
    });

    await user.type(within(strategySection).getByPlaceholderText("종목코드"), "000660");
    await user.click(within(strategySection).getByRole("button", { name: "전략 종목 추가" }));

    await waitFor(() => {
      expect(
        mockFetchJson.mock.calls.some(([path, init]) => path === "/api/subscriptions/strategy-symbols" && init?.method === "POST")
      ).toBe(true);
    });
  });

  it("supports routing status refresh and failed write retry with original payload replay", async () => {
    const user = userEvent.setup();

    let shouldFailFavorites = true;
    mockFetchJson.mockImplementation((path: string, init?: RequestInit) => {
      if (path.startsWith("/api/kis/symbols/catalog")) return Promise.resolve({ items: [], returnedCount: 0, totalCatalogCount: 0, totalSubscribedCount: 0 });
      if (path.includes("/api/kis/symbols/subscriptions") && !init?.method) return Promise.resolve({ items: [], returnedCount: 0 });
      if (path.includes("/api/kis/ws/subscriptions")) return Promise.resolve({ paper: [], live: [] });
      if (path.startsWith("/api/subscriptions/favorites")) {
        if (!init?.method) return Promise.resolve({ mode: "paper", channel: "ws", items: [], returnedCount: 0, status: "ok" });
        if (init?.method === "POST" && shouldFailFavorites) {
          shouldFailFavorites = false;
          return Promise.reject(new Error("500 Internal Server Error: temporary"));
        }
        return Promise.resolve({ status: "added", mode: "paper", channel: "ws", symbol: "005930", totalSelected: 1 });
      }
      if (path.startsWith("/api/subscriptions/strategy-symbols")) return Promise.resolve({ mode: "paper", items: [], returnedCount: 0, status: "ok" });
      if (path.startsWith("/api/subscriptions/routing-status")) {
        return Promise.resolve({
          generatedAt: "2026-05-05T10:15:30Z",
          mode: "paper",
          ws: { slotUsed: 1, slotMax: 40, symbols: ["005930"] },
          rest: { symbols: [] },
          sources: { manual: ["005930"], favorites: [], strategyPriority: [] },
          status: "ok"
        });
      }
      return Promise.resolve({});
    });

    render(<KisPanel />, { wrapper: createWrapper() });
    await screen.findByText("한국투자증권(KIS) 종목 카탈로그");

    const favoritesSection = screen.getByRole("heading", { name: "즐겨찾기 라우팅 (Favorites)" }).closest("section");
    const statusSection = screen.getByRole("heading", { name: "라우팅 상태 검증 (Routing Status)" }).closest("section");
    if (!favoritesSection || !statusSection) throw new Error("section not found");

    await user.type(within(favoritesSection).getByPlaceholderText("종목코드"), "005930");
    await user.click(within(favoritesSection).getByRole("button", { name: "즐겨찾기 추가" }));
    expect(await screen.findByText(/요청 실패:/)).toBeInTheDocument();

    await user.clear(within(favoritesSection).getByPlaceholderText("종목코드"));
    await user.type(within(favoritesSection).getByPlaceholderText("종목코드"), "000660");
    await user.click(within(favoritesSection).getByRole("button", { name: "실패한 요청 재시도" }));
    await waitFor(() => {
      expect(
        mockFetchJson.mock.calls.filter(([path, init]) => path === "/api/subscriptions/favorites" && init?.method === "POST")
      ).toHaveLength(2);
    });
    const favoritePostCalls = mockFetchJson.mock.calls.filter(
      ([path, init]) => path === "/api/subscriptions/favorites" && init?.method === "POST"
    );
    expect(JSON.parse(favoritePostCalls[0][1]?.body as string).symbol).toBe("005930");
    expect(JSON.parse(favoritePostCalls[1][1]?.body as string).symbol).toBe("005930");

    const before = mockFetchJson.mock.calls.filter(([path]) => path.startsWith("/api/subscriptions/routing-status")).length;
    await user.click(within(statusSection).getByRole("button", { name: "상태 새로고침" }));
    await waitFor(() => {
      const after = mockFetchJson.mock.calls.filter(([path]) => path.startsWith("/api/subscriptions/routing-status")).length;
      expect(after).toBeGreaterThan(before);
    });
  });
});
