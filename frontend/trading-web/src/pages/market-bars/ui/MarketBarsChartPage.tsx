import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchMarketBars, type MarketBar, type MarketBarsInterval } from "../../../shared/api/marketBarsApi";
import "./MarketBarsChartPage.css";

const LIMIT_OPTIONS = [50, 100, 200, 500] as const;
const INTERVAL_OPTIONS: MarketBarsInterval[] = ["1m", "5m", "10m"];

function getErrorStatus(error: unknown): number | null {
  if (!(error instanceof Error)) {
    return null;
  }
  const matched = error.message.match(/\b(\d{3})\b/);
  return matched ? Number(matched[1]) : null;
}

function formatUpdatedAt(value: string): string {
  return new Date(value).toLocaleString("ko-KR");
}

function BarsTable({ bars }: { bars: MarketBar[] }) {
  return (
    <div className="market-bars-chart-wrap">
      <table className="market-bars-table">
        <thead>
          <tr>
            <th>시각</th>
            <th className="market-bars-number">시가</th>
            <th className="market-bars-number">고가</th>
            <th className="market-bars-number">저가</th>
            <th className="market-bars-number">종가</th>
            <th className="market-bars-number">거래량</th>
          </tr>
        </thead>
        <tbody>
          {bars.map((bar) => (
            <tr key={bar.startedAt}>
              <td>{formatUpdatedAt(bar.startedAt)}</td>
              <td className="market-bars-number">{bar.open.toLocaleString("ko-KR")}</td>
              <td className="market-bars-number">{bar.high.toLocaleString("ko-KR")}</td>
              <td className="market-bars-number">{bar.low.toLocaleString("ko-KR")}</td>
              <td className="market-bars-number">{bar.close.toLocaleString("ko-KR")}</td>
              <td className="market-bars-number">{bar.volume.toLocaleString("ko-KR")}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function MarketBarsChartPage() {
  const [inputSymbol, setInputSymbol] = useState("");
  const [symbol, setSymbol] = useState("");
  const [interval, setInterval] = useState<MarketBarsInterval>("1m");
  const [limit, setLimit] = useState<number>(100);
  const [symbolError, setSymbolError] = useState("");

  const barsQuery = useQuery({
    queryKey: ["market-bars", symbol, interval, limit],
    enabled: symbol.trim().length > 0,
    retry: false,
    queryFn: () => fetchMarketBars({ symbol, interval, limit }),
  });

  const submit = () => {
    const next = inputSymbol.trim();
    if (!next) {
      setSymbolError("심볼을 입력해 주세요.");
      return;
    }
    setSymbolError("");
    setSymbol(next);
  };

  const statusCode = getErrorStatus(barsQuery.error);
  const isNotFound = statusCode === 404;
  const isBadRequest = statusCode === 400;
  const isEmpty = !barsQuery.isLoading && (isNotFound || (!barsQuery.isError && (barsQuery.data?.length ?? 0) === 0));
  const isServerError = barsQuery.isError && !isNotFound && !isBadRequest;

  return (
    <section className="panel market-bars-page">
      <div className="panel-header">
        <h2>분봉 히스토리 차트</h2>
      </div>

      <div className="market-bars-controls">
        <div className="market-bars-symbol-row">
          <input
            aria-label="심볼 입력"
            className="market-bars-input"
            value={inputSymbol}
            onChange={(event) => setInputSymbol(event.target.value)}
            placeholder="예: 005930, AAPL"
          />
          <button type="button" className="market-bars-btn" onClick={submit}>
            조회
          </button>
        </div>
        {symbolError ? <p style={{ color: "#dc2626", margin: 0 }}>{symbolError}</p> : null}
        <div className="market-bars-second-row">
          <div className="market-bars-tabs">
            {INTERVAL_OPTIONS.map((option) => (
              <button
                key={option}
                type="button"
                className="market-bars-tab"
                aria-pressed={interval === option}
                onClick={() => setInterval(option)}
              >
                {option}
              </button>
            ))}
          </div>
          <label>
            분봉 개수
            <select
              aria-label="분봉 개수"
              className="market-bars-select"
              value={limit}
              onChange={(event) => setLimit(Number(event.target.value))}
              style={{ marginLeft: 8 }}
            >
              {LIMIT_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      {barsQuery.isLoading ? <p>분봉 데이터를 불러오는 중...</p> : null}

      {isBadRequest ? (
        <div>
          <p>조회 조건을 확인해 주세요.</p>
          <button type="button" onClick={() => barsQuery.refetch()}>
            재시도
          </button>
        </div>
      ) : null}

      {isServerError ? (
        <div>
          <p>일시적인 오류가 발생했습니다. 다시 시도해 주세요.</p>
          <button type="button" onClick={() => barsQuery.refetch()}>
            재시도
          </button>
        </div>
      ) : null}

      {isEmpty ? <p>해당 조건의 분봉 데이터가 없습니다.</p> : null}

      {barsQuery.data && barsQuery.data.length > 0 ? (
        <>
          <p>마지막 갱신: {formatUpdatedAt(barsQuery.data[barsQuery.data.length - 1].startedAt)}</p>
          <BarsTable bars={barsQuery.data} />
        </>
      ) : null}
    </section>
  );
}
