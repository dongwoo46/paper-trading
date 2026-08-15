import fs from "node:fs";
import path from "node:path";
import { describe, expect, it } from "vitest";

const PAGE_FILES = [
  "account/ui/AccountDashboardPage.tsx",
  "chart-analysis/ui/ChartAnalysisPage.tsx",
  "historical/ui/HistoricalPage.tsx",
  "home/ui/HomePage.tsx",
  "macro/ui/MacroPage.tsx",
  "market-unified/ui/MarketUnifiedChartPage.tsx",
  "order/ui/OrderPage.tsx",
  "portfolio/ui/PortfolioChartPage.tsx",
  "realtime/ui/RealtimePage.tsx",
  "tax-summary/ui/TaxSummaryPage.tsx",
  "trading-journal/ui/TradingJournalPage.tsx",
];

describe("page header contract", () => {
  it.each(PAGE_FILES)("%s uses the shared primary heading", (relativePath) => {
    const source = fs.readFileSync(
      path.resolve("src/pages", relativePath),
      "utf8",
    );

    expect(source).toContain("<PageHeader");
    expect(source).not.toMatch(/<h1\b/);
  });
});
