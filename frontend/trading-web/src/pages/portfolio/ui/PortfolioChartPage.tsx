import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchAccounts } from "../../../entities/account/api/accountApi";
import type { BenchmarkPoint } from "../../../entities/portfolio/model/types";
import { buildReturnSeries } from "../../../features/portfolio-chart/model/normalizeSeries";
import { PortfolioChartPanel } from "../../../features/portfolio-chart/ui/PortfolioChartPanel";
import { fetchDailyBalances, fetchKospiBenchmark } from "../../../shared/api/portfolioApi";
import { PageHeader } from "../../../shared/ui/PageHeader";
import { Label } from "@/shared/ui/shadcn/label";
import { NativeSelect, NativeSelectOption } from "@/shared/ui/shadcn/native-select";

function formatDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

export function PortfolioChartPage() {
  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null);
  const [rangeDays, setRangeDays] = useState(90);

  const { data: accounts = [] } = useQuery({
    queryKey: ["accounts", "list"],
    queryFn: fetchAccounts,
    staleTime: 30_000,
  });

  const resolvedAccountId = selectedAccountId ?? (accounts[0]?.id ?? null);

  const to = useMemo(() => formatDate(new Date()), []);
  const from = useMemo(() => {
    const d = new Date();
    d.setDate(d.getDate() - rangeDays);
    return formatDate(d);
  }, [rangeDays]);

  const chartQuery = useQuery({
    queryKey: ["portfolio-chart", resolvedAccountId, from, to],
    enabled: resolvedAccountId !== null,
    queryFn: async () => {
      const balances = await fetchDailyBalances(resolvedAccountId!, from, to);

      let benchmarkWarning = false;
      let benchmark: BenchmarkPoint[] = [];
      try {
        benchmark = await fetchKospiBenchmark(from, to);
      } catch (error) {
        if (error instanceof Error && error.message.includes("404")) {
          benchmarkWarning = true;
        } else {
          throw error;
        }
      }

      const series = buildReturnSeries({ balances, benchmark });
      if (benchmark.length > 0 && series.length < balances.length) {
        benchmarkWarning = true;
      }

      return { series, benchmarkWarning };
    },
  });

  return (
    <section className="flex flex-col gap-5 animate-fade-in">
      <PageHeader
        eyebrow="트레이딩"
        title="포트폴리오 차트"
        description="일별 평가금액 추이와 KOSPI 대비 누적 수익률을 비교합니다."
      />
      <div className="mb-4 flex flex-wrap gap-4">
        <div className="flex items-center gap-2">
          <Label htmlFor="portfolio-account">계좌 선택</Label>
          <NativeSelect
            id="portfolio-account"
            aria-label="계좌 선택"
            value={resolvedAccountId ?? ""}
            onChange={(e) => setSelectedAccountId(Number(e.target.value))}
          >
            {accounts.map((account) => (
              <NativeSelectOption key={account.id} value={account.id}>
                {account.accountName}
              </NativeSelectOption>
            ))}
          </NativeSelect>
        </div>
        <div className="flex items-center gap-2">
          <Label htmlFor="portfolio-range">기간</Label>
          <NativeSelect
            id="portfolio-range"
            aria-label="기간 선택"
            value={rangeDays}
            onChange={(e) => setRangeDays(Number(e.target.value))}
          >
            <NativeSelectOption value={30}>1개월</NativeSelectOption>
            <NativeSelectOption value={90}>3개월</NativeSelectOption>
            <NativeSelectOption value={180}>6개월</NativeSelectOption>
          </NativeSelect>
        </div>
      </div>
      <PortfolioChartPanel
        series={chartQuery.data?.series ?? []}
        isLoading={chartQuery.isLoading}
        isError={chartQuery.isError}
        benchmarkWarning={chartQuery.data?.benchmarkWarning ?? false}
      />
    </section>
  );
}
