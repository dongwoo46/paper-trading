import { fetchJson } from "../../../shared/api";
import type {
  TaxSummaryRecalculateResponse,
  TaxSummaryResponse,
} from "../model/types";

export function fetchTaxSummary(
  accountId: number,
  taxYear: number
): Promise<TaxSummaryResponse | null> {
  return fetchJson<TaxSummaryResponse | null>(`/api/accounts/${accountId}/tax-summaries/${taxYear}`);
}

export function recalculateTaxSummary(
  accountId: number,
  taxYear: number
): Promise<TaxSummaryRecalculateResponse> {
  return fetchJson<TaxSummaryRecalculateResponse>(
    `/api/accounts/${accountId}/tax-summaries/${taxYear}/recalculate`,
    { method: "POST" }
  );
}
