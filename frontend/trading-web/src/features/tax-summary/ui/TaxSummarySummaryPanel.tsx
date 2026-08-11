import type { TaxSummaryResponse } from "../../../entities/tax-summary/model/types";
import { mapTaxSummaryStatusLabel } from "../../../entities/tax-summary/model/types";
import { formatAmount, formatRate } from "../../../shared/utils/format";
import { Badge } from "@/shared/ui/shadcn/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/shadcn/card";

interface TaxSummarySummaryPanelProps {
  summary: TaxSummaryResponse;
}

export function TaxSummarySummaryPanel({ summary }: TaxSummarySummaryPanelProps) {
  const effectiveTaxRatePercent = (() => {
    const rate = summary.effectiveTaxRate?.trim();
    if (!rate) return null;
    if (!rate.includes(".")) return `${rate}.00`;
    const [intPart, fractionPart] = rate.split(".");
    const safeFraction = (fractionPart ?? "").replace(/[^0-9]/g, "");
    const firstTwo = safeFraction.slice(0, 2);
    const thirdDigit = safeFraction.charCodeAt(2) - 48;
    const carry = Number.isFinite(thirdDigit) && thirdDigit >= 5 ? 1 : 0;
    const frac = firstTwo.padEnd(2, "0").split("").map((digit) => Number(digit));

    if (carry === 1) {
      frac[1] += 1;
      if (frac[1] >= 10) {
        frac[1] = 0;
        frac[0] += 1;
        if (frac[0] >= 10) {
          frac[0] = 0;
          return `${String(Number(intPart || "0") + 1)}.00`;
        }
      }
    }

    return `${intPart || "0"}.${frac.join("")}`;
  })();

  const cards = [
    ["실현 이익", formatAmount(summary.realizedProfit, "KRW")],
    ["실현 손실", formatAmount(summary.realizedLoss, "KRW")],
    ["과세 표준", formatAmount(summary.taxableBase, "KRW")],
    ["세액", formatAmount(summary.taxAmount, "KRW")],
    ["지방세", formatAmount(summary.localTaxAmount, "KRW")],
    ["실효 세율", formatRate(effectiveTaxRatePercent)],
  ] as const;

  return (
    <div className="grid gap-3">
      <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-muted-foreground">
        <Badge variant="secondary">상태: {mapTaxSummaryStatusLabel(summary.status)}</Badge>
        <span>계산 시각: {summary.computedAt ?? "-"}</span>
      </div>
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {cards.map(([title, value]) => (
          <Card key={title} size="sm">
            <CardHeader><CardTitle className="text-xs text-muted-foreground">{title}</CardTitle></CardHeader>
            <CardContent className="font-semibold tabular-nums">{value}</CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
