import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { TaxSummarySummaryPanel } from "./TaxSummarySummaryPanel";

describe("TaxSummarySummaryPanel", () => {
  it("renders effectiveTaxRate without client-side percentage recomputation", () => {
    render(
      <TaxSummarySummaryPanel
        summary={{
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
        }}
      />
    );

    expect(screen.getByText("0.24%")).toBeInTheDocument();
    expect(screen.queryByText("24.20%")).not.toBeInTheDocument();
  });
});
