import { describe, expect, it, vi, beforeEach } from "vitest";
import {
  fetchTradingJournalDetail,
  fetchTradingJournalList,
  updateTradingJournal,
} from "./tradingJournalApi";

function mockFetch(body: unknown, status = 200): ReturnType<typeof vi.fn> {
  const json = async () => body;
  const text = async () => (typeof body === "string" ? body : JSON.stringify(body));
  return vi.fn().mockResolvedValue({ ok: status >= 200 && status < 300, status, statusText: "OK", json, text });
}

beforeEach(() => {
  vi.restoreAllMocks();
});

describe("tradingJournalApi", () => {
  it("fetches trading journal list with query params", async () => {
    vi.stubGlobal("fetch", mockFetch({ items: [], total: 0, page: 0, size: 20 }));
    await fetchTradingJournalList({ accountId: 3, ticker: "AAPL", from: "2026-01-01", to: "2026-01-31", page: 0, size: 20 });
    const [url] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0] as [string, RequestInit?];
    expect(url).toContain("/api/trading-journals");
    expect(url).toContain("accountId=3");
    expect(url).toContain("ticker=AAPL");
    expect(url).toContain("from=2026-01-01");
    expect(url).toContain("to=2026-01-31");
    expect(url).toContain("page=0");
    expect(url).toContain("size=20");
  });

  it("fetches trading journal detail", async () => {
    vi.stubGlobal("fetch", mockFetch({ journalId: 11 }));
    await fetchTradingJournalDetail(11);
    const [url] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0] as [string, RequestInit?];
    expect(url).toContain("/api/trading-journals/11");
  });

  it("updates trading journal with PATCH body", async () => {
    vi.stubGlobal("fetch", mockFetch({ journalId: 11, status: "UPDATED" }));
    await updateTradingJournal(11, { title: "patched", content: "content", sentiment: "NEUTRAL" });
    const [url, init] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0] as [string, RequestInit];
    expect(url).toContain("/api/trading-journals/11");
    expect(init.method).toBe("PATCH");
    expect(JSON.parse(init.body as string)).toEqual({ title: "patched", content: "content", sentiment: "NEUTRAL" });
  });

  it("propagates 400 error from list request", async () => {
    vi.stubGlobal("fetch", mockFetch({ message: "bad request" }, 400));
    await expect(
      fetchTradingJournalList({ accountId: 3, from: "2026-01-31", to: "2026-01-01", page: 0, size: 20 })
    ).rejects.toThrow("400");
  });

  it("propagates 404 error from detail request", async () => {
    vi.stubGlobal("fetch", mockFetch({ message: "not found" }, 404));
    await expect(fetchTradingJournalDetail(999)).rejects.toThrow("404");
  });

  it("propagates 409 error from update request", async () => {
    vi.stubGlobal("fetch", mockFetch({ message: "conflict" }, 409));
    await expect(
      updateTradingJournal(11, { title: "patched", content: "content", sentiment: "NEUTRAL" })
    ).rejects.toThrow("409");
  });
});
