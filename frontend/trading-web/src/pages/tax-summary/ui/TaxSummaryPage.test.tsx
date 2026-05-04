import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { AccountResponse } from "../../../entities/account/model/types";
import { TaxSummaryPage } from "./TaxSummaryPage";

vi.mock("../../../entities/account/api/accountApi", () => ({
  fetchAccounts: vi.fn(),
}));

vi.mock("../../../entities/tax-summary/api/taxSummaryApi", () => ({
  fetchTaxSummary: vi.fn(),
  recalculateTaxSummary: vi.fn(),
}));

import { fetchAccounts } from "../../../entities/account/api/accountApi";
import {
  fetchTaxSummary,
  recalculateTaxSummary,
} from "../../../entities/tax-summary/api/taxSummaryApi";

const mockedFetchAccounts = vi.mocked(fetchAccounts);
const mockedFetchTaxSummary = vi.mocked(fetchTaxSummary);
const mockedRecalculateTaxSummary = vi.mocked(recalculateTaxSummary);

const accounts: AccountResponse[] = [
  {
    id: 1,
    accountName: "주계좌",
    accountType: "PAPER",
    tradingMode: "LOCAL",
    deposit: "1000000",
    availableDeposit: "1000000",
    lockedDeposit: "0",
    baseCurrency: "KRW",
    externalAccountId: null,
    isActive: true,
    createdAt: null,
    updatedAt: null,
  },
  {
    id: 2,
    accountName: "부계좌",
    accountType: "PAPER",
    tradingMode: "LOCAL",
    deposit: "2000000",
    availableDeposit: "2000000",
    lockedDeposit: "0",
    baseCurrency: "KRW",
    externalAccountId: null,
    isActive: true,
    createdAt: null,
    updatedAt: null,
  },
];

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <TaxSummaryPage />
    </QueryClientProvider>
  );
}

describe("TaxSummaryPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("shows account loading", () => {
    mockedFetchAccounts.mockImplementation(
      () => new Promise<AccountResponse[]>(() => {})
    );
    renderPage();
    expect(screen.getByText("계좌 정보를 불러오는 중...")).toBeInTheDocument();
  });

  it("shows empty account state", async () => {
    mockedFetchAccounts.mockResolvedValue([]);
    renderPage();
    expect(await screen.findByText("등록된 계좌가 없습니다.")).toBeInTheDocument();
  });

  it("calls summary API for selected account and year", async () => {
    mockedFetchAccounts.mockResolvedValue(accounts);
    mockedFetchTaxSummary.mockResolvedValue({
      accountId: 1,
      taxYear: 2026,
      realizedProfit: "100000",
      realizedLoss: "0",
      taxableBase: "100000",
      taxAmount: "22000",
      localTaxAmount: "2200",
      effectiveTaxRate: "0.242",
      status: "READY",
      computedAt: "2026-05-04T00:00:00.000Z",
    });

    const user = userEvent.setup();
    renderPage();

    await screen.findByText("세금 요약");
    await waitFor(() => {
      expect(mockedFetchTaxSummary).toHaveBeenCalledWith(1, 2026);
    });

    await user.selectOptions(screen.getByLabelText("계좌 선택"), "2");
    await waitFor(() => {
      expect(mockedFetchTaxSummary).toHaveBeenCalledWith(2, 2026);
    });

    await user.selectOptions(screen.getByLabelText("연도 선택"), "2025");
    await waitFor(() => {
      expect(mockedFetchTaxSummary).toHaveBeenCalledWith(2, 2025);
    });
  });

  it("shows summary error state", async () => {
    mockedFetchAccounts.mockResolvedValue(accounts);
    mockedFetchTaxSummary.mockRejectedValue(new Error("boom"));
    renderPage();
    expect(await screen.findByText("세금 요약을 불러오지 못했습니다.")).toBeInTheDocument();
  });

  it("shows actionable message for 400 summary error", async () => {
    mockedFetchAccounts.mockResolvedValue(accounts);
    mockedFetchTaxSummary.mockRejectedValue(new Error("400 Bad Request: invalid year"));
    renderPage();
    expect(
      await screen.findByText("요청 값이 올바르지 않습니다. 계좌/연도를 다시 확인하세요.")
    ).toBeInTheDocument();
  });

  it("shows actionable message for 404 summary error", async () => {
    mockedFetchAccounts.mockResolvedValue(accounts);
    mockedFetchTaxSummary.mockRejectedValue(new Error("404 Not Found: missing summary"));
    renderPage();
    expect(
      await screen.findByText("선택한 계좌/연도의 세금 요약이 없습니다. 연도를 변경하거나 재계산을 실행하세요.")
    ).toBeInTheDocument();
  });

  it("shows empty summary state", async () => {
    mockedFetchAccounts.mockResolvedValue(accounts);
    mockedFetchTaxSummary.mockResolvedValue(null);
    renderPage();
    expect(await screen.findByText("세금 요약 데이터가 없습니다.")).toBeInTheDocument();
  });

  it("prevents duplicate recalculate while running and refetches after success", async () => {
    mockedFetchAccounts.mockResolvedValue(accounts);
    mockedFetchTaxSummary.mockResolvedValue({
      accountId: 1,
      taxYear: 2026,
      realizedProfit: "100000",
      realizedLoss: "0",
      taxableBase: "100000",
      taxAmount: "22000",
      localTaxAmount: "2200",
      effectiveTaxRate: "0.242",
      status: "READY",
      computedAt: "2026-05-04T00:00:00.000Z",
    });

    let resolveMutation: (() => void) | null = null;
    mockedRecalculateTaxSummary.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveMutation = resolve;
        }) as Promise<unknown>
    );

    const user = userEvent.setup();
    renderPage();

    const button = await screen.findByRole("button", { name: "세금 재계산" });
    await user.dblClick(button);

    expect(mockedRecalculateTaxSummary).toHaveBeenCalledTimes(1);
    expect(button).toBeDisabled();

    resolveMutation?.();

    await waitFor(() => {
      expect(mockedFetchTaxSummary.mock.calls.length).toBeGreaterThanOrEqual(2);
    });
  });

  it("shows running guidance for 409 recalculate error and blocks duplicate retry", async () => {
    mockedFetchAccounts.mockResolvedValue(accounts);
    mockedFetchTaxSummary.mockResolvedValue({
      accountId: 1,
      taxYear: 2026,
      realizedProfit: "100000",
      realizedLoss: "0",
      taxableBase: "100000",
      taxAmount: "22000",
      localTaxAmount: "2200",
      effectiveTaxRate: "24.2000",
      status: "READY",
      computedAt: "2026-05-04T00:00:00.000Z",
    });
    mockedRecalculateTaxSummary.mockRejectedValue(new Error("409 Conflict: RUNNING"));

    const user = userEvent.setup();
    renderPage();

    const button = await screen.findByRole("button", { name: "세금 재계산" });
    await user.click(button);

    expect(await screen.findByText("이미 재계산이 진행 중입니다. 완료 후 다시 확인하세요.")).toBeInTheDocument();
    expect(button).toBeDisabled();

    await user.click(button);
    expect(mockedRecalculateTaxSummary).toHaveBeenCalledTimes(1);
  });
});
