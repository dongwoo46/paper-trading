import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TradingJournalPage } from "./TradingJournalPage";

vi.mock("../../../entities/account/api/accountApi", () => ({ fetchAccounts: vi.fn() }));
vi.mock("../../../entities/trading-journal/api/tradingJournalApi", () => ({
  fetchTradingJournalList: vi.fn(),
  fetchTradingJournalDetail: vi.fn(),
  updateTradingJournal: vi.fn(),
}));

import { fetchAccounts } from "../../../entities/account/api/accountApi";
import {
  fetchTradingJournalDetail,
  fetchTradingJournalList,
  updateTradingJournal,
} from "../../../entities/trading-journal/api/tradingJournalApi";

const mockedFetchAccounts = vi.mocked(fetchAccounts);
const mockedFetchTradingJournalList = vi.mocked(fetchTradingJournalList);
const mockedFetchTradingJournalDetail = vi.mocked(fetchTradingJournalDetail);
const mockedUpdateTradingJournal = vi.mocked(updateTradingJournal);

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <TradingJournalPage />
    </QueryClientProvider>
  );
}

describe("TradingJournalPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("updates list query by filter and saves detail", async () => {
    mockedFetchAccounts.mockResolvedValue([
      {
        id: 1, accountName: "주계좌", accountType: "PAPER", tradingMode: "LOCAL", deposit: "0", availableDeposit: "0",
        lockedDeposit: "0", baseCurrency: "KRW", externalAccountId: null, isActive: true, createdAt: null, updatedAt: null,
      },
    ]);
    mockedFetchTradingJournalList.mockResolvedValue({
      items: [{
        journalId: 10,
        accountId: 1,
        orderId: 77,
        ticker: "AAPL",
        journalType: "MANUAL_NOTE",
        sentiment: "BULLISH",
        title: "매수 기록",
        summary: "요약",
        createdAt: "2026-05-01T00:00:00Z",
        updatedAt: "2026-05-01T00:00:00Z",
      }],
      total: 1,
      page: 0,
      size: 20,
    });
    mockedFetchTradingJournalDetail.mockResolvedValue({
      journalId: 10,
      accountId: 1,
      orderId: 77,
      ticker: "AAPL",
      journalType: "MANUAL_NOTE",
      sentiment: "BULLISH",
      title: "매수 기록",
      content: "hello",
      summary: "요약",
      createdAt: "2026-05-01T00:00:00Z",
      updatedAt: "2026-05-01T00:00:00Z",
    });
    mockedUpdateTradingJournal.mockResolvedValue({ journalId: 10, status: "UPDATED" });

    const user = userEvent.setup();
    renderPage();

    await waitFor(() => expect(mockedFetchTradingJournalList).toHaveBeenCalled());
    await user.type(screen.getByLabelText("티커"), "MSFT");
    await user.click(screen.getByRole("button", { name: "조회" }));
    await waitFor(() => {
      expect(mockedFetchTradingJournalList).toHaveBeenLastCalledWith(
        expect.objectContaining({ ticker: "MSFT" })
      );
    });

    await user.click(screen.getByRole("button", { name: "AAPL" }));
    await screen.findByText("일지 상세");

    const title = screen.getByLabelText("제목");
    await user.clear(title);
    await user.type(title, "수정 제목");
    const content = screen.getByLabelText("내용");
    await user.clear(content);
    await user.type(content, "updated");
    const saveButton = screen.getByRole("button", { name: "저장" });
    await user.click(saveButton);

    expect(mockedUpdateTradingJournal).toHaveBeenCalledWith(
      10,
      expect.objectContaining({ title: "수정 제목", content: "updated", sentiment: "BULLISH" })
    );
  });
});
