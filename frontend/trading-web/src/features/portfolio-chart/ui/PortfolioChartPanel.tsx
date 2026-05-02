import type { ReturnSeriesPoint } from "../../../entities/portfolio/model/types";

interface PortfolioChartPanelProps {
  series: ReturnSeriesPoint[];
  isLoading: boolean;
  isError: boolean;
  benchmarkWarning: boolean;
}

export function PortfolioChartPanel(props: PortfolioChartPanelProps) {
  if (props.isLoading) {
    return <div style={{ padding: "24px", textAlign: "center" }}>차트 데이터를 불러오는 중...</div>;
  }

  if (props.isError) {
    return <div style={{ padding: "16px", color: "#ef4444" }}>차트 데이터를 불러오지 못했습니다.</div>;
  }

  if (props.series.length === 0) {
    return <div style={{ padding: "24px", textAlign: "center" }}>표시할 차트 데이터가 없습니다.</div>;
  }

  return (
    <section>
      {props.benchmarkWarning && (
        <div style={{ marginBottom: "12px", color: "#b45309", backgroundColor: "#fffbeb", padding: "10px" }}>
          일부 날짜의 벤치마크 데이터가 없어 해당 포인트를 제외했습니다.
        </div>
      )}
      <div style={{ overflowX: "auto" }}>
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "13px" }}>
          <thead>
            <tr>
              <th style={{ textAlign: "left" }}>날짜</th>
              <th style={{ textAlign: "right" }}>평가금액</th>
              <th style={{ textAlign: "right" }}>포트폴리오 수익률(%)</th>
              <th style={{ textAlign: "right" }}>KOSPI 수익률(%)</th>
            </tr>
          </thead>
          <tbody>
            {props.series.map((row) => (
              <tr key={row.date}>
                <td>{row.date}</td>
                <td style={{ textAlign: "right" }}>{row.evaluationAmount.toLocaleString("ko-KR")}</td>
                <td style={{ textAlign: "right" }}>{row.portfolioReturnPct.toFixed(2)}</td>
                <td style={{ textAlign: "right" }}>{row.benchmarkReturnPct.toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

