import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { KisPanel } from "./KisPanel";
import { fetchJson } from "../../../shared/api";

const statusBarLoadingValues: boolean[] = [];

vi.mock("../../../shared/api", async () => {
  const actual = await vi.importActual<typeof import("../../../shared/api")>("../../../shared/api");
  return {
    ...actual,
    fetchJson: vi.fn()
  };
});

vi.mock("../../../shared/ui", async () => {
  const actual = await vi.importActual<typeof import("../../../shared/ui")>("../../../shared/ui");
  return {
    ...actual,
    StatusBar: ({ loading, message }: { loading: boolean; message: string }) => {
      statusBarLoadingValues.push(loading);
      return <div data-testid="status-bar" data-loading={String(loading)}>{message}</div>;
    }
  };
});

vi.mock("./KisModeList", () => ({
  KisModeList: () => <div>Mock KisModeList</div>
}));

vi.mock("./KisSearchList", () => ({
  KisSearchList: () => <div>Mock KisSearchList</div>
}));

const mockFetchJson = vi.mocked(fetchJson);

function renderPanel() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } }
  });
  return render(
    <QueryClientProvider client={client}>
      <KisPanel />
    </QueryClientProvider>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  statusBarLoadingValues.length = 0;
  mockFetchJson.mockImplementation(async (path: string) => {
    if (path === "/api/kis/ws/subscriptions") return { paper: [], live: [] };
    if (path.startsWith("/api/kis/symbols/catalog")) {
      return { items: [], returnedCount: 0, totalCatalogCount: 0, totalSubscribedCount: 0 };
    }
    if (path.startsWith("/api/kis/symbols/subscriptions")) return { items: [], returnedCount: 0 };
    if (path === "/api/subscriptions/status") {
      return {
        generatedAt: "2026-05-05T00:00:00Z",
        totalWsSlotUsed: 2,
        totalWsSlotMax: 50,
        modes: [
          {
            mode: "paper",
            connectionStatus: "CONNECTED",
            lastConnectedAt: "2026-05-05T00:00:00Z",
            reconnectAttempts: 0,
            wsSymbols: ["005930"],
            restSymbols: ["000660"],
            wsSlotUsed: 1,
            wsSlotMax: 25
          }
        ]
      };
    }
    throw new Error(`Unhandled path: ${path}`);
  });
});

describe("KisPanel monitor section", () => {
  it("renders read-only monitor data from /api/subscriptions/status", async () => {
    renderPanel();

    expect(await screen.findByText("구독 상태 모니터링 (읽기 전용)")).toBeInTheDocument();
    expect(await screen.findByText("생성 시각: 2026-05-05T00:00:00Z")).toBeInTheDocument();
    expect(await screen.findByText("전역 WS 슬롯: 2 / 50")).toBeInTheDocument();
    expect(await screen.findByText("paper")).toBeInTheDocument();
    expect(await screen.findByText("CONNECTED")).toBeInTheDocument();
    expect(await screen.findByText(/005930/)).toBeInTheDocument();
    expect(await screen.findByText(/000660/)).toBeInTheDocument();
  });

  it("supports manual refresh and does not render mutation action inside monitor section", async () => {
    renderPanel();
    const sectionTitle = await screen.findByText("구독 상태 모니터링 (읽기 전용)");
    const sectionRoot = sectionTitle.closest("section") ?? sectionTitle.parentElement;
    expect(sectionRoot).not.toBeNull();
    const scoped = within(sectionRoot as HTMLElement);
    expect(scoped.queryByRole("button", { name: "구독 추가" })).not.toBeInTheDocument();
    expect(scoped.queryByRole("button", { name: "구독 해지" })).not.toBeInTheDocument();

    const before = mockFetchJson.mock.calls.filter(([path]) => path === "/api/subscriptions/status").length;
    fireEvent.click(screen.getByRole("button", { name: "새로고침" }));
    await waitFor(() => {
      const after = mockFetchJson.mock.calls.filter(([path]) => path === "/api/subscriptions/status").length;
      expect(after).toBeGreaterThan(before);
    });
  });

  it("keeps global loading off while monitor refetch is in-flight", async () => {
    let statusCallCount = 0;
    let resolvePendingStatus: ((value: unknown) => void) | null = null;

    mockFetchJson.mockImplementation((path: string) => {
      if (path === "/api/kis/ws/subscriptions") return Promise.resolve({ paper: [], live: [] });
      if (path.startsWith("/api/kis/symbols/catalog")) {
        return Promise.resolve({ items: [], returnedCount: 0, totalCatalogCount: 0, totalSubscribedCount: 0 });
      }
      if (path.startsWith("/api/kis/symbols/subscriptions")) return Promise.resolve({ items: [], returnedCount: 0 });
      if (path === "/api/subscriptions/status") {
        statusCallCount += 1;
        if (statusCallCount === 1) {
          return Promise.resolve({
            generatedAt: "2026-05-05T00:00:00Z",
            totalWsSlotUsed: 2,
            totalWsSlotMax: 50,
            modes: []
          });
        }
        return new Promise((resolve) => {
          resolvePendingStatus = resolve;
        });
      }
      return Promise.reject(new Error(`Unhandled path: ${path}`));
    });

    renderPanel();
    await screen.findByText("구독 상태 모니터링 (읽기 전용)");
    await waitFor(() => {
      expect(screen.getByTestId("status-bar")).toHaveAttribute("data-loading", "false");
    });

    fireEvent.click(screen.getByRole("button", { name: "새로고침" }));
    await waitFor(() => {
      expect(statusCallCount).toBeGreaterThanOrEqual(2);
    });
    expect(screen.getByTestId("status-bar")).toHaveAttribute("data-loading", "false");

    resolvePendingStatus?.({
      generatedAt: "2026-05-05T00:00:10Z",
      totalWsSlotUsed: 3,
      totalWsSlotMax: 50,
      modes: []
    });
    await waitFor(() => {
      expect(screen.getByText("생성 시각: 2026-05-05T00:00:10Z")).toBeInTheDocument();
    });
  });
});
