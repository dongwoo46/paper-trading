import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchAccounts } from "../../../entities/account/api/accountApi";
import type { BenchmarkPoint } from "../../../entities/portfolio/model/types";
import { buildReturnSeries } from "../../../features/portfolio-chart/model/normalizeSeries";
import { PortfolioChartPanel } from "../../../features/portfolio-chart/ui/PortfolioChartPanel";
import { fetchDailyBalances, fetchKospiBenchmark } from "../../../shared/api/portfolioApi";

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
    <section className="panel">
      <div className="panel-header">
        <h2>포트폴리오 차트</h2>
        <p className="lead">일별 평가금액 추이와 KOSPI 대비 누적 수익률을 확인합니다.</p>
      </div>
      <div style={{ display: "flex", gap: "12px", marginBottom: "16px" }}>
        <label>
          계좌 선택
          <select
            aria-label="계좌 선택"
            value={resolvedAccountId ?? ""}
            onChange={(e) => setSelectedAccountId(Number(e.target.value))}
            style={{ marginLeft: "8px" }}
          >
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.accountName}
              </option>
            ))}
          </select>
        </label>
        <label>
          기간
          <select
            aria-label="기간 선택"
            value={rangeDays}
            onChange={(e) => setRangeDays(Number(e.target.value))}
            style={{ marginLeft: "8px" }}
          >
            <option value={30}>1개월</option>
            <option value={90}>3개월</option>
            <option value={180}>6개월</option>
          </select>
        </label>
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
