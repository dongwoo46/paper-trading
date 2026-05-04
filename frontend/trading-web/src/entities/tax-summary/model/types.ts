export type TaxSummaryStatus = "READY" | "RUNNING" | "FAILED";

export interface TaxSummaryResponse {
  accountId: number;
  taxYear: number;
  realizedProfit: string;
  realizedLoss: string;
  taxableBase: string;
  taxAmount: string;
  localTaxAmount: string;
  effectiveTaxRate: string;
  status: TaxSummaryStatus;
  computedAt: string | null;
}

export interface TaxSummaryRecalculateResponse {
  runId: number;
  status: TaxSummaryStatus;
  requestedAt: string;
}

export function mapTaxSummaryStatusLabel(status: TaxSummaryStatus): string {
  if (status === "READY") return "준비됨";
  if (status === "RUNNING") return "계산 중";
  return "실패";
}
